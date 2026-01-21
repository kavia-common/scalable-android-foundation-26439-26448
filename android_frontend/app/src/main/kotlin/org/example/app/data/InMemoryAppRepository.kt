package org.example.app.data

/**
 * Simple in-memory implementation of [AppRepository].
 *
 * No persistence, no network; just hardcoded sample values for the MVVM wiring.
 */
class InMemoryAppRepository : AppRepository {

    private val titles: Map<String, String> = mapOf(
        "home" to "Home",
        "dashboard" to "Dashboard",
        "settings" to "Settings"
    )

    private val initialCounters: Map<String, Int> = mapOf(
        "home" to 0,
        "dashboard" to 10,
        "settings" to 100
    )

    override fun getTitle(screenKey: String): String = titles[screenKey] ?: "Screen"

    override fun getInitialCounter(screenKey: String): Int = initialCounters[screenKey] ?: 0
}
