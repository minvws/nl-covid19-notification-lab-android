/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.lab.keys

import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import nl.rijksoverheid.en.lab.R

class ClearResultsDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext()).apply {
            setTitle(R.string.clear_results_title)
            setMessage(R.string.clear_results_message)
            setNegativeButton(R.string.clear_results_negative, null)
            setPositiveButton(R.string.clear_results_positive) { _, _ ->
                setFragmentResult(KEY_CLEAR_RESULTS, bundleOf(KEY_CLEAR_RESULTS to true))
            }
        }
        return builder.create()
    }

    companion object {
        const val KEY_CLEAR_RESULTS = "clear"
    }
}
