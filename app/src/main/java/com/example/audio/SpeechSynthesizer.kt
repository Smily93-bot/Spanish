package com.example.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Spanish pronunciation through the device's built-in Android TextToSpeech engine (works offline
 * once a Spanish voice is installed — no cloud API key).
 */
class SpeechSynthesizer(context: Context) : TextToSpeech.OnInitListener {

    private val tts = TextToSpeech(context.applicationContext, this)

    @Volatile var isReady: Boolean = false
        private set

    /** True when the device has no Spanish voice; the UI can suggest installing one. */
    @Volatile var spanishVoiceMissing: Boolean = false
        private set

    var speechRate: Float = 0.95f

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        val candidates = listOf(Locale("es", "ES"), Locale("es", "MX"), Locale("es", "US"), Locale("es"))
        val chosen = candidates.firstOrNull {
            tts.isLanguageAvailable(it) >= TextToSpeech.LANG_AVAILABLE
        }
        if (chosen == null) {
            spanishVoiceMissing = true
            return
        }
        tts.language = chosen
        isReady = true
    }

    fun speakSpanish(text: String, rate: Float = speechRate) {
        if (!isReady || text.isBlank()) return
        tts.setSpeechRate(rate)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, text.hashCode().toString())
    }

    fun stop() {
        if (isReady) tts.stop()
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
