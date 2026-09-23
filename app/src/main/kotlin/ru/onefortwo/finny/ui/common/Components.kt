package ru.onefortwo.finny.ui.common

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlin.math.roundToInt
import ru.onefortwo.finny.economy.GrowthStage
import ru.onefortwo.finny.economy.StatLevel
import ru.onefortwo.finny.ui.state.FeedbackMessage
import ru.onefortwo.finny.ui.theme.LocalAppliqueDecor
import ru.onefortwo.finny.ui.theme.LocalHighContrast
import ru.onefortwo.finny.ui.theme.LocalNightScene
import ru.onefortwo.finny.ui.theme.LocalSceneColors

/**
 * Общие элементы интерфейса.
 *
 * Требования доступности (ТЗ 3.6): интерактивные элементы не меньше 48 dp,
 * основной текст не меньше 16 sp, состояние передаётся текстом и знаком,
 * а не только цветом, кнопка возврата расположена единообразно.
 */

/**
 * Минимальный размер интерактивного элемента.
 *
 * Поднимается с 48 до 56 dp при системном увеличении шрифта и в
 * чёрно-белом режиме. Причина в обоих случаях одна и не в самом шрифте:
 * их включают те, кому трудно попадать в мелкие элементы, и запас зоны
 * нажатия нужен им по той же причине, по которой нужен крупный текст.
 */
val MinTouchTarget: Dp
    @Composable get() = if (
        LocalDensity.current.fontScale >= 1.3f || LocalHighContrast.current
    ) {
        56.dp
    } else {
        48.dp
    }

/**
 * Предельная ширина колонки содержимого.
 *
 * На телефоне колонка занимает всю ширину экрана. На планшете и в альбомной
 * ориентации она ограничивается этим значением и центрируется: иначе строки
 * текста растягиваются на всю ширину и перестают читаться, а кнопки
 * превращаются в полосы во весь экран (ТЗ 3.1, поддержка планшетов).
 */
val MaxContentWidth = 640.dp

/**
 * Каркас экрана: заголовок, кнопка возврата в одном и том же месте
 * и прокручиваемое содержимое, ограниченное по ширине на широких экранах.
 *
 * Отклик на действие ([message]) закрепляется у нижнего края и не уезжает
 * вместе с прокруткой. Раньше он выводился первым блоком содержимого,
 * и на длинных экранах — покупки, копилка — оказывался выше видимой
 * части: кнопка действия внизу, ответ на неё вверху. Проверка на телефоне
 * показала, что после покупки на экране не менялось ничего, хотя монеты
 * списывались. Требование ТЗ 2.5.9 — увидеть изменение после действия.
 */
@Composable
fun ScreenScaffold(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    message: FeedbackMessage? = null,
    onDismissMessage: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
      Box(modifier = Modifier.fillMaxSize()) {
        // Высота закреплённого отклика замеряется, а не задаётся числом:
        // при увеличенном шрифте карточка выше, и содержимое под ней
        // иначе осталось бы недоступным.
        var messageHeight by remember { mutableStateOf(0.dp) }
        val density = LocalDensity.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = MaxContentWidth)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    // Запас снизу нужен, только пока отклик показан:
                    // замеренная высота сама не обнуляется, когда карточка
                    // исчезает, и внизу оставалась бы пустая полоса.
                    .padding(bottom = 24.dp + if (message != null) messageHeight else 0.dp),
            ) {
                if (onBack != null) {
                    val decor = LocalAppliqueDecor.current
                    val backInteraction = remember { MutableInteractionSource() }
                    val backPressed by backInteraction.collectIsPressedAsState()
                    val backShape = MaterialTheme.shapes.small

                    OutlinedButton(
                        onClick = onBack,
                        interactionSource = backInteraction,
                        shape = backShape,
                        border = null,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .heightIn(min = MinTouchTarget)
                            .applique(shape = backShape, decor = decor, pressed = backPressed),
                    ) {
                        Text("Назад", style = MaterialTheme.typography.labelLarge)
                    }
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 12.dp, bottom = 18.dp),
                )

                content()
            }
        }

        if (message != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .onSizeChanged { size ->
                        messageHeight = with(density) { size.height.toDp() }
                    },
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = MaxContentWidth)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp),
                ) {
                    FeedbackCard(message = message, onDismiss = onDismissMessage)
                }
            }
        }
      }
    }
}

