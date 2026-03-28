package com.excelsior.pocketvault.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.excelsior.pocketvault.domain.model.ThemeMode

private val CleanWhiteColors = lightColorScheme(
    primary = Color(0xFF111827),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF3F5F7),
    secondary = Color(0xFF616A75),
    tertiary = Color(0xFF4F7CF4),
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF6F7F9),
    onSurface = Color(0xFF111418),
    onSurfaceVariant = Color(0xFF67707B),
    outline = Color(0xFFE9ECF1),
    error = Color(0xFFB84545),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF22262C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF0E9DE),
    secondary = Color(0xFF687282),
    tertiary = Color(0xFF9E7447),
    background = Color(0xFFF4F2EE),
    surface = Color(0xFFFCFBF8),
    surfaceVariant = Color(0xFFF1EEE8),
    onSurface = Color(0xFF171A1F),
    onSurfaceVariant = Color(0xFF69707A),
    outline = Color(0xFFDDD7CE),
    error = Color(0xFFB84545),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE8EDF4),
    onPrimary = Color(0xFF14171B),
    primaryContainer = Color(0xFF222831),
    secondary = Color(0xFF9BA5B2),
    tertiary = Color(0xFFC9A57D),
    background = Color(0xFF101214),
    surface = Color(0xFF171A1E),
    surfaceVariant = Color(0xFF1D2228),
    onSurface = Color(0xFFF0F3F8),
    onSurfaceVariant = Color(0xFF9EA6B2),
    outline = Color(0xFF2B323B),
    error = Color(0xFFFFB4AB),
)

val VaultTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 27.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 23.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 15.sp),
)

val VaultShapes = Shapes(
    small = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
)

@Immutable
data class VaultMetrics(
    val xxs: Int = 4,
    val xs: Int = 8,
    val sm: Int = 12,
    val md: Int = 16,
    val lg: Int = 20,
    val xl: Int = 24,
    val xxl: Int = 32,
)

val LocalVaultMetrics = staticCompositionLocalOf { VaultMetrics() }

@Composable
fun PocketVaultTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val colors: ColorScheme = when (themeMode) {
        ThemeMode.SYSTEM -> CleanWhiteColors
        ThemeMode.LIGHT -> LightColors
        ThemeMode.DARK -> DarkColors
    }
    MaterialTheme(
        colorScheme = colors,
        typography = VaultTypography,
        shapes = VaultShapes,
        content = content,
    )
}
