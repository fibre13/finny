package ru.onefortwo.finny.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.onefortwo.finny.economy.GrowthCondition
import ru.onefortwo.finny.economy.PeriodOutcome
import ru.onefortwo.finny.economy.PetStatKind
import ru.onefortwo.finny.ui.common.LabeledValue
import ru.onefortwo.finny.ui.common.PrimaryButton
import ru.onefortwo.finny.ui.common.ScreenScaffold
import ru.onefortwo.finny.ui.common.SecondaryButton
import ru.onefortwo.finny.ui.common.SectionCard
import ru.onefortwo.finny.ui.state.Explanations

/**
 * Итоги игрового периода (ТЗ 2.5.9, 2.5.10).
 *
 * Показывает сравнение плана с фактом, изменение показателей питомца
 * и причину изменения стадии развития. При неудачном дне предлагается
 * понятный путь восстановления, прогресс при этом сохраняется.
 */
@Composable
fun PeriodResultScreen(
    petName: String,
    outcome: PeriodOutcome,
    nextDayTomorrow: Boolean,
    onOpenRecoveryTask: () -> Unit,
    onContinue: () -> Unit,
) {
    val summary = Explanations.periodSummary(petName, outcome)

    ScreenScaffold(title = "Итоги дня ${outcome.number}", onBack = null) {
        Column {
            SectionCard(title = if (outcome.isSetback) "День был непростым" else "Как прошёл день") {
                Column {
                    Text(summary.text, style = MaterialTheme.typography.bodyMedium)
                    if (summary.nextStep != null) {
                        Text(
                            text = "Что дальше: ${summary.nextStep}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }

            SectionCard(title = "Шаги роста: ${outcome.earnedPoints} из 3") {
                Column {
                    GrowthCondition.entries.forEach { condition ->
                        val met = condition in outcome.metConditions
                        Text(
                            text = if (met) {
                                "Да — ${Explanations.condition(condition)}"
                            } else {
                                "Нет — ${Explanations.conditionMissed(condition)}"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                }
            }

            SectionCard(title = "План и факт") {
                PlanFactSummary(outcome)
            }

            SectionCard(title = "Как изменился $petName") {
                Column {
                    StatChangeRow(
                        name = Explanations.statName(PetStatKind.CARE),
                        before = outcome.petBefore.care.value,
                        after = outcome.petAfter.care.value,
                        label = Explanations.statLabel(PetStatKind.CARE, outcome.petAfter.care),
                    )
                    StatChangeRow(
                        name = Explanations.statName(PetStatKind.JOY),
                        before = outcome.petBefore.joy.value,
                        after = outcome.petAfter.joy.value,
                        label = Explanations.statLabel(PetStatKind.JOY, outcome.petAfter.joy),
                    )
                    Text(
                        text = "За день питомец успевает проголодаться и соскучиться — " +
                            "поэтому показатели немного снижаются каждый день.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            SectionCard(title = "Развитие") {
                Column {
                    LabeledValue("Стадия", outcome.stageAfter.displayName)
                    LabeledValue("Всего шагов роста", outcome.pointsAfter.toString())
                    Text(
                        text = if (outcome.stageAdvanced) {
                            "$petName подрос! Так бывает, когда решения раз за разом продуманные."
                        } else {
                            "Стадия не изменилась. Она растёт по сумме решений за несколько дней."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            outcome.nextPeriodIncome?.let { income ->
                SectionCard(title = "Новый день") {
                    Column {
                        Text(
                            text = "${Explanations.incomeSource(income.source)}: " +
                                "+${Explanations.coins(income.amount)}. " +
                                "Теперь у тебя ${Explanations.coins(income.balanceAfter)}.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        // Монеты следующего дня начисляются сразу, и план на него
                        // можно составить сейчас. Завтра нужно только закончить
                        // этот день: об этом сказано здесь, чтобы ребёнок не ждал
                        // завтра ни монет, ни плана.
                        if (nextDayTomorrow) {
                            Text(
                                text = "План на новый день можно составить уже сейчас, " +
                                    "а закончить этот день получится завтра.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }
            }

            if (outcome.isSetback) {
                SecondaryButton(
                    text = "Выполнить задание «Помоги Финни»",
                    onClick = onOpenRecoveryTask,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            PrimaryButton(text = "Начать новый день", onClick = onContinue)
        }
    }
}

/** Изменение показателя с указанием было и стало. */
@Composable
private fun StatChangeRow(name: String, before: Int, after: Int, label: String) {
    val delta = after - before
    val change = when {
        delta > 0 -> "выросла на $delta"
        delta < 0 -> "снизилась на ${-delta}"
        else -> "не изменилась"
    }

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Text(
            text = "Было $before, стало $after — $change. Сейчас: $label.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
