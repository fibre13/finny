package ru.onefortwo.finny.ui.common

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.onefortwo.finny.ui.theme.FinnyTheme

/** Вкладка навигации: раздел приложения и его подпись. */
data class NavDestination(
    val section: NavSection,
    val label: String,
)

/**
 * Четыре вкладки приложения. Все ведут в существующие разделы: главный
 * экран, список заданий, гардероб питомца и учебный прогресс.
 */
val NavDestinations = listOf(
    NavDestination(NavSection.HOME, "Главная"),
    NavDestination(NavSection.TASKS, "Задания"),
    NavDestination(NavSection.PET, "Питомец"),
    NavDestination(NavSection.PROGRESS, "Прогресс"),
)

/**
 * Нижняя панель навигации телефона. Подпись каждой вкладки видна всегда,
 * выбранная вкладка отмечена подложкой и объявляется программой чтения
 * с экрана как выбранная.
 */
@Composable
fun FinnyNavigationBar(
    selected: NavSection,
    onSelect: (NavSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FinnyTheme.colors

    NavigationBar(
        modifier = modifier.border(width = 1.dp, color = colors.divider),
        containerColor = colors.surface,
        tonalElevation = 0.dp,
    ) {
        NavDestinations.forEach { destination ->
            val isSelected = destination.section == selected
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelect(destination.section) },
                icon = {
                    NavIcon(
                        section = destination.section,
                        color = if (isSelected) colors.primary else colors.onSurfaceMuted,
                    )
                },
                label = { TabLabel(destination.label) },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedTextColor = colors.onSurface,
                    unselectedTextColor = colors.onSurfaceMuted,
                    indicatorColor = colors.selectedContainer,
                ),
            )
        }
    }
}

/**
 * Боковая колонка навигации планшета. Та же последовательность вкладок,
 * что и у нижней панели телефона: на широком экране панель внизу
 * растянулась бы на всю ширину.
 */
@Composable
fun FinnyNavigationRail(
    selected: NavSection,
    onSelect: (NavSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FinnyTheme.colors

    NavigationRail(
        modifier = modifier.fillMaxHeight(),
        containerColor = colors.surface,
    ) {
        Column {
            Spacer(modifier = Modifier.height(12.dp))
            NavDestinations.forEach { destination ->
                val isSelected = destination.section == selected
                NavigationRailItem(
                    selected = isSelected,
                    onClick = { onSelect(destination.section) },
                    icon = {
                        NavIcon(
                            section = destination.section,
                            color = if (isSelected) colors.primary else colors.onSurfaceMuted,
                        )
                    },
                    label = { TabLabel(destination.label) },
                    alwaysShowLabel = true,
                    colors = NavigationRailItemDefaults.colors(
                        selectedTextColor = colors.onSurface,
                        unselectedTextColor = colors.onSurfaceMuted,
                        indicatorColor = colors.selectedContainer,
                    ),
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/** Во сколько раз подпись вкладки может вырасти вслед за системным шрифтом. */
private const val TAB_LABEL_MAX_SCALE = 1.15f

/**
 * Подпись вкладки. Растёт вместе с системным шрифтом, но не больше чем
 * в [TAB_LABEL_MAX_SCALE] раза: четыре вкладки делят ширину телефона,
 * и при шрифте 1,5× слово «Питомец» разрывалось посередине. Подпись
 * при этом остаётся видимой всегда, а программа чтения с экрана
 * произносит её полностью.
 */
@Composable
private fun TabLabel(text: String) {
    val fontScale = LocalDensity.current.fontScale
    val base = MaterialTheme.typography.labelMedium
    val scale = minOf(fontScale, TAB_LABEL_MAX_SCALE) / fontScale

    Text(
        text = text,
        style = base.copy(
            fontSize = (base.fontSize.value * scale).sp,
            lineHeight = (base.lineHeight.value * scale).sp,
        ),
        textAlign = TextAlign.Center,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Visible,
    )
}
