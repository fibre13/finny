package ru.onefortwo.finny.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import ru.onefortwo.finny.economy.BudgetCategory
import ru.onefortwo.finny.economy.GameState
import ru.onefortwo.finny.economy.PeriodOutcome
import ru.onefortwo.finny.ui.common.BudgetDirectionIcon
import ru.onefortwo.finny.ui.common.CoinSlider
import ru.onefortwo.finny.ui.common.LabeledValue
import ru.onefortwo.finny.ui.common.MinTouchTarget
import ru.onefortwo.finny.ui.common.PrimaryButton
import ru.onefortwo.finny.ui.common.ProgressBar
import ru.onefortwo.finny.ui.common.ScreenScaffold
import ru.onefortwo.finny.ui.common.SecondaryButton
import ru.onefortwo.finny.ui.common.SectionCard
import ru.onefortwo.finny.ui.state.Explanations
import ru.onefortwo.finny.ui.state.FeedbackMessage
import ru.onefortwo.finny.ui.theme.LocalAppliqueDecor
import ru.onefortwo.finny.ui.theme.LocalBudgetColors

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
) {
    ScreenScaffold(
        title = "План на день ${game.period.number}",
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
                Text(
                    text = "План составлен. Теперь видно, как расходы сходятся с планом.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 12.dp),
                )

                val palette = LocalBudgetColors.current

                SectionCard(title = "План и факт") {
                    Column {
                        PlanFactRow(
                            title = BudgetCategory.NEEDS.displayName,
                            planned = plan.needs.amount,
                            actual = game.period.spent(BudgetCategory.NEEDS).amount,
                            color = palette.needs,
                        )
                        PlanFactRow(
                            title = BudgetCategory.WANTS.displayName,
                            planned = plan.wants.amount,
                            actual = game.period.spent(BudgetCategory.WANTS).amount,
                            color = palette.wants,
                        )
                        PlanFactRow(
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
    // бюджет, урезаются до него. Иначе при нулевом бюджете ползунки не
    // выводятся, и уменьшить лишние суммы было бы нечем.
    LaunchedEffect(available) {
        needs = needs.coerceAtMost(available)
        wants = wants.coerceAtMost(available)
        savings = savings.coerceAtMost(available)
    }

    val budget = LocalBudgetColors.current
    val total = needs + wants + savings
    val remainder = available - total
    val withinBudget = remainder >= 0
    val needsGoal = savings > 0 && !hasGoal
    val valid = withinBudget && !needsGoal

    Column {
        Text(
            text = "У тебя ${Explanations.coins(available)}. Реши, сколько монет на что отложить.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        SectionCard(
            title = BudgetCategory.NEEDS.displayName,
            icon = { BudgetDirectionIcon(BudgetCategory.NEEDS) },
            edgeColor = budget.needs,
        ) {
            AmountSlider(
                hint = "Корм, вода, уход. Это покупают в первую очередь.",
                value = needs,
                max = available,
                color = budget.needs,
                onChange = { needs = it },
            )
        }

        SectionCard(
            title = BudgetCategory.WANTS.displayName,
            icon = { BudgetDirectionIcon(BudgetCategory.WANTS) },
            edgeColor = budget.wants,
        ) {
            AmountSlider(
                hint = "Игрушки и украшения. Можно отложить на завтра.",
                value = wants,
                max = available,
                color = budget.wants,
                onChange = { wants = it },
            )
        }

        SectionCard(
            title = BudgetCategory.SAVINGS.displayName,
            icon = { BudgetDirectionIcon(BudgetCategory.SAVINGS) },
            edgeColor = budget.savings,
        ) {
            Column {
                AmountSlider(
                    hint = if (hasGoal) {
                        "Эти монеты сразу уйдут в копилку на твою цель."
                    } else {
                        "Цель ещё не выбрана. Чтобы откладывать, сначала выбери, на что копишь."
                    },
                    value = savings,
                    max = available,
                    color = budget.savings,
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

        SectionCard(title = "Итого") {
            Column {
                LabeledValue("Распределено", Explanations.coins(total))
                LabeledValue(
                    label = if (withinBudget) "Остаток" else "Лишние монеты",
                    value = Explanations.coins(kotlin.math.abs(remainder)),
                )
                // Причина выводится для каждого случая, когда кнопка
                // «Утвердить план» неактивна (ТЗ 3.6).
                Text(
                    text = when {
                        !withinBudget ->
                            "Ты распределил больше, чем есть. Уменьши одно из направлений."
                        needsGoal ->
                            "Чтобы отложить в копилку, сначала выбери цель. " +
                                "Введённые суммы сохранятся."
                        else -> "Остаток можно оставить на всякий случай."
                    },
                    style = MaterialTheme.typography.bodyMedium,
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

/** Ползунок суммы с подписью и подсказкой. */
@Composable
private fun AmountSlider(
    hint: String,
    value: Int,
    max: Int,
    color: Color,
    onChange: (Int) -> Unit,
) {
    Column {
        Text(hint, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = Explanations.coins(value),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp),
        )
        CoinSlider(
            value = value,
            max = max,
            color = color,
            onChange = onChange,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** Строка сравнения плана и факта по направлению. */
@Composable
private fun PlanFactRow(
    title: String,
    planned: Int,
    actual: Int,
    color: Color,
) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        // Название направления окрашено: рядом идёт полоса того же цвета,
        // и по ней видно соотношение факта с планом. Само соотношение
        // названо словами ниже — цвет только помогает связать их взглядом.
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        LabeledValue("По плану", Explanations.coins(planned))
        LabeledValue("На самом деле", Explanations.coins(actual))
        ProgressBar(
            fraction = if (planned > 0) actual.toFloat() / planned else 0f,
            color = color,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = when {
                actual > planned -> "Потрачено больше плана на ${Explanations.coins(actual - planned)}."
                actual < planned -> "Осталось в пределах плана: ${Explanations.coins(planned - actual)} не потрачено."
                else -> "Точно по плану."
            },
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/** Сравнение плана с фактом по завершённому периоду. */
@Composable
fun PlanFactSummary(outcome: PeriodOutcome) {
    Column {
        val palette = LocalBudgetColors.current

        PlanFactRow(
            title = BudgetCategory.NEEDS.displayName,
            planned = outcome.plan.needs.amount,
            actual = outcome.spentNeeds.amount,
            color = palette.needs,
        )
        PlanFactRow(
            title = BudgetCategory.WANTS.displayName,
            planned = outcome.plan.wants.amount,
            actual = outcome.spentWants.amount,
            color = palette.wants,
        )
        PlanFactRow(
            title = BudgetCategory.SAVINGS.displayName,
            planned = outcome.plan.savings.amount,
            actual = outcome.deposited.amount,
            color = palette.savings,
        )
    }
}
