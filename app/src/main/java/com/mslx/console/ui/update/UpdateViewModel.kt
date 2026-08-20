package com.mslx.console.ui.update

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mslx.console.MSLXApplication
import com.mslx.console.data.AppUpdateInfo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UpdateUiState(
    val checking: Boolean = false,
    /** 检测到的新版本（非空时 UI 弹窗展示）。 */
    val update: AppUpdateInfo? = null,
)

class UpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = getApplication<MSLXApplication>().container.updateRepository

    private val _state = MutableStateFlow(UpdateUiState())
    val state = _state.asStateFlow()

    /** 一次性提示消息（如"已是最新版本"/检查失败）。 */
    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val message = _message.asSharedFlow()

    /** 是否已执行过启动自动检查（避免重复弹窗）。 */
    private var autoChecked = false

    /** 当前应用版本号，如 "1.2.6"。 */
    private val currentVersion: String
        get() = runCatching {
            getApplication<Application>().packageManager
                .getPackageInfo(getApplication<Application>().packageName, 0)
                .versionName.orEmpty()
        }.getOrDefault("")

    /** 启动时自动检查。仅首次生效。 */
    fun checkOnLaunch() {
        if (autoChecked) return
        autoChecked = true
        check(manual = false)
    }

    /** 手动检查更新（设置页）。 */
    fun checkManually() = check(manual = true)

    private fun check(manual: Boolean) {
        if (_state.value.checking) return
        _state.update { it.copy(checking = true) }
        viewModelScope.launch {
            repository.checkLatest(currentVersion).fold(
                onSuccess = { update ->
                    _state.update {
                        it.copy(checking = false, update = update)
                    }
                    if (manual && update == null) {
                        _message.tryEmit("当前已是最新版本")
                    }
                },
                onFailure = { e ->
                    _state.update { it.copy(checking = false) }
                    if (manual) {
                        _message.tryEmit("检查更新失败：${e.message ?: "网络错误"}")
                    }
                },
            )
        }
    }

    /** 用户选择"跳过"（关闭弹窗，本次启动不再提示）。 */
    fun skip() {
        _state.update { it.copy(update = null) }
    }

    /** 用户选择"更新"：跳转浏览器下载 APK。 */
    fun openUpdate() {
        val url = _state.value.update?.downloadUrl ?: return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { getApplication<Application>().startActivity(intent) }
    }
}
