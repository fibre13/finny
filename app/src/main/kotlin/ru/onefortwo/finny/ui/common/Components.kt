package ru.onefortwo.finny.ui.common

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ru.onefortwo.finny.economy.Coins
import ru.onefortwo.finny.economy.GrowthStage
import ru.onefortwo.finny.economy.StatLevel
import ru.onefortwo.finny.ui.state.Explanations
import ru.onefortwo.finny.ui.state.FeedbackMessage
import ru.onefortwo.finny.ui.theme.FinnyTheme
import ru.onefortwo.finny.ui.theme.LightFinnyColors
import ru.onefortwo.finny.ui.theme.LocalSceneColors
import ru.onefortwo.finny.ui.theme.PillShape

/**
 * Общие элементы интерфейса по спецификации оформления (Figma,
 * «Спецификация Compose»): каркас экрана, кнопки, карточки, поля,
 * кнопки шага, полосы и показатели.
 *
 * Требования доступности (ТЗ 3.6): интерактивные элементы не меньше 48 dp,
 * основной текст не меньше 16 sp, состояние передаётся текстом и знаком,
 * а не только цветом, кнопка возврата расположена единообразно.
 */

/**
 * Минимальный размер интерактивного элемента.
 *
 * Поднимается с 48 до 56 dp при системном увеличении шрифта: его включают
 * те, кому трудно попадать в мелкие элементы, и запас зоны нажатия нужен
 * им по той же причине, по которой нужен крупный текст.
 */
val MinTouchTarget: Dp
    @Composable get() = if (LocalDensity.current.fontScale >= 1.3f) 56.dp else 48.dp

/** Высота основного элемента управления: кнопки и поля ввода. */
val ControlHeight = 54.dp

/**
 * Предельная ширина колонки содержимого.
 *
 * На телефоне колонка занимает всю ширину экрана за вычетом полей. На
 * планшете и в альбомной ориентации она ограничивается и центрируется:
 * иначе строки текста растягиваются на всю ширину и перестают читаться,
 * а кнопки превращаются в полосы во весь экран (ТЗ 3.1).
 */
val MaxContentWidth = 640.dp

/** Боковые поля экрана телефона. */
val ScreenPadding = 18.dp

/** Расстояние между карточками. */
val CardSpacing = 14.dp

/**
 * Цвет вспомогательного текста внутри текущей карточки. На тёмной
 * карточке он светлый, на остальных — приглушённый тёмный.
 */
val LocalMutedColor = compositionLocalOf { LightFinnyColors.onSurfaceMuted }

/**
 * Мягкая тень карточек и кнопок: размытая, со смещением вниз, цвета
 * основного с малой прозрачностью — как в макете.
 */
fun Modifier.softShadow(shape: Shape, lift: Boolean = true): Modifier = dropShadow(
    shape = shape,
    shadow = Shadow(
        radius = if (lift) 18.dp else 8.dp,
        color = LightFinnyColors.shadow,
        offset = DpOffset(0.dp, if (lift) 6.dp else 2.dp),
    ),
)

/** Вспомогательный текст: пояснение, подпись, подсказка. */
@Composable
fun SupportingText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = LocalMutedColor.current,
        modifier = modifier,
    )
}

/**
 * Надзаголовок прописными: «Задание дня», «Не хватает монет». Цвет
 * по умолчанию — затемнённый коралловый, он проходит порог 4,5:1.
 */
@Composable
fun Eyebrow(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = FinnyTheme.colors.attentionText,
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier,
    )
}

/**
 * Каркас экрана: шапка с кнопкой возврата, надзаголовком, заголовком
 * и балансом монет, ниже — прокручиваемое содержимое, ограниченное
 * по ширине на широких экранах.
 *
 * Кнопка возврата всегда на одном месте — слева в шапке (ТЗ 3.6).
 *
 * Отклик на действие ([message]) закрепляется у нижнего края и не уезжает
 * вместе с прокруткой. Раньше он выводился первым блоком содержимого,
 * и на длинных экранах — покупки, копилка — оказывался выше видимой
 * части: кнопка действия внизу, ответ на неё вверху. Требование ТЗ 2.5.9 —
 * увидеть изменение после действия.
 *
 * @param eyebrow строка над заголовком: откуда экран или к чему относится.
 * @param balance баланс монет в правом углу шапки; `null` — не показывать.
 * @param top содержимое над шапкой, например шаги знакомства.
 */
