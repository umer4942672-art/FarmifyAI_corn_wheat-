package com.example.util

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Reads diagnoses, advisory replies and crop guidance aloud.
 *
 * The previous version swallowed every failure. If Urdu was unavailable it fell
 * back to Hindi and then to English, handing Urdu script to an engine that
 * cannot read it, so the farmer heard nothing useful and was told nothing.
 * Worse, when the engine refused a request the speaking flag stayed true, which
 * left the button stuck: the next tap counted as "stop" and nothing played
 * again until the app restarted.
 *
 * This version checks the results the platform actually returns, reports what
 * went wrong through [errors], and never pretends to speak Urdu with a voice
 * that cannot.
 */
class VoiceAssistantHelper(private val context: Context) {

    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _currentSpeakingId = MutableStateFlow<String?>(null)
    val currentSpeakingId: StateFlow<String?> = _currentSpeakingId.asStateFlow()

    /** Bilingual messages for the screen to surface. */
    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 2)
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    private var pendingSpeech: Triple<String, Boolean, String>? = null
    private var initAttempts = 0

    init {
        initializeTts()
    }

    private fun initializeTts() {
        // A failed engine keeps failing; retrying without limit would spin on
        // every tap of the speaker button.
        if (initAttempts >= MAX_INIT_ATTEMPTS) return
        initAttempts++

        runCatching {
            textToSpeech = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isInitialized = true
                    configure()
                    pendingSpeech?.let { (text, isUrdu, id) ->
                        pendingSpeech = null
                        speak(text, isUrdu, id)
                    }
                } else {
                    isInitialized = false
                    pendingSpeech = null
                    report(
                        "آواز کی سہولت اس فون پر دستیاب نہیں۔",
                        "Text-to-speech is not available on this phone."
                    )
                }
            }
        }.onFailure {
            isInitialized = false
            Log.w(TAG, "TTS init failed: ${it.localizedMessage}")
            report("آواز کی سہولت شروع نہیں ہو سکی۔", "Could not start text-to-speech.")
        }
    }

    private fun configure() {
        val tts = textToSpeech ?: return
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
                _currentSpeakingId.value = utteranceId
            }

            override fun onDone(utteranceId: String?) = clearSpeaking()

            @Deprecated("Superseded by the two-argument form")
            override fun onError(utteranceId: String?) = clearSpeaking()

            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.w(TAG, "Utterance $utteranceId failed with code $errorCode")
                clearSpeaking()
                report("آواز چلانے میں مسئلہ ہوا۔", "Playback failed.")
            }
        })
        // A shade under natural pace. Advisory text carries doses and dates, and
        // a farmer is often listening while doing something else.
        tts.setSpeechRate(0.92f)
        tts.setPitch(1.0f)
    }

    private fun clearSpeaking() {
        _isSpeaking.value = false
        _currentSpeakingId.value = null
    }

    private fun report(urdu: String, english: String) {
        _errors.tryEmit("$urdu\n$english")
    }

    /**
     * Speaks [text], or stops if this same utterance is already playing.
     *
     * Returns false when nothing will be spoken, having already reported why.
     */
    fun speak(text: String, isUrdu: Boolean = false, utteranceId: String = "kisan_speech"): Boolean {
        if (text.isBlank()) return false

        // Tapping the speaker on the item already playing means stop.
        if (_isSpeaking.value && _currentSpeakingId.value == utteranceId) {
            stop()
            return true
        }

        val tts = textToSpeech
        if (!isInitialized || tts == null) {
            // The engine binds asynchronously, so an early tap is queued rather
            // than dropped. Only one is held; the newest request wins.
            pendingSpeech = Triple(text, isUrdu, utteranceId)
            if (tts == null) initializeTts()
            return true
        }

        stop()

        if (!applyLanguage(tts, isUrdu)) return false

        val spoken = prepareText(text, isUrdu)
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        // Set optimistically so the button reacts at once, then corrected below
        // if the engine refuses the request.
        _currentSpeakingId.value = utteranceId
        _isSpeaking.value = true

        val result = runCatching {
            tts.speak(spoken, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        }.getOrDefault(TextToSpeech.ERROR)

        if (result == TextToSpeech.ERROR) {
            // This is what used to leave the button stuck on "stop".
            clearSpeaking()
            report("آواز شروع نہیں ہو سکی۔", "Could not start playback.")
            return false
        }
        return true
    }

    /**
     * Selects a voice that can actually read the text.
     *
     * Urdu is missing on many phones. Falling through to English would hand
     * Urdu script to a voice that cannot read it, so that is refused here and
     * the farmer is told to read the text instead.
     */
    private fun applyLanguage(tts: TextToSpeech, isUrdu: Boolean): Boolean {
        val wanted = if (isUrdu) {
            listOf(Locale("ur", "PK"), Locale("ur"))
        } else {
            listOf(Locale.US, Locale.UK, Locale.ENGLISH)
        }

        for (locale in wanted) {
            val availability = runCatching { tts.isLanguageAvailable(locale) }
                .getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
            if (availability < TextToSpeech.LANG_AVAILABLE) continue

            // isLanguageAvailable can still be followed by a refusal here, for
            // instance when the voice is listed but its data is not downloaded.
            val applied = runCatching { tts.setLanguage(locale) }
                .getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
            if (applied >= TextToSpeech.LANG_AVAILABLE) return true

            if (applied == TextToSpeech.LANG_MISSING_DATA) {
                report(
                    "آواز کا ڈیٹا انسٹال نہیں۔ فون کی ترتیبات سے انسٹال کریں۔",
                    "The voice data is not installed. Install it from the phone's settings."
                )
                return false
            }
        }

        if (isUrdu) {
            report(
                "اس فون پر اردو آواز موجود نہیں۔ متن پڑھ لیں یا انگریزی میں سنیں۔",
                "Urdu speech is not available on this phone. Read the text, or switch to English."
            )
        } else {
            report("آواز کی زبان دستیاب نہیں۔", "No usable voice is installed for this language.")
        }
        return false
    }

    /**
     * Tidies text for speech.
     *
     * The currency and bullet substitutions are English-only; applying them to
     * Urdu inserted English words into the middle of an Urdu sentence.
     */
    private fun prepareText(text: String, isUrdu: Boolean): String {
        var out = text
            .replace(Regex("[*#_`~>\u2022]"), "")
            .replace("\n", ". ")

        if (!isUrdu) {
            out = out
                .replace(Regex("Rs\\.?\\s*(\\d+)"), "$1 rupees")
                .replace(Regex("PKR\\s*(\\d+)"), "$1 rupees")
                .replace("- ", ", ")
        }

        return out.replace(Regex("\\s+"), " ").trim()
    }

    fun stop() {
        runCatching { textToSpeech?.stop() }
        clearSpeaking()
    }

    fun shutdown() {
        runCatching {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        }
        textToSpeech = null
        isInitialized = false
        pendingSpeech = null
        clearSpeaking()
    }

    private companion object {
        const val TAG = "VoiceAssistant"
        const val MAX_INIT_ATTEMPTS = 3
    }
}
