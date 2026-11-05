package de.felixneumann.hydr8m8.widget

import android.content.Context
import androidx.compose.runtime.Composable
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
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

object WidgetUpdater {
    suspend fun updateAll(context: Context) {
        HydrationWidget().updateAll(context)
    }
}

private data class WidgetData(
    val config: WidgetConfig,
    val presets: List<Preset>,
    val waterMl: Int,
    val goalMl: Int,
    val percent: Int,
    val mood: MateMood,
    val streakDays: Int,
    val jokers: Int,
    val nextReminder: Instant?,
) {
    val fraction: Float get() = if (goalMl > 0) waterMl.toFloat() / goalMl else 0f
}

private data class Palette(
    val background: Color?,
    val text: Color,
    val muted: Color,
    val button: Color,
    val track: Int,
)

private fun paletteFor(config: WidgetConfig): Palette {
    val alpha = config.opacityPercent / 100f
    return when (config.background) {
        WidgetBackground.DARK -> {
            Palette(
                Color(BrandColors.NIGHT).copy(alpha = alpha),
                Color(BrandColors.TEXT),
                Color(BrandColors.MUTED),
                Color(0xFF173F63),
                BrandColors.TRACK,
            )
        }

        WidgetBackground.LIGHT -> {
            Palette(
                Color(0xFFF4F8FC).copy(alpha = alpha),
                Color(0xFF0F1D2A),
                Color(0xFF53606C),
                Color(0xFFD3E7FB),
                0xFFD9E6F2.toInt(),
            )
        }

        WidgetBackground.TRANSPARENT -> {
            Palette(null, Color.White, Color(0xFFDDE8F2), Color(0x33FFFFFF), 0x55FFFFFF)
        }
    }
}

class HydrationWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(TINY, WIDE_SHORT, WIDE, SQUARE, LARGE))

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val c = context.container
        c.repository.ensureSeeded()
        c.repository.ensureWidgetDefaults(appWidgetId)
        val config = c.repository.widgetConfig(appWidgetId)
        val presets = c.repository.surfaceNow(Surfaces.widget(appWidgetId))
        val history = c.repository.historyNow()
        val settings = c.settings.current()
        val today = history.todaySummary
        val zone = ZoneId.systemDefault()
        val data =
            WidgetData(
                config = config,
                presets = presets,
                waterMl = today.waterMl,
                goalMl = today.goalMl,
                percent = today.percent,
                mood =
                    MateMood.of(
                        Instant.now(),
                        settings.schedule.windowFor(history.today, zone),
                        today.waterMl,
                        today.goalMl,
                        today.hasAlcohol,
                    ),
                streakDays = history.streak.currentDays,
                jokers = history.streak.jokers,
                nextReminder = if (settings.reminders.enabled) c.reminderScheduler.nextReminderAt(history) else null,
            )
        provideContent { WidgetContent(data) }
    }

    companion object {
        val TINY = DpSize(50.dp, 50.dp)
        val WIDE_SHORT = DpSize(110.dp, 50.dp)
        val WIDE = DpSize(240.dp, 50.dp)
        val SQUARE = DpSize(110.dp, 110.dp)
        val LARGE = DpSize(240.dp, 110.dp)
    }
}

@Composable
private fun WidgetContent(data: WidgetData) {
    val size = LocalSize.current
    val palette = paletteFor(data.config)
    var modifier = GlanceModifier.fillMaxSize().cornerRadius(26.dp)
    palette.background?.let { modifier = modifier.background(ColorProvider(it)) }
    Box(modifier = modifier.padding(8.dp), contentAlignment = Alignment.Center) {
        when {
            size.height >= HydrationWidget.SQUARE.height && size.width >= HydrationWidget.LARGE.width -> {
                Large(
                    data,
                    palette,
                )
            }

            size.height >= HydrationWidget.SQUARE.height -> {
                Square(data, palette)
            }

            size.width >= HydrationWidget.WIDE.width -> {
                Wide(data, palette)
            }

            size.width >= HydrationWidget.WIDE_SHORT.width -> {
                WideShort(data, palette)
            }

            else -> {
                Tiny(data)
            }
        }
    }
}

