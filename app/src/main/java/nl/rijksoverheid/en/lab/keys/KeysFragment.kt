/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.lab.keys

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.view.View
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

        viewModel.lastResults.observe(viewLifecycleOwner) { exposures ->
            val attenuations = exposures.map { it.attenuation }.joinToString()
            val durations = exposures.map { it.duration }.joinToString()
            val totalRiskScores = exposures.map { it.totalRiskScore }.joinToString()
            val transmissionRiskScores = exposures.map { it.transmissionRisk }.joinToString()
            binding.attenuations.text = getString(R.string.attenuations, attenuations)
            binding.durations.text = getString(R.string.durations, durations)
            binding.totalRiskScore.text = getString(R.string.riskScore, totalRiskScores)
            binding.transmissionRiskScore.text =
                getString(R.string.transmissionRisk, transmissionRiskScores)
        }

        binding.scanTekQr.setOnClickListener {
            startActivityForResult(
                Intent(requireContext(), BarcodeScanActivity::class.java),
                RC_SCAN_BARCODE
            )
        }
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
            viewModel.importKey(tek)
        }
    }
}
