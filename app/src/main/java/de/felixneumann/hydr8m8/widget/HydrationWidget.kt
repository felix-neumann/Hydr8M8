package de.felixneumann.hydr8m8.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import de.felixneumann.hydr8m8.MainActivity
import de.felixneumann.hydr8m8.R
import de.felixneumann.hydr8m8.container
import de.felixneumann.hydr8m8.data.WidgetBackground
import de.felixneumann.hydr8m8.data.WidgetConfig
import de.felixneumann.hydr8m8.domain.MateMood
import de.felixneumann.hydr8m8.domain.Preset
import de.felixneumann.hydr8m8.domain.Surfaces
import de.felixneumann.hydr8m8.ui.BrandColors
import de.felixneumann.hydr8m8.ui.Format
import de.felixneumann.hydr8m8.ui.drawable
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

object WidgetUpdater {
    suspend fun updateAll(context: Context) {
        HydrationWidget().updateAll(context)
    }
}

class HydrationWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val c = context.container
        c.repository.ensureSeeded()
        c.repository.ensureWidgetDefaults(appWidgetId)
        // Glance keeps a session alive between updates, so the content must observe the data
        // instead of capturing a snapshot.
        val dataFlow =
            combine(
                c.repository.history,
                c.settings.settings,
                c.repository.surface(Surfaces.widget(appWidgetId)),
                c.repository.widgetConfigFlow(appWidgetId),
            ) { history, settings, presets, config ->
                val today = history.todaySummary
                WidgetData(
                    config = config,
                    presets = presets,
                    waterMl = today.waterMl,
                    goalMl = today.goalMl,
                    percent = today.percent,
                    mood =
                        MateMood.of(
                            Instant.now(),
                            settings.schedule.windowFor(history.today, ZoneId.systemDefault()),
                            today.waterMl,
                            today.goalMl,
                            today.hasAlcohol,
                        ),
                    streakDays = history.streak.currentDays,
                    jokers = history.streak.jokers,
                    nextReminder =
                        if (settings.reminders.enabled) {
                            c.reminderScheduler.nextReminderAt(
                                history,
                            )
                        } else {
                            null
                        },
                )
            }
        val initial = dataFlow.first()
        provideContent {
            val data by dataFlow.collectAsState(initial)
            WidgetContent(data)
        }
    }

    companion object {
        val TINY = DpSize(50.dp, 50.dp)
        val WIDE_SHORT = DpSize(110.dp, 50.dp)
        val WIDE = DpSize(240.dp, 50.dp)
        val SQUARE = DpSize(110.dp, 110.dp)
        val LARGE = DpSize(240.dp, 110.dp)
    }
}

private val PresetKey = ActionParameters.Key<Long>("preset_id")

internal fun logAction(presetId: Long) = actionRunCallback<LogPresetCallback>(actionParametersOf(PresetKey to presetId))

class LogPresetCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val id = parameters[PresetKey] ?: return
        context.container.actions.logPreset(id)
    }
}

class UndoCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        context.container.actions.undoLast()
    }
}

class HydrationWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HydrationWidget()

    override fun onDeleted(
        context: Context,
        appWidgetIds: IntArray,
    ) {
        super.onDeleted(context, appWidgetIds)
        val c = context.container
        c.appScope.launch { appWidgetIds.forEach { c.repository.deleteWidget(it) } }
    }
}
