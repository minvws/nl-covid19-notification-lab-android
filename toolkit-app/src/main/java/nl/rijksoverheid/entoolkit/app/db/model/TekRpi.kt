/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.entoolkit.app.db.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity
@ForeignKey(
    entity = ImportedTek::class,
    parentColumns = ["id"],
    childColumns = ["tekId"],
    onDelete = ForeignKey.CASCADE
)
data class TekRpi(@PrimaryKey val rpiHex: String, val interval: Int, val tekId: Long? = null)
