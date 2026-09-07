package com.example.ui.screens.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.data.model.MessageSender
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.util.AppLanguage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun KisanChatScreen(
    viewModel: MainViewModel,
    onNavigateToCropGuide: (String?) -> Unit = {},
    onNavigateToDiseaseScan: () -> Unit = {}
) {
    val language by viewModel.currentLanguage.collectAsState()
    val isUrdu = language == AppLanguage.URDU
    val chatMessages by viewModel.chatMessages.collectAsState()
    val currentInput by viewModel.currentChatInput.collectAsState()
    val isAiThinking by viewModel.isAiThinking.collectAsState()
    val isListening by viewModel.isVoiceListening.collectAsState()
    val isSpeaking by viewModel.voiceHelper.isSpeaking.collectAsState()
    val speakingId by viewModel.voiceHelper.currentSpeakingId.collectAsState()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto scroll on new messages
    LaunchedEffect(chatMessages.size, isAiThinking) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .background(MaterialTheme.colorScheme.background)
            .testTag("kisan_chat_screen")
    ) {
        // Top Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 3.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SmartToy,
                            contentDescription = "AI Assistant",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (isUrdu) "کسان دوست AI" else "Kisan Dost AI",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            // Live status dot
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(SuccessGreen)
                            )
                        }
                        Text(
                            text = if (isUrdu) "آواز اور چیٹ زرعی مشیر (اردو/English)" else "Voice & Chat Agri Advisor (Urdu/English)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Clear chat button
                    IconButton(
                        onClick = { viewModel.clearChatHistory() },
                        modifier = Modifier.testTag("clear_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Clear Chat",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Voice Listening Active Banner
        AnimatedVisibility(
            visible = isListening,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            VoiceListeningIndicator(
                isUrdu = isUrdu,
                onStop = { viewModel.setVoiceListening(false) },
                onSendVoiceQuery = { voiceQuery ->
                    viewModel.setVoiceListening(false)
                    viewModel.sendChatMessage(voiceQuery)
                }
            )
        }

        // Chat Messages List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                // Welcome Info Card
                item {
                    KisanAiIntroBanner(isUrdu = isUrdu)
                }

                items(chatMessages, key = { it.id }) { msg ->
                    ChatBubbleItem(
                        message = msg,
                        isUrdu = isUrdu,
                        isSpeaking = isSpeaking && speakingId == msg.id,
                        onSpeakClick = { textToSpeak ->
                            if (isSpeaking && speakingId == msg.id) {
                                viewModel.stopAudio()
                            } else {
                                viewModel.speakAudio(textToSpeak, isUrdu = isUrdu, utteranceId = msg.id)
                            }
                        },
                        onActionClick = { chipText ->
                            viewModel.sendChatMessage(chipText)
                        },
                        onNavigateToCropGuide = onNavigateToCropGuide
                    )
                }

                if (isAiThinking) {
                    item {
                        AiThinkingBubble(isUrdu = isUrdu)
                    }
                }
            }
        }

        // Quick Suggestion Chips Carousel
        val quickChips = if (isUrdu) {
            listOf(
                "🌾 گندم کی کھاد کا شیڈول",
                "🐛 کپاس میں سفید مکھی کا علاج",
                "💰 آج کے منڈی کے ریٹ",
                "🥔 آلو میں جھلساؤ کا سپرے",
                "🌱 چاول میں زنک کی مقدار",
                "🌧️ بارش کا اثر اور احتیاط"
            )
        } else {
            listOf(
                "🌾 Wheat Fertilizer Schedule",
                "🐛 Cotton Whitefly Remedy",
                "💰 Today's Mandi Rates",
                "🥔 Potato Late Blight Spray",
                "🌱 Rice Zinc Application",
                "🌧️ Rain Farming Advisory"
            )
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickChips) { chip ->
                SuggestionPill(
                    text = chip,
                    onClick = { viewModel.sendChatMessage(chip) }
                )
            }
        }

        // Input Field & Action Buttons Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Voice Mic Button
                VoiceMicButton(
                    isListening = isListening,
                    onClick = {
                        viewModel.setVoiceListening(!isListening)
                    }
                )

                // Text Input
                OutlinedTextField(
                    value = currentInput,
                    onValueChange = { viewModel.onChatInputChange(it) },
                    placeholder = {
                        Text(
                            text = if (isUrdu) "سوال لکھیں یا مائیک دبائیں..." else "Ask crop, spray or fertilizer question...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    singleLine = false,
                    maxLines = 3
                )

                // Send Button
                IconButton(
                    onClick = {
                        if (currentInput.isNotBlank()) {
                            viewModel.sendChatMessage()
                        }
                    },
                    enabled = currentInput.isNotBlank() && !isAiThinking,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            if (currentInput.isNotBlank()) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .testTag("send_message_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (currentInput.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun KisanAiIntroBanner(isUrdu: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.RecordVoiceOver,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (isUrdu) "آواز اور لکھائی میں مکمل رہنمائی" else "Voice & Text Bilingual Advisor",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Text(
                text = if (isUrdu)
                    "گندم، کپاس، چاول، کماد، آلو کی کھاد، زرد کنگی، سفید مکھی اور جھلساؤ کے مصدقہ پاکستانی سپرے اور منڈی ریٹس پوچھیں! آواز سننے کے لیے اسپیکر بٹن دبائیں۔"
                else
                    "Ask in English, Urdu or Roman Urdu about crop nutrition, spray dosages (Nativo, Tilt, Coragen, Polo), and live mandi rates! Tap the speaker icon to listen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                lineHeight = 18.sp
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatBubbleItem(
    message: ChatMessage,
    isUrdu: Boolean,
    isSpeaking: Boolean,
    onSpeakClick: (String) -> Unit,
    onActionClick: (String) -> Unit,
    onNavigateToCropGuide: (String?) -> Unit
) {
    val isUser = message.sender == MessageSender.USER
    val alignment = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(0.92f),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(bottom = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Spa,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                color = if (isUser) MaterialTheme.colorScheme.primary
                else if (message.isError) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.surface,
                shadowElevation = if (isUser) 1.dp else 2.dp,
                modifier = Modifier
                    .border(
                        width = if (isUser) 0.dp else 1.dp,
                        color = if (isUser) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val displayText = if (isUrdu && !message.textUr.isNullOrBlank()) {
                        message.textUr
                    } else {
                        message.textEn.ifBlank { message.textUr ?: "" }
                    }

                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isUser) Color.White
                        else if (message.isError) MaterialTheme.colorScheme.onErrorContainer
                        else MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp,
                        textAlign = if (isUrdu && displayText.any { Character.UnicodeBlock.of(it) == Character.UnicodeBlock.ARABIC })
                            TextAlign.Right else TextAlign.Start
                    )

                    // Audio Readout Speaker button for AI Assistant
                    if (!isUser && displayText.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = { onSpeakClick(displayText) },
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (isSpeaking) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSpeaking) Icons.Filled.VolumeUp else Icons.Outlined.VolumeUp,
                                        contentDescription = "Listen",
                                        tint = if (isSpeaking) Color.White else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (isSpeaking) {
                                            if (isUrdu) "آواز بند کریں" else "Stop Audio"
                                        } else {
                                            if (isUrdu) "آواز سنیں" else "Listen Aloud"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSpeaking) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            if (!message.relatedCropOrDisease.isNullOrBlank()) {
                                TextButton(
                                    onClick = { onNavigateToCropGuide(message.relatedCropOrDisease) },
                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                ) {
                                    Text(
                                        text = if (isUrdu) "تفصیلی گائیڈ دیکھیں →" else "View Guide →",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Follow-up suggestion action chips
        if (!isUser && message.suggestedActions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                modifier = Modifier.padding(start = 38.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                message.suggestedActions.forEach { action ->
                    AssistChip(
                        onClick = { onActionClick(action) },
                        label = {
                            Text(
                                text = action,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        border = AssistChipDefaults.assistChipBorder(
                            borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            enabled = true
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun AiThinkingBubble(isUrdu: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        modifier = Modifier.padding(start = 8.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.SmartToy,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (isUrdu) "کسان دوست جواب تیار کر رہا ہے..." else "Kisan AI is analyzing crop agronomy...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun VoiceMicButton(
    isListening: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isListening) 1.25f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_scale"
    )

    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(46.dp)
            .scale(if (isListening) scale else 1f)
            .clip(CircleShape)
            .background(
                if (isListening) ErrorRed
                else MaterialTheme.colorScheme.primaryContainer
            )
            .testTag("voice_assistant_mic_button")
    ) {
        Icon(
            imageVector = if (isListening) Icons.Filled.Mic else Icons.Filled.MicNone,
            contentDescription = "Voice Input",
            tint = if (isListening) Color.White else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun VoiceListeningIndicator(
    isUrdu: Boolean,
    onStop: () -> Unit,
    onSendVoiceQuery: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(ErrorRed)
                )
                Text(
                    text = if (isUrdu) "آواز سن رہا ہے... بولیں یا تجویز منتخب کریں" else "Listening... Speak or tap sample query",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Quick spoken voice prompt options
            val voicePrompts = if (isUrdu) {
                listOf(
                    "گندم میں زرد کنگی کا کیا علاج ہے؟",
                    "کپاس میں سفید مکھی کے لیے بہترین سپرے؟",
                    "آج کا گندم اور کپاس کا منڈی ریٹ کیا ہے؟"
                )
            } else {
                listOf(
                    "What is the best spray for wheat yellow rust?",
                    "How to control whitefly in cotton crop?",
                    "What is today's wheat and cotton mandi rate?"
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                voicePrompts.forEach { prompt ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSendVoiceQuery(prompt) },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "🗣️ \"$prompt\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp
                            )
                            Icon(
                                imageVector = Icons.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onStop,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isUrdu) "بند کریں" else "Close Voice Mode",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
fun SuggestionPill(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clickable(onClick = onClick)
            .shadow(1.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = CardDefaults.outlinedCardBorder()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 12.sp
        )
    }
}
