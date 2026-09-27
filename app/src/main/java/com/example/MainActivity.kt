package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.SabKitTopBar
import com.example.ui.screens.AdMobCenterScreen
import com.example.ui.screens.ApkInspectorScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.KeystoreGenScreen
import com.example.ui.screens.WifiSntpScreen
import com.example.ui.screens.ZipViewerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.SabKitViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                SabKitApp()
            }
        }
    }
}

@Composable
fun SabKitApp(
    viewModel: SabKitViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
    }

    val topBarSubtitle = when (state.currentScreen) {
        AppScreen.DASHBOARD -> "All-in-One Developer Utility"
        AppScreen.ZIP_VIEWER -> "In-memory Zip & Source Explorer"
        AppScreen.KEYSTORE_GEN -> "RSA / ECDSA & Fingerprint Hashes"
        AppScreen.APK_INSPECTOR -> "Package Metadata & Signatures"
        AppScreen.WIFI_SNTP -> "Latency, Speed & True NTP Time"
        AppScreen.ADMOB_CENTER -> "Official Test IDs & eCPM Estimator"
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

        when (state.currentScreen) {
            AppScreen.DASHBOARD -> {
                DashboardScreen(
                    state = state,
                    viewModel = viewModel,
                    onNavigate = { viewModel.navigateTo(it) },
                    modifier = screenModifier
                )
            }
            AppScreen.ZIP_VIEWER -> {
                ZipViewerScreen(
                    state = state,
                    viewModel = viewModel,
                    modifier = screenModifier
                )
            }
            AppScreen.KEYSTORE_GEN -> {
                KeystoreGenScreen(
                    state = state,
                    viewModel = viewModel,
                    modifier = screenModifier
                )
            }
            AppScreen.APK_INSPECTOR -> {
                ApkInspectorScreen(
                    state = state,
                    viewModel = viewModel,
                    modifier = screenModifier
                )
            }
            AppScreen.WIFI_SNTP -> {
                WifiSntpScreen(
                    state = state,
                    viewModel = viewModel,
                    modifier = screenModifier
                )
            }
            AppScreen.ADMOB_CENTER -> {
                AdMobCenterScreen(
                    state = state,
                    viewModel = viewModel,
                    modifier = screenModifier
                )
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

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme { Greeting("SabKit") }
}

