package com.mslx.console.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 实例状态码 → 展示颜色。0未启动 1启动中 2运行中 3停止中 4重启中 */
fun statusColor(status: Int): Color = when (status) {
    2 -> Color(0xFF2E7D32)
    1, 4 -> Color(0xFFF9A825)
    3 -> Color(0xFFFB8C00)
    else -> Color(0xFF9E9E9E)
}

/** 实例状态码 → 中文文案(优先使用后端返回的 statusText)。 */
fun statusLabel(status: Int, statusText: String? = null): String =
    statusText?.takeIf { it.isNotBlank() } ?: when (status) {
        0 -> "未启动"
        1 -> "启动中"
        2 -> "运行中"
        3 -> "停止中"
        4 -> "重启中"
        else -> "未知"
    }

/** 状态圆点。 */
@Composable
fun StatusDot(status: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(statusColor(status)),
    )
}

/** 状态徽章(圆点 + 文字)。 */
@Composable
fun StatusBadge(status: Int, statusText: String? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(statusColor(status).copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        StatusDot(status)
        Text(
            text = statusLabel(status, statusText),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 12.sp,
            color = statusColor(status),
            modifier = Modifier.padding(start = 5.dp),
        )
    }
}
