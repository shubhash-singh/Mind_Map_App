package com.ragnar.mindmaplearningapp.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ragnar.mindmaplearningapp.R
import com.ragnar.mindmaplearningapp.core.ChatViewModel
import com.ragnar.mindmaplearningapp.core.ChatViewModelFactory
import com.ragnar.mindmaplearningapp.speechModels.SpeechToText
import com.ragnar.mindmaplearningapp.speechModels.TextToSpeech
import com.ragnar.mindmaplearningapp.ui.components.ChatMessageBubbleModel
import com.ragnar.mindmaplearningapp.ui.components.ConceptMapModel
import com.ragnar.mindmaplearningapp.ui.theme.BackgroundPrimary
import com.ragnar.mindmaplearningapp.ui.theme.BackgroundSecondary
import com.ragnar.mindmaplearningapp.ui.theme.BrandPrimary
import com.ragnar.mindmaplearningapp.ui.theme.ColorHint
import com.ragnar.mindmaplearningapp.ui.theme.SendButtonColor
import com.ragnar.mindmaplearningapp.ui.theme.TextPrimary
import com.ragnar.mindmaplearningapp.ui.theme.TextSecondary
import com.ragnar.mindmaplearningapp.ui.theme.White

@Composable
fun ChatBotScreen(
    sttController: SpeechToText = viewModel(),
) {
    val context = LocalContext.current
    val chatBotController: ChatViewModel = viewModel(
        factory = ChatViewModelFactory(
            apiKey = stringResource(R.string.chat_bot_api_key),
        )
    )

    val sttState by sttController.state.collectAsState()
    val chatMessages by chatBotController.messages.collectAsState()
    val isChatLoading by chatBotController.isLoading.collectAsState()
    val conceptMapResult = chatBotController.conceptMapJSON.collectAsState()
    val conceptMapJSON = conceptMapResult.value
    val chatListState = rememberLazyListState()
    val scrollState = rememberScrollState()

    var messageInput by remember { mutableStateOf("") }

    val aiMessageOutput = chatMessages.lastOrNull { it.sender == "ai" }?.content
        ?: "Hi! I'm ready to help you learn. What would you like to work on today?"

    LaunchedEffect(sttState.resultText) {
        if (sttState.resultText.isNotBlank()) {
            messageInput = sttState.resultText
        }
    }

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            chatListState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        sttController.handlePermissionResult(
            SpeechToText.RECORD_AUDIO_PERMISSION_REQUEST,
            if (isGranted) intArrayOf(PackageManager.PERMISSION_GRANTED)
            else intArrayOf(PackageManager.PERMISSION_DENIED)
        )
    }

    LaunchedEffect(Unit) {
        sttController.initialize(context)
        if (!sttState.hasPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            sttController.destroy()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(White)
                .verticalScroll(scrollState) // Make the main Column scrollable
        ) {
            // AI Output Card
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp, 15.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundPrimary)
                        .padding(0.dp, 10.dp)
                ) {
                    Text(
                        text = aiMessageOutput,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                        modifier = Modifier.padding(
                            top = 0.dp,
                            bottom = 10.dp,
                            start = 10.dp,
                            end = 10.dp
                        )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextField(
                            value = messageInput,
                            onValueChange = { messageInput = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Ask Sarah a question...") },
                            shape = RoundedCornerShape(20.dp),
                            singleLine = false,
                            colors = TextFieldDefaults.colors(
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedContainerColor = BackgroundSecondary,
                                focusedContainerColor = BackgroundSecondary,
                                cursorColor = ColorHint,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                unfocusedPlaceholderColor = TextSecondary,
                                focusedPlaceholderColor = TextSecondary
                            )
                        )

                        IconButton(
                            onClick = {
                                if (messageInput.isNotBlank() && !isChatLoading) {
                                    chatBotController.sendMessage(messageInput)
                                    messageInput = ""
                                }
                            },
                            enabled = messageInput.isNotBlank() && !isChatLoading,
                            modifier = Modifier.size(48.dp),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = SendButtonColor,
                                contentColor = Color.White,
                                disabledContainerColor = ColorHint,
                                disabledContentColor = Color.White
                            )
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Message"
                            )
                        }

                        IconButton(
                            onClick = {
                                Log.i("ChatScreen", "Mic Button Clicked")
                                if (!sttState.isSpeaking) {
                                    if (sttState.isInitialized && sttState.hasPermission) {
                                        sttController.startListening()
                                    } else if (!sttState.hasPermission) {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                } else {
                                    sttController.stopListening()
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .border(1.dp, SendButtonColor, CircleShape)
                        ) {
                            Icon(
                                if (sttState.isSpeaking) Icons.Outlined.Stop else Icons.Outlined.Mic,
                                contentDescription = "Record Audio",
                                tint = SendButtonColor
                            )
                        }
                    }

                    Text(
                        text = if (isChatLoading) "Sending..." else "Tap to send",
                        style = MaterialTheme.typography.labelMedium,
                        color = ColorHint,
                        modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)
                    )
                }
            }

            // Concept Map Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
                    .padding(horizontal = 10.dp)
                    .background(BackgroundSecondary),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                ConceptMapModel(conceptMapJSON)
            }

            Spacer(modifier = Modifier.padding(10.dp))

            // Previous Conversation Card
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .padding(bottom = 15.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(White)
                ) {
                    Row(
                        modifier = Modifier
                            .background(BackgroundSecondary)
                            .fillMaxWidth()
                            .padding(5.dp, 10.dp)
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = "Previous Conversation Icon",
                            tint = TextPrimary
                        )
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text(
                            text = "Previous Conversation: ",
                            color = TextPrimary
                        )
                    }

                    LazyColumn(
                        state = chatListState,
                        modifier = Modifier
                            .height(300.dp)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (chatMessages.isEmpty()) {
                            item {
                                Text(
                                    text = "No conversation yet...",
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        } else {
                            items(chatMessages) { message ->
                                ChatMessageBubbleModel(
                                    message = message,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}