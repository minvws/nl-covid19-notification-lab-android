/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.lab

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient
import nl.rijksoverheid.en.lab.keys.KeysViewModel
import nl.rijksoverheid.en.lab.status.NotificationsStatusViewModel
import nl.rijksoverheid.en.lab.storage.TestResultDatabase

class LabViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    private val repository: NotificationsRepository by lazy {
        NotificationsRepository(
            context,
            createExposureNotificationClient(context),
            TestResultDatabase.getInstance(context)
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return when (modelClass) {
            NotificationsStatusViewModel::class.java -> NotificationsStatusViewModel(
                repository,
                getDevicePreferences(context)
            ) as T
            KeysViewModel::class.java -> KeysViewModel(
                repository,
                getDevicePreferences(context).getString("device_name", Build.MODEL)!!
            ) as T
            else -> throw IllegalStateException("Unknown view model class $modelClass")
        }
    }

    @SuppressLint("InlinedApi")
    private fun getDevicePreferences(context: Context): SharedPreferences {
        val prefs = context.getSharedPreferences("device", 0)
        if (!prefs.contains("device_name")) {
            prefs.edit {
                putString(
                    "device_name",
                    Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
                        ?: Build.MODEL
                )
            }
        }
        return prefs
    }

    private fun createExposureNotificationClient(context: Context): ExposureNotificationClient =
        Nearby.getExposureNotificationClient(context)
}
