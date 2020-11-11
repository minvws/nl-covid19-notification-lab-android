package nl.rijksoverheid.en.lab.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import nl.rijksoverheid.en.lab.storage.model.ExposureScanInstance
import nl.rijksoverheid.en.lab.storage.model.ExposureWindow
import nl.rijksoverheid.en.lab.storage.model.TestResult

@Dao
abstract class TestResultDao {
    @Query("select * from TestResult order by timestamp")
    abstract fun getTestResults(): Flow<List<TestResult>>

    @Query("select * from ExposureWindow where testId = :testId")
    abstract suspend fun getExposureWindows(testId: String): List<ExposureWindow>

    @Query("select * from ExposureScanInstance where exposureWindowId = :windowId")
    abstract suspend fun getScanInstances(windowId: String): List<ExposureScanInstance>

    @Query("delete from TestResult")
    abstract suspend fun removeTestResults()

    @Insert(entity = TestResult::class, onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertTestResult(testResult: TestResult)

    @Insert(entity = ExposureWindow::class, onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertExposureWindows(windows: List<ExposureWindow>)

    @Insert(entity = ExposureScanInstance::class, onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertScanInstances(windows: List<ExposureScanInstance>)

    @Transaction
    open suspend fun storeTestResult(testResult: TestResult) {
        insertTestResult(testResult)
        if (testResult.exposureWindows.isNotEmpty()) {
            insertExposureWindows(testResult.exposureWindows)
            val scanInstances = testResult.exposureWindows.map { it.scanInstances }.flatten()
            if (scanInstances.isNotEmpty()) {
                insertScanInstances(scanInstances)
            }
        }
    }
}