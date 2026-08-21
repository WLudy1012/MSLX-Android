package com.mslx.console.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/** 顶层页面标识，用于底部导航高亮。 */
enum class TopPage(val label: String, val icon: ImageVector) {
    HOME("主页", Icons.Filled.Home),
    INSTANCES("实例", Icons.Filled.List),
    NEW_INSTANCE("新建", Icons.Filled.Add),
    SETTINGS("设置", Icons.Filled.Settings),
}

/** 四个顶层页共享的底部导航栏。 */
@Composable
fun MainBottomNav(
    current: TopPage,
    onNavigate: (TopPage) -> Unit,
) {
    NavigationBar {
        TopPage.entries.forEach { page ->
            RowScopeNavigationItem(
                page = page,
                selected = page == current,
                onNavigate = onNavigate,
            )
        }
    }
}

@Composable
private fun RowScope.RowScopeNavigationItem(
    page: TopPage,
    selected: Boolean,
    onNavigate: (TopPage) -> Unit,
) {
    NavigationBarItem(
        selected = selected,
        onClick = { if (!selected) onNavigate(page) },
        icon = { Icon(page.icon, null) },
        label = { Text(page.label) },
    )
}
