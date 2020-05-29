/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.lab

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient
import nl.rijksoverheid.en.lab.keys.KeysViewModel
import nl.rijksoverheid.en.lab.status.NotificationsStatusViewModel

class LabViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val repository: NotificationsRepository by lazy {
        NotificationsRepository(
            context,
            createExposureNotificationClient(context)
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return when (modelClass) {
            NotificationsStatusViewModel::class.java -> NotificationsStatusViewModel(
                repository
            ) as T
            KeysViewModel::class.java -> KeysViewModel(
                repository
            ) as T
            else -> throw IllegalStateException("Unknown view model class $modelClass")
        }
    }

    private fun createExposureNotificationClient(context: Context): ExposureNotificationClient =
        Nearby.getExposureNotificationClient(context)
}
