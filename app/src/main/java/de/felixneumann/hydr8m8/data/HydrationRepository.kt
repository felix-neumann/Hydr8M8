package de.felixneumann.hydr8m8.data

import de.felixneumann.hydr8m8.data.db.AppDatabase
import de.felixneumann.hydr8m8.data.db.EntryEntity
import de.felixneumann.hydr8m8.data.db.GoalChangeEntity
import de.felixneumann.hydr8m8.data.db.PresetEntity
import de.felixneumann.hydr8m8.data.db.WeatherEntity
import de.felixneumann.hydr8m8.data.db.WidgetConfigEntity
import de.felixneumann.hydr8m8.domain.BuiltInPresets
import de.felixneumann.hydr8m8.domain.DayClock
import de.felixneumann.hydr8m8.domain.DrinkIcon
import de.felixneumann.hydr8m8.domain.DrinkKind
import de.felixneumann.hydr8m8.domain.Entry
import de.felixneumann.hydr8m8.domain.GoalChange
import de.felixneumann.hydr8m8.domain.History
import de.felixneumann.hydr8m8.domain.Preset
import de.felixneumann.hydr8m8.domain.Surfaces
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/** Per-widget appearance; the preset buttons live in the surface table under `widget:<id>`. */
data class WidgetConfig(
    val appWidgetId: Int,
    val showMate: Boolean = true,
    val showInfo: Boolean = true,
    val showUndo: Boolean = false,
    val tapPresetId: Long? = null,
    val background: WidgetBackground = WidgetBackground.DARK,
    val opacityPercent: Int = 90,
)

enum class WidgetBackground { DARK, LIGHT, TRANSPARENT }

