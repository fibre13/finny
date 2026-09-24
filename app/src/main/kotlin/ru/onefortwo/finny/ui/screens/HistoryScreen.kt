package ru.onefortwo.finny.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.onefortwo.finny.content.TaskContent
import ru.onefortwo.finny.economy.GameState
import ru.onefortwo.finny.ui.common.LabeledValue
import ru.onefortwo.finny.ui.common.ProgressBar
import ru.onefortwo.finny.ui.common.ScreenScaffold
import ru.onefortwo.finny.ui.common.SectionCard
import ru.onefortwo.finny.ui.state.Explanations
import ru.onefortwo.finny.ui.theme.LocalBudgetColors

/**
 * История и учебный прогресс (ТЗ 2.5.11): завершённые задания,
 * прогресс по текущей цели и итоги последних игровых дней.
 */
@Composable
fun HistoryScreen(
    game: GameState,
    tasks: List<TaskContent>,
    completedIds: Set<String>,
    goalTitle: String?,
    /** Названия уже полученных целей, в порядке достижения. */
    achievedGoalTitles: List<String>,
    onBack: () -> Unit,
) {
    ScreenScaffold(title = "Мой прогресс", onBack = onBack) {
        Column {
            SectionCard(title = "Питомец") {
                Column {
                    LabeledValue("Стадия развития", game.stage.displayName)
                    LabeledValue("Шагов роста", game.growthPoints.toString())
                    LabeledValue("Прожито дней", (game.period.number - 1).toString())
                }
            }

            SectionCard(title = "Цель", edgeColor = LocalBudgetColors.current.savings) {
                Column {
                    val goal = game.savings.goal
                    if (goal == null) {
                        Text("Цель пока не выбрана.", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        LabeledValue("Копим на", goalTitle ?: "цель")
                        LabeledValue("Накоплено", Explanations.coins(game.savings.saved))
                        LabeledValue("Осталось", Explanations.coins(game.savings.remaining))
                        ProgressBar(
                            fraction = game.savings.saved.amount.toFloat() / goal.price.amount,
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = LocalBudgetColors.current.savings,
                        )
                        Text(
                            text = Explanations.forecast(game.goalForecast()),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            if (achievedGoalTitles.isNotEmpty()) {
                SectionCard(title = "Накоплено и получено") {
                    Column {
                        achievedGoalTitles.forEach { title ->
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 2.dp),
                            )
                        }
                    }
                }
            }

            SectionCard(title = "Задания: ${completedIds.size} из ${tasks.size}") {
                Column {
                    tasks.forEach { task ->
                        Text(
                            text = if (task.id in completedIds) {
                                "Уже решал — ${task.title}"
                            } else {
                                "Ещё впереди — ${task.title}"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                }
            }

            Text(
                text = "Прошедшие дни",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            )

            if (game.history.isEmpty()) {
                Text(
                    text = "Пока нет завершённых дней. Закончи день на главном экране.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                game.history.reversed().forEach { outcome ->
                    SectionCard(title = "День ${outcome.number}") {
                        Column {
                            LabeledValue("Шагов роста", "${outcome.earnedPoints} из 3")
                            LabeledValue("Потрачено на нужное", Explanations.coins(outcome.spentNeeds))
                            LabeledValue("Потрачено на желаемое", Explanations.coins(outcome.spentWants))
                            LabeledValue("Отложено", Explanations.coins(outcome.deposited))
                            if (outcome.stageAdvanced) {
                                Text(
                                    text = "В этот день питомец подрос до стадии " +
                                        "«${outcome.stageAfter.displayName}».",
                                    style = MaterialTheme.typography.bodyMedium,
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
