/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.entoolkit.app.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import nl.rijksoverheid.entoolkit.app.db.model.ImportedTek
import nl.rijksoverheid.entoolkit.app.db.model.TekRpi

@Database(entities = [TekRpi::class, ImportedTek::class], version = 2, exportSchema = false)
abstract class TekDatabase : RoomDatabase() {
    abstract fun getDao(): TekDao

    companion object {
        private var instance: TekDatabase? = null

        fun getInstance(context: Context): TekDatabase {
            return instance ?: Room.databaseBuilder(context, TekDatabase::class.java, "tek_db")
                .fallbackToDestructiveMigration().build().also { instance = it }
        }
    }
}
