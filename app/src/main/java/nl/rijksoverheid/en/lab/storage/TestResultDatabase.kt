/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.lab.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import nl.rijksoverheid.en.lab.storage.model.ExposureScanInstance
import nl.rijksoverheid.en.lab.storage.model.ExposureWindow
import nl.rijksoverheid.en.lab.storage.model.TestResult

@Database(
    entities = [ExposureWindow::class, ExposureScanInstance::class, TestResult::class],
    version = 2,
    exportSchema = false
)
abstract class TestResultDatabase : RoomDatabase() {
    abstract fun getTestResultDao(): TestResultDao

    companion object {
        private var instance: TestResultDatabase? = null
        fun getInstance(context: Context): TestResultDatabase {
            if (instance == null) {
                instance =
                    Room.databaseBuilder(context, TestResultDatabase::class.java, "results.db")
                        .fallbackToDestructiveMigration().build()
            }
            return instance!!
        }
    }
}