/** Крупная основная кнопка. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val decor = LocalAppliqueDecor.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = MaterialTheme.shapes.small

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaction,
        // Кнопка Material 3 по умолчанию скругляется полностью и тему
        // не читает, поэтому форма передаётся явно.
        shape = shape,
        // Собственная тень Material убирается: в направлении 1b её роль
        // играет сплошное смещение.
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
        ),
        colors = ButtonDefaults.buttonColors(
            disabledContainerColor = decor.disabledContainer,
            disabledContentColor = decor.disabledContent,
        ),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MinTouchTarget)
            .applique(shape = shape, decor = decor, pressed = pressed, enabled = enabled),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Вторичная кнопка того же размера. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    /**
     * Пиктограмма перед подписью. Ничего не заменяет: подпись читается
     * сама по себе, рисунок помогает узнать раздел в списке однотипных
     * кнопок (ТЗ 3.6). Программе чтения с экрана пиктограмма не мешает —
     * собственного описания у неё нет, произносится подпись.
     */
    icon: (@Composable () -> Unit)? = null,
) {
    val decor = LocalAppliqueDecor.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = MaterialTheme.shapes.small

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaction,
        shape = shape,
        // Контур рисуется оформлением, собственная рамка не нужна.
        border = null,
        // Заливка непрозрачная: у OutlinedButton фон по умолчанию
        // прозрачный, и смещённая тень просвечивала бы сквозь кнопку.
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = decor.disabledContainer,
            disabledContentColor = decor.disabledContent,
        ),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MinTouchTarget)
            .applique(shape = shape, decor = decor, pressed = pressed, enabled = enabled),
    ) {
        if (icon == null) {
            Text(text, style = MaterialTheme.typography.labelLarge)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon()
                Spacer(modifier = Modifier.width(10.dp))
                Text(text, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/**
 * Кнопка выбора одного варианта из нескольких.
 *
 * Выбранное помечается словом, а не только заливкой: состояние обязано
 * читаться текстом (ТЗ 3.6). Слово подставляется по месту — в гардеробе
 * это «надето», в остальных случаях «выбрано».
 */
@Composable
fun SelectButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selectedSuffix: String = "выбрано",
) {
    val label = if (selected) "$text — $selectedSuffix" else text

    if (selected) {
        PrimaryButton(text = label, onClick = onClick, enabled = enabled, modifier = modifier)
    } else {
        SecondaryButton(text = label, onClick = onClick, enabled = enabled, modifier = modifier)
    }
}

/** Карточка с заголовком и содержимым. */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    /**
     * Пиктограмма слева от заголовка. Ничего не заменяет: раздел назван
     * словом, а рисунок помогает его узнать (ТЗ 3.6).
     */
    icon: (@Composable () -> Unit)? = null,
    /**
     * Цветная кромка по левому краю карточки: обозначение учебной
     * категории. Как и пиктограмма, ничего не заменяет — категория
     * названа словом в заголовке или подписи рядом (ТЗ 3.6).
     */
    edgeColor: Color? = null,
    content: @Composable () -> Unit,
) {
    val shape = MaterialTheme.shapes.medium

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp)
            .applique(shape = shape, decor = LocalAppliqueDecor.current),
        shape = shape,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        // Высота ряда берётся по содержимому, иначе кромка растянула бы
        // карточку на всю доступную высоту.
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            if (edgeColor != null) {
                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .fillMaxHeight()
                        .background(edgeColor),
                )
            }
            Column(modifier = Modifier.padding(16.dp)) {
                if (title != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp),
                    ) {
                        if (icon != null) {
                            Box(modifier = Modifier.padding(end = 8.dp)) { icon() }
                        }
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                content()
            }
        }
    }
}

/**
 * Сообщение обратной связи. Затруднение помечается словом и знаком,
 * а не только цветом (ТЗ 3.6).
 */
@Composable
fun FeedbackCard(
    message: FeedbackMessage,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp)
            .applique(shape = MaterialTheme.shapes.medium, decor = LocalAppliqueDecor.current),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (message.isProblem) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (message.isProblem) "Внимание" else "Что произошло",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (message.nextStep != null) {
                Text(
                    text = "Что дальше: ${message.nextStep}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            SecondaryButton(
                text = "Понятно",
                onClick = onDismiss,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

/**
 * Поле ввода в оформлении направления 1b.
 *
 * Контур тот же, что у карточек и кнопок, а смещённой тени нет: поле
 * лежит на странице, а не приподнято над ней. Собственная рамка
 * Material убрана — иначе их было бы две.
 */
@Composable
fun AppliqueTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val decor = LocalAppliqueDecor.current
    val shape = MaterialTheme.shapes.small

    // Подпись стоит над полем, а не внутри него. Плавающая подпись Material
    // при вводе уезжает на рамку и разрывает её: свою «ступеньку» Material
    // вырезает только в собственной рамке, а здесь рамка рисуется
    // оформлением поверх. Подпись сверху к тому же совпадает с остальными
    // экранами — над ползунками и списками подписи тоже сверху.
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            shape = shape,
            keyboardOptions = keyboardOptions,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MinTouchTarget)
                .border(width = decor.strokeWidth, color = decor.ink, shape = shape),
        )
    }
}

