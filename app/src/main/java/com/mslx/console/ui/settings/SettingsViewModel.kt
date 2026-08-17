package com.mslx.console.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mslx.console.MSLXApplication
import com.mslx.console.data.AppSettings
import com.mslx.console.data.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val store = getApplication<MSLXApplication>().container.settingsStore

    val settings = store.settingsFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        AppSettings(),
    )

    fun setTheme(mode: ThemeMode, seedColor: Long) {
        viewModelScope.launch { store.setTheme(mode, seedColor) }
    }

    fun setActiveDaemon(id: String) {
        viewModelScope.launch { store.setActiveDaemon(id) }
    }

    fun removeDaemon(id: String) {
        viewModelScope.launch { store.removeDaemon(id) }
    }
}
