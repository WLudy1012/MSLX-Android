package com.mslx.console

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mslx.console.data.AppSettings
import com.mslx.console.ui.navigation.AppNavHost
import com.mslx.console.ui.theme.MSLXConsoleTheme
import com.mslx.console.ui.theme.ThemeConfig
import com.mslx.console.ui.update.DisclaimerDialog
import com.mslx.console.ui.update.UpdateHost
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()

        val app = application as MSLXApplication
        setContent {
            val settings by app.container.settingsStore.settingsFlow
                .collectAsStateWithLifecycle(initialValue = AppSettings())
            val scope = rememberCoroutineScope()

            MSLXConsoleTheme(
                themeConfig = ThemeConfig(
                    mode = settings.themeMode,
                    seedColor = settings.seedColor,
                ),
            ) {
                AppNavHost(settings = settings)
                // 全局更新弹窗：启动自动检查 + 手动检查结果都走这里
                UpdateHost()
                // 首次开屏免责协议：5 秒后可确认，同意后持久化
                DisclaimerDialog(
                    settings = settings,
                    onAccept = {
                        scope.launch { app.container.settingsStore.acceptDisclaimer() }
                    },
                )
            }
        }
    }
}