/**
 * Диалог в оформлении направления 1b.
 *
 * Собран из тех же частей, что карточки и кнопки, а не взят готовым:
 * `AlertDialog` приносит своё скругление, свои отступы и текстовые
 * кнопки без контура, и на фоне остальных экранов выглядел бы чужим.
 *
 * Затемнение под диалогом рисует система; здесь — только сама карточка.
 *
 * @param actions кнопки внизу. Передаются целиком, потому что их число
 * и расположение зависят от вопроса: в одном случае две в строке,
 * в другом две в столбец.
 */
@Composable
fun AppliqueDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
    actions: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.medium

    Dialog(onDismissRequest = onDismiss) {
        // Цвет текста задаётся явно. Диалог вызывается вне каркаса экрана,
        // то есть вне Surface, который обычно его и назначает; без этого
        // текст берёт значение по умолчанию — чёрный, и на тёмной паре
        // оказывается чёрным по тёмному.
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .applique(shape = shape, decor = LocalAppliqueDecor.current)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(20.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                content()
                Spacer(modifier = Modifier.height(16.dp))
                actions()
            }
        }
    }
}

/**
 * Строка «подпись — значение» для показа сумм.
 *
 * Когда подпись и значение перестают помещаться рядом, значение уходит
 * на вторую строку целиком, а не сжимает подпись. При увеличении шрифта
 * до 1,5× подпись в узкой колонке разрывалась бы на три строки, и пара
 * переставала читаться как одна запись.
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
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
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
    Column(
        modifier = modifier
            .padding(vertical = 6.dp)
            // Программа чтения с экрана озвучивает показатель одной фразой,
            // а не набором разрозненных фрагментов.
            .semantics(mergeDescendants = true) {
                contentDescription = "$name: $label, $value из 100"
            },
    ) {
        // Значение уходит на вторую строку целиком, когда перестаёт
        // помещаться рядом с подписью, — так же, как в «подпись — значение».
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            maxItemsInEachRow = 2,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Пиктограмма только помогает узнать уровень: он уже назван
                // словом в подписи рядом и озвучивается программой чтения.
                StatLevelIcon(level = level, modifier = Modifier.padding(end = 8.dp))
                Text("$name: $label", style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                "$value из 100",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        ProgressBar(
            fraction = value / 100f,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/**
 * Ползунок распределения монет.
 *
 * Дорожка и ручка собраны вручную и переданы `Slider` слотами: по
 * умолчанию Material красит незаполненную часть своей палитрой, не
 * связанной с нашей схемой, и контура у дорожки нет вовсе.
 *
 * Сам `Slider` остаётся — он приносит жесты, шаг и озвучивание для
 * программы чтения с экрана. Слоты отмечены экспериментальными: другого
 * способа задать своё оформление, сохранив это, нет. Версия Compose
 * закреплена в каталоге версий, смена API отследится сборкой.
 *
 * @param color цвет заполненной части и ручки. На экране плана это цвет
 * направления, в задании — основной цвет схемы.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinSlider(
    value: Int,
    max: Int,
    color: Color,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (max <= 0) return

    val fraction = (value.toFloat() / max).coerceIn(0f, 1f)

    Slider(
        value = value.toFloat(),
        onValueChange = { onChange(it.roundToInt()) },
        valueRange = 0f..max.toFloat(),
        // Шаг в одну монету: доли монеты не существует, и ручка
        // не должна останавливаться между значениями.
        steps = (max - 1).coerceAtLeast(0),
        modifier = modifier.heightIn(min = MinTouchTarget),
        thumb = { CoinSliderThumb(color) },
        track = { CoinSliderTrack(fraction = fraction, color = color) },
    )
}

/** Дорожка ползунка: контур 1b, незаполненная часть — чернила с прозрачностью. */
@Composable
private fun CoinSliderTrack(fraction: Float, color: Color) {
    val ink = LocalAppliqueDecor.current.ink
    val shape = RoundedCornerShape(7.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(shape)
            .background(ink.copy(alpha = 0.25f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .fillMaxHeight()
                .background(color),
        )
        // Контур поверх заливок отдельным слоем: иначе заполненная часть
        // закрашивает его внутреннюю половину и линия выходит тоньше.
        Box(modifier = Modifier.matchParentSize().border(1.5.dp, ink, shape))
    }
}

/**
 * Ручка ползунка: 28 dp видимого размера при зоне нажатия 48 dp.
 * Крупнее дорожка становится толще контура карточки.
 */
@Composable
private fun CoinSliderThumb(color: Color) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(color, CircleShape)
            .border(2.5.dp, LocalAppliqueDecor.current.ink, CircleShape),
    )
}

