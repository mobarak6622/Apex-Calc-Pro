package com.example.viewmodel

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.util.NotificationHelper
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.Locale
import java.util.UUID

private const val TAG = "AiMathLabVM"

enum class AiModelMode(
    val id: String,
    val modelName: String,
    val label: String,
    val bnSubtitle: String
) {
    HIGH_THINKING(
        id = "HIGH_THINKING",
        modelName = "gemini-4-argon",
        label = "Deep Thinking (gemini-4-argon)",
        bnSubtitle = "জটিল গাণিতিক প্রমাণ, ক্যালকুলাস ও গভীর বিশ্লেষণের জন্য (ThinkingLevel.HIGH)"
    ),
    SEARCH_GROUNDED(
        id = "SEARCH_GROUNDED",
        modelName = "gemini-3.8-flash",
        label = "Search Grounded (gemini-3.8-flash)",
        bnSubtitle = "গুগল সার্চ ডেটা, লাইভ কারেন্সি ও বৈজ্ঞানিক তথ্যের জন্য"
    ),
    FAST_LITE(
        id = "FAST_LITE",
        modelName = "gemini-3.1-flash-lite",
        label = "Fast Solver (gemini-3.1-flash-lite)",
        bnSubtitle = "দ্রুত সূত্র ও তাৎক্ষণিক হিসাবের জন্য"
    )
}

data class ChatMessageItem(
    val id: String = UUID.randomUUID().toString(),
    val role: String, // "user" or "model"
    val text: String,
    val modelBadge: String = "",
    val extractedFormula: String? = null,
    val isStreaming: Boolean = false
)

sealed interface AiLabUiState {
    data object Idle : AiLabUiState
    data object Loading : AiLabUiState
    data class Streaming(val partialText: String) : AiLabUiState
    data class Success(val responseText: String) : AiLabUiState
    data class Error(val message: String) : AiLabUiState
}

class AiMathLabViewModel : ViewModel() {

    private val systemPromptContent: Content by lazy {
        content {
            text(
                "You are Apex Calc AI Professor, an expert mathematician, physicist, and scientific calculator assistant. " +
                    "You answer in clear Bengali and English as requested by the user. " +
                    "When solving mathematical or scientific problems, show clear step-by-step derivations, " +
                    "highlight the final numerical answer clearly on a line starting with 'RESULT: ', " +
                    "and provide a calculator-ready expression on a line starting with 'EXPR: ' when applicable."
            )
        }
    }

