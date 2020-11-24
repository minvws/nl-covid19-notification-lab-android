package nl.rijksoverheid.en.lab.storage.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity
data class TestResult @JvmOverloads constructor(
    @PrimaryKey val id: String,
    val device: String,
    val scannedTek: String,
    val scannedDeviceId: String,
    val testId: String,
    val timestamp: Long,
    @Ignore val exposureWindows: List<ExposureWindow> = emptyList()
)