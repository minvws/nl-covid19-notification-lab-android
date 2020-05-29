/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.entoolkit.ble

import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.ParcelUuid
import kotlinx.coroutines.suspendCancellableCoroutine
import nl.rijksoverheid.entoolkit.Crypto
import java.util.UUID
import kotlin.coroutines.resume

private val EN_SERVICE_UUID = UUID.fromString("0000FD6F-0000-1000-8000-00805F9B34FB")

class ExposureNotificationsAdvertiser(private val context: Context) {
    private var enServiceCallback: AdvertiseCallback? = null

    /**
     * Start advertising the RPI for the given Tek and timestamp. Note that this will not automatically
     * roll over the RPI after the associated interval changes. Advertising must be stopped and restarted when the RPI
     * needs to roll over.
     *
     * @param tek the TEK used for creating the RPI and metadata
     * @param interval the interval for this RPI.
     * @param txPower the value for the tx power field in the associated metadata
     * @see [Crypto.getEnIntervalNumber]
     */
    suspend fun startAdvertising(
        tek: Crypto.TemporaryExposureKey,
        interval: Long = Crypto.getEnIntervalNumber(System.currentTimeMillis()),
        txPower: Int = -42
    ) = suspendCancellableCoroutine<Boolean> { c ->
        stopAdvertising()

        val manager = context.getSystemService(BluetoothManager::class.java)!!

        if (manager.adapter.bluetoothLeAdvertiser == null) {
            c.resume(false)
            return@suspendCancellableCoroutine
        }

        enServiceCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                if (!c.isCompleted) {
                    c.resume(true)
                }
            }

            override fun onStartFailure(errorCode: Int) {
                if (!c.isCompleted) {
                    c.resume(false)
                }
            }
        }

        val rpiKey = Crypto.createRpiKey(tek)
        val rpi = Crypto.createRpi(rpiKey, interval)
        val metadataKey = Crypto.createAssociatedMetadataKey(tek)
        val metadata = Crypto.encryptAssociatedMetadata(rpi, metadataKey, -txPower)

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setConnectable(false)
            // based on rssi this seems to be the power used by the GACT API, but needs further investigation
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .build()

        val serviceData = rpi + metadata

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .addServiceUuid(ParcelUuid(EN_SERVICE_UUID))
            .addServiceData(ParcelUuid(EN_SERVICE_UUID), serviceData)
            .build()

        manager.adapter.bluetoothLeAdvertiser?.startAdvertising(settings, data, enServiceCallback)

        c.invokeOnCancellation {
            stopAdvertising()
        }
    }

    fun stopAdvertising() {
        enServiceCallback?.let {
            enServiceCallback = null
            val manager = context.getSystemService(BluetoothManager::class.java)!!
            manager.adapter.bluetoothLeAdvertiser?.stopAdvertising(it)
        }
    }
}
