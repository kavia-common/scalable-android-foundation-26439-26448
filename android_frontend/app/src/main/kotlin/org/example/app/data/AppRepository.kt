package org.example.app.data

/**
 * Repository boundary for app data.
 *
 * This is intentionally simple and in-memory for the foundation app.
 */
interface AppRepository {
    /** Returns a user-visible title for a given screen key. */
    fun getTitle(screenKey: String): String

    /** Returns the initial counter value for a given screen key. */
    fun getInitialCounter(screenKey: String): Int
}
