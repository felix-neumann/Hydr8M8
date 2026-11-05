package de.felixneumann.hydr8m8

import android.os.Bundle
import androidx.activity.ComponentActivity
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
import de.felixneumann.hydr8m8.domain.Entry
import de.felixneumann.hydr8m8.domain.MateMood
import de.felixneumann.hydr8m8.domain.Preset
import de.felixneumann.hydr8m8.domain.Surfaces
import de.felixneumann.hydr8m8.ui.Format
import de.felixneumann.hydr8m8.ui.MainViewModel
import de.felixneumann.hydr8m8.ui.UiEvent
import de.felixneumann.hydr8m8.ui.calendar.CalendarScreen
import de.felixneumann.hydr8m8.ui.entry.EntrySheet
import de.felixneumann.hydr8m8.ui.onboarding.OnboardingScreen
import de.felixneumann.hydr8m8.ui.settings.PresetLibraryScreen
import de.felixneumann.hydr8m8.ui.settings.SettingsScreen
import de.felixneumann.hydr8m8.ui.settings.SurfaceEditorScreen
import de.felixneumann.hydr8m8.ui.settings.surfaceTitle
import de.felixneumann.hydr8m8.ui.stats.StatsScreen
import de.felixneumann.hydr8m8.ui.theme.Hydr8M8Theme
import de.felixneumann.hydr8m8.ui.theme.HydrTheme
import de.felixneumann.hydr8m8.ui.today.TodayScreen
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Hydr8M8Theme {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(HydrTheme.colors.backgroundBrush),
                ) {
                    HydrApp(viewModel)
                }
            }
        }
    }
}

private object Routes {
    const val TODAY = "today"
    const val CALENDAR = "calendar"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val PRESETS = "presets"
    const val SURFACE = "surface/{key}"

    fun surface(key: String) = "surface/$key"
}

private data class Tab(
    val route: String,
    val label: Int,
    val icon: ImageVector,
)

private val tabs =
    listOf(
        Tab(Routes.TODAY, R.string.nav_today, Icons.Default.WaterDrop),
        Tab(Routes.CALENDAR, R.string.nav_calendar, Icons.Default.CalendarMonth),
        Tab(Routes.STATS, R.string.nav_stats, Icons.Default.BarChart),
        Tab(Routes.SETTINGS, R.string.nav_settings, Icons.Default.Settings),
    )

/** What the entry sheet is opened for: a new entry on [date] or editing [entry]. */
private data class SheetRequest(
    val date: LocalDate,
    val entry: Entry? = null,
)

@Suppress("ktlint:compose:vm-forwarding-check") // App root shares the activity ViewModel.
@Composable
private fun HydrApp(vm: MainViewModel) {
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
            onFinish = { goal, schedule, homeIds ->
                vm.completeOnboarding(goal, { it.copy(schedule = schedule) }, homeIds)
            },
            onLocationGrant = vm::onLocationPermissionGranted,
        )
        return
    }
    MainScaffold(vm, h, s, presets, homePresets)
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
    var sheet by remember { mutableStateOf<SheetRequest?>(null) }
    val resources = LocalResources.current

    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is UiEvent.Logged -> {
                    val result =
                        snackbar.showSnackbar(
                            message = resources.getString(R.string.logged_snackbar, Format.volume(event.entry.volumeMl)),
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

    Scaffold(
        containerColor = Color.Transparent,
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
                    onUpdate = vm::updateSettings,
                    onSetGoal = vm::setBaseGoal,
                    onOpenPresets = { nav.navigate(Routes.PRESETS) },
                    onOpenSurface = { nav.navigate(Routes.surface(it)) },
                    onLocationGrant = vm::onLocationPermissionGranted,
                )
            }
            composable(Routes.PRESETS) {
                PresetLibraryScreen(presets = presets, onSave = { vm.savePreset(it) }, onDelete = vm::deletePreset)
            }
            composable(Routes.SURFACE) { entry ->
                val key = entry.arguments?.getString("key") ?: Surfaces.HOME
                val selected by remember(key) { vm.surface(key) }.collectAsStateWithLifecycle()
                SurfaceEditorScreen(key, selected, presets, onChange = { vm.setSurface(key, it) })
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
                            Preset(0, draft.kind, draft.volumeMl, draft.abvPercent, draft.icon, null, false, 0),
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

private fun subTitle(
    route: String?,
    key: String?,
): Int =
    when (route) {
        Routes.PRESETS -> R.string.preset_library
        Routes.SURFACE -> surfaceTitle(key ?: Surfaces.HOME)
        else -> R.string.app_name
    }

@Composable
private fun BottomBar(
    nav: NavHostController,
    route: String?,
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f)) {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = route == tab.route || (route == null && tab.route == Routes.TODAY),
                onClick = {
                    nav.navigate(tab.route) {
                        popUpTo(Routes.TODAY) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(stringResource(tab.label)) },
            )
        }
    }
}
