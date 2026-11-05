package de.felixneumann.hydr8m8.ui.theme

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.felixneumann.hydr8m8.container
import de.felixneumann.hydr8m8.data.ThemeMode

/**
 * Root of every activity's Compose content: applies the user's design setting (system, light,
 * dark) to the Material theme and to the status/navigation bar icons.
 */
@Composable
fun ComponentActivity.ThemedContent(content: @Composable () -> Unit) {
    val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
    val dark = (settings?.themeMode ?: ThemeMode.SYSTEM).isDark()
    LaunchedEffect(dark) { applySystemBars(dark) }
    Hydr8M8Theme(darkTheme = dark, content = content)
}

private fun ComponentActivity.applySystemBars(dark: Boolean) {
    val style =
        if (dark) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }
    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
}
