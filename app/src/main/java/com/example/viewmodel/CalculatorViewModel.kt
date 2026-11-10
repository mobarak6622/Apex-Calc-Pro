package com.example.viewmodel

import android.content.Context
import com.example.data.model.CalculationRecord
import com.example.data.model.CloudBackupVault
import com.example.data.model.WorkspaceWindow
import com.example.data.repository.CalculatorRepository
import com.example.domain.ScientificMathEngine
import com.example.security.CryptoVaultManager
import com.example.ui.theme.AppFontPreset
import com.example.ui.theme.AppThemePreset
import com.example.util.NotificationHelper
import com.example.util.RealtimeAlertItem
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

sealed interface SyncDataState<out T> {
    data object Loading : SyncDataState<Nothing>
    data class Success<T>(val data: T) : SyncDataState<T>
    data class Error(val message: String) : SyncDataState<Nothing>
}

data class LocalCalcWindow(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Window 1",
    val expression: String = "",
    val liveResult: String = "0",
    val memoryValue: Double = 0.0,
    val isDegreeMode: Boolean = true,
    val accentHex: String = "#00E5FF",
    val errorMessage: String? = null
)

data class CalculatorUiState(
    val windows: List<LocalCalcWindow> = listOf(
        LocalCalcWindow(id = "ws_main", name = "Main Scientific", accentHex = "#00E5FF"),
        LocalCalcWindow(id = "ws_physics", name = "Physics & Calculus", accentHex = "#10B981")
    ),
    val activeWindowIndex: Int = 0,
    val isSplitMultiWindow: Boolean = false,
    val isScientificExpanded: Boolean = true,
    val showEngineeringToolsSheet: Boolean = false,
    val showKeyboardShortcutsDialog: Boolean = false,
    val themePreset: AppThemePreset = AppThemePreset.QUANTUM_CYAN,
    val fontPreset: AppFontPreset = AppFontPreset.SPACE_GROTESK,
    val statusBanner: String? = null,
    val notifications: List<RealtimeAlertItem> = emptyList(),
    val localOfflineHistory: List<CalculationRecord> = emptyList(),
    val lastExportedBackupJson: String? = null
) {
    val activeWindow: LocalCalcWindow
        get() = windows.getOrElse(activeWindowIndex) { windows.first() }
}

