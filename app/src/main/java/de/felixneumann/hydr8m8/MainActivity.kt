package de.felixneumann.hydr8m8

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
import de.felixneumann.hydr8m8.ui.theme.HydrTheme
import de.felixneumann.hydr8m8.ui.theme.ThemedContent
import de.felixneumann.hydr8m8.ui.theme.isDark
import de.felixneumann.hydr8m8.ui.today.TodayScreen
import de.felixneumann.hydr8m8.ui.update.UpdateDialog
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ThemedContent {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(HydrTheme.colors.backgroundBrush),
                ) {
                    AppRoot(viewModel)
                }
            }
        }
    }
}