@Composable
fun ScreenScaffold(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    balance: Coins? = null,
    message: FeedbackMessage? = null,
    onDismissMessage: () -> Unit = {},
    top: (@Composable () -> Unit)? = null,
    /** Смена значения возвращает прокрутку к началу экрана. */
    scrollKey: Any? = null,
    content: @Composable () -> Unit,
) {
    val colors = FinnyTheme.colors
    val scroll = key(scrollKey) { rememberScrollState() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.appBackground)
            // Отступы от системных панелей и экранной клавиатуры: поле
            // ввода не уходит под клавиатуру, последняя кнопка — под
            // панель жестов.
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        CompositionLocalProvider(
            LocalContentColor provides colors.onSurface,
            LocalMutedColor provides colors.onSurfaceMuted,
        ) {
            // Высота закреплённого отклика замеряется, а не задаётся числом:
            // при увеличенном шрифте карточка выше, и содержимое под ней
            // иначе осталось бы недоступным.
            var messageHeight by remember { mutableStateOf(0.dp) }
            val density = LocalDensity.current

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = MaxContentWidth)
                        .fillMaxWidth()
                        .padding(horizontal = ScreenPadding)
                        // Запас снизу нужен, только пока отклик показан:
                        // замеренная высота сама не обнуляется, когда карточка
                        // исчезает, и внизу оставалась бы пустая полоса.
                        .padding(bottom = 28.dp + if (message != null) messageHeight else 0.dp),
                ) {
                    if (top != null) {
                        Box(modifier = Modifier.padding(top = 16.dp)) { top() }
                    }
                    ScreenHeader(
                        title = title,
                        eyebrow = eyebrow,
                        balance = balance,
                        onBack = onBack,
                    )
                    content()
                }
            }

            if (message != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(colors.appBackground)
                        .onSizeChanged { size ->
                            messageHeight = with(density) { size.height.toDp() }
                        },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = MaxContentWidth)
                            .fillMaxWidth()
                            .padding(horizontal = ScreenPadding)
                            .padding(top = 8.dp, bottom = 4.dp),
                    ) {
                        FeedbackCard(message = message, onDismiss = onDismissMessage)
                    }
                }
            }
        }
    }
}

/**
 * Шапка экрана. Заголовок переносится по словам, а баланс не сжимается:
 * при увеличенном шрифте заголовок уходит на вторую строку, число монет
 * остаётся читаемым целиком.
 */
@Composable
private fun ScreenHeader(
    title: String,
    eyebrow: String?,
    balance: Coins?,
    onBack: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            BackButton(onClick = onBack)
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            if (eyebrow != null) {
                Text(
                    text = eyebrow,
                    style = MaterialTheme.typography.labelMedium,
                    color = LocalMutedColor.current,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
        }
        if (balance != null) {
            Spacer(modifier = Modifier.width(12.dp))
            CoinBalance(amount = balance)
        }
    }
}

/** Кнопка возврата: стрелка в квадрате со скруглением, подписана «Назад». */
@Composable
private fun BackButton(onClick: () -> Unit) {
    val colors = FinnyTheme.colors
    val shape = MaterialTheme.shapes.small
    val size = maxOf(MinTouchTarget, 48.dp)

    Box(
        modifier = Modifier
            .size(size)
            .softShadow(shape, lift = false)
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.divider, shape)
            .clickable(role = Role.Button, onClickLabel = null, onClick = onClick)
            .semantics { contentDescription = "Назад" },
        contentAlignment = Alignment.Center,
    ) {
        ArrowBackIcon(color = colors.onSurface)
    }
}

/**
 * Баланс монет: круглая монета и число. Баланс не передаётся одним цветом:
 * число написано, а программа чтения с экрана произносит «Баланс: 120 монет».
 */
