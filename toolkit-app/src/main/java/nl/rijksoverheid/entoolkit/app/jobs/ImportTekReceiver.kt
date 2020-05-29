/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.entoolkit.app.jobs

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Base64
import nl.rijksoverheid.entoolkit.app.BuildConfig
import okio.ByteString.Companion.decodeHex
import timber.log.Timber

private const val ACTION_SET_TEK = "${BuildConfig.APPLICATION_ID}.ACTION_SET_TEK"

class ImportTekReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_SET_TEK) {
            val base64 = intent.getStringExtra("base64")
            val hex = intent.getStringExtra("hex")
            val device = intent.getStringExtra("device") ?: "EN device"
            if (base64 == null && hex == null) {
                Timber.e("$ACTION_SET_TEK did not have a base64 or hex extra")
                return
            }
            if (base64 != null) {
                try {
                    val key = Base64.decode(base64, 0)
                    ImportTeksJob.queue(context, key, device)
                } catch (ex: IllegalArgumentException) {
                    Timber.e("Could not decode key bytes")
                    return
                }
            } else if (hex != null) {
                ImportTeksJob.queue(context, hex.decodeHex().toByteArray(), device)
            }
        }
    }
}
