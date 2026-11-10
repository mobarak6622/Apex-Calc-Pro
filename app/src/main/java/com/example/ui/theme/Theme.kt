package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppThemePreset(
    val label: String,
    val bnLabel: String,
    val isDark: Boolean,
    val accentHex: String
) {
    QUANTUM_CYAN("Quantum Cyan (AMOLED Dark)", "কোয়ান্টাম সায়ান (ডার্ক)", true, "#00E5FF"),
    EMERALD_MATRIX("Emerald Matrix (Dark)", "এমেরাল্ড ম্যাট্রিক্স (ডার্ক)", true, "#10B981"),
    SOLARIS_AMBER("Solaris Amber (Dark)", "সোলারিস অ্যাম্বার (ডার্ক)", true, "#F59E0B"),
    CRIMSON_PULSAR("Crimson Pulsar (Dark)", "ক্রিমসন পালসার (ডার্ক)", true, "#F43F5E"),
    DAYLIGHT_PRECISION("Daylight Precision (Light)", "ডেলাইট প্রিসিশন (লাইট)", false, "#0284C7")
}

private val QuantumCyanScheme = darkColorScheme(
    primary = QuantumPrimary,
    onPrimary = Color(0xFF001F26),
    primaryContainer = QuantumCard,
    onPrimaryContainer = Color(0xFFCFFAFE),
    secondary = QuantumSecondary,
    onSecondary = Color(0xFF002114),
    secondaryContainer = Color(0xFF112E28),
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = QuantumTertiary,
    background = QuantumBg,
    onBackground = Color(0xFFF1F5F9),
    surface = QuantumSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = QuantumCard,
    onSurfaceVariant = Color(0xFF94A3B8)
)

private val EmeraldMatrixScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color(0xFF002114),
    primaryContainer = EmeraldCard,
    onPrimaryContainer = Color(0xFFD1FAE5),
    secondary = EmeraldSecondary,
    onSecondary = Color(0xFF002114),
    secondaryContainer = Color(0xFF0F2920),
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = EmeraldTertiary,
    background = EmeraldBg,
    onBackground = Color(0xFFECFDF5),
    surface = EmeraldSurface,
    onSurface = Color(0xFFECFDF5),
    surfaceVariant = EmeraldCard,
    onSurfaceVariant = Color(0xFF86EFAC)
)

private val SolarisAmberScheme = darkColorScheme(
    primary = SolarisPrimary,
    onPrimary = Color(0xFF281600),
    primaryContainer = SolarisCard,
    onPrimaryContainer = Color(0xFFFEF3C7),
    secondary = SolarisSecondary,
    onSecondary = Color(0xFF281600),
    secondaryContainer = Color(0xFF2A1D0D),
    onSecondaryContainer = Color(0xFFFDE68A),
    tertiary = SolarisTertiary,
    background = SolarisBg,
    onBackground = Color(0xFFFFFBEB),
    surface = SolarisSurface,
    onSurface = Color(0xFFFFFBEB),
    surfaceVariant = SolarisCard,
    onSurfaceVariant = Color(0xFFFCD34D)
)

private val CrimsonPulsarScheme = darkColorScheme(
    primary = CrimsonPrimary,
    onPrimary = Color(0xFF2B020A),
    primaryContainer = CrimsonCard,
    onPrimaryContainer = Color(0xFFFFE4E6),
    secondary = CrimsonSecondary,
    onSecondary = Color(0xFF2B020A),
    secondaryContainer = Color(0xFF2C0F19),
    onSecondaryContainer = Color(0xFFFECDD3),
    tertiary = CrimsonTertiary,
    background = CrimsonBg,
    onBackground = Color(0xFFFFF1F2),
    surface = CrimsonSurface,
    onSurface = Color(0xFFFFF1F2),
    surfaceVariant = CrimsonCard,
    onSurfaceVariant = Color(0xFFFDA4AF)
)

private val DaylightPrecisionScheme = lightColorScheme(
    primary = DaylightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0C4A6E),
    secondary = DaylightSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF134E4A),
    tertiary = DaylightTertiary,
    background = DaylightBg,
    onBackground = Color(0xFF0F172A),
    surface = DaylightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = DaylightCard,
    onSurfaceVariant = Color(0xFF475569)
)

@Composable
fun ApexCalcTheme(
    themePreset: AppThemePreset = AppThemePreset.QUANTUM_CYAN,
    fontPreset: AppFontPreset = AppFontPreset.SPACE_GROTESK,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themePreset) {
        AppThemePreset.QUANTUM_CYAN -> QuantumCyanScheme
        AppThemePreset.EMERALD_MATRIX -> EmeraldMatrixScheme
        AppThemePreset.SOLARIS_AMBER -> SolarisAmberScheme
        AppThemePreset.CRIMSON_PULSAR -> CrimsonPulsarScheme
        AppThemePreset.DAYLIGHT_PRECISION -> DaylightPrecisionScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = buildAppTypography(fontPreset),
        content = content
    )
}
