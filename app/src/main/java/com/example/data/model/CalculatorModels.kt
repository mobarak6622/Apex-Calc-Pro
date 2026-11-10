package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class CalculationRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val workspaceId: String = "ws_primary",
    val title: String = "Calculation",
    val expression: String = "",
    val result: String = "",
    val encryptedPayload: String = "",
    val isPinned: Boolean = false,
    val category: String = "SCIENTIFIC",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class WorkspaceWindow(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "Window 1",
    val currentExpression: String = "",
    val currentResult: String = "0",
    val memoryValue: String = "0",
    val angleMode: String = "DEG",
    val accentColorHex: String = "#00E5FF",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudBackupVault(
    @DocumentId val id: String = "",
    val userId: String = "",
    val backupName: String = "Encrypted Vault Snapshot",
    val providerTag: String = "FIREBASE_CLOUD",
    val encryptedData: String = "",
    val checksumSha256: String = "",
    val itemCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
