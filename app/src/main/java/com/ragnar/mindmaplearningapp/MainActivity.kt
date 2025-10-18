package com.ragnar.mindmaplearningapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ragnar.mindmaplearningapp.ui.theme.AppTheme
import com.ragnar.mindmaplearningapp.ui.screens.ChatBotScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                ChatBotScreen()
            }
        }
    }
}