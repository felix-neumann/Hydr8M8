package de.felixneumann.hydr8m8.notifications

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.IconCompat
import de.felixneumann.hydr8m8.R
import de.felixneumann.hydr8m8.data.AppSettings
import de.felixneumann.hydr8m8.domain.History
import de.felixneumann.hydr8m8.domain.MateMood
import de.felixneumann.hydr8m8.ui.BrandColors
import de.felixneumann.hydr8m8.ui.Format
import de.felixneumann.hydr8m8.ui.drawable
import de.felixneumann.hydr8m8.ui.label
import java.time.Instant
import java.time.ZoneId

/**
 * Ongoing progress notification during the wake window. On Android 16+ it requests promotion to a
 * Live Update (status chip / Samsung Now Bar). Once the goal is reached it turns into a short
 * celebration that times out.
 */
class ProgressNotifier(
    private val context: Context,
) {
    @SuppressLint("MissingPermission") // checked via Notifications.canPost()
    fun update(
        history: History,
        settings: AppSettings,
    ) {
        val nm = NotificationManagerCompat.from(context)
        val zone = ZoneId.systemDefault()
        val now = Instant.now()
        val today = history.todaySummary
        val window = settings.schedule.windowFor(history.today, zone)
        val awake = settings.schedule.isAwake(now, zone)

        if (!settings.liveUpdateEnabled || !awake || !Notifications.canPost(context)) {
            nm.cancel(Notifications.ID_PROGRESS)
            return
        }

        if (today.reached) {
            if (isShowingOngoing()) nm.notify(Notifications.ID_PROGRESS, celebration(today.waterMl))
            return
        }

        val mood = MateMood.of(now, window, today.waterMl, today.goalMl, today.hasAlcohol)
        val title = "${Format.litres(today.waterMl)} / ${Format.litres(today.goalMl)}"
        // Privacy: the lock screen / Now Bar only gets a neutral version. The mood can reveal
        // alcohol consumption ("Tipsy"), so it is shown only once the phone is unlocked.
        val lockScreenVersion =
            progressBuilder(title, "${today.percent} %", MateMood.HAPPY, today.waterMl, today.goalMl).build()
        val notification =
            progressBuilder(
                title,
                "${today.percent} % · ${context.getString(mood.label())}",
                mood,
                today.waterMl,
                today.goalMl,
            ).setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPublicVersion(lockScreenVersion)
                .build()
        nm.notify(Notifications.ID_PROGRESS, notification)
    }

    private fun progressBuilder(
        title: String,
        text: String,
        trackerMood: MateMood,
        waterMl: Int,
        goalMl: Int,
    ): NotificationCompat.Builder {
        val style =
            NotificationCompat
                .ProgressStyle()
                .setProgressSegments(
                    listOf(NotificationCompat.ProgressStyle.Segment(goalMl).setColor(BrandColors.WATER)),
                )
                .setProgress(waterMl.coerceAtMost(goalMl))
                .setProgressTrackerIcon(IconCompat.createWithResource(context, trackerMood.drawable()))
        return NotificationCompat
            .Builder(context, Notifications.CHANNEL_PROGRESS)
            .setSmallIcon(R.drawable.ic_stat_drop)
            .setColor(BrandColors.WATER)
            .setContentTitle(title)
            .setContentText(text)
            .setShortCriticalText(text.substringBefore(" ·").replace(" ", ""))
            .setStyle(style)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setRequestPromotedOngoing(true)
            .setContentIntent(Notifications.openAppIntent(context))
    }

    private fun isShowingOngoing(): Boolean =
        context
            .getSystemService(NotificationManager::class.java)
            .activeNotifications
            .any { it.id == Notifications.ID_PROGRESS && it.isOngoing }

    private fun celebration(waterMl: Int) =
        NotificationCompat
            .Builder(context, Notifications.CHANNEL_PROGRESS)
            .setSmallIcon(R.drawable.ic_stat_drop)
            .setColor(BrandColors.WATER)
            .setContentTitle(context.getString(R.string.progress_goal_reached_title))
            .setContentText(context.getString(R.string.progress_goal_reached_text, Format.litres(waterMl)))
            .setAutoCancel(true)
            .setSilent(true)
            .setTimeoutAfter(CELEBRATION_MS)
            .setContentIntent(Notifications.openAppIntent(context))
            .build()

    private companion object {
        const val CELEBRATION_MS = 10 * 60 * 1000L
    }
}