/** Простая полоса заполнения без зависимостей от версии библиотеки. */
@Composable
fun ProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val shape = RoundedCornerShape(6.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 12.dp)
            // Контур у полосы: её край виден и тогда, когда заполнение
            // почти пустое, а подложка сливается с фоном карточки.
            .border(width = 1.5.dp, color = LocalAppliqueDecor.current.ink, shape = shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.background),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .sizeIn(minHeight = 12.dp)
                .clip(shape)
                .background(color),
        )
    }
}

/**
 * Изображение питомца. На текущем этапе это цветная фигура с подписью;
 * финальная графика добавляется позже.
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
     * «Домика-палатки» и остаётся навсегда: это вещь, а не состояние,
     * поэтому подписи рядом с ней нет — покупка уже подписана в каталоге.
     */
    house: Boolean = false,
    size: androidx.compose.ui.unit.Dp = 140.dp,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val base = parseColor(colorHex)
        // Контур фигуры берётся у пары, а не у оформления карточек:
        // чёрно-белому режиму рисунок не подчиняется по тем же причинам,
        // что и сцена, — сплошные чернила превращают его в пятно.
        val petInk = LocalSceneColors.current.ink

        // Фигура озвучивается одной фразой, а не набором фрагментов: для
        // программы чтения с экрана это один объект.
        val describePet = Modifier.semantics(mergeDescendants = true) {
            contentDescription = "$speciesTitle, $accessoryTitle"
        }

        if (scene) {
            // Питомец стоит внутри сцены, а не поверх неё: дом занимает
            // левую часть, дерево середину, питомец правую. Отдельной
            // фигуры при этом нет — иначе дерево оказывалось бы за ней.
            val scene = LocalSceneColors.current
            val night = LocalNightScene.current
            // В альбомной ориентации высота экрана мала, и сцена берётся
            // окном без верхней полосы неба: фигуры остаются прежними.
            val landscape = LocalConfiguration.current.orientation ==
                Configuration.ORIENTATION_LANDSCAPE
            val topCrop = if (landscape) SCENE_TOP_CROP_LANDSCAPE else 0f

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(sceneAspect(topCrop))
                    .applique(
                        shape = MaterialTheme.shapes.medium,
                        decor = LocalAppliqueDecor.current,
                    )
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.background)
                    .then(describePet),
            ) {
                drawScene(
                    primary = scene.primary,
                    secondary = scene.secondary,
                    surface = scene.surface,
                    onBackground = scene.onBackground,
                    ink = scene.ink,
                    night = night,
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
                        outlineColor = scene.ink,
                    )
                }
            }
        } else {
            // Вне сцены фигура стоит в рамке того же оформления, что
            // и карточки: без неё она висит на бумаге без опоры.
            val petShape = MaterialTheme.shapes.extraLarge

            Box(
                modifier = Modifier
                    .size(size)
                    .applique(shape = petShape, decor = LocalAppliqueDecor.current)
                    .clip(petShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .then(describePet),
                contentAlignment = Alignment.Center,
            ) {
                // Доля 118 к 140 взята из канвы: фигура не упирается
                // в рамку.
                Canvas(modifier = Modifier.size(size * 0.84f)) {
                    drawPet(
                        speciesId = speciesId,
                        baseColor = base,
                        accessoryId = accessoryId,
                        stage = stage,
                        care = care,
                        joy = joy,
                        outlineColor = petInk,
                    )
                }
            }
        }
        Text(
            text = petName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = "${stage.displayName}, $accessoryTitle".lowercase()
                .replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/** Переводит цвет вида «#RRGGBB» в значение Compose. */
fun parseColor(hex: String): Color =
    runCatching { Color(("FF" + hex.removePrefix("#")).toLong(16)) }
        .getOrElse { Color(0xFFCCCCCC) }
