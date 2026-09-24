package ru.onefortwo.finny.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ru.onefortwo.finny.content.PetPartsContent
import ru.onefortwo.finny.ui.common.AdaptiveGrid
import ru.onefortwo.finny.ui.common.CardTone
import ru.onefortwo.finny.ui.common.ColorSwatch
import ru.onefortwo.finny.ui.common.OptionTile
import ru.onefortwo.finny.ui.common.PetFigure
import ru.onefortwo.finny.ui.common.PrimaryButton
import ru.onefortwo.finny.ui.common.ScreenScaffold
import ru.onefortwo.finny.ui.common.SecondaryButton
import ru.onefortwo.finny.ui.common.SectionCard
import ru.onefortwo.finny.ui.common.SupportingText
import ru.onefortwo.finny.ui.common.parseColor
import ru.onefortwo.finny.ui.state.AppState

/**
 * Гардероб питомца. Открывается вкладкой «Питомец».
 *
 * Меняются окрас и украшение; вид питомца и его имя остаются прежними —
 * это опознание питомца, а не украшение. Украшение можно надеть, только
 * если оно куплено в каталоге, поэтому гардероб остаётся следствием
 * финансовых решений, а не наградой за время в приложении.
 *
 * Выбранное помечается словом «надето», недоступное — «не куплено»,
 * а не только цветом (ТЗ 3.6).
 *
 * @param onBack возврат; `null`, когда экран открыт вкладкой.
 */
@Composable
fun WardrobeScreen(
    state: AppState,
    parts: PetPartsContent,
    onApply: (String, String) -> Unit,
    onOpenShop: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    val profile = state.profile ?: return

    var colorId by rememberSaveable { mutableStateOf(profile.appearance.colorId) }
    var accessoryId by rememberSaveable { mutableStateOf(profile.appearance.accessoryId) }

    val species = parts.species.firstOrNull { it.id == profile.appearance.speciesId }
    val color = parts.colors.firstOrNull { it.id == colorId }
    val accessory = parts.accessories.firstOrNull { it.id == accessoryId }
    val locked = parts.accessories.filterNot { state.isAccessoryAvailable(it.id) }

    ScreenScaffold(
        eyebrow = "Наряды: ${profile.petName}",
        title = "Гардероб",
        balance = state.game.balance,
        onBack = onBack,
    ) {
        Column {
            SectionCard(tone = CardTone.Sage) {
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
                )
            }

            GroupTitle("Окрас")
            AdaptiveGrid(count = parts.colors.size, minItemWidth = 100.dp) { index ->
                val option = parts.colors[index]
                OptionTile(
                    title = option.title,
                    selected = option.id == colorId,
                    onClick = { colorId = option.id },
                    selectedSuffix = "надето",
                    status = "есть",
                    swatch = { ColorSwatch(parseColor(option.hex)) },
                    modifier = Modifier.weight(1f),
                )
            }

            GroupTitle("Украшение")
            AdaptiveGrid(count = parts.accessories.size, minItemWidth = 140.dp) { index ->
                val option = parts.accessories[index]
                val available = state.isAccessoryAvailable(option.id)
                OptionTile(
                    title = option.title,
                    selected = option.id == accessoryId,
                    enabled = available,
                    onClick = { accessoryId = option.id },
                    selectedSuffix = "надето",
                    status = if (available) "есть" else "не куплено",
                    modifier = Modifier.weight(1f),
                )
            }

            if (locked.isNotEmpty()) {
                SupportingText(
                    text = "Украшения появляются здесь после покупки в разделе «Покупки».",
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            PrimaryButton(
                text = "Надеть",
                onClick = { onApply(colorId, accessoryId) },
                modifier = Modifier.padding(top = 20.dp, bottom = 10.dp),
            )

            // Кнопка показывается, только когда есть некупленное
            // украшение: иначе идти в покупки незачем.
            if (locked.isNotEmpty()) {
                SecondaryButton(text = "Перейти в покупки", onClick = onOpenShop)
            }
        }
    }
}

@Composable
private fun GroupTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier
            .padding(top = 8.dp, bottom = 12.dp)
            .semantics { heading() },
    )
}
