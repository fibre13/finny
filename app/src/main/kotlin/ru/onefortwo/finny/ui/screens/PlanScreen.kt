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
import ru.onefortwo.finny.ui.state.SEASON_DAYS
import ru.onefortwo.finny.ui.state.Season
import ru.onefortwo.finny.ui.state.SeasonExtras
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
 * ТЕСТ 3: план сезона по банкам «Нужное», «Хочу», «Копим на мечту».
 *
 * Раскладываются все свободные монеты и то, что лежит в банках: распределить
 * больше, чем есть, нельзя — «+» перестаёт работать, когда остаток равен
 * нулю, — а утвердить план можно, только когда разложено всё. Пустая банка
 * требует подтверждения. После утверждения экран показывает план и факт
 * сезона и кнопку «Изменить план»: план можно поправить в любой момент.
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
    extras: SeasonExtras = SeasonExtras(),
    free: Int = game.balance.amount,
) {
    var editing by rememberSaveable { mutableStateOf(false) }
    val showEditor = !extras.planned || editing
    ScreenScaffold(
        eyebrow = "Сезон ${extras.season} · план на $SEASON_DAYS дня",
        title = "План бюджета",
        balance = balance,
        onBack = if (editing && extras.planned) ({ editing = false }) else onBack,
        message = message,
        onDismissMessage = onDismissMessage,
        bottomPadding = 16.dp,
        singleLineTitle = true,
    ) {
        if (showEditor) {
            PlanEditor(
                available = free + extras.needsJar + extras.wantsJar,
                startNeeds = extras.needsJar,
                startWants = extras.wantsJar,
                correction = extras.planned,
                saved = game.savings.saved.amount,
                hasGoal = game.savings.goal != null,
                goalTitle = goalTitle,
                onConfirm = onConfirm,
                onChooseGoal = onChooseGoal,
            )
        } else {
            val palette = LocalBudgetColors.current
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionCard(
                    title = "План и факт сезона",
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    bottomSpacing = 0.dp,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        val (needsNote, wantsNote) = Season.borrowNotes(extras)
                        PlanFactRow(BudgetCategory.NEEDS, extras.plannedNeeds, extras.spentNeeds, palette.needs, needsNote)
                        PlanFactRow(BudgetCategory.WANTS, extras.plannedWants, extras.spentWants, palette.wants, wantsNote)
                        PlanFactRow(BudgetCategory.SAVINGS, extras.plannedSavings, extras.deposited, palette.savings)
                    }
                }
                SectionCard(
                    title = "Сейчас в банках",
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    bottomSpacing = 0.dp,
                ) {
                    Column {
                        LabeledValue(BudgetCategory.NEEDS.displayName, Explanations.coins(extras.needsJar))
                        LabeledValue(BudgetCategory.WANTS.displayName, Explanations.coins(extras.wantsJar))
                        LabeledValue("В копилке", Explanations.coins(game.savings.saved))
                        if (free > 0) {
                            SupportingText(
                                "Свободных монет: ${Explanations.coins(free)}. Разложи их по банкам.",
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                }
                PrimaryButton(text = "Изменить план", onClick = { editing = true })
            }
        }
    }
}

/** Пустая банка, про которую спрашивают перед утверждением плана. */
private enum class EmptyJar(val text: String) {
    NEEDS("Ты уверен? Если не отложишь на нужное, ты не сможешь купить корм. Питомец останется голодным."),
    WANTS("Ты уверен? Если не отложишь на «Хочу», ты не сможешь порадовать питомца."),
    SAVINGS("Ты уверен? Если не отложишь в копилку, мечта не станет ближе."),
}

/**
 * Редактор плана: три банка и остаток крупной строкой. Полосы
 * распределения и цветовой подсказки нет — остаток назван числом.
 */
@Composable
private fun PlanEditor(
    available: Int,
    startNeeds: Int,
    startWants: Int,
    correction: Boolean,
    saved: Int,
    hasGoal: Boolean,
    goalTitle: String?,
    onConfirm: (Int, Int, Int) -> Unit,
    onChooseGoal: () -> Unit,
) {
    var needs by rememberSaveable { mutableIntStateOf(startNeeds) }
    var wants by rememberSaveable { mutableIntStateOf(startWants) }
    var savings by rememberSaveable { mutableIntStateOf(0) }
    // Пустые банки, про которые ребёнок уже сказал «Да, я уверен».
    var acknowledged by rememberSaveable { mutableStateOf(listOf<String>()) }
    var asking by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(available) {
        if (needs + wants + savings > available) {
            needs = needs.coerceAtMost(available)
            wants = wants.coerceAtMost(available - needs)
            savings = savings.coerceAtMost(available - needs - wants)
        }
    }

    val remainder = (available - needs - wants - savings).coerceAtLeast(0)
    val needsGoal = savings > 0 && !hasGoal
    val valid = remainder == 0 && !needsGoal
    val colors = FinnyTheme.colors

    fun tryConfirm() {
        val empty = buildList {
            if (needs == 0) add(EmptyJar.NEEDS)
            if (wants == 0) add(EmptyJar.WANTS)
            if (savings == 0 && !correction && hasGoal) add(EmptyJar.SAVINGS)
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
                    text = if (correction) "Поправь план: ${Explanations.coins(available)} в банках." else "Распредели ${Explanations.coins(available)} по банкам.",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = when {
                        remainder > 0 -> "Осталось распределить: ${Explanations.coins(remainder)}"
                        needsGoal -> "Чтобы отложить в копилку, сначала выбери цель."
                        else -> "Всё распределено!"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.coin,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }

        DirectionCard(BudgetCategory.NEEDS, CardTone.Sage, needs, needs + remainder, { needs = it }) {
            SupportingText("Корм, вода, уход.")
        }
        DirectionCard(BudgetCategory.WANTS, CardTone.Coin, wants, wants + remainder, { wants = it }) {
            SupportingText("Игрушки и украшения.")
        }
        DirectionCard(
            BudgetCategory.SAVINGS, CardTone.Surface, savings, savings + remainder, { savings = it },
            title = if (correction) "Добавить в копилку" else BudgetCategory.SAVINGS.displayName,
        ) {
            if (hasGoal) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PixelIcon("ui_flag", cell = 1.5.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    SupportingText(
                        buildString {
                            append(if (goalTitle != null) "На «$goalTitle»" else "Сразу уйдут в копилку на цель.")
                            if (saved > 0) append(" Уже в копилке: ${Explanations.coins(saved)}.")
                        },
                    )
                }
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    SupportingText(text = "Цель не выбрана.", modifier = Modifier.align(Alignment.CenterVertically))
                    ChooseGoalButton(onClick = onChooseGoal)
                }
            }
        }

        PrimaryButton(text = "Утвердить план", enabled = valid, onClick = { tryConfirm() })
    }

    asking?.let { name ->
        val jar = EmptyJar.valueOf(name)
        FinnyDialog(
            title = "Банка пустая",
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
 * Карточка банка: значок, название и сумма в одной строке, под ними
 * подсказка и кнопки шага. Когда в банк кладут монеты, значок подпрыгивает.
 */
@Composable
private fun DirectionCard(
    category: BudgetCategory,
    tone: CardTone,
    value: Int,
    max: Int,
    onChange: (Int) -> Unit,
    title: String = category.displayName,
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
                        // Монетка прыгает в банк.
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
                        text = title,
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
                label = title,
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
internal fun PlanFactRow(
    category: BudgetCategory,
    planned: Int,
    actual: Int,
    color: Color,
    /** ТЕСТ 3: пояснение вместо стандартного вывода — например, куда ушли монеты банка. */
    note: String? = null,
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
            note ?: when {
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
