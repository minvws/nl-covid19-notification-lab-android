package nl.rijksoverheid.en.lab.storage.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import java.util.UUID

@Entity(
    foreignKeys = [ForeignKey(
        entity = TestResult::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("testId"),
        onDelete = ForeignKey.CASCADE
    )]
)

data class ExposureWindow @JvmOverloads constructor(
    val timestamp: Long,
    val calibrationConfidence: Int,
    @Ignore val scanInstances: List<ExposureScanInstance> = emptyList(),
    @ColumnInfo(index = true) val testId: String,
    @PrimaryKey val id: String
) {
    companion object {
        fun fromExposureWindow(
            testId: String,
            window: ExposureWindow
        ): nl.rijksoverheid.en.lab.storage.model.ExposureWindow {
            val id = UUID.randomUUID().toString()
            val scanInstances = window.scanInstances.map {
                ExposureScanInstance.fromScanInstance(id, it)
            }
            return ExposureWindow(
                window.dateMillisSinceEpoch,
                window.calibrationConfidence,
                scanInstances,
                testId,
                id
            )
        }
    }
}