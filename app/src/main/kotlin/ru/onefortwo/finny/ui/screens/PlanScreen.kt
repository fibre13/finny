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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import ru.onefortwo.finny.ui.common.FinnyDialog
import ru.onefortwo.finny.ui.common.SecondaryButton
import ru.onefortwo.finny.ui.common.motionAllowed
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

/** Пустое направление, про которое спрашивают перед утверждением плана. */
private enum class EmptyJar(val text: String) {
    NEEDS("Ты уверен? Если не отложишь на нужное, ты не сможешь купить корм. Питомец останется голодным."),
    WANTS("Ты уверен? Если не отложишь на «Хочу», ты не сможешь порадовать питомца."),
    SAVINGS("Ты уверен? Если не отложишь в копилку, мечта не станет ближе."),
}

/**
 * Редактор плана: три направления и остаток крупной строкой.
 *
 * Распределить больше, чем есть, нельзя: «+» перестаёт работать, когда
 * остаток равен нулю (ТЗ 2.5.5 — сумма не превышает бюджет). Остаток можно
 * не раскладывать — это запас «на всякий случай». Пустое направление
 * требует подтверждения: так ребёнок видит последствие до утверждения.
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
    // Пустые направления, про которые ребёнок уже сказал «Да, я уверен».
    var acknowledged by rememberSaveable { mutableStateOf(listOf<String>()) }
    var asking by rememberSaveable { mutableStateOf<String?>(null) }

    // Бюджет может уменьшиться, пока экран плана лежит в стеке под копилкой.
    // Суммы, превышающие новый бюджет, урезаются до него сразу.
    LaunchedEffect(available) {
        if (needs + wants + savings > available) {
            needs = needs.coerceAtMost(available)
            wants = wants.coerceAtMost(available - needs)
            savings = savings.coerceAtMost(available - needs - wants)
        }
    }

    val remainder = (available - needs - wants - savings).coerceAtLeast(0)
    val needsGoal = savings > 0 && !hasGoal
    val colors = FinnyTheme.colors

    fun tryConfirm() {
        val empty = buildList {
            if (needs == 0) add(EmptyJar.NEEDS)
            if (wants == 0) add(EmptyJar.WANTS)
            if (savings == 0 && hasGoal) add(EmptyJar.SAVINGS)
        }.firstOrNull { it.name !in acknowledged }
        if (empty != null) asking = empty.name else onConfirm(needs, wants, savings)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionCard(
            tone = CardTone.Primary,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            bottomSpacing = 0.dp,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Распредели ${Explanations.coins(available)}.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.semantics { heading() },
                )
                // Причина, по которой кнопка «Утвердить план» неактивна, названа
                // словами (ТЗ 3.6).
                Text(
                    text = when {
                        needsGoal -> "Чтобы отложить в копилку, сначала выбери цель. Суммы сохранятся."
                        remainder == 0 -> "Всё распределено!"
                        else -> "Осталось распределить: ${Explanations.coins(remainder)}"
                    },
                    // В одну строку: экран плана помещается целиком на 360 × 800 dp.
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.coin,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                if (remainder > 0 && !needsGoal && needs + wants + savings > 0) {
                    Text(
                        text = "Остаток можно оставить на всякий случай.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onPrimary.copy(alpha = 0.9f),
                    )
                }
            }
        }

        DirectionCard(BudgetCategory.NEEDS, CardTone.Sage, needs, needs + remainder, { needs = it }) {
            SupportingText("Корм, вода, уход.")
        }
        DirectionCard(BudgetCategory.WANTS, CardTone.Coin, wants, wants + remainder, { wants = it }) {
            SupportingText("Игрушки и украшения.")
        }
        DirectionCard(BudgetCategory.SAVINGS, CardTone.Surface, savings, savings + remainder, { savings = it }) {
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
            enabled = !needsGoal,
            onClick = { tryConfirm() },
        )
    }

    asking?.let { name ->
        val jar = EmptyJar.valueOf(name)
        FinnyDialog(
            title = "Пусто в направлении",
            onDismiss = { asking = null },
            content = { Text(jar.text, style = MaterialTheme.typography.bodyLarge) },
            actions = {
                PrimaryButton(
                    text = "Да, я уверен",
                    onClick = {
                        acknowledged = acknowledged + name
                        asking = null
                        tryConfirm()
                    },
                )
                Spacer(modifier = Modifier.height(8.dp))
                SecondaryButton(text = "Вернуться к плану", onClick = { asking = null })
            },
        )
    }
}

/**
 * Карточка направления: значок, название и сумма в одной строке, под ними
 * подсказка и кнопки шага. Когда в направление кладут монеты, в него
 * запрыгивает монетка.
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
    val jump = remember { Animatable(0f) }
    var last by remember { mutableIntStateOf(value) }
    val motion = motionAllowed()
    LaunchedEffect(value) {
        if (motion && value > last) {
            jump.snapTo(0f)
            jump.animateTo(1f, tween(140))
            jump.animateTo(0f, tween(220))
        }
        last = value
    }
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
                    Box {
                        BudgetDirectionIcon(category)
                        if (jump.value > 0f) {
                            PixelIcon(
                                "coin_0",
                                cell = 2.dp,
                                modifier = Modifier.graphicsLayer {
                                    translationY = -18.dp.toPx() * (1f - jump.value)
                                    alpha = jump.value
                                },
                            )
                        }
                    }
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
                        .graphicsLayer {
                            val s = 1f + 0.15f * jump.value
                            scaleX = s
                            scaleY = s
                        }
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
                // Для копилки «больше плана» — хорошо: отложено больше.
                category == BudgetCategory.SAVINGS && actual > planned ->
                    "Отложено больше плана на ${Explanations.coinsAccusative(actual - planned)}. Отлично!"
                category == BudgetCategory.SAVINGS && actual < planned ->
                    "Отложено меньше плана на ${Explanations.coinsAccusative(planned - actual)}."
                actual > planned -> "Потрачено больше плана на ${Explanations.coinsAccusative(actual - planned)}."
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
