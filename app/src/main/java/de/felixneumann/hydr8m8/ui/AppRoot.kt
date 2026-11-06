package de.felixneumann.hydr8m8.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.felixneumann.hydr8m8.R
import de.felixneumann.hydr8m8.container
import de.felixneumann.hydr8m8.data.ThemeMode
import de.felixneumann.hydr8m8.domain.Entry
import de.felixneumann.hydr8m8.domain.MateMood
import de.felixneumann.hydr8m8.domain.Preset
import de.felixneumann.hydr8m8.domain.Surfaces
import de.felixneumann.hydr8m8.ui.AppRoot
import de.felixneumann.hydr8m8.ui.Format
import de.felixneumann.hydr8m8.ui.MainViewModel
import de.felixneumann.hydr8m8.ui.UiEvent
import de.felixneumann.hydr8m8.ui.calendar.CalendarScreen
import de.felixneumann.hydr8m8.ui.entry.EntrySheet
import de.felixneumann.hydr8m8.ui.navigation.BottomBar
import de.felixneumann.hydr8m8.ui.navigation.Routes
import de.felixneumann.hydr8m8.ui.navigation.subTitle
import de.felixneumann.hydr8m8.ui.navigation.tabs
import de.felixneumann.hydr8m8.ui.onboarding.OnboardingScreen
import de.felixneumann.hydr8m8.ui.settings.PresetLibraryScreen
import de.felixneumann.hydr8m8.ui.settings.SettingsScreen
import de.felixneumann.hydr8m8.ui.settings.SurfaceEditorScreen
import de.felixneumann.hydr8m8.ui.settings.surfaceTitle
import de.felixneumann.hydr8m8.ui.stats.StatsScreen
import de.felixneumann.hydr8m8.ui.theme.Hydr8M8Theme
import de.felixneumann.hydr8m8.ui.theme.HydrTheme
import de.felixneumann.hydr8m8.ui.theme.isDark
import de.felixneumann.hydr8m8.ui.today.TodayScreen
import de.felixneumann.hydr8m8.ui.update.UpdateDialog
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** What the entry sheet is opened for: a new entry on [date] or editing [entry]. */
private data class SheetRequest(
    val date: LocalDate,
    val entry: Entry? = null,
)

@Suppress("ktlint:compose:vm-forwarding-check") // App root shares the activity ViewModel.
@Composable
fun AppRoot(vm: MainViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val presets by vm.presets.collectAsStateWithLifecycle()
    val homePresets by vm.homePresets.collectAsStateWithLifecycle()

    val s = settings ?: return
    val h = history ?: return

    if (!s.onboardingDone) {
        OnboardingScreen(
            initialHomePresets = homePresets,
            library = presets,
            onFinish = vm::completeOnboarding,
            onLocationGrant = vm::onLocationPermissionGranted,
            onCreatePreset = vm::createPreset,
        )
        return
    }
    MainScaffold(vm, h, s, presets, homePresets)

    val updateState by vm.updateState.collectAsStateWithLifecycle()
    val updateDialog by vm.updateDialogVisible.collectAsStateWithLifecycle()
    if (updateDialog) {
        UpdateDialog(
            state = updateState,
            onDownload = vm::downloadUpdate,
            onInstall = vm::installUpdate,
            onRetryCheck = vm::checkForUpdates,
            onDismiss = vm::dismissUpdate,
        )
    }
}

