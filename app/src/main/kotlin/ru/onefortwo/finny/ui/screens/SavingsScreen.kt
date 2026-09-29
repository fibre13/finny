package ru.onefortwo.finny.ui.screens

import ru.onefortwo.finny.content.Accessories
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import ru.onefortwo.finny.content.PetPartsContent
import ru.onefortwo.finny.content.celebrationFor
import ru.onefortwo.finny.content.dreamSize
import ru.onefortwo.finny.ui.common.ChevronIcon
import ru.onefortwo.finny.ui.common.MinTouchTarget
import ru.onefortwo.finny.ui.common.PetFigure
import ru.onefortwo.finny.ui.common.StatusPill
import ru.onefortwo.finny.ui.common.rememberPulse
import ru.onefortwo.finny.ui.common.softShadow
import ru.onefortwo.finny.ui.state.PetReaction
import ru.onefortwo.finny.ui.state.PetReactions
import ru.onefortwo.finny.ui.state.Profile
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ru.onefortwo.finny.content.GoalContent
import ru.onefortwo.finny.economy.Coins
import ru.onefortwo.finny.economy.GameState
import ru.onefortwo.finny.economy.WithdrawalPreview
import ru.onefortwo.finny.ui.common.ButtonTone
import ru.onefortwo.finny.ui.common.CardTone
import ru.onefortwo.finny.ui.common.CoinStepper
import ru.onefortwo.finny.ui.common.FinnyDialog
import ru.onefortwo.finny.ui.common.LabeledValue
import ru.onefortwo.finny.ui.common.PrimaryButton
import ru.onefortwo.finny.ui.common.PixelImage
import ru.onefortwo.finny.ui.common.ProgressBar
import ru.onefortwo.finny.ui.common.ScreenScaffold
import ru.onefortwo.finny.ui.common.SecondaryButton
import ru.onefortwo.finny.ui.common.SectionCard
import ru.onefortwo.finny.ui.common.SupportingText
import ru.onefortwo.finny.ui.common.composeGoal
import ru.onefortwo.finny.ui.common.pixelImage
import ru.onefortwo.finny.ui.common.rememberPixelArt
import ru.onefortwo.finny.ui.state.Explanations
import ru.onefortwo.finny.ui.state.FeedbackMessage
import ru.onefortwo.finny.ui.theme.FinnyTheme

/**
 * Копилка и финансовая цель (ТЗ 2.5.7).
 *
 * Видны стоимость цели, накопленная сумма, остаток и понятный срок
 * достижения. Снятие выполняется только после отдельного подтверждения,
 * до которого показывается, как изменятся сумма и срок.
 */
@Composable
fun SavingsScreen(
    game: GameState,
    goals: List<GoalContent>,
    message: FeedbackMessage?,
    onDismissMessage: () -> Unit,
    onChooseGoal: (String) -> Unit,
    onClaimGoal: () -> Unit,
    onDeposit: (Int) -> Unit,
    onPreviewWithdrawal: (Int) -> WithdrawalPreview,
    onWithdraw: (Int) -> Unit,
    onBack: () -> Unit,
    balance: Coins? = null,
    /** Профиль и части внешности: питомец ребёнка на празднике и при выборе цели. */
    profile: Profile? = null,
    parts: PetPartsContent? = null,
    /** Цель уже получали: выбор цели — «новая мечта». */
    afterClaim: Boolean = false,
    /** «Изменить» — пополнение копилки делается в плане дня, всё в одном месте. */
    onOpenPlan: () -> Unit = {},
) {
    // Суммы выставляются кнопками шага, как на экране плана: печатать
    // число с клавиатуры не нужно (замечание тестировщика о вводе).
    var depositAmount by rememberSaveable { mutableIntStateOf(0) }
    var withdrawAmount by rememberSaveable { mutableIntStateOf(0) }
    // Хранится сумма, а не объект предпросмотра: примитив переживает
    // поворот экрана, а сам предпросмотр пересчитывается из неё.
    var previewAmount by rememberSaveable { mutableStateOf<Int?>(null) }

    val goal = game.savings.goal
    val goalContent = goals.firstOrNull { it.id == goal?.id }
    val goalTitle = goalContent?.title
    val reached = goal != null && game.savings.saved >= goal.price

    val pet: PetSlot = { size, reaction, onEnd ->
        if (profile != null && parts != null) {
            val species = parts.species.firstOrNull { it.id == profile.appearance.speciesId }
            val color = parts.colors.firstOrNull { it.id == profile.appearance.colorId }
            PetFigure(
                petName = profile.petName,
                speciesId = profile.appearance.speciesId,
                speciesTitle = species?.title ?: "Питомец",
                accessoryId = profile.appearance.accessoryId,
                accessoryTitle = Accessories.title(parts, profile.appearance.accessoryId),
                colorHex = color?.hex ?: "#CCCCCC",
                stage = game.stage,
                care = game.pet.care.level,
                joy = game.pet.joy.level,
                size = size,
                caption = false,
                plain = true,
                reaction = reaction,
                onReactionEnd = onEnd,
                description = "${profile.petName} радуется",
                modifier = Modifier.width(size),
            )
        }
    }

    ScreenScaffold(
        eyebrow = if (goal == null && afterClaim) "Новая мечта" else "Моя цель",
        title = "Копилка",
        balance = balance,
        onBack = onBack,
        message = message,
        onDismissMessage = onDismissMessage,
    ) {
        Column {
            if (goal == null) {
                GoalChooser(
                    goals = goals,
                    saved = game.savings.saved,
                    afterClaim = afterClaim,
                    pet = pet,
                    onChoose = onChooseGoal,
                )
            } else if (reached) {
                GoalCelebration(
                    goal = goalContent,
                    goalId = goal.id,
                    price = goal.price,
                    saved = game.savings.saved,
                    petName = profile?.petName ?: "Финни",
                    pet = pet,
                    onClaim = onClaimGoal,
                )
            } else {
                SectionCard(
                    eyebrow = goalTitle ?: "Моя цель",
                    title = "${game.savings.saved.amount} из ${Explanations.coins(goal.price)}",
                    tone = CardTone.Primary,
                    trailing = { GoalPicture(goal.id) },
                ) {
                    Column {
                        ProgressBar(
                            fraction = game.savings.saved.amount.toFloat() / goal.price.amount,
                            color = FinnyTheme.colors.coin,
                            trackColor = FinnyTheme.colors.onPrimary.copy(alpha = 0.22f),
                            modifier = Modifier.padding(bottom = 10.dp),
                        )
                        LabeledValue("Стоимость", Explanations.coins(goal.price))
                        LabeledValue("Уже накоплено", Explanations.coins(game.savings.saved))
                        // Пополнение — в плане дня. Ссылка — отдельной строкой под
                        // суммой: в одной строке сумма переносилась. План меняется
                        // только до утверждения (ТЗ 2.5.5), поэтому после — подсказка.
                        if (game.period.isPlanConfirmed) {
                            SupportingText(
                                text = "Отложить ещё можно в плане завтра.",
                                modifier = Modifier.padding(vertical = 6.dp),
                            )
                        } else Text(
                            text = "Изменить →",
                            style = MaterialTheme.typography.labelLarge,
                            color = FinnyTheme.colors.onPrimary,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier
                                .align(Alignment.End)
                                .heightIn(min = 48.dp)
                                .clickable(role = Role.Button, onClick = onOpenPlan)
                                .wrapContentHeight(Alignment.CenterVertically),
                        )
                        LabeledValue("Осталось накопить", Explanations.coins(game.savings.remaining))
                        SupportingText(
                            text = Explanations.forecast(game.goalForecast()),
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }

                // Снять можно, но только с подтверждением и показом, как изменятся сумма и срок.
                if (game.savings.saved.amount > 0) {
                    SecondaryButton(
                        text = "Забрать монеты на покупки",
                        onClick = { withdrawAmount = 0; previewAmount = 0 },
                    )
                }
            }
        }
    }

    previewAmount?.let { amount ->
        WithdrawalConfirmation(
            preview = onPreviewWithdrawal(amount),
            max = game.savings.saved.amount,
            onAmount = { previewAmount = it },
            onConfirm = {
                if (amount > 0) onWithdraw(amount)
                withdrawAmount = 0
                previewAmount = null
            },
            onCancel = { previewAmount = null },
        )
    }
}

/**
 * Сумма кнопками шага: подпись, крупное число и «−5», «−1», «+1», «+5».
 * Число объявляется программой чтения с экрана при каждом изменении.
 */
@Composable
private fun AmountPicker(
    label: String,
    value: Int,
    max: Int,
    onChange: (Int) -> Unit,
) {
    Column(modifier = Modifier.padding(top = 10.dp)) {
        SupportingText(label)
        Text(
            text = Explanations.coins(value),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .padding(top = 2.dp)
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

/**
 * Подтверждение снятия. До подтверждения показано, как уменьшится
 * накопленная сумма и как изменится срок достижения цели (ТЗ 2.5.7).
 */
@Composable
private fun WithdrawalConfirmation(
    preview: WithdrawalPreview,
    max: Int,
    onAmount: (Int) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    FinnyDialog(
        title = "Забрать из копилки?",
        onDismiss = onCancel,
        content = {
            SupportingText("Монеты вернутся на баланс — разложишь их в «Плане». Цель станет дальше.")
            AmountPicker(label = "Сколько забрать", value = preview.amount.amount, max = max, onChange = onAmount)
            Spacer(modifier = Modifier.height(8.dp))
            LabeledValue("Сейчас в копилке", Explanations.coins(preview.savedBefore))
            LabeledValue("Станет", Explanations.coins(preview.savedAfter))
            Text(
                text = "Сейчас: ${Explanations.forecast(preview.forecastBefore)}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = "Станет: ${Explanations.forecast(preview.forecastAfter)}",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        actions = {
            // В столбец: «Оставить в копилке» в строку рядом с «Забрать»
            // не помещается.
            PrimaryButton(
                text = "Забрать",
                enabled = preview.amount.amount > 0,
                onClick = onConfirm,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            SecondaryButton(text = "Оставить в копилке", onClick = onCancel)
        },
    )
}

/**
 * Иллюстрация цели на светлой плитке. Декоративная: цель названа
 * надзаголовком карточки или описанием карточки выбора.
 */
@Composable
private fun GoalPicture(
    goalId: String,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    container: Color = FinnyTheme.colors.surface,
    inset: Dp = 4.dp,
) {
    val art = rememberPixelArt()
    val image = remember(art, goalId) {
        composeGoal(art, goalId)?.let { pixelImage(it, art.sprite(goalId)?.width ?: 32) }
    } ?: return

    Box(
        modifier = modifier
            .size(size)
            .clip(MaterialTheme.shapes.small)
            .background(container)
            .clearAndSetSemantics { },
    ) {
        PixelImage(image = image, modifier = Modifier.fillMaxSize().padding(inset))
    }
}

/** Сколько раз питомец прыгает от радости, когда открывается праздник. */
private const val CELEBRATION_ROUNDS = 3

/** Питомец ребёнка заданного размера с реакцией; на экране копилки — без плитки. */
private typealias PetSlot = @Composable (size: Dp, reaction: PetReaction?, onReactionEnd: (Long) -> Unit) -> Unit

/**
 * Цель накоплена: праздник вместо карточки цели, пополнения и снятия.
 * Питомец ребёнка прыгает рядом с предметом цели, счётчик — «цена из цены»,
 * лишнее названо отдельной строкой. Кнопка «Забрать …» пульсирует.
 */
@Composable
private fun GoalCelebration(
    goal: GoalContent?,
    goalId: String,
    price: Coins,
    saved: Coins,
    petName: String,
    pet: PetSlot,
    onClaim: () -> Unit,
) {
    val colors = FinnyTheme.colors
    val shape = RoundedCornerShape(24.dp)
    // Прыжки проигрываются несколько раз подряд, затем питомец просто дышит.
    // При выключенных движениях реакции снимаются сразу, и фигура стоит.
    var round by rememberSaveable(goalId) { mutableIntStateOf(0) }
    val reaction = if (round < CELEBRATION_ROUNDS) PetReaction(PetReactions.PLAY, id = round + 1L) else null
    val title = goal?.title ?: "Цель"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .softShadow(shape)
            .clip(shape)
            .background(colors.surface)
            .border(2.dp, colors.coin, shape)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(236.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(colors.coinContainer),
        ) {
            Box(modifier = Modifier.align(Alignment.Center).size(width = 292.dp, height = 236.dp)) {
                Sparkles(modifier = Modifier.matchParentSize())
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp)
                        .size(width = 260.dp, height = 26.dp)
                        .clip(CircleShape)
                        .background(colors.coin.copy(alpha = 0.55f)),
                )
                Box(modifier = Modifier.offset(x = (-30).dp, y = (-6).dp)) {
                    pet(240.dp, reaction) { round++ }
                }
                GoalPicture(
                    goalId = goalId,
                    size = 160.dp,
                    container = Color.Transparent,
                    modifier = Modifier.offset(x = 136.dp, y = 62.dp),
                )
            }
        }
        Text(
            text = "Ты сделал это!",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp).semantics { heading() },
        )
        val line = goal?.celebrationFor(petName).orEmpty()
        if (line.isNotBlank()) {
            Text(
                text = line,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = "$title: накоплено ${price.amount} из ${price.amount}, цель собрана"
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f),
            )
            Text(text = "${price.amount} из ${price.amount} ✓", style = MaterialTheme.typography.titleMedium)
        }
        ProgressBar(
            fraction = 1f,
            color = colors.coin,
            trackColor = colors.track,
            modifier = Modifier.padding(top = 6.dp),
        )
        val surplus = saved.amount - price.amount
        if (surplus > 0) {
            Text(
                text = "Ещё ${Explanations.coins(surplus)} ${remainVerb(surplus)} в копилке — для новой цели.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
    val pulse = rememberPulse()
    PrimaryButton(
        text = "Забрать ${goal?.claimTitle ?: "цель"}",
        onClick = onClaim,
        modifier = Modifier
            .padding(top = 16.dp)
            .graphicsLayer {
                scaleX = pulse.value
                scaleY = pulse.value
            },
    )
}

/** «останется» для 1, 21, 31…, иначе «останутся». */
private fun remainVerb(n: Int): String = if (n % 10 == 1 && n % 100 != 11) "останется" else "останутся"

/** Пиксельные искры вокруг питомца: крестики из пяти клеток по 5 dp. */
@Composable
private fun Sparkles(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.clearAndSetSemantics { }) {
        val cell = 5.dp.toPx()
        SPARKLES.forEach { (x, y, color) ->
            val ox = x.dp.toPx()
            val oy = y.dp.toPx()
            listOf(1 to 0, 0 to 1, 1 to 1, 2 to 1, 1 to 2).forEach { (cx, cy) ->
                drawRect(color, topLeft = Offset(ox + cx * cell, oy + cy * cell), size = Size(cell, cell))
            }
        }
    }
}

/** Положения (dp от левого верхнего угла сцены 292 × 236) и цвета искр. */
private val SPARKLES = listOf(
    Triple(250, 20, Color(0xFFF2B233)),
    Triple(222, 40, Color(0xFF4A90C8)),
    Triple(18, 150, Color(0xFFF2B233)),
    Triple(268, 120, Color(0xFFE0655A)),
    Triple(150, 12, Color(0xFFF2B233)),
    Triple(40, 20, Color(0xFF7BB86F)),
)

/**
 * Выбор цели — «новая мечта» после полученной цели. Остаток копилки
 * назван плашкой, у каждой цели — картинка, размер мечты, цена и
 * сколько осталось накопить с учётом остатка.
 */
@Composable
private fun GoalChooser(
    goals: List<GoalContent>,
    saved: Coins,
    afterClaim: Boolean,
    pet: PetSlot,
    onChoose: (String) -> Unit,
) {
    val colors = FinnyTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (saved.amount > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.coinContainer)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(colors.coin)
                        .border(2.dp, colors.warning.copy(alpha = 0.35f), CircleShape),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "В копилке ${Explanations.coins(saved)} — это старт для новой мечты!",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (afterClaim) "Выбери новую цель" else "Выбери цель",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = "На что будем копить?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceMuted,
                )
            }
            pet(96.dp, null) {}
        }
        goals.forEach { GoalOption(goal = it, saved = saved, onChoose = onChoose) }
        Text(
            text = "Совет: начни с маленькой мечты — её достичь быстрее.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Карточка цели: нажимается целиком. */
@Composable
private fun GoalOption(goal: GoalContent, saved: Coins, onChoose: (String) -> Unit) {
    val colors = FinnyTheme.colors
    val shape = RoundedCornerShape(20.dp)
    val left = (goal.price - saved.amount).coerceAtLeast(0)
    val size = goal.dreamSize()
    // Размер мечты назван словом; цвет метки его только дублирует (ТЗ 3.6).
    val (pillBg, pillFg) = when {
        goal.price <= 60 -> colors.successContainer to colors.successText
        goal.price <= 100 -> colors.warningContainer to colors.warningText
        else -> colors.errorContainer to colors.attentionText
    }
    val leftText = if (left > 0) "осталось $left" else "уже накоплено ✓"
    val leftSpoken = if (left > 0) "осталось накопить ${Explanations.coins(left)}" else "уже накоплено"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MinTouchTarget)
            .softShadow(shape)
            .clip(shape)
            .background(colors.surface)
            .clickable(role = Role.Button, onClick = { onChoose(goal.id) })
            .semantics(mergeDescendants = true) {
                contentDescription = "Выбрать цель: ${goal.title}, ${Explanations.coins(goal.price)}, " +
                    "$leftSpoken, ${size.lowercase()}"
            }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GoalPicture(goalId = goal.id, size = 64.dp, container = colors.appBackground, inset = 0.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            StatusPill(text = size, container = pillBg, content = pillFg, uppercase = false)
            Text(
                text = goal.title,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 3.dp),
            )
            Text(
                text = "${Explanations.coins(goal.price)} · $leftText",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceMuted,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        ChevronIcon()
    }
}
