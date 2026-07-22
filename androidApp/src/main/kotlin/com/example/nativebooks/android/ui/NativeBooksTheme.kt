package com.example.nativebooks.android.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object NativeBooksColors {
    val Accent = Color(0xFF365F4C)
    val AccentSoft = Color(0xFFDCE8E1)
    val Ink = Color(0xFF1A1F1C)
    val SecondaryInk = Color(0xFF616963)
    val Paper = Color(0xFFF5F2E8)
    val Card = Color(0xFFFFFDF7)
    val Line = Color(0x1A000000)
    val Error = Color(0xFFB3261E)
}

private val colorScheme = lightColorScheme(
    primary = NativeBooksColors.Accent,
    onPrimary = Color.White,
    primaryContainer = NativeBooksColors.AccentSoft,
    onPrimaryContainer = NativeBooksColors.Ink,
    background = NativeBooksColors.Paper,
    onBackground = NativeBooksColors.Ink,
    surface = NativeBooksColors.Card,
    onSurface = NativeBooksColors.Ink,
    onSurfaceVariant = NativeBooksColors.SecondaryInk,
    outline = NativeBooksColors.Line,
    error = NativeBooksColors.Error,
)

private val typography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 42.sp,
        lineHeight = 47.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 29.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
    ),
)

@Composable
fun NativeBooksTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = Shapes(
            small = RoundedCornerShape(10.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(22.dp),
        ),
        content = content,
    )
}
