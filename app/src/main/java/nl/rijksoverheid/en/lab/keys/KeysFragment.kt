/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.lab.keys

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import androidx.core.app.ShareCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import nl.rijksoverheid.en.lab.BaseFragment
import nl.rijksoverheid.en.lab.NotificationsRepository
import nl.rijksoverheid.en.lab.R
import nl.rijksoverheid.en.lab.barcodescanner.BarcodeScanActivity
import nl.rijksoverheid.en.lab.databinding.FragmentKeysBinding
import org.json.JSONObject

private const val RC_SCAN_BARCODE = 1

class KeysFragment : BaseFragment(R.layout.fragment_keys) {

    private val viewModel: KeysViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentKeysBinding.bind(view)

        viewModel.lastResults.observe(viewLifecycleOwner) { result ->
            binding.shareResults.isEnabled = true
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

            binding.testId.text = getString(R.string.keys_test_id, result.testId)
            binding.deviceId.text = getString(R.string.keys_device_id, result.sourceDeviceId)
            binding.tekBase64.text = getString(R.string.tek_base64, result.scannedTek)

            binding.attenuations.text = getString(R.string.attenuations, attenuations)
            binding.attenuationDurations.text =
                getString(R.string.attenuation_durations, attenuationDurations)
            binding.durations.text = getString(R.string.durations, durations)
            binding.totalRiskScore.text = getString(R.string.riskScore, totalRiskScores)
            binding.transmissionRiskScore.text =
                getString(R.string.transmissionRisk, transmissionRiskScores)
        }

        viewModel.scanEnabled.observe(viewLifecycleOwner) {
            binding.scanTekQr.isEnabled = it
        }

        binding.scanTekQr.setOnClickListener {
            startActivityForResult(
                Intent(requireContext(), BarcodeScanActivity::class.java),
                RC_SCAN_BARCODE
            )
        }

        binding.shareResults.setOnClickListener {
            viewModel.lastResults.value?.let { shareResults(it) }
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SCAN_BARCODE && resultCode == Activity.RESULT_OK) {
            val json =
                JSONObject(data!!.getStringExtra(BarcodeScanActivity.RESULT_KEY_SCANNED_QR_CODE)!!)
            val tek = TemporaryExposureKey.TemporaryExposureKeyBuilder().apply {
                setKeyData(Base64.decode(json.getString("keyData"), 0))
                setRollingStartIntervalNumber(json.getString("rollingStartNumber").toInt())
                setRollingPeriod(NotificationsRepository.DEFAULT_ROLLING_PERIOD)
                setTransmissionRiskLevel(1)
            }.build()
            viewModel.importKey(tek, json.getString("deviceId"), json.getString("testId"))
        }
    }
}
