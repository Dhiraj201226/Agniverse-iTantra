package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.MainViewModel
import com.example.myapplication.ui.screens.*
import com.example.myapplication.ui.theme.MyApplicationTheme

enum class NavigationTab(val label: String, val icon: ImageVector) {
    COMMS("Comms", Icons.Default.Chat),
    WALKIE_TALKIE("Radio", Icons.Default.Radio),
    EMERGENCY("SOS", Icons.Default.Warning),
    TELEMETRY("Metrics", Icons.Default.Analytics),
    SETTINGS("Models", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request Audio Record Permission on startup if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 200)
        }

        setContent {
            MyApplicationTheme {
                val mainViewModel: MainViewModel = viewModel()
                var currentTab by remember { mutableStateOf(NavigationTab.COMMS) }

                val nodeProfile by mainViewModel.nodeProfile.collectAsState()
                val showSetupSheet by mainViewModel.showNodeSetupSheet.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            NavigationTab.entries.forEach { tab ->
                                NavigationBarItem(
                                    selected = currentTab == tab,
                                    onClick = { currentTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = tab.label,
                                            tint = if (tab == NavigationTab.EMERGENCY) Color(0xFFD32F2F) else LocalContentColor.current
                                        )
                                    },
                                    label = { Text(tab.label) }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentTab) {
                            NavigationTab.COMMS -> CommsScreen(viewModel = mainViewModel)
                            NavigationTab.WALKIE_TALKIE -> WalkieTalkieScreen(viewModel = mainViewModel)
                            NavigationTab.EMERGENCY -> EmergencyScreen(viewModel = mainViewModel)
                            NavigationTab.TELEMETRY -> TelemetryScreen(viewModel = mainViewModel)
                            NavigationTab.SETTINGS -> ModelCenterScreen(viewModel = mainViewModel)
                        }
                    }

                    // Initial Node Profile Information Collection Sheet (No Login Required)
                    if (showSetupSheet) {
                        NodeSetupSheet(
                            currentProfile = nodeProfile,
                            onSaveProfile = { callSign, language, role ->
                                mainViewModel.updateNodeProfile(callSign, language, role)
                            },
                            onDismiss = { mainViewModel.dismissNodeSetupSheet() }
                        )
                    }
                }
            }
        }
    }
}
