package com.apkgit

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalContext

import java.util.Locale

abstract class BaseActivity : ComponentActivity() {
    @Composable
    fun isDarkTheme(): Boolean {
        val context = LocalContext.current
        val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
        val themeSetting = remember { prefs.getString("theme", "system") ?: "system" }

        return when (themeSetting) {
            "light" -> false
            "dark" -> true
            else -> isSystemInDarkTheme()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
    }
    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val lang = prefs.getString("language", Locale.getDefault().language) ?: "en"
        val locale = Locale.forLanguageTag(lang)
        Locale.setDefault(locale)
        val config = newBase.resources.configuration
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }
}