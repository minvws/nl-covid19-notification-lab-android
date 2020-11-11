/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.lab.keys

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.gms.nearby.Nearby
import nl.rijksoverheid.en.lab.NotificationsRepository
import nl.rijksoverheid.en.lab.storage.TestResultDatabase

class ExposureResultWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val repository = NotificationsRepository(
            applicationContext,
            Nearby.getExposureNotificationClient(applicationContext),
            TestResultDatabase.getInstance(applicationContext)
        )
        repository.storeExposureInformation()
        return Result.success()
    }

    companion object {
        fun queue(context: Context) {
            val request = OneTimeWorkRequestBuilder<ExposureResultWorker>()
            WorkManager.getInstance(context)
                .enqueueUniqueWork("exposure", ExistingWorkPolicy.REPLACE, request.build())
        }
    }
}
