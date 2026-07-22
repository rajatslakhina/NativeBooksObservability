package com.example.nativebooks.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BookCover(
    category: String,
    title: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val palette = coverPalette(category)
    val width = if (compact) 70.dp else 210.dp
    val height = if (compact) 94.dp else 292.dp
    val radius = if (compact) 10.dp else 18.dp

    Box(
        modifier = modifier
            .size(width, height)
            .shadow(if (compact) 5.dp else 14.dp, RoundedCornerShape(radius))
            .clip(RoundedCornerShape(radius))
            .background(Brush.linearGradient(palette)),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = if (compact) 22.dp else 70.dp, y = if (compact) (-12).dp else (-55).dp)
                .size(if (compact) 54.dp else 150.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.14f)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(width = if (compact) 4.dp else 7.dp, height = height)
                .background(Color.Black.copy(alpha = 0.13f)),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(if (compact) 10.dp else 20.dp),
        ) {
            Text(
                text = categorySymbol(category),
                color = Color.White,
                fontSize = if (compact) 17.sp else 28.sp,
                fontWeight = FontWeight.Bold,
            )
            if (!compact) {
                Text(
                    text = title,
                    modifier = Modifier.padding(top = 10.dp),
                    color = Color.White,
                    fontFamily = FontFamily.Serif,
                    fontSize = 21.sp,
                    lineHeight = 25.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun coverPalette(category: String): List<Color> = when (category) {
    "Software Design" -> listOf(Color(0xFF296B57), Color(0xFF12342B))
    "Engineering" -> listOf(Color(0xFFA6472E), Color(0xFF4D1C14))
    "Distributed Systems" -> listOf(Color(0xFF2B5C94), Color(0xFF14294D))
    "Productivity" -> listOf(Color(0xFFBF872B), Color(0xFF6E3D12))
    else -> listOf(Color(0xFF78478F), Color(0xFF361A4A))
}

private fun categorySymbol(category: String): String = when (category) {
    "Software Design" -> "▦"
    "Engineering" -> "⌘"
    "Distributed Systems" -> "△"
    "Productivity" -> "✦"
    else -> "✎"
}
