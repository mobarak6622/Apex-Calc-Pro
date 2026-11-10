package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.domain.ScientificMathEngine
import com.example.viewmodel.CalculatorUiState
import com.example.viewmodel.CalculatorViewModel
import com.example.viewmodel.LocalCalcWindow

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CalculatorWorkspaceScreen(
    uiState: CalculatorUiState,
    viewModel: CalculatorViewModel,
    onAskAiWithExpression: (String) -> Unit,
    onSpeakResult: (String) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activeWindow = uiState.activeWindow

    var showVoiceQuickSheet by remember { mutableStateOf(false) }
    var customVoiceInput by remember { mutableStateOf("") }

    // SpeechRecognizer Intent Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = matches?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                viewModel.applyVoiceCommand(context, spoken)
            }
        }
    }

    // Audio Permission Launcher for Voice Command
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak math expression (বাংলা বা English)")
                }
                speechLauncher.launch(intent)
            } catch (_: Exception) {
                showVoiceQuickSheet = true
            }
        } else {
            showVoiceQuickSheet = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when {
                        event.isCtrlPressed && event.key == Key.N -> {
                            viewModel.addNewCalculationWindow(context)
                            true
                        }
                        event.isCtrlPressed && event.key == Key.B -> {
                            viewModel.createEncryptedBackupSnapshot(context, "Shortcut Cloud Backup", "FIREBASE_CLOUD")
                            true
                        }
                        event.isCtrlPressed && event.key == Key.S -> {
                            viewModel.evaluateAndSave(context)
                            true
                        }
                        event.key == Key.Enter || event.key == Key.NumPadEnter -> {
                            viewModel.evaluateAndSave(context)
                            true
                        }
                        event.key == Key.Backspace -> {
                            viewModel.deleteLastChar()
                            true
                        }
                        event.key == Key.Escape -> {
                            viewModel.clearActiveWindow()
                            true
                        }
                        else -> {
                            val ch = event.utf16CodePoint.toChar()
                            if (ch in "0123456789+-*/^().%!") {
                                viewModel.appendToken(ch.toString())
                                true
                            } else {
                                false
                            }
                        }
                    }
                } else {
                    false
                }
            }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Multi-Window Switcher Bar + Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                uiState.windows.forEachIndexed { index, win ->
                    val isSelected = index == uiState.activeWindowIndex
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectWindow(index) },
                        label = {
                            Text(
                                text = win.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        trailingIcon = if (uiState.windows.size > 1 && isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Close Window",
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { viewModel.removeCalculationWindow(index) }
                                )
                            }
                        } else null,
                        modifier = Modifier.testTag("window_tab_$index")
                    )
                }

                IconButton(
                    onClick = { viewModel.addNewCalculationWindow(context) },
                    modifier = Modifier.testTag("quick_add_window_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "New Calculation Window",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (uiState.windows.size >= 2) {
                    IconButton(
                        onClick = { viewModel.toggleSplitMultiWindow() },
                        modifier = Modifier.testTag("split_window_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ViewColumn,
                            contentDescription = "Split Multi-Window View",
                            tint = if (uiState.isSplitMultiWindow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = { viewModel.setShowKeyboardShortcuts(true) },
                    modifier = Modifier.testTag("keyboard_shortcuts_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Keyboard,
                        contentDescription = "Keyboard Shortcuts",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Optional Split Multi-Window Comparison Bar
        AnimatedVisibility(visible = uiState.isSplitMultiWindow && uiState.windows.size >= 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.windows.take(2).forEachIndexed { idx, win ->
                    MiniWindowPreviewCard(
                        window = win,
                        isActive = idx == uiState.activeWindowIndex,
                        onClick = { viewModel.selectWindow(idx) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 2. Main Display Console
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(22.dp)
                ),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Top Telemetry Row inside Display
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
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .clickable { viewModel.toggleDegreeMode() }
                                .testTag("deg_rad_button")
                        ) {
                            Text(
                                text = if (activeWindow.isDegreeMode) "DEG" else "RAD",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "AES-256",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        if (activeWindow.memoryValue != 0.0) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "M=${ScientificMathEngine.formatNumber(activeWindow.memoryValue)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Voice Command Button
                        IconButton(
                            onClick = {
                                val hasPerm = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm) {
                                    showVoiceQuickSheet = true
                                } else {
                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier.testTag("voice_command_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Mic,
                                contentDescription = "Voice Command Input",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Gemini TTS Speak Result Button
                        IconButton(
                            onClick = {
                                val speechTarget = if (activeWindow.expression.isNotBlank()) {
                                    "${activeWindow.expression} equals ${activeWindow.liveResult}"
                                } else {
                                    activeWindow.liveResult
                                }
                                onSpeakResult(speechTarget)
                            },
                            modifier = Modifier.testTag("tts_speak_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Speak Result Aloud",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }

                        // Ask Gemini 4 Argon Button
                        IconButton(
                            onClick = {
                                onAskAiWithExpression(activeWindow.expression.ifBlank { activeWindow.liveResult })
                            },
                            modifier = Modifier.testTag("ask_ai_solver_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Psychology,
                                contentDescription = "Analyze with Gemini AI",
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }

                // Expression Input Display
                Text(
                    text = activeWindow.expression.ifEmpty { "0" },
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calculator_expression_display")
                )

                // Live Evaluated Result or Error
                if (activeWindow.errorMessage != null) {
                    Text(
                        text = activeWindow.errorMessage,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = "= ${activeWindow.liveResult}",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("calculator_result_display")
                    )
                }
            }
        }

        // 3. Memory, Calculus Lab & Cloud Sync Action Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmallActionChip("MC", "mem_mc") { viewModel.memoryClear() }
            SmallActionChip("MR", "mem_mr") { viewModel.memoryRecall() }
            SmallActionChip("M+", "mem_m_plus") { viewModel.memoryAdd() }
            SmallActionChip("M-", "mem_m_minus") { viewModel.memorySubtract() }

            AssistChip(
                onClick = { viewModel.setShowEngineeringTools(true) },
                label = { Text("∫dx / Matrix / Roots") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Functions,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                modifier = Modifier.testTag("engineering_tools_button")
            )

            AssistChip(
                onClick = { viewModel.syncActiveWindowToCloud(context) },
                label = { Text("Cloud Sync") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                modifier = Modifier.testTag("sync_window_cloud_button")
            )

            IconButton(
                onClick = { viewModel.toggleScientificPanel() },
                modifier = Modifier.testTag("toggle_scientific_button")
            ) {
                Icon(
                    imageVector = if (uiState.isScientificExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = "Toggle Scientific Keypad"
                )
            }
        }

        // 4. Expandable Scientific Function Keypad
        AnimatedVisibility(visible = uiState.isScientificExpanded) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val sciRows = listOf(
                    listOf("sin(" to "sin", "cos(" to "cos", "tan(" to "tan", "ln(" to "ln", "log(" to "log"),
                    listOf("asin(" to "sin⁻¹", "acos(" to "cos⁻¹", "atan(" to "tan⁻¹", "sqrt(" to "√x", "cbrt(" to "∛x"),
                    listOf("^" to "xʸ", "!" to "n!", "pi" to "π", "e" to "e", "abs(" to "|x|"),
                    listOf("nCr(" to "nCr", "nPr(" to "nPr", "gcd(" to "gcd", "lcm(" to "lcm", "," to ",")
                )
                sciRows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        row.forEach { (token, label) ->
                            Surface(
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    viewModel.appendToken(token)
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("sci_key_$label")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Primary Tactile Keypad Grid (Fills remaining height cleanly)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val mainRows = listOf(
                listOf("AC", "(", ")", "%", "÷"),
                listOf("7", "8", "9", "×", "⌫"),
                listOf("4", "5", "6", "-", "^2"),
                listOf("1", "2", "3", "+", "="),
                listOf("0", ".", "00", "SAVE", "AI")
            )

            mainRows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    row.forEach { keyLabel ->
                        val isEquals = keyLabel == "="
                        val isClear = keyLabel == "AC"
                        val isOperator = keyLabel in listOf("÷", "×", "-", "+", "%", "(", ")", "^2")
                        val isSpecial = keyLabel in listOf("SAVE", "AI", "⌫")

                        val containerColor = when {
                            isEquals -> MaterialTheme.colorScheme.primary
                            isClear -> MaterialTheme.colorScheme.errorContainer
                            isOperator -> MaterialTheme.colorScheme.primaryContainer
                            isSpecial -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.surface
                        }

                        val contentColor = when {
                            isEquals -> MaterialTheme.colorScheme.onPrimary
                            isClear -> MaterialTheme.colorScheme.onErrorContainer
                            isOperator -> MaterialTheme.colorScheme.primary
                            isSpecial -> MaterialTheme.colorScheme.onSecondaryContainer
                            else -> MaterialTheme.colorScheme.onSurface
                        }

                        Surface(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                when (keyLabel) {
                                    "AC" -> viewModel.clearActiveWindow()
                                    "⌫" -> viewModel.deleteLastChar()
                                    "=" -> viewModel.evaluateAndSave(context)
                                    "SAVE" -> viewModel.evaluateAndSave(context, category = "PINNED_VAULT")
                                    "AI" -> onAskAiWithExpression(activeWindow.expression.ifBlank { activeWindow.liveResult })
                                    else -> viewModel.appendToken(keyLabel)
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = containerColor,
                            tonalElevation = 3.dp,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .testTag("calc_key_$keyLabel")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                when (keyLabel) {
                                    "⌫" -> Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = "Backspace",
                                        tint = contentColor
                                    )
                                    "SAVE" -> Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Save,
                                            contentDescription = "Save to Encrypted History",
                                            modifier = Modifier.size(16.dp),
                                            tint = contentColor
                                        )
                                        Text(
                                            text = "Save",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = contentColor
                                        )
                                    }
                                    else -> Text(
                                        text = keyLabel,
                                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = contentColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Voice Command Modal Sheet (Supports Hardware Mic + Instant Bengali & English Voice Presets)
    if (showVoiceQuickSheet) {
        ModalBottomSheet(
            onDismissRequest = { showVoiceQuickSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "🎙️ Voice Command Calculator (ভয়েস কমান্ড হিসাব)",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Speak naturally in Bengali or English, or tap a preset voice phrase below:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customVoiceInput,
                        onValueChange = { customVoiceInput = it },
                        label = { Text("Spoken command (e.g. পাঁচ গুণ দশ যোগ বিশ)") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("voice_custom_input")
                    )
                    Button(
                        onClick = {
                            viewModel.applyVoiceCommand(context, customVoiceInput)
                            showVoiceQuickSheet = false
                        },
                        modifier = Modifier.testTag("apply_voice_command_button")
                    ) {
                        Text("Run")
                    }
                }

                Button(
                    onClick = {
                        try {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak math expression...")
                            }
                            speechLauncher.launch(intent)
                            showVoiceQuickSheet = false
                        } catch (_: Exception) {
                            viewModel.applyVoiceCommand(context, "square root of 144 plus 25 multiplied by 4")
                            showVoiceQuickSheet = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Launch System Microphone")
                }

                Text(
                    text = "Quick Voice Command Presets (এক ট্যাপে ভয়েস টেস্ট):",
                    style = MaterialTheme.typography.labelLarge
                )

                val presets = listOf(
                    "পাঁচ গুণ দশ যোগ বিশ",
                    "বর্গমূল 144 যোগ ২৫ গুণ ৪",
                    "square root of 256 plus sine 30",
                    "25 percent of 1200 plus 5 factorial",
                    "cosine 0 multiplied by pi"
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { phrase ->
                        FilledTonalButton(
                            onClick = {
                                viewModel.applyVoiceCommand(context, phrase)
                                showVoiceQuickSheet = false
                            }
                        ) {
                            Text(phrase, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Engineering, Calculus & Matrix Tools Bottom Sheet
    if (uiState.showEngineeringToolsSheet) {
        EngineeringToolsBottomSheet(
            isDegreeMode = activeWindow.isDegreeMode,
            onInsertResult = { expr, res, category ->
                viewModel.setExpressionDirectly(expr)
                viewModel.evaluateAndSave(context, category = category, customTitle = "$category: $expr = $res")
                viewModel.setShowEngineeringTools(false)
            },
            onDismiss = { viewModel.setShowEngineeringTools(false) }
        )
    }

    // Keyboard Shortcuts Dialog
    if (uiState.showKeyboardShortcutsDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowKeyboardShortcuts(false) },
            title = { Text("⌨️ Hardware & On-Screen Keyboard Shortcuts") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShortcutRow("0 - 9, +, -, *, /, ^, (, )", "Direct Expression Input")
                    ShortcutRow("Enter / =", "Evaluate & Save to Encrypted History")
                    ShortcutRow("Backspace", "Delete Last Character")
                    ShortcutRow("Escape (Esc)", "Clear Active Calculation Window")
                    ShortcutRow("Ctrl + N", "Open New Calculation Window")
                    ShortcutRow("Ctrl + S", "Save & Encrypt Current Calculation")
                    ShortcutRow("Ctrl + B", "Trigger Instant Encrypted Cloud Backup")
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.setShowKeyboardShortcuts(false) }) {
                    Text("Got It")
                }
            }
        )
    }
}

@Composable
private fun MiniWindowPreviewCard(
    window: LocalCalcWindow,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.border(
            width = if (isActive) 2.dp else 1.dp,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
            shape = RoundedCornerShape(14.dp)
        ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = window.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
            Text(
                text = window.expression.ifBlank { "0" },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "= ${window.liveResult}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SmallActionChip(
    label: String,
    tag: String,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        modifier = Modifier
            .height(34.dp)
            .testTag(tag),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ShortcutRow(keys: String, desc: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = keys,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = desc,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EngineeringToolsBottomSheet(
    isDegreeMode: Boolean,
    onInsertResult: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedToolTab by remember { mutableStateOf(0) }

    // Calculus state
    var funcExpr by remember { mutableStateOf("x^2 + 2*x") }
    var lowerA by remember { mutableStateOf("0") }
    var upperB by remember { mutableStateOf("3") }
    var calcOutput by remember { mutableStateOf("") }

    // Quadratic state
    var quadA by remember { mutableStateOf("1") }
    var quadB by remember { mutableStateOf("-5") }
    var quadC by remember { mutableStateOf("6") }
    var quadOutput by remember { mutableStateOf("") }

    // Matrix 2x2 state
    var m11 by remember { mutableStateOf("4") }
    var m12 by remember { mutableStateOf("7") }
    var m21 by remember { mutableStateOf("2") }
    var m22 by remember { mutableStateOf("6") }
    var matrixOutput by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "🔬 Scientific & Engineering Solver Suite",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedToolTab == 0,
                    onClick = { selectedToolTab = 0 },
                    label = { Text("∫dx & d/dx") }
                )
                FilterChip(
                    selected = selectedToolTab == 1,
                    onClick = { selectedToolTab = 1 },
                    label = { Text("Quadratic Roots") }
                )
                FilterChip(
                    selected = selectedToolTab == 2,
                    onClick = { selectedToolTab = 2 },
                    label = { Text("2×2 Matrix") }
                )
            }

            when (selectedToolTab) {
                0 -> {
                    OutlinedTextField(
                        value = funcExpr,
                        onValueChange = { funcExpr = it },
                        label = { Text("Function f(x) (e.g. x^2 + sin(x))") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = lowerA,
                            onValueChange = { lowerA = it },
                            label = { Text("Lower limit a / x₀") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = upperB,
                            onValueChange = { upperB = it },
                            label = { Text("Upper limit b") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val a = lowerA.toDoubleOrNull() ?: 0.0
                                val b = upperB.toDoubleOrNull() ?: 1.0
                                val res = ScientificMathEngine.definiteIntegral(funcExpr, a, b, isDegreeMode)
                                calcOutput = res.getOrElse { "Error: ${it.message}" }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Evaluate ∫_a^b f(x)dx")
                        }
                        OutlinedButton(
                            onClick = {
                                val x0 = lowerA.toDoubleOrNull() ?: 1.0
                                val res = ScientificMathEngine.derivativeAt(funcExpr, x0, isDegreeMode)
                                calcOutput = res.getOrElse { "Error: ${it.message}" }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Derivative f'(a)")
                        }
                    }
                    if (calcOutput.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Result = $calcOutput", style = MaterialTheme.typography.titleMedium)
                                TextButton(
                                    onClick = {
                                        onInsertResult(calcOutput, calcOutput, "CALCULUS")
                                    }
                                ) {
                                    Text("Use in Calc")
                                }
                            }
                        }
                    }
                }
                1 -> {
                    Text("Solve ax² + bx + c = 0 (Real & Complex Roots)", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = quadA,
                            onValueChange = { quadA = it },
                            label = { Text("a") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = quadB,
                            onValueChange = { quadB = it },
                            label = { Text("b") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = quadC,
                            onValueChange = { quadC = it },
                            label = { Text("c") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Button(
                        onClick = {
                            val a = quadA.toDoubleOrNull() ?: 1.0
                            val b = quadB.toDoubleOrNull() ?: 0.0
                            val c = quadC.toDoubleOrNull() ?: 0.0
                            quadOutput = ScientificMathEngine.solveQuadratic(a, b, c)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Solve Roots")
                    }
                    if (quadOutput.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = quadOutput,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
                2 -> {
                    Text("2×2 Matrix Determinant, Trace & Inverse", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = m11, onValueChange = { m11 = it }, label = { Text("a₁₁") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = m12, onValueChange = { m12 = it }, label = { Text("a₁₂") }, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = m21, onValueChange = { m21 = it }, label = { Text("a₂₁") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = m22, onValueChange = { m22 = it }, label = { Text("a₂₂") }, modifier = Modifier.weight(1f))
                    }
                    Button(
                        onClick = {
                            matrixOutput = ScientificMathEngine.analyzeMatrix2x2(
                                m11.toDoubleOrNull() ?: 0.0,
                                m12.toDoubleOrNull() ?: 0.0,
                                m21.toDoubleOrNull() ?: 0.0,
                                m22.toDoubleOrNull() ?: 0.0
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Analyze Matrix A")
                    }
                    if (matrixOutput.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = matrixOutput,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
