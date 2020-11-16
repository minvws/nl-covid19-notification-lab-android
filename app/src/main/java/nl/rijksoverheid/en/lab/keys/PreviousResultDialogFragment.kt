package nl.rijksoverheid.en.lab.keys

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import nl.rijksoverheid.en.lab.R

class PreviousResultDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext()).apply {
            setTitle(R.string.keys_previous_result_title)
            setMessage(R.string.keys_previous_result_message)
            setPositiveButton(R.string.keys_previous_result_positive) { _, _ ->
                startActivity(Intent(ExposureNotificationClient.ACTION_EXPOSURE_NOTIFICATION_SETTINGS))
            }
        }
        return builder.create()
    }
}