package ru.onefortwo.finny.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Оформление приложения. Значения перенесены из визуальной системы,
 * подготовленной по брифу (docs/design), направление 1b «Аппликация».
 *
 * Размеры основного текста — не менее 16 sp, читаемость сохраняется при
 * системном увеличении шрифта (ТЗ 3.6). Шрифт системный: приложение
 * работает без сети, а подключаемые веб-шрифты вроде Caprasimo и Figtree
 * вдобавок не содержат кириллицы.
 */

// --- Цвет ----------------------------------------------------------------

private val LightColors = lightColorScheme(
    primary = Color(0xFF3D7C47),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFFF2B705),
    onSecondary = Color(0xFF2B2B2B),
    background = Color(0xFFFFFBF2),
    onBackground = Color(0xFF2B2B2B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF2B2B2B),
    outline = Color(0xFF8A8578),
    outlineVariant = Color(0xFFDAD4C4),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8CCB96),
    onPrimary = Color(0xFF10230F),
    secondary = Color(0xFFD8A82E),
    onSecondary = Color(0xFF221A00),
    background = Color(0xFF14170F),
    onBackground = Color(0xFFF0EDE2),
    surface = Color(0xFF1F241A),
    onSurface = Color(0xFFF0EDE2),
    outline = Color(0xFF7C8473),
    outlineVariant = Color(0xFF4A5142),
    error = Color(0xFFF2B8B5),
)

/**
 * Чёрно-белая пара режима высокого контраста.
 *
 * Отдельная пара, а не поправка к светлой: цвета не приглушаются,
 * а заменяются на предельные. Чернила #000000 на бумаге #FFFFFF,
 * полутонов нет.
 */
private val HighContrastColors = lightColorScheme(
    primary = Color(0xFF000000),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF000000),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    outline = Color(0xFF000000),
    outlineVariant = Color(0xFF000000),
    error = Color(0xFF000000),
)

/**
 * Цвета направлений плана бюджета. В схему Material не входят: это не
 * роли интерфейса, а обозначения учебных категорий, поэтому вынесены
 * отдельно.
 *
 * Цвет здесь только дополняет подпись и пиктограмму: состояние и
 * категория читаются без него (ТЗ 3.6).
 */
data class BudgetColors(
    val needs: Color,
    val wants: Color,
    val savings: Color,
)

private val LightBudgetColors = BudgetColors(
    needs = Color(0xFF2E6B4F),
    wants = Color(0xFFB4552A),
    savings = Color(0xFF35558A),
)

private val DarkBudgetColors = BudgetColors(
    needs = Color(0xFF7FC9A0),
    wants = Color(0xFFE6A377),
    savings = Color(0xFF9DB6E8),
)

/**
 * В чёрно-белом режиме цвет направления не различает: все три чёрные.
 * Направление читается подписью и штриховкой пиктограммы.
 */
private val HighContrastBudgetColors = BudgetColors(
    needs = Color(0xFF000000),
    wants = Color(0xFF000000),
    savings = Color(0xFF000000),
)

/**
 * Оформление направления 1b «Аппликация»: бумажная вырезка.
 *
 * Контур и сплошное смещение вместо размытой тени дают границу элемента,
 * не опирающуюся на цвет: форма читается и при слабом различении оттенков
 * (ТЗ 3.6). При нажатии тень убирается, а элемент смещается на её место —
 * это заменяет подсветку нажатия.
 */
data class AppliqueDecor(
    val ink: Color,
    /**
     * Заливка неактивного элемента. Обязана быть непрозрачной: смещённая
     * тень рисуется под элементом, и сквозь полупрозрачный фон она
     * просвечивает, превращая кнопку в сплошное пятно.
     */
    val disabledContainer: Color,
    val disabledContent: Color,
    /** Цвет пунктирного контура неактивного элемента. */
    val disabledOutline: Color,
    val strokeWidth: Dp = 2.5.dp,
    val shadowOffset: Dp = 4.dp,
    val pressShift: Dp = 3.dp,
)

private val LightDecor = AppliqueDecor(
    ink = Color(0xFF23201A),
    disabledContainer = Color(0xFFEFE9DA),
    disabledContent = Color(0xFF6A655A),
    disabledOutline = Color(0xFF8A8578),
)

// На тёмной паре роль чернил играет светлый цвет: контур обязан
// оставаться видимым, а не сливаться с фоном.
private val DarkDecor = AppliqueDecor(
    ink = Color(0xFFF0EDE2),
    disabledContainer = Color(0xFF2A2F24),
    // Два значения подняты против исходных ради порога: 8F9686 давало
    // 4,49:1 при нужных 4,5:1, а 6D7566 — 2,87:1 при нужных 3:1.
    disabledContent = Color(0xFF909787),
    disabledOutline = Color(0xFF71796A),
)

/**
 * Оформление 1b в чёрно-белом режиме: контур толще, смещение убрано.
 *
 * Сплошная тень при обводке 2 dp читается как вторая рамка, поэтому
 * смещение обнуляется, а нажатие остаётся видимым за счёт сдвига.
 */
private val HighContrastDecor = AppliqueDecor(
    ink = Color(0xFF000000),
    disabledContainer = Color(0xFFFFFFFF),
    disabledContent = Color(0xFF000000),
    disabledOutline = Color(0xFF000000),
    strokeWidth = 2.dp,
    shadowOffset = 0.dp,
    pressShift = 2.dp,
)

/** Доступ к оформлению направления из любого места дерева композиции. */
val LocalAppliqueDecor = staticCompositionLocalOf { LightDecor }

