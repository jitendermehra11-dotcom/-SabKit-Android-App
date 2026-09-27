package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.SystemClock
import com.example.model.SntpResult
import com.example.model.SpeedTestResult
import com.example.model.TestStatus
import com.example.model.WifiStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.FileNotFoundException
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object NetworkTimeUtils {

    const val DEFAULT_SPEED_TEST_URL = "https://httpbin.org/bytes/1024"
    const val DEFAULT_NTP_SERVER = "time.google.com"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * Executes safe network download speed & latency test.
     * Guaranteed never to crash the app or throw unhandled exceptions.
     */
    suspend fun performSpeedTest(url: String = DEFAULT_SPEED_TEST_URL): SpeedTestResult =
        withContext(Dispatchers.IO) {
            val startTime = SystemClock.elapsedRealtime()
            val effectiveUrl = if (url.isBlank()) DEFAULT_SPEED_TEST_URL else url.trim()

            try {
                val request = Request.Builder()
                    .url(effectiveUrl)
                    .header("User-Agent", "SabKit-NetworkTest/1.0")
                    .build()

                val callStartTime = System.currentTimeMillis()
                httpClient.newCall(request).execute().use { response ->
                    val latencyMs = (System.currentTimeMillis() - callStartTime).coerceAtLeast(1)

                    if (!response.isSuccessful) {
                        return@withContext SpeedTestResult(
                            status = TestStatus.ERROR,
                            endpointUrl = effectiveUrl,
                            responseCode = response.code,
                            durationMs = (SystemClock.elapsedRealtime() - startTime).coerceAtLeast(1),
                            errorMessage = "HTTP Error ${response.code}: ${response.message.ifEmpty { "Not Found" }}"
                        )
                    }

                    val body = response.body
                    if (body == null) {
                        return@withContext SpeedTestResult(
                            status = TestStatus.ERROR,
                            endpointUrl = effectiveUrl,
                            responseCode = response.code,
                            errorMessage = "Empty response received from server"
                        )
                    }

                    val bytes = body.bytes()
                    val totalDurationMs = (SystemClock.elapsedRealtime() - startTime).coerceAtLeast(1)
                    val bytesCount = bytes.size.toLong()

                    val speedKbps = (bytesCount.toDouble() / 1024.0) / (totalDurationMs.toDouble() / 1000.0)

                    SpeedTestResult(
                        status = TestStatus.SUCCESS,
                        endpointUrl = effectiveUrl,
                        bytesDownloaded = bytesCount,
                        durationMs = totalDurationMs,
                        speedKbps = speedKbps,
                        latencyMs = latencyMs,
                        responseCode = response.code,
                        errorMessage = null
                    )
                }
            } catch (fnf: FileNotFoundException) {
                SpeedTestResult(
                    status = TestStatus.ERROR,
                    endpointUrl = effectiveUrl,
                    durationMs = (SystemClock.elapsedRealtime() - startTime).coerceAtLeast(1),
                    errorMessage = "Resource Not Found (404): ${fnf.message}"
                )
            } catch (timeout: SocketTimeoutException) {
                SpeedTestResult(
                    status = TestStatus.ERROR,
                    endpointUrl = effectiveUrl,
                    durationMs = (SystemClock.elapsedRealtime() - startTime).coerceAtLeast(1),
                    errorMessage = "Connection Timed Out: Server did not respond within 8 seconds"
                )
            } catch (host: UnknownHostException) {
                SpeedTestResult(
                    status = TestStatus.ERROR,
                    endpointUrl = effectiveUrl,
                    durationMs = (SystemClock.elapsedRealtime() - startTime).coerceAtLeast(1),
                    errorMessage = "Network Offline or Host Unresolved: ${host.message}"
                )
            } catch (ioe: IOException) {
                SpeedTestResult(
                    status = TestStatus.ERROR,
                    endpointUrl = effectiveUrl,
                    durationMs = (SystemClock.elapsedRealtime() - startTime).coerceAtLeast(1),
                    errorMessage = "Network I/O Error: ${ioe.localizedMessage ?: "Connection interrupted"}"
                )
            } catch (t: Throwable) {
                SpeedTestResult(
                    status = TestStatus.ERROR,
                    endpointUrl = effectiveUrl,
                    durationMs = (SystemClock.elapsedRealtime() - startTime).coerceAtLeast(1),
                    errorMessage = "Network Error: ${t.localizedMessage ?: t.javaClass.simpleName}"
                )
            }
        }

    /**
     * Executes SNTP UDP time query with HTTP Date header fallback.
     * Guaranteed to run strictly on Dispatchers.IO inside safe try-catch blocks.
     */
    suspend fun querySntpOrHttpTime(server: String = DEFAULT_NTP_SERVER): SntpResult =
        withContext(Dispatchers.IO) {
            val systemNow = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS z", Locale.getDefault())
            val targetServer = server.ifBlank { DEFAULT_NTP_SERVER }.trim()

            // 1. Attempt standard SNTP UDP 123 query
            try {
                val udpResult = querySntpUdp(targetServer)
                if (udpResult != null) {
                    val sntpTime = udpResult.first
                    val rtt = udpResult.second
                    val drift = sntpTime - systemNow
                    return@withContext SntpResult(
                        status = TestStatus.SUCCESS,
                        server = targetServer,
                        sntpTimeMs = sntpTime,
                        systemTimeMs = systemNow,
                        driftMs = drift,
                        roundTripDelayMs = rtt,
                        formattedSntpTime = dateFormat.format(Date(sntpTime)),
                        formattedSystemTime = dateFormat.format(Date(systemNow)),
                        protocolSource = "SNTP (UDP 123)",
                        errorMessage = null
                    )
                }
            } catch (_: Throwable) {
                // Fallback gracefully on UDP failure
            }

            // 2. Resilient Fallback: Query HTTP Date header from working endpoint https://httpbin.org/bytes/1024
            try {
                val request = Request.Builder()
                    .url(DEFAULT_SPEED_TEST_URL)
                    .head()
                    .build()

                val callStart = SystemClock.elapsedRealtime()
                httpClient.newCall(request).execute().use { response ->
                    val rtt = (SystemClock.elapsedRealtime() - callStart).coerceAtLeast(1)
                    val serverDate = response.headers.getDate("Date")
                    val networkTime = (serverDate?.time ?: System.currentTimeMillis()) + (rtt / 2)
                    val drift = networkTime - systemNow

                    return@withContext SntpResult(
                        status = TestStatus.SUCCESS,
                        server = "httpbin.org (HTTP Date Sync Fallback)",
                        sntpTimeMs = networkTime,
                        systemTimeMs = systemNow,
                        driftMs = drift,
                        roundTripDelayMs = rtt,
                        formattedSntpTime = dateFormat.format(Date(networkTime)),
                        formattedSystemTime = dateFormat.format(Date(systemNow)),
                        protocolSource = "HTTP Date Header (UTC Sync)",
                        errorMessage = null
                    )
                }
            } catch (t: Throwable) {
                SntpResult(
                    status = TestStatus.ERROR,
                    server = targetServer,
                    systemTimeMs = systemNow,
                    formattedSystemTime = dateFormat.format(Date(systemNow)),
                    errorMessage = "Time sync error: ${t.localizedMessage ?: t.javaClass.simpleName}"
                )
            }
        }

    private fun querySntpUdp(server: String): Pair<Long, Long>? {
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            socket.soTimeout = 2500

            val address = InetAddress.getByName(server)
            val buffer = ByteArray(48)
            buffer[0] = 0x1B

            val requestStartMs = System.currentTimeMillis()
            val requestTicks = SystemClock.elapsedRealtime()

            writeNtpTimeStamp(buffer, 40, requestStartMs)

            val packet = DatagramPacket(buffer, buffer.size, address, 123)
            socket.send(packet)

            val responsePacket = DatagramPacket(buffer, buffer.size)
            socket.receive(responsePacket)

            val responseTicks = SystemClock.elapsedRealtime()
            val roundTrip = responseTicks - requestTicks

            val origMs = readNtpTimeStamp(buffer, 24)
            val recvMs = readNtpTimeStamp(buffer, 32)
            val transMs = readNtpTimeStamp(buffer, 40)
            val destMs = requestStartMs + roundTrip

            val offset = if (origMs != 0L) {
                ((recvMs - origMs) + (transMs - destMs)) / 2
            } else {
                transMs - destMs
            }

            val accurateTime = System.currentTimeMillis() + offset
            return Pair(accurateTime, roundTrip)
        } catch (_: Throwable) {
            return null
        } finally {
            runCatching { socket?.close() }
        }
    }

    private fun writeNtpTimeStamp(buffer: ByteArray, offset: Int, timeMs: Long) {
        val seconds = timeMs / 1000L + 2208988800L
        val fraction = ((timeMs % 1000L) * 0x100000000L) / 1000L

        buffer[offset] = (seconds shr 24).toByte()
        buffer[offset + 1] = (seconds shr 16).toByte()
        buffer[offset + 2] = (seconds shr 8).toByte()
        buffer[offset + 3] = seconds.toByte()

        buffer[offset + 4] = (fraction shr 24).toByte()
        buffer[offset + 5] = (fraction shr 16).toByte()
        buffer[offset + 6] = (fraction shr 8).toByte()
        buffer[offset + 7] = fraction.toByte()
    }

    private fun readNtpTimeStamp(buffer: ByteArray, offset: Int): Long {
        val seconds = (buffer[offset].toLong() and 0xFF shl 24) or
                (buffer[offset + 1].toLong() and 0xFF shl 16) or
                (buffer[offset + 2].toLong() and 0xFF shl 8) or
                (buffer[offset + 3].toLong() and 0xFF)

        val fraction = (buffer[offset + 4].toLong() and 0xFF shl 24) or
                (buffer[offset + 5].toLong() and 0xFF shl 16) or
                (buffer[offset + 6].toLong() and 0xFF shl 8) or
                (buffer[offset + 7].toLong() and 0xFF)

        if (seconds == 0L && fraction == 0L) return 0L
        return ((seconds - 2208988800L) * 1000L) + ((fraction * 1000L) / 0x100000000L)
    }

    /**
     * Safely reads Wifi & Network telemetry strictly inside Dispatchers.IO.
     * Guaranteed never to crash.
     */
    suspend fun getWifiAndNetworkStats(context: Context): WifiStats = withContext(Dispatchers.IO) {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

            val isWifiEnabled = runCatching { wm?.isWifiEnabled == true }.getOrDefault(false)
            val activeNetwork = runCatching { cm?.activeNetwork }.getOrNull()
            val capabilities = runCatching { cm?.getNetworkCapabilities(activeNetwork) }.getOrNull()

            val isConnected = capabilities != null && (
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            )

            val netType = when {
                capabilities == null -> "Disconnected / Offline"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi (WLAN)"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular Mobile Data"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN Tunnel"
                else -> "Active Connection"
            }

            var linkSpeed = 0
            var rssi = 0
            var freq = 0
            var ssid = "Unknown"
            var bssid = "N/A"

            runCatching {
                val wifiInfo = wm?.connectionInfo
                if (wifiInfo != null) {
                    linkSpeed = wifiInfo.linkSpeed
                    rssi = wifiInfo.rssi
                    freq = wifiInfo.frequency
                    val cleanSsid = wifiInfo.ssid.removeSurrounding("\"")
                    if (cleanSsid != "<unknown ssid>" && cleanSsid.isNotBlank()) {
                        ssid = cleanSsid
                    }
                    bssid = wifiInfo.bssid ?: "N/A"
                }
            }

            var ipAddress = "127.0.0.1"
            var gateway = "N/A"
            var dns = "N/A"

            runCatching {
                val linkProperties = cm?.getLinkProperties(activeNetwork)
                if (linkProperties != null) {
                    val linkAddresses = linkProperties.linkAddresses
                    val ipv4 = linkAddresses.firstOrNull { it.address.address.size == 4 }?.address?.hostAddress
                    ipAddress = ipv4 ?: linkAddresses.firstOrNull()?.address?.hostAddress ?: "Unavailable"

                    val routes = linkProperties.routes
                    val gw = routes.firstOrNull { it.isDefaultRoute }?.gateway?.hostAddress
                    gateway = gw ?: "N/A"

                    val dnsServers = linkProperties.dnsServers
                    if (dnsServers.isNotEmpty()) {
                        dns = dnsServers.joinToString(", ") { it.hostAddress ?: "" }
                    }
                } else {
                    ipAddress = getDeviceIpAddress()
                }
            }

            WifiStats(
                isConnected = isConnected,
                networkType = netType,
                ssid = ssid,
                bssid = bssid,
                ipAddress = ipAddress,
                gateway = gateway,
                dnsServers = dns,
                linkSpeedMbps = linkSpeed,
                rssi = rssi,
                frequencyMhz = freq,
                isWifiEnabled = isWifiEnabled
            )
        } catch (_: Throwable) {
            WifiStats(
                isConnected = false,
                networkType = "Offline / Unavailable",
                ipAddress = getDeviceIpAddress()
            )
        }
    }

    private fun getDeviceIpAddress(): String {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr.address.size == 4) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
            "127.0.0.1"
        } catch (_: Throwable) {
            "127.0.0.1"
        }
    }
}
