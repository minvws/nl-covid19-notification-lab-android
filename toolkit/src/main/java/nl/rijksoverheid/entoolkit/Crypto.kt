/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.entoolkit

import android.annotation.SuppressLint
import com.google.crypto.tink.subtle.Hkdf
import okio.Buffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object Crypto {
    private const val EN_ROLLING_PERIOD = 144

    /**
     * Get the interval number for the given timestamp in millis
     * @param timestampMs the epoch timestamp in millis, defaults to [System.currentTimeMillis]
     * @return the interval number
     */
    fun getEnIntervalNumber(timestampMs: Long = System.currentTimeMillis()): Long {
        return (timestampMs / 1000) / (60 * 10)
    }

    /**
     * Create new key. The interval for this key will be based on the creation timestamp
     * @param timestampMs creation time for this key in millis, defaults to [System.currentTimeMillis]
     * @return the [TemporaryExposureKey]
     */
    fun createTemporaryExposureKey(timestampMs: Long = System.currentTimeMillis()): TemporaryExposureKey {
        val keyBytes = ByteArray(16)
        SecureRandom().nextBytes(keyBytes)
        return TemporaryExposureKey(
            keyBytes,
            (getEnIntervalNumber(timestampMs) / EN_ROLLING_PERIOD) * EN_ROLLING_PERIOD
        )
    }

    /**
     * Create a rolling proximity identifies for the given interval
     * @param rpiKey the rolling proximity key
     * @param interval the interval for the identifiers
     * @return the rpi as 16 bytes in size
     */
    fun createRpi(rpiKey: ByteArray, interval: Long): ByteArray {
        val buffer = Buffer()
        buffer.write("EN-RPI".toByteArray())
        buffer.write(ByteArray(6))
        buffer.writeIntLe((interval and 0xffffffff).toInt())

        @SuppressLint("GetInstance")
        val cipher: Cipher = Cipher.getInstance("AES/ECB/NoPadding")
        val secretKey = SecretKeySpec(rpiKey, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val data = buffer.readByteArray()
        return cipher.doFinal(data)
    }

    /**
     * Create a rolling proximity identifier key
     * @param tek the [TemporaryExposureKey]
     * @return the rpi key, 16 bytes in size
     */
    fun createRpiKey(tek: TemporaryExposureKey): ByteArray {
        return Hkdf.computeHkdf("HMACSHA256", tek.data, null, "EN-RPIK".toByteArray(), 16)
    }

    /**
     * Create the associated metadata key
     * @param tek the [TemporaryExposureKey]
     * @return the amk, 16 bytes in size
     */
    fun createAssociatedMetadataKey(tek: TemporaryExposureKey): ByteArray {
        return Hkdf.computeHkdf("HMACSHA256", tek.data, null, "EN-AEMK".toByteArray(), 16)
    }

    /**
     * Create the associated meta data, holding the version and tx power
     * @param rpi the rpi for associated with the metadata
     * @param amk the associated metadata key
     * @param tx the tx power for the meta data, in the range of -127 - 127
     * @return the metadata block to be used in the bluetooth service data
     */
    fun encryptAssociatedMetadata(rpi: ByteArray, amk: ByteArray, tx: Int): ByteArray {
        val buffer = Buffer()
        buffer.writeByte(0x40)
        buffer.writeByte(tx and 0xff)
        buffer.writeByte(0)
        buffer.writeByte(0)

        val cipher: Cipher = Cipher.getInstance("AES/CTR/NoPadding")
        val secretKey = SecretKeySpec(amk, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(rpi))
        return cipher.doFinal(buffer.readByteArray())
    }

    /**
     * Decrypt the encrypted associated metadata block. Does not check for validity of the
     * decrypted bytes.
     *
     * @param data the data, must be 4 bytes
     * @param rpi the RPI associated with the metadata, 16 bytes
     * @param amk the associated meta data key
     * @return the decrypted metadata block
     */
    fun decryptAssociatedMetadata(data: ByteArray, rpi: ByteArray, amk: ByteArray): ByteArray {
        val cipher: Cipher = Cipher.getInstance("AES/CTR/NoPadding")
        val secretKey = SecretKeySpec(amk, "AES")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(rpi))
        return cipher.doFinal(data)
    }

    class TemporaryExposureKey(val data: ByteArray, val interval: Long)
}
