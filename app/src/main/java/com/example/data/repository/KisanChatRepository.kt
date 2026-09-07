package com.example.data.repository

import android.content.Context
import com.example.data.model.ChatMessage
import com.example.data.model.MessageSender
import com.example.data.remote.ApiConfig
import com.example.data.remote.AuthSessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Client for FarmifyAI's custom agriculture chatbot.
 *
 * The Android layer sends the farmer's
 * question to FastAPI; the backend retrieves verified agriculture context from Supabase
 * and uses the configured Google Gemini model to generate the response.
 */
class KisanChatRepository(context: Context, private val mandiRepository: MandiRepository) {
    private val session = AuthSessionStore(context.applicationContext)

    suspend fun getAgriAiResponse(
        userQuery: String,
        chatHistory: List<ChatMessage>,
        isUrdu: Boolean
    ): ChatMessage = withContext(Dispatchers.IO) {
        if (isMandiQuery(userQuery)) return@withContext getLiveMandiResponse(isUrdu)

        val answer = callCustomAgricultureApi(userQuery, chatHistory, isUrdu)
            ?: return@withContext ChatMessage(
                sender = MessageSender.AI_ASSISTANT,
                textEn = if (isUrdu) "" else "The custom agriculture assistant is temporarily unavailable. Please try again when the server is online.",
                textUr = if (isUrdu) "کسٹم زرعی اسسٹنٹ عارضی طور پر دستیاب نہیں ہے۔ سرور آن ہونے پر دوبارہ کوشش کریں۔" else null,
                suggestedActions = getSuggestedFollowUps(userQuery, isUrdu),
                isError = true
            )

        ChatMessage(
            sender = MessageSender.AI_ASSISTANT,
            textEn = if (isUrdu) "" else answer,
            textUr = if (isUrdu) answer else null,
            suggestedActions = getSuggestedFollowUps(userQuery, isUrdu)
        )
    }

    private fun isMandiQuery(query: String): Boolean {
        val q = query.lowercase(Locale.ENGLISH)
        return q.contains("mandi") || q.contains("rate") || q.contains("price") ||
            q.contains("ریٹ") || q.contains("منڈی") || q.contains("قیمت")
    }

    private suspend fun getLiveMandiResponse(isUrdu: Boolean): ChatMessage {
        val live = mandiRepository.refreshRates()
        if (!live) {
            return ChatMessage(
                sender = MessageSender.AI_ASSISTANT,
                textEn = if (isUrdu) "" else "💰 **Mandi rates are temporarily unavailable.**\n\nThe live market service could not be reached, so I won't present old or estimated numbers as current prices.",
                textUr = if (isUrdu) "💰 **منڈی ریٹس عارضی طور پر دستیاب نہیں ہیں۔**\n\nلائیو مارکیٹ سروس سے رابطہ نہیں ہو سکا، اس لیے پرانے یا اندازاً نمبروں کو موجودہ ریٹ کے طور پر نہیں دکھایا جائے گا۔" else null,
                suggestedActions = getSuggestedFollowUps("mandi", isUrdu)
            )
        }
        val rates = mandiRepository.rates.value.take(8)
        val en = buildString {
            appendLine("💰 **Live Pakistan Mandi Rates:**")
            rates.forEach { rate ->
                appendLine("• **${rate.cropNameEn}**: PKR ${rate.minPricePerKg}–${rate.maxPricePerKg} / Kg — ${rate.city}")
            }
            append("\nFetched from the live Mandi service just now. Prices can change during the day.")
        }
        val ur = buildString {
            appendLine("💰 **پاکستان کے لائیو منڈی ریٹس:**")
            rates.forEach { rate ->
                appendLine("• **${rate.cropNameUr}**: ${rate.minPricePerKg} تا ${rate.maxPricePerKg} روپے فی کلو — ${rate.city}")
            }
            append("\nیہ ریٹس ابھی لائیو منڈی سروس سے حاصل کیے گئے ہیں، دن کے دوران قیمت بدل سکتی ہے۔")
        }
        return ChatMessage(
            sender = MessageSender.AI_ASSISTANT,
            textEn = if (isUrdu) "" else en,
            textUr = if (isUrdu) ur else null,
            suggestedActions = getSuggestedFollowUps("mandi", isUrdu)
        )
    }

    private fun callCustomAgricultureApi(
        userQuery: String,
        chatHistory: List<ChatMessage>,
        isUrdu: Boolean
    ): String? {
        val endpoint = ApiConfig.endpoint("/api/chat")
        val conn = URL(endpoint).openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            session.accessToken()?.let { conn.setRequestProperty("Authorization", "Bearer $it") }
            conn.doOutput = true
            conn.connectTimeout = 30000
            conn.readTimeout = 90000

            val history = JSONArray()
            chatHistory.takeLast(6).forEach { msg ->
                val text = msg.textUr?.ifBlank { msg.textEn } ?: msg.textEn
                history.put(
                    JSONObject().apply {
                        put("role", if (msg.sender == MessageSender.USER) "user" else "assistant")
                        put("text", text.take(4000))
                    }
                )
            }

            val body = JSONObject().apply {
                put("message", userQuery)
                put("language", if (isUrdu) "ur" else "en")
                put("history", history)
            }
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            if (conn.responseCode !in 200..299) return null

            JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                .optString("answer")
                .ifBlank { null }
        } finally {
            conn.disconnect()
        }
    }

    private fun getSuggestedFollowUps(query: String, isUrdu: Boolean): List<String> {
        val q = query.lowercase(Locale.ENGLISH)
        return when {
            q.contains("wheat") || q.contains("گندم") -> listOf(
                "گندم کی آبپاشی", "Wheat fertilizer", "گندم میں بیماری"
            )
            q.contains("corn") || q.contains("maize") || q.contains("مکئی") -> listOf(
                "مکئی کی بیماری", "Corn irrigation", "مکئی کی کھاد"
            )
            else -> listOf(
                if (isUrdu) "گندم کے بارے میں بتائیں" else "Wheat crop guide",
                if (isUrdu) "مکئی کی بیماری" else "Corn disease help",
                if (isUrdu) "مٹی کا ٹیسٹ" else "Soil testing advice"
            )
        }
    }
}
