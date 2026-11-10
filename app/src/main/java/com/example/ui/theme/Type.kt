package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

enum class AppFontPreset(val label: String, val bnLabel: String) {
    SPACE_GROTESK("Space Grotesk", "স্পেস গ্রোটেস্ক (আধুনিক)"),
    JETBRAINS_MONO("JetBrains Mono", "জেটব্রেইনস মনো (কোডিং)"),
    OUTFIT("Outfit Geometric", "আউটফিট (মিনিমাল)"),
    ROBOTO_MONO("Roboto Mono", "রোবোটো মনো (ক্লাসিক)")
}

val SpaceGroteskFamily = FontFamily(Font(R.font.space_grotesk, FontWeight.Normal))
val JetBrainsMonoFamily = FontFamily(Font(R.font.jetbrains_mono, FontWeight.Normal))
val OutfitFamily = FontFamily(Font(R.font.outfit, FontWeight.Normal))
val RobotoMonoFamily = FontFamily(Font(R.font.roboto_mono, FontWeight.Normal))

fun buildAppTypography(preset: AppFontPreset): Typography {
    val displayFamily = when (preset) {
        AppFontPreset.SPACE_GROTESK -> SpaceGroteskFamily
        AppFontPreset.JETBRAINS_MONO -> JetBrainsMonoFamily
        AppFontPreset.OUTFIT -> OutfitFamily
        AppFontPreset.ROBOTO_MONO -> RobotoMonoFamily
    }
    val monoFamily = when (preset) {
        AppFontPreset.JETBRAINS_MONO -> JetBrainsMonoFamily
        AppFontPreset.ROBOTO_MONO -> RobotoMonoFamily
        else -> JetBrainsMonoFamily
    }

    return Typography(
        displayLarge = TextStyle(
            fontFamily = monoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 42.sp,
            lineHeight = 48.sp,
            letterSpacing = (-0.5).sp
        ),
        displayMedium = TextStyle(
            fontFamily = monoFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp,
            lineHeight = 38.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 30.sp
        ),
        titleLarge = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            lineHeight = 26.sp
        ),
        titleMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 22.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        labelLarge = TextStyle(
            fontFamily = monoFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 18.sp
        ),
        labelMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    )
}
