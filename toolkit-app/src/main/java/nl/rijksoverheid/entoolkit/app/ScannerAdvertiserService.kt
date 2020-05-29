/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.entoolkit.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.distinctUntilChanged
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import nl.rijksoverheid.entoolkit.Crypto
import nl.rijksoverheid.entoolkit.ble.ExposureNotificationsAdvertiser
import nl.rijksoverheid.entoolkit.ble.ExposureNotificationsScanner
import okio.ByteString.Companion.toByteString
import timber.log.Timber

const val ACTION_FOREGROUND = "${BuildConfig.APPLICATION_ID}.START_FOREGROUND"
const val ACTION_STOP = "${BuildConfig.APPLICATION_ID}.STOP"

class ScannerAdvertiserService : LifecycleService() {

    private lateinit var advertiser: ExposureNotificationsAdvertiser
    private lateinit var scanner: ExposureNotificationsScanner

    private var currentTek: Crypto.TemporaryExposureKey = Crypto.createTemporaryExposureKey()
    private var rpiKey: ByteArray = Crypto.createRpiKey(currentTek)
    private var currentInterval: Long = Crypto.getEnIntervalNumber()
    private var rpiHex: String = Crypto.createRpi(rpiKey, currentInterval).toByteString().hex()
    private var scanJob: Job? = null
    private val scanResults = mutableListOf<ScannedRpi>()
    private val scanResultsLiveData = MutableLiveData<List<ScannedRpi>>()

    private val tickReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            onTick()
        }
    }

    private val advertisingState = MutableLiveData<AdvertisingState>()

    override fun onCreate() {
        super.onCreate()
        Timber.d("onCreate")
        advertiser = ExposureNotificationsAdvertiser(this)
        scanner = ExposureNotificationsScanner(this)
        startAdvertising(currentInterval)
        registerReceiver(tickReceiver, IntentFilter(Intent.ACTION_TIME_TICK))
    }

    override fun onDestroy() {
        Timber.d("onDestroy")
        unregisterReceiver(tickReceiver)
        advertiser.stopAdvertising()
        super.onDestroy()
    }

    private fun onTick() {
        Timber.d("onTick")
        val interval = Crypto.getEnIntervalNumber()
        if (interval != currentInterval) {
            Timber.d("New interval")
            currentInterval = interval
            startAdvertising(interval)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_FOREGROUND) {
            startForeground(1, createNotification())
        } else if (intent?.action == ACTION_STOP) {
            stopSelf()
            return Service.START_NOT_STICKY
        }
        return Service.START_STICKY
    }

    private fun createNotification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)!!
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(
                    "advertiser",
                    getString(R.string.advertising_notification_channel),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }

        return NotificationCompat.Builder(this, "advertiser")
            .setContentTitle(getString(R.string.advertising))
            .setContentText(getString(R.string.advertising_message))
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    0
                )
            )
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.ic_action_stop,
                    getString(R.string.action_stop_advertising),
                    PendingIntent.getService(
                        this,
                        0,
                        Intent(this, ScannerAdvertiserService::class.java).setAction(ACTION_STOP),
                        0
                    )
                ).build()
            ).build()
    }

    private fun startAdvertising(interval: Long) {
        if (interval % 144 == 0L) {
            Timber.d("Rolling over TEK")
            currentTek = Crypto.createTemporaryExposureKey()
            rpiKey = Crypto.createRpiKey(currentTek)
        }
        lifecycle.coroutineScope.launchWhenCreated {
            rpiHex = Crypto.createRpi(rpiKey, interval).toByteString().hex()
            val started = advertiser.startAdvertising(currentTek, currentInterval)
            if (started) {
                rpiHex = Crypto.createRpi(rpiKey, interval).toByteString().hex()
                Timber.d("Started advertiser for RPI $rpiHex")
                advertisingState.value = AdvertisingState.Started(
                    currentTek.data.toByteString().hex(),
                    currentTek.interval,
                    rpiHex,
                    currentInterval
                )
            } else {
                Timber.e("Could not start advertiser")
                advertisingState.value = AdvertisingState.Error
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        onRebind(intent)
        return LocalBinder()
    }

    private fun startScanning() {
        Timber.d("Start scanning")
        scanJob?.cancel()
        scanJob = lifecycle.coroutineScope.launchWhenCreated {
            scanner.scanResults().collect { advertisement ->
                val hex = advertisement.rpi.toByteString().hex()
                val index = scanResults.indexOfFirst { it.rpiHex == hex }
                if (index > -1) {
                    scanResults[index] =
                        ScannedRpi(
                            hex,
                            advertisement.aem,
                            advertisement.rssi,
                            advertisement.timestamp
                        )
                } else {
                    scanResults.add(
                        ScannedRpi(
                            hex,
                            advertisement.aem,
                            advertisement.rssi,
                            advertisement.timestamp
                        )
                    )
                }
                scanResults.removeAll { System.currentTimeMillis() - it.timestamp > 30000 }
                scanResultsLiveData.value = scanResults
            }
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (advertisingState.value is AdvertisingState.Started) {
            ContextCompat.startForegroundService(
                this,
                Intent(this, ScannerAdvertiserService::class.java).setAction(ACTION_FOREGROUND)
            )
        } else {
            stopSelf()
        }
        scanJob?.cancel()
        return true
    }

    override fun onRebind(intent: Intent?) {
        stopForeground(true)
        // ensure we keep running after unbind
        startService(Intent(this, ScannerAdvertiserService::class.java))
        startScanning()
    }

    inner class LocalBinder : Binder() {
        val advertisingState: LiveData<AdvertisingState> =
            this@ScannerAdvertiserService.advertisingState.distinctUntilChanged()
        val scanResults: LiveData<List<ScannedRpi>> =
            this@ScannerAdvertiserService.scanResultsLiveData
    }

    sealed class AdvertisingState {
        data class Started(
            val tekHex: String,
            val tekInterval: Long,
            val rpiHex: String,
            val rpiInterval: Long
        ) : AdvertisingState()

        object Error : AdvertisingState()
    }

    data class ScannedRpi(
        val rpiHex: String,
        val aem: ByteArray,
        val rssi: Int,
        val timestamp: Long,
        /* fields that are set when the tek is known */
        val deviceName: String? = null,
        val tx: Int = 0
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ScannedRpi

            if (rpiHex != other.rpiHex) return false
            if (!aem.contentEquals(other.aem)) return false
            if (rssi != other.rssi) return false
            if (timestamp != other.timestamp) return false
            if (deviceName != other.deviceName) return false
            if (tx != other.tx) return false

            return true
        }

        override fun hashCode(): Int {
            var result = rpiHex.hashCode()
            result = 31 * result + aem.contentHashCode()
            result = 31 * result + rssi
            result = 31 * result + timestamp.hashCode()
            result = 31 * result + (deviceName?.hashCode() ?: 0)
            result = 31 * result + (tx ?: 0)
            return result
        }
    }
}
