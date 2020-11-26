/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.lab.barcodescanner

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import timber.log.Timber

class BarcodeAnalyzer(
    private val onBarcodesDetected: (qrCodes: List<Barcode>) -> Unit
) : ImageAnalysis.Analyzer {
    @SuppressLint("UnsafeExperimentalUsageError")
    override fun analyze(image: ImageProxy) {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()

        val scanner = BarcodeScanning.getClient(options)
        val inputImage = InputImage.fromMediaImage(image.image!!, image.imageInfo.rotationDegrees)

        val task = scanner.process(inputImage)
        try {
            val result = Tasks.await(task)
            if (result?.isNotEmpty() == true) {
                onBarcodesDetected(result)
            }
        } catch (ex: Exception) {
            Timber.e(ex)
        } finally {
            image.close()
        }
    }
}
