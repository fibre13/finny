package ru.onefortwo.finny.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ru.onefortwo.finny.economy.GrowthCondition
import ru.onefortwo.finny.economy.PeriodOutcome
import ru.onefortwo.finny.economy.PetStatKind
import ru.onefortwo.finny.ui.common.AdaptiveGrid
import ru.onefortwo.finny.ui.common.CardTone
import ru.onefortwo.finny.ui.common.LabeledValue
import ru.onefortwo.finny.ui.common.PrimaryButton
import ru.onefortwo.finny.ui.common.ScreenScaffold
import ru.onefortwo.finny.ui.common.SecondaryButton
import ru.onefortwo.finny.ui.common.SectionCard
import ru.onefortwo.finny.ui.common.SupportingText
import ru.onefortwo.finny.ui.state.Explanations
import ru.onefortwo.finny.ui.theme.FinnyTheme

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

    ScreenScaffold(
        eyebrow = "День завершён",
        title = "Итоги дня ${outcome.number}",
        onBack = null,
    ) {
        Column {
            SectionCard(
                title = if (outcome.isSetback) "День был непростым" else "Как прошёл день",
                tone = if (outcome.isSetback) CardTone.Warning else CardTone.Sage,
            ) {
                Column {
                    Text(summary.text, style = MaterialTheme.typography.bodyMedium)
                    if (summary.nextStep != null) {
                        SupportingText(
                            text = "→ ${summary.nextStep}",
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }

            SectionCard(title = "Итоги дня", tone = CardTone.Primary) {
                val tiles = buildList {
                    add("${outcome.earnedPoints} из 3" to "шаги роста")
                    add(Explanations.coins(outcome.deposited) to "в копилку")
                    outcome.nextPeriodIncome?.let { income ->
                        add("+${Explanations.coins(income.amount)}" to "на новый день")
                    }
                }
                // Плитки не уже 140 dp: сумма с единицей «25 монет» не
                // разрывается между строками.
                AdaptiveGrid(count = tiles.size, minItemWidth = 140.dp, spacing = 8.dp) { index ->
                    val (value, label) = tiles[index]
                    SummaryTile(value = value, label = label, modifier = Modifier.weight(1f))
                }
            }

            SectionCard(title = "Шаги роста: ${outcome.earnedPoints} из 3") {
                Column {
                    GrowthCondition.entries.forEach { condition ->
                        val met = condition in outcome.metConditions
                        ConditionRow(
                            met = met,
                            text = if (met) {
                                "Да — ${Explanations.condition(condition)}"
                            } else {
                                "Нет — ${Explanations.conditionMissed(condition)}"
                            },
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
                    SupportingText(
                        text = "За день питомец успевает проголодаться и соскучиться — " +
                            "поэтому показатели немного снижаются каждый день.",
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            SectionCard(title = "Развитие", tone = if (outcome.stageAdvanced) CardTone.Success else CardTone.Surface) {
                Column {
                    LabeledValue("Стадия", outcome.stageAfter.displayName)
                    LabeledValue("Всего шагов роста", outcome.pointsAfter.toString())
                    SupportingText(
                        text = if (outcome.stageAdvanced) {
                            "$petName подрос! Так бывает, когда решения раз за разом продуманные."
                        } else {
                            "Стадия не изменилась. Она растёт по сумме решений за несколько дней."
                        },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            outcome.nextPeriodIncome?.let { income ->
                SectionCard(title = "Новый день", tone = CardTone.Coin) {
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
                            SupportingText(
                                text = "План на новый день можно составить уже сейчас, " +
                                    "а закончить этот день получится завтра.",
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
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }

            PrimaryButton(text = "Начать новый день", onClick = onContinue)
        }
    }
}

/** Плитка сводки на тёмной карточке: число и подпись под ним. */
@Composable
private fun SummaryTile(value: String, label: String, modifier: Modifier = Modifier) {
    val colors = FinnyTheme.colors

    Column(
        modifier = modifier
            .heightIn(min = 72.dp)
            .clip(MaterialTheme.shapes.small)
            .background(colors.primaryTile)
            .semantics(mergeDescendants = true) { }
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = colors.coin)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.onPrimary)
    }
}

/**
 * Условие шага роста: знак и текст. Выполнено ли оно, сказано словом
 * «Да» или «Нет» в начале строки, знак только помогает увидеть это сразу.
 */
@Composable
private fun ConditionRow(met: Boolean, text: String) {
    val colors = FinnyTheme.colors

    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = if (met) "✓" else "×",
            style = MaterialTheme.typography.titleMedium,
            color = if (met) colors.successText else colors.errorText,
            modifier = Modifier.clearAndSetSemantics { },
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
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
        Text(name, style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Было $before, стало $after — $change. Сейчас: $label.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
