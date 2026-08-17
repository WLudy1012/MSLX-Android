package com.mslx.console.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 使用 Material3 默认排版，仅补充控制台等宽字体。
val Typography = Typography()

val ConsoleFont = FontFamily.Monospace

val ConsoleTextStyle = TextStyle(
    fontFamily = ConsoleFont,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    fontWeight = FontWeight.Normal,
)
