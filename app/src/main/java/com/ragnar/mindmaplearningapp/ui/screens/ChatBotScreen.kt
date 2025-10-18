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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.ragnar.mindmaplearningapp.ui.theme.BackgroundSecondary
import com.ragnar.mindmaplearningapp.ui.theme.ColorHint
import com.ragnar.mindmaplearningapp.ui.theme.SendButtonColor
import com.ragnar.mindmaplearningapp.ui.theme.TextPrimary
import com.ragnar.mindmaplearningapp.ui.theme.TextSecondary
import com.ragnar.mindmaplearningapp.ui.theme.White


@Composable
fun ChatBotScreen(
    sttController: SpeechToText = viewModel(), // SpeechToText core Util
) {

    val context = LocalContext.current

    val chatBotController: ChatViewModel = viewModel(
        factory = ChatViewModelFactory(
            apiKey = stringResource(R.string.chat_bot_api_key),
        )
    )

    val sttState by sttController.state.collectAsState() // STT states

    // Collects chat state from ChatViewModel
    val chatMessages by chatBotController.messages.collectAsState()
    val isChatLoading by chatBotController.isLoading.collectAsState()

    // Add after existing state collectors
    val typingText by chatBotController.typingText.collectAsState()
    val isTyping by chatBotController.isTyping.collectAsState()


    // ConceptMap json output from AI
    val conceptMapResult = chatBotController.conceptMapJSON.collectAsState()

    val conceptMapJSON = conceptMapResult.value

    val chatListState = rememberLazyListState()

    var messageInput by remember { mutableStateOf("") }

    // gets the latest AI message from chat history
    val aiMessageOutput = when {
        isTyping -> typingText
        else -> chatMessages.lastOrNull { it.sender == "ai" }?.content
            ?: "Hi! I'm ready to help you learn. What would you like to work on today?"
    }

    // updates messageInput from STT when speech recognition completes
    LaunchedEffect(sttState.resultText) {
        if (sttState.resultText.isNotBlank()) {
            messageInput = sttState.resultText
        }
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            chatListState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        sttController.handlePermissionResult(
            SpeechToText.RECORD_AUDIO_PERMISSION_REQUEST,
            if (isGranted) intArrayOf(PackageManager.PERMISSION_GRANTED)
            else intArrayOf(PackageManager.PERMISSION_DENIED)
        )
    }

    // Launch Activity
    LaunchedEffect(Unit) {
        sttController.initialize(context)
        if (!sttState.hasPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Disposal Activity
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
        Column {
            /*
            Card to display current result from AI
             */
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp) ,
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentHeight()
                    .padding(0.dp, 15.dp)

            ) {
                Column {

                    Text(
                        text = aiMessageOutput,
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    /**
                     * A text field send icon and mic button in a row
                     */
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // standard TextField
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
                        // Button to send message to chat
                        IconButton(
                            onClick = {
                                if (messageInput.isNotBlank() && !isChatLoading) {
                                    // Send message to chatbot
                                    chatBotController.sendMessage(messageInput)
                                    // Clear input
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

                        // mic button
                        IconButton(
                            onClick = {
                                Log.i("ChatScreen", "Mic Button Clicked")
                                if (!sttState.isSpeaking) {
                                    if (sttState.isInitialized && sttState.hasPermission) {
                                        sttController.startListening()
                                    } else if (!sttState.hasPermission){
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

            /**
             * Card to display concept map
             * this is a dynamic compose model
             */
            Card(
                modifier = Modifier
                    .height(500.dp)
                    .background(BackgroundSecondary),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                ConceptMapModel(conceptMapJSON)
            }
            Spacer(modifier = Modifier.padding(10.dp))
            /*
            Card to display previous messages
             */
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                modifier = Modifier.align(Alignment.Start)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(White)
                ) {
                    // Header row
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

                    // ChatMessage with auto-scroll
                    LazyColumn(
                        state = chatListState,
                        modifier = Modifier
                            .height(300.dp) // fixed height
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