    // 1. High-Thinking Complex Math Model (gemini-4-argon with ThinkingLevel.HIGH, no maxOutputTokens)
    private val highThinkingModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-4-argon",
            generationConfig = generationConfig {
                thinkingConfig = thinkingConfig {
                    thinkingLevel = ThinkingLevel.HIGH
                }
            },
            systemInstruction = systemPromptContent
        )
    }

    // 2. General + Google Search Grounded Model (gemini-3.8-flash with googleSearch tool)
    private val searchGroundedModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            tools = listOf(Tool.googleSearch()),
            systemInstruction = systemPromptContent
        )
    }

    // 3. Fast Lite Model (gemini-3.1-flash-lite)
    private val fastLiteModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.1-flash-lite",
            systemInstruction = systemPromptContent
        )
    }

    // 4. Gemini Text-to-Speech Model (gemini-3.8-flash-tts)
    private val geminiTtsModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash-tts"
        )
    }

    private val _selectedMode = MutableStateFlow(AiModelMode.HIGH_THINKING)
    val selectedMode: StateFlow<AiModelMode> = _selectedMode.asStateFlow()

    private val _uiState = MutableStateFlow<AiLabUiState>(AiLabUiState.Idle)
    val uiState: StateFlow<AiLabUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessageItem>>(
        listOf(
            ChatMessageItem(
                role = "model",
                text = "স্বাগতম! আমি **Apex Calc AI Math & Science Lab**।\n" +
                    "• **Deep Thinking (`gemini-4-argon`)**: ক্যালকুলাস, ম্যাট্রিক্স, ডিফারেনশিয়াল সমীকরণ ও জটিল গাণিতিক প্রমাণের জন্য।\n" +
                    "• **Google Search (`gemini-3.8-flash`)**: লাইভ মুদ্রা বিনিময় হার ও হালনাগাদ বৈজ্ঞানিক তথ্যের জন্য।\n" +
                    "• **Fast Solver (`gemini-3.1-flash-lite`)**: দ্রুত সূত্র ও শর্টকাট সমাধানের জন্য।\n\n" +
                    "যেকোনো গাণিতিক প্রশ্ন লিখুন বা আপনার ক্যালকুলেটরের সমীকরণ বিশ্লেষণ করুন!",
                modelBadge = "System Ready"
            )
        )
    )
    val messages: StateFlow<List<ChatMessageItem>> = _messages.asStateFlow()

    private val _ttsStatus = MutableStateFlow<String?>(null)
    val ttsStatus: StateFlow<String?> = _ttsStatus.asStateFlow()

    private var androidTts: TextToSpeech? = null

    fun selectModelMode(mode: AiModelMode) {
        _selectedMode.value = mode
    }

    fun sendChatMessage(context: Context, userPrompt: String, activeCalcExpression: String = "") {
        val trimmed = userPrompt.trim()
        if (trimmed.isEmpty()) return

        val mode = _selectedMode.value
        val userMessage = ChatMessageItem(
            role = "user",
            text = trimmed,
            modelBadge = "You"
        )
        val placeholderId = UUID.randomUUID().toString()
        val streamingPlaceholder = ChatMessageItem(
            id = placeholderId,
            role = "model",
            text = "Thinking with ${mode.modelName}...",
            modelBadge = mode.label,
            isStreaming = true
        )

        // Snapshot prior conversation history for multi-turn startChat
        val priorHistory = _messages.value
            .drop(1) // skip welcome banner
            .map { msg ->
                content(role = if (msg.role == "user") "user" else "model") {
                    text(msg.text)
                }
            }

        _messages.update { it + userMessage + streamingPlaceholder }
        _uiState.value = AiLabUiState.Loading

        viewModelScope.launch {
            val accumulated = StringBuilder()
            try {
                val activeModel = when (mode) {
                    AiModelMode.HIGH_THINKING -> highThinkingModel
                    AiModelMode.SEARCH_GROUNDED -> searchGroundedModel
                    AiModelMode.FAST_LITE -> fastLiteModel
                }

                val contextualPrompt = if (activeCalcExpression.isNotBlank()) {
                    "$trimmed\n(Current calculator workspace expression: $activeCalcExpression)"
                } else {
                    trimmed
                }

                val chat = activeModel.startChat(history = priorHistory)

                withTimeout(60_000L) {
                    chat.sendMessageStream(contextualPrompt).collect { chunk ->
                        val partText = chunk.text.orEmpty()
                        if (partText.isNotEmpty()) {
                            accumulated.append(partText)
                            val currentText = accumulated.toString()
                            _uiState.value = AiLabUiState.Streaming(currentText)
                            updateMessageById(placeholderId) {
                                it.copy(text = currentText, isStreaming = true)
                            }
                        }
                    }
                }

                val finalText = accumulated.toString().ifBlank {
                    val unaryResponse = withTimeout(60_000L) {
                        chat.sendMessage(contextualPrompt)
                    }
                    unaryResponse.text.orEmpty()
                }

                if (finalText.isNotBlank()) {
                    val extractedExpr = extractFormulaFromResponse(finalText)
                    updateMessageById(placeholderId) {
                        it.copy(
                            text = finalText,
                            extractedFormula = extractedExpr,
                            isStreaming = false
                        )
                    }
                    _uiState.value = AiLabUiState.Success(finalText)

                    if (mode == AiModelMode.HIGH_THINKING) {
                        NotificationHelper.dispatchRealtimeNotification(
                            context = context,
                            title = "Gemini 4 Argon Analysis Ready",
                            message = finalText.lineSequence().firstOrNull()?.take(90) ?: "Step-by-step solution completed.",
                            category = "AI_SOLVER"
                        )
                    }
                } else {
                    val errMsg = "Model returned an empty response."
                    updateMessageById(placeholderId) {
                        it.copy(text = errMsg, isStreaming = false)
                    }
                    _uiState.value = AiLabUiState.Error(errMsg)
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI generation timed out", e)
                val msg = "Generation timed out after 60 seconds. Please try again."
                updateMessageById(placeholderId) { it.copy(text = "⚠️ $msg", isStreaming = false) }
                _uiState.value = AiLabUiState.Error(msg)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Firebase AI generation failed", e)
                val msg = e.localizedMessage ?: "AI generation failed."
                updateMessageById(placeholderId) { it.copy(text = "⚠️ $msg", isStreaming = false) }
                _uiState.value = AiLabUiState.Error(msg)
            }
        }
    }

    /**
     * Converts text/math result to speech using gemini-3.8-flash-tts via Firebase AI Logic,
     * with seamless fallback to Android's on-device TextToSpeech engine.
     */
    fun speakTextWithGeminiTts(context: Context, textToSpeak: String) {
        val cleanText = textToSpeak
            .replace("*", " times ")
            .replace("/", " divided by ")
            .replace("^", " to the power of ")
            .replace("sqrt", " square root of ")
            .take(400)

        if (cleanText.isBlank()) return
        _ttsStatus.value = "Synthesizing audio with gemini-3.8-flash-tts..."

        viewModelScope.launch {
            var playedCloudAudio = false
            try {
                val response = withTimeout(30_000L) {
                    geminiTtsModel.generateContent(
                        content {
                            text("Read this mathematical result clearly and concisely: $cleanText")
                        }
                    )
                }
                val inlineAudioBytes = response.candidates
                    .firstOrNull()
                    ?.content
                    ?.parts
                    ?.firstNotNullOfOrNull { part ->
                        // Check if inline data / audio bytes are returned
                        runCatching {
                            val method = part.javaClass.methods.firstOrNull { it.name == "getInlineData" || it.name == "getAudio" }
                            val blob = method?.invoke(part)
                            val bytesMethod = blob?.javaClass?.methods?.firstOrNull { it.name == "getData" || it.name == "getBytes" }
                            bytesMethod?.invoke(blob) as? ByteArray
                        }.getOrNull()
                    }

                if (inlineAudioBytes != null && inlineAudioBytes.size > 100) {
                    playedCloudAudio = playRawPcmAudio(inlineAudioBytes)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "gemini-3.8-flash-tts cloud audio fallback to Android TTS: ${e.message}")
            }

            if (!playedCloudAudio) {
                speakWithAndroidTts(context, cleanText)
            } else {
                _ttsStatus.value = "Played via gemini-3.8-flash-tts"
            }
        }
    }

    private suspend fun playRawPcmAudio(pcmBytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
        try {
            val sampleRate = 24000
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (minBuf <= 0) return@withContext false
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minBuf, pcmBytes.size))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(pcmBytes, 0, pcmBytes.size)
            track.play()
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun speakWithAndroidTts(context: Context, text: String) {
        try {
            if (androidTts == null) {
                androidTts = TextToSpeech(context.applicationContext) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        androidTts?.language = Locale.US
                        androidTts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "apex_tts_utt")
                        _ttsStatus.value = "Speaking aloud: \"$text\""
                    } else {
                        _ttsStatus.value = "Audio readouts ready: $text"
                    }
                }
            } else {
                androidTts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "apex_tts_utt")
                _ttsStatus.value = "Speaking aloud: \"$text\""
            }
        } catch (e: Exception) {
            Log.e(TAG, "TTS error", e)
            _ttsStatus.value = "TTS: $text"
        }
    }

    fun clearTtsStatus() {
        _ttsStatus.value = null
    }

    fun clearConversation() {
        _messages.value = _messages.value.take(1)
        _uiState.value = AiLabUiState.Idle
    }

    private fun updateMessageById(id: String, transform: (ChatMessageItem) -> ChatMessageItem) {
        _messages.update { list ->
            list.map { if (it.id == id) transform(it) else it }
        }
    }

    private fun extractFormulaFromResponse(text: String): String? {
        val exprLine = text.lineSequence()
            .firstOrNull { it.trim().startsWith("EXPR:", ignoreCase = true) }
            ?.substringAfter(":")
            ?.trim()
            ?.removeSurrounding("`")
        if (!exprLine.isNullOrBlank()) return exprLine

        val resLine = text.lineSequence()
            .firstOrNull { it.trim().startsWith("RESULT:", ignoreCase = true) }
            ?.substringAfter(":")
            ?.trim()
            ?.removeSurrounding("`")
        return resLine?.takeIf { it.isNotBlank() }
    }

    override fun onCleared() {
        super.onCleared()
        androidTts?.shutdown()
        androidTts = null
    }
}
