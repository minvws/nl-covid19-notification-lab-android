/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.entoolkit.ble

import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okio.Buffer
import java.util.UUID

private val EN_SERVICE_UUID = UUID.fromString("0000FD6F-0000-1000-8000-00805F9B34FB")

class ExposureNotificationsScanner(private val context: Context) {

    fun scanResults(): Flow<ExposureNotificationAdvertisement> = callbackFlow {
        val manager = context.getSystemService(BluetoothManager::class.java)!!

        val callback = object : ScanCallback() {
            @Suppress("BlockingMethodInNonBlockingContext")
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val data = result.scanRecord?.serviceData?.get(ParcelUuid(EN_SERVICE_UUID))
                if (data != null) {
                    val buffer = Buffer()
                    buffer.write(data)
                    if (buffer.size == 20L) {
                        val rpi = buffer.readByteArray(16)
                        val aem = buffer.readByteArray(4)
                        offer(
                            ExposureNotificationAdvertisement(
                                rpi,
                                aem,
                                result.rssi,
                                System.currentTimeMillis()
                            )
                        )
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                close(RuntimeException("Scan failed with error $errorCode"))
            }
        }

        manager.adapter.bluetoothLeScanner.startScan(
            listOf(
                ScanFilter.Builder().setServiceUuid(
                    ParcelUuid(EN_SERVICE_UUID)
                ).build()
            ),
            ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES).build(),
            callback
        )

        awaitClose {
            manager.adapter.bluetoothLeScanner.stopScan(callback)
        }
    }

    data class ExposureNotificationAdvertisement(
        val rpi: ByteArray,
        val aem: ByteArray,
        val rssi: Int,
        val timestamp: Long
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ExposureNotificationAdvertisement

            if (!rpi.contentEquals(other.rpi)) return false
            if (!aem.contentEquals(other.aem)) return false
            if (rssi != other.rssi) return false
            if (timestamp != other.timestamp) return false

            return true
        }

        override fun hashCode(): Int {
            var result = rpi.contentHashCode()
            result = 31 * result + aem.contentHashCode()
            result = 31 * result + rssi
            result = 31 * result + timestamp.hashCode()
            return result
        }
    }
}
