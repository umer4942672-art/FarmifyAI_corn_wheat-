package com.example.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat

/**
 * Speech to text for the advisory chat.
 *
 * Two things matter for the farmers this is built for. Recognition runs in the
 * language the interface is set to, so an Urdu speaker is understood in Urdu.
 * And partial results are surfaced while the person is still talking, so they
 * can see it is working rather than speaking into silence.
 *
 * Recognised text is handed back for the farmer to read and correct before it
 * is sent, since misheard words are common with accented or mixed speech.
 */
class VoiceInputManager(private val context: Context) {

    interface Callbacks {
        fun onPartial(text: String)
        fun onFinal(text: String)
        fun onError(message: String)
        fun onEndOfSpeech()
    }

    private var recognizer: SpeechRecognizer? = null
    private var callbacks: Callbacks? = null

    /**
     * Whether a recogniser exists on this device.
     *
     * Android 11 and above hide other apps' services unless the manifest
     * declares a <queries> entry for RecognitionService; without it this always
     * returns false no matter what the device has installed.
     */
    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    fun start(isUrdu: Boolean, callbacks: Callbacks) {
        this.callbacks = callbacks

        if (!isAvailable()) {
            callbacks.onError(
                if (isUrdu) "اس فون پر آواز کی پہچان دستیاب نہیں۔ سوال لکھ کر پوچھیں۔"
                else "Speech recognition is not available on this phone. Please type your question."
            )
            return
        }

        stop()

        val locale = if (isUrdu) "ur-PK" else "en-PK"
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
            // Fall back to the other language rather than failing outright, since
            // farmers often mix Urdu and English in the same sentence.
            putExtra(
                RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES,
                arrayOf(locale, if (isUrdu) "en-PK" else "ur-PK")
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // A farmer speaking a full question pauses mid-sentence; the default
            // silence window cuts them off.
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                putExtra(RecognizerIntent.EXTRA_ENABLE_FORMATTING, RecognizerIntent.FORMATTING_OPTIMIZE_QUALITY)
            }
        }

        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(listener(isUrdu))
            runCatching { startListening(intent) }
                .onFailure {
                    callbacks.onError(
                        if (isUrdu) "مائیک شروع نہیں ہو سکا۔ دوبارہ کوشش کریں۔"
                        else "Could not start the microphone. Please try again."
                    )
                }
        }
    }

    fun stop() {
        recognizer?.let { r ->
            runCatching { r.stopListening() }
            runCatching { r.cancel() }
            runCatching { r.destroy() }
        }
        recognizer = null
    }

    private fun listener(isUrdu: Boolean) = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            callbacks?.onEndOfSpeech()
        }

        override fun onPartialResults(partialResults: Bundle?) {
            firstResult(partialResults)?.let { callbacks?.onPartial(it) }
        }

        override fun onResults(results: Bundle?) {
            val text = firstResult(results)
            if (text.isNullOrBlank()) {
                callbacks?.onError(
                    if (isUrdu) "کچھ سنائی نہیں دیا۔ دوبارہ بولیں۔"
                    else "Nothing was heard. Please try again."
                )
            } else {
                callbacks?.onFinal(text)
            }
            stop()
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onError(error: Int) {
            callbacks?.onError(message(error, isUrdu))
            stop()
        }
    }

    private fun firstResult(bundle: Bundle?): String? =
        bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.trim()

    private fun message(error: Int, isUrdu: Boolean): String = when (error) {
        SpeechRecognizer.ERROR_NO_MATCH ->
            if (isUrdu) "سمجھ نہیں آیا۔ دوبارہ بولیں۔" else "That was not understood. Please try again."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
            if (isUrdu) "کچھ سنائی نہیں دیا۔" else "No speech was heard."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
            if (isUrdu) "مائیک کی اجازت درکار ہے۔" else "Microphone permission is needed."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            if (isUrdu) "آواز کی پہچان کے لیے انٹرنیٹ درکار ہے۔"
            else "Speech recognition needs an internet connection."
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ->
            if (isUrdu) "اردو آواز کی پہچان اس فون پر نہیں ہے۔ سوال لکھ کر پوچھیں۔"
            else "This language is not available for speech on this phone. Please type instead."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
            if (isUrdu) "مائیک مصروف ہے۔ ایک لمحے بعد کوشش کریں۔"
            else "The microphone is busy. Try again in a moment."
        else ->
            if (isUrdu) "آواز نہیں سنی جا سکی۔" else "Could not capture the voice."
    }
}