@Composable
fun CoinBalance(
    amount: Coins,
    modifier: Modifier = Modifier,
) {
    val colors = FinnyTheme.colors
    val description = "Баланс: ${Explanations.coins(amount)}"

    Row(
        modifier = modifier
            .softShadow(PillShape, lift = false)
            .clip(PillShape)
            .background(colors.surface)
            .heightIn(min = 44.dp)
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(colors.coin),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = amount.amount.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = colors.onSurface,
        )
    }
}

// --- Кнопки ----------------------------------------------------------------

/** Заливка основной кнопки. */
enum class ButtonTone {
    /** Тёмно-зелёная: основное действие экрана. */
    Primary,

    /** Жёлтая: действие внутри жёлтой карточки отклика. */
    Coin,

    /** Светлая: действие на тёмной карточке. */
    Light,
}

/**
 * Основная кнопка: «таблетка» высотой 54 dp.
 *
 * Неактивная сохраняет подпись и становится серой; причину недоступности
 * экран пишет текстом рядом. При нажатии кнопка чуть уменьшается.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tone: ButtonTone = ButtonTone.Primary,
) {
    val colors = FinnyTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val (container, content) = when (tone) {
        ButtonTone.Primary -> colors.primary to colors.onPrimary
        ButtonTone.Coin -> colors.coin to colors.onSurface
        ButtonTone.Light -> colors.surface to colors.onSurface
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaction,
        shape = PillShape,
        elevation = null,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = colors.disabledContainer,
            disabledContentColor = colors.disabledContent,
        ),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = maxOf(ControlHeight, MinTouchTarget))
            .pressScale(pressed),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

/** Вторичная кнопка того же размера: светлая заливка и контур. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    /**
     * Пиктограмма перед подписью. Ничего не заменяет: подпись читается
     * сама по себе, рисунок помогает узнать раздел (ТЗ 3.6).
     */
    icon: (@Composable () -> Unit)? = null,
) {
    val colors = FinnyTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaction,
        shape = PillShape,
        border = if (enabled) BorderStroke(1.5.dp, colors.outline) else null,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = colors.surface,
            contentColor = colors.onSurface,
            disabledContainerColor = colors.disabledContainer,
            disabledContentColor = colors.disabledContent,
        ),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = maxOf(ControlHeight, MinTouchTarget))
            .pressScale(pressed),
    ) {
        if (icon != null) {
            icon()
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

/** Нажатый элемент чуть уменьшается: отклик виден без анимации цвета. */
private fun Modifier.pressScale(pressed: Boolean): Modifier =
    if (pressed) graphicsLayer(scaleX = 0.98f, scaleY = 0.98f) else this

/**
 * Вариант выбора: строка с отметкой. Выбранный выделен фоном, обводкой,
 * знаком ✓ и словом «выбрано» — состояние читается без цвета (ТЗ 3.6).
 * Для программы чтения с экрана это переключатель с состоянием.
 *
 * @param selectedSuffix слово выбранного состояния: в гардеробе «надето».
 * @param supporting пояснение под названием.
 */
@Composable
fun SelectButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selectedSuffix: String = "выбрано",
    supporting: String? = null,
    leading: (@Composable () -> Unit)? = null,
) {
    val colors = FinnyTheme.colors
    val shape = MaterialTheme.shapes.small

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = maxOf(ControlHeight, MinTouchTarget))
            .clip(shape)
            .background(
                when {
                    !enabled -> colors.disabledContainer
                    selected -> colors.selectedContainer
                    else -> colors.surface
                },
            )
            .border(
                width = if (selected) 2.dp else 1.5.dp,
                color = when {
                    !enabled -> colors.disabledContainer
                    selected -> colors.primary
                    else -> colors.outline
                },
                shape = shape,
            )
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .semantics { stateDescription = if (selected) selectedSuffix else "не $selectedSuffix" }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SelectionMark(selected = selected, enabled = enabled)
        Spacer(modifier = Modifier.width(12.dp))
        if (leading != null) {
            leading()
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) colors.onSurface else colors.disabledContent,
            )
            if (supporting != null) {
                SupportingText(supporting)
            }
            if (selected) {
                Text(
                    text = "✓ $selectedSuffix",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.successText,
                    modifier = Modifier.clearAndSetSemantics { },
                )
            }
        }
    }
}

