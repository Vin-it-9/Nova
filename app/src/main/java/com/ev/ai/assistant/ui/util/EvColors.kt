package com.ev.ai.assistant.ui.util

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * EV's One UI-inspired color tokens.
 *
 * Samsung-published One UI blue tokens are called out separately below. Neutrals,
 * container colors, EV cyan, and status-container colors are EV design choices that
 * follow Samsung's documented principles; they are not claimed to be Samsung's
 * private/internal palette or universal Galaxy system colors.
 */
object OneUiColors {

    // 1. OFFICIAL ONE UI BLUE ROLES
    // Reference: Samsung Developer > One UI > Color system and usage.

    /** Samsung's published One UI primary blue; used for brand accents in either theme. */
    val BrandPrimary = Color(0xFF0381FE)

    /** High-emphasis contained action background for the light theme. */
    val PrimaryActionLight = Color(0xFF0072DE)

    /** High-emphasis contained action background for the dark theme. */
    val PrimaryActionDark = Color(0xFF3E91FF)

    /** Activated control color published by Samsung for checkboxes and switches. */
    val ControlActive = Color(0xFF3E91FF)

    /** EV-tuned pressed overlay for a dark-theme action; not a published Samsung token. */
    val PrimaryPressedDarkCustom = Color(0xFF69A9FF)

    /** Low-opacity brand-blue fill for selected/soft-highlighted states. */
    val PrimaryAlpha20 = Color(0x330381FE)

    /**
     * Backwards-compatible names used by the existing EV UI.
     * Prefer BrandPrimary, PrimaryActionLight/PrimaryActionDark, and ControlActive
     * when selecting a color for a specific semantic role.
     */
    val PrimaryLight = BrandPrimary
    val PrimaryDark = PrimaryActionDark
    val PrimaryPressedLight = PrimaryActionLight
    val PrimaryPressedDark = PrimaryPressedDarkCustom

    fun primaryAction(darkTheme: Boolean): Color =
        if (darkTheme) PrimaryActionDark else PrimaryActionLight

    fun primaryPressed(darkTheme: Boolean): Color =
        if (darkTheme) PrimaryPressedDarkCustom else PrimaryActionLight

    // 2. ROOT BACKGROUNDS AND SURFACES
    // These neutral values are EV's chosen One UI-inspired tones, not fixed
    // Samsung system-wide hex values. Samsung recommends calm, monochrome surfaces.

    val BackgroundLight = Color(0xFFF7F7F7)
    val BackgroundDark = Color(0xFF000000)

    val BackgroundSecondaryLight = Color(0xFFEDEFF2)
    val BackgroundSecondaryDark = Color(0xFF0C0C0C)

    val SurfaceLight = Color(0xFFFFFFFF)
    val SurfaceDark = Color(0xFF1A1A1A)

    val CardSurfaceLight = SurfaceLight
    val CardSurfaceDark = SurfaceDark

    val SurfaceVariantLight = Color(0xFFF1F3F5)
    val SurfaceVariantDark = Color(0xFF242424)

    /** Subtle blue-tinted containers for selected or emphasized content. */
    val PrimaryContainerLight = Color(0xFFDCEBFF)
    val PrimaryContainerDark = Color(0xFF15314C)
    val SecondaryContainerLight = Color(0xFFEAF2FF)
    val SecondaryContainerDark = Color(0xFF202D3C)

    /** EV's optional cyan accent containers. Use sparingly. */
    val TertiaryAccent = Color(0xFF22C7D8)
    val TertiaryContainerLight = Color(0xFFD7F7FA)
    val TertiaryContainerDark = Color(0xFF123438)

    // 3. TEXT AND ICONS
    // Secondary/tertiary text is chosen to remain readable on the app's neutral
    // surfaces. Disabled text intentionally has lower emphasis.

    val TextPrimaryLight = Color(0xFF000000)
    val TextPrimaryDark = Color(0xFFFAFAFA)

    // Slightly strengthened from the original #6E7378 so small text also clears
    // the usual 4.5:1 target against BackgroundLight (#F7F7F7).
    val TextSecondaryLight = Color(0xFF696E73)
    val TextSecondaryDark = Color(0xFFB5B5B5)

    // Tertiary is quieter than secondary while retaining readable contrast on
    // the default background/surface tones.
    val TextTertiaryLight = Color(0xFF707070)
    val TextTertiaryDark = Color(0xFFA0A0A0)

    val TextDisabledLight = Color(0xFFC4C4C4)
    val TextDisabledDark = Color(0xFF525252)

    // 4. DIVIDERS, OUTLINES, SELECTION AND INTERACTION

    val DividerLight = Color(0xFFE2E5E8)
    val DividerDark = Color(0xFF2C2C2E)

    val OutlineLight = Color(0xFFD1D1D6)
    val OutlineDark = Color(0xFF3A3A3C)

    val OutlineVariantLight = DividerLight
    val OutlineVariantDark = DividerDark

    val ControlTrackLight = Color(0xFFE5E5EA)
    val ControlTrackDark = Color(0xFF393939)

    val RippleLight = Color(0x1F000000) // approximately 12% black
    val RippleDark = Color(0x33FFFFFF) // approximately 20% white

    val SelectionLight = Color(0x263E91FF) // EV selection tint, approximately 15%
    val SelectionDark = Color(0x333E91FF)  // EV selection tint, approximately 20%

    /** Scrim for modal surfaces. */
    val Scrim = Color(0x66000000)

