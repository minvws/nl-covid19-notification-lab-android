/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.entoolkit.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.invoke
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.liveData
import androidx.lifecycle.observe
import androidx.lifecycle.switchMap
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Section
import com.xwray.groupie.viewbinding.GroupieViewHolder
import nl.rijksoverheid.entoolkit.Crypto
import nl.rijksoverheid.entoolkit.app.databinding.ActivityMainBinding
import nl.rijksoverheid.entoolkit.app.db.TekDatabase
import nl.rijksoverheid.entoolkit.app.db.model.ImportedTek
import nl.rijksoverheid.entoolkit.app.items.AdvertiserErrorItem
import nl.rijksoverheid.entoolkit.app.items.AdvertiserItem
import nl.rijksoverheid.entoolkit.app.items.HeaderItem
import nl.rijksoverheid.entoolkit.app.items.ScannedRpiItem
import okio.ByteString.Companion.decodeHex
import timber.log.Timber

private const val KNOWN_KEY = "8e06999807e3a1a1a2d88b9a0eb465f7"
private const val KNOWN_INTERVAL = 2649312L

class MainActivity : AppCompatActivity() {

    private var binder: ScannerAdvertiserService.LocalBinder? = null
    private val adapter = GroupAdapter<GroupieViewHolder<*>>()
    private val advertiserSection = Section().apply {
        setHeader(HeaderItem(R.string.header_advertiser))
    }.also { adapter.add(it) }
    private val scanSection =
        Section().apply { setHeader(HeaderItem(R.string.header_scan_results)) }
            .also { adapter.add(it) }

    private val requestLocationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                connectService()
            }
        }

    private val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            binder = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Timber.d("Service connected")
            binder = service as ScannerAdvertiserService.LocalBinder
            startObserving(binder!!)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Timber.forest().isEmpty()) {
            Timber.plant(Timber.DebugTree())
        }
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.list.adapter = adapter
        binding.list.itemAnimator = null
    }

    private fun startObserving(binder: ScannerAdvertiserService.LocalBinder) {
        binder.advertisingState.observe(this) {
            Timber.d("Advertising state: $it")
            when (it) {
                is ScannerAdvertiserService.AdvertisingState.Started -> advertiserSection.update(
                    listOf(AdvertiserItem(it))
                )
                ScannerAdvertiserService.AdvertisingState.Error -> advertiserSection.update(
                    listOf(
                        AdvertiserErrorItem()
                    )
                )
            }
        }
        val dao = TekDatabase.getInstance(this).getDao()
        val rpiCache = mutableMapOf<String, ImportedTek>()

        binder.scanResults.switchMap {
            liveData {
                emit(it.map {
                    val tek = rpiCache[it.rpiHex] ?: dao.getTekForRpi(it.rpiHex)
                    if (tek != null) {
                        rpiCache[it.rpiHex] = tek
                        val aemKey = Crypto.createAssociatedMetadataKey(
                            Crypto.TemporaryExposureKey(
                                tek.tekHex.decodeHex().toByteArray(),
                                tek.rollingPeriodStart.toLong()
                            )
                        )
                        val metadata = Crypto.decryptAssociatedMetadata(
                            it.aem,
                            it.rpiHex.decodeHex().toByteArray(),
                            aemKey
                        )
                        it.copy(deviceName = tek.deviceName, tx = metadata[1].toInt())
                    } else {
                        it
                    }
                })
            }
        }.observe(this) { scanResults ->
            // Timber.d("Scanresults: $scanResults")
            scanSection.update(scanResults.map { ScannedRpiItem(it) })
        }
    }

    private fun connectService() {
        bindService(
            Intent(this, ScannerAdvertiserService::class.java),
            connection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onStart() {
        super.onStart()
        requestLocationPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }

    override fun onStop() {
        try {
            unbindService(connection)
        } catch (ex: IllegalArgumentException) {
            // ignore
        }
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