/**
 * Плитка выбора для сетки: образец сверху, название и состояние снизу.
 * Используется для окраса и украшений, где рядом нужен образец цвета.
 *
 * @param status строка состояния для невыбранной плитки: «есть»,
 * «не куплено». У выбранной вместо неё «✓ выбрано».
 */
@Composable
fun OptionTile(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selectedSuffix: String = "выбрано",
    status: String? = null,
    swatch: (@Composable () -> Unit)? = null,
) {
    val colors = FinnyTheme.colors
    val shape = MaterialTheme.shapes.small

    Column(
        modifier = modifier
            .heightIn(min = MinTouchTarget)
            .clip(shape)
            .background(
                when {
                    !enabled -> colors.disabledContainer
                    selected -> colors.selectedContainer
                    else -> colors.surface
                },
            )
            .border(
                width = if (selected) 2.dp else 1.5.dp,
                color = when {
                    !enabled -> colors.disabledContainer
                    selected -> colors.primary
                    else -> colors.outline
                },
                shape = shape,
            )
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .semantics { stateDescription = if (selected) selectedSuffix else (status ?: "не $selectedSuffix") }
            .padding(horizontal = 10.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (swatch != null) {
            swatch()
            Spacer(modifier = Modifier.size(10.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = if (enabled) colors.onSurface else colors.disabledContent,
        )
        val line = if (selected) "✓ $selectedSuffix" else status
        if (line != null) {
            Text(
                text = line,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                color = when {
                    selected -> colors.successText
                    !enabled -> colors.disabledContent
                    else -> colors.onSurfaceMuted
                },
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clearAndSetSemantics { },
            )
        }
    }
}

/** Круглая отметка выбора: пустая или с галочкой. */
@Composable
private fun SelectionMark(selected: Boolean, enabled: Boolean) {
    val colors = FinnyTheme.colors
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(if (selected) colors.primary else colors.surface)
            .border(2.dp, if (enabled) colors.primary else colors.disabledContent, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) CheckIcon(color = colors.onPrimary, size = 16.dp)
    }
}

/** Образец цвета для плитки выбора окраса. */
@Composable
fun ColorSwatch(color: Color, size: Dp = 36.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            // Контур у образца: светлый окрас иначе теряется на подложке.
            .border(1.5.dp, FinnyTheme.colors.outline, CircleShape),
    )
}

/**
 * Сетка, которая сама подбирает число колонок по ширине: не меньше
 * [minItemWidth] на ячейку, с учётом масштаба шрифта. Так плитки выбора
 * не рвут слова посередине: при нехватке места колонок становится меньше.
 */
@Composable
fun AdaptiveGrid(
    count: Int,
    modifier: Modifier = Modifier,
    minItemWidth: Dp = 150.dp,
    spacing: Dp = 10.dp,
    item: @Composable RowScope.(index: Int) -> Unit,
) {
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val cell = minItemWidth * fontScale
        val columns = ((maxWidth + spacing) / (cell + spacing)).toInt().coerceIn(1, count.coerceAtLeast(1))
        Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
            (0 until count).chunked(columns).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                ) {
                    row.forEach { index -> item(index) }
                    // Пустые ячейки держат ширину плиток в неполном ряду.
                    repeat(columns - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}

// --- Карточки ----------------------------------------------------------------

/** Подложка карточки. Смысл карточки всегда назван заголовком, а не тоном. */
enum class CardTone {
    /** Светлая с мягкой тенью — по умолчанию. */
    Surface,

    /** Салатовая: нужное, питомец, спокойное пояснение. */
    Sage,

    /** Жёлтая: желаемое, задание, напоминание. */
    Coin,

    /** Тёмно-зелёная со светлым текстом: сводка, цель, главное число. */
    Primary,
    Success,
    Warning,
    Error,
}

/**
 * Карточка раздела: необязательные пиктограмма, надзаголовок, заголовок
 * и содержимое. Скругление 24 dp, высота по содержимому.
 *
 * @param trailing элемент справа от заголовка: плашка состояния, число.
 */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    tone: CardTone = CardTone.Surface,
    eyebrow: String? = null,
    /**
     * Пиктограмма в плитке над заголовком. Ничего не заменяет: раздел
     * назван словом, а рисунок помогает его узнать (ТЗ 3.6).
     */
    icon: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val colors = FinnyTheme.colors
    val shape = MaterialTheme.shapes.medium
    val dark = tone == CardTone.Primary
    val container = when (tone) {
        CardTone.Surface -> colors.surface
        CardTone.Sage -> colors.selectedContainer
        CardTone.Coin -> colors.coinContainer
        CardTone.Primary -> colors.primary
        CardTone.Success -> colors.successContainer
        CardTone.Warning -> colors.warningContainer
        CardTone.Error -> colors.errorContainer
    }
    val contentColor = if (dark) colors.onPrimary else colors.onSurface
    val muted = if (dark) colors.onPrimary.copy(alpha = 0.82f) else colors.onSurfaceMuted

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = CardSpacing)
            .then(if (tone == CardTone.Surface) Modifier.softShadow(shape) else Modifier)
            .clip(shape)
            .background(container)
            .padding(horizontal = 18.dp, vertical = 18.dp),
    ) {
        CompositionLocalProvider(
            LocalContentColor provides contentColor,
            LocalMutedColor provides muted,
        ) {
            if (icon != null) {
                IconTile(modifier = Modifier.padding(bottom = 12.dp)) { icon() }
            }
            if (title != null || trailing != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (eyebrow != null) {
                            Eyebrow(
                                text = eyebrow,
                                color = if (dark) colors.coin else colors.attentionText,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                        }
                        if (title != null) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.semantics { heading() },
                            )
                        }
                    }
                    if (trailing != null) {
                        Spacer(modifier = Modifier.width(10.dp))
                        trailing()
                    }
                }
            } else if (eyebrow != null) {
                Eyebrow(
                    text = eyebrow,
                    color = if (dark) colors.coin else colors.attentionText,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            content()
        }
    }
}

