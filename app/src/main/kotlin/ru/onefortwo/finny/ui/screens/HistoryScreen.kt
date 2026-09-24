package ru.onefortwo.finny.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ru.onefortwo.finny.content.TaskContent
import ru.onefortwo.finny.economy.Coins
import ru.onefortwo.finny.economy.GameState
import ru.onefortwo.finny.ui.common.CardTone
import ru.onefortwo.finny.ui.common.LabeledValue
import ru.onefortwo.finny.ui.common.ProgressBar
import ru.onefortwo.finny.ui.common.ScreenScaffold
import ru.onefortwo.finny.ui.common.SectionCard
import ru.onefortwo.finny.ui.common.StatusPill
import ru.onefortwo.finny.ui.common.SupportingText
import ru.onefortwo.finny.ui.state.Explanations
import ru.onefortwo.finny.ui.theme.FinnyTheme

/**
 * История и учебный прогресс (ТЗ 2.5.11): завершённые задания,
 * прогресс по текущей цели и итоги последних игровых дней.
 * Открывается вкладкой «Прогресс».
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
) {
    val colors = FinnyTheme.colors
    val daysLived = game.period.number - 1

    ScreenScaffold(
        eyebrow = "$petName растёт вместе с тобой",
        title = "Мой прогресс",
        balance = balance,
        onBack = onBack,
    ) {
        Column {
            SectionCard(
                eyebrow = "Ступень роста",
                title = game.stage.displayName,
                tone = CardTone.Sage,
                trailing = {
                    StatusPill(
                        text = "Прожито дней: $daysLived",
                        container = colors.surface,
                    )
                },
            ) {
                LabeledValue("Шагов роста", game.growthPoints.toString())
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
                            MarkedRow(done = true, text = title)
                        }
                    }
                }
            }

            SectionCard(title = "Задания: ${completedIds.size} из ${tasks.size}") {
                Column {
                    tasks.forEach { task ->
                        val done = task.id in completedIds
                        MarkedRow(
                            done = done,
                            text = if (done) {
                                "Уже решал — ${task.title}"
                            } else {
                                "Ещё впереди — ${task.title}"
                            },
                        )
                    }
                }
            }

            Text(
                text = "Прошедшие дни",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 12.dp)
                    .semantics { heading() },
            )

            if (game.history.isEmpty()) {
                SupportingText("Пока нет завершённых дней. Закончи день на главном экране.")
            } else {
                game.history.reversed().forEach { outcome ->
                    SectionCard(title = "День ${outcome.number}") {
                        Column {
                            LabeledValue("Шагов роста", "${outcome.earnedPoints} из 3")
                            LabeledValue("Потрачено на нужное", Explanations.coins(outcome.spentNeeds))
                            LabeledValue("Потрачено на желаемое", Explanations.coins(outcome.spentWants))
                            LabeledValue("Отложено", Explanations.coins(outcome.deposited))
                            if (outcome.stageAdvanced) {
                                SupportingText(
                                    text = "В этот день питомец подрос до стадии " +
                                        "«${outcome.stageAfter.displayName}».",
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Строка списка с отметкой. Решено ли задание, сказано словами в начале
 * строки; знак только помогает увидеть это сразу.
 */
@Composable
private fun MarkedRow(done: Boolean, text: String) {
    val colors = FinnyTheme.colors

    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = if (done) "✓" else "•",
            style = MaterialTheme.typography.titleMedium,
            color = if (done) colors.successText else colors.onSurfaceMuted,
            modifier = Modifier.clearAndSetSemantics { },
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}
