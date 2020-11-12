/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.lab.keys

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.view.View
import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.viewModels
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.viewbinding.GroupieViewHolder
import nl.rijksoverheid.en.lab.BaseFragment
import nl.rijksoverheid.en.lab.NotificationsRepository
import nl.rijksoverheid.en.lab.R
import nl.rijksoverheid.en.lab.barcodescanner.BarcodeScanActivity
import nl.rijksoverheid.en.lab.databinding.FragmentKeysBinding
import nl.rijksoverheid.en.lab.keys.items.TestResultSection
import org.json.JSONObject

class KeysFragment : BaseFragment(R.layout.fragment_keys) {

    private val viewModel: KeysViewModel by viewModels()
    private val adapter = GroupAdapter<GroupieViewHolder<*>>()
    private lateinit var section: TestResultSection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        section = TestResultSection(requireContext()).also { adapter.add(it) }
    }

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
            section.testResults = results.reversed()
        }
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

