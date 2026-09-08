package com.example.data.repository

import android.content.Context
import com.example.data.model.ChatMessage
import com.example.data.model.MessageSender
import com.example.data.remote.ApiConfig
import com.example.data.remote.AuthSessionStore
import com.example.data.remote.AuthorizedApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * Client for FarmifyAI's custom agriculture chatbot.
 *
 * The Android layer sends the farmer's
 * question to FastAPI; the backend retrieves verified agriculture context from Supabase
 * and uses the configured Google Gemini model to generate the response.
 */
class KisanChatRepository(context: Context, private val mandiRepository: MandiRepository) {
    private val api = AuthorizedApiClient(context.applicationContext)
    private val session = AuthSessionStore(context.applicationContext)

    suspend fun getAgriAiResponse(
        userQuery: String,
        chatHistory: List<ChatMessage>,
        isUrdu: Boolean
    ): ChatMessage = withContext(Dispatchers.IO) {
        if (isMandiQuery(userQuery)) return@withContext getLiveMandiResponse(isUrdu)

        val outcome = callCustomAgricultureApi(userQuery, chatHistory, isUrdu)
        val answer = (outcome as? ChatOutcome.Answer)?.text
            ?: return@withContext ChatMessage(
                sender = MessageSender.AI_ASSISTANT,
                // "Temporarily unavailable" covered four unrelated causes and told the
                // farmer nothing actionable. Each one now names itself.
                textEn = if (isUrdu) "" else (outcome as ChatOutcome.Failure).messageEn,
                textUr = if (isUrdu) (outcome as ChatOutcome.Failure).messageUr else null,
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

    /** Either an answer, or a failure that can explain itself to the farmer. */
    private sealed interface ChatOutcome {
        data class Answer(val text: String) : ChatOutcome
        data class Failure(val messageEn: String, val messageUr: String) : ChatOutcome
    }

    /**
     * Posts the question to the backend chatbot.
     * Routed through AuthorizedApiClient so an expired Supabase access token is
     * refreshed and the request retried.
     */
    private suspend fun callCustomAgricultureApi(
        userQuery: String,
        chatHistory: List<ChatMessage>,
        isUrdu: Boolean
    ): ChatOutcome {
        if (!ApiConfig.isConfigured) {
            return ChatOutcome.Failure(
                "The app was built without a backend URL, so the assistant cannot be reached. Rebuild with backendBaseUrl set.",
                "ایپ میں سرور کا پتہ سیٹ نہیں ہوا، اس لیے اسسٹنٹ سے رابطہ نہیں ہو سکتا۔"
            )
        }
        if (!session.hasSession()) {
            return ChatOutcome.Failure(
                "You are signed in on this device only. Sign in again with your email so the assistant can reach the server.",
                "آپ صرف اس فون پر سائن اِن ہیں۔ اسسٹنٹ چلانے کے لیے ای میل سے دوبارہ سائن اِن کریں۔"
            )
        }

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
        }.toString()

        val response = api.call { token ->
            Request.Builder()
                .url(ApiConfig.endpoint("/api/chat"))
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/json")
                .post(body.toRequestBody(AuthorizedApiClient.JSON))
                .build()
        } ?: return ChatOutcome.Failure(
            "Could not reach the server. Check your internet connection and try again.",
            "سرور تک رسائی نہیں ہو سکی۔ انٹرنیٹ چیک کر کے دوبارہ کوشش کریں۔"
        )

        if (!response.isSuccessful) {
            val detail = response.errorDetail("Server error ${response.code}")
            return ChatOutcome.Failure(
                when (response.code) {
                    401 -> "Your session has expired. Please sign in again."
                    404 -> "The chat endpoint is missing on the server. Redeploy the backend."
                    // 503 means genuinely unconfigured, 502 means the key exists but the
                    // upstream call failed. Both carry the server's own detail, because
                    // guessing a single cause for a status code sent me chasing a key
                    // that was already set.
                    502, 503 -> detail
                    504 -> "The AI took too long to answer. Try a shorter question."
                    else -> detail
                },
                when (response.code) {
                    401 -> "آپ کا سیشن ختم ہو گیا ہے۔ دوبارہ سائن اِن کریں۔"
                    404 -> "سرور پر چیٹ کی سہولت موجود نہیں۔ بیک اینڈ دوبارہ ڈیپلائے کریں۔"
                    504 -> "AI نے جواب دینے میں بہت وقت لیا۔ مختصر سوال کریں۔"
                    else -> "سرور کی خرابی: $detail"
                }
            )
        }

        val answer = response.json().optString("answer").takeIf { it.isNotBlank() }
            ?: return ChatOutcome.Failure(
                "The server replied but the answer was empty. Try rephrasing the question.",
                "سرور نے جواب دیا مگر وہ خالی تھا۔ سوال دوبارہ لکھ کر کوشش کریں۔"
            )
        return ChatOutcome.Answer(answer)
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
