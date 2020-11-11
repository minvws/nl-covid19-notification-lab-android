/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.lab.keys

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ShareCompat
import androidx.fragment.app.viewModels
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Section
import com.xwray.groupie.viewbinding.GroupieViewHolder
import nl.rijksoverheid.en.lab.BaseFragment
import nl.rijksoverheid.en.lab.NotificationsRepository
import nl.rijksoverheid.en.lab.R
import nl.rijksoverheid.en.lab.barcodescanner.BarcodeScanActivity
import nl.rijksoverheid.en.lab.databinding.FragmentKeysBinding
import nl.rijksoverheid.en.lab.keys.items.TestResultItem
import org.json.JSONObject

private const val RC_SCAN_BARCODE = 1

class KeysFragment : BaseFragment(R.layout.fragment_keys) {

    private val viewModel: KeysViewModel by viewModels()
    private val adapter = GroupAdapter<GroupieViewHolder<*>>()
    private val section = Section().also { adapter.add(it) }

    private val scanBarcode = registerForActivityResult(ScanBarcodeContract()) {
        it?.let {
            importTek(it)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentKeysBinding.bind(view)
        binding.scan.setOnClickListener {
            scanBarcode.launch(null)
        }

        binding.list.adapter = adapter

        viewModel.lastResults.observe(viewLifecycleOwner) { results ->
            section.update(results.reversed().map { TestResultItem(it) })
        }
    }

    private fun shareResults(result: NotificationsRepository.TestResults) {
        val attenuations = result.exposures.map { it.attenuation }.joinToString()
        val durations = result.exposures.map { it.duration }.joinToString()
        val totalRiskScores = result.exposures.map { it.totalRiskScore }.joinToString()
        val transmissionRiskScores = result.exposures.map { it.transmissionRisk }.joinToString()
        val attenuationDurations = result.exposures.map {
            it.attenuationDurations.joinToString(
                prefix = "[",
                postfix = "]"
            )
        }

        val intent = ShareCompat.IntentBuilder.from(requireActivity())
            .setChooserTitle(R.string.keys_share_results)
            .setSubject(
                getString(
                    R.string.share_results_title,
                    viewModel.deviceName,
                    result.testId
                )
            )
            .setText(
                getString(
                    R.string.share_results,
                    result.testId,
                    result.sourceDeviceId,
                    attenuations,
                    durations,
                    totalRiskScores,
                    transmissionRiskScores,
                    viewModel.deviceName,
                    attenuationDurations,
                    result.scannedTek
                )
            )
            .intent

        intent.action = Intent.ACTION_VIEW
        intent.data = Uri.parse("mailto:")
        startActivity(Intent.createChooser(intent, getString(R.string.keys_share_results)))
    }

    private fun importTek(result: String) {
        val json = JSONObject(result)
        val tek = TemporaryExposureKey.TemporaryExposureKeyBuilder().apply {
            setKeyData(Base64.decode(json.getString("keyData"), 0))
            setRollingStartIntervalNumber(json.getString("rollingStartNumber").toInt())
            setRollingPeriod(NotificationsRepository.DEFAULT_ROLLING_PERIOD)
            setTransmissionRiskLevel(1)
        }.build()
        viewModel.importKey(tek, json.getString("deviceId"), json.getString("testId"))
    }

    private class ScanBarcodeContract : ActivityResultContract<Unit, String?>() {
        override fun createIntent(context: Context, input: Unit?): Intent {
            return Intent(context, BarcodeScanActivity::class.java)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): String? {
            return intent?.getStringExtra(BarcodeScanActivity.RESULT_KEY_SCANNED_QR_CODE)
        }
    }
}

