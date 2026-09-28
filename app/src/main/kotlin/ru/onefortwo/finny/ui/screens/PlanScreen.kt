package ru.onefortwo.finny.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.onefortwo.finny.economy.BudgetCategory
import ru.onefortwo.finny.economy.Coins
import ru.onefortwo.finny.economy.GameState
import ru.onefortwo.finny.economy.PeriodOutcome
import ru.onefortwo.finny.ui.common.BudgetDirectionIcon
import ru.onefortwo.finny.ui.common.CardTone
import ru.onefortwo.finny.ui.common.ChevronIcon
import ru.onefortwo.finny.ui.common.CoinStepper
import ru.onefortwo.finny.ui.common.LabeledValue
import ru.onefortwo.finny.ui.common.PixelIcon
import ru.onefortwo.finny.ui.common.PrimaryButton
import ru.onefortwo.finny.ui.common.ProgressBar
import ru.onefortwo.finny.ui.common.ScreenScaffold
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
 *
 * На телефоне 360 × 800 dp при обычном шрифте редактор помещается целиком,
 * вместе с кнопкой «Утвердить план»; при крупном шрифте экран прокручивается.
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
    /** Название выбранной цели: куда уйдут монеты копилки. */
    goalTitle: String? = null,
) {
    ScreenScaffold(
        eyebrow = "План на день ${game.period.number}",
        title = "План бюджета",
        balance = balance,
        onBack = onBack,
        message = message,
        onDismissMessage = onDismissMessage,
        bottomPadding = 16.dp,
        singleLineTitle = true,
    ) {
        val plan = game.period.plan

        if (plan == null) {
            PlanEditor(
                available = game.balance.amount,
                hasGoal = game.savings.goal != null,
                goalTitle = goalTitle,
                onConfirm = onConfirm,
                onChooseGoal = onChooseGoal,
            )
        } else {
            val palette = LocalBudgetColors.current

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SupportingText("План составлен. Видно, как расходы сходятся с планом.")

                SectionCard(
                    title = "План и факт",
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    bottomSpacing = 0.dp,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        PlanFactRow(
                            category = BudgetCategory.NEEDS,
                            planned = plan.needs.amount,
                            actual = game.period.spent(BudgetCategory.NEEDS).amount,
                            color = palette.needs,
                        )
                        PlanFactRow(
                            category = BudgetCategory.WANTS,
                            planned = plan.wants.amount,
                            actual = game.period.spent(BudgetCategory.WANTS).amount,
                            color = palette.wants,
                        )
                        PlanFactRow(
                            category = BudgetCategory.SAVINGS,
                            planned = plan.savings.amount,
                            actual = game.period.depositedToSavings.amount,
                            color = palette.savings,
                        )
                    }
                }

                SectionCard(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    bottomSpacing = 0.dp,
                ) {
                    LabeledValue("Можно потратить", Explanations.coins(game.balance))
                }
            }
        }
    }
}

/**
 * Редактор плана: три направления и остаток.
 *
 * Остаток и причина, по которой кнопка «Утвердить план» неактивна, стоят
 * под полосой распределения в верхней карточке, рядом с суммой «N из M»
 * (ТЗ 3.6). Отдельный блок «Итого» не выводится: экран помещается целиком.
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
    goalTitle: String?,
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
    val colors = FinnyTheme.colors

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionCard(
            tone = CardTone.Primary,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            bottomSpacing = 0.dp,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Распредели монеты",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = "$total из $available",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.coin,
                    )
                }
                DistributionBar(available = available, needs = needs, wants = wants, savings = savings)
                // Причина выводится для каждого случая, когда кнопка
                // «Утвердить план» неактивна (ТЗ 3.6).
                Text(
                    text = when {
                        !withinBudget ->
                            "Больше, чем есть, на ${Explanations.coins(-remainder)}. Убавь одно из направлений."
                        needsGoal ->
                            "Чтобы отложить в копилку, сначала выбери цель. Суммы сохранятся."
                        else ->
                            "Остаток ${Explanations.coins(remainder)} — можно оставить на всякий случай."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (valid) FontWeight.Normal else FontWeight.Bold,
                    color = if (valid) colors.onPrimary.copy(alpha = 0.9f) else colors.coin,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }

        DirectionCard(
            category = BudgetCategory.NEEDS,
            tone = CardTone.Sage,
            value = needs,
            max = available,
            onChange = { needs = it },
        ) {
            SupportingText("Корм, вода, уход.")
        }

        DirectionCard(
            category = BudgetCategory.WANTS,
            tone = CardTone.Coin,
            value = wants,
            max = available,
            onChange = { wants = it },
        ) {
            SupportingText("Игрушки и украшения.")
        }

        DirectionCard(
            category = BudgetCategory.SAVINGS,
            tone = CardTone.Surface,
            value = savings,
            max = available,
            onChange = { savings = it },
        ) {
            if (hasGoal) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PixelIcon("ui_flag", cell = 1.5.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    SupportingText(
                        if (goalTitle != null) "На «$goalTitle»" else "Сразу уйдут в копилку на цель.",
                    )
                }
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    SupportingText(
                        text = "Цель не выбрана.",
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                    ChooseGoalButton(onClick = onChooseGoal)
                }
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
 * Карточка направления: значок, название и сумма в одной строке, под ними
 * подсказка и кнопки шага.
 *
 * Сумма объявляется программой чтения с экрана при каждом изменении:
 * кнопки шага меняют число, которое видно рядом с названием.
 */
