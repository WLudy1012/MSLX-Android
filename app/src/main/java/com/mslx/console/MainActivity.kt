package com.mslx.console

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mslx.console.data.AppSettings
import com.mslx.console.ui.navigation.AppNavHost
import com.mslx.console.ui.theme.MSLXConsoleTheme
import com.mslx.console.ui.theme.ThemeConfig
import com.mslx.console.ui.update.UpdateHost

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()

        val app = application as MSLXApplication
        setContent {
            val settings by app.container.settingsStore.settingsFlow
                .collectAsStateWithLifecycle(initialValue = AppSettings())

            MSLXConsoleTheme(
                themeConfig = ThemeConfig(
                    mode = settings.themeMode,
                    seedColor = settings.seedColor,
                ),
            ) {
                AppNavHost(settings = settings)
                // 全局更新弹窗：启动自动检查 + 手动检查结果都走这里
                UpdateHost()
            }
        }
    }
}
