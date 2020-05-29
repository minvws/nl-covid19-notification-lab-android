/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.lab.keys

import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import com.google.protobuf.ByteString
import nl.rijksoverheid.en.lab.proto.SignatureInfo
import nl.rijksoverheid.en.lab.proto.TEKSignature
import nl.rijksoverheid.en.lab.proto.TEKSignatureList
import nl.rijksoverheid.en.lab.proto.TemporaryExposureKeyExport
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private const val EXPORT_FILENAME = "export.bin"
private const val SIG_FILENAME = "export.sig"

private const val HEADER_V1 = "EK Export v1"
private const val HEADER_LEN = 16

class KeyFileWriter(private val signer: KeyFileSigner) {

    fun exportKey(temporaryExposureKey: TemporaryExposureKey, file: File) {
        val proto = TemporaryExposureKeyExport.newBuilder()
            .addKeys(temporaryExposureKey.toProto())
            .addSignatureInfos(signatureInfo())
            .setStartTimestamp(System.currentTimeMillis() / 1000)
            .setEndTimestamp(System.currentTimeMillis() / 1000)
            .setRegion("NL")
            .setBatchSize(1)
            .setBatchNum(1)
            .build()

        val output = ByteArrayOutputStream()
        val zip = ZipOutputStream(output)
        zip.putNextEntry(ZipEntry(EXPORT_FILENAME))
        zip.write(
            HEADER_V1.padEnd(
                HEADER_LEN, ' '
            ).toByteArray()
        )
        zip.write(proto.toByteArray())
        zip.closeEntry()
        zip.putNextEntry(ZipEntry(SIG_FILENAME))
        zip.write(sign(proto.toByteArray(), 1, 1).toByteArray())
        zip.closeEntry()
        zip.flush()
        zip.close()
        file.outputStream().use {
            it.write(output.toByteArray())
        }
    }

    private fun sign(
        exportBytes: ByteArray,
        batchSize: Int,
        batchNum: Int
    ): TEKSignatureList {
        // In tests the signer is null because Robolectric doesn't support the crypto constructs we use.
        val signature = ByteString.copyFrom(
            signer.sign(exportBytes)
        )
        return TEKSignatureList.newBuilder()
            .addSignatures(
                TEKSignature.newBuilder()
                    .setSignatureInfo(signatureInfo())
                    .setBatchNum(batchNum)
                    .setBatchSize(batchSize)
                    .setSignature(signature)
            )
            .build()
    }

    private fun signatureInfo(): SignatureInfo {
        return signer.signatureInfo()
    }
}

private fun TemporaryExposureKey.toProto(): nl.rijksoverheid.en.lab.proto.TemporaryExposureKey {
    return nl.rijksoverheid.en.lab.proto.TemporaryExposureKey.newBuilder()
        .setKeyData(ByteString.copyFrom(keyData))
        .setRollingPeriod(rollingPeriod)
        .setRollingStartIntervalNumber(rollingStartIntervalNumber)
        .setTransmissionRiskLevel(1)
        .build()
}
