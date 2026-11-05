package de.felixneumann.hydr8m8.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import de.felixneumann.hydr8m8.MainActivity
import de.felixneumann.hydr8m8.R
import de.felixneumann.hydr8m8.domain.History
import de.felixneumann.hydr8m8.domain.Milestone
import de.felixneumann.hydr8m8.domain.MilestoneType
import de.felixneumann.hydr8m8.domain.Preset
import de.felixneumann.hydr8m8.ui.BrandColors
import de.felixneumann.hydr8m8.ui.Format
import java.time.Duration
import java.time.Instant
import kotlin.math.max

object Notifications {
    const val CHANNEL_REMINDERS = "reminders"
    const val CHANNEL_MILESTONES = "milestones"
    const val CHANNEL_PROGRESS = "progress"

    const val ID_REMINDER = 1
    const val ID_PROGRESS = 2
    const val ID_MILESTONE = 3

    private const val LITRES_PER_BUCKET = 10
    private const val LITRES_PER_BATHTUB = 150
    private const val ML_PER_LITRE = 1000

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannels(
            listOf(
                NotificationChannel(
                    CHANNEL_REMINDERS,
                    context.getString(R.string.channel_reminders),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply { description = context.getString(R.string.channel_reminders_desc) },
                NotificationChannel(
                    CHANNEL_MILESTONES,
                    context.getString(R.string.channel_milestones),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply { description = context.getString(R.string.channel_milestones_desc) },
                NotificationChannel(
                    CHANNEL_PROGRESS,
                    context.getString(R.string.channel_progress),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = context.getString(R.string.channel_progress_desc)
                    setShowBadge(false)
                },
            ),
        )
    }

    fun canPost(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun openAppIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    @SuppressLint("MissingPermission") // checked via canPost()
    fun postReminder(
        context: Context,
        history: History,
        lastEntryAt: Instant?,
        now: Instant,
        actions: List<Preset>,
    ) {
        if (!canPost(context)) return
        val today = history.todaySummary
        val remaining = max(0, today.goalMl - today.waterMl)
        val titles = context.resources.getStringArray(R.array.reminder_titles)
        val title = titles[(now.epochSecond / SECONDS_PER_HOUR % titles.size).toInt()]
        val text =
            if (lastEntryAt == null || history.todaySummary.entries.isEmpty()) {
                context.getString(R.string.reminder_text_nothing_yet, Format.litres(remaining))
            } else {
                context.getString(
                    R.string.reminder_text,
                    durationText(context, Duration.between(lastEntryAt, now)),
                    Format.litres(remaining),
                )
            }
        val builder =
            NotificationCompat
                .Builder(context, CHANNEL_REMINDERS)
                .setSmallIcon(R.drawable.ic_stat_drop)
                .setColor(BrandColors.WATER)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(openAppIntent(context))
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
        actions.forEachIndexed { index, preset ->
            builder.addAction(
                0,
                "+" + Format.presetLabel(preset),
                NotificationActionReceiver.logIntent(context, preset.id, requestCode = index + 1),
            )
        }
        builder.addAction(
            0,
            context.getString(R.string.action_snooze),
            NotificationActionReceiver.snoozeIntent(context),
        )
        NotificationManagerCompat.from(context).notify(ID_REMINDER, builder.build())
    }

    fun cancelReminder(context: Context) {
        NotificationManagerCompat.from(context).cancel(ID_REMINDER)
    }

    @SuppressLint("MissingPermission") // checked via canPost()
    fun postMilestone(
        context: Context,
        milestone: Milestone,
        history: History,
    ) {
        if (!canPost(context)) return
        val streakMl =
            history
                .days(milestone.streakStart, history.today)
                .sumOf { it.waterMl.toLong() }
        val litres = Format.litres(streakMl, decimals = 0)
        val res = context.resources
        val (title, text) =
            when (milestone.type) {
                MilestoneType.WEEK -> {
                    context.getString(R.string.milestone_week_title) to context.getString(R.string.milestone_week_text)
                }

                MilestoneType.MONTH -> {
                    res.getQuantityString(R.plurals.milestone_month_title, milestone.months, milestone.months) to
                        context.getString(
                            R.string.milestone_month_text,
                            litres,
                            (streakMl / ML_PER_LITRE / LITRES_PER_BUCKET).toInt(),
                        )
                }

                MilestoneType.HALF_YEAR -> {
                    context.getString(R.string.milestone_half_year_title) to
                        context.getString(
                            R.string.milestone_half_year_text,
                            litres,
                            Format.number(streakMl.toDouble() / ML_PER_LITRE / LITRES_PER_BATHTUB, maxDecimals = 1),
                        )
                }

                MilestoneType.YEAR -> {
                    val years = milestone.months / MONTHS_PER_YEAR
                    res.getQuantityString(R.plurals.milestone_year_title, years, years) to
                        context.getString(R.string.milestone_year_text, litres)
                }
            }
        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_MILESTONES)
                .setSmallIcon(R.drawable.ic_stat_drop)
                .setColor(BrandColors.WATER)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(openAppIntent(context))
                .setAutoCancel(true)
                .build()
        NotificationManagerCompat.from(context).notify(ID_MILESTONE, notification)
    }

    private fun durationText(
        context: Context,
        d: Duration,
    ): String {
        val hours = d.toHours()
        val minutes = d.toMinutesPart()
        return if (hours > 0) {
            context.getString(R.string.duration_hours_minutes, hours, minutes)
        } else {
            context.getString(R.string.duration_minutes, d.toMinutes())
        }
    }

    private const val SECONDS_PER_HOUR = 3600L
    private const val MONTHS_PER_YEAR = 12
}
