package com.example.data.model

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val textEn: String,
    val textUr: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isAudioAvailable: Boolean = true,
    val suggestedActions: List<String> = emptyList(),
    val isError: Boolean = false,
    val relatedCropOrDisease: String? = null
)

enum class MessageSender {
    USER,
    AI_ASSISTANT
}
