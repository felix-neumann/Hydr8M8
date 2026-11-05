package de.felixneumann.hydr8m8.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.felixneumann.hydr8m8.LogResult
import de.felixneumann.hydr8m8.container
import de.felixneumann.hydr8m8.data.AppSettings
import de.felixneumann.hydr8m8.domain.DrinkIcon
import de.felixneumann.hydr8m8.domain.DrinkKind
import de.felixneumann.hydr8m8.domain.Entry
import de.felixneumann.hydr8m8.domain.History
import de.felixneumann.hydr8m8.domain.Preset
import de.felixneumann.hydr8m8.domain.Surfaces
import de.felixneumann.hydr8m8.sound.Sound
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

sealed interface UiEvent {
    data class Logged(
        val entry: Entry,
    ) : UiEvent

    data class Undone(
        val entry: Entry?,
    ) : UiEvent
}

class MainViewModel(
    app: Application,
) : AndroidViewModel(app) {
    private val c = app.container
    private val repo = c.repository

    private fun <T> Flow<T>.state(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), initial)

    val history: StateFlow<History?> = repo.history.map<History, History?> { it }.state(null)
    val settings: StateFlow<AppSettings?> = c.settings.settings.map<AppSettings, AppSettings?> { it }.state(null)
    val presets: StateFlow<List<Preset>> = repo.presets.state(emptyList())

    fun surface(key: String): StateFlow<List<Preset>> = repo.surface(key).state(emptyList())

    val homePresets: StateFlow<List<Preset>> = surface(Surfaces.HOME)

    val nextReminder: StateFlow<Instant?> =
        combine(repo.history, c.settings.settings, c.settings.runtime) { h, s, _ ->
            if (s.reminders.enabled) c.reminderScheduler.nextReminderAt(h) else null
        }.state(null)

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<UiEvent> = _events

    init {
        viewModelScope.launch {
            repo.ensureSeeded()
            c.weather.captureLocation()
            c.actions.refreshAll()
        }
    }

    fun logPreset(preset: Preset) {
        viewModelScope.launch { c.actions.logPreset(preset.id)?.let(::onLogged) }
    }

    fun log(
        kind: DrinkKind,
        volumeMl: Int,
        abvPercent: Double?,
        icon: DrinkIcon,
        at: Instant,
    ) {
        viewModelScope.launch { onLogged(c.actions.log(kind, volumeMl, abvPercent, icon, at)) }
    }

    private fun onLogged(result: LogResult) {
        val sound =
            when {
                result.newMilestone != null -> Sound.STREAK
                result.goalJustReached -> Sound.GOAL
                else -> Sound.DROP
            }
        c.sounds.play(sound, settings.value?.soundsEnabled ?: true)
        _events.tryEmit(UiEvent.Logged(result.entry))
    }

    fun undo() {
        viewModelScope.launch { _events.tryEmit(UiEvent.Undone(c.actions.undoLast())) }
    }

    fun deleteEntryById(id: Long) {
        viewModelScope.launch { c.actions.deleteEntry(id) }
    }

    fun updateEntry(entry: Entry) {
        viewModelScope.launch { c.actions.updateEntry(entry) }
    }

    fun setSurface(
        key: String,
        ids: List<Long>,
    ) {
        viewModelScope.launch {
            repo.setSurface(key, ids)
            c.actions.refreshAll()
        }
    }

    /** Saves a preset; when [addToSurface] is given, the preset is appended there if space is left. */
    fun savePreset(
        preset: Preset,
        addToSurface: String? = null,
    ) {
        viewModelScope.launch {
            val id = repo.savePreset(preset)
            if (addToSurface != null) {
                val current = repo.surfaceNow(addToSurface).map { it.id }
                if (id !in current &&
                    current.size < Surfaces.maxFor(addToSurface)
                ) {
                    repo.setSurface(addToSurface, current + id)
                }
            }
            c.actions.refreshAll()
        }
    }

    fun deletePreset(preset: Preset) {
        viewModelScope.launch {
            repo.deletePreset(preset)
            c.actions.refreshAll()
        }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            c.settings.update(transform)
            c.actions.refreshAll()
        }
    }

    fun setBaseGoal(goalMl: Int) {
        viewModelScope.launch {
            repo.setBaseGoal(goalMl)
            c.actions.refreshAll()
        }
    }

    fun onLocationPermissionGranted() {
        viewModelScope.launch {
            c.weather.captureLocation()
            c.actions.refreshAll()
        }
    }

    fun completeOnboarding(
        goalMl: Int,
        settingsTransform: (AppSettings) -> AppSettings,
        homePresetIds: List<Long>,
    ) {
        viewModelScope.launch {
            repo.setBaseGoal(goalMl)
            repo.setSurface(Surfaces.HOME, homePresetIds)
            c.settings.update { settingsTransform(it).copy(onboardingDone = true, defaultGoalMl = goalMl) }
            c.actions.refreshAll()
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
