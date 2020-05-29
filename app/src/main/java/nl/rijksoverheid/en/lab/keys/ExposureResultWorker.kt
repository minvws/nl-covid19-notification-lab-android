/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.lab.keys

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient
import nl.rijksoverheid.en.lab.NotificationsRepository

class ExposureResultWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val token =
            inputData.getString(ExposureNotificationClient.EXTRA_TOKEN) ?: return Result.success()
        val repository = NotificationsRepository(
            applicationContext,
            Nearby.getExposureNotificationClient(applicationContext)
        )
        repository.storeExposureInformation(token)
        return Result.success()
    }

    companion object {
        fun queue(context: Context, token: String) {
            val request = OneTimeWorkRequestBuilder<ExposureResultWorker>()
            request.setInputData(
                Data.Builder().putString(ExposureNotificationClient.EXTRA_TOKEN, token).build()
            )
            WorkManager.getInstance(context)
                .enqueueUniqueWork("exposure", ExistingWorkPolicy.REPLACE, request.build())
        }
    }
}