private val openApp get() = actionStartActivity<MainActivity>()

@Composable
private fun AmountText(
    data: WidgetData,
    palette: Palette,
    sizeSp: Int = 13,
) {
    Text(
        text = "${Format.litres(data.waterMl)} / ${Format.litres(data.goalMl)}",
        style = TextStyle(color = ColorProvider(palette.text), fontSize = sizeSp.sp, fontWeight = FontWeight.Bold),
        maxLines = 1,
    )
}

@Composable
private fun Tiny(data: WidgetData) {
    val tapPreset = data.config.tapPresetId ?: data.presets.firstOrNull()?.id
    val action = tapPreset?.let { logAction(it) } ?: openApp
    Box(modifier = GlanceModifier.fillMaxSize().clickable(action), contentAlignment = Alignment.Center) {
        Image(
            provider = ImageProvider(WidgetBitmaps.ring(data.fraction, RING_PX, RING_PX * 0.1f)),
            contentDescription = null,
            modifier = GlanceModifier.fillMaxSize(),
        )
        Text(
            text = "${data.percent}%",
            style =
                TextStyle(
                    color = ColorProvider(Color(BrandColors.TEXT)),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                ),
        )
    }
}

@Composable
private fun WideShort(
    data: WidgetData,
    palette: Palette,
) {
    Column(
        modifier = GlanceModifier.fillMaxSize().clickable(openApp).padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${Format.litres(data.waterMl)} · ${data.percent}%",
            style = TextStyle(color = ColorProvider(palette.text), fontSize = 13.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
        )
        Spacer(GlanceModifier.height(6.dp))
        ProgressBar(data, palette)
    }
}

@Composable
private fun ProgressBar(
    data: WidgetData,
    palette: Palette,
) {
    Image(
        provider = ImageProvider(WidgetBitmaps.bar(data.fraction, BAR_W_PX, BAR_H_PX, palette.track)),
        contentDescription = null,
        modifier = GlanceModifier.fillMaxWidth().height(10.dp),
    )
}

@Composable
private fun Wide(
    data: WidgetData,
    palette: Palette,
) {
    Row(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        if (data.config.showMate) {
            Image(
                provider = ImageProvider(data.mood.drawable()),
                contentDescription = null,
                modifier = GlanceModifier.size(40.dp).clickable(openApp),
            )
            Spacer(GlanceModifier.width(8.dp))
        }
        Column(modifier = GlanceModifier.defaultWeight().clickable(openApp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AmountText(data, palette)
                Text(
                    text = "  ${data.percent}%",
                    style = TextStyle(color = ColorProvider(palette.muted), fontSize = 12.sp),
                    maxLines = 1,
                )
            }
            Spacer(GlanceModifier.height(5.dp))
            ProgressBar(data, palette)
        }
        Spacer(GlanceModifier.width(8.dp))
        Buttons(data, palette, maxButtons = 3, compact = true)
    }
}

@Composable
private fun Square(
    data: WidgetData,
    palette: Palette,
) {
    Column(modifier = GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = GlanceModifier.defaultWeight().fillMaxWidth().clickable(openApp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(WidgetBitmaps.ring(data.fraction, RING_PX, RING_PX * 0.09f, palette.track)),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxHeight(),
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (data.config.showMate) {
                    Image(
                        provider = ImageProvider(data.mood.drawable()),
                        contentDescription = null,
                        modifier = GlanceModifier.size(32.dp),
                    )
                }
                Text(
                    text = Format.litres(data.waterMl),
                    style =
                        TextStyle(
                            color = ColorProvider(palette.text),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                )
            }
        }
        Spacer(GlanceModifier.height(6.dp))
        Buttons(data, palette, maxButtons = 2, compact = true)
    }
}

