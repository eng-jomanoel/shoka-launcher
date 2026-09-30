package com.lifetracker.engine.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Stable
class EngineColors(
    background: Color,
    card: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    textMuted: Color,
    accentGreen: Color,
    accentCyan: Color,
    accentAmber: Color,
    accentRed: Color
) {
    var background by mutableStateOf(background)
    var card by mutableStateOf(card)
    var cardBorder by mutableStateOf(cardBorder)
    var textPrimary by mutableStateOf(textPrimary)
    var textSecondary by mutableStateOf(textSecondary)
    var textMuted by mutableStateOf(textMuted)
    var accentGreen by mutableStateOf(accentGreen)
    var accentCyan by mutableStateOf(accentCyan)
    var accentAmber by mutableStateOf(accentAmber)
    var accentRed by mutableStateOf(accentRed)
}

val AmoledColorScheme = EngineColors(
    background = AmoledBlack,
    card = AmoledDarkCard,
    cardBorder = AmoledDarkCardBorder,
    textPrimary = AmoledTextPrimary,
    textSecondary = AmoledTextSecondary,
    textMuted = AmoledTextMuted,
    accentGreen = AmoledAccentGreen,
    accentCyan = AmoledAccentCyan,
    accentAmber = AmoledAccentAmber,
    accentRed = AmoledAccentRed
)

val SolarizedDarkColorScheme = EngineColors(
    background = SolarizedDarkBase03,
    card = SolarizedDarkBase02,
    cardBorder = SolarizedDarkBase01,
    // Aumentando drasticamente o contraste do texto (usando Base2 e Base1 para brilhar mais no escuro)
    textPrimary = SolarizedLightBase2,   // Um branco/amarelado forte (#eee8d5)
    textSecondary = SolarizedDarkBase1,  // Cinza claro (#93a1a1)
    textMuted = SolarizedDarkBase0,      // Cinza médio (#839496)
    accentGreen = SolarizedDarkGreen,
    accentCyan = SolarizedDarkBlue,
    accentAmber = SolarizedDarkYellow,
    accentRed = SolarizedDarkRed
)

val SolarizedLightColorScheme = EngineColors(
    background = SolarizedLightBase3,
    card = SolarizedLightBase2,
    cardBorder = SolarizedLightBase1,
    textPrimary = SolarizedLightBase00,
    textSecondary = SolarizedLightBase01,
    textMuted = SolarizedLightBase1,
    accentGreen = SolarizedDarkGreen, // O verde funciona nos dois
    accentCyan = SolarizedDarkBlue,
    accentAmber = SolarizedDarkYellow,
    accentRed = SolarizedDarkRed
)

val LocalEngineColors = staticCompositionLocalOf { AmoledColorScheme }

object EngineTheme {
    val colors: EngineColors
        @Composable
        get() = LocalEngineColors.current

    val typography = EngineTypography
}

@Composable
fun ModularLifeTrackerTheme(
    engineColors: EngineColors = AmoledColorScheme,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalEngineColors provides engineColors,
        androidx.compose.material3.LocalContentColor provides engineColors.textPrimary
    ) {
        content()
    }
}
