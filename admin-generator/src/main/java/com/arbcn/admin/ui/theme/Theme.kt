package com.arbcn.admin.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AdminPrimary = Color(0xFFFF6B00)
val AdminPrimaryDark = Color(0xFFE05300)
val AdminContainer = Color(0xFFFFF2E8)
val AdminSubtle = Color(0xFFFFF8F3)

val LightBackground = Color(0xFFF8F9FB)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceSecondary = Color(0xFFF1F5F9)

val BorderLight = Color(0xFFE2E8F0)
val BorderSubtle = Color(0xFFF1F5F9)

val TextPrimary = Color(0xFF0F172A)
val TextSecondary = Color(0xFF475569)
val TextMuted = Color(0xFF94A3B8)

val SuccessGreen = Color(0xFF10B981)
val SuccessContainer = Color(0xFFECFDF5)

val Typography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        color = TextPrimary
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        color = TextPrimary
    )
)