@Composable
private fun Large(
    data: WidgetData,
    palette: Palette,
) {
    Row(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Image(
            provider = ImageProvider(WidgetBitmaps.glass(data.fraction, GLASS_W_PX, GLASS_H_PX)),
            contentDescription = null,
            modifier = GlanceModifier.width(70.dp).fillMaxHeight().clickable(openApp),
        )
        Spacer(GlanceModifier.width(10.dp))
        Column(modifier = GlanceModifier.defaultWeight().fillMaxHeight()) {
            Row(
                modifier = GlanceModifier.fillMaxWidth().clickable(openApp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    AmountText(data, palette, sizeSp = 16)
                    Text(
                        text = "${data.percent}%",
                        style = TextStyle(color = ColorProvider(palette.muted), fontSize = 12.sp),
                    )
                }
                if (data.config.showMate) {
                    Image(
                        provider = ImageProvider(data.mood.drawable()),
                        contentDescription = null,
                        modifier = GlanceModifier.size(36.dp),
                    )
                }
            }
            if (data.config.showInfo) {
                val info =
                    buildList {
                        add("🔥 ${data.streakDays}")
                        if (data.jokers > 0) add("🛡 ${data.jokers}")
                        data.nextReminder?.let { add("🔔 ${Format.time(it)}") }
                    }.joinToString("   ")
                Text(
                    text = info,
                    style = TextStyle(color = ColorProvider(palette.muted), fontSize = 11.sp),
                    maxLines = 1,
                )
            }
            Spacer(GlanceModifier.defaultWeight())
            Buttons(data, palette, maxButtons = 4, compact = false)
        }
    }
}

@Composable
private fun Buttons(
    data: WidgetData,
    palette: Palette,
    maxButtons: Int,
    compact: Boolean,
) {
    val presets = data.presets.take(if (data.config.showUndo) maxButtons - 1 else maxButtons)
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        presets.forEachIndexed { index, preset ->
            if (index > 0) Spacer(GlanceModifier.width(5.dp))
            Button(
                icon = preset.icon.drawable(),
                tint = BrandColors.forKind(preset.kind),
                label = if (compact) shortLabel(preset) else Format.presetLabel(preset),
                palette = palette,
                action = logAction(preset.id),
                compact = compact,
            )
        }
        if (data.config.showUndo) {
            if (presets.isNotEmpty()) Spacer(GlanceModifier.width(5.dp))
            Button(
                R.drawable.ic_undo,
                palette.muted.toArgbInt(),
                "",
                palette,
                actionRunCallback<UndoCallback>(),
                compact,
            )
        }
    }
}

private fun Color.toArgbInt(): Int =
    android.graphics.Color.argb(alpha, red, green, blue)

private fun shortLabel(preset: Preset): String =
    if (preset.volumeMl <
        ML_PER_LITRE
    ) {
        "${preset.volumeMl}"
    } else {
        Format.volume(preset.volumeMl).removeSuffix(" L") + "L"
    }

@Composable
private fun androidx.glance.layout.RowScope.Button(
    icon: Int,
    tint: Int,
    label: String,
    palette: Palette,
    action: androidx.glance.action.Action,
    compact: Boolean,
) {
    Row(
        modifier =
            GlanceModifier
                .defaultWeight()
                .height(if (compact) 36.dp else 40.dp)
                .cornerRadius(14.dp)
                .background(ColorProvider(palette.button))
                .clickable(action),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            provider = ImageProvider(icon),
            contentDescription = label,
            colorFilter = ColorFilter.tint(ColorProvider(Color(tint))),
            modifier = GlanceModifier.size(18.dp),
        )
        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = TextStyle(color = ColorProvider(palette.text), fontSize = 11.sp),
                maxLines = 1,
            )
        }
    }
}

private val PresetKey = ActionParameters.Key<Long>("preset_id")

private fun logAction(presetId: Long) = actionRunCallback<LogPresetCallback>(actionParametersOf(PresetKey to presetId))

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

private const val RING_PX = 240
private const val BAR_W_PX = 600
private const val BAR_H_PX = 24
private const val GLASS_W_PX = 180
private const val GLASS_H_PX = 260
private const val ML_PER_LITRE = 1000
