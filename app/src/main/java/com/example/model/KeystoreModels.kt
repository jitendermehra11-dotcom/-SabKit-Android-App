package com.example.model

enum class KeyAlgorithm(val title: String, val standardName: String, val defaultSize: Int) {
    RSA_2048("RSA (2048-bit)", "RSA", 2048),
    RSA_4096("RSA (4096-bit)", "RSA", 4096),
    ECDSA_256("ECDSA (secp256r1)", "EC", 256)
}

data class GeneratedKeyInfo(
    val alias: String,
    val algorithm: String,
    val keySize: Int,
    val createdAt: Long,
    val sha1Hex: String,
    val sha256Hex: String,
    val md5Hex: String,
    val publicKeyPem: String,
    val isHardwareBacked: Boolean = false
)

data class AppDebugKeyInfo(
    val packageName: String,
    val sha1: String,
    val sha256: String,
    val md5: String,
    val signatureFound: Boolean,
    val issuer: String = ""
)
