package com.example.util

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import com.example.model.AppDebugKeyInfo
import com.example.model.GeneratedKeyInfo
import com.example.model.KeyAlgorithm
import java.io.ByteArrayInputStream
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.ECGenParameterSpec

object CryptoUtils {

    fun generateKeyPair(
        alias: String,
        algorithm: KeyAlgorithm
    ): Result<GeneratedKeyInfo> {
        return runCatching {
            val keyPair = when (algorithm) {
                KeyAlgorithm.RSA_2048 -> {
                    val kpg = KeyPairGenerator.getInstance("RSA")
                    kpg.initialize(2048)
                    kpg.generateKeyPair()
                }
                KeyAlgorithm.RSA_4096 -> {
                    val kpg = KeyPairGenerator.getInstance("RSA")
                    kpg.initialize(4096)
                    kpg.generateKeyPair()
                }
                KeyAlgorithm.ECDSA_256 -> {
                    val kpg = KeyPairGenerator.getInstance("EC")
                    kpg.initialize(ECGenParameterSpec("secp256r1"))
                    kpg.generateKeyPair()
                }
            }

            val encodedPublicKey = keyPair.public.encoded
            val sha1 = computeHash(encodedPublicKey, "SHA-1")
            val sha256 = computeHash(encodedPublicKey, "SHA-256")
            val md5 = computeHash(encodedPublicKey, "MD5")
            val pem = toPem(encodedPublicKey)

            GeneratedKeyInfo(
                alias = alias.ifBlank { "key_${System.currentTimeMillis()}" },
                algorithm = algorithm.title,
                keySize = algorithm.defaultSize,
                createdAt = System.currentTimeMillis(),
                sha1Hex = sha1,
                sha256Hex = sha256,
                md5Hex = md5,
                publicKeyPem = pem,
                isHardwareBacked = false
            )
        }
    }

    fun computeHash(bytes: ByteArray, algorithm: String): String {
        val digest = MessageDigest.getInstance(algorithm)
        val hashBytes = digest.digest(bytes)
        return hashBytes.joinToString(":") { "%02X".format(it) }
    }

    fun toPem(encodedPublicKey: ByteArray): String {
        val base64 = Base64.encodeToString(encodedPublicKey, Base64.NO_WRAP)
        val chunks = base64.chunked(64).joinToString("\n")
        return "-----BEGIN PUBLIC KEY-----\n$chunks\n-----END PUBLIC KEY-----"
    }

    fun getAppSignatureInfo(context: Context): AppDebugKeyInfo {
        return try {
            val packageName = context.packageName
            val packageManager = context.packageManager
            val signaturesBytes: List<ByteArray> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val packageInfo = packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
                val signingInfo = packageInfo.signingInfo
                if (signingInfo != null) {
                    if (signingInfo.hasMultipleSigners()) {
                        signingInfo.apkContentsSigners.map { it.toByteArray() }
                    } else {
                        signingInfo.signingCertificateHistory.map { it.toByteArray() }
                    }
                } else emptyList()
            } else {
                @Suppress("DEPRECATION")
                val packageInfo = packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNATURES
                )
                @Suppress("DEPRECATION")
                packageInfo.signatures?.map { it.toByteArray() } ?: emptyList()
            }

            if (signaturesBytes.isNotEmpty()) {
                val certBytes = signaturesBytes.first()
                val sha1 = computeHash(certBytes, "SHA-1")
                val sha256 = computeHash(certBytes, "SHA-256")
                val md5 = computeHash(certBytes, "MD5")

                var issuer = "Unknown"
                runCatching {
                    val cf = CertificateFactory.getInstance("X.509")
                    val cert = cf.generateCertificate(ByteArrayInputStream(certBytes)) as? X509Certificate
                    cert?.let {
                        issuer = it.issuerDN.name
                    }
                }

                AppDebugKeyInfo(
                    packageName = packageName,
                    sha1 = sha1,
                    sha256 = sha256,
                    md5 = md5,
                    signatureFound = true,
                    issuer = issuer
                )
            } else {
                AppDebugKeyInfo(
                    packageName = packageName,
                    sha1 = "Unavailable",
                    sha256 = "Unavailable",
                    md5 = "Unavailable",
                    signatureFound = false
                )
            }
        } catch (e: Exception) {
            AppDebugKeyInfo(
                packageName = context.packageName,
                sha1 = "Error: ${e.message}",
                sha256 = "Error: ${e.message}",
                md5 = "Error: ${e.message}",
                signatureFound = false
            )
        }
    }
}
