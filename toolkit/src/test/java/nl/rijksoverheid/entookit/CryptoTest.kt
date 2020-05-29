/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.entookit

import com.google.crypto.tink.config.TinkConfig
import nl.rijksoverheid.entoolkit.Crypto
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

private val TEK = Crypto.TemporaryExposureKey(
    byteArrayOf(
        0x75,
        0xc7.toByte(),
        0x34,
        0xc6.toByte(),
        0xdd.toByte(),
        0x1a,
        0x78,
        0x2d,
        0xe7.toByte(),
        0xa9.toByte(),
        0x65,
        0xda.toByte(),
        0x5e,
        0xb9.toByte(),
        0x31,
        0x25
    ), 2642976
)

private val RPI_KEY = byteArrayOf(
    0x18, 0x5a,
    0xd9.toByte(), 0x1d,
    0xb6.toByte(), 0x9e.toByte(),
    0xc7.toByte(), 0xdd.toByte(), 0x04, 0x89.toByte(), 0x60, 0xf1.toByte(),
    0xf3.toByte(), 0xba.toByte(), 0x61,
    0x75
)

class CryptoTest {

    @Before
    fun setup() {
        TinkConfig.register()
    }

    @Test
    fun testTekIntervalAlignsOnRollingPeriod() {
        val tek = Crypto.createTemporaryExposureKey((1585785600 + 600) * 1000L)
        assertEquals(2642976, tek.interval)
    }

    @Test
    fun testGenerateRpiKey() {
        val rpiKey = Crypto.createRpiKey(TEK)

        assertArrayEquals(RPI_KEY, rpiKey)
    }

    @Test
    fun testInterval() {
        assertEquals(2642976, Crypto.getEnIntervalNumber(1585785600 * 1000L))
    }

    @Test
    fun testGenerateRpi() {
        val rpi = Crypto.createRpi(RPI_KEY, Crypto.getEnIntervalNumber(1585785600 * 1000L))
        assertArrayEquals(
            byteArrayOf(
                0x8b.toByte(),
                0xe6.toByte(),
                0xcd.toByte(), 0x37, 0x1c, 0x5c,
                0x89.toByte(), 0x16, 0x04, 0xbf.toByte(), 0xbe.toByte(), 0x49,
                0xdf.toByte(), 0x84.toByte(), 0x50,
                0x96.toByte()
            ), rpi
        )
    }

    @Test
    fun testEncryptMetadata() {
        val rpi = Crypto.createRpi(RPI_KEY, Crypto.getEnIntervalNumber(1585785600 * 1000L))
        val amk = Crypto.createAssociatedMetadataKey(TEK)
        val aem = Crypto.encryptAssociatedMetadata(rpi, amk, 8)

        assertArrayEquals(byteArrayOf(0x72, 0x03, 0x38, 0x74), aem)
    }

    @Test
    fun testDecryptMetadata() {
        val rpi = Crypto.createRpi(RPI_KEY, Crypto.getEnIntervalNumber(1585785600 * 1000L))
        val amk = Crypto.createAssociatedMetadataKey(TEK)
        val aem = Crypto.encryptAssociatedMetadata(rpi, amk, 8)

        val decrypted = Crypto.decryptAssociatedMetadata(aem, rpi, amk)
        assertArrayEquals(byteArrayOf(0x40, 0x8, 0, 0), decrypted)
    }
}
