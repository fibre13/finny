package ru.onefortwo.finny.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Оформление приложения по спецификации из Figma: светлая тема Material 3
 * с шрифтом Inter, мягкими тенями и тоновыми карточками.
 *
 * Тёмная тема и чёрно-белый режим в этой версии отключены: под новое
 * оформление они не подготовлены. Хранилище их настроек в модуле данных
 * оставлено, чтобы вернуть их без переноса данных.
 */
private val LightColorScheme = lightColorScheme(
    primary = LightFinnyColors.primary,
    onPrimary = LightFinnyColors.onPrimary,
    primaryContainer = LightFinnyColors.selectedContainer,
    onPrimaryContainer = LightFinnyColors.onSurface,
    secondary = LightFinnyColors.coin,
    onSecondary = LightFinnyColors.onSurface,
    secondaryContainer = LightFinnyColors.coinContainer,
    onSecondaryContainer = LightFinnyColors.onSurface,
    tertiary = LightFinnyColors.attention,
    onTertiary = LightFinnyColors.onPrimary,
    background = LightFinnyColors.appBackground,
    onBackground = LightFinnyColors.onSurface,
    surface = LightFinnyColors.surface,
    onSurface = LightFinnyColors.onSurface,
    surfaceVariant = LightFinnyColors.selectedContainer,
    onSurfaceVariant = LightFinnyColors.onSurfaceMuted,
    surfaceContainer = LightFinnyColors.surface,
    surfaceContainerLow = LightFinnyColors.surface,
    surfaceContainerHigh = LightFinnyColors.surface,
    outline = LightFinnyColors.outline,
    outlineVariant = LightFinnyColors.divider,
    error = LightFinnyColors.error,
    onError = LightFinnyColors.onPrimary,
    errorContainer = LightFinnyColors.errorContainer,
    onErrorContainer = LightFinnyColors.onSurface,
)

@Composable
fun FinnyTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalFinnyColors provides LightFinnyColors,
        LocalBudgetColors provides LightBudgetColors,
        LocalSceneColors provides LightScene,
    ) {
        MaterialTheme(
            colorScheme = LightColorScheme,
            typography = FinnyTypography,
            shapes = FinnyShapes,
            content = content,
        )
    }
}

/** Короткий доступ к цветовым ролям: `FinnyTheme.colors.coin`. */
object FinnyTheme {
    val colors: FinnyColors
        @Composable get() = LocalFinnyColors.current
}
