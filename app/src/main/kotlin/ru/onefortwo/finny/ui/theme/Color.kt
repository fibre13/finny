package ru.onefortwo.finny.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Цветовые роли интерфейса. Значения взяты из спецификации оформления
 * (Figma, «Спецификация Compose» и «Дизайн-система интерфейса»).
 *
 * Роли, у которых есть пара в `MaterialTheme.colorScheme`, передаются туда
 * же (см. [FinnyTheme]); здесь собраны все, включая те, для которых роли
 * Material нет: монета, тоновые подложки карточек, цвета направлений.
 *
 * Отступления от макета — только ради порогов контраста, которые задаёт
 * сама спецификация (текст 4,5:1, границы интерактивных элементов 3:1):
 *
 * - вспомогательный текст `#5C6660` вместо `#667169`: на тоновых подложках
 *   исходный давал 4,2:1;
 * - для надписей акцентными цветами заведены более тёмные варианты
 *   (`*Text`): коралловый `#D96C4D` на белом даёт 3,3:1, жёлтый
 *   предупреждения на своей подложке — 4,25:1. Сами заливки остаются
 *   из макета;
 * - граница интерактивного элемента `#8A8578`: светлая обводка макета
 *   `#E2DBCF` даёт 1,35:1 и оставлена только для декоративных разделителей.
 *
 * Контраст пар посчитан в `docs/design/design-system/контраст.py`.
 */
@Immutable
data class FinnyColors(
    val appBackground: Color,
    val surface: Color,
    val onSurface: Color,
    /** Вспомогательный текст: подписи, пояснения, надзаголовки. */
    val onSurfaceMuted: Color,
    val primary: Color,
    val onPrimary: Color,
    /** Подложка внутренних плиток на тёмной карточке. */
    val primaryTile: Color,
    /** Выбранный вариант и спокойная тоновая карточка. */
    val selectedContainer: Color,
    val coin: Color,
    /** Жёлтая тоновая карточка: желаемое, задание, напоминание. */
    val coinContainer: Color,
    val attention: Color,
    val attentionText: Color,
    val success: Color,
    val successText: Color,
    val successContainer: Color,
    val warning: Color,
    val warningText: Color,
    val warningContainer: Color,
    val error: Color,
    val errorText: Color,
    val errorContainer: Color,
    /** Граница интерактивного элемента, не ниже 3:1 к фону. */
    val outline: Color,
    /** Декоративный разделитель, к контрасту не обязан. */
    val divider: Color,
    val disabledContainer: Color,
    val disabledContent: Color,
    /** Незаполненная часть полосы прогресса. */
    val track: Color,
    /** Тень карточек: цвет основного с малой прозрачностью. */
    val shadow: Color,
)

val LightFinnyColors = FinnyColors(
    appBackground = Color(0xFFF7F1E5),
    surface = Color(0xFFFFFDF8),
    onSurface = Color(0xFF24352C),
    onSurfaceMuted = Color(0xFF5C6660),
    primary = Color(0xFF294C3A),
    onPrimary = Color(0xFFFFFDF8),
    primaryTile = Color(0xFF3A5D4B),
    selectedContainer = Color(0xFFE7EDDC),
    coin = Color(0xFFF8D77A),
    coinContainer = Color(0xFFFBE8AA),
    attention = Color(0xFFD96C4D),
    attentionText = Color(0xFFA84B2C),
    success = Color(0xFF3F7B57),
    successText = Color(0xFF346B4B),
    successContainer = Color(0xFFE3F1E7),
    warning = Color(0xFF9A6815),
    warningText = Color(0xFF845A12),
    warningContainer = Color(0xFFFFF0C7),
    error = Color(0xFFB94A3B),
    errorText = Color(0xFFA6402F),
    errorContainer = Color(0xFFFBE4DF),
    outline = Color(0xFF8A8578),
    divider = Color(0xFFE2DBCF),
    disabledContainer = Color(0xFFD8D4CB),
    disabledContent = Color(0xFF5C6660),
    track = Color(0xFFE6DFD2),
    shadow = Color(0x14294C3A),
)

/** Доступ к цветовым ролям из любого места дерева композиции. */
val LocalFinnyColors = staticCompositionLocalOf { LightFinnyColors }

/**
 * Цвета направлений плана бюджета. В схему Material не входят: это не
 * роли интерфейса, а обозначения учебных категорий.
 *
 * Цвет здесь только дополняет подпись и пиктограмму: категория читается
 * без него (ТЗ 3.6). Поэтому текстом эти цвета не пишутся — ими
 * закрашиваются полосы, отметки легенды и подложки карточек.
 */
@Immutable
data class BudgetColors(
    val needs: Color,
    val wants: Color,
    val savings: Color,
    val needsContainer: Color,
    val wantsContainer: Color,
    val savingsContainer: Color,
)

val LightBudgetColors = BudgetColors(
    needs = Color(0xFF5E8B68),
    wants = Color(0xFFF8D77A),
    savings = Color(0xFFD96C4D),
    needsContainer = Color(0xFFE7EDDC),
    wantsContainer = Color(0xFFFBE8AA),
    savingsContainer = Color(0xFFFFFDF8),
)

/** Доступ к цветам направлений из любого места дерева композиции. */
val LocalBudgetColors = staticCompositionLocalOf { LightBudgetColors }

/**
 * Цвета сцены главного экрана и контура питомца.
 *
 * Рисунок питомца и сцены — существующая графика приложения, и по
 * условиям переноса оформления она не меняется. Поэтому её цвета
 * отделены от ролей интерфейса и остались прежними.
 */
@Immutable
data class SceneColors(
    val primary: Color,
    val secondary: Color,
    val surface: Color,
    val onBackground: Color,
    val ink: Color,
)

val LightScene = SceneColors(
    primary = Color(0xFF3D7C47),
    secondary = Color(0xFFF2B705),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF2B2B2B),
    ink = Color(0xFF23201A),
)

/** Доступ к цветам сцены из любого места дерева композиции. */
val LocalSceneColors = staticCompositionLocalOf { LightScene }
