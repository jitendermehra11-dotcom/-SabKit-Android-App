package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.AdMobTestUnit
import com.example.model.AppDebugKeyInfo
import com.example.model.AppInfoItem
import com.example.model.CodeFileView
import com.example.model.DeviceSpec
import com.example.model.GeneratedKeyInfo
import com.example.model.KeyAlgorithm
import com.example.model.SntpResult
import com.example.model.SpeedTestResult
import com.example.model.TestStatus
import com.example.model.WifiStats
import com.example.model.ZipArchive
import com.example.model.ZipEntryItem
import com.example.util.ApkInspectorUtils
import com.example.util.CryptoUtils
import com.example.util.NetworkTimeUtils
import com.example.util.ZipUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream

enum class AppScreen {
    DASHBOARD,
    ZIP_VIEWER,
    KEYSTORE_GEN,
    APK_INSPECTOR,
    WIFI_SNTP,
    ADMOB_CENTER
}

data class SabKitUiState(
    val currentScreen: AppScreen = AppScreen.DASHBOARD,

    // Zip Module State
    val zipArchive: ZipArchive? = null,
    val selectedCodeFile: CodeFileView? = null,
    val zipFilterQuery: String = "",
    val isZipLoading: Boolean = false,
    val zipError: String? = null,

    // Keystore Module State
    val aliasInput: String = "sabkit_key",
    val selectedAlgorithm: KeyAlgorithm = KeyAlgorithm.RSA_2048,
    val generatedKeys: List<GeneratedKeyInfo> = emptyList(),
    val latestKey: GeneratedKeyInfo? = null,
    val appDebugKey: AppDebugKeyInfo? = null,
    val isGeneratingKey: Boolean = false,
    val keystoreError: String? = null,

    // APK Inspector State
    val installedApps: List<AppInfoItem> = emptyList(),
    val selectedApp: AppInfoItem? = null,
    val apkSearchQuery: String = "",
    val includeSystemApps: Boolean = false,
    val isLoadingApps: Boolean = false,
    val apkError: String? = null,

    // Wifi & SNTP State
    val wifiStats: WifiStats = WifiStats(),
    val speedTestResult: SpeedTestResult = SpeedTestResult(),
    val sntpResult: SntpResult = SntpResult(),
    val isSpeedTesting: Boolean = false,
    val isSntpSyncing: Boolean = false,
    val speedTestUrl: String = NetworkTimeUtils.DEFAULT_SPEED_TEST_URL,
    val ntpServer: String = NetworkTimeUtils.DEFAULT_NTP_SERVER,

    // AdMob & Dev Info State
    val testAdUnits: List<AdMobTestUnit> = emptyList(),
    val impressionsInput: String = "10000",
    val ecpmInput: String = "2.50",
    val calculatedRevenue: Double = 25.0,
    val deviceSpecs: List<DeviceSpec> = emptyList()
)

class SabKitViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SabKitUiState())
    val uiState: StateFlow<SabKitUiState> = _uiState.asStateFlow()

    // Cached raw zip bytes (for sample project or small picks)
    private var inMemoryZipBytes: ByteArray? = null
    private var lastLoadedZipUri: Uri? = null

    init {
        initAdMobUnits()
        loadDeviceSpecs()
        refreshAppDebugKey()
        refreshWifiStats()
        // Pre-load sample zip so the user has immediate data to explore
        loadSampleProject()
    }

    fun navigateTo(screen: AppScreen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun navigateBack(): Boolean {
        val current = _uiState.value.currentScreen
        if (_uiState.value.selectedCodeFile != null && current == AppScreen.ZIP_VIEWER) {
            closeCodeViewer()
            return true
        }
        if (_uiState.value.selectedApp != null && current == AppScreen.APK_INSPECTOR) {
            selectApp(null)
            return true
        }
        if (current != AppScreen.DASHBOARD) {
            _uiState.update { it.copy(currentScreen = AppScreen.DASHBOARD) }
            return true
        }
        return false
    }

    // -------------------------------------------------------------
    // ZIP & SOURCE CODE VIEWER
    // -------------------------------------------------------------

    fun loadSampleProject() {
        viewModelScope.launch {
            _uiState.update { it.copy(isZipLoading = true, zipError = null) }
            try {
                val sampleBytes = ZipUtils.createSampleAndroidProjectZip()
                inMemoryZipBytes = sampleBytes
                lastLoadedZipUri = null

                val parseResult = withContext(Dispatchers.IO) {
                    ZipUtils.parseZip(ByteArrayInputStream(sampleBytes), "SabKitSampleProject.zip")
                }

                parseResult.onSuccess { archive ->
                    _uiState.update {
                        it.copy(
                            zipArchive = archive,
                            isZipLoading = false,
                            selectedCodeFile = null,
                            zipError = null
                        )
                    }
                }.onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isZipLoading = false,
                            zipError = "Failed to parse sample zip: ${err.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isZipLoading = false,
                        zipError = "Sample generation error: ${e.message}"
                    )
                }
            }
        }
    }

    fun loadZipFromUri(uri: Uri) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            _uiState.update { it.copy(isZipLoading = true, zipError = null) }
            try {
                val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "archive.zip"
                val stream = context.contentResolver.openInputStream(uri)
                if (stream == null) {
                    _uiState.update {
                        it.copy(isZipLoading = false, zipError = "Could not open file from storage.")
                    }
                    return@launch
                }

                // Copy to byte array or cache if size permits, otherwise parse directly
                val bytes = withContext(Dispatchers.IO) {
                    stream.use { it.readBytes() }
                }
                inMemoryZipBytes = bytes
                lastLoadedZipUri = uri

                val parseResult = withContext(Dispatchers.IO) {
                    ZipUtils.parseZip(ByteArrayInputStream(bytes), fileName)
                }

                parseResult.onSuccess { archive ->
                    _uiState.update {
                        it.copy(
                            zipArchive = archive,
                            isZipLoading = false,
                            selectedCodeFile = null,
                            zipError = null
                        )
                    }
                }.onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isZipLoading = false,
                            zipError = "Failed to parse ZIP archive: ${err.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isZipLoading = false,
                        zipError = "Exception reading ZIP: ${e.localizedMessage ?: e::class.java.simpleName}"
                    )
                }
            }
        }
    }

    fun selectZipEntry(entry: ZipEntryItem) {
        if (entry.isDirectory) return
        viewModelScope.launch {
            _uiState.update { it.copy(isZipLoading = true, zipError = null) }
            val bytes = inMemoryZipBytes
            if (bytes != null) {
                val readResult = withContext(Dispatchers.IO) {
                    ZipUtils.readEntryContent(ByteArrayInputStream(bytes), entry.path)
                }
                readResult.onSuccess { codeView ->
                    _uiState.update {
                        it.copy(selectedCodeFile = codeView, isZipLoading = false)
                    }
                }.onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isZipLoading = false,
                            zipError = "Error reading entry: ${err.message}"
                        )
                    }
                }
            } else {
                _uiState.update {
                    it.copy(
                        isZipLoading = false,
                        zipError = "Archive stream is no longer available. Please reload the zip file."
                    )
                }
            }
        }
    }

    fun closeCodeViewer() {
        _uiState.update { it.copy(selectedCodeFile = null) }
    }

    fun setZipFilterQuery(query: String) {
        _uiState.update { it.copy(zipFilterQuery = query) }
    }

    // -------------------------------------------------------------
    // KEYSTORE & SHA-1 / SHA-256 GENERATOR
    // -------------------------------------------------------------

    fun setAliasInput(alias: String) {
        _uiState.update { it.copy(aliasInput = alias) }
    }

    fun setSelectedAlgorithm(algo: KeyAlgorithm) {
        _uiState.update { it.copy(selectedAlgorithm = algo) }
    }

    fun generateKeyPair() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingKey = true, keystoreError = null) }
            val alias = _uiState.value.aliasInput
            val algo = _uiState.value.selectedAlgorithm

            val result = withContext(Dispatchers.Default) {
                CryptoUtils.generateKeyPair(alias, algo)
            }

            result.onSuccess { keyInfo ->
                _uiState.update {
                    it.copy(
                        isGeneratingKey = false,
                        latestKey = keyInfo,
                        generatedKeys = listOf(keyInfo) + it.generatedKeys,
                        keystoreError = null
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isGeneratingKey = false,
                        keystoreError = "Key generation failed: ${err.message}"
                    )
                }
            }
        }
    }

    fun refreshAppDebugKey() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val keyInfo = withContext(Dispatchers.IO) {
                CryptoUtils.getAppSignatureInfo(context)
            }
            _uiState.update { it.copy(appDebugKey = keyInfo) }
        }
    }

    // -------------------------------------------------------------
    // APK & AAB INSPECTOR
    // -------------------------------------------------------------

    fun loadInstalledApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingApps = true, apkError = null) }
            val context = getApplication<Application>()
            val query = _uiState.value.apkSearchQuery
            val includeSystem = _uiState.value.includeSystemApps

            try {
                val apps = ApkInspectorUtils.getInstalledApps(context, includeSystem, query)
                _uiState.update {
                    it.copy(
                        installedApps = apps,
                        isLoadingApps = false,
                        apkError = if (apps.isEmpty()) "No matching apps found" else null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingApps = false,
                        apkError = "Error loading apps: ${e.message}"
                    )
                }
            }
        }
    }

    fun inspectApkUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingApps = true, apkError = null) }
            val context = getApplication<Application>()
            val result = ApkInspectorUtils.inspectApkUri(context, uri)
            result.onSuccess { inspected ->
                _uiState.update {
                    it.copy(
                        selectedApp = inspected,
                        isLoadingApps = false,
                        apkError = null
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoadingApps = false,
                        apkError = "Could not parse APK file: ${err.message}"
                    )
                }
            }
        }
    }

    fun selectApp(app: AppInfoItem?) {
        _uiState.update { it.copy(selectedApp = app) }
    }

    fun setApkSearchQuery(query: String) {
        _uiState.update { it.copy(apkSearchQuery = query) }
        loadInstalledApps()
    }

    fun setIncludeSystemApps(include: Boolean) {
        _uiState.update { it.copy(includeSystemApps = include) }
        loadInstalledApps()
    }

    // -------------------------------------------------------------
    // WIFI STATISTICS & SNTP TIME (Crash-safe & updated endpoint)
    // -------------------------------------------------------------

    fun refreshWifiStats() {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val stats = withContext(Dispatchers.IO) {
                    NetworkTimeUtils.getWifiAndNetworkStats(context)
                }
                _uiState.update { it.copy(wifiStats = stats) }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        wifiStats = WifiStats(
                            isConnected = false,
                            networkType = "Offline / Error (${t.localizedMessage ?: "Unknown"})"
                        )
                    )
                }
            }
        }
    }

    fun setSpeedTestUrl(url: String) {
        _uiState.update { it.copy(speedTestUrl = url) }
    }

    fun setNtpServer(server: String) {
        _uiState.update { it.copy(ntpServer = server) }
    }

    fun runSpeedTest() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSpeedTesting = true,
                    speedTestResult = SpeedTestResult(status = TestStatus.RUNNING, endpointUrl = it.speedTestUrl)
                )
            }

            try {
                val url = _uiState.value.speedTestUrl
                val result = withContext(Dispatchers.IO) {
                    NetworkTimeUtils.performSpeedTest(url)
                }

                _uiState.update {
                    it.copy(
                        isSpeedTesting = false,
                        speedTestResult = result
                    )
                }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        isSpeedTesting = false,
                        speedTestResult = SpeedTestResult(
                            status = TestStatus.ERROR,
                            endpointUrl = _uiState.value.speedTestUrl,
                            errorMessage = t.localizedMessage ?: "Speed test encountered an error"
                        )
                    )
                }
            }
            refreshWifiStats()
        }
    }

    fun runSntpSync() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSntpSyncing = true,
                    sntpResult = SntpResult(status = TestStatus.RUNNING, server = it.ntpServer)
                )
            }

            try {
                val server = _uiState.value.ntpServer
                val result = withContext(Dispatchers.IO) {
                    NetworkTimeUtils.querySntpOrHttpTime(server)
                }

                _uiState.update {
                    it.copy(
                        isSntpSyncing = false,
                        sntpResult = result
                    )
                }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        isSntpSyncing = false,
                        sntpResult = SntpResult(
                            status = TestStatus.ERROR,
                            server = _uiState.value.ntpServer,
                            errorMessage = t.localizedMessage ?: "SNTP query failed"
                        )
                    )
                }
            }
        }
    }

    // -------------------------------------------------------------
    // ADMOB & DEVELOPER MONETIZATION CENTER
    // -------------------------------------------------------------

    private fun initAdMobUnits() {
        val units = listOf(
            AdMobTestUnit(
                format = "Banner",
                unitId = "ca-app-pub-3940256099942544/6300978111",
                description = "Standard 320x50 or Adaptive Banner for top/bottom placement.",
                recommendedSize = "BANNER / ADAPTIVE_BANNER"
            ),
            AdMobTestUnit(
                format = "Interstitial",
                unitId = "ca-app-pub-3940256099942544/1033173712",
                description = "Full-screen interstitial ad displayed between natural flow transitions.",
                recommendedSize = "FULL SCREEN"
            ),
            AdMobTestUnit(
                format = "Rewarded Video",
                unitId = "ca-app-pub-3940256099942544/5224354917",
                description = "Users opt-in to view video in exchange for in-app currency or perks.",
                recommendedSize = "FULL SCREEN VIDEO"
            ),
            AdMobTestUnit(
                format = "Rewarded Interstitial",
                unitId = "ca-app-pub-3940256099942544/5354046379",
                description = "Seamless interstitial that grants rewards upon completion.",
                recommendedSize = "FULL SCREEN"
            ),
            AdMobTestUnit(
                format = "App Open",
                unitId = "ca-app-pub-3940256099942544/9257395921",
                description = "Shown when users bring the app to the foreground.",
                recommendedSize = "FULL SCREEN"
            ),
            AdMobTestUnit(
                format = "Native Advanced",
                unitId = "ca-app-pub-3940256099942544/2247696110",
                description = "Custom layout ad blending natively into list items and cards.",
                recommendedSize = "CUSTOM VIEW"
            )
        )
        _uiState.update { it.copy(testAdUnits = units) }
    }

    fun updateImpressions(impressions: String) {
        val clean = impressions.filter { it.isDigit() }
        _uiState.update { it.copy(impressionsInput = clean) }
        calculateRevenue()
    }

    fun updateEcpm(ecpm: String) {
        val clean = ecpm.filter { it.isDigit() || it == '.' }
        _uiState.update { it.copy(ecpmInput = clean) }
        calculateRevenue()
    }

    private fun calculateRevenue() {
        val imp = _uiState.value.impressionsInput.toDoubleOrNull() ?: 0.0
        val ecpm = _uiState.value.ecpmInput.toDoubleOrNull() ?: 0.0
        val revenue = (imp / 1000.0) * ecpm
        _uiState.update { it.copy(calculatedRevenue = revenue) }
    }

    private fun loadDeviceSpecs() {
        val specs = listOf(
            DeviceSpec("Device Model", "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"),
            DeviceSpec("Brand & Product", "${Build.BRAND} (${Build.PRODUCT})"),
            DeviceSpec("Android OS", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"),
            DeviceSpec("Build Fingerprint", Build.FINGERPRINT),
            DeviceSpec("Supported ABIs", Build.SUPPORTED_ABIS.joinToString(", ")),
            DeviceSpec("Hardware / Board", "${Build.HARDWARE} / ${Build.BOARD}"),
            DeviceSpec("Build ID & Type", "${Build.ID} (${Build.TYPE})")
        )
        _uiState.update { it.copy(deviceSpecs = specs) }
    }
}