/** Светлая плитка под пиктограмму. */
@Composable
fun IconTile(
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    container: Color = FinnyTheme.colors.surface,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(MaterialTheme.shapes.small)
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/**
 * Плашка состояния: «В порядке», «Готово». Текст обязателен — плашка
 * не передаёт состояние одним цветом.
 */
@Composable
fun StatusPill(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = FinnyTheme.colors.coin,
    content: Color = FinnyTheme.colors.onSurface,
    uppercase: Boolean = true,
) {
    Text(
        text = if (uppercase) text.uppercase() else text,
        style = MaterialTheme.typography.labelSmall,
        color = content,
        modifier = modifier
            .clip(PillShape)
            .background(container)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

/**
 * Сообщение обратной связи. Затруднение помечается знаком «!» и словом
 * «Внимание», удача — знаком «✓», а не только цветом (ТЗ 3.6).
 */
@Composable
fun FeedbackCard(
    message: FeedbackMessage,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FinnyTheme.colors
    val problem = message.isProblem

    SectionCard(
        modifier = modifier,
        tone = if (problem) CardTone.Warning else CardTone.Success,
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(size = 38.dp) {
                    Text(
                        text = if (problem) "!" else "✓",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (problem) colors.warningText else colors.successText,
                        modifier = Modifier.clearAndSetSemantics { },
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Eyebrow(
                    text = if (problem) "Внимание" else "Что произошло",
                    color = if (problem) colors.warningText else colors.successText,
                )
            }
            Text(
                text = message.text,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 10.dp),
            )
            if (message.nextStep != null) {
                SupportingText(
                    text = "Что дальше: ${message.nextStep}",
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            PrimaryButton(
                text = "Понятно",
                onClick = onDismiss,
                tone = if (problem) ButtonTone.Coin else ButtonTone.Primary,
                modifier = Modifier.padding(top = 14.dp),
            )
        }
    }
}

// --- Поля и диалоги ----------------------------------------------------------

/**
 * Поле ввода. Подпись стоит над полем, а не внутри него: плавающая подпись
 * при вводе уезжает вверх и мелчает, а над кнопками шага и списками подписи
 * тоже сверху.
 *
 * Высота не меньше 54 dp; в фокусе обводка 2 dp основным цветом, при
 * ошибке — цветом ошибки и с текстом ошибки под полем.
 *
 * @param maxLength предел длины; если задан, в поле виден счётчик «5 / 12».
 */
@Composable
fun FinnyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
    supportingText: String? = null,
    errorText: String? = null,
    maxLength: Int? = null,
) {
    val colors = FinnyTheme.colors
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            isError = errorText != null,
            shape = MaterialTheme.shapes.small,
            textStyle = MaterialTheme.typography.titleMedium,
            // «Готово» на клавиатуре убирает её: иначе клавиатура закрывает
            // кнопку, ради которой поле заполняли.
            keyboardOptions = keyboardOptions.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            trailingIcon = maxLength?.let {
                {
                    Text(
                        text = "${value.length} / $it",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceMuted,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.outline,
                errorBorderColor = colors.error,
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surface,
                errorContainerColor = colors.surface,
                cursorColor = colors.primary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = maxOf(ControlHeight, MinTouchTarget)),
        )
        if (errorText != null) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.errorText,
                modifier = Modifier.padding(top = 6.dp),
            )
        } else if (supportingText != null) {
            SupportingText(supportingText, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

/**
 * Диалог подтверждения. Собран из тех же частей, что карточки и кнопки:
 * `AlertDialog` приносит свои отступы и текстовые кнопки без заливки
 * и выглядел бы чужим.
 *
 * @param actions кнопки внизу. Передаются целиком, потому что их число
 * и расположение зависят от вопроса.
 */
@Composable
fun FinnyDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
    actions: @Composable ColumnScope.() -> Unit,
) {
    val colors = FinnyTheme.colors
    val shape = MaterialTheme.shapes.extraLarge

    Dialog(onDismissRequest = onDismiss) {
        // Цвет текста задаётся явно: диалог вызывается вне каркаса экрана.
        CompositionLocalProvider(
            LocalContentColor provides colors.onSurface,
            LocalMutedColor provides colors.onSurfaceMuted,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(colors.surface)
                    .padding(24.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .semantics { heading() },
                )
                content()
                Spacer(modifier = Modifier.size(20.dp))
                actions()
            }
        }
    }
}

// --- Значения и показатели ---------------------------------------------------

/**
 * Строка «подпись — значение» для показа сумм.
 *
 * Когда подпись и значение перестают помещаться рядом, значение уходит
 * на вторую строку целиком, а не сжимает подпись: при шрифте 1,5× подпись
 * в узкой колонке разрывалась бы на три строки.
 */
@Composable
fun LabeledValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        maxItemsInEachRow = 2,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = LocalMutedColor.current)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

/**
 * Полоса показателя. Уровень дублируется числом, текстовой меткой
 * и знаком, поэтому информация не передаётся только цветом (ТЗ 3.6).
 */
@Composable
fun StatBar(
    name: String,
    value: Int,
    label: String,
    level: StatLevel,
    modifier: Modifier = Modifier,
) {
    val colors = FinnyTheme.colors

    Column(
        modifier = modifier
            .padding(vertical = 6.dp)
            // Программа чтения с экрана озвучивает показатель одной фразой.
            .semantics(mergeDescendants = true) {
                contentDescription = "$name: $label, $value из 100"
            },
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.Center,
            maxItemsInEachRow = 2,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Пиктограмма только помогает узнать уровень: он уже назван
                // словом в подписи рядом.
                StatLevelIcon(level = level, modifier = Modifier.padding(end = 8.dp))
                Text("$name: $label", style = MaterialTheme.typography.bodyMedium)
            }
            Text("$value из 100", style = MaterialTheme.typography.titleMedium)
        }
        ProgressBar(
            fraction = value / 100f,
            color = when (level) {
                StatLevel.LOW -> colors.attention
                StatLevel.MEDIUM -> colors.success
                StatLevel.HIGH -> colors.success
            },
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** Полоса заполнения: дорожка 10 dp со скруглёнными краями. */
@Composable
fun ProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = FinnyTheme.colors.success,
    trackColor: Color = FinnyTheme.colors.track,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 10.dp)
            .clip(PillShape)
            .background(trackColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .sizeIn(minHeight = 10.dp)
                .clip(PillShape)
                .background(color),
        )
    }
}

