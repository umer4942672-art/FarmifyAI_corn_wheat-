package com.example.util

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class VoiceAssistantHelper(private val context: Context) {

    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _currentSpeakingId = MutableStateFlow<String?>(null)
    val currentSpeakingId: StateFlow<String?> = _currentSpeakingId.asStateFlow()

    private var pendingSpeech: Triple<String, Boolean, String>? = null

    init {
        initializeTts()
    }

    private fun initializeTts() {
        try {
            textToSpeech = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isInitialized = true
                    setupTtsSettings()
                    
                    // Execute queued speech if user tapped audio before init completed
                    pendingSpeech?.let { (text, isUrdu, utteranceId) ->
                        pendingSpeech = null
                        speak(text, isUrdu, utteranceId)
                    }
                } else {
                    isInitialized = false
                }
            }
        } catch (e: Exception) {
            isInitialized = false
        }
    }

    private fun setupTtsSettings() {
        val tts = textToSpeech ?: return
        try {
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                    _currentSpeakingId.value = utteranceId
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    _currentSpeakingId.value = null
                }

                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    _currentSpeakingId.value = null
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                    _currentSpeakingId.value = null
                }
            })

            tts.setSpeechRate(0.92f)
            tts.setPitch(1.0f)
        } catch (e: Exception) {
            // ignore
        }
    }

    fun speak(text: String, isUrdu: Boolean = false, utteranceId: String = "kisan_speech") {
        if (text.isBlank()) return

        if (_isSpeaking.value && _currentSpeakingId.value == utteranceId) {
            stop()
            return
        }

        if (!isInitialized || textToSpeech == null) {
            pendingSpeech = Triple(text, isUrdu, utteranceId)
            // Retry init if null
            if (textToSpeech == null) {
                initializeTts()
            }
            return
        }

        try {
            stop()

            val tts = textToSpeech ?: return

            if (isUrdu) {
                val urduPk = Locale("ur", "PK")
                val urduGen = Locale("ur")
                val hindi = Locale("hi", "IN")

                val resPk = try { tts.isLanguageAvailable(urduPk) } catch (e: Exception) { TextToSpeech.LANG_NOT_SUPPORTED }
                if (resPk >= TextToSpeech.LANG_AVAILABLE) {
                    tts.language = urduPk
                } else {
                    val resGen = try { tts.isLanguageAvailable(urduGen) } catch (e: Exception) { TextToSpeech.LANG_NOT_SUPPORTED }
                    if (resGen >= TextToSpeech.LANG_AVAILABLE) {
                        tts.language = urduGen
                    } else {
                        val resHi = try { tts.isLanguageAvailable(hindi) } catch (e: Exception) { TextToSpeech.LANG_NOT_SUPPORTED }
                        if (resHi >= TextToSpeech.LANG_AVAILABLE) {
                            tts.language = hindi
                        } else {
                            tts.language = Locale.ENGLISH
                        }
                    }
                }
            } else {
                val engUs = Locale.US
                if (tts.isLanguageAvailable(engUs) >= TextToSpeech.LANG_AVAILABLE) {
                    tts.language = engUs
                } else {
                    tts.language = Locale.ENGLISH
                }
            }

            // Clean text for natural speech synthesis
            val cleanText = text
                .replace(Regex("[*#_`~>•]"), "")
                .replace(Regex("Rs\\.?\\s*(\\d+)"), "$1 rupees")
                .replace(Regex("PKR\\s*(\\d+)"), "$1 rupees")
                .replace("- ", ", ")
                .replace("\n", ". ")
                .replace(Regex("\\s+"), " ")
                .trim()

            _currentSpeakingId.value = utteranceId
            _isSpeaking.value = true

            val params = Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            tts.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        } catch (e: Exception) {
            _isSpeaking.value = false
            _currentSpeakingId.value = null
        }
    }

    fun stop() {
        try {
            textToSpeech?.stop()
        } catch (e: Exception) {
            // ignore
        } finally {
            _isSpeaking.value = false
            _currentSpeakingId.value = null
        }
    }

    fun shutdown() {
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
            isInitialized = false
        } catch (e: Exception) {
            // ignore
        }
    }
}
