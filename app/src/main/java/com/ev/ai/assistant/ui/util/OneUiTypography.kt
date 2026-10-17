package com.ev.ai.assistant.ui.util

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Official Samsung One UI Typography and Hierarchy Design Tokens.
 * Optimized for natural text scannability and clean layout structures.
 */
object OneUiTypography {

    // --- VIEWING AREA DISPLAYS (TOP 1/3 SCREEN SPLIT) ---
    // Large prominent indicators used for empty states or deep greeting captions
    val Display = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 24.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.Light, // One UI uses clean Light/Regular fonts for extreme macro displays
        letterSpacing = 0.sp
    )

    // Standard header for main configuration panes and Bixby greeting screens
    val ScreenTitle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp
    )

    // Primary action toolbar text signature
    val AppTitle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 26.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp
    )

    // --- INTERACTIVE LABELS & CATEGORY SEPARATORS ---
    // Subdued, uppercase capsule category blocks used above settings sections
    val SectionTitle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp // Slight spacing increase for clean structural headers
    )

    // --- CORE DIALOG & TEXT CHAT MESSAGES ---
    // Standard streaming chat transcription body string
    val Body = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp
    )

    // Main interactive lists, title subheaders, and focused text responses
    val BodyMedium = Body.copy(
        fontWeight = FontWeight.Medium
    )

    // Focused button indicators, pill tags, and key text interactions
    val Label = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 15.sp, // Upgraded to match standard One UI list row headers
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp
    )

    // --- PASSIVE STRUCTURAL DETAILS ---
    // Subtext indicators inside rounded card layers
    val Caption = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp
    )

    // Footnotes, timestamps, and deep background system tracking values
    val Supporting = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp
    )

    // --- CONTEXTUAL SEMANTIC COLOR MAPS ---
    @Composable
    fun bodyColor() = MaterialTheme.colorScheme.onSurface

    @Composable
    fun supportingColor() = MaterialTheme.colorScheme.onSurfaceVariant
}