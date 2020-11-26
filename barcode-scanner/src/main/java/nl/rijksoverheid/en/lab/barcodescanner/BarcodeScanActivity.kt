/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.lab.barcodescanner

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import timber.log.Timber
import java.util.concurrent.Executors

class BarcodeScanActivity : AppCompatActivity() {
    companion object {
        const val RESULT_KEY_SCANNED_QR_CODE = "RESULT_KEY_SCANNED_QR_CODE"
    }

    private lateinit var previewView: PreviewView
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCameraNew()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_barcode)

        previewView = findViewById(R.id.preview)
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        requestPermissions.launch(Manifest.permission.CAMERA)
    }

    private fun startCameraNew() {
        cameraProviderFuture.addListener(
            {
                val cameraProvider = cameraProviderFuture.get()
                startCamera(cameraProvider)
            },
            ContextCompat.getMainExecutor(this)
        )
    }

    private fun startCamera(provider: ProcessCameraProvider) {
        val executor = Executors.newSingleThreadExecutor()
        val preview = Preview.Builder().build()
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        preview.setSurfaceProvider(previewView.surfaceProvider)

        val imageAnalysis =
            ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

        val qrCodeAnalyzer = BarcodeAnalyzer { qrCodes ->
            qrCodes.forEach {
                Timber.d("QR Code detected: ${it.rawValue}.")
            }
            qrCodes.firstOrNull()?.displayValue?.let { value ->
                setResult(Activity.RESULT_OK, Intent().putExtra(RESULT_KEY_SCANNED_QR_CODE, value))
                finish()
            }
        }

        imageAnalysis.setAnalyzer(executor, qrCodeAnalyzer)
        provider.bindToLifecycle(this, cameraSelector, imageAnalysis, preview)
    }
}