/**
 * Выбор количества монет кнопками «−5», «−1», «+1», «+5».
 *
 * Заменил ползунок: при бюджете 50 монет одно значение ползунка
 * приходилось примерно на 5 dp хода, и точное число с первого раза
 * не выставлялось (замечание тестировщика). Каждая кнопка — цель нажатия
 * не меньше [MinTouchTarget] (ТЗ 3.6), число меняется на известный шаг.
 *
 * Значение не выходит за `0..max`: шаг у края урезается до края, а кнопка,
 * которой двигаться некуда, выключается. При нулевом `max` кнопки остаются
 * на месте выключенными, а не исчезают.
 *
 * Программа чтения с экрана произносит у каждой кнопки направление и
 * действие: «Нужное: прибавить 5», а не только «+5».
 *
 * @param label название направления для программы чтения с экрана.
 */
@Composable
fun CoinStepper(
    label: String,
    value: Int,
    max: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        STEPS.forEach { step ->
            // «Плюс» только прибавляет, «минус» только убавляет: если
            // значение больше максимума — так бывает, когда бюджет
            // уменьшился, — «+5» не должно урезать его вниз.
            val target = if (step > 0) {
                minOf(value + step, max).coerceAtLeast(value)
            } else {
                maxOf(value + step, 0).coerceAtMost(value)
            }
            StepButton(
                text = if (step < 0) "−${-step}" else "+$step",
                description = if (step < 0) "$label: убавить на ${-step}" else "$label: прибавить $step",
                enabled = target != value,
                onClick = { onChange(target) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private val STEPS = listOf(-5, -1, 1, 5)

/**
 * Экранная цифровая панель для числового ответа.
 *
 * Заменила системную клавиатуру: не нужно попадать в поле и ждать
 * клавиатуру, а клавиши крупные и одинаковые на любом устройстве. Ответ
 * ребёнок по-прежнему набирает сам (ТЗ 2.5.8).
 */
@Composable
fun DigitPad(
    onDigit: (Int) -> Unit,
    onErase: () -> Unit,
    eraseEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9)).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { digit ->
                    StepButton(
                        text = "$digit",
                        description = null,
                        enabled = true,
                        onClick = { onDigit(digit) },
                        modifier = Modifier.weight(1f),
                        tall = true,
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StepButton(
                text = "0",
                description = null,
                enabled = true,
                onClick = { onDigit(0) },
                modifier = Modifier.weight(1f),
                tall = true,
            )
            StepButton(
                text = "Стереть",
                description = null,
                enabled = eraseEnabled,
                onClick = onErase,
                modifier = Modifier.weight(2f),
                tall = true,
            )
        }
    }
}

/**
 * Компактная кнопка шага или цифры: светлая плитка со скруглением 18 dp
 * и контуром, узкие боковые отступы — в ряд из четырёх кнопок стандартные
 * отступы не оставили бы места подписи при крупном шрифте.
 */
@Composable
private fun StepButton(
    text: String,
    description: String?,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tall: Boolean = false,
) {
    val colors = FinnyTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val minHeight = if (tall) maxOf(MinTouchTarget, 56.dp) else maxOf(MinTouchTarget, 52.dp)

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaction,
        shape = MaterialTheme.shapes.small,
        border = if (enabled) BorderStroke(1.5.dp, colors.outline) else null,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = colors.surface,
            contentColor = colors.onSurface,
            disabledContainerColor = colors.disabledContainer,
            disabledContentColor = colors.disabledContent,
        ),
        modifier = modifier
            .heightIn(min = minHeight)
            .pressScale(pressed),
    ) {
        // Описание заменяет подпись, а не добавляется к ней: программа
        // чтения с экрана произносит «Нужное: прибавить 5, кнопка».
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            modifier = if (description != null) {
                Modifier.clearAndSetSemantics { contentDescription = description }
            } else {
                Modifier
            },
        )
    }
}

