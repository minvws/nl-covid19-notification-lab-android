/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.lab

import android.app.PendingIntent
import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import com.google.android.gms.nearby.exposurenotification.DiagnosisKeyFileProvider
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import nl.rijksoverheid.en.lab.exposurenotification.StartResult
import nl.rijksoverheid.en.lab.exposurenotification.StatusResult
import nl.rijksoverheid.en.lab.exposurenotification.StopResult
import nl.rijksoverheid.en.lab.exposurenotification.TemporaryExposureKeysResult
import nl.rijksoverheid.en.lab.exposurenotification.getApiStatus
import nl.rijksoverheid.en.lab.exposurenotification.getTemporaryExposureKeys
import nl.rijksoverheid.en.lab.exposurenotification.requestDisableNotifications
import nl.rijksoverheid.en.lab.exposurenotification.requestEnableNotifications
import nl.rijksoverheid.en.lab.exposurenotification.retrieveExposureWindows
import nl.rijksoverheid.en.lab.keys.KeyFileSigner
import nl.rijksoverheid.en.lab.keys.KeyFileWriter
import nl.rijksoverheid.en.lab.keys.ResultsExporter
import nl.rijksoverheid.en.lab.storage.TestResultDatabase
import nl.rijksoverheid.en.lab.storage.model.ExposureWindow
import nl.rijksoverheid.en.lab.storage.model.TestResult
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

private val EQUAL_WEIGHTS = intArrayOf(1, 1, 1, 1, 1, 1, 1, 1)
private val SEQUENTIAL_WEIGHTS = intArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
private const val KEY_SOURCE_DEVICE = "source_device"
private const val KEY_TEST_ID = "test_id"
private const val KEY_SCANNED_TEK = "scanned_tek"
private const val ATTN_THRESHOLD_LOW = 42
private const val ATTN_THRESHOLD_HIGH = 56

