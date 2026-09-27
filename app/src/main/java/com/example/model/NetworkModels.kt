package com.example.model

enum class TestStatus {
    IDLE,
    RUNNING,
    SUCCESS,
    ERROR
}

data class WifiStats(
    val isConnected: Boolean = false,
    val networkType: String = "Unknown",
    val ssid: String = "N/A",
    val bssid: String = "N/A",
    val ipAddress: String = "N/A",
    val gateway: String = "N/A",
    val dnsServers: String = "N/A",
    val linkSpeedMbps: Int = 0,
    val rssi: Int = 0,
    val frequencyMhz: Int = 0,
    val isWifiEnabled: Boolean = false
)

data class SpeedTestResult(
    val status: TestStatus = TestStatus.IDLE,
    val endpointUrl: String = "https://httpbin.org/bytes/1024",
    val bytesDownloaded: Long = 0,
    val durationMs: Long = 0,
    val speedKbps: Double = 0.0,
    val latencyMs: Long = 0,
    val responseCode: Int = 0,
    val errorMessage: String? = null
)

data class SntpResult(
    val status: TestStatus = TestStatus.IDLE,
    val server: String = "time.google.com",
    val sntpTimeMs: Long = 0L,
    val systemTimeMs: Long = 0L,
    val driftMs: Long = 0L,
    val roundTripDelayMs: Long = 0L,
    val formattedSntpTime: String = "",
    val formattedSystemTime: String = "",
    val protocolSource: String = "SNTP (UDP 123)",
    val errorMessage: String? = null
)
