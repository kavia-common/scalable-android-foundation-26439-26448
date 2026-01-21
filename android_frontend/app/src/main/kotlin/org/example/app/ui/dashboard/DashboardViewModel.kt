package org.example.app.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.example.app.data.AppRepository

class DashboardViewModel(
    repository: AppRepository,
    screenKey: String = "dashboard"
) : ViewModel() {

    private val _title = MutableLiveData(repository.getTitle(screenKey))
    val title: LiveData<String> = _title

    private val _counter = MutableLiveData(repository.getInitialCounter(screenKey))
    val counter: LiveData<Int> = _counter

    fun increment() {
        val current = _counter.value ?: 0
        _counter.value = current + 1
    }
}
