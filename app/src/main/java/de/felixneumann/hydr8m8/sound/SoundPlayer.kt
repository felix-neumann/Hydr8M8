package de.felixneumann.hydr8m8.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import de.felixneumann.hydr8m8.R

enum class Sound { DROP, GOAL, STREAK }

/**
 * Subtle in-app sound effects. Silent when the ringer is in silent/vibrate mode or sounds are
 * disabled in the settings.
 */
class SoundPlayer(
    private val context: Context,
) {
    private val pool: SoundPool =
        SoundPool
            .Builder()
            .setMaxStreams(2)
            .setAudioAttributes(
                AudioAttributes
                    .Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            ).build()

    private val ids: Map<Sound, Int> =
        mapOf(
            Sound.DROP to pool.load(context, R.raw.sfx_drop, 1),
            Sound.GOAL to pool.load(context, R.raw.sfx_goal, 1),
            Sound.STREAK to pool.load(context, R.raw.sfx_streak, 1),
        )

    fun play(
        sound: Sound,
        enabled: Boolean,
    ) {
        if (!enabled) return
        val audio = context.getSystemService(AudioManager::class.java)
        if (audio.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        val id = ids[sound] ?: return
        pool.play(id, VOLUME, VOLUME, 1, 0, 1f)
    }

    private companion object {
        const val VOLUME = 0.6f
    }
}