class HydrationRepository(
    private val db: AppDatabase,
    private val settings: SettingsRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    fun now(): Instant = clock.instant()

    suspend fun dayClock(): DayClock = DayClock(clock.zone, settings.current().dayStart)

    // ---------------------------------------------------------------- seeding

    /** Creates the built-in preset library and default surface selections on first launch. */
    suspend fun ensureSeeded() {
        if (db.presets().count() > 0) return
        val idsByKey =
            BuiltInPresets.all.mapIndexed { index, t ->
                t.key to
                    db.presets().insert(
                        PresetEntity(
                            kind = t.icon.kind.name,
                            volumeMl = t.volumeMl,
                            abvPercent = t.abvPercent,
                            icon = t.icon.name,
                            builtInKey = t.key,
                            hidden = false,
                            sortOrder = index,
                        ),
                    )
            }.toMap()
        db.surfaces().replace(Surfaces.HOME, BuiltInPresets.defaultHome.mapNotNull(idsByKey::get))
        db.surfaces().replace(Surfaces.SHORTCUTS, BuiltInPresets.defaultShortcuts.mapNotNull(idsByKey::get))
        db.surfaces().replace(Surfaces.NOTIFICATION, BuiltInPresets.defaultNotification.mapNotNull(idsByKey::get))
    }

    // ---------------------------------------------------------------- presets

    val presets: Flow<List<Preset>> = db.presets().observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun allPresets(): List<Preset> = db.presets().all().map { it.toDomain() }

    suspend fun preset(id: Long): Preset? = db.presets().byId(id)?.toDomain()

    suspend fun savePreset(preset: Preset): Long =
        if (preset.id == 0L) {
            val order = (db.presets().all().maxOfOrNull { it.sortOrder } ?: 0) + 1
            db.presets().insert(preset.copy(sortOrder = order).toEntity())
        } else {
            db.presets().update(preset.toEntity())
            preset.id
        }

    suspend fun deletePreset(preset: Preset) {
        if (preset.isBuiltIn) return
        db.surfaces().removePreset(preset.id)
        db.presets().deleteCustom(preset.id)
    }

    /** Presets selected for a surface, in order, skipping hidden ones. */
    fun surface(surface: String): Flow<List<Preset>> =
        combine(db.surfaces().observeAll(), presets) { slots, all ->
            val byId = all.associateBy { it.id }
            slots.filter { it.surface == surface }.mapNotNull { byId[it.presetId] }.filterNot { it.hidden }
        }

    suspend fun surfaceNow(surface: String): List<Preset> {
        val byId = allPresets().associateBy { it.id }
        return db.surfaces().slots(surface).mapNotNull { byId[it.presetId] }.filterNot { it.hidden }
    }

    suspend fun setSurface(
        surface: String,
        presetIds: List<Long>,
    ) {
        db.surfaces().replace(surface, presetIds.distinct().take(Surfaces.maxFor(surface)))
    }

    // ---------------------------------------------------------------- entries

    suspend fun logPreset(
        presetId: Long,
        at: Instant = now(),
    ): Entry? {
        val p = preset(presetId) ?: return null
        return log(p.kind, p.volumeMl, p.abvPercent, p.icon, at)
    }

    suspend fun log(
        kind: DrinkKind,
        volumeMl: Int,
        abvPercent: Double?,
        icon: DrinkIcon,
        at: Instant = now(),
    ): Entry {
        val entity =
            EntryEntity(
                atMillis = at.toEpochMilli(),
                kind = kind.name,
                volumeMl = volumeMl,
                abvPercent = if (kind == DrinkKind.ALCOHOL) abvPercent else null,
                icon = icon.name,
                createdAtMillis = now().toEpochMilli(),
            )
        val id = db.entries().insert(entity)
        return entity.copy(id = id).toDomain()
    }

    suspend fun updateEntry(entry: Entry) {
        db.entries().update(entry.toEntity())
    }

    suspend fun deleteEntry(id: Long) {
        db.entries().delete(id)
    }

    /** Removes the most recently created entry if it is at most 24 h old. */
    suspend fun undoLast(): Entry? {
        val since = now().minus(UNDO_WINDOW).toEpochMilli()
        val last = db.entries().lastCreatedSince(since) ?: return null
        db.entries().delete(last.id)
        return last.toDomain()
    }

    suspend fun lastEntryAt(): Instant? = db.entries().lastEntryAt(now().toEpochMilli())?.let(Instant::ofEpochMilli)

    // ---------------------------------------------------------------- goal & weather

    /** Sets the base goal from [from] (default: today) onwards; past days keep their goal. */
    suspend fun setBaseGoal(
        goalMl: Int,
        from: LocalDate? = null,
    ) {
        val date = from ?: dayClock().logicalDate(now())
        db.goals().upsert(GoalChangeEntity(date.toEpochDay(), goalMl))
        settings.update { it.copy(defaultGoalMl = goalMl) }
    }

    suspend fun weatherFor(date: LocalDate): WeatherEntity? = db.weather().byDay(date.toEpochDay())

    suspend fun saveWeather(
        date: LocalDate,
        maxTempC: Double,
        bonusMl: Int,
    ) {
        db.weather().upsert(WeatherEntity(date.toEpochDay(), maxTempC, bonusMl))
    }

    // ---------------------------------------------------------------- history

    /** Ticks every minute so that "today" and time-based state stay current in the UI. */
    private val minuteTicker: Flow<Instant> =
        flow {
            while (true) {
                emit(now())
                delay(TICK)
            }
        }

    val history: Flow<History> =
        combine(
            db.entries().observeAll(),
            db.goals().observeAll(),
            db.weather().observeAll(),
            settings.settings,
            minuteTicker,
        ) { entries, goals, weather, s, now ->
            buildHistory(entries, goals, weather, s, now)
        }

    suspend fun historyNow(): History =
        buildHistory(
            db.entries().all(),
            db.goals().all(),
            db.weather().all(),
            settings.current(),
            now(),
        )

    private fun buildHistory(
        entries: List<EntryEntity>,
        goals: List<GoalChangeEntity>,
        weather: List<WeatherEntity>,
        s: AppSettings,
        now: Instant,
    ): History {
        val dayClock = DayClock(clock.zone, s.dayStart)
        return History(
            clock = dayClock,
            entries = entries.map { it.toDomain() },
            goalChanges = goals.map { GoalChange(LocalDate.ofEpochDay(it.effectiveFromEpochDay), it.goalMl) },
            weatherBonus = weather.associate { LocalDate.ofEpochDay(it.epochDay) to it.bonusMl },
            fallbackGoalMl = s.defaultGoalMl,
            today = dayClock.logicalDate(now),
        )
    }

    // ---------------------------------------------------------------- widgets

    suspend fun widgetConfig(appWidgetId: Int): WidgetConfig =
        db.widgetConfigs().byId(appWidgetId)?.toDomain() ?: WidgetConfig(appWidgetId)

    fun widgetConfigFlow(appWidgetId: Int): Flow<WidgetConfig> =
        db.widgetConfigs().observeAll().map { all ->
            all.firstOrNull { it.appWidgetId == appWidgetId }?.toDomain() ?: WidgetConfig(appWidgetId)
        }

    suspend fun saveWidgetConfig(config: WidgetConfig) {
        db.widgetConfigs().upsert(config.toEntity())
    }

    suspend fun deleteWidget(appWidgetId: Int) {
        db.widgetConfigs().delete(appWidgetId)
        db.surfaces().clear(Surfaces.widget(appWidgetId))
    }

    /** Gives a freshly placed widget the default buttons if it has none yet. */
    suspend fun ensureWidgetDefaults(appWidgetId: Int) {
        val key = Surfaces.widget(appWidgetId)
        if (db.surfaces().slots(key).isNotEmpty()) return
        val byKey = allPresets().filter { it.builtInKey != null }.associateBy { it.builtInKey }
        setSurface(key, BuiltInPresets.defaultWidget.mapNotNull { byKey[it]?.id })
    }

    companion object {
        val UNDO_WINDOW: Duration = Duration.ofHours(24)
        private const val TICK = 60_000L
    }
}

