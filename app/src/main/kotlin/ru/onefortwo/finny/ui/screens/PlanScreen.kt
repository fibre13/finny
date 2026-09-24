package ru.onefortwo.finny.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ru.onefortwo.finny.economy.BudgetCategory
import ru.onefortwo.finny.economy.Coins
import ru.onefortwo.finny.economy.GameState
import ru.onefortwo.finny.economy.PeriodOutcome
import ru.onefortwo.finny.ui.common.BudgetDirectionIcon
import ru.onefortwo.finny.ui.common.CardTone
import ru.onefortwo.finny.ui.common.CoinStepper
import ru.onefortwo.finny.ui.common.LabeledValue
import ru.onefortwo.finny.ui.common.PrimaryButton
import ru.onefortwo.finny.ui.common.ProgressBar
import ru.onefortwo.finny.ui.common.ScreenScaffold
import ru.onefortwo.finny.ui.common.SecondaryButton
import ru.onefortwo.finny.ui.common.SectionCard
import ru.onefortwo.finny.ui.common.SupportingText
import ru.onefortwo.finny.ui.state.Explanations
import ru.onefortwo.finny.ui.state.FeedbackMessage
import ru.onefortwo.finny.ui.theme.FinnyTheme
import ru.onefortwo.finny.ui.theme.LocalBudgetColors
import ru.onefortwo.finny.ui.theme.PillShape

/**
 * План личного бюджета (ТЗ 2.5.5).
 *
 * До подтверждения план свободно изменяется, приложение контролирует, чтобы
 * сумма не превышала доступный бюджет, и постоянно показывает остаток.
 * После подтверждения экран показывает сравнение плана с фактом.
 *
 * Если подтвердить план не удалось, причина показывается на этом же экране,
 * а введённые суммы остаются: экран закрывается только принятым планом.
 */
@Composable
fun PlanScreen(
    game: GameState,
    onConfirm: (Int, Int, Int) -> Unit,
    onBack: () -> Unit,
    message: FeedbackMessage? = null,
    onDismissMessage: () -> Unit = {},
    onChooseGoal: () -> Unit = {},
    balance: Coins? = null,
) {
    ScreenScaffold(
        eyebrow = "План на день ${game.period.number}",
        title = "План бюджета",
        balance = balance,
        onBack = onBack,
        message = message,
        onDismissMessage = onDismissMessage,
    ) {
        val plan = game.period.plan

        if (plan == null) {
            PlanEditor(
                available = game.balance.amount,
                hasGoal = game.savings.goal != null,
                onConfirm = onConfirm,
                onChooseGoal = onChooseGoal,
            )
        } else {
            Column {
                SupportingText(
                    text = "План составлен. Теперь видно, как расходы сходятся с планом.",
                    modifier = Modifier.padding(bottom = 12.dp),
                )

                val palette = LocalBudgetColors.current

                SectionCard(title = "План и факт") {
                    Column {
                        PlanFactRow(
                            category = BudgetCategory.NEEDS,
                            title = BudgetCategory.NEEDS.displayName,
                            planned = plan.needs.amount,
                            actual = game.period.spent(BudgetCategory.NEEDS).amount,
                            color = palette.needs,
                        )
                        PlanFactRow(
                            category = BudgetCategory.WANTS,
                            title = BudgetCategory.WANTS.displayName,
                            planned = plan.wants.amount,
                            actual = game.period.spent(BudgetCategory.WANTS).amount,
                            color = palette.wants,
                        )
                        PlanFactRow(
                            category = BudgetCategory.SAVINGS,
                            title = BudgetCategory.SAVINGS.displayName,
                            planned = plan.savings.amount,
                            actual = game.period.depositedToSavings.amount,
                            color = palette.savings,
                        )
                    }
                }

                SectionCard(title = "Осталось монет") {
                    LabeledValue("Можно потратить", Explanations.coins(game.balance))
                }
            }
        }
    }
}

/**
 * Редактор плана: три направления и остаток.
 *
 * Откладывать в копилку можно только на выбранную цель. Без цели план с
 * ненулевой копилкой не принимается, поэтому об этом сказано заранее, на
 * карточке копилки, с переходом к выбору цели: копилка открывается поверх
 * плана, и введённые суммы при возврате сохраняются.
 */
