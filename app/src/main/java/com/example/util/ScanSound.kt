package com.example.util

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper

/**
 * Short confirmation tone played when a disease scan finishes.
 *
 * A farmer holding the phone over a leaf usually isn't looking at the screen,
 * so the result is announced audibly as well. ToneGenerator is used instead of
 * a bundled audio file so no extra asset ships in the APK.
 */
object ScanSound {

    private const val TONE_MILLIS = 900
    private const val RELEASE_DELAY_MILLIS = 1_100L

    /**
     * @param isSuccess true when a plant was recognised (pleasant double beep),
     *                  false when the image was rejected (distinct error tone).
     */
    fun playScanComplete(isSuccess: Boolean = true) {
        try {
            val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, VOLUME_PERCENT)
            val toneType = if (isSuccess) {
                ToneGenerator.TONE_PROP_BEEP2
            } else {
                ToneGenerator.TONE_PROP_NACK
            }
            tone.startTone(toneType, TONE_MILLIS)
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    tone.release()
                } catch (_: Exception) {
                    // Already released.
                }
            }, RELEASE_DELAY_MILLIS)
        } catch (_: Exception) {
            // Silent mode, no audio focus, or an unavailable stream. Staying quiet
            // is the right outcome here, so the scan result is never blocked by it.
        }
    }

    private const val VOLUME_PERCENT = 75
}
