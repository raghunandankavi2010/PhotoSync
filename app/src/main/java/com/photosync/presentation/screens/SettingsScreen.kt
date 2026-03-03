package com.photosync.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.photosync.domain.model.SyncPreferences
import com.photosync.presentation.intent.SettingsIntent
import com.photosync.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val preferences by viewModel.preferences.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.processIntent(SettingsIntent.ResetToDefaults)
                    }) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Reset")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsContent(
                preferences = preferences,
                onIntent = viewModel::processIntent
            )
        }

        // Error Snackbar
        errorMessage?.let { message ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(message)
            }
        }
    }
}

@Composable
fun SettingsContent(
    preferences: SyncPreferences,
    onIntent: (SettingsIntent) -> Unit
) {
    // Sync Settings Section
    SettingsSection(title = "Sync Settings") {
        SettingsSwitch(
            title = "Auto Sync",
            description = "Automatically sync new photos",
            icon = Icons.Default.Sync,
            checked = preferences.autoSync,
            onCheckedChange = { onIntent(SettingsIntent.SetAutoSync(it)) }
        )

        SettingsSwitch(
            title = "Wi-Fi Only",
            description = "Only sync when connected to Wi-Fi",
            icon = Icons.Default.Wifi,
            checked = preferences.wifiOnly,
            onCheckedChange = { onIntent(SettingsIntent.SetWifiOnly(it)) }
        )

        SettingsSwitch(
            title = "Charging Only",
            description = "Only sync when device is charging",
            icon = Icons.Default.BatteryChargingFull,
            checked = preferences.chargingOnly,
            onCheckedChange = { onIntent(SettingsIntent.SetChargingOnly(it)) }
        )

        SettingsSwitch(
            title = "Data Saver",
            description = "Reduce data usage on cellular networks",
            icon = Icons.Default.DataSaverOn,
            checked = preferences.dataSaver,
            onCheckedChange = { onIntent(SettingsIntent.SetDataSaver(it)) }
        )
    }

    // Privacy Settings Section
    SettingsSection(title = "Privacy") {
        SettingsSwitch(
            title = "Strip EXIF Data",
            description = "Remove location and metadata before upload",
            icon = Icons.Default.PrivacyTip,
            checked = preferences.stripExif,
            onCheckedChange = { onIntent(SettingsIntent.SetStripExif(it)) }
        )
    }

    // About Section
    SettingsSection(title = "About") {
        ListItem(
            headlineContent = { Text("Version") },
            supportingContent = { Text("1.0.0") },
            leadingContent = {
                Icon(Icons.Default.Info, contentDescription = null)
            }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun SettingsSwitch(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(description) },
        leadingContent = {
            Icon(icon, contentDescription = null)
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    )
}
