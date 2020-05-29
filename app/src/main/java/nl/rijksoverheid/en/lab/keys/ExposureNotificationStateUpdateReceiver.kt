/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.lab.keys

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient
import timber.log.Timber

class ExposureNotificationStateUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("EXPOSURE ${intent.getStringExtra(ExposureNotificationClient.EXTRA_TOKEN)}")
        ExposureResultWorker.queue(
            context,
            intent.getStringExtra(ExposureNotificationClient.EXTRA_TOKEN)!!
        )
    }
}
