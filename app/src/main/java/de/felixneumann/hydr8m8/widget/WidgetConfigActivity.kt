package de.felixneumann.hydr8m8.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.felixneumann.hydr8m8.R
import de.felixneumann.hydr8m8.container
import de.felixneumann.hydr8m8.data.ThemeMode
import de.felixneumann.hydr8m8.data.WidgetBackground
import de.felixneumann.hydr8m8.data.WidgetConfig
import de.felixneumann.hydr8m8.domain.Surfaces
import de.felixneumann.hydr8m8.ui.Format
import de.felixneumann.hydr8m8.ui.components.PresetPickerDialog
import de.felixneumann.hydr8m8.ui.components.SectionCard
import de.felixneumann.hydr8m8.ui.components.SettingRow
import de.felixneumann.hydr8m8.ui.components.SurfaceEditor
import de.felixneumann.hydr8m8.ui.components.SwitchRow
import de.felixneumann.hydr8m8.ui.theme.HydrTheme
import de.felixneumann.hydr8m8.ui.theme.ThemedContent
import de.felixneumann.hydr8m8.ui.theme.isDark
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Per-widget configuration: buttons, Mate, info line, Undo, 1×1 tap action and background. */
class WidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val widgetId =
            intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // The activity is exported (required for APPWIDGET_CONFIGURE), so only accept ids of
        // widgets that really belong to this app.
        if (!isOwnWidget(widgetId)) {
            finish()
            return
        }
        // Placing the widget without saving still keeps it (configuration is optional).
        setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        setContent {
            ThemedContent {
                ConfigScreen(widgetId, onDone = { finish() })
            }
        }
    }

    private fun isOwnWidget(widgetId: Int): Boolean {
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return false
        val provider = AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId)?.provider ?: return false
        return provider.packageName == packageName && provider.className == HydrationWidgetReceiver::class.java.name
    }
}

@Composable
private fun ConfigScreen(
    widgetId: Int,
    onDone: () -> Unit,
) {
    val c = androidx.compose.ui.platform.LocalContext.current.container
    val scope = rememberCoroutineScope()
    val key = Surfaces.widget(widgetId)
    var config by remember { mutableStateOf<WidgetConfig?>(null) }
    var showTapPicker by remember { mutableStateOf(false) }
    LaunchedEffect(widgetId) {
        c.repository.ensureSeeded()
        c.repository.ensureWidgetDefaults(widgetId)
        config = c.repository.widgetConfig(widgetId)
    }
    val selected by remember(key) { c.repository.surface(key) }.collectAsStateWithLifecycle(emptyList())
    val library by c.repository.presets.collectAsStateWithLifecycle(emptyList())
    val cfg = config ?: return

    Column(
        Modifier
            .fillMaxSize()
            .background(HydrTheme.colors.backgroundBrush)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(R.string.widget_settings),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        SectionCard(title = stringResource(R.string.widget_buttons)) {
            SurfaceEditor(
                selected,
                library,
                Surfaces.MAX_WIDGET,
                onChange = { ids -> scope.launch { c.repository.setSurface(key, ids) } },
                onCreatePreset = { preset, onCreated -> scope.launch { onCreated(c.repository.savePreset(preset)) } },
            )
        }
        SectionCard {
            SwitchRow(stringResource(R.string.widget_show_mate), cfg.showMate, { config = cfg.copy(showMate = it) })
            SwitchRow(stringResource(R.string.widget_show_info), cfg.showInfo, { config = cfg.copy(showInfo = it) })
            SwitchRow(stringResource(R.string.widget_show_undo), cfg.showUndo, { config = cfg.copy(showUndo = it) })
            val tap = library.find { it.id == cfg.tapPresetId } ?: selected.firstOrNull()
            SettingRow(
                stringResource(R.string.widget_tap_action),
                tap?.let { "+" + Format.presetLabel(it) } ?: "–",
                { showTapPicker = true },
                supporting = stringResource(R.string.widget_tap_action_hint),
            )
        }
        SectionCard(title = stringResource(R.string.widget_background)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WidgetBackground.entries.forEach { bg ->
                    FilterChip(
                        selected = cfg.background == bg,
                        onClick = { config = cfg.copy(background = bg) },
                        label = {
                            Text(
                                stringResource(
                                    when (bg) {
                                        WidgetBackground.DARK -> R.string.bg_dark
                                        WidgetBackground.LIGHT -> R.string.bg_light
                                        WidgetBackground.TRANSPARENT -> R.string.bg_transparent
                                    },
                                ),
                            )
                        },
                    )
                }
            }
            if (cfg.background != WidgetBackground.TRANSPARENT) {
                Text(stringResource(R.string.widget_opacity, cfg.opacityPercent))
                Slider(
                    value = cfg.opacityPercent.toFloat(),
                    onValueChange = {
                        config = cfg.copy(opacityPercent = ((it / 5).roundToInt() * 5).coerceIn(30, 100))
                    },
                    valueRange = 30f..100f,
                )
            }
        }
        Button(
            onClick = {
                scope.launch {
                    c.repository.saveWidgetConfig(cfg)
                    WidgetUpdater.updateAll(c.appContext)
                    onDone()
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text(stringResource(R.string.save)) }
    }

    if (showTapPicker) {
        PresetPickerDialog(
            presets = library.filterNot { it.hidden },
            onPick = {
                config = cfg.copy(tapPresetId = it.id)
                showTapPicker = false
            },
            onDismiss = { showTapPicker = false },
        )
    }
}
