/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.entoolkit.app.jobs

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import nl.rijksoverheid.entoolkit.Crypto
import nl.rijksoverheid.entoolkit.app.db.TekDatabase
import nl.rijksoverheid.entoolkit.app.db.model.ImportedTek
import nl.rijksoverheid.entoolkit.app.db.model.TekRpi
import okio.ByteString.Companion.toByteString

class ImportTeksJob(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val keyData = inputData.getByteArray("tek") ?: return Result.success()
        val deviceName = inputData.getString("device") ?: "EN device"
        val tek = Crypto.TemporaryExposureKey(keyData, Crypto.createTemporaryExposureKey().interval)
        val dao = TekDatabase.getInstance(applicationContext).getDao()
        dao.clearTeks()
        val rpiKey = Crypto.createRpiKey(tek)
        val rpis = mutableListOf<TekRpi>()
        for (i in tek.interval.toInt() until tek.interval.toInt() + 144) {
            rpis.add(TekRpi(Crypto.createRpi(rpiKey, i.toLong()).toByteString().hex(), i))
        }
        dao.insertTekWithRpis(
            ImportedTek(
                tek.data.toByteString().hex(),
                tek.interval.toInt(),
                deviceName
            ),
            rpis
        )
        return Result.success()
    }

    companion object {
        fun queue(context: Context, tek: ByteArray, deviceName: String = "EN Device") {
            val request = OneTimeWorkRequestBuilder<ImportTeksJob>()
                .setInputData(
                    Data.Builder()
                        .putByteArray("tek", tek)
                        .putString("device", deviceName)
                        .build()
                )
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork("keys", ExistingWorkPolicy.REPLACE, request)
        }
    }
}
