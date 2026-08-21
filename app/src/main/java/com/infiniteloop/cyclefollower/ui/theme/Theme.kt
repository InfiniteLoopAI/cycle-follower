package com.infiniteloop.cyclefollower.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.infiniteloop.cyclefollower.domain.CyclePhase

private val Plum = Color(0xFF8E4A63)
private val PlumLight = Color(0xFFFFD9E2)
private val PlumDark = Color(0xFF3B071F)
private val Slate = Color(0xFF74565F)
private val Sand = Color(0xFF7C5635)

private val LightColors = lightColorScheme(
    primary = Plum,
    onPrimary = Color.White,
    primaryContainer = PlumLight,
    onPrimaryContainer = PlumDark,
    secondary = Slate,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD9E2),
    onSecondaryContainer = Color(0xFF2B151C),
    tertiary = Sand,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDCC1),
    onTertiaryContainer = Color(0xFF2E1500),
    background = Color(0xFFFFFBFC),
    onBackground = Color(0xFF201A1C),
    surface = Color(0xFFFFFBFC),
    onSurface = Color(0xFF201A1C),
    surfaceVariant = Color(0xFFF3DDE3),
    onSurfaceVariant = Color(0xFF524348),
    outline = Color(0xFF847378),
    outlineVariant = Color(0xFFD6C2C7),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB1C7),
    onPrimary = Color(0xFF561D34),
    primaryContainer = Color(0xFF72334B),
    onPrimaryContainer = PlumLight,
    secondary = Color(0xFFE3BDC6),
    onSecondary = Color(0xFF422931),
    secondaryContainer = Color(0xFF5A3F47),
    onSecondaryContainer = Color(0xFFFFD9E2),
    tertiary = Color(0xFFF0BC94),
    onTertiary = Color(0xFF48290C),
    tertiaryContainer = Color(0xFF623F20),
    onTertiaryContainer = Color(0xFFFFDCC1),
    background = Color(0xFF181114),
    onBackground = Color(0xFFECE0E3),
    surface = Color(0xFF181114),
    onSurface = Color(0xFFECE0E3),
    surfaceVariant = Color(0xFF524348),
    onSurfaceVariant = Color(0xFFD6C2C7),
    outline = Color(0xFF9E8C91),
    outlineVariant = Color(0xFF524348),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

/** Each phase gets a consistent colour used by the ring, the chips and the calendar. */
data class PhasePalette(val accent: Color, val container: Color, val onContainer: Color)

fun phasePalette(phase: CyclePhase, dark: Boolean): PhasePalette = when (phase) {
    CyclePhase.MENSTRUAL ->
        if (dark) PhasePalette(Color(0xFFFF8FA3), Color(0xFF5C1B2A), Color(0xFFFFD9E0))
        else PhasePalette(Color(0xFFC2185B), Color(0xFFFFE0E8), Color(0xFF5C0B26))
    CyclePhase.FOLLICULAR ->
        if (dark) PhasePalette(Color(0xFF8CD9A3), Color(0xFF1E4A2C), Color(0xFFD4F2DD))
        else PhasePalette(Color(0xFF2E7D4F), Color(0xFFDDF2E4), Color(0xFF10361F))
    CyclePhase.FERTILE_WINDOW ->
        if (dark) PhasePalette(Color(0xFF8ED0E8), Color(0xFF17414F), Color(0xFFD2ECF6))
        else PhasePalette(Color(0xFF00697F), Color(0xFFD5EEF6), Color(0xFF00323E))
    CyclePhase.OVULATION ->
        if (dark) PhasePalette(Color(0xFFF3C969), Color(0xFF57430E), Color(0xFFFAECC8))
        else PhasePalette(Color(0xFF9A6E00), Color(0xFFFBEECB), Color(0xFF3D2B00))
    CyclePhase.EARLY_LUTEAL ->
        if (dark) PhasePalette(Color(0xFFB7B4F0), Color(0xFF34327A), Color(0xFFE2E1FA))
        else PhasePalette(Color(0xFF4F4CB0), Color(0xFFE4E3FA), Color(0xFF1E1C5C))
    CyclePhase.LATE_LUTEAL ->
        if (dark) PhasePalette(Color(0xFFF0A97C), Color(0xFF64330E), Color(0xFFFBE0CE))
        else PhasePalette(Color(0xFFB35309), Color(0xFFFDE3D1), Color(0xFF4A2100))
    CyclePhase.HORMONE_BREAK ->
        if (dark) PhasePalette(Color(0xFFC5C0C9), Color(0xFF423E47), Color(0xFFE9E4EE))
        else PhasePalette(Color(0xFF6A626F), Color(0xFFEAE4EE), Color(0xFF2A2530))
    CyclePhase.STEADY_STATE ->
        if (dark) PhasePalette(Color(0xFF9FCBD9), Color(0xFF25454F), Color(0xFFD9EDF4))
        else PhasePalette(Color(0xFF3E6B79), Color(0xFFDCEDF3), Color(0xFF17323B))
}

private val AppTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.5.sp,
        lineHeight = 21.sp,
    ),
)

@Composable
fun CycleFollowerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