@Suppress("ktlint:compose:vm-forwarding-check") // App root: both composables use the activity ViewModel.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold(
    vm: MainViewModel,
    history: de.felixneumann.hydr8m8.domain.History,
    settings: de.felixneumann.hydr8m8.data.AppSettings,
    presets: List<Preset>,
    homePresets: List<Preset>,
) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val isTab = tabs.any { it.route == route } || route == null
    val snackbar = remember { SnackbarHostState() }
    val nextReminder by vm.nextReminder.collectAsStateWithLifecycle()
    val updateState by vm.updateState.collectAsStateWithLifecycle()
    var sheet by remember { mutableStateOf<SheetRequest?>(null) }
    val resources = LocalResources.current

    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            snackbar.currentSnackbarData?.dismiss()
            scope.launch {
                when (event) {
                    is UiEvent.Logged -> {
                        val result =
                            snackbar.showSnackbar(
                                message =
                                    resources.getString(
                                        R.string.logged_snackbar,
                                        Format.volume(event.entry.volumeMl),
                                    ),
                                actionLabel = resources.getString(R.string.undo),
                                duration = SnackbarDuration.Short,
                            )
                        if (result == SnackbarResult.ActionPerformed) vm.undo()
                    }

                    is UiEvent.Undone -> {
                        snackbar.showSnackbar(
                            event.entry?.let {
                                resources.getString(R.string.undo_done, Format.volume(it.volumeMl), Format.time(it.at))
                            }
                                ?: resources.getString(R.string.undo_nothing),
                        )
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            if (!isTab) {
                TopAppBar(
                    title = { Text(stringResource(subTitle(route, backStack?.arguments?.getString("key")))) },
                    navigationIcon = {
                        IconButton(onClick = { nav.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )
            }
        },
        bottomBar = { if (isTab) BottomBar(nav, route) },
        floatingActionButton = {
            if (route == Routes.TODAY || route == null) {
                FloatingActionButton(
                    onClick = { sheet = SheetRequest(history.today) },
                    containerColor = MaterialTheme.colorScheme.primary,
                ) { Icon(Icons.Default.Add, stringResource(R.string.add_custom)) }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Routes.TODAY, modifier = Modifier.padding(padding)) {
            composable(Routes.TODAY) {
                val window = settings.schedule.windowFor(history.today, ZoneId.systemDefault())
                val today = history.todaySummary
                TodayScreen(
                    date = history.today,
                    today = today,
                    streak = history.streak,
                    mood = MateMood.of(Instant.now(), window, today.waterMl, today.goalMl, today.hasAlcohol),
                    nextReminder = nextReminder,
                    homePresets = homePresets,
                    onPresetClick = vm::logPreset,
                    onConfigureHome = { nav.navigate(Routes.surface(Surfaces.HOME)) },
                )
            }
            composable(Routes.CALENDAR) {
                CalendarScreen(
                    history = history,
                    onEditEntry = { sheet = SheetRequest(history.today, it) },
                    onAddForDay = { sheet = SheetRequest(it) },
                )
            }
            composable(Routes.STATS) { StatsScreen(history) }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    settings = settings,
                    currentGoalMl = history.todaySummary.goal.baseMl,
                    updateState = updateState,
                    onUpdate = vm::updateSettings,
                    onSetGoal = vm::setBaseGoal,
                    onApplyProfile = vm::applyCalculatedGoal,
                    onOpenPresets = { nav.navigate(Routes.PRESETS) },
                    onOpenSurface = { nav.navigate(Routes.surface(it)) },
                    onLocationGrant = vm::onLocationPermissionGranted,
                    onCheckForUpdate = vm::checkForUpdates,
                )
            }
            composable(Routes.PRESETS) {
                PresetLibraryScreen(presets = presets, onSave = { vm.savePreset(it) }, onDelete = vm::deletePreset)
            }
            composable(Routes.SURFACE) { entry ->
                val key = entry.arguments?.getString("key") ?: Surfaces.HOME
                val selected by remember(key) { vm.surface(key) }.collectAsStateWithLifecycle()
                SurfaceEditorScreen(key, selected, presets, onChange = {
                    vm.setSurface(key, it)
                }, onCreatePreset = vm::createPreset)
            }
        }
    }

    sheet?.let { request ->
        EntrySheet(
            initialDate = request.date,
            today = history.today,
            editing = request.entry,
            onDismiss = { sheet = null },
            onSave = { draft ->
                val editing = request.entry
                if (editing == null) {
                    vm.log(draft.kind, draft.volumeMl, draft.abvPercent, draft.icon, draft.at)
                    if (draft.saveAsFavorite) {
                        vm.savePreset(
                            Preset(
                                0,
                                draft.kind,
                                draft.volumeMl,
                                draft.abvPercent,
                                draft.icon,
                                null,
                                false,
                                0,
                                draft.favoriteName,
                            ),
                            addToSurface = Surfaces.HOME,
                        )
                    }
                } else {
                    vm.updateEntry(
                        editing.copy(
                            kind = draft.kind,
                            volumeMl = draft.volumeMl,
                            abvPercent = draft.abvPercent,
                            icon = draft.icon,
                            at = draft.at,
                        ),
                    )
                }
                sheet = null
            },
            onDelete =
                request.entry?.let { e ->
                    {
                        vm.deleteEntryById(e.id)
                        sheet = null
                    }
                },
        )
    }
}
