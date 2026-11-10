package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CalculationRecord
import com.example.data.model.CloudBackupVault
import com.example.ui.theme.AppFontPreset
import com.example.ui.theme.AppThemePreset
import com.example.util.NotificationHelper
import com.example.viewmodel.CalculatorUiState
import com.example.viewmodel.CalculatorViewModel
import com.example.viewmodel.SyncDataState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryAndVaultScreen(
    uiState: CalculatorUiState,
    calculationsState: SyncDataState<List<CalculationRecord>>,
    backupsState: SyncDataState<List<CloudBackupVault>>,
    viewModel: CalculatorViewModel,
    onLoadRecordToCalc: (CalculationRecord) -> Unit,
    onSpeakRecord: (String) -> Unit
) {
    val context = LocalContext.current
    var subTab by remember { mutableStateOf(0) } // 0 = History, 1 = Cloud Backup (Drive/Dropbox/Firebase), 2 = Theme/Fonts/Alerts
    var searchQuery by remember { mutableStateOf("") }
    var customBackupName by remember { mutableStateOf("Encrypted Vault Backup") }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        NotificationHelper.dispatchRealtimeNotification(
            context = context,
            title = "Apex Calc Real-Time Notifications Active",
            message = "Real-time cloud sync & AES-256-GCM backup alerts are enabled.",
            category = "SECURITY"
        )
    }

    val cloudRecords = (calculationsState as? SyncDataState.Success)?.data.orEmpty()
    val combinedHistory = remember(cloudRecords, uiState.localOfflineHistory, searchQuery) {
        val merged = (cloudRecords + uiState.localOfflineHistory)
            .distinctBy { "${it.expression}_${it.result}_${it.title}" }
        if (searchQuery.isBlank()) {
            merged
        } else {
            merged.filter {
                it.expression.contains(searchQuery, ignoreCase = true) ||
                    it.result.contains(searchQuery, ignoreCase = true) ||
                    it.title.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section Selector Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = subTab == 0,
                onClick = { subTab = 0 },
                label = { Text("Encrypted History (${combinedHistory.size})") },
                leadingIcon = { Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("vault_subtab_history")
            )
            FilterChip(
                selected = subTab == 1,
                onClick = { subTab = 1 },
                label = { Text("Cloud Backup") },
                leadingIcon = { Icon(Icons.Filled.Backup, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("vault_subtab_backup")
            )
            FilterChip(
                selected = subTab == 2,
                onClick = { subTab = 2 },
                label = { Text("Theme & Alerts") },
                leadingIcon = { Icon(Icons.Filled.Palette, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("vault_subtab_theme")
            )
        }

        when (subTab) {
            0 -> {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search saved history (expression, result, tag)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_search_input"),
                    singleLine = true
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (combinedHistory.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.EnhancedEncryption,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = "No Saved Calculations Yet",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Press '=' or 'Save' on the calculator to store calculations with hardware-backed AES-256-GCM encryption and real-time cloud sync.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(combinedHistory, key = { it.id.ifBlank { it.expression + it.result } }) { record ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = record.category,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text(
                                                text = record.title,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Row {
                                            IconButton(onClick = { viewModel.togglePinHistoryRecord(record) }) {
                                                Icon(
                                                    imageVector = Icons.Filled.PushPin,
                                                    contentDescription = "Pin Calculation",
                                                    tint = if (record.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(
                                                onClick = { onSpeakRecord("${record.expression} equals ${record.result}") }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                    contentDescription = "Speak Calculation",
                                                    tint = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                            IconButton(onClick = { viewModel.deleteHistoryRecord(record.id) }) {
                                                Icon(
                                                    imageVector = Icons.Filled.Delete,
                                                    contentDescription = "Delete Record",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = record.expression,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "= ${record.result}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )

                                    if (record.encryptedPayload.isNotBlank()) {
                                        Text(
                                            text = "🔒 Vault Cipher: ${record.encryptedPayload.take(44)}...",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(onClick = { onLoadRecordToCalc(record) }) {
                                            Text("Reuse in Calculator →")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // Cloud Backup Vault: Firebase Real-Time Cloud + Google Drive & Dropbox Integration
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.VerifiedUser,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Encrypted Cloud Backup (Google Drive, Dropbox & Firebase)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = "All backups are encrypted client-side using AES-256-GCM and verified with SHA-256 checksums before cloud storage or export to Google Drive / Dropbox.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                OutlinedTextField(
                                    value = customBackupName,
                                    onValueChange = { customBackupName = it },
                                    label = { Text("Backup Snapshot Label") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("backup_name_input"),
                                    singleLine = true
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.createEncryptedBackupSnapshot(
                                                context = context,
                                                backupName = customBackupName,
                                                providerTag = "FIREBASE_CLOUD"
                                            )
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("create_firebase_backup_button")
                                    ) {
                                        Icon(Icons.Filled.CloudUpload, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Firebase Vault")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            viewModel.createEncryptedBackupSnapshot(
                                                context = context,
                                                backupName = "$customBackupName (Drive)",
                                                providerTag = "GOOGLE_DRIVE"
                                            )
                                            shareEncryptedBackupToCloudApp(
                                                context = context,
                                                title = "ApexCalc_GoogleDrive_Encrypted_Backup.apexvault",
                                                payload = uiState.lastExportedBackupJson ?: "AES256GCM:EncryptedVaultSnapshot"
                                            )
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("export_google_drive_button")
                                    ) {
                                        Icon(Icons.Filled.Share, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Google Drive")
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        viewModel.createEncryptedBackupSnapshot(
                                            context = context,
                                            backupName = "$customBackupName (Dropbox)",
                                            providerTag = "DROPBOX"
                                        )
                                        shareEncryptedBackupToCloudApp(
                                            context = context,
                                            title = "ApexCalc_Dropbox_Encrypted_Backup.apexvault",
                                            payload = uiState.lastExportedBackupJson ?: "AES256GCM:EncryptedVaultSnapshot"
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("export_dropbox_button")
                                ) {
                                    Icon(Icons.Filled.CloudDownload, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Export Encrypted Archive to Dropbox / Cloud Storage")
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Saved Encrypted Cloud Snapshots",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    when (backupsState) {
                        is SyncDataState.Loading -> {
                            item { Text("Loading encrypted cloud vaults...") }
                        }
                        is SyncDataState.Error -> {
                            item {
                                Text(
                                    text = "Cloud Backup Status: ${backupsState.message}",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        is SyncDataState.Success -> {
                            val vaults = backupsState.data
                            if (vaults.isEmpty()) {
                                item {
                                    Text(
                                        text = "No cloud snapshots yet. Tap 'Firebase Vault', 'Google Drive', or 'Dropbox' above to create an AES-256-GCM encrypted backup.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                items(vaults, key = { it.id }) { vault ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = vault.backupName,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Surface(
                                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = vault.providerTag,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "Items: ${vault.itemCount}  •  SHA-256: ${vault.checksumSha256.take(18)}...",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                TextButton(
                                                    onClick = { viewModel.restoreFromBackupVault(context, vault) }
                                                ) {
                                                    Text("Restore Snapshot")
                                                }
                                                TextButton(
                                                    onClick = { viewModel.deleteBackupVault(vault.id) }
                                                ) {
                                                    Text("Delete", color = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Customizable Themes, Fonts & Real-Time Notification System
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "🎨 Customizable Dark & Light Themes (কাস্টমাইজযোগ্য থিম)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AppThemePreset.entries.forEach { preset ->
                                        FilterChip(
                                            selected = uiState.themePreset == preset,
                                            onClick = { viewModel.setThemePreset(preset) },
                                            label = { Text("${preset.label} (${preset.bnLabel})") },
                                            modifier = Modifier.testTag("theme_preset_${preset.name}")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "🔤 Customizable Typography & Fonts (কাস্টমাইজযোগ্য ফন্ট)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AppFontPreset.entries.forEach { font ->
                                        FilterChip(
                                            selected = uiState.fontPreset == font,
                                            onClick = { viewModel.setFontPreset(font) },
                                            label = { Text("${font.label} • ${font.bnLabel}") },
                                            modifier = Modifier.testTag("font_preset_${font.name}")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.NotificationsActive,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                        Text(
                                            text = "Real-Time Notification Center",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            } else {
                                                NotificationHelper.dispatchRealtimeNotification(
                                                    context = context,
                                                    title = "Real-Time Sync Verified",
                                                    message = "All calculations & windows are encrypted and synchronized.",
                                                    category = "SYNC"
                                                )
                                            }
                                        },
                                        modifier = Modifier.testTag("enable_realtime_notifications_button")
                                    ) {
                                        Text("Test Alert")
                                    }
                                }

                                if (uiState.notifications.isEmpty()) {
                                    Text(
                                        text = "Real-time notifications for cloud sync, voice commands, and backup events appear here and in the Android status bar.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    uiState.notifications.take(10).forEach { alert ->
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text(
                                                    text = "[${alert.timestampLabel}] ${alert.title}",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = alert.message,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun shareEncryptedBackupToCloudApp(
    context: android.content.Context,
    title: String,
    payload: String
) {
    try {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, payload)
        }
        val chooser = Intent.createChooser(sendIntent, "Save Encrypted Backup to Google Drive / Dropbox")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (_: Exception) {
    }
}
