package com.mslx.console.ui.user

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mslx.console.MSLXApplication
import com.mslx.console.data.model.UserInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UserCenterUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val user: UserInfo? = null,
)

class UserCenterViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = getApplication<MSLXApplication>().container.instanceRepository

    private val _state = MutableStateFlow(UserCenterUiState())
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            repository.userMe().fold(
                onSuccess = { user -> _state.update { it.copy(loading = false, user = user) } },
                onFailure = { e ->
                    _state.update {
                        it.copy(loading = false, error = e.message ?: "获取失败")
                    }
                },
            )
        }
    }
}
