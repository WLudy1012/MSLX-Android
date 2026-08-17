package com.mslx.console.ui.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mslx.console.MSLXApplication
import com.mslx.console.R
import kotlinx.coroutines.launch

private val features = listOf(
    "连接 MSLX 守护程序，随时管理你的服务器",
    "一键启动 / 停止 / 重启 / 备份实例",
    "实时控制台日志，手机也能发命令",
    "多台 Daemon 自由切换，主题随心换",
)

/** 首次启动欢迎页。 */
@Composable
fun WelcomeScreen(onStart: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.mslx_logo),
                contentDescription = "MSLX",
                modifier = Modifier.size(150.dp),
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = "欢迎使用 MSLX 控制台",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "在手机上远程管理你的 Minecraft 服务器",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))

            features.forEach { feature ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.size(10.dp))
                    Text(
                        text = feature,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {
                    val app = context.applicationContext as MSLXApplication
                    scope.launch { app.container.settingsStore.markOnboarded() }
                    onStart()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text("开始使用", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