@Composable
private fun PlanEditor(
    available: Int,
    hasGoal: Boolean,
    onConfirm: (Int, Int, Int) -> Unit,
    onChooseGoal: () -> Unit,
) {
    var needs by rememberSaveable { mutableIntStateOf(0) }
    var wants by rememberSaveable { mutableIntStateOf(0) }
    var savings by rememberSaveable { mutableIntStateOf(0) }

    // Бюджет может уменьшиться, пока экран плана лежит в стеке под копилкой:
    // из копилки можно отложить монеты с баланса. Суммы, превышающие новый
    // бюджет, урезаются до него сразу, чтобы ребёнок не видел направление
    // с суммой больше всего, что у него есть.
    LaunchedEffect(available) {
        needs = needs.coerceAtMost(available)
        wants = wants.coerceAtMost(available)
        savings = savings.coerceAtMost(available)
    }

    val total = needs + wants + savings
    val remainder = available - total
    val withinBudget = remainder >= 0
    val needsGoal = savings > 0 && !hasGoal
    val valid = withinBudget && !needsGoal

    Column {
        SectionCard(
            title = "Распредели монеты",
            tone = CardTone.Primary,
            trailing = {
                Text(
                    text = "$total из $available",
                    style = MaterialTheme.typography.titleMedium,
                    color = FinnyTheme.colors.coin,
                )
            },
        ) {
            Column {
                Text(
                    text = "У тебя ${Explanations.coins(available)}. Реши, сколько монет на что отложить.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                DistributionBar(
                    available = available,
                    needs = needs,
                    wants = wants,
                    savings = savings,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }
        }

        SectionCard(
            title = BudgetCategory.NEEDS.displayName,
            tone = CardTone.Sage,
            icon = { BudgetDirectionIcon(BudgetCategory.NEEDS, size = 32.dp) },
        ) {
            AmountStepper(
                hint = "Корм, вода, уход. Это покупают в первую очередь.",
                value = needs,
                max = available,
                label = BudgetCategory.NEEDS.displayName,
                onChange = { needs = it },
            )
        }

        SectionCard(
            title = BudgetCategory.WANTS.displayName,
            tone = CardTone.Coin,
            icon = { BudgetDirectionIcon(BudgetCategory.WANTS, size = 32.dp) },
        ) {
            AmountStepper(
                hint = "Игрушки и украшения. Можно отложить на завтра.",
                value = wants,
                max = available,
                label = BudgetCategory.WANTS.displayName,
                onChange = { wants = it },
            )
        }

        SectionCard(
            title = BudgetCategory.SAVINGS.displayName,
            icon = { BudgetDirectionIcon(BudgetCategory.SAVINGS, size = 32.dp) },
        ) {
            Column {
                AmountStepper(
                    hint = if (hasGoal) {
                        "Эти монеты сразу уйдут в копилку на твою цель."
                    } else {
                        "Цель ещё не выбрана. Чтобы откладывать, сначала выбери, на что копишь."
                    },
                    value = savings,
                    max = available,
                    label = BudgetCategory.SAVINGS.displayName,
                    onChange = { savings = it },
                )
                if (!hasGoal) {
                    SecondaryButton(
                        text = "Выбрать цель",
                        onClick = onChooseGoal,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }

        SectionCard(
            title = "Итого",
            tone = if (valid) CardTone.Surface else CardTone.Warning,
        ) {
            Column {
                LabeledValue("Распределено", Explanations.coins(total))
                LabeledValue(
                    label = if (withinBudget) "Остаток" else "Лишние монеты",
                    value = Explanations.coins(kotlin.math.abs(remainder)),
                )
                // Причина выводится для каждого случая, когда кнопка
                // «Утвердить план» неактивна (ТЗ 3.6).
                SupportingText(
                    text = when {
                        !withinBudget ->
                            "Ты распределил больше, чем есть. Уменьши одно из направлений."
                        needsGoal ->
                            "Чтобы отложить в копилку, сначала выбери цель. " +
                                "Введённые суммы сохранятся."
                        else -> "Остаток можно оставить на всякий случай."
                    },
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        PrimaryButton(
            text = "Утвердить план",
            enabled = valid,
            onClick = { onConfirm(needs, wants, savings) },
        )
    }
}

/**
 * Сумма направления с подсказкой и кнопками шага.
 *
 * Сумма выводится крупно и объявляется программой чтения с экрана при
 * каждом изменении: кнопки шага меняют число, которое видно над ними.
 */
@Composable
private fun AmountStepper(
    hint: String,
    value: Int,
    max: Int,
    label: String,
    onChange: (Int) -> Unit,
) {
    Column {
        SupportingText(hint)
        Text(
            text = Explanations.coins(value),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .padding(top = 10.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
        CoinStepper(
            label = label,
            value = value,
            max = max,
            onChange = onChange,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** Строка сравнения плана и факта по направлению. */
@Composable
private fun PlanFactRow(
    category: BudgetCategory,
    title: String,
    planned: Int,
    actual: Int,
    color: Color,
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        // Направление названо словом и отмечено пиктограммой; цвет полосы
        // только помогает связать строку с направлением. Соотношение факта
        // с планом названо словами ниже.
        Row(verticalAlignment = Alignment.CenterVertically) {
            BudgetDirectionIcon(category, size = 28.dp, modifier = Modifier.padding(end = 8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
        LabeledValue("По плану", Explanations.coins(planned))
        LabeledValue("На самом деле", Explanations.coins(actual))
        ProgressBar(
            fraction = if (planned > 0) actual.toFloat() / planned else 0f,
            color = color,
            modifier = Modifier.padding(top = 4.dp),
        )
        SupportingText(
            text = when {
                actual > planned -> "Потрачено больше плана на ${Explanations.coins(actual - planned)}."
                actual < planned -> "Осталось в пределах плана: ${Explanations.coins(planned - actual)} не потрачено."
                else -> "Точно по плану."
            },
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/** Сравнение плана с фактом по завершённому периоду. */
@Composable
fun PlanFactSummary(outcome: PeriodOutcome) {
    Column {
        val palette = LocalBudgetColors.current

        PlanFactRow(
            category = BudgetCategory.NEEDS,
            title = BudgetCategory.NEEDS.displayName,
            planned = outcome.plan.needs.amount,
            actual = outcome.spentNeeds.amount,
            color = palette.needs,
        )
        PlanFactRow(
            category = BudgetCategory.WANTS,
            title = BudgetCategory.WANTS.displayName,
            planned = outcome.plan.wants.amount,
            actual = outcome.spentWants.amount,
            color = palette.wants,
        )
        PlanFactRow(
            category = BudgetCategory.SAVINGS,
            title = BudgetCategory.SAVINGS.displayName,
            planned = outcome.plan.savings.amount,
            actual = outcome.deposited.amount,
            color = palette.savings,
        )
    }
}

/**
 * Полоса распределения: доли нужного, желаемого и копилки от бюджета,
 * нераспределённое — дорожкой. Под полосой те же числа словами, поэтому
 * цвет ничего не передаёт в одиночку (ТЗ 3.6).
 */
@Composable
private fun DistributionBar(
    available: Int,
    needs: Int,
    wants: Int,
    savings: Int,
    modifier: Modifier = Modifier,
) {
    val palette = LocalBudgetColors.current
    val track = FinnyTheme.colors.onPrimary.copy(alpha = 0.22f)
    val parts = listOf(needs to palette.needs, wants to palette.wants, savings to palette.savings)
    val total = needs + wants + savings
    // При перерасходе полоса делится по распределённому, а не по бюджету.
    val scale = maxOf(available, total).coerceAtLeast(1)
    val rest = (scale - total).coerceAtLeast(0)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(PillShape)
                .background(track)
                .clearAndSetSemantics { },
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            parts.filter { it.first > 0 }.forEach { (value, color) ->
                Box(
                    modifier = Modifier
                        .weight(value.toFloat())
                        .height(12.dp)
                        .background(color),
                )
            }
            if (rest > 0) {
                Box(modifier = Modifier.weight(rest.toFloat()).height(12.dp))
            }
        }
        Text(
            text = "Нужное $needs · Хочу $wants · Копилка $savings",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
