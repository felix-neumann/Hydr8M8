package de.felixneumann.hydr8m8

import android.app.Application
import android.content.Context
import de.felixneumann.hydr8m8.data.HydrationRepository
import de.felixneumann.hydr8m8.data.SettingsRepository
import de.felixneumann.hydr8m8.data.db.AppDatabase
import de.felixneumann.hydr8m8.notifications.Notifications
import de.felixneumann.hydr8m8.notifications.ProgressNotifier
import de.felixneumann.hydr8m8.notifications.ReminderScheduler
import de.felixneumann.hydr8m8.shortcuts.ShortcutPublisher
import de.felixneumann.hydr8m8.sound.SoundPlayer
import de.felixneumann.hydr8m8.update.GitHubReleases
import de.felixneumann.hydr8m8.update.UpdateInstaller
import de.felixneumann.hydr8m8.update.UpdateManager
import de.felixneumann.hydr8m8.weather.WeatherService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Manual dependency container; one instance per process, owned by [Hydr8M8App]. */
class AppContainer(
    context: Context,
) {
    val appContext: Context = context.applicationContext
    val appScope = CoroutineScope(SupervisorJob())
    val database: AppDatabase = AppDatabase.create(appContext)
    val settings = SettingsRepository(appContext)
    val repository = HydrationRepository(database, settings)
    val sounds = SoundPlayer(appContext)
    val reminderScheduler = ReminderScheduler(appContext, repository, settings)
    val progressNotifier = ProgressNotifier(appContext)
    val shortcuts = ShortcutPublisher(appContext)
    val weather = WeatherService(appContext, repository, settings)
    val actions = HydrationActions(this)
    val updates =
        UpdateManager(
            scope = appScope,
            installedVersionName = BuildConfig.VERSION_NAME,
            releases = GitHubReleases(BuildConfig.GITHUB_REPO),
            installer = UpdateInstaller(appContext),
        )
}

class Hydr8M8App : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        container.appScope.launch {
            container.repository.ensureSeeded()
            container.actions.refreshAll()
        }
    }
}

val Context.container: AppContainer get() = (applicationContext as Hydr8M8App).container
