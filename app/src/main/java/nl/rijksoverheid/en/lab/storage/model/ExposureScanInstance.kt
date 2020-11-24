/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.lab.storage.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.google.android.gms.nearby.exposurenotification.ScanInstance
import java.util.UUID

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = ExposureWindow::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("exposureWindowId"),
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ExposureScanInstance(
    val minAttenuationDb: Int,
    val typicalAttenuationDb: Int,
    val secondsSinceLastScan: Int,
    @ColumnInfo(index = true) val exposureWindowId: String,
    @PrimaryKey val id: String
) {
    companion object {
        fun fromScanInstance(windowId: String, scanInstance: ScanInstance): ExposureScanInstance {
            return ExposureScanInstance(
                scanInstance.minAttenuationDb,
                scanInstance.typicalAttenuationDb,
                scanInstance.secondsSinceLastScan,
                windowId,
                UUID.randomUUID().toString()
            )
        }
    }
}