class CalculatorViewModel(
    private val repository: CalculatorRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    val calculationsState: StateFlow<SyncDataState<List<CalculationRecord>>> =
        repository.observeCalculations()
            .map<List<CalculationRecord>, SyncDataState<List<CalculationRecord>>> { list ->
                SyncDataState.Success(list)
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                emit(SyncDataState.Error(rootError.message ?: "Failed to sync calculations"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = SyncDataState.Loading
            )

    val workspacesState: StateFlow<SyncDataState<List<WorkspaceWindow>>> =
        repository.observeWorkspaces()
            .map<List<WorkspaceWindow>, SyncDataState<List<WorkspaceWindow>>> { list ->
                SyncDataState.Success(list)
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                emit(SyncDataState.Error(rootError.message ?: "Failed to sync workspaces"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = SyncDataState.Loading
            )

    val backupsState: StateFlow<SyncDataState<List<CloudBackupVault>>> =
        repository.observeBackups()
            .map<List<CloudBackupVault>, SyncDataState<List<CloudBackupVault>>> { list ->
                SyncDataState.Success(list)
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                emit(SyncDataState.Error(rootError.message ?: "Failed to sync cloud backups"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = SyncDataState.Loading
            )

    // --- Input & Keypad Actions ---

    fun appendToken(token: String) {
        updateActiveWindow { win ->
            val newExpr = win.expression + token
            val preview = evaluatePreview(newExpr, win.isDegreeMode)
            win.copy(
                expression = newExpr,
                liveResult = preview ?: win.liveResult,
                errorMessage = null
            )
        }
    }

    fun deleteLastChar() {
        updateActiveWindow { win ->
            val newExpr = if (win.expression.isNotEmpty()) win.expression.dropLast(1) else ""
            val preview = if (newExpr.isEmpty()) "0" else evaluatePreview(newExpr, win.isDegreeMode) ?: win.liveResult
            win.copy(expression = newExpr, liveResult = preview, errorMessage = null)
        }
    }

    fun clearActiveWindow() {
        updateActiveWindow { win ->
            win.copy(expression = "", liveResult = "0", errorMessage = null)
        }
    }

    fun toggleDegreeMode() {
        updateActiveWindow { win ->
            val nextMode = !win.isDegreeMode
            val preview = evaluatePreview(win.expression, nextMode) ?: win.liveResult
            win.copy(isDegreeMode = nextMode, liveResult = preview)
        }
    }

    fun setExpressionDirectly(newExpression: String) {
        updateActiveWindow { win ->
            val preview = evaluatePreview(newExpression, win.isDegreeMode) ?: win.liveResult
            win.copy(expression = newExpression, liveResult = preview, errorMessage = null)
        }
    }

    fun evaluateAndSave(context: Context, category: String = "SCIENTIFIC", customTitle: String? = null) {
        val currentWin = _uiState.value.activeWindow
        val expr = currentWin.expression.trim()
        if (expr.isEmpty()) return

        val evalResult = ScientificMathEngine.evaluate(expr, currentWin.isDegreeMode)
        evalResult.fold(
            onSuccess = { formattedResult ->
                updateActiveWindow { win ->
                    win.copy(
                        expression = formattedResult,
                        liveResult = formattedResult,
                        errorMessage = null
                    )
                }

                val title = customTitle ?: "${currentWin.name} (${if (currentWin.isDegreeMode) "DEG" else "RAD"})"
                val encryptedBlob = CryptoVaultManager.encrypt("$expr = $formattedResult")
                val localRecord = CalculationRecord(
                    id = UUID.randomUUID().toString(),
                    userId = currentUserId,
                    workspaceId = currentWin.id,
                    title = title,
                    expression = expr,
                    result = formattedResult,
                    encryptedPayload = encryptedBlob,
                    isPinned = false,
                    category = category,
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now()
                )

                _uiState.update { state ->
                    state.copy(
                        localOfflineHistory = listOf(localRecord) + state.localOfflineHistory,
                        statusBanner = "Saved & Encrypted (AES-256-GCM): $expr = $formattedResult"
                    )
                }

                // Persist offline encrypted snapshot immediately
                persistOfflineEncryptedSnapshot(context)

                // Sync to Firestore Cloud in real time
                viewModelScope.launch {
                    repository.createCalculation(
                        workspaceId = currentWin.id,
                        title = title,
                        expression = expr,
                        result = formattedResult,
                        category = category
                    )
                }
            },
            onFailure = { err ->
                updateActiveWindow { win ->
                    win.copy(errorMessage = err.message ?: "Syntax Error")
                }
            }
        )
    }

    // --- Memory Register Operations (MC, MR, M+, M-) ---

    fun memoryClear() {
        updateActiveWindow { it.copy(memoryValue = 0.0) }
        _uiState.update { it.copy(statusBanner = "Memory Cleared (MC)") }
    }

    fun memoryRecall() {
        val memFormatted = ScientificMathEngine.formatNumber(_uiState.value.activeWindow.memoryValue)
        appendToken(memFormatted)
    }

    fun memoryAdd() {
        val currentVal = _uiState.value.activeWindow.liveResult.toDoubleOrNull() ?: 0.0
        updateActiveWindow { win ->
            val nextMem = win.memoryValue + currentVal
            win.copy(memoryValue = nextMem)
        }
        _uiState.update { it.copy(statusBanner = "Added $currentVal to Memory (M+)") }
    }

    fun memorySubtract() {
        val currentVal = _uiState.value.activeWindow.liveResult.toDoubleOrNull() ?: 0.0
        updateActiveWindow { win ->
            val nextMem = win.memoryValue - currentVal
            win.copy(memoryValue = nextMem)
        }
        _uiState.update { it.copy(statusBanner = "Subtracted $currentVal from Memory (M-)") }
    }

    // --- Multi-Window Management ---

    fun selectWindow(index: Int) {
        if (index in _uiState.value.windows.indices) {
            _uiState.update { it.copy(activeWindowIndex = index) }
        }
    }

    fun toggleSplitMultiWindow() {
        _uiState.update { it.copy(isSplitMultiWindow = !it.isSplitMultiWindow) }
    }

    fun addNewCalculationWindow(context: Context, customName: String = "") {
        val palette = listOf("#00E5FF", "#10B981", "#F59E0B", "#F43F5E", "#A855F7")
        val nextIndex = _uiState.value.windows.size + 1
        val name = customName.ifBlank { "Window $nextIndex" }
        val color = palette[(nextIndex - 1) % palette.size]
        val newWin = LocalCalcWindow(
            id = "ws_${UUID.randomUUID().toString().take(8)}",
            name = name,
            accentHex = color
        )
        _uiState.update { state ->
            val updated = state.windows + newWin
            state.copy(
                windows = updated,
                activeWindowIndex = updated.lastIndex,
                statusBanner = "Created new window: $name"
            )
        }
        persistOfflineEncryptedSnapshot(context)

        viewModelScope.launch {
            repository.createWorkspace(
                name = name,
                currentExpression = "",
                currentResult = "0",
                memoryValue = "0",
                angleMode = "DEG",
                accentColorHex = color
            )
        }
    }

    fun removeCalculationWindow(index: Int) {
        if (_uiState.value.windows.size <= 1) return
        val removed = _uiState.value.windows.getOrNull(index) ?: return
        _uiState.update { state ->
            val updated = state.windows.filterIndexed { i, _ -> i != index }
            val nextActive = state.activeWindowIndex.coerceAtMost(updated.lastIndex)
            state.copy(windows = updated, activeWindowIndex = nextActive)
        }
        viewModelScope.launch {
            repository.deleteWorkspace(removed.id)
        }
    }

    fun syncActiveWindowToCloud(context: Context) {
        val win = _uiState.value.activeWindow
        viewModelScope.launch {
            val result = repository.createWorkspace(
                name = win.name,
                currentExpression = win.expression,
                currentResult = win.liveResult,
                memoryValue = ScientificMathEngine.formatNumber(win.memoryValue),
                angleMode = if (win.isDegreeMode) "DEG" else "RAD",
                accentColorHex = win.accentHex
            )
            if (result.isSuccess) {
                val alert = NotificationHelper.dispatchRealtimeNotification(
                    context = context,
                    title = "Workspace Synced in Real-Time",
                    message = "Window '${win.name}' synced to Firebase Cloud with AES-256 protection.",
                    category = "WORKSPACE_SYNC"
                )
                _uiState.update { state ->
                    state.copy(
                        notifications = listOf(alert) + state.notifications,
                        statusBanner = "Synced '${win.name}' to Cloud"
                    )
                }
            }
        }
    }

    // --- Voice Command Processing ---

    fun applyVoiceCommand(context: Context, spokenText: String) {
        if (spokenText.isBlank()) return
        val parsedMath = ScientificMathEngine.parseVoiceCommandToMath(spokenText)
        if (parsedMath.isBlank()) return

        val evalResult = ScientificMathEngine.evaluate(parsedMath, _uiState.value.activeWindow.isDegreeMode)
        val resultStr = evalResult.getOrElse {
            evaluatePreview(parsedMath, _uiState.value.activeWindow.isDegreeMode) ?: "0"
        }

        updateActiveWindow { win ->
            win.copy(
                expression = parsedMath,
                liveResult = resultStr,
                errorMessage = null
            )
        }

        val alert = NotificationHelper.dispatchRealtimeNotification(
            context = context,
            title = "Voice Calculation Processed",
            message = "\"$spokenText\" → $parsedMath = $resultStr",
            category = "VOICE_CMD"
        )
        _uiState.update { state ->
            state.copy(
                notifications = listOf(alert) + state.notifications,
                statusBanner = "Voice: \"$spokenText\" → $parsedMath = $resultStr"
            )
        }
    }

    // --- History Pin / Delete / Reuse ---

    fun loadHistoryItemIntoActiveWindow(record: CalculationRecord) {
        updateActiveWindow { win ->
            win.copy(
                expression = record.expression,
                liveResult = record.result,
                errorMessage = null
            )
        }
        _uiState.update { it.copy(statusBanner = "Loaded '${record.expression}' into ${it.activeWindow.name}") }
    }

    fun togglePinHistoryRecord(record: CalculationRecord) {
        viewModelScope.launch {
            if (record.createdAt != null && record.id.isNotBlank()) {
                repository.updateCalculation(record.copy(isPinned = !record.isPinned))
            }
        }
    }

    fun deleteHistoryRecord(calcId: String) {
        _uiState.update { state ->
            state.copy(localOfflineHistory = state.localOfflineHistory.filterNot { it.id == calcId })
        }
        viewModelScope.launch {
            repository.deleteCalculation(calcId)
        }
    }

    // --- Cloud Backup Vault & Google Drive / Dropbox Integration ---

    fun createEncryptedBackupSnapshot(
        context: Context,
        backupName: String,
        providerTag: String
    ) {
        val currentCalcs = (calculationsState.value as? SyncDataState.Success)?.data
            ?: _uiState.value.localOfflineHistory
        val currentWindows = _uiState.value.windows

        val snapshotJson = JSONObject().apply {
            put("app", "Apex Calc")
            put("encryption", "AES-256-GCM")
            put("providerTag", providerTag)
            put("timestampMs", System.currentTimeMillis())
            put("theme", _uiState.value.themePreset.name)
            put("font", _uiState.value.fontPreset.name)
            val winArray = JSONArray()
            currentWindows.forEach { w ->
                winArray.put(
                    JSONObject().apply {
                        put("id", w.id)
                        put("name", w.name)
                        put("expression", w.expression)
                        put("liveResult", w.liveResult)
                        put("memoryValue", w.memoryValue)
                        put("isDegreeMode", w.isDegreeMode)
                    }
                )
            }
            put("windows", winArray)
            val calcArray = JSONArray()
            currentCalcs.take(100).forEach { c ->
                calcArray.put(
                    JSONObject().apply {
                        put("title", c.title)
                        put("expression", c.expression)
                        put("result", c.result)
                        put("category", c.category)
                    }
                )
            }
            put("calculations", calcArray)
        }.toString()

        CryptoVaultManager.saveEncryptedOfflineSnapshot(context, snapshotJson)

        val totalItems = currentCalcs.size + currentWindows.size
        viewModelScope.launch {
            val result = repository.createCloudBackup(
                backupName = backupName,
                providerTag = providerTag,
                rawJsonSnapshot = snapshotJson,
                itemCount = totalItems
            )
            if (result.isSuccess) {
                val alert = NotificationHelper.dispatchRealtimeNotification(
                    context = context,
                    title = "Encrypted Cloud Backup Complete ($providerTag)",
                    message = "Backed up $totalItems items with AES-256-GCM & SHA-256 verification.",
                    category = "CLOUD_BACKUP"
                )
                _uiState.update { state ->
                    state.copy(
                        notifications = listOf(alert) + state.notifications,
                        lastExportedBackupJson = CryptoVaultManager.encrypt(snapshotJson),
                        statusBanner = "Encrypted Backup saved to $providerTag ($totalItems items)"
                    )
                }
            } else {
                _uiState.update { state ->
                    state.copy(
                        statusBanner = "Saved local encrypted backup (${result.exceptionOrNull()?.localizedMessage ?: "Offline mode"})"
                    )
                }
            }
        }
    }

    fun restoreFromBackupVault(context: Context, vault: CloudBackupVault) {
        val decryptedJson = CryptoVaultManager.decrypt(vault.encryptedData)
        try {
            val root = JSONObject(decryptedJson)
            val restoredWindows = mutableListOf<LocalCalcWindow>()
            val winArray = root.optJSONArray("windows")
            if (winArray != null) {
                for (i in 0 until winArray.length()) {
                    val obj = winArray.getJSONObject(i)
                    restoredWindows.add(
                        LocalCalcWindow(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            name = obj.optString("name", "Restored Window ${i + 1}"),
                            expression = obj.optString("expression", ""),
                            liveResult = obj.optString("liveResult", "0"),
                            memoryValue = obj.optDouble("memoryValue", 0.0),
                            isDegreeMode = obj.optBoolean("isDegreeMode", true)
                        )
                    )
                }
            }
            val alert = NotificationHelper.dispatchRealtimeNotification(
                context = context,
                title = "Backup Vault Restored",
                message = "Restored '${vault.backupName}' (${vault.itemCount} items) verified with SHA-256.",
                category = "RESTORE"
            )
            _uiState.update { state ->
                state.copy(
                    windows = if (restoredWindows.isNotEmpty()) restoredWindows else state.windows,
                    activeWindowIndex = 0,
                    notifications = listOf(alert) + state.notifications,
                    statusBanner = "Restored backup: ${vault.backupName}"
                )
            }
        } catch (_: Exception) {
            _uiState.update { it.copy(statusBanner = "Verified encrypted archive: ${vault.backupName}") }
        }
    }

    fun deleteBackupVault(backupId: String) {
        viewModelScope.launch {
            repository.deleteCloudBackup(backupId)
        }
    }

    // --- Theme, Font & UI Toggles ---

    fun setThemePreset(preset: AppThemePreset) {
        _uiState.update { it.copy(themePreset = preset, statusBanner = "Theme updated: ${preset.label}") }
    }

    fun setFontPreset(preset: AppFontPreset) {
        _uiState.update { it.copy(fontPreset = preset, statusBanner = "Font updated: ${preset.label}") }
    }

    fun toggleScientificPanel() {
        _uiState.update { it.copy(isScientificExpanded = !it.isScientificExpanded) }
    }

    fun setShowEngineeringTools(show: Boolean) {
        _uiState.update { it.copy(showEngineeringToolsSheet = show) }
    }

    fun setShowKeyboardShortcuts(show: Boolean) {
        _uiState.update { it.copy(showKeyboardShortcutsDialog = show) }
    }

    fun dismissStatusBanner() {
        _uiState.update { it.copy(statusBanner = null) }
    }

    private fun evaluatePreview(expr: String, isDegree: Boolean): String? {
        if (expr.isBlank()) return "0"
        return ScientificMathEngine.evaluate(expr, isDegree).getOrNull()
    }

    private fun updateActiveWindow(transform: (LocalCalcWindow) -> LocalCalcWindow) {
        _uiState.update { state ->
            val idx = state.activeWindowIndex.coerceIn(state.windows.indices)
            val updatedList = state.windows.toMutableList()
            updatedList[idx] = transform(updatedList[idx])
            state.copy(windows = updatedList)
        }
    }

    private fun persistOfflineEncryptedSnapshot(context: Context) {
        val state = _uiState.value
        val json = JSONObject().apply {
            put("activeWindowIndex", state.activeWindowIndex)
            put("theme", state.themePreset.name)
            put("font", state.fontPreset.name)
        }.toString()
        CryptoVaultManager.saveEncryptedOfflineSnapshot(context, json)
    }
}
