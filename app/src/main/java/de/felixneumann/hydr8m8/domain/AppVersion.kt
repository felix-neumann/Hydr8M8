package de.felixneumann.hydr8m8.domain

/** Semantic version as used in release tags (`v1.2.3`) and `versionName` (`1.2.3-debug`). */
data class AppVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
) : Comparable<AppVersion> {
    override fun compareTo(
        other: AppVersion,
    ): Int = compareValuesBy(this, other, AppVersion::major, AppVersion::minor, AppVersion::patch)

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        private val PATTERN = Regex("""^v?(\d+)(?:\.(\d+))?(?:\.(\d+))?""")

        /** Parses `v1.2.3`, `1.2`, `1.2.3-debug`; returns null for anything else. */
        fun parse(text: String): AppVersion? {
            val m = PATTERN.find(text.trim()) ?: return null
            return AppVersion(
                m.groupValues[1].toInt(),
                m.groupValues[2].ifEmpty { "0" }.toInt(),
                m.groupValues[3].ifEmpty { "0" }.toInt(),
            )
        }
    }
}