// -------------------------------------------------------------------- mapping

private fun PresetEntity.toDomain() =
    Preset(
        id = id,
        kind = DrinkKind.valueOf(kind),
        volumeMl = volumeMl,
        abvPercent = abvPercent,
        icon = DrinkIcon.valueOf(icon),
        builtInKey = builtInKey,
        hidden = hidden,
        sortOrder = sortOrder,
    )

private fun Preset.toEntity() =
    PresetEntity(
        id = id,
        kind = kind.name,
        volumeMl = volumeMl,
        abvPercent = if (kind == DrinkKind.ALCOHOL) abvPercent else null,
        icon = icon.name,
        builtInKey = builtInKey,
        hidden = hidden,
        sortOrder = sortOrder,
    )

private fun EntryEntity.toDomain() =
    Entry(
        id = id,
        at = Instant.ofEpochMilli(atMillis),
        kind = DrinkKind.valueOf(kind),
        volumeMl = volumeMl,
        abvPercent = abvPercent,
        icon = DrinkIcon.valueOf(icon),
        createdAt = Instant.ofEpochMilli(createdAtMillis),
    )

private fun Entry.toEntity() =
    EntryEntity(
        id = id,
        atMillis = at.toEpochMilli(),
        kind = kind.name,
        volumeMl = volumeMl,
        abvPercent = if (kind == DrinkKind.ALCOHOL) abvPercent else null,
        icon = icon.name,
        createdAtMillis = createdAt.toEpochMilli(),
    )

private fun WidgetConfigEntity.toDomain() =
    WidgetConfig(
        appWidgetId = appWidgetId,
        showMate = showMate,
        showInfo = showInfo,
        showUndo = showUndo,
        tapPresetId = tapPresetId,
        background = WidgetBackground.valueOf(background),
        opacityPercent = opacityPercent,
    )

private fun WidgetConfig.toEntity() =
    WidgetConfigEntity(
        appWidgetId = appWidgetId,
        showMate = showMate,
        showInfo = showInfo,
        showUndo = showUndo,
        tapPresetId = tapPresetId,
        background = background.name,
        opacityPercent = opacityPercent,
    )
