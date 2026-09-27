package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.KeyAlgorithm
import com.example.util.CryptoUtils
import com.example.util.ZipUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("SabKit", appName)
    }

    @Test
    fun `generate keypair and compute sha256`() {
        val result = CryptoUtils.generateKeyPair("test_alias", KeyAlgorithm.RSA_2048)
        assertTrue(result.isSuccess)
        val info = result.getOrNull()
        assertNotNull(info)
        assertEquals("test_alias", info?.alias)
        assertTrue(info?.sha256Hex?.isNotEmpty() == true)
        assertTrue(info?.sha1Hex?.isNotEmpty() == true)
    }

    @Test
    fun `sample zip creates and parses successfully`() {
        val zipBytes = ZipUtils.createSampleAndroidProjectZip()
        assertTrue(zipBytes.isNotEmpty())
        val archiveResult = ZipUtils.parseZip(ByteArrayInputStream(zipBytes), "test.zip")
        assertTrue(archiveResult.isSuccess)
        val archive = archiveResult.getOrNull()
        assertNotNull(archive)
        assertTrue(archive?.fileCount ?: 0 > 0)
    }
}
