package com.apkgit

import android.app.Activity
import android.os.Build
import androidx.core.view.WindowCompat

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalTextApi::class)
val GoogleSans = FontFamily(
    Font(
        resId = R.font.google_sans,
        weight = FontWeight.W400,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(400)
        )
    ),
    Font(
        resId = R.font.google_sans,
        weight = FontWeight.W500,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(500)
        )
    ),
    Font(
        resId = R.font.google_sans,
        weight = FontWeight.W700,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(700)
        )
    )
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    secondary = Color(0xFFCCC2DC),
    tertiary = Color(0xFFEFB8C8)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6650a4),
    secondary = Color(0xFF625b71),
    tertiary = Color(0xFF7D5260)
)

val Typography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = GoogleSans),
        displayMedium = displayMedium.copy(fontFamily = GoogleSans),
        displaySmall = displaySmall.copy(fontFamily = GoogleSans),

        headlineLarge = headlineLarge.copy(fontFamily = GoogleSans, fontWeight = FontWeight.W400),
        headlineMedium = headlineMedium.copy(fontFamily = GoogleSans, fontWeight = FontWeight.W500),
        headlineSmall = headlineSmall.copy(fontFamily = GoogleSans, fontWeight = FontWeight.W400),

        titleLarge = titleLarge.copy(
            fontFamily = GoogleSans,
            fontWeight = FontWeight.W500,
            fontSize = 22.sp,
            lineHeight = 28.sp
        ),
        titleMedium = titleMedium.copy(
            fontFamily = GoogleSans,
            fontWeight = FontWeight.W500,
            fontSize = 16.sp,
            letterSpacing = 0.1.sp
        ),
        titleSmall = titleSmall.copy(fontFamily = GoogleSans, fontWeight = FontWeight.W500),

        bodyLarge = bodyLarge.copy(
            fontFamily = GoogleSans,
            fontWeight = FontWeight.W400,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = bodyMedium.copy(fontFamily = GoogleSans, fontSize = 14.sp),
        bodySmall = bodySmall.copy(fontFamily = GoogleSans),

        labelLarge = labelLarge.copy(
            fontFamily = GoogleSans,
            fontWeight = FontWeight.W500,
            fontSize = 14.sp,
            letterSpacing = 0.1.sp
        ),
        labelMedium = labelMedium.copy(fontFamily = GoogleSans, fontWeight = FontWeight.W500),
        labelSmall = labelSmall.copy(fontFamily = GoogleSans, fontWeight = FontWeight.W500)
    )
}

@Composable
fun ApkGitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = dynamicColorScheme(darkTheme)
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
private fun dynamicColorScheme(darkTheme: Boolean): ColorScheme {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (darkTheme) DarkColorScheme else LightColorScheme
    }
}