class NotificationsRepository(
    private val context: Context,
    private val exposureNotificationClient: ExposureNotificationClient,
    private val db: TestResultDatabase
) {

    companion object {
        const val DEFAULT_ROLLING_PERIOD = 144
    }

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val exposuresAdapter = moshi.adapter<List<ExposureInfo>>(
        Types.newParameterizedType(
            List::class.java,
            ExposureInfo::class.java
        )
    )

    private val measurements = context.getSharedPreferences("measurements", 0)

    suspend fun requestEnableNotifications(): StartResult {
        return exposureNotificationClient.requestEnableNotifications()
    }

    suspend fun requestDisableNotifications(): StopResult {
        return exposureNotificationClient.requestDisableNotifications()
    }

    suspend fun getStatus(): StatusResult {
        return exposureNotificationClient.getApiStatus()
    }

    suspend fun clearResults() {
        db.getTestResultDao().removeTestResults()
    }

    suspend fun exportResults(): File {
        val exportsDirectory = File(context.filesDir, "exports")
        exportsDirectory.mkdirs()
        val export = File(exportsDirectory, "export.csv")
        val results = getTestResults().take(1).toList().flatten()
        withContext(Dispatchers.IO) {
            val exporter = ResultsExporter()
            exporter.export(results, export)
        }
        return export
    }

    suspend fun storeExposureInformation() {
        val results = exposureNotificationClient.retrieveExposureWindows()
        val testResult = TestResult(
            UUID.randomUUID().toString(),
            measurements.getString(KEY_SCANNED_TEK, null)!!,
            measurements.getString(
                KEY_SOURCE_DEVICE, null
            )!!,
            measurements.getString(KEY_TEST_ID, null)!!,
            System.currentTimeMillis()
        )
        val windows = results.map { ExposureWindow.fromExposureWindow(testResult.id, it) }
        Timber.d("Storing exposure information $testResult ${windows.size} exposure windows")
        db.getTestResultDao().storeTestResult(testResult.copy(exposureWindows = windows))
    }

    @ExperimentalCoroutinesApi
    fun getTestResults(): Flow<List<TestResult>> {
        return db.getTestResultDao().getTestResults().map { results ->
            results.map { testResult ->
                val windows = db.getTestResultDao().getExposureWindows(testResult.id).map {
                    val scans = db.getTestResultDao().getScanInstances(it.id)
                    it.copy(scanInstances = scans)
                }
                testResult.copy(exposureWindows = windows)
            }
        }
    }

    suspend fun exportTemporaryExposureKeys(): ExportTemporaryExposureKeysResult {
        val result = exposureNotificationClient.getTemporaryExposureKeys()
        Timber.d("Result = $result")

        when (result) {
            is TemporaryExposureKeysResult.Success -> {
                val tekDateTime = LocalDate.now(
                    ZoneId.of("UTC")
                ).atStartOfDay().atZone(ZoneId.of("UTC"))
                val rollingStartIntervalNumber = tekDateTime.toEpochSecond() / 600
                val keysForToday =
                    result.keys.filter { it.rollingStartIntervalNumber == rollingStartIntervalNumber.toInt() }
                return if (keysForToday.isNotEmpty()) {
                    ExportTemporaryExposureKeysResult.Success(keysForToday)
                } else {
                    ExportTemporaryExposureKeysResult.NoKeys
                }
            }
            is TemporaryExposureKeysResult.RequireConsent -> return ExportTemporaryExposureKeysResult.RequireConsent(
                result.resolution
            )
            is TemporaryExposureKeysResult.Error -> return ExportTemporaryExposureKeysResult.Error(
                result.ex
            )
        }
    }

    suspend fun importTemporaryExposureKey(key: TemporaryExposureKey): ImportTemporaryExposureKeysResult {
        val currentMatches = exposureNotificationClient.retrieveExposureWindows()
        if (currentMatches.isNotEmpty()) {
            return ImportTemporaryExposureKeysResult.PreviousResults
        }
        measurements.edit {
            putString(KEY_SCANNED_TEK, Base64.encodeToString(key.keyData, Base64.NO_WRAP))
        }
        return withContext(Dispatchers.IO) {
            try {
                val file = File(context.cacheDir, "keyimport")
                val writer = KeyFileWriter(
                    KeyFileSigner.get(context)
                )
                writer.exportKey(key, file)

                exposureNotificationClient.provideDiagnosisKeys(
                    DiagnosisKeyFileProvider(
                        listOf(file)
                    )
                ).addOnFailureListener {
                    Timber.e(it, "Error importing keys")
                }
                ImportTemporaryExposureKeysResult.Success
            } catch (ex: IOException) {
                Timber.e(ex, "Error downloading and processing keys")
                ImportTemporaryExposureKeysResult.Error(
                    ex
                )
            }
        }
    }

    fun setSourceAndTestId(sourceDeviceId: String, testId: String) {
        measurements.edit {
            putString(KEY_SOURCE_DEVICE, sourceDeviceId)
            putString(KEY_TEST_ID, testId)
        }
    }

    @JsonClass(generateAdapter = true)
    data class ExposureInfo(
        val attenuation: Int,
        val duration: Int,
        val transmissionRisk: Int,
        val totalRiskScore: Int,
        val attenuationDurations: IntArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ExposureInfo

            if (attenuation != other.attenuation) return false
            if (duration != other.duration) return false
            if (transmissionRisk != other.transmissionRisk) return false
            if (totalRiskScore != other.totalRiskScore) return false
            if (!attenuationDurations.contentEquals(other.attenuationDurations)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = attenuation
            result = 31 * result + duration
            result = 31 * result + transmissionRisk
            result = 31 * result + totalRiskScore
            result = 31 * result + attenuationDurations.contentHashCode()
            return result
        }
    }

    data class TestResults(
        val scannedTek: String,
        val sourceDeviceId: String,
        val testId: String,
        val exposures: List<ExposureInfo>
    )
}

sealed class ExportTemporaryExposureKeysResult {
    data class Success(val keys: List<TemporaryExposureKey>) :
        ExportTemporaryExposureKeysResult()

    object NoKeys : ExportTemporaryExposureKeysResult()
    data class RequireConsent(val resolution: PendingIntent) : ExportTemporaryExposureKeysResult()
    data class Error(val exception: Exception) : ExportTemporaryExposureKeysResult()
}

sealed class ImportTemporaryExposureKeysResult {
    object Success : ImportTemporaryExposureKeysResult()
    object PreviousResults : ImportTemporaryExposureKeysResult()
    data class Error(val exception: Exception) : ImportTemporaryExposureKeysResult()
}
