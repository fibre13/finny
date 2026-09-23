package ru.onefortwo.finny.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ru.onefortwo.finny.content.PetAppearance
import ru.onefortwo.finny.content.PetPartsContent
import ru.onefortwo.finny.economy.GrowthStage
import ru.onefortwo.finny.ui.common.AppliqueTextField
import ru.onefortwo.finny.ui.common.MinTouchTarget
import ru.onefortwo.finny.ui.common.PetFigure
import ru.onefortwo.finny.ui.common.PrimaryButton
import ru.onefortwo.finny.ui.common.ScreenScaffold
import ru.onefortwo.finny.ui.common.SectionCard
import ru.onefortwo.finny.ui.common.SelectButton
import ru.onefortwo.finny.ui.common.parseColor
import ru.onefortwo.finny.ui.theme.LocalAppliqueDecor

/**
 * Создание питомца: внешний вид и игровое имя (ТЗ 2.5.2).
 *
 * Класс здесь не спрашивается: он выбран на экране знакомства до того,
 * как ребёнок дошёл до питомца. От него зависит сложность заданий,
 * и выбор обязан быть сделан раньше, чем начнётся игра.
 *
 * Реальное имя, телефон и почта не запрашиваются: профиль остаётся
 * локальным и обезличенным (ТЗ 3.5, Приложение А шаг 2).
 */
@Composable
fun PetSetupScreen(
    parts: PetPartsContent,
    onDone: (String, PetAppearance) -> Unit,
    onBack: (() -> Unit)? = null,
) {
    var speciesId by rememberSaveable { mutableStateOf(parts.species.first().id) }
    var colorId by rememberSaveable { mutableStateOf(parts.colors.first().id) }
    var accessoryId by rememberSaveable { mutableStateOf(parts.accessories.first().id) }
    var name by rememberSaveable { mutableStateOf("") }

    val species = parts.species.first { it.id == speciesId }
    val color = parts.colors.first { it.id == colorId }
    val accessory = parts.accessories.first { it.id == accessoryId }

    ScreenScaffold(title = "Твой питомец", onBack = onBack) {
        Column {
            PetFigure(
                petName = if (name.isBlank()) "Пока без имени" else name,
                speciesId = species.id,
                speciesTitle = species.title,
                accessoryId = accessory.id,
                accessoryTitle = accessory.title,
                colorHex = color.hex,
                // Профиля ещё нет: питомец показывается на начальной стадии
                // и в спокойном состоянии.
                stage = GrowthStage.BABY,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            SectionCard(title = "Кто это") {
                ChoiceRow(
                    options = parts.species.map { it.id to it.title },
                    selectedId = speciesId,
                    onSelect = { speciesId = it },
                )
            }

            SectionCard(title = "Какого цвета") {
                Column {
                    parts.colors.forEach { option ->
                        val selected = option.id == colorId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    // Контур у плитки: светлый окрас иначе
                                    // теряется на белой подложке карточки.
                                    .border(
                                        width = 1.5.dp,
                                        color = LocalAppliqueDecor.current.ink,
                                        shape = RoundedCornerShape(8.dp),
                                    )
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(parseColor(option.hex)),
                            )
                            SelectButton(
                                text = option.title,
                                selected = selected,
                                onClick = { colorId = option.id },
                                modifier = Modifier
                                    .padding(start = 12.dp)
                                    .fillMaxWidth(),
                            )
                        }
                    }
                }
            }

            SectionCard(title = "Украшение") {
                ChoiceRow(
                    options = parts.accessories.map { it.id to it.title },
                    selectedId = accessoryId,
                    onSelect = { accessoryId = it },
                )
            }

            SectionCard(title = "Как назовём") {
                Column {
                    AppliqueTextField(
                        value = name,
                        onValueChange = { if (it.length <= 12) name = it },
                        label = "Игровое имя",
                    )
                    Text(
                        text = "Придумай любое игровое имя. Настоящее имя писать не нужно.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            PrimaryButton(
                text = "Готово",
                enabled = name.isNotBlank(),
                onClick = { onDone(name, PetAppearance(speciesId, colorId, accessoryId)) },
            )

            if (name.isBlank()) {
                Text(
                    text = "Придумай имя, и кнопка станет доступной.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

/** Ряд вариантов выбора, переносится на новую строку при нехватке ширины. */
@Composable
private fun ChoiceRow(
    options: List<Pair<String, String>>,
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    Column {
        options.forEach { (id, title) ->
            SelectButton(
                text = title,
                selected = id == selectedId,
                onClick = { onSelect(id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            )
        }
    }
}

