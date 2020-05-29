/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.entoolkit.app.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import nl.rijksoverheid.entoolkit.app.db.model.ImportedTek
import nl.rijksoverheid.entoolkit.app.db.model.TekRpi

@Dao
abstract class TekDao {
    @Query("delete from ImportedTek")
    abstract suspend fun clearTeks()

    @Query("delete from TekRpi")
    abstract suspend fun clearRpis(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    internal abstract suspend fun insertTek(tek: ImportedTek): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertRpis(rpis: List<TekRpi>)

    @Query("select t.* from importedtek t, tekrpi r where r.rpiHex = :rpiHex and t.id = r.tekId limit 1")
    abstract suspend fun getTekForRpi(rpiHex: String): ImportedTek?

    @Transaction
    open suspend fun insertTekWithRpis(importedTek: ImportedTek, rpis: List<TekRpi>) {
        val id = insertTek(importedTek)
        insertRpis(rpis.map { it.copy(tekId = id) })
    }
}