/**
 * Цвета сцены главного экрана: небо, холмы, дерево, дом.
 *
 * Отдельно от ролей интерфейса, потому что чёрно-белому режиму сцена
 * не подчиняется. Требование режима касается текста, обводок и
 * обозначения направлений, а не рисунка: солнце и крона, окрашенные
 * в чернила, превращаются в чёрные пятна и перестают читаться. Окрас
 * питомца сохраняется по той же причине.
 */
data class SceneColors(
    val primary: Color,
    val secondary: Color,
    val surface: Color,
    val onBackground: Color,
    val ink: Color,
)

private val LightScene = SceneColors(
    primary = Color(0xFF3D7C47),
    secondary = Color(0xFFF2B705),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF2B2B2B),
    ink = Color(0xFF23201A),
)

private val DarkScene = SceneColors(
    primary = Color(0xFF8CCB96),
    secondary = Color(0xFFD8A82E),
    surface = Color(0xFF1F241A),
    onBackground = Color(0xFFF0EDE2),
    ink = Color(0xFFF0EDE2),
)

/** Доступ к цветам сцены из любого места дерева композиции. */
val LocalSceneColors = staticCompositionLocalOf { LightScene }

/**
 * Ночное небо на фоне главного экрана. Следует тёмной паре: небо — часть
 * той же сцены, и подменяется вместе с цветами, а не отдельной настройкой.
 */
val LocalNightScene = staticCompositionLocalOf { false }

/**
 * Чёрно-белый режим включён.
 *
 * Отдельный признак нужен там, где одной палитры мало: направления
 * различаются штриховкой, а зона нажатия растёт до 56 dp.
 */
val LocalHighContrast = staticCompositionLocalOf { false }

/** Доступ к цветам направлений из любого места дерева композиции. */
val LocalBudgetColors = staticCompositionLocalOf { LightBudgetColors }

// --- Типографика ---------------------------------------------------------

private val FinnyTypography = Typography(
    // Заголовок экрана.
    titleLarge = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
    // Заголовок карточки раздела.
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold),
    // Крупный текст.
    bodyLarge = TextStyle(fontSize = 18.sp, lineHeight = 26.sp),
    // Основной текст.
    bodyMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    // Надпись на кнопке.
    labelLarge = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold),
    // Подпись под фигурой питомца — единственный стиль мельче основного
    // текста; применяется только к пояснению вида и стадии.
    labelMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
)

/**
 * Типографика чёрно-белого режима: основной текст 18 sp вместо 16,
 * заголовок экрана 26 sp вместо 24.
 */
private val HighContrastTypography = Typography(
    titleLarge = TextStyle(fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold),
    bodyLarge = TextStyle(fontSize = 20.sp, lineHeight = 28.sp),
    bodyMedium = TextStyle(fontSize = 18.sp, lineHeight = 26.sp),
    labelLarge = TextStyle(fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold),
    labelMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
)

// --- Форма ---------------------------------------------------------------

/**
 * Роли формы подобраны под то, откуда их берёт Material 3, а не по
 * созвучию названий: карточка читает `medium`, поэтому радиус карточки
 * задан именно там. Кнопка по умолчанию скругляется полностью и `Shapes`
 * не читает — её форма передаётся явно в `PrimaryButton` и
 * `SecondaryButton` как `shapes.small`.
 */
private val FinnyShapes = Shapes(
    // Ярлык, плашка.
    extraSmall = RoundedCornerShape(10.dp),
    // Кнопка.
    small = RoundedCornerShape(16.dp),
    // Карточка раздела.
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Какую пару показывать. Выбирается взрослым на стартовом экране. */
enum class ThemeMode(val displayName: String) {
    SYSTEM("Как в системе"),
    LIGHT("Светлая"),
    DARK("Тёмная"),
}

/**
 * Настройки отображения. Выбираются взрослым и хранятся отдельно от
 * профиля: сброс игры их не затрагивает.
 */
data class DisplaySettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val highContrast: Boolean = false,
)

/**
 * Оформление приложения.
 *
 * Чёрно-белый режим перекрывает выбор пары: он заменяет цвета на
 * предельные, и совмещать его с тёмной парой нечем — «чёрным по белому»
 * тёмной версии не бывает. Выбор пары при этом сохраняется и вернётся,
 * когда режим выключат.
 *
 * Контраст пар посчитан отдельно — `docs/design/design-system/контраст.py`.
 */
@Composable
fun FinnyTheme(
    settings: DisplaySettings = DisplaySettings(),
    content: @Composable () -> Unit,
) {
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colors = when {
        settings.highContrast -> HighContrastColors
        dark -> DarkColors
        else -> LightColors
    }
    val budget = when {
        settings.highContrast -> HighContrastBudgetColors
        dark -> DarkBudgetColors
        else -> LightBudgetColors
    }
    val decor = when {
        settings.highContrast -> HighContrastDecor
        dark -> DarkDecor
        else -> LightDecor
    }

    CompositionLocalProvider(
        LocalBudgetColors provides budget,
        LocalAppliqueDecor provides decor,
        // Ночное небо следует тёмной паре, но не чёрно-белому режиму:
        // там небо рисуется теми же чернилами, что и всё остальное.
        LocalNightScene provides (dark && !settings.highContrast),
        LocalHighContrast provides settings.highContrast,
        // Сцена следует паре, но не чёрно-белому режиму.
        LocalSceneColors provides if (dark && !settings.highContrast) DarkScene else LightScene,
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = if (settings.highContrast) {
                HighContrastTypography
            } else {
                FinnyTypography
            },
            shapes = FinnyShapes,
            content = content,
        )
    }
}
