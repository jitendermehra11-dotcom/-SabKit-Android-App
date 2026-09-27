package com.example

import com.example.model.KeyAlgorithm
import com.example.util.CryptoUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4L, 2L + 2L)
    }

    @Test
    fun testAppName() {
        val appName = "SabKit"
        assertEquals("SabKit", appName)
        assertNotNull(appName)
    }

    @Test
    fun testSha256Hashing() {
        val sampleData = "SabKit".toByteArray(Charsets.UTF_8)
        val hash = CryptoUtils.computeHash(sampleData, "SHA-256")
        assertNotNull(hash)
        assertTrue(hash.isNotEmpty())
    }

    @Test
    fun testKeyAlgorithmEnum() {
        assertEquals("RSA", KeyAlgorithm.RSA_2048.standardName)
        assertEquals(2048, KeyAlgorithm.RSA_2048.defaultSize)
        assertEquals("EC", KeyAlgorithm.ECDSA_256.standardName)
    }

    @Test
    fun testDefaultSpeedTestUrlIsHttpbin() {
        assertEquals("https://httpbin.org/bytes/1024", com.example.util.NetworkTimeUtils.DEFAULT_SPEED_TEST_URL)
    }
}