// --- Питомец -----------------------------------------------------------------

/**
 * Изображение питомца. Сам рисунок и сцена — существующая графика
 * приложения и не меняются; здесь только рамка вокруг них.
 */
@Composable
fun PetFigure(
    petName: String,
    speciesId: String,
    speciesTitle: String,
    accessoryId: String,
    accessoryTitle: String,
    colorHex: String,
    stage: GrowthStage,
    modifier: Modifier = Modifier,
    care: StatLevel = StatLevel.MEDIUM,
    joy: StatLevel = StatLevel.MEDIUM,
    /**
     * Показывать фон-сцену за фигурой. На главном экране — да; на экранах
     * выбора внешности и гардероба фигура показывается без сцены, чтобы
     * ничто не спорило с выбором окраса и украшения.
     */
    scene: Boolean = false,
    /**
     * Показывать дом питомца на фоне. Появляется после покупки
     * «Домика-палатки» и остаётся навсегда.
     */
    house: Boolean = false,
    size: Dp = 140.dp,
    /** Подписи с именем и стадией под рисунком. */
    caption: Boolean = true,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val base = parseColor(colorHex)
        val sceneColors = LocalSceneColors.current

        // Фигура озвучивается одной фразой: для программы чтения с экрана
        // это один объект.
        val describePet = Modifier.semantics(mergeDescendants = true) {
            contentDescription = "$speciesTitle, $accessoryTitle"
        }

        if (scene) {
            // Питомец стоит внутри сцены, а не поверх неё: дом занимает
            // левую часть, дерево середину, питомец правую.
            val landscape = LocalConfiguration.current.orientation ==
                Configuration.ORIENTATION_LANDSCAPE
            val topCrop = if (landscape) SCENE_TOP_CROP_LANDSCAPE else 0f
            val shape = MaterialTheme.shapes.medium

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(sceneAspect(topCrop))
                    .clip(shape)
                    .background(FinnyTheme.colors.appBackground)
                    .then(describePet),
            ) {
                drawScene(
                    primary = sceneColors.primary,
                    secondary = sceneColors.secondary,
                    surface = sceneColors.surface,
                    onBackground = sceneColors.onBackground,
                    ink = sceneColors.ink,
                    night = false,
                    house = house,
                    topCrop = topCrop,
                ) {
                    drawPet(
                        speciesId = speciesId,
                        baseColor = base,
                        accessoryId = accessoryId,
                        stage = stage,
                        care = care,
                        joy = joy,
                        fitToCanvas = false,
                        outlineColor = sceneColors.ink,
                    )
                }
            }
        } else {
            val petShape = MaterialTheme.shapes.large

            Box(
                modifier = Modifier
                    .size(size)
                    .clip(petShape)
                    .background(FinnyTheme.colors.surface)
                    .then(describePet),
                contentAlignment = Alignment.Center,
            ) {
                // Доля 118 к 140 взята из канвы: фигура не упирается в рамку.
                Canvas(modifier = Modifier.size(size * 0.84f)) {
                    drawPet(
                        speciesId = speciesId,
                        baseColor = base,
                        accessoryId = accessoryId,
                        stage = stage,
                        care = care,
                        joy = joy,
                        outlineColor = sceneColors.ink,
                    )
                }
            }
        }
        if (caption) {
            Text(
                text = petName,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 10.dp),
            )
            Text(
                text = petCaption(stage, accessoryTitle),
                style = MaterialTheme.typography.bodyMedium,
                color = LocalMutedColor.current,
            )
        }
    }
}

/** Подпись питомца: «Малыш · бантик». */
fun petCaption(stage: GrowthStage, accessoryTitle: String): String =
    "${stage.displayName} · ${accessoryTitle.lowercase()}"

/** Переводит цвет вида «#RRGGBB» в значение Compose. */
fun parseColor(hex: String): Color =
    runCatching { Color(("FF" + hex.removePrefix("#")).toLong(16)) }
        .getOrElse { Color(0xFFCCCCCC) }
