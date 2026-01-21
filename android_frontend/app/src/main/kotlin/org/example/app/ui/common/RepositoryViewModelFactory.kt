package org.example.app.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.example.app.data.AppRepository

/**
 * A very small ViewModel factory that injects an [AppRepository] and a screenKey string.
 *
 * This keeps wiring explicit and avoids introducing any DI frameworks.
 */
class RepositoryViewModelFactory(
    private val repository: AppRepository,
    private val screenKey: String,
    private val creator: (AppRepository, String) -> ViewModel
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return creator(repository, screenKey) as T
    }
}
