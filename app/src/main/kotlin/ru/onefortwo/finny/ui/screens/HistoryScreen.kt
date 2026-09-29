package ru.onefortwo.finny.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.onefortwo.finny.content.Accessories
import ru.onefortwo.finny.content.PetPartsContent
import ru.onefortwo.finny.content.TaskContent
import ru.onefortwo.finny.economy.Coins
import ru.onefortwo.finny.economy.GameState
import ru.onefortwo.finny.economy.GrowthStage
import ru.onefortwo.finny.economy.StatLevel
import ru.onefortwo.finny.ui.common.CardTone
import ru.onefortwo.finny.ui.common.LabeledValue
import ru.onefortwo.finny.ui.common.PetFigure
import ru.onefortwo.finny.ui.common.PrimaryButton
import ru.onefortwo.finny.ui.common.ProgressBar
import ru.onefortwo.finny.ui.common.ScreenScaffold
import ru.onefortwo.finny.ui.common.SectionCard
import ru.onefortwo.finny.ui.common.SupportingText
import ru.onefortwo.finny.ui.common.rememberPixelArt
import ru.onefortwo.finny.ui.state.Explanations
import ru.onefortwo.finny.ui.state.Profile
import ru.onefortwo.finny.ui.theme.FinnyTheme

/**
 * История и учебный прогресс (ТЗ 2.5.11): ступень роста питомца — цепочкой
 * стадий и тем, сколько шагов роста осталось до следующей; прогресс по
 * текущей цели; полученные цели; сколько заданий решено.
 *
 * @param onBack возврат; `null`, когда экран открыт вкладкой.
 */
@Composable
fun HistoryScreen(
    game: GameState,
    tasks: List<TaskContent>,
    completedIds: Set<String>,
    goalTitle: String?,
    /** Названия уже полученных целей, в порядке достижения. */
    achievedGoalTitles: List<String>,
    onBack: (() -> Unit)? = null,
    petName: String = "Финни",
    balance: Coins? = null,
    /** Питомец ребёнка для цепочки стадий; без него цепочка не рисуется. */
    profile: Profile? = null,
    parts: PetPartsContent? = null,
    onOpenTasks: () -> Unit = {},
) {
    val colors = FinnyTheme.colors
    val stage = game.stage
    val next = GrowthStage.entries.firstOrNull { it.requiredPoints > game.growthPoints }

    ScreenScaffold(
        title = "Мой прогресс",
        balance = balance,
        onBack = onBack,
    ) {
        Column {
            SectionCard(
                eyebrow = "Ступень роста",
                title = "$petName — ${stage.displayName.lowercase()}",
                tone = CardTone.Sage,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (profile != null && parts != null) StageChain(profile, parts, stage)
                    LabeledValue("Прожито дней", "${game.history.size}")
                    if (next != null) {
                        val left = next.requiredPoints - game.growthPoints
                        val who = if (next == GrowthStage.TEEN) "подростком" else "взрослым"
                        Text(
                            text = "Ещё ${steps(left)} — и $petName станет $who.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        ProgressBar(
                            fraction = game.growthPoints.toFloat() / next.requiredPoints,
                            color = colors.successText,
                            height = 8.dp,
                        )
                        SupportingText(
                            "Шаг роста дают за день, в котором ты купил нужное, уложился в план " +
                                "или пополнил копилку — до трёх шагов за день.",
                        )
                    } else {
                        SupportingText("$petName совсем взрослый!")
                    }
                }
            }

            val goal = game.savings.goal
            if (goal == null) {
                SectionCard(title = "Цель", tone = CardTone.Primary) {
                    Text("Цель пока не выбрана.", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                val percent = (game.savings.saved.amount * 100 / goal.price.amount.coerceAtLeast(1))
                    .coerceIn(0, 100)
                SectionCard(
                    title = "Копим на: ${goalTitle ?: "цель"}",
                    tone = CardTone.Primary,
                    trailing = {
                        Text(
                            text = "$percent%",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.coin,
                        )
                    },
                ) {
                    Column {
                        ProgressBar(
                            fraction = game.savings.saved.amount.toFloat() / goal.price.amount,
                            color = colors.coin,
                            trackColor = colors.onPrimary.copy(alpha = 0.22f),
                            modifier = Modifier.padding(bottom = 10.dp),
                        )
                        LabeledValue("Накоплено", Explanations.coins(game.savings.saved))
                        LabeledValue("Осталось", Explanations.coins(game.savings.remaining))
                        SupportingText(
                            text = Explanations.forecast(game.goalForecast()),
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }

            if (achievedGoalTitles.isNotEmpty()) {
                SectionCard(title = "Накоплено и получено", tone = CardTone.Coin) {
                    Column {
                        achievedGoalTitles.forEach { title ->
                            Text("✓ $title", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            val solved = tasks.count { it.id in completedIds }
            SectionCard(title = "Задания: ты решил уже $solved из ${tasks.size}") {
                PrimaryButton(text = "К заданиям →", onClick = onOpenTasks)
            }
        }
    }
}

/** Склонение слова «шаг» для числа. */
private fun steps(count: Int): String {
    val tail = count % 100
    val last = count % 10
    val word = when {
        tail in 11..14 -> "шагов"
        last == 1 -> "шаг"
        last in 2..4 -> "шага"
        else -> "шагов"
    }
    return "$count $word"
}

/** Цепочка стадий: пройденные — с галочкой, текущая — в рамке, будущие — бледно. */
@Composable
private fun StageChain(profile: Profile, parts: PetPartsContent, current: GrowthStage) {
    val art = rememberPixelArt()
    val yard = remember(art) { YardColors(art) }
    val species = parts.species.firstOrNull { it.id == profile.appearance.speciesId }
    val color = parts.colors.firstOrNull { it.id == profile.appearance.colorId }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "Стадии: малыш, подросток, взрослый. Сейчас — ${current.displayName.lowercase()}"
            },
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom,
    ) {
        GrowthStage.entries.forEach { s ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clearAndSetSemantics { }.alpha(if (s <= current) 1f else 0.35f),
            ) {
                Box(
                    modifier = if (s == current) {
                        Modifier.pixelPanel(yard.card, yard.card, yard.cardShadow, yard.outline, 2).padding(4.dp)
                    } else {
                        Modifier.padding(4.dp)
                    },
                ) {
                    PetFigure(
                        petName = profile.petName,
                        speciesId = profile.appearance.speciesId,
                        speciesTitle = species?.title ?: "Питомец",
                        accessoryId = profile.appearance.accessoryId,
                        accessoryTitle = Accessories.title(parts, profile.appearance.accessoryId),
                        colorHex = color?.hex ?: "#CCCCCC",
                        stage = s,
                        care = StatLevel.HIGH,
                        joy = StatLevel.HIGH,
                        size = 72.dp,
                        caption = false,
                        plain = true,
                        modifier = Modifier.width(72.dp),
                    )
                }
                Text(
                    text = (if (s < current) "✓ " else "") + s.displayName,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
