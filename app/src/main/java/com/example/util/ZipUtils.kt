package com.example.util

import com.example.model.CodeFileView
import com.example.model.ZipArchive
import com.example.model.ZipEntryItem
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipUtils {

    private const val MAX_READ_BYTES = 250_000 // 250KB max preview

    fun parseZip(inputStream: InputStream, archiveName: String): Result<ZipArchive> {
        return runCatching {
            val entries = mutableListOf<ZipEntryItem>()
            var totalSize = 0L

            ZipInputStream(inputStream).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val isDir = entry.isDirectory
                    val size = if (entry.size >= 0) entry.size else 0L
                    val cSize = if (entry.compressedSize >= 0) entry.compressedSize else 0L
                    val name = entry.name.substringAfterLast('/').ifEmpty { entry.name }
                    val ext = if (isDir) "" else name.substringAfterLast('.', "").lowercase()

                    totalSize += size
                    entries.add(
                        ZipEntryItem(
                            path = entry.name,
                            name = name,
                            isDirectory = isDir,
                            size = size,
                            compressedSize = cSize,
                            extension = ext
                        )
                    )
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            // Sort entries: directories first, then alphabetical
            val sorted = entries.sortedWith(
                compareBy<ZipEntryItem> { !it.isDirectory }
                    .thenBy { it.path.lowercase() }
            )

            ZipArchive(
                fileName = archiveName,
                entries = sorted,
                totalSize = totalSize,
                fileCount = entries.count { !it.isDirectory }
            )
        }
    }

    fun readEntryContent(inputStream: InputStream, targetPath: String): Result<CodeFileView> {
        return runCatching {
            ZipInputStream(inputStream).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    if (entry.name == targetPath) {
                        val buffer = ByteArrayOutputStream()
                        val temp = ByteArray(4096)
                        var read: Int
                        var totalRead = 0
                        var truncated = false

                        while (zis.read(temp).also { read = it } != -1) {
                            if (totalRead + read > MAX_READ_BYTES) {
                                val remaining = MAX_READ_BYTES - totalRead
                                if (remaining > 0) {
                                    buffer.write(temp, 0, remaining)
                                    totalRead += remaining
                                }
                                truncated = true
                                break
                            } else {
                                buffer.write(temp, 0, read)
                                totalRead += read
                            }
                        }

                        var text = buffer.toString(StandardCharsets.UTF_8.name())
                        if (truncated) {
                            text += "\n\n// --- Content truncated for display (max 250KB preview limit) ---"
                        }

                        val lines = text.lines().size
                        val name = targetPath.substringAfterLast('/').ifEmpty { targetPath }

                        return@runCatching CodeFileView(
                            entryPath = targetPath,
                            fileName = name,
                            content = text,
                            sizeBytes = entry.size.takeIf { it >= 0 } ?: totalRead.toLong(),
                            linesCount = lines
                        )
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
                throw NoSuchElementException("Entry '$targetPath' not found in ZIP archive.")
            }
        }
    }

    fun createSampleAndroidProjectZip(): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            fun addFile(path: String, content: String) {
                val entry = ZipEntry(path)
                val bytes = content.toByteArray(StandardCharsets.UTF_8)
                entry.size = bytes.size.toLong()
                zos.putNextEntry(entry)
                zos.write(bytes)
                zos.closeEntry()
            }

            addFile(
                "SabKitProject/app/src/main/java/com/example/MainActivity.kt",
                """
                package com.example

                import android.os.Bundle
                import androidx.activity.ComponentActivity
                import androidx.activity.compose.setContent
                import androidx.compose.material3.Text
                import androidx.compose.runtime.Composable

                class MainActivity : ComponentActivity() {
                    override fun onCreate(savedInstanceState: Bundle?) {
                        super.onCreate(savedInstanceState)
                        setContent {
                            Greeting(name = "SabKit Developer")
                        }
                    }
                }

                @Composable
                fun Greeting(name: String) {
                    Text(text = "Hello, ${'$'}name! Built with Kotlin & Jetpack Compose.")
                }
                """.trimIndent()
            )

            addFile(
                "SabKitProject/app/src/main/AndroidManifest.xml",
                """
                <?xml version="1.0" encoding="utf-8"?>
                <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                    <uses-permission android:name="android.permission.INTERNET" />
                    <application
                        android:allowBackup="true"
                        android:icon="@mipmap/ic_launcher"
                        android:label="SabKit Demo"
                        android:supportsRtl="true"
                        android:theme="@style/Theme.SabKit">
                        <activity
                            android:name=".MainActivity"
                            android:exported="true">
                            <intent-filter>
                                <action android:name="android.intent.action.MAIN" />
                                <category android:name="android.intent.category.LAUNCHER" />
                            </intent-filter>
                        </activity>
                    </application>
                </manifest>
                """.trimIndent()
            )

            addFile(
                "SabKitProject/app/build.gradle.kts",
                """
                plugins {
                    alias(libs.plugins.android.application)
                    alias(libs.plugins.kotlin.compose)
                }

                android {
                    namespace = "com.example.sabkit"
                    compileSdk = 35

                    defaultConfig {
                        applicationId = "com.example.sabkit"
                        minSdk = 24
                        targetSdk = 35
                        versionCode = 1
                        versionName = "1.0.0"
                    }
                }

                dependencies {
                    implementation(libs.androidx.compose.material3)
                    implementation(libs.androidx.activity.compose)
                }
                """.trimIndent()
            )

            addFile(
                "SabKitProject/app/src/main/res/values/strings.xml",
                """
                <resources>
                    <string name="app_name">SabKit Demo</string>
                    <string name="welcome_message">Welcome to SabKit Developer Studio!</string>
                </resources>
                """.trimIndent()
            )

            addFile(
                "SabKitProject/README.md",
                """
                # SabKit Sample Android Project
                This archive demonstrates the ZIP inspection and Source Code viewing capabilities of SabKit.
                - Supports Kotlin, XML, Gradle, Markdown, JSON
                - Extracts directly in-memory via java.util.zip
                - Developed by SabKit Team
                """.trimIndent()
            )
        }
        return baos.toByteArray()
    }
}
