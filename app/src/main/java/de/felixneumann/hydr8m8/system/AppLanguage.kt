package de.felixneumann.hydr8m8.system

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList

/**
 * Per-app language via the platform LocaleManager (Android 13+). The choice is stored by the
 * system, shows up under "App languages" in Android settings and recreates the activity.
 */
object AppLanguage {
    /** Empty tag = follow the system language. */
    const val SYSTEM = ""
    val supported = listOf(SYSTEM, "de", "en")

    fun current(context: Context): String {
        val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
        return if (locales.isEmpty) SYSTEM else locales[0].language
    }

    fun set(
        context: Context,
        tag: String,
    ) {
        context.getSystemService(LocaleManager::class.java).applicationLocales =
            if (tag == SYSTEM) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
    }
}
