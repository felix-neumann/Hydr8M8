package de.felixneumann.hydr8m8.domain

import java.time.Instant

/** The Drop Mate's mood, driven by progress relative to the pace plan. */
enum class MateMood {
    CHAMPION,
    HAPPY,
    THIRSTY,
    VERY_THIRSTY,
    TIPSY,
    SLEEPY,
    ;

    companion object {
        private const val THIRSTY_FRACTION = 0.15
        private const val VERY_THIRSTY_FRACTION = 0.35

        fun of(
            now: Instant,
            window: TimeWindow,
            intakeMl: Int,
            goalMl: Int,
            alcoholToday: Boolean,
        ): MateMood {
            if (now !in window) return SLEEPY
            if (Progress.isReached(intakeMl, goalMl)) return CHAMPION
            val behind = Pace.behindFraction(now, window, intakeMl, goalMl)
            return when {
                behind > VERY_THIRSTY_FRACTION -> VERY_THIRSTY
                behind > THIRSTY_FRACTION -> THIRSTY
                alcoholToday -> TIPSY
                else -> HAPPY
            }
        }
    }
}
