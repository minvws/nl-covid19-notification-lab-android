/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.lab.status

import android.app.PendingIntent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Base64
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import kotlinx.coroutines.launch
import nl.rijksoverheid.en.lab.ExportTemporaryExposureKeysResult
import nl.rijksoverheid.en.lab.NotificationsRepository
import nl.rijksoverheid.en.lab.exposurenotification.StartResult
import nl.rijksoverheid.en.lab.exposurenotification.StatusResult
import nl.rijksoverheid.en.lab.exposurenotification.StopResult
import nl.rijksoverheid.en.lab.lifecyle.Event
import org.json.JSONObject
import timber.log.Timber

class NotificationsStatusViewModel(
    private val repository: NotificationsRepository,
    private val preferences: SharedPreferences
) : ViewModel() {

    val notificationState: LiveData<NotificationsState> = MutableLiveData()
    val notificationsResult: LiveData<Event<NotificationsStatusResult>> = MutableLiveData()
    val shareTekResult: LiveData<Event<ShareTekResult>> = MutableLiveData()

    val testId = MutableLiveData("")
    val deviceName = MutableLiveData(preferences.getString("device_name", "")!!)
    private var currentKeys: List<TemporaryExposureKey> = emptyList()

    init {
        viewModelScope.launch {
            when (val result = repository.getStatus()) {
                is StatusResult.Enabled -> updateState(NotificationsState.Enabled)
                is StatusResult.Disabled -> updateState(NotificationsState.Disabled)
                is StatusResult.Unavailable -> updateState(NotificationsState.Unavailable)
                is StatusResult.UnknownError -> {
                    Timber.d(
                        result.ex,
                        "Unknown error while getting status"
                    )
                    updateResult(
                        NotificationsStatusResult.UnknownError(
                            result.ex
                        )
                    )
                }
            }
        }
    }

    fun requestEnableNotifications() {
        viewModelScope.launch {
            when (val result = repository.requestEnableNotifications()) {
                is StartResult.Started -> updateState(NotificationsState.Enabled)
                is StartResult.ResolutionRequired -> updateResult(
                    NotificationsStatusResult.ConsentRequired(
                        result.resolution
                    )
                )
                is StartResult.UnknownError -> updateResult(
                    NotificationsStatusResult.UnknownError(
                        result.ex
                    )
                )
            }
        }
    }

    fun requestDisableNotifications() {
        viewModelScope.launch {
            when (val result = repository.requestDisableNotifications()) {
                is StopResult.Stopped -> updateState(NotificationsState.Disabled)
                is StopResult.UnknownError -> updateResult(
                    NotificationsStatusResult.UnknownError(
                        result.ex
                    )
                )
            }
        }
    }

    fun notificationsStarted() {
        updateState(NotificationsState.Enabled)
    }

    fun notificationsNotStarted() {
        updateState(NotificationsState.Disabled)
    }

    private fun updateState(state: NotificationsState) {
        (notificationState as MutableLiveData).value = state
    }

    private fun updateResult(result: NotificationsStatusResult) {
        (notificationsResult as MutableLiveData).value = Event(result)
    }

    private fun updateResult(result: ShareTekResult) {
        (shareTekResult as MutableLiveData).value = Event(result)
    }

    fun canShareTek(): Boolean {
        return deviceName.value!!.isNotBlank() && testId.value!!.isNotBlank() && notificationState.value == NotificationsState.Enabled
    }

    fun shareTek(size: Int) {
        viewModelScope.launch {
            when (val result = repository.exportTemporaryExposureKeys()) {
                is ExportTemporaryExposureKeysResult.RequireConsent -> updateResult(
                    ShareTekResult.RequestConsent(
                        result.resolution
                    )
                )
                is ExportTemporaryExposureKeysResult.Success -> {
                    currentKeys = result.keys
                    generateQrCode(size, result.keys)
                }
                is ExportTemporaryExposureKeysResult.NoKeys -> {
                    updateResult(ShareTekResult.NoKeys)
                }
                is ExportTemporaryExposureKeysResult.Error -> {
                    Timber.e(result.exception, "Error while exporting keys")
                    updateResult(ShareTekResult.Error)
                }
            }
        }
    }

    private fun generateQrCode(size: Int, keys: List<TemporaryExposureKey>) {
        val latestKey = keys.first() // appears to be the "latest key"
        val json = JSONObject()
            .put(
                "keyData", Base64.encodeToString(latestKey.keyData, Base64.NO_WRAP)
            )
            .put(
                "rollingStartNumber",
                latestKey.rollingStartIntervalNumber
            )
            .put(
                "rollingPeriod",
                NotificationsRepository.DEFAULT_ROLLING_PERIOD
            )
            .put(
                "transmissionRiskLevel",
                latestKey.transmissionRiskLevel
            )
            .put("allKeys", keys.map { Base64.encodeToString(it.keyData, Base64.NO_WRAP) })
            .put("testId", testId.value)
            .put("deviceId", deviceName.value)

        val bitmap = encodeAsQRCode(size, json.toString())
        bitmap?.let {
            updateResult(
                ShareTekResult.Success(
                    it,
                    Base64.encodeToString(latestKey.keyData, 0)
                )
            )
        } ?: Timber.w("QR Code could not be generated")
    }

    private fun encodeAsQRCode(size: Int, data: String): Bitmap? {
        val result: BitMatrix =
            MultiFormatWriter().encode(data, BarcodeFormat.QR_CODE, size, size, null)
        val width = result.width
        val height = result.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (result[x, y]) Color.BLACK else Color.WHITE
            }
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    fun storeDeviceId() {
        preferences.edit {
            putString("device_name", deviceName.value!!)
        }
    }

    fun updateQrCode(size: Int) {
        if (currentKeys.isNotEmpty()) {
            generateQrCode(size, currentKeys)
        }
    }

    sealed class NotificationsState {
        object Enabled : NotificationsState()
        object Disabled : NotificationsState()
        object Unavailable : NotificationsState()
    }

    sealed class ShareTekResult {
        data class RequestConsent(val resolution: PendingIntent) : ShareTekResult()
        object Error : ShareTekResult()
        object NoKeys : ShareTekResult()
        data class Success(val qrCode: Bitmap, val keyBase64: String) : ShareTekResult()
    }

    sealed class NotificationsStatusResult {
        data class ConsentRequired(val intent: PendingIntent) : NotificationsStatusResult()
        data class UnknownError(val exception: Exception) : NotificationsStatusResult()
    }
}
