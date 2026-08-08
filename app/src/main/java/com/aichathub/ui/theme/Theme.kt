package com.aichathub.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// 颜色定义
val PrimaryBlue = Color(0xFF2196F3)
val PrimaryBlueDark = Color(0xFF1976D2)
val SecondaryTeal = Color(0xFF03DAC6)
val BackgroundLight = Color(0xFFF5F5F5)
val BackgroundDark = Color(0xFF1C1C1E)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceDark = Color(0xFF2C2C2E)
val UserMessageBg = Color(0xFFDCF8C6)
val AIMessageBg = Color(0xFFE8E8E8)
val AIMessageBgDark = Color(0xFF3A3A3C)
val TextPrimary = Color(0xFF1C1C1E)
val TextSecondary = Color(0xFF757575)
val TextOnPrimary = Color(0xFFFFFFFF)
val ErrorRed = Color(0xFFB00020)

// 平台颜色
val DeepSeekColor = Color(0xFF6B5CE7)
val MiniMaxColor = Color(0xFFFF6B6B)
val OpenAIColor = Color(0xFF10A37F)
val GeminiColor = Color(0xFF4285F4)
val AnthropicColor = Color(0xFFD97757)
val QwenColor = Color(0xFF615CED)
val ZhipuColor = Color(0xFF3859FF)
val MoonshotColor = Color(0xFF1D1D1F)
val YiColor = Color(0xFF003D2E)
val BaichuanColor = Color(0xFFFF8C00)
val DoubaoColor = Color(0xFF3D5AFE)
val HunyuanColor = Color(0xFF0053E0)
val SparkColor = Color(0xFFEE6C4D)
val SiliconFlowColor = Color(0xFF155EEF)
val GroqColor = Color(0xFFF55036)
val TogetherColor = Color(0xFF0F6FFF)
val OpenRouterColor = Color(0xFF6466F1)
val CustomColor = Color(0xFF607D8B)

// 终端颜色
val TerminalBg = Color(0xFF1E1E1E)
val TerminalText = Color(0xFFD4D4D4)
val TerminalAccent = Color(0xFF569CD6)
val TerminalError = Color(0xFFEF5350)
val TerminalWarn = Color(0xFFFFB74D)
val TerminalSuccess = Color(0xFF81C784)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = TextOnPrimary,
    primaryContainer = PrimaryBlueDark,
    secondary = SecondaryTeal,
    background = BackgroundDark,
    surface = SurfaceDark,
    onBackground = TextOnPrimary,
    onSurface = TextOnPrimary,
    error = ErrorRed,
    onError = TextOnPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = TextOnPrimary,
    primaryContainer = PrimaryBlueDark,
    secondary = SecondaryTeal,
    background = BackgroundLight,
    surface = SurfaceLight,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = ErrorRed,
    onError = TextOnPrimary
)

@Composable
fun AIChatHubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,  // 默认关闭动态颜色，使用品牌色
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}

val AppTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)
