package com.ragnar.mindmaplearningapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.ragnar.mindmaplearningapp.ui.screens.ChatBotScreen
import com.ragnar.mindmaplearningapp.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, true)

        setContent {
            AppTheme {
                ChatBotScreen()
            }
        }
    }
}