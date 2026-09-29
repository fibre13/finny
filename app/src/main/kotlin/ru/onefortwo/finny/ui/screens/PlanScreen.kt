package ru.onefortwo.finny.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import ru.onefortwo.finny.ui.common.MinTouchTarget
import ru.onefortwo.finny.ui.common.SecondaryButton
import ru.onefortwo.finny.ui.common.motionAllowed
import ru.onefortwo.finny.ui.state.SEASON_DAYS
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
import ru.onefortwo.finny.economy.Goal
import ru.onefortwo.finny.economy.PeriodOutcome
import ru.onefortwo.finny.ui.common.BudgetDirectionIcon
import ru.onefortwo.finny.ui.common.CardTone
import ru.onefortwo.finny.ui.common.ChevronIcon
import ru.onefortwo.finny.ui.common.CoinStepper
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
 * План сезона: «Нужное», «Хочу», «Копим на мечту».
 *
 * Раскладываются все свободные монеты и то, что уже отложено на нужное и
 * на желаемое: распределить больше, чем есть, нельзя — «+» перестаёт
 * работать, когда остаток равен нулю, — а утвердить план можно, только
 * когда разложено всё. Пустое направление требует подтверждения. После
 * утверждения экран «Мой бюджет» показывает план и факт сезона полосами
 * и кнопку «Изменить план»: план можно поправить в любой момент.
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
    /** Переход в «Копилку» по нажатию на мечту; по умолчанию — туда же, куда выбор цели. */
    onOpenGoal: () -> Unit = onChooseGoal,
) {
    var editing by rememberSaveable { mutableStateOf(false) }
    val showEditor = !extras.planned || editing
    ScreenScaffold(
        eyebrow = "Сезон ${extras.season} · план на $SEASON_DAYS дня",
        title = if (showEditor) "План бюджета" else "Мой бюджет",
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
                goal = game.savings.goal,
                goalTitle = goalTitle,
                onConfirm = onConfirm,
                onChooseGoal = onChooseGoal,
                onOpenGoal = onOpenGoal,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionCard(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    bottomSpacing = 0.dp,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        BudgetBars(
                            plannedNeeds = extras.plannedNeeds,
                            spentNeeds = extras.spentNeeds,
                            plannedWants = extras.plannedWants,
                            spentWants = extras.spentWants,
                            plannedSavings = extras.plannedSavings,
                            deposited = extras.deposited,
                            leftNeeds = extras.needsJar,
                            leftWants = extras.wantsJar,
                            leftSavings = game.savings.saved.amount,
                        )
                        if (free > 0) {
                            SupportingText("Свободных монет: ${Explanations.coins(free)}. Разложи их в плане")
                        }
                    }
                }
                PrimaryButton(text = "Изменить план", onClick = { editing = true })
            }
        }
    }
}

/** Пустое направление, про которое спрашивают перед утверждением плана. */
private enum class EmptyJar(val title: String, val text: String) {
    NEEDS("«Нужное» — 0 монет", "Ты уверен? Если не отложишь на нужное, ты не сможешь купить корм. Питомец останется голодным."),
    WANTS("«Хочу» — 0 монет", "Ты уверен? Если не отложишь на «Хочу», ты не сможешь порадовать питомца."),
    SAVINGS("«Копим на мечту» — 0 монет", "Ты уверен? Если не отложишь в копилку, мечта не станет ближе."),
}

/**
 * Редактор плана: вверху — сколько монет распределить, затем три
 * направления, под ними остаток крупной строкой и кнопка «Утвердить план».
 */
@Composable
private fun PlanEditor(
    available: Int,
    startNeeds: Int,
    startWants: Int,
    correction: Boolean,
    saved: Int,
    goal: Goal?,
    goalTitle: String?,
    onConfirm: (Int, Int, Int) -> Unit,
    onChooseGoal: () -> Unit,
    onOpenGoal: () -> Unit,
) {
    val hasGoal = goal != null
    var needs by rememberSaveable { mutableIntStateOf(startNeeds) }
    var wants by rememberSaveable { mutableIntStateOf(startWants) }
    var savings by rememberSaveable { mutableIntStateOf(0) }
    // Пустые направления, про которые ребёнок уже сказал «Да, я уверен».
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
    // Утвердить можно только при нулевом остатке (и с целью, если что-то отложено в копилку).
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
            Text(
                text = if (correction) {
                    "Распредели ${Explanations.coins(available)} заново"
                } else {
                    "Распредели ${Explanations.coins(available)}"
                },
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
        }

        DirectionCard(BudgetCategory.NEEDS, CardTone.Sage, needs, needs + remainder, { needs = it }) {
            SupportingText("Корм, вода, уход")
        }
        DirectionCard(BudgetCategory.WANTS, CardTone.Coin, wants, wants + remainder, { wants = it }) {
            SupportingText("Игрушки и украшения")
        }
        DirectionCard(
            BudgetCategory.SAVINGS, CardTone.Surface, savings, savings + remainder, { savings = it },
            title = if (correction) "Добавить в копилку" else BudgetCategory.SAVINGS.displayName,
        ) {
            if (goal != null) {
                DreamRow(goal = goal, title = goalTitle, saved = saved, onClick = onOpenGoal)
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    SupportingText(text = "Цель не выбрана", modifier = Modifier.align(Alignment.CenterVertically))
                    ChooseGoalButton(onClick = onChooseGoal)
                }
            }
        }

        // Остаток — под направлениями, над кнопкой. Текст тёмными
        // вариантами цветов: контраст к фону экрана не ниже 4,5:1.
        Text(
            text = when {
                remainder > 0 -> "Осталось распределить: ${Explanations.coins(remainder)}"
                needsGoal -> "Чтобы отложить в копилку, сначала выбери цель"
                else -> "Всё распределено!"
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (valid) colors.successText else colors.warningText,
            modifier = Modifier
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )

        PrimaryButton(text = "Утвердить план", enabled = valid, onClick = { tryConfirm() })
    }

    asking?.let { name ->
        val jar = EmptyJar.valueOf(name)
        FinnyDialog(
            title = jar.title,
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
 * Мечта на карточке копилки: пиксельная картинка, название, стоимость и
 * полоса «сколько уже в копилке из стоимости». Строка целиком — кнопка
 * перехода в «Копилку», где цель можно поменять.
 */
@Composable
private fun DreamRow(goal: Goal, title: String?, saved: Int, onClick: () -> Unit) {
    val colors = FinnyTheme.colors
    val price = goal.price.amount
    val name = title ?: "Моя мечта"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 48.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "Мечта «$name»: стоит ${Explanations.coins(price)}, " +
                    "в копилке ${Explanations.coins(saved)}. Открыть копилку"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GoalPicture(goalId = goal.id, size = 64.dp, container = colors.appBackground)
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .clearAndSetSemantics { },
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            SupportingText("Стоит ${Explanations.coins(price)}")
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgressBar(
                    fraction = saved.toFloat() / price,
                    color = colors.success,
                    height = 8.dp,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("$saved из $price", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(modifier = Modifier.width(4.dp))
        ChevronIcon(size = 18.dp)
    }
}

/**
 * Карточка направления: значок, название, сумма и крестик «сбросить в 0»
 * в одной строке, под ними подсказка и кнопки шага. Когда в направление
 * кладут монеты, значок подпрыгивает.
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
                Row(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box {
                        BudgetDirectionIcon(category)
                        // Монетка прыгает в направление.
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = Explanations.coins(value),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .graphicsLayer {
                                val s = 1f + 0.15f * jump.value
                                scaleX = s
                                scaleY = s
                            }
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    )
                    ResetButton(label = title, visible = value > 0, onClick = { onChange(0) })
                }
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

/**
 * Крестик «сбросить в 0» справа от суммы: цель нажатия [MinTouchTarget].
 * При нулевой сумме крестика нет, но место под него сохраняется, чтобы
 * строка не прыгала.
 */
@Composable
private fun ResetButton(label: String, visible: Boolean, onClick: () -> Unit) {
    if (!visible) {
        Spacer(modifier = Modifier.size(MinTouchTarget))
        return
    }
    Box(
        modifier = Modifier
            .size(MinTouchTarget)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "$label: сбросить в 0" },
        contentAlignment = Alignment.Center,
    ) {
        PixelIcon("ui_close", cell = 2.dp)
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

/** Высота полосы на графике «План и факт». */
private val BarHeight = 14.dp

/**
 * График «План и факт» по трём направлениям и строка остатков.
 *
 * У каждого направления две полосы: жёлтая «План» и «Факт» цветом
 * сравнения — зелёный, если факт равен плану, красный, если больше
 * плана, жёлтый, если меньше. Для копилки «больше плана» означает
 * «отложено больше» и закрашивается зелёным. Все полосы в одном масштабе
 * (по наибольшему значению), при нуле полоса не рисуется. Под полосами —
 * подписи «План N» и «Факт N»: соотношение читается по числам, цвет его
 * только дополняет; программа чтения с экрана называет его словами.
 *
 * Строка «Осталось» выводится, когда заданы все три остатка.
 */
@Composable
internal fun BudgetBars(
    plannedNeeds: Int,
    spentNeeds: Int,
    plannedWants: Int,
    spentWants: Int,
    plannedSavings: Int,
    deposited: Int,
    modifier: Modifier = Modifier,
    /** Сколько осталось на нужное. */
    leftNeeds: Int? = null,
    /** Сколько осталось на «Хочу». */
    leftWants: Int? = null,
    /** Сколько в копилке. */
    leftSavings: Int? = null,
) {
    val scale = maxOf(1, plannedNeeds, spentNeeds, plannedWants, spentWants, plannedSavings, deposited)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BudgetBarGroup(BudgetCategory.NEEDS, plannedNeeds, spentNeeds, scale)
        BudgetBarGroup(BudgetCategory.WANTS, plannedWants, spentWants, scale)
        BudgetBarGroup(BudgetCategory.SAVINGS, plannedSavings, deposited, scale)
        if (leftNeeds != null && leftWants != null && leftSavings != null) {
            RemainingRow(leftNeeds, leftWants, leftSavings)
        }
    }
}

/** Направление на графике: заголовок и полосы «План» и «Факт». */
@Composable
private fun BudgetBarGroup(category: BudgetCategory, planned: Int, actual: Int, scale: Int) {
    val colors = FinnyTheme.colors
    val factColor = when {
        actual == planned -> colors.success
        actual > planned && category == BudgetCategory.SAVINGS -> colors.success
        actual > planned -> colors.error
        else -> colors.coin
    }
    val verdict = when {
        actual == planned -> "точно по плану"
        actual > planned -> "больше плана"
        else -> "меньше плана"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = "${category.displayName}: план ${Explanations.coins(planned)}, " +
                    "факт ${Explanations.coins(actual)}, $verdict"
            },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BudgetDirectionIcon(category)
            Spacer(modifier = Modifier.width(8.dp))
            Text(category.displayName, style = MaterialTheme.typography.titleMedium)
        }
        BudgetBar(label = "План $planned", value = planned, scale = scale, color = colors.coin)
        BudgetBar(label = "Факт $actual", value = actual, scale = scale, color = factColor)
    }
}

/**
 * Полоса графика и подпись под ней. Контур цвета границы выделяет светлые
 * заливки на светлой карточке; подпись — основным цветом текста.
 */
@Composable
private fun BudgetBar(label: String, value: Int, scale: Int, color: Color) {
    val colors = FinnyTheme.colors

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        // При нуле полоса не рисуется; место под неё остаётся.
        Box(modifier = Modifier.fillMaxWidth().height(BarHeight)) {
            if (value > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((value.toFloat() / scale).coerceIn(0.04f, 1f))
                        .height(BarHeight)
                        .clip(PillShape)
                        .background(color)
                        .border(1.dp, colors.outline, PillShape),
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, colors.outline, CircleShape),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
        }
    }
}

/**
 * Строка остатков: «Осталось:» и по направлениям пиксельный значок с
 * числом — миска (нужное), мячик (хочу), монета (копилка).
 */
@Composable
private fun RemainingRow(needs: Int, wants: Int, savings: Int) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = "Осталось: ${BudgetCategory.NEEDS.displayName} — ${Explanations.coins(needs)}, " +
                    "${BudgetCategory.WANTS.displayName} — ${Explanations.coins(wants)}, " +
                    "в копилке — ${Explanations.coins(savings)}"
            },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Осталось:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.CenterVertically),
        )
        listOf(
            BudgetCategory.NEEDS to needs,
            BudgetCategory.WANTS to wants,
            BudgetCategory.SAVINGS to savings,
        ).forEach { (category, amount) ->
            Row(
                modifier = Modifier.align(Alignment.CenterVertically),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BudgetDirectionIcon(category, size = 22.dp)
                Spacer(modifier = Modifier.width(4.dp))
                Text("$amount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
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
    /** Пояснение вместо стандартного вывода — например, откуда взяты монеты. */
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
                else -> "Точно по плану"
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
