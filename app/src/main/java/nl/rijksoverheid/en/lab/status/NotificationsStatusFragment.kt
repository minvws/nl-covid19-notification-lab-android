/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.lab.status

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import nl.rijksoverheid.en.lab.BaseFragment
import nl.rijksoverheid.en.lab.R
import nl.rijksoverheid.en.lab.databinding.FragmentStatusBinding
import nl.rijksoverheid.en.lab.lifecyle.EventObserver
import timber.log.Timber

private const val RC_REQUEST_PERMISSION = 1
private const val RC_REQUEST_SHARE_KEYS = 2

class NotificationsStatusFragment : BaseFragment(R.layout.fragment_status) {

    private val viewModel: NotificationsStatusViewModel by viewModels()
    private lateinit var binding: FragmentStatusBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentStatusBinding.bind(view)

        viewModel.notificationState.observe(viewLifecycleOwner) {
            when (it) {
                NotificationsStatusViewModel.NotificationsState.Enabled -> {
                    Timber.d("Enabled")
                    binding.enableExposureNotification.isChecked = true
                    binding.shareTek.isEnabled = true
                }
                NotificationsStatusViewModel.NotificationsState.Disabled -> {
                    Timber.d("Disabled")
                    binding.enableExposureNotification.isChecked = false
                    binding.shareTek.isEnabled = false
                }
                NotificationsStatusViewModel.NotificationsState.Unavailable -> {
                    Timber.d("Unavailable")
                    binding.enableExposureNotification.isChecked = false
                    binding.shareTek.isEnabled = false
                }
            }
        }

        viewModel.notificationsResult.observe(viewLifecycleOwner, EventObserver {
            when (it) {
                is NotificationsStatusViewModel.NotificationsStatusResult.ConsentRequired -> {
                    try {
                        requireActivity().startIntentSenderFromFragment(
                            this,
                            it.intent.intentSender,
                            RC_REQUEST_PERMISSION,
                            null,
                            0,
                            0,
                            0,
                            null
                        )
                    } catch (ex: IntentSender.SendIntentException) {
                        Timber.e(ex, "Error requesting consent")
                    }
                }
                is NotificationsStatusViewModel.NotificationsStatusResult.UnknownError -> Timber.e(
                    it.exception,
                    "Unexpected error has occurred"
                )
            }
        })

        viewModel.shareTekResult.observe(viewLifecycleOwner, EventObserver {
            when (it) {
                is NotificationsStatusViewModel.ShareTekResult.Success -> binding.tekQrCode.setImageBitmap(
                    it.qrCode
                )
                is NotificationsStatusViewModel.ShareTekResult.RequestConsent -> {
                    startIntentSenderForResult(
                        it.resolution.intentSender,
                        RC_REQUEST_SHARE_KEYS,
                        null,
                        0,
                        0,
                        0,
                        null
                    )
                }
                NotificationsStatusViewModel.ShareTekResult.Error ->
                    Toast.makeText(
                        requireContext(),
                        R.string.share_tek_generic_error,
                        Toast.LENGTH_LONG
                    ).show()

                NotificationsStatusViewModel.ShareTekResult.NoKeys -> Toast.makeText(
                    requireContext(),
                    R.string.share_tek_no_key_error,
                    Toast.LENGTH_LONG
                ).show()
            }
        })

        binding.enableExposureNotification.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.requestEnableNotifications() else viewModel.requestDisableNotifications()
        }

        binding.shareTek.setOnClickListener {
            shareTek()
        }
    }

    private fun shareTek() {
        val size = resources.getDimensionPixelSize(R.dimen.qr_code)
        viewModel.shareTek(size)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_REQUEST_PERMISSION && resultCode == Activity.RESULT_OK) {
            viewModel.requestEnableNotifications()
            viewModel.notificationsStarted()
        } else if (requestCode == RC_REQUEST_PERMISSION) {
            viewModel.notificationsNotStarted()
        } else if (requestCode == RC_REQUEST_SHARE_KEYS && resultCode == Activity.RESULT_OK) {
            shareTek()
        }
    }
}
