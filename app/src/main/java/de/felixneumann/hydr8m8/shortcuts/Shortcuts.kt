package de.felixneumann.hydr8m8.shortcuts

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import de.felixneumann.hydr8m8.R
import de.felixneumann.hydr8m8.container
import de.felixneumann.hydr8m8.data.HydrationRepository
import de.felixneumann.hydr8m8.domain.Surfaces
import de.felixneumann.hydr8m8.ui.BrandColors
import de.felixneumann.hydr8m8.ui.Format
import de.felixneumann.hydr8m8.ui.drawable
import kotlinx.coroutines.launch

/** Publishes the user's launcher shortcuts: up to 3 presets plus "Undo last entry". */
class ShortcutPublisher(
    private val context: Context,
) {
    private var lastSignature: String? = null

    suspend fun publish(repository: HydrationRepository) {
        val presets = repository.surfaceNow(Surfaces.SHORTCUTS)
        val signature = presets.joinToString { "${it.id}:${it.volumeMl}:${it.abvPercent}:${it.icon}" }
        if (signature == lastSignature) return
        lastSignature = signature

        val shortcuts =
            presets.mapIndexed { index, preset ->
                val label = "+" + Format.presetTitle(preset)
                ShortcutInfoCompat
                    .Builder(context, "preset_${preset.id}")
                    .setShortLabel(label)
                    .setLongLabel("+" + Format.presetDisplay(preset))
                    .setRank(index)
                    .setIcon(iconBitmap(preset.icon.drawable(), BrandColors.forKind(preset.kind)))
                    .setIntent(ShortcutActivity.logIntent(context, preset.id))
                    .build()
            } +
                ShortcutInfoCompat
                    .Builder(context, "undo")
                    .setShortLabel(context.getString(R.string.shortcut_undo))
                    .setRank(presets.size)
                    .setIcon(iconBitmap(R.drawable.ic_undo, BrandColors.TEXT))
                    .setIntent(ShortcutActivity.undoIntent(context))
                    .build()
        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
    }

    /**
     * Adaptive icon: dark full-bleed background with the colored line icon inside the safe zone,
     * so the white line icons stay visible and large in the launcher menu.
     */
    private fun iconBitmap(
        drawableRes: Int,
        color: Int,
    ): IconCompat {
        val size = ICON_PX
        val bitmap = createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(BrandColors.CARD)
        val icon = context.getDrawable(drawableRes)!!.mutate()
        icon.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        val inset = (size * ICON_INSET).toInt()
        icon.setBounds(inset, inset, size - inset, size - inset)
        icon.draw(canvas)
        return IconCompat.createWithAdaptiveBitmap(bitmap)
    }

    private companion object {
        const val ICON_PX = 216
        const val ICON_INSET = 0.3f
    }
}

/** Invisible activity that performs a shortcut action, shows a toast and finishes. */
class ShortcutActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val c = container
        val presetId = intent.getLongExtra(EXTRA_PRESET_ID, -1)
        val action = intent.action
        c.appScope.launch {
            c.repository.ensureSeeded()
            val message =
                when (action) {
                    ACTION_UNDO -> {
                        c.actions.undoLast()?.let {
                            getString(R.string.undo_done, Format.volume(it.volumeMl), Format.time(it.at))
                        } ?: getString(R.string.undo_nothing)
                    }

                    else -> {
                        c.actions.logPreset(presetId)?.let { result ->
                            val today = result.history.todaySummary
                            getString(
                                R.string.logged_toast,
                                Format.volume(result.entry.volumeMl),
                                Format.litres(today.waterMl),
                                Format.litres(today.goalMl),
                            )
                        } ?: getString(R.string.preset_missing)
                    }
                }
            runOnUiThread { Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show() }
        }
        finish()
    }

    companion object {
        private const val ACTION_LOG = "de.felixneumann.hydr8m8.shortcut.LOG"
        private const val ACTION_UNDO = "de.felixneumann.hydr8m8.shortcut.UNDO"
        private const val EXTRA_PRESET_ID = "preset_id"

        fun logIntent(
            context: Context,
            presetId: Long,
        ): Intent =
            Intent(context, ShortcutActivity::class.java)
                .setAction(ACTION_LOG)
                .putExtra(EXTRA_PRESET_ID, presetId)

        fun undoIntent(context: Context): Intent = Intent(context, ShortcutActivity::class.java).setAction(ACTION_UNDO)
    }
}
