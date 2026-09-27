package com.example.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.example.model.AppInfoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ApkInspectorUtils {

    suspend fun getInstalledApps(
        context: Context,
        includeSystem: Boolean = false,
        searchQuery: String = ""
    ): List<AppInfoItem> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_PERMISSIONS or PackageManager.GET_ACTIVITIES or PackageManager.GET_SERVICES or PackageManager.GET_RECEIVERS or PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_PERMISSIONS or PackageManager.GET_ACTIVITIES or PackageManager.GET_SERVICES or PackageManager.GET_RECEIVERS or PackageManager.GET_SIGNATURES
        }

        val installedPackages = pm.getInstalledPackages(flags)
        val result = mutableListOf<AppInfoItem>()

        for (pkg in installedPackages) {
            val appInfo = pkg.applicationInfo ?: continue
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            if (!includeSystem && isSystem) {
                // If excluding system apps, still keep our own app!
                if (pkg.packageName != context.packageName) continue
            }

            val appName = try {
                appInfo.loadLabel(pm).toString()
            } catch (_: Exception) {
                pkg.packageName
            }

            if (searchQuery.isNotBlank()) {
                val matches = appName.contains(searchQuery, ignoreCase = true) ||
                        pkg.packageName.contains(searchQuery, ignoreCase = true)
                if (!matches) continue
            }

            val permissions = pkg.requestedPermissions?.toList() ?: emptyList()
            val activities = pkg.activities?.map { it.name } ?: emptyList()
            val services = pkg.services?.map { it.name } ?: emptyList()
            val receivers = pkg.receivers?.map { it.name } ?: emptyList()
            val signatures = extractSignatures(pkg)

            val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkg.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pkg.versionCode.toLong()
            }

            val minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                appInfo.minSdkVersion
            } else {
                24
            }

            val icon = try {
                appInfo.loadIcon(pm)
            } catch (_: Exception) {
                null
            }

            result.add(
                AppInfoItem(
                    packageName = pkg.packageName,
                    appName = appName,
                    versionName = pkg.versionName ?: "1.0",
                    versionCode = vCode,
                    minSdk = minSdk,
                    targetSdk = appInfo.targetSdkVersion,
                    isSystemApp = isSystem,
                    permissions = permissions,
                    activities = activities,
                    services = services,
                    receivers = receivers,
                    signatures = signatures,
                    icon = icon
                )
            )
        }

        result.sortedBy { it.appName.lowercase() }
    }

    suspend fun inspectApkUri(context: Context, uri: Uri): Result<AppInfoItem> =
        withContext(Dispatchers.IO) {
            runCatching {
                // Copy stream to cache file to parse via PackageManager
                val cacheFile = File(context.cacheDir, "inspect_${System.currentTimeMillis()}.apk")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(cacheFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: throw IllegalArgumentException("Could not open APK content stream")

                val pm = context.packageManager
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    PackageManager.GET_PERMISSIONS or PackageManager.GET_ACTIVITIES or PackageManager.GET_SERVICES or PackageManager.GET_RECEIVERS or PackageManager.GET_SIGNING_CERTIFICATES
                } else {
                    @Suppress("DEPRECATION")
                    PackageManager.GET_PERMISSIONS or PackageManager.GET_ACTIVITIES or PackageManager.GET_SERVICES or PackageManager.GET_RECEIVERS or PackageManager.GET_SIGNATURES
                }

                val pkgInfo = pm.getPackageArchiveInfo(cacheFile.absolutePath, flags)
                    ?: throw IllegalArgumentException("Failed to parse APK archive metadata. Verify it is a valid APK file.")

                val appInfo = pkgInfo.applicationInfo
                if (appInfo != null) {
                    appInfo.sourceDir = cacheFile.absolutePath
                    appInfo.publicSourceDir = cacheFile.absolutePath
                }

                val appName = if (appInfo != null) {
                    try {
                        pm.getApplicationLabel(appInfo).toString().ifEmpty { pkgInfo.packageName }
                    } catch (_: Exception) {
                        pkgInfo.packageName
                    }
                } else {
                    pkgInfo.packageName
                }

                val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pkgInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    pkgInfo.versionCode.toLong()
                }

                val minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && appInfo != null) {
                    appInfo.minSdkVersion
                } else 24

                val icon = if (appInfo != null) {
                    runCatching { pm.getApplicationIcon(appInfo) }.getOrNull()
                } else null

                val signatures = extractSignatures(pkgInfo)

                AppInfoItem(
                    packageName = pkgInfo.packageName,
                    appName = appName,
                    versionName = pkgInfo.versionName ?: "1.0",
                    versionCode = vCode,
                    minSdk = minSdk,
                    targetSdk = appInfo?.targetSdkVersion ?: 0,
                    isSystemApp = false,
                    permissions = pkgInfo.requestedPermissions?.toList() ?: emptyList(),
                    activities = pkgInfo.activities?.map { it.name } ?: emptyList(),
                    services = pkgInfo.services?.map { it.name } ?: emptyList(),
                    receivers = pkgInfo.receivers?.map { it.name } ?: emptyList(),
                    signatures = signatures,
                    icon = icon
                )
            }
        }

    private fun extractSignatures(pkg: PackageInfo): List<String> {
        val signaturesBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = pkg.signingInfo
            if (signingInfo != null) {
                if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners.map { it.toByteArray() }
                } else {
                    signingInfo.signingCertificateHistory.map { it.toByteArray() }
                }
            } else emptyList()
        } else {
            @Suppress("DEPRECATION")
            pkg.signatures?.map { it.toByteArray() } ?: emptyList()
        }

        return signaturesBytes.map { bytes ->
            val sha256 = CryptoUtils.computeHash(bytes, "SHA-256")
            val sha1 = CryptoUtils.computeHash(bytes, "SHA-1")
            "SHA-256: $sha256\nSHA-1: $sha1"
        }
    }
}
