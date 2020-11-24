package nl.rijksoverheid.en.lab.keys

import de.siegmar.fastcsv.writer.CsvAppender
import de.siegmar.fastcsv.writer.CsvWriter
import nl.rijksoverheid.en.lab.storage.model.ExposureScanInstance
import nl.rijksoverheid.en.lab.storage.model.ExposureWindow
import nl.rijksoverheid.en.lab.storage.model.TestResult
import java.io.File
import java.nio.charset.StandardCharsets

class ResultsExporter {
    fun export(results: List<TestResult>, file: File) {
        val writer = CsvWriter()
        writer.append(file, StandardCharsets.UTF_8).use { appender ->
            appender.appendLine(
                "Id",
                "Test",
                "Scanning device",
                "Scanned device",
                "Scanned TEK",
                "Timestamp",
                "Exposure window id",
                "Exposure window timestamp",
                "Calibration confidence",
                "Scan instance id",
                "Min attenuation",
                "Typical attenuation",
                "Seconds since last scan"
            )
            for (result in results) {
                if (result.exposureWindows.isEmpty()) {
                    // single line for empty test result
                    writeTestResultFields(result, appender)
                    appender.endLine()
                } else {
                    for (window in result.exposureWindows) {
                        if (window.scanInstances.isEmpty()) {
                            // single line for empty exposure window
                            writeTestResultFields(result, appender)
                            writeExposureWindowFields(window, appender)
                            appender.endLine()
                        } else {
                            for (scanInstance in window.scanInstances) {
                                writeTestResultFields(result, appender)
                                writeExposureWindowFields(window, appender)
                                writeScanInstanceFields(scanInstance, appender)
                                appender.endLine()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun writeScanInstanceFields(
        scanInstance: ExposureScanInstance,
        appender: CsvAppender
    ) {
        appender.appendField(scanInstance.id)
        appender.appendField(scanInstance.minAttenuationDb.toString())
        appender.appendField(scanInstance.typicalAttenuationDb.toString())
        appender.appendField(scanInstance.secondsSinceLastScan.toString())
    }

    private fun writeExposureWindowFields(window: ExposureWindow, appender: CsvAppender) {
        appender.appendField(window.id)
        appender.appendField(window.timestamp.toString())
        appender.appendField(window.calibrationConfidence.toString())
    }

    private fun writeTestResultFields(result: TestResult, appender: CsvAppender) {
        appender.appendField(result.id)
        appender.appendField(result.testId)
        appender.appendField(result.device)
        appender.appendField(result.scannedDeviceId)
        appender.appendField(result.scannedTek)
        appender.appendField(result.timestamp.toString())
    }
}