package ru.onefortwo.finny.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ru.onefortwo.finny.content.PetPartsContent
import ru.onefortwo.finny.ui.common.PetFigure
import ru.onefortwo.finny.ui.common.PrimaryButton
import ru.onefortwo.finny.ui.common.ScreenScaffold
import ru.onefortwo.finny.ui.common.SecondaryButton
import ru.onefortwo.finny.ui.common.SectionCard
import ru.onefortwo.finny.ui.common.parseColor
import ru.onefortwo.finny.ui.state.AppState
import ru.onefortwo.finny.ui.theme.LocalAppliqueDecor

/**
 * Гардероб питомца.
 *
 * Меняются окрас и украшение; вид питомца и его имя остаются прежними —
 * это опознание питомца, а не украшение. Украшение можно надеть, только
 * если оно куплено в каталоге, поэтому гардероб остаётся следствием
 * финансовых решений, а не наградой за время в приложении.
 */
@Composable
fun WardrobeScreen(
    state: AppState,
    parts: PetPartsContent,
    onApply: (String, String) -> Unit,
    onOpenShop: () -> Unit,
    onBack: () -> Unit,
) {
    val profile = state.profile ?: return

    var colorId by rememberSaveable { mutableStateOf(profile.appearance.colorId) }
    var accessoryId by rememberSaveable { mutableStateOf(profile.appearance.accessoryId) }

    val species = parts.species.firstOrNull { it.id == profile.appearance.speciesId }
    val color = parts.colors.firstOrNull { it.id == colorId }
    val accessory = parts.accessories.firstOrNull { it.id == accessoryId }
    val locked = parts.accessories.filterNot { state.isAccessoryAvailable(it.id) }

    ScreenScaffold(title = "Гардероб", onBack = onBack) {
        Column {
            PetFigure(
                petName = profile.petName,
                speciesId = profile.appearance.speciesId,
                speciesTitle = species?.title ?: "Питомец",
                accessoryId = accessoryId,
                accessoryTitle = accessory?.title ?: "без украшения",
                colorHex = color?.hex ?: "#CCCCCC",
                stage = state.game.stage,
                care = state.game.pet.care.level,
                joy = state.game.pet.joy.level,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            SectionCard(title = "Окрас") {
                Column {
                    parts.colors.forEach { option ->
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
                            WardrobeButton(
                                text = option.title,
                                selected = option.id == colorId,
                                enabled = true,
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
                Column {
                    parts.accessories.forEach { option ->
                        val available = state.isAccessoryAvailable(option.id)
                        WardrobeButton(
                            text = if (available) option.title else "${option.title} — не куплено",
                            selected = option.id == accessoryId,
                            enabled = available,
                            onClick = { accessoryId = option.id },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                        )
                    }

                    if (locked.isNotEmpty()) {
                        Text(
                            text = "Украшения появляются здесь после покупки в разделе «Покупки».",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }

            PrimaryButton(
                text = "Надеть",
                onClick = { onApply(colorId, accessoryId) },
                modifier = Modifier.padding(bottom = 8.dp),
            )

            // Кнопка показывается, только когда есть некупленное
            // украшение: иначе идти в покупки незачем.
            if (locked.isNotEmpty()) {
                SecondaryButton(text = "Перейти в покупки", onClick = onOpenShop)
            }
        }
    }
}

/**
 * Кнопка выбора в гардеробе. Выбранное помечается словом, недоступное —
 * подписью «не куплено», а не только цветом (ТЗ 3.6).
 */
@Composable
private fun WardrobeButton(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = if (selected) "$text — надето" else text

    if (selected) {
        PrimaryButton(text = label, onClick = onClick, enabled = enabled, modifier = modifier)
    } else {
        SecondaryButton(text = label, onClick = onClick, enabled = enabled, modifier = modifier)
    }
}