    // 5. SEMANTIC STATUS COLORS
    // Samsung recommends keeping informative hues consistent between themes.
    // These base hues are EV choices. Use the theme-specific text/container tokens
    // below for readable status text and filled status messages.

    val Error = Color(0xFFF23D3D)
    val Success = Color(0xFF2BB34D)
    val Warning = Color(0xFFFF9500)

    // Backwards-compatible theme names now share stable semantic hues.
    val ErrorLight = Error
    val ErrorDark = Error
    val SuccessLight = Success
    val SuccessDark = Success
    val WarningLight = Warning
    val WarningDark = Warning

    // Contrast-conscious status text colors for small text on standard surfaces.
    val ErrorTextLight = Color(0xFFB3261E)
    val ErrorTextDark = Color(0xFFFF7B72)
    val SuccessTextLight = Color(0xFF1E6B35)
    val SuccessTextDark = Color(0xFF67E08B)
    val WarningTextLight = Color(0xFF835000)
    val WarningTextDark = Color(0xFFFFD08A)

    val ErrorContainerLight = Color(0xFFFDE7E5)
    val ErrorContainerDark = Color(0xFF421A1A)
    val OnErrorContainerLight = Color(0xFF8C1D18)
    val OnErrorContainerDark = Color(0xFFFFDAD6)

    val SuccessContainerLight = Color(0xFFE1F5E8)
    val SuccessContainerDark = Color(0xFF103322)
    val OnSuccessContainerLight = Color(0xFF1E5E33)
    val OnSuccessContainerDark = Color(0xFFA7F3BD)

    val WarningContainerLight = Color(0xFFFFF0D6)
    val WarningContainerDark = Color(0xFF352300)
    val OnWarningContainerLight = Color(0xFF7A4300)
    val OnWarningContainerDark = Color(0xFFFFD9A0)

    // 6. MATERIAL 3 COLOR-SCHEME MAPPING
    // MaterialTheme.colorScheme.primary is mapped to the One UI high-emphasis
    // action color. Use BrandPrimary explicitly for the stable #0381FE brand blue;
    // use ControlActive explicitly for activated switches/checkboxes.

    fun colorScheme(darkTheme: Boolean): ColorScheme =
        if (darkTheme) {
            darkColorScheme(
                primary = PrimaryActionDark,
                onPrimary = Color(0xFF071522),
                primaryContainer = PrimaryContainerDark,
                onPrimaryContainer = Color(0xFFD8E9FF),
                inversePrimary = PrimaryActionLight,
                secondary = BrandPrimary,
                onSecondary = Color(0xFF001827),
                secondaryContainer = SecondaryContainerDark,
                onSecondaryContainer = Color(0xFFD4E4F8),
                tertiary = TertiaryAccent,
                onTertiary = Color(0xFF00343A),
                tertiaryContainer = TertiaryContainerDark,
                onTertiaryContainer = Color(0xFFBDF5F7),
                error = Error,
                onError = Color(0xFF1A0000),
                errorContainer = ErrorContainerDark,
                onErrorContainer = OnErrorContainerDark,
                background = BackgroundDark,
                onBackground = TextPrimaryDark,
                surface = SurfaceDark,
                onSurface = TextPrimaryDark,
                surfaceVariant = SurfaceVariantDark,
                onSurfaceVariant = TextSecondaryDark,
                inverseSurface = SurfaceLight,
                inverseOnSurface = TextPrimaryLight,
                outline = OutlineDark,
                outlineVariant = OutlineVariantDark,
                scrim = Scrim
            )
        } else {
            lightColorScheme(
                primary = PrimaryActionLight,
                onPrimary = Color.White,
                primaryContainer = PrimaryContainerLight,
                onPrimaryContainer = Color(0xFF092D55),
                inversePrimary = PrimaryActionDark,
                secondary = BrandPrimary,
                onSecondary = Color(0xFF001827),
                secondaryContainer = SecondaryContainerLight,
                onSecondaryContainer = Color(0xFF112D4C),
                tertiary = TertiaryAccent,
                onTertiary = Color(0xFF00343A),
                tertiaryContainer = TertiaryContainerLight,
                onTertiaryContainer = Color(0xFF00383D),
                error = Error,
                onError = Color(0xFF1A0000),
                errorContainer = ErrorContainerLight,
                onErrorContainer = OnErrorContainerLight,
                background = BackgroundLight,
                onBackground = TextPrimaryLight,
                surface = SurfaceLight,
                onSurface = TextPrimaryLight,
                surfaceVariant = SurfaceVariantLight,
                onSurfaceVariant = TextSecondaryLight,
                inverseSurface = SurfaceDark,
                inverseOnSurface = TextPrimaryDark,
                outline = OutlineLight,
                outlineVariant = OutlineVariantLight,
                scrim = Scrim
            )
        }
}

/**
 * Pure color aliases providing direct compile-time token bindings.
 */
object OneUiPureColors {
    val CardSurfaceLight = OneUiColors.CardSurfaceLight
    val CardSurfaceDark = OneUiColors.CardSurfaceDark
    val TextPrimaryLight = OneUiColors.TextPrimaryLight
    val TextPrimaryDark = OneUiColors.TextPrimaryDark
    val BrandPrimary = OneUiColors.BrandPrimary
    val ControlActive = OneUiColors.ControlActive
    val BackgroundLight = OneUiColors.BackgroundLight
    val BackgroundDark = OneUiColors.BackgroundDark
}