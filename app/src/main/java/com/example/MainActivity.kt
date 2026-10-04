package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.SabKitTopBar
import com.example.ui.screens.AdMobCenterScreen
import com.example.ui.screens.ApkInspectorScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GovernmentLoanPopupDialog
import com.example.ui.screens.KeystoreGenScreen
import com.example.ui.screens.LoanProfileScreen
import com.example.ui.screens.WifiSntpScreen
import com.example.ui.screens.ZipViewerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.SabKitViewModel
import com.google.android.gms.ads.MobileAds
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Security Layer 1: Setup Uncaught Exception Handler to mask sensitive internal stack traces
        setupGlobalSecurityExceptionHandler()

        // Security Layer 2: Initialize Encrypted Storage Layer Safely
        initEncryptedStorage()

        enableEdgeToEdge()

        // Ads initialization safely
        runCatching {
            MobileAds.initialize(this) {}
        }

        setContent {
            MyApplicationTheme {
                SabKitAppSafe()
            }
        }
    }

    private fun setupGlobalSecurityExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("SabKitSecurity", "Unhandled exception safely intercepted", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun initEncryptedStorage() {
        runCatching {
            val masterKey = MasterKey.Builder(applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                applicationContext,
                "sabkit_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SKEY,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }.onFailure {
            Log.e("SabKitSecurity", "EncryptedSharedPreferences initialization failed", it)
        }
    }
}

@Composable
fun SabKitAppSafe() {
    val viewModel: SabKitViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()

    BackHandler(enabled = state.currentScreen != AppScreen.DASHBOARD || state.selectedCodeFile != null || state.selectedApp != null) {
        viewModel.navigateBack()
    }

    val topBarTitle = when (state.currentScreen) {
        AppScreen.DASHBOARD -> "SabKit"
        AppScreen.ZIP_VIEWER -> "ZIP & Code Viewer"
        AppScreen.KEYSTORE_GEN -> "Keystore & SHA Generator"
        AppScreen.APK_INSPECTOR -> "APK & AAB Inspector"
        AppScreen.WIFI_SNTP -> "Wifi & SNTP Diagnostics"
        AppScreen.ADMOB_CENTER -> "AdMob & Developer Info"
        AppScreen.LOAN_PROFILE -> "सरकारी लोन सहायता"
    }

    val topBarSubtitle = when (state.currentScreen) {
        AppScreen.DASHBOARD -> "All-in-One Developer Utility"
        AppScreen.ZIP_VIEWER -> "In-memory Zip & Source Explorer"
        AppScreen.KEYSTORE_GEN -> "RSA / ECDSA & Fingerprint Hashes"
        AppScreen.APK_INSPECTOR -> "Package Metadata & Signatures"
        AppScreen.WIFI_SNTP -> "Latency, Speed & True NTP Time"
        AppScreen.ADMOB_CENTER -> "Official Test IDs & eCPM Estimator"
        AppScreen.LOAN_PROFILE -> "PM SVANidhi, MUDRA & MSME Schemes"
    }

    // Pure Compose Dialog
    if (state.showLoanDialog) {
        GovernmentLoanPopupDialog(
            showDialog = state.showLoanDialog,
            onDismiss = { viewModel.setLoanDialogVisible(false) },
            onApplyNow = {
                viewModel.setLoanDialogVisible(false)
                viewModel.navigateTo(AppScreen.LOAN_PROFILE)
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            SabKitTopBar(
                title = topBarTitle,
                subtitle = topBarSubtitle,
                canNavigateBack = state.currentScreen != AppScreen.DASHBOARD,
                onNavigateBack = { viewModel.navigateBack() }
            )
        },
        bottomBar = {
            SabKitBottomNav(
                currentScreen = state.currentScreen,
                onSelectScreen = { viewModel.navigateTo(it) }
            )
        }
    ) { innerPadding ->
        val screenModifier = Modifier.padding(innerPadding)

        Box(modifier = screenModifier.fillMaxSize()) {
            when (state.currentScreen) {
                AppScreen.DASHBOARD -> {
                    DashboardScreen(
                        state = state,
                        viewModel = viewModel,
                        onNavigate = { viewModel.navigateTo(it) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                AppScreen.ZIP_VIEWER -> {
                    ZipViewerScreen(
                        state = state,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                AppScreen.KEYSTORE_GEN -> {
                    KeystoreGenScreen(
                        state = state,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                AppScreen.APK_INSPECTOR -> {
                    ApkInspectorScreen(
                        state = state,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                AppScreen.WIFI_SNTP -> {
                    WifiSntpScreen(
                        state = state,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                AppScreen.ADMOB_CENTER -> {
                    AdMobCenterScreen(
                        state = state,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                AppScreen.LOAN_PROFILE -> {
                    LoanProfileScreen(
                        state = state,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun SabKitBottomNav(
    currentScreen: AppScreen,
    onSelectScreen: (AppScreen) -> Unit
) {
    NavigationBar(
        modifier = Modifier.testTag("sabkit_bottom_nav")
    ) {
        NavigationBarItem(
            selected = currentScreen == AppScreen.DASHBOARD,
            onClick = { onSelectScreen(AppScreen.DASHBOARD) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Dashboard,
                    contentDescription = "Dashboard",
                    modifier = Modifier.size(20.dp)
                )
            },
            label = { Text("Home", fontSize = 10.sp) },
            modifier = Modifier.testTag("nav_item_dashboard")
        )

        NavigationBarItem(
            selected = currentScreen == AppScreen.ZIP_VIEWER,
            onClick = { onSelectScreen(AppScreen.ZIP_VIEWER) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = "ZIP",
                    modifier = Modifier.size(20.dp)
                )
            },
            label = { Text("ZIP", fontSize = 10.sp) },
            modifier = Modifier.testTag("nav_item_zip")
        )

        NavigationBarItem(
            selected = currentScreen == AppScreen.KEYSTORE_GEN,
            onClick = { onSelectScreen(AppScreen.KEYSTORE_GEN) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Keystore",
                    modifier = Modifier.size(20.dp)
                )
            },
            label = { Text("KeyStore", fontSize = 10.sp) },
            modifier = Modifier.testTag("nav_item_keystore")
        )

        NavigationBarItem(
            selected = currentScreen == AppScreen.APK_INSPECTOR,
            onClick = { onSelectScreen(AppScreen.APK_INSPECTOR) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "APK",
                    modifier = Modifier.size(20.dp)
                )
            },
            label = { Text("APK", fontSize = 10.sp) },
            modifier = Modifier.testTag("nav_item_apk")
        )

        NavigationBarItem(
            selected = currentScreen == AppScreen.WIFI_SNTP,
            onClick = { onSelectScreen(AppScreen.WIFI_SNTP) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = "Network",
                    modifier = Modifier.size(20.dp)
                )
            },
            label = { Text("Wifi/NTP", fontSize = 10.sp) },
            modifier = Modifier.testTag("nav_item_wifi")
        )

        NavigationBarItem(
            selected = currentScreen == AppScreen.ADMOB_CENTER,
            onClick = { onSelectScreen(AppScreen.ADMOB_CENTER) },
            icon = {
                Icon(
                    imageVector = Icons.Default.MonetizationOn,
                    contentDescription = "AdMob",
                    modifier = Modifier.size(20.dp)
                )
            },
            label = { Text("AdMob", fontSize = 10.sp) },
            modifier = Modifier.testTag("nav_item_admob")
        )
    }
}
