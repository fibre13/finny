package ru.onefortwo.finny.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import ru.onefortwo.finny.content.PetAppearance
import ru.onefortwo.finny.content.PetPartsContent
import ru.onefortwo.finny.economy.Difficulty
import ru.onefortwo.finny.economy.GrowthStage
import ru.onefortwo.finny.ui.common.AdaptiveGrid
import ru.onefortwo.finny.ui.common.CardTone
import ru.onefortwo.finny.ui.common.ColorSwatch
import ru.onefortwo.finny.ui.common.FinnyTextField
import ru.onefortwo.finny.ui.common.LabeledValue
import ru.onefortwo.finny.ui.common.MinTouchTarget
import ru.onefortwo.finny.ui.common.OptionTile
import ru.onefortwo.finny.ui.common.PetFigure
import ru.onefortwo.finny.ui.common.PrimaryButton
import ru.onefortwo.finny.ui.common.ScreenScaffold
import ru.onefortwo.finny.ui.common.SectionCard
import ru.onefortwo.finny.ui.common.SelectButton
import ru.onefortwo.finny.ui.common.SupportingText
import ru.onefortwo.finny.ui.common.parseColor
import ru.onefortwo.finny.ui.theme.FinnyTheme

/** Предел длины игрового имени. */
private const val NAME_MAX_LENGTH = 12

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

    ScreenScaffold(
        title = title,
        onBack = back,
        top = { StepProgress(step = step) },
        // Каждый шаг открывается сверху, а не с той же прокрутки, где
        // остался предыдущий.
        scrollKey = step,
    ) {
        Column {
            SupportingText(
                text = when (step) {
                    2 -> "Выбери, каким будет твой новый друг."
                    3 -> "Выбери задания и придумай имя питомцу."
                    else -> "Проверь выбор — и начинайте первый день вместе."
                },
                modifier = Modifier.padding(bottom = 16.dp),
            )

            SectionCard(tone = CardTone.Sage) {
                PetFigure(
                    petName = name.ifBlank { "Пока без имени" },
                    speciesId = species.id,
                    speciesTitle = species.title,
                    accessoryId = accessory.id,
                    accessoryTitle = accessory.title,
                    colorHex = color.hex,
                    // Профиля ещё нет: питомец показывается на начальной
                    // стадии и в спокойном состоянии.
                    stage = GrowthStage.BABY,
                    caption = step == 4,
                )
            }

            when (step) {
                2 -> AppearanceStep(
                    parts = parts,
                    speciesId = speciesId,
                    colorId = colorId,
                    accessoryId = accessoryId,
                    onSpecies = { speciesId = it },
                    onColor = { colorId = it },
                    onAccessory = { accessoryId = it },
                    onNext = { step = 3 },
                )

                3 -> NameStep(
                    difficulty = difficulty,
                    onDifficulty = { difficultyName = it.name },
                    name = name,
                    onName = { if (it.length <= NAME_MAX_LENGTH) name = it },
                    onNext = { step = 4 },
                )

                else -> ConfirmStep(
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

/** Шаг 2: вид, окрас и украшение. */
@Composable
private fun AppearanceStep(
    parts: PetPartsContent,
    speciesId: String,
    colorId: String,
    accessoryId: String,
    onSpecies: (String) -> Unit,
    onColor: (String) -> Unit,
    onAccessory: (String) -> Unit,
    onNext: () -> Unit,
) {
    Column {
        GroupTitle("Кто это")
        AdaptiveGrid(count = parts.species.size, minItemWidth = 140.dp) { index ->
            val option = parts.species[index]
            OptionTile(
                title = option.title,
                selected = option.id == speciesId,
                onClick = { onSpecies(option.id) },
                modifier = Modifier.weight(1f),
            )
        }

        GroupTitle("Какого цвета")
        AdaptiveGrid(count = parts.colors.size, minItemWidth = 100.dp) { index ->
            val option = parts.colors[index]
            OptionTile(
                title = option.title,
                selected = option.id == colorId,
                onClick = { onColor(option.id) },
                swatch = { ColorSwatch(parseColor(option.hex)) },
                modifier = Modifier.weight(1f),
            )
        }

        GroupTitle("Украшение")
        AdaptiveGrid(count = parts.accessories.size, minItemWidth = 140.dp) { index ->
            val option = parts.accessories[index]
            OptionTile(
                title = option.title,
                selected = option.id == accessoryId,
                onClick = { onAccessory(option.id) },
                modifier = Modifier.weight(1f),
            )
        }

        PrimaryButton(
            text = "Выбрать",
            onClick = onNext,
            modifier = Modifier.padding(top = 20.dp),
        )
    }
}

/** Шаг 3: уровень заданий и игровое имя. */
@Composable
private fun NameStep(
    difficulty: Difficulty?,
    onDifficulty: (Difficulty) -> Unit,
    name: String,
    onName: (String) -> Unit,
    onNext: () -> Unit,
) {
    Column {
        GroupTitle("Какие задания тебе по силам")
        SupportingText(
            text = "Выбери, с каких начать. Это можно поменять потом — " +
                "ничего о тебе мы не спрашиваем.",
            modifier = Modifier.padding(bottom = 10.dp),
        )
        Difficulty.entries.forEach { option ->
            SelectButton(
                text = option.displayName,
                supporting = option.hint,
                selected = option == difficulty,
                onClick = { onDifficulty(option) },
                modifier = Modifier.padding(bottom = 10.dp),
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
            supportingText = "Придумай любое игровое имя. Настоящее имя писать не нужно.",
            modifier = Modifier.padding(top = 12.dp),
        )

        val ready = difficulty != null && name.isNotBlank()
        PrimaryButton(
            text = "Проверить выбор",
            enabled = ready,
            onClick = onNext,
            modifier = Modifier.padding(top = 20.dp),
        )

        // Причина недоступности названа текстом: по одному виду кнопки
        // непонятно, чего она ждёт.
        if (!ready) {
            SupportingText(
                text = when {
                    difficulty == null && name.isBlank() ->
                        "Выбери задания и придумай имя, и кнопка станет доступной."
                    difficulty == null -> "Выбери сложность, и кнопка станет доступной."
                    else -> "Придумай имя, и кнопка станет доступной."
                },
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/** Шаг 4: проверка выбора и начало первого дня. */
@Composable
private fun ConfirmStep(
    speciesTitle: String,
    colorTitle: String,
    accessoryTitle: String,
    difficulty: Difficulty,
    name: String,
    onEdit: () -> Unit,
    onStart: () -> Unit,
) {
    Column {
        SectionCard(
            title = "Твой выбор",
            trailing = {
                Text(
                    text = "Изменить",
                    style = MaterialTheme.typography.titleMedium,
                    color = FinnyTheme.colors.attentionText,
                    modifier = Modifier
                        .heightIn(min = MinTouchTarget)
                        .clickable(role = Role.Button, onClick = onEdit)
                        .padding(horizontal = 4.dp, vertical = 12.dp),
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
        ) {
            SupportingText("Ты получишь первые монеты и решишь, что купить.")
        }

        PrimaryButton(text = "Начать первый день", onClick = onStart)
    }
}

@Composable
private fun GroupTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = 8.dp, bottom = 10.dp),
    )
}