@Composable
private fun DirectionCard(
    category: BudgetCategory,
    tone: CardTone,
    value: Int,
    max: Int,
    onChange: (Int) -> Unit,
    hint: @Composable () -> Unit,
) {
    SectionCard(
        tone = tone,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        bottomSpacing = 0.dp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BudgetDirectionIcon(category)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = category.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                }
                Text(
                    text = Explanations.coins(value),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            hint()
            CoinStepper(
                label = category.displayName,
                value = value,
                max = max,
                onChange = onChange,
                compact = true,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/** Переход к выбору цели: текстовая кнопка с целью нажатия 48 dp. */
@Composable
private fun ChooseGoalButton(onClick: () -> Unit) {
    val colors = FinnyTheme.colors

    Row(
        modifier = Modifier
            .clip(PillShape)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Выбрать цель",
            style = MaterialTheme.typography.labelLarge,
            color = colors.attentionText,
        )
        ChevronIcon(size = 18.dp)
    }
}

/**
 * Строка сравнения плана и факта по направлению: «факт из плана», полоса
 * цвета направления и вывод словами. Цвет полосы только помогает связать
 * строку с направлением: направление названо словом, соотношение — числом
 * и фразой.
 */
@Composable
private fun PlanFactRow(
    category: BudgetCategory,
    planned: Int,
    actual: Int,
    color: Color,
) {
    val title = category.displayName

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription = "$title: по плану ${Explanations.coins(planned)}, " +
                        "на самом деле ${Explanations.coins(actual)}"
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BudgetDirectionIcon(category)
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("$actual из $planned", style = MaterialTheme.typography.titleMedium)
        }
        ProgressBar(
            fraction = if (planned > 0) actual.toFloat() / planned else 0f,
            color = color,
            height = 8.dp,
        )
        SupportingText(
            when {
                actual > planned -> "Потрачено больше плана на ${Explanations.coins(actual - planned)}."
                actual < planned -> "Осталось в пределах плана: ${Explanations.coins(planned - actual)} не потрачено."
                else -> "Точно по плану."
            },
        )
    }
}

/** Сравнение плана с фактом по завершённому периоду. */
@Composable
fun PlanFactSummary(outcome: PeriodOutcome) {
    val palette = LocalBudgetColors.current

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        PlanFactRow(
            category = BudgetCategory.NEEDS,
            planned = outcome.plan.needs.amount,
            actual = outcome.spentNeeds.amount,
            color = palette.needs,
        )
        PlanFactRow(
            category = BudgetCategory.WANTS,
            planned = outcome.plan.wants.amount,
            actual = outcome.spentWants.amount,
            color = palette.wants,
        )
        PlanFactRow(
            category = BudgetCategory.SAVINGS,
            planned = outcome.plan.savings.amount,
            actual = outcome.deposited.amount,
            color = palette.savings,
        )
    }
}

/**
 * Полоса распределения: доли нужного, желаемого и копилки от бюджета,
 * нераспределённое — дорожкой. Для программы чтения с экрана полоса скрыта:
 * те же числа названы на карточках направлений и в строке «N из M», поэтому
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

    Row(
        modifier = modifier
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
}
