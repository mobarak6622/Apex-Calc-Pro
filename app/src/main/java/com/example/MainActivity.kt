package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Window
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.CalculatorRepository
import com.example.ui.screens.AiMathLabScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CalculatorWorkspaceScreen
import com.example.ui.screens.HistoryAndVaultScreen
import com.example.ui.screens.MultiWindowManagerScreen
import com.example.ui.screens.signOutUser
import com.example.ui.theme.ApexCalcTheme
import com.example.util.configureAppCheck
import com.example.viewmodel.AiMathLabViewModel
import com.example.viewmodel.CalculatorViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI, ViewModels, or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            AppRoot()
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppRoot(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        ApexCalcTheme {
            AuthScreen(
                onAuthSuccess = {}
            )
        }
    } else {
        val uid = user.uid
        key(uid) {
            AuthenticatedCalculatorApp(
                currentUserId = uid,
                userEmail = user.email ?: "Encrypted Cloud User"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedCalculatorApp(
    currentUserId: String,
    userEmail: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val calcViewModel: CalculatorViewModel = viewModel(
        key = "calc_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                CalculatorViewModel(CalculatorRepository(db), currentUserId)
            }
        }
    )

    val aiViewModel: AiMathLabViewModel = viewModel(
        key = "ai_$currentUserId"
    )

    val uiState by calcViewModel.uiState.collectAsStateWithLifecycle()
    val calculationsState by calcViewModel.calculationsState.collectAsStateWithLifecycle()
    val workspacesState by calcViewModel.workspacesState.collectAsStateWithLifecycle()
    val backupsState by calcViewModel.backupsState.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableIntStateOf(0) }

    if (currentTab != 0) {
        BackHandler {
            currentTab = 0
        }
    }

    ApexCalcTheme(
        themePreset = uiState.themePreset,
        fontPreset = uiState.fontPreset
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Apex Calc Pro",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$userEmail • ${uiState.activeWindow.name}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                signOutUser(
                                    credentialManager = credentialManager,
                                    scope = scope
                                )
                            },
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Sign Out"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        icon = { Icon(Icons.Filled.Calculate, contentDescription = null) },
                        label = { Text("Calculator") },
                        modifier = Modifier.testTag("nav_calculator")
                    )
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        icon = { Icon(Icons.Filled.Window, contentDescription = null) },
                        label = { Text("Windows (${uiState.windows.size})") },
                        modifier = Modifier.testTag("nav_windows")
                    )
                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 },
                        icon = { Icon(Icons.Filled.EnhancedEncryption, contentDescription = null) },
                        label = { Text("Vault & Sync") },
                        modifier = Modifier.testTag("nav_vault")
                    )
                    NavigationBarItem(
                        selected = currentTab == 3,
                        onClick = { currentTab = 3 },
                        icon = { Icon(Icons.Filled.Psychology, contentDescription = null) },
                        label = { Text("AI Math Lab") },
                        modifier = Modifier.testTag("nav_ai_lab")
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Status / Notification Banner
                AnimatedVisibility(visible = uiState.statusBanner != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = uiState.statusBanner ?: "",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { calcViewModel.dismissStatusBanner() }) {
                                Text("OK")
                            }
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when (currentTab) {
                        0 -> CalculatorWorkspaceScreen(
                            uiState = uiState,
                            viewModel = calcViewModel,
                            onAskAiWithExpression = { expr ->
                                currentTab = 3
                                aiViewModel.sendChatMessage(
                                    context = context,
                                    userPrompt = "Solve and explain step-by-step: $expr",
                                    activeCalcExpression = expr
                                )
                            },
                            onSpeakResult = { textToSpeak ->
                                aiViewModel.speakTextWithGeminiTts(context, textToSpeak)
                            }
                        )
                        1 -> MultiWindowManagerScreen(
                            uiState = uiState,
                            cloudWorkspacesState = workspacesState,
                            viewModel = calcViewModel,
                            onSwitchToCalculatorTab = { currentTab = 0 }
                        )
                        2 -> HistoryAndVaultScreen(
                            uiState = uiState,
                            calculationsState = calculationsState,
                            backupsState = backupsState,
                            viewModel = calcViewModel,
                            onLoadRecordToCalc = { record ->
                                calcViewModel.loadHistoryItemIntoActiveWindow(record)
                                currentTab = 0
                            },
                            onSpeakRecord = { textToSpeak ->
                                aiViewModel.speakTextWithGeminiTts(context, textToSpeak)
                            }
                        )
                        3 -> AiMathLabScreen(
                            aiViewModel = aiViewModel,
                            activeCalcExpression = uiState.activeWindow.expression,
                            onInsertFormulaIntoCalculator = { formula ->
                                calcViewModel.setExpressionDirectly(formula)
                                currentTab = 0
                            }
                        )
                    }
                }
            }
        }
    }
}
