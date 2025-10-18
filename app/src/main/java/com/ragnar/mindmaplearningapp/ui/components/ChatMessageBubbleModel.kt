package com.ragnar.mindmaplearningapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ragnar.mindmaplearningapp.data.model.ChatMessageModel
import com.ragnar.mindmaplearningapp.ui.theme.BrandPrimary
import com.ragnar.mindmaplearningapp.ui.theme.TextPrimary
import com.ragnar.mindmaplearningapp.ui.theme.TextSecondary
import com.ragnar.mindmaplearningapp.ui.theme.White


@Composable
fun ChatMessageBubbleModel(
    message: ChatMessageModel,
    modifier: Modifier = Modifier
) {
    val isFromAI = message.sender == "ai"

    Row(
        modifier = modifier,
        horizontalArrangement = if (isFromAI) Arrangement.Start else Arrangement.End
    ) {
        if (isFromAI) {
            // AI message - aligned left
            Row(
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.SmartToy,
                    contentDescription = "AI",
                    tint = BrandPrimary,
                    modifier = Modifier.padding(top = 4.dp, end = 8.dp)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp, 12.dp, 12.dp, 4.dp))
                        .background(White)
                        .padding(12.dp)
                ) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }
            }
        } else {
            // User message - aligned right
            Row(
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp, 12.dp, 4.dp, 12.dp))
                        .background(BrandPrimary)
                        .padding(12.dp)
                ) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = White
                    )
                }

                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "User",
                    tint = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp, start = 8.dp)
                )
            }
        }
    }
}
