/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.lab

import android.app.PendingIntent
import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.core.content.edit
import com.google.android.gms.nearby.exposurenotification.ExposureConfiguration
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import nl.rijksoverheid.en.lab.exposurenotification.StartResult
import nl.rijksoverheid.en.lab.exposurenotification.StatusResult
import nl.rijksoverheid.en.lab.exposurenotification.StopResult
import nl.rijksoverheid.en.lab.exposurenotification.TemporaryExposureKeysResult
import nl.rijksoverheid.en.lab.exposurenotification.getApiStatus
import nl.rijksoverheid.en.lab.exposurenotification.getExposureDetails
import nl.rijksoverheid.en.lab.exposurenotification.getTemporaryExposureKeys
import nl.rijksoverheid.en.lab.exposurenotification.requestDisableNotifications
import nl.rijksoverheid.en.lab.exposurenotification.requestEnableNotifications
import nl.rijksoverheid.en.lab.keys.KeyFileSigner
import nl.rijksoverheid.en.lab.keys.KeyFileWriter
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.security.SecureRandom

private val EQUAL_WEIGHTS = intArrayOf(1, 1, 1, 1, 1, 1, 1, 1)
private val SEQUENTIAL_WEIGHTS = intArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
private const val KEY_SOURCE_DEVICE = "source_device"
private const val KEY_TEST_ID = "test_id"
private const val KEY_SCANNED_TEK = "scanned_tek"
private const val ATTN_THRESHOLD_LOW = 42
private const val ATTN_THRESHOLD_HIGH = 56

class NotificationsRepository(
    private val context: Context,
    private val exposureNotificationClient: ExposureNotificationClient
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

    fun clearExposureInformation() {
        measurements.edit {
            putString("result", "[]")
        }
    }

    suspend fun storeExposureInformation(token: String) {
        val details = exposureNotificationClient.getExposureDetails(token)

        measurements.edit {
            putString(
                "result",
                exposuresAdapter.toJson(details.map {
                    ExposureInfo(
                        it.attenuationValue,
                        it.durationMinutes,
                        it.transmissionRiskLevel,
                        it.totalRiskScore,
                        it.attenuationDurationsInMinutes
                    )
                })
            )
        }
    }

    private fun getTestResultsFromPreferences(prefs: SharedPreferences): TestResults {
        val json = prefs.getString("result", "[]") ?: "[]"
        val exposures = exposuresAdapter.fromJson(json)!!
        return TestResults(
            prefs.getString(KEY_SCANNED_TEK, "") ?: "",
            prefs.getString(KEY_SOURCE_DEVICE, "no-source")!!, prefs.getString(
                KEY_TEST_ID, "no-test-id"
            )!!, exposures
        )
    }

    fun getTestResults(): Flow<TestResults> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, _ ->
            offer(getTestResultsFromPreferences(sharedPreferences))
        }

        offer(getTestResultsFromPreferences(measurements))

        measurements.registerOnSharedPreferenceChangeListener(listener)

        awaitClose {
            measurements.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    suspend fun exportTemporaryExposureKeys(): ExportTemporaryExposureKeysResult {
        val result = exposureNotificationClient.getTemporaryExposureKeys()
        Timber.d("Result = $result")

        when (result) {
            is TemporaryExposureKeysResult.Success -> {
                return if (result.keys.isNotEmpty()) {
                    val latest = result.keys.maxBy { it.rollingStartIntervalNumber }!!
                    ExportTemporaryExposureKeysResult.Success(
                        latest
                    )
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

    suspend fun importTemporaryExposureKey(key: com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey): ImportTemporaryExposureKeysResult {
        measurements.edit {
            putString(KEY_SCANNED_TEK, Base64.encodeToString(key.keyData, 0))
        }
        return withContext(Dispatchers.IO) {
            try {
                val file = File(context.cacheDir, "keyimport")
                val writer = KeyFileWriter(
                    KeyFileSigner.get(context)
                )
                writer.exportKey(key, file)

                exposureNotificationClient.provideDiagnosisKeys(
                    listOf(file),
                    ExposureConfiguration.ExposureConfigurationBuilder()
                        .setAttenuationScores(*SEQUENTIAL_WEIGHTS)
                        .setAttenuationWeight(1)
                        .setDaysSinceLastExposureScores(*EQUAL_WEIGHTS)
                        .setDaysSinceLastExposureWeight(1)
                        .setDurationScores(*EQUAL_WEIGHTS)
                        .setDurationWeight(1)
                        .setTransmissionRiskScores(*EQUAL_WEIGHTS)
                        .setTransmissionRiskWeight(1)
                        .setMinimumRiskScore(1)
                        .setDurationAtAttenuationThresholds(ATTN_THRESHOLD_LOW, ATTN_THRESHOLD_HIGH)
                        .build(),
                    generateImportToken()
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

    private fun generateImportToken(): String {
        val tokenBytes = ByteArray(32)
        SecureRandom().nextBytes(tokenBytes)
        return Base64.encodeToString(tokenBytes, Base64.NO_WRAP or Base64.NO_PADDING)
            .replace("/", "_").replace("+", "-")
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
    data class Success(val latestKey: com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey) :
        ExportTemporaryExposureKeysResult()

    object NoKeys : ExportTemporaryExposureKeysResult()
    data class RequireConsent(val resolution: PendingIntent) : ExportTemporaryExposureKeysResult()
    data class Error(val exception: Exception) : ExportTemporaryExposureKeysResult()
}

sealed class ImportTemporaryExposureKeysResult {
    object Success : ImportTemporaryExposureKeysResult()
    data class Error(val exception: Exception) : ImportTemporaryExposureKeysResult()
}
