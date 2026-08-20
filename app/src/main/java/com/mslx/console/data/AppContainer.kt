package com.mslx.console.data

import android.content.Context

/** 简易手动依赖注入容器。 */
class AppContainer(context: Context) {
    val settingsStore = SettingsStore(context.applicationContext)
    val instanceRepository = InstanceRepository()
    val updateRepository = UpdateRepository()
}
