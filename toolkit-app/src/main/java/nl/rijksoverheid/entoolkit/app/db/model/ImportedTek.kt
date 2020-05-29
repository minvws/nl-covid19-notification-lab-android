/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.entoolkit.app.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ImportedTek(
    val tekHex: String,
    val rollingPeriodStart: Int,
    val deviceName: String,
    @PrimaryKey(autoGenerate = true) val id: Long? = null
)
