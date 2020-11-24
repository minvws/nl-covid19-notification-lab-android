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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider.getUriForFile
import androidx.fragment.app.viewModels
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.viewbinding.GroupieViewHolder
import nl.rijksoverheid.en.lab.BaseFragment
import nl.rijksoverheid.en.lab.BuildConfig
import nl.rijksoverheid.en.lab.ImportTemporaryExposureKeysResult
import nl.rijksoverheid.en.lab.NotificationsRepository
import nl.rijksoverheid.en.lab.R
import nl.rijksoverheid.en.lab.barcodescanner.BarcodeScanActivity
import nl.rijksoverheid.en.lab.databinding.FragmentKeysBinding
import nl.rijksoverheid.en.lab.keys.items.TestResultSection
import nl.rijksoverheid.en.lab.lifecyle.EventObserver
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
        binding.toolbar.inflateMenu(R.menu.keys)
        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.clear_results -> {
                    ClearResultsDialogFragment().show(childFragmentManager, null)
                }
                R.id.export_results -> {
                    viewModel.exportResults()
                }
            }
            true
        }

        childFragmentManager.setFragmentResultListener(
            ClearResultsDialogFragment.KEY_CLEAR_RESULTS,
            viewLifecycleOwner
        ) { _, result ->
            if (result.getBoolean(ClearResultsDialogFragment.KEY_CLEAR_RESULTS, false)) {
                viewModel.clearResults()
            }
        }

        viewModel.importResult.observe(
            viewLifecycleOwner,
            EventObserver {
                when (it) {
                    ImportTemporaryExposureKeysResult.Success -> {
                        Toast.makeText(
                            requireContext(),
                            R.string.keys_import_tek_success,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    ImportTemporaryExposureKeysResult.PreviousResults -> {
                        PreviousResultDialogFragment().show(childFragmentManager, null)
                    }
                    is ImportTemporaryExposureKeysResult.Error -> {
                        Toast.makeText(
                            requireContext(),
                            R.string.keys_import_tek_failure,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        )

        viewModel.lastResults.observe(viewLifecycleOwner) { results ->
            section.testResults = results.reversed()
        }

        viewModel.exportFile.observe(
            viewLifecycleOwner,
            EventObserver {
                val uri = getUriForFile(requireContext(), "${BuildConfig.APPLICATION_ID}.files", it)
                val builder = ShareCompat.IntentBuilder.from(requireActivity())
                    .setChooserTitle(R.string.export_results).setStream(uri)
                builder.intent.setDataAndType(uri, "text/csv")
                builder.intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                builder.startChooser()
            }
        )
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
