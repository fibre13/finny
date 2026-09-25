package ru.onefortwo.finny.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.onefortwo.finny.content.PetAppearance
import ru.onefortwo.finny.content.PetPartsContent
import ru.onefortwo.finny.economy.Difficulty
import ru.onefortwo.finny.economy.GrowthStage
import ru.onefortwo.finny.ui.common.AdaptiveGrid
import ru.onefortwo.finny.ui.common.CardTone
import ru.onefortwo.finny.ui.common.CheckOption
import ru.onefortwo.finny.ui.common.ColorSwatch
import ru.onefortwo.finny.ui.common.FinnyTextField
import ru.onefortwo.finny.ui.common.LabeledValue
import ru.onefortwo.finny.ui.common.MinTouchTarget
import ru.onefortwo.finny.ui.common.PetFigure
import ru.onefortwo.finny.ui.common.PrimaryButton
import ru.onefortwo.finny.ui.common.ScreenScaffold
import ru.onefortwo.finny.ui.common.SectionCard
import ru.onefortwo.finny.ui.common.SelectButton
import ru.onefortwo.finny.ui.common.SupportingText
import ru.onefortwo.finny.ui.common.parseColor
import ru.onefortwo.finny.ui.common.petCaption
import ru.onefortwo.finny.ui.theme.FinnyTheme

/** Предел длины игрового имени. */
private const val NAME_MAX_LENGTH = 12

/** Масштаб шрифта, с которого варианты выстраиваются сеткой, а не в строку. */
private const val LARGE_FONT_SCALE = 1.3f

/**
 * Шаги 2–4 знакомства: внешность питомца, уровень заданий с игровым
 * именем и проверка выбора перед началом (ТЗ 2.5.2, 2.5.8).
 *
 * Выбор держится на этом экране и переживает поворот; кнопка «Назад»
 * и системный жест возвращают на предыдущий шаг, а не сбрасывают выбор.
 *
 * Реальное имя, телефон и почта не запрашиваются: профиль остаётся
 * локальным и обезличенным (ТЗ 3.5, Приложение А шаг 2). Спрашивается
 * уровень заданий, а не класс и не возраст: для подбора заданий этого
 * достаточно, и о ребёнке ничего не собирается.
 *
 * На телефоне 360 × 800 dp при обычном шрифте каждый шаг помещается
 * целиком вместе с кнопкой; при крупном шрифте экран прокручивается,
 * а варианты выстраиваются сеткой.
 */
@Composable
fun PetSetupScreen(
    parts: PetPartsContent,
    onDone: (String, PetAppearance, Difficulty) -> Unit,
    onBack: (() -> Unit)? = null,
) {
    var step by rememberSaveable { mutableIntStateOf(2) }
    var speciesId by rememberSaveable { mutableStateOf(parts.species.first().id) }
    var colorId by rememberSaveable { mutableStateOf(parts.colors.first().id) }
    var accessoryId by rememberSaveable { mutableStateOf(parts.accessories.first().id) }
    var name by rememberSaveable { mutableStateOf("") }
    var difficultyName by rememberSaveable { mutableStateOf<String?>(null) }
    val difficulty = difficultyName?.let(Difficulty::ofName)

    val species = parts.species.first { it.id == speciesId }
    val color = parts.colors.first { it.id == colorId }
    val accessory = parts.accessories.first { it.id == accessoryId }

    // Системный жест «назад» на шагах 3 и 4 возвращает на шаг раньше.
    BackHandler(enabled = step > 2) { step -= 1 }
    val back: (() -> Unit)? = if (step > 2) ({ step -= 1 }) else onBack

    val title = when (step) {
        2 -> "Твой питомец"
        3 -> "Почти готово"
        else -> "Всё готово!"
    }

    // Профиля ещё нет: питомец показывается на начальной стадии и
    // в спокойном состоянии.
    val figure: @Composable (Dp) -> Unit = { size ->
        PetFigure(
            petName = name.ifBlank { "Пока без имени" },
            speciesId = species.id,
            speciesTitle = species.title,
            accessoryId = accessory.id,
            accessoryTitle = accessory.title,
            colorHex = color.hex,
            stage = GrowthStage.BABY,
            size = size,
            caption = false,
            modifier = Modifier.width(size),
        )
    }

    ScreenScaffold(
        title = title,
        onBack = back,
        top = { StepProgress(step = step) },
        // Каждый шаг открывается сверху, а не с той же прокрутки, где
        // остался предыдущий.
        scrollKey = step,
        bottomPadding = 16.dp,
        centerOnTablet = true,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when (step) {
                2 -> AppearanceStep(
                    parts = parts,
                    speciesId = speciesId,
                    colorId = colorId,
                    accessoryId = accessoryId,
                    figure = figure,
                    onSpecies = { speciesId = it },
                    onColor = { colorId = it },
                    onAccessory = { accessoryId = it },
                    onNext = { step = 3 },
                )

                3 -> NameStep(
                    preview = {
                        PreviewRow(
                            figure = { figure(64.dp) },
                            name = name.trim(),
                            caption = "${species.title} · ${accessory.title.lowercase()}",
                            large = false,
                        )
                    },
                    difficulty = difficulty,
                    onDifficulty = { difficultyName = it.name },
                    name = name,
                    onName = { if (it.length <= NAME_MAX_LENGTH) name = it },
                    onNext = { step = 4 },
                )

                else -> ConfirmStep(
                    preview = {
                        PreviewRow(
                            figure = { figure(104.dp) },
                            name = name.trim(),
                            caption = petCaption(GrowthStage.BABY, accessory.title),
                            large = true,
                        )
                    },
                    speciesTitle = species.title,
                    colorTitle = color.title,
                    accessoryTitle = accessory.title,
                    difficulty = difficulty ?: Difficulty.SIMPLE,
                    name = name,
                    onEdit = { step = 2 },
                    onStart = {
                        onDone(
                            name.trim(),
                            PetAppearance(speciesId, colorId, accessoryId),
                            difficulty ?: Difficulty.SIMPLE,
                        )
                    },
                )
            }
        }
    }
}

