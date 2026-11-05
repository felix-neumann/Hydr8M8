package de.felixneumann.hydr8m8.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.felixneumann.hydr8m8.container
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Handles "+amount" and "Snooze" buttons on reminder notifications. */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val pending = goAsync()
        val actions = context.container.actions
        CoroutineScope(Dispatchers.Default).launch {
            try {
                when (intent.action) {
                    ACTION_LOG -> {
                        Notifications.cancelReminder(context)
                        actions.logPreset(intent.getLongExtra(EXTRA_PRESET_ID, -1))
                    }

                    ACTION_SNOOZE -> {
                        actions.snoozeReminder()
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val ACTION_LOG = "de.felixneumann.hydr8m8.LOG"
        private const val ACTION_SNOOZE = "de.felixneumann.hydr8m8.SNOOZE"
        private const val EXTRA_PRESET_ID = "preset_id"
        private const val SNOOZE_REQUEST = 100

        fun logIntent(
            context: Context,
            presetId: Long,
            requestCode: Int,
        ): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                requestCode,
                Intent(context, NotificationActionReceiver::class.java)
                    .setAction(ACTION_LOG)
                    .putExtra(EXTRA_PRESET_ID, presetId),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        fun snoozeIntent(context: Context): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                SNOOZE_REQUEST,
                Intent(context, NotificationActionReceiver::class.java).setAction(ACTION_SNOOZE),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
    }
}
