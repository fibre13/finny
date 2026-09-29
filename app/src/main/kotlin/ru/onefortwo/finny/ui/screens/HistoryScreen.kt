package ru.onefortwo.finny.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.onefortwo.finny.content.PetPartsContent
import ru.onefortwo.finny.content.TaskContent
import ru.onefortwo.finny.economy.Coins
import ru.onefortwo.finny.economy.GameState
import ru.onefortwo.finny.economy.GrowthStage
import ru.onefortwo.finny.ui.common.CardTone
import ru.onefortwo.finny.ui.common.LabeledValue
import ru.onefortwo.finny.ui.common.PrimaryButton
import ru.onefortwo.finny.ui.common.ProgressBar
import ru.onefortwo.finny.ui.common.ScreenScaffold
import ru.onefortwo.finny.ui.common.SectionCard
import ru.onefortwo.finny.ui.common.SupportingText
import ru.onefortwo.finny.ui.common.rememberPixelArt
import ru.onefortwo.finny.ui.state.AppState
import ru.onefortwo.finny.ui.state.Explanations
import ru.onefortwo.finny.ui.state.Growth
import ru.onefortwo.finny.ui.state.Season
import ru.onefortwo.finny.ui.theme.FinnyTheme

/**
 * Мой прогресс (ТЗ 2.5.11). Ступень роста — цепочка стадий и
 * условие следующей: мечты и задания; прогресс по мечте; сколько заданий
 * решено и сколько до сюрприза. Список прошедших дней и «шаги роста» убраны.
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
    state: AppState? = null,
    parts: PetPartsContent? = null,
    toSurprise: Int? = null,
    onOpenTasks: () -> Unit = {},
) {
    val colors = FinnyTheme.colors
    val stage = game.stage
    val dreams = achievedGoalTitles.size
    val solved = state?.extras?.tasksSolved ?: completedIds.size
    val next = Growth.nextNeeds(stage)

    ScreenScaffold(
        title = "Мой прогресс",
        balance = balance,
        onBack = onBack,
    ) {
        Column {
            SectionCard(eyebrow = "Ступень роста", title = "$petName — ${stage.displayName.lowercase()}", tone = CardTone.Sage) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (state != null && parts != null) StageChain(state, parts, stage)
                    LabeledValue("Прожито дней", "${game.history.size}")
                    LabeledValue("Сезонов", "${Season.seasonOf(game.period.number)}")
                    if (next != null) {
                        val (needDreams, needTasks) = next
                        val who = if (stage == GrowthStage.BABY) "подростком" else "взрослым"
                        if (dreams == needDreams - 1 && stage == GrowthStage.BABY) {
                            Text(
                                text = "Ты накопил на первую мечту! Ещё одна мечта — и $petName станет $who, и с ним будет интереснее играть.",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        } else {
                            Text(
                                text = "Накопи на ${needDreams} мечты и реши ${Explanations.tasks(needTasks)} — $petName станет $who, " +
                                    "и с ним будет ещё интереснее играть.",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        ProgressLine("Мечты: ${dreams.coerceAtMost(needDreams)} из $needDreams", dreams, needDreams)
                        ProgressLine("Задания: ${solved.coerceAtMost(needTasks)} из $needTasks", solved, needTasks)
                    } else {
                        SupportingText("$petName совсем взрослый!")
                    }
                }
            }

            val goal = game.savings.goal
            if (goal == null) {
                SectionCard(title = "Копим на мечту", tone = CardTone.Primary) {
                    Text("Мечта пока не выбрана.", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                val percent = (game.savings.saved.amount * 100 / goal.price.amount.coerceAtLeast(1)).coerceIn(0, 100)
                SectionCard(
                    title = "Копим на: ${goalTitle ?: "цель"}",
                    tone = CardTone.Primary,
                    trailing = { Text(text = "$percent%", style = MaterialTheme.typography.titleMedium, color = colors.coin) },
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
                        SupportingText(text = Explanations.forecast(game.goalForecast()), modifier = Modifier.padding(top = 8.dp))
                        if (next != null && dreams == next.first - 1) {
                            Text(
                                text = "Это твоя ${if (next.first == 2) "вторая" else "третья"} мечта! Накопи на неё — и $petName вырастет.",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }
            }

            if (achievedGoalTitles.isNotEmpty()) {
                SectionCard(title = "Накоплено и получено", tone = CardTone.Coin) {
                    Column { achievedGoalTitles.forEach { title -> Text("✓ $title", style = MaterialTheme.typography.bodyLarge) } }
                }
            }

            SectionCard(title = "Задания: ты выполнил уже ${Explanations.tasks(solved)}") {
                Column {
                    SupportingText(
                        if (toSurprise != null) "До новых товаров в лавке — ${Explanations.tasks(toSurprise)}." else "Все сюрпризы в лавке открыты!",
                    )
                    PrimaryButton(text = "К заданиям →", onClick = onOpenTasks, modifier = Modifier.padding(top = 10.dp))
                }
            }
        }
    }
}

/** Подпись и полоса прогресса к стадии. */
@Composable
private fun ProgressLine(label: String, value: Int, total: Int) {
    val colors = FinnyTheme.colors
    Column {
        Text(label, style = MaterialTheme.typography.titleSmall)
        ProgressBar(
            fraction = value.toFloat() / total.coerceAtLeast(1),
            color = colors.successText,
            height = 8.dp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** Цепочка стадий: пройденные — с галочкой, текущая — ярко, будущие — бледно. */
@Composable
private fun StageChain(state: AppState, parts: PetPartsContent, current: GrowthStage) {
    val art = rememberPixelArt()
    val yard = remember(art) { YardColors(art) }
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
                    ProfilePet(state, parts, size = 72.dp, stage = s)
                }
                Text(
                    text = (if (s < current) "✓ " else "") + s.displayName,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                )
            }
            if (s != GrowthStage.ADULT) Spacer(modifier = Modifier.width(4.dp))
        }
    }
}