/**
 * Шаг 2: вид, окрас и украшение.
 *
 * Вид выбирается списком справа от превью: так превью, вид, окрас и
 * украшение помещаются на один экран. При крупном шрифте превью встаёт
 * над вариантами, а варианты — в сетку, чтобы подписи не рвались.
 */
@Composable
private fun AppearanceStep(
    parts: PetPartsContent,
    speciesId: String,
    colorId: String,
    accessoryId: String,
    figure: @Composable (Dp) -> Unit,
    onSpecies: (String) -> Unit,
    onColor: (String) -> Unit,
    onAccessory: (String) -> Unit,
    onNext: () -> Unit,
) {
    val large = LocalDensity.current.fontScale >= LARGE_FONT_SCALE

    SupportingText("Выбери, каким будет твой новый друг.")

    if (large) {
        PreviewCard { figure(140.dp) }
        GroupTitle("Кто это")
        AdaptiveGrid(count = parts.species.size, minItemWidth = 140.dp) { index ->
            val option = parts.species[index]
            CheckOption(
                title = option.title,
                selected = option.id == speciesId,
                onClick = { onSpecies(option.id) },
                modifier = Modifier.weight(1f),
            )
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PreviewCard { figure(140.dp) }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                GroupTitle("Кто это")
                parts.species.forEach { option ->
                    CheckOption(
                        title = option.title,
                        selected = option.id == speciesId,
                        onClick = { onSpecies(option.id) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    GroupTitle("Какого цвета")
    OptionRow(
        count = parts.colors.size,
        large = large,
        minItemWidth = 100.dp,
    ) { index, modifier ->
        val option = parts.colors[index]
        CheckOption(
            title = option.title,
            selected = option.id == colorId,
            onClick = { onColor(option.id) },
            swatch = { ColorSwatch(parseColor(option.hex), size = 24.dp) },
            modifier = modifier.heightIn(min = 64.dp),
        )
    }

    GroupTitle("Украшение")
    OptionRow(
        count = parts.accessories.size,
        large = large,
        minItemWidth = 140.dp,
        // «Без украшения» — самая длинная подпись; плитка шире, чтобы
        // слово «украшения» не разрывалось.
        weights = parts.accessories.map { if (it.id == "none") 1.35f else 1f },
    ) { index, modifier ->
        val option = parts.accessories[index]
        CheckOption(
            title = option.title,
            selected = option.id == accessoryId,
            onClick = { onAccessory(option.id) },
            modifier = modifier.heightIn(min = 64.dp),
        )
    }

    PrimaryButton(text = "Выбрать", onClick = onNext, modifier = Modifier.padding(top = 4.dp))
}

/**
 * Ряд вариантов: в одну строку с заданными долями ширины, при крупном
 * шрифте — сетка, которая сама подбирает число колонок.
 */
@Composable
private fun OptionRow(
    count: Int,
    large: Boolean,
    minItemWidth: Dp,
    weights: List<Float> = List(count) { 1f },
    item: @Composable (index: Int, modifier: Modifier) -> Unit,
) {
    if (large) {
        AdaptiveGrid(count = count, minItemWidth = minItemWidth) { index ->
            item(index, Modifier.weight(1f))
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(count) { index -> item(index, Modifier.weight(weights[index])) }
        }
    }
}

/** Шаг 3: уровень заданий и игровое имя. */
@Composable
private fun NameStep(
    preview: @Composable () -> Unit,
    difficulty: Difficulty?,
    onDifficulty: (Difficulty) -> Unit,
    name: String,
    onName: (String) -> Unit,
    onNext: () -> Unit,
) {
    preview()

    GroupTitle("Какие задания тебе по силам")
    SupportingText("Можно поменять потом. О тебе мы ничего не спрашиваем.")
    Difficulty.entries.forEach { option ->
        SelectButton(
            text = option.displayName,
            supporting = option.hint,
            selected = option == difficulty,
            onClick = { onDifficulty(option) },
            compact = true,
        )
    }

    FinnyTextField(
        value = name,
        onValueChange = onName,
        label = "Как назовём",
        maxLength = NAME_MAX_LENGTH,
        // Имя — не слово словаря: автоисправление подменяло бы его.
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            autoCorrectEnabled = false,
        ),
        supportingText = "Придумай любое игровое имя. Настоящее писать не нужно.",
    )

    val ready = difficulty != null && name.isNotBlank()
    PrimaryButton(text = "Проверить выбор", enabled = ready, onClick = onNext)

    // Причина недоступности названа текстом: по одному виду кнопки
    // непонятно, чего она ждёт.
    if (!ready) {
        SupportingText(
            when {
                difficulty == null && name.isBlank() ->
                    "Выбери задания и придумай имя, и кнопка станет доступной."
                difficulty == null -> "Выбери сложность, и кнопка станет доступной."
                else -> "Придумай имя, и кнопка станет доступной."
            },
        )
    }
}

/** Шаг 4: проверка выбора и начало первого дня. */
@Composable
private fun ConfirmStep(
    preview: @Composable () -> Unit,
    speciesTitle: String,
    colorTitle: String,
    accessoryTitle: String,
    difficulty: Difficulty,
    name: String,
    onEdit: () -> Unit,
    onStart: () -> Unit,
) {
    SupportingText("Проверь выбор — и начинайте первый день вместе.")
    preview()

    SectionCard(
        title = "Твой выбор",
        contentPadding = PaddingValues(start = 16.dp, top = 6.dp, end = 16.dp, bottom = 10.dp),
        bottomSpacing = 0.dp,
        trailing = {
            Text(
                text = "Изменить",
                style = MaterialTheme.typography.labelLarge,
                color = FinnyTheme.colors.attentionText,
                modifier = Modifier
                    .heightIn(min = MinTouchTarget)
                    .clickable(role = Role.Button, onClick = onEdit)
                    .padding(horizontal = 4.dp, vertical = 13.dp),
            )
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            LabeledValue("Питомец", speciesTitle)
            LabeledValue("Цвет", colorTitle)
            LabeledValue("Украшение", accessoryTitle)
            LabeledValue("Задания", difficulty.displayName)
            LabeledValue("Имя", name.trim())
        }
    }

    SectionCard(
        tone = CardTone.Coin,
        eyebrow = "Первый день",
        title = "Познакомься с ${name.trim()}",
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        bottomSpacing = 0.dp,
    ) {
        SupportingText("Ты получишь первые монеты и решишь, что купить.")
    }

    PrimaryButton(text = "Начать первый день", onClick = onStart)
}

/** Превью питомца в салатовой карточке. */
@Composable
private fun PreviewCard(figure: @Composable () -> Unit) {
    SectionCard(
        tone = CardTone.Sage,
        contentPadding = PaddingValues(12.dp),
        bottomSpacing = 0.dp,
        modifier = Modifier.width(164.dp),
    ) {
        figure()
    }
}

/**
 * Превью с именем: фигура слева, имя и подпись справа. Имя обновляется
 * по мере ввода; пока его нет — «Пока без имени».
 */
@Composable
private fun PreviewRow(
    figure: @Composable () -> Unit,
    name: String,
    caption: String,
    large: Boolean,
) {
    SectionCard(
        tone = CardTone.Sage,
        contentPadding = PaddingValues(start = 10.dp, top = 10.dp, end = 12.dp, bottom = 10.dp),
        bottomSpacing = 0.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            figure()
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = name.ifBlank { "Пока без имени" },
                    style = if (large) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                    color = if (name.isBlank()) FinnyTheme.colors.onSurfaceMuted else FinnyTheme.colors.onSurface,
                )
                SupportingText(caption)
            }
        }
    }
}

@Composable
private fun GroupTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() },
    )
}
