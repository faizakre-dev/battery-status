package com.faizakram.batterystatus.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Light Material 3 scheme seeded with the app's brand green. */
val LightColors = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB7F2B2),
    onPrimaryContainer = Color(0xFF002204),
    secondary = Color(0xFF526350),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD5E8CF),
    onSecondaryContainer = Color(0xFF101F10),
    tertiary = Color(0xFF386568),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFBFDF7),
    onBackground = Color(0xFF191D17),
    surface = Color(0xFFFBFDF7),
    onSurface = Color(0xFF191D17),
    surfaceVariant = Color(0xFFDEE5D9),
    onSurfaceVariant = Color(0xFF424940),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
)

/** Dark Material 3 scheme seeded with the app's brand green. */
val DarkColors = darkColorScheme(
    primary = Color(0xFF9BD69A),
    onPrimary = Color(0xFF00390A),
    primaryContainer = Color(0xFF11541A),
    onPrimaryContainer = Color(0xFFB7F2B2),
    secondary = Color(0xFFB9CCB5),
    onSecondary = Color(0xFF243424),
    secondaryContainer = Color(0xFF3A4B39),
    onSecondaryContainer = Color(0xFFD5E8CF),
    tertiary = Color(0xFFA0CED1),
    onTertiary = Color(0xFF00373A),
    background = Color(0xFF11140F),
    onBackground = Color(0xFFE1E4DB),
    surface = Color(0xFF11140F),
    onSurface = Color(0xFFE1E4DB),
    surfaceVariant = Color(0xFF424940),
    onSurfaceVariant = Color(0xFFC2C9BD),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

/** Semantic colors for battery health/level, tuned for contrast per theme. */
@Immutable
data class StatusPalette(
    val good: Color,
    val warning: Color,
    val danger: Color,
    val neutral: Color,
)

private val LightStatusPalette = StatusPalette(
    good = Color(0xFF2E7D32),
    warning = Color(0xFFB26A00),
    danger = Color(0xFFC62828),
    neutral = Color(0xFF757575),
)

private val DarkStatusPalette = StatusPalette(
    good = Color(0xFF81C784),
    warning = Color(0xFFFFD54F),
    danger = Color(0xFFEF9A9A),
    neutral = Color(0xFFBDBDBD),
)

@Composable
fun statusPalette(): StatusPalette =
    if (isSystemInDarkTheme()) DarkStatusPalette else LightStatusPalette
