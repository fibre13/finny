package ru.onefortwo.finny.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.core.graphics.toColorInt
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.onefortwo.finny.content.Accessories
import ru.onefortwo.finny.content.EventContent
import ru.onefortwo.finny.content.GoalContent
import ru.onefortwo.finny.content.ItemCategory
import ru.onefortwo.finny.content.PetPartsContent
import ru.onefortwo.finny.content.ShopItemContent
import ru.onefortwo.finny.content.effectFor
import ru.onefortwo.finny.content.withPetName
import ru.onefortwo.finny.economy.BudgetCategory
import ru.onefortwo.finny.economy.GrowthStage
import ru.onefortwo.finny.economy.StatLevel
import ru.onefortwo.finny.ui.common.FinnyDialog
import ru.onefortwo.finny.ui.common.LabeledValue
import ru.onefortwo.finny.ui.common.PetFigure
import ru.onefortwo.finny.ui.common.PrimaryButton
import ru.onefortwo.finny.ui.common.ScreenScaffold
import ru.onefortwo.finny.ui.common.SecondaryButton
import ru.onefortwo.finny.ui.common.SectionCard
import ru.onefortwo.finny.ui.common.SupportingText
import ru.onefortwo.finny.ui.common.motionAllowed
import ru.onefortwo.finny.ui.common.rememberPixelArt
import ru.onefortwo.finny.ui.state.AppState
import ru.onefortwo.finny.ui.state.EventResult
import ru.onefortwo.finny.ui.state.Explanations
import ru.onefortwo.finny.ui.state.PetReaction
import ru.onefortwo.finny.ui.state.PetVoice
import ru.onefortwo.finny.ui.state.PetReactions
import ru.onefortwo.finny.ui.state.SavingsAsk
import ru.onefortwo.finny.ui.state.Season
import ru.onefortwo.finny.ui.theme.FinnyTheme
import ru.onefortwo.finny.ui.theme.LocalBudgetColors

/*
 * Части сезона — окно события, «взять из копилки», сюрприз за
 * задания, итоги сезона с переводом остатка в копилку, праздник роста.
 */

/** Питомец ребёнка в окнах сезона. */
@Composable
internal fun ProfilePet(
    state: AppState,
    parts: PetPartsContent,
    size: Dp,
    reaction: PetReaction? = null,
    stage: GrowthStage = state.game.stage,
    happy: Boolean = true,
) {
    val profile = state.profile ?: return
    val species = parts.species.firstOrNull { it.id == profile.appearance.speciesId }
    val color = parts.colors.firstOrNull { it.id == profile.appearance.colorId }
    PetFigure(
        petName = profile.petName,
        speciesId = profile.appearance.speciesId,
        speciesTitle = species?.title ?: "Питомец",
        accessoryId = profile.appearance.accessoryId,
        accessoryTitle = Accessories.title(parts, profile.appearance.accessoryId),
        colorHex = color?.hex ?: "#CCCCCC",
        stage = stage,
        care = if (happy) StatLevel.HIGH else StatLevel.MEDIUM,
        joy = if (happy) StatLevel.HIGH else StatLevel.LOW,
        size = size,
        caption = false,
        plain = true,
        reaction = reaction,
        modifier = Modifier.width(size),
    )
}

/**
 * Погода на картинке события: небо и частицы — капли дождя, снежинки,
 * лучи солнца, вспышка молнии. При выключенных движениях частицы стоят
 * на месте. Картинка только украшает: что происходит, говорит текст события.
 */
@Composable
private fun WeatherBackdrop(weather: String?, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val sky = when (weather) {
        "rain" -> Color(0xFFCAD6E3)
        "storm" -> Color(0xFFB4BFCC)
        "cold" -> Color(0xFFB9CFE4)
        "heat" -> Color(0xFFFFEDBF)
        else -> null
    }
    if (sky == null) {
        Box(modifier) { content() }
        return
    }
    val motion = motionAllowed()
    val phase = if (motion) {
        val transition = rememberInfiniteTransition(label = "weather")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(if (weather == "cold") 3600 else 1100, easing = LinearEasing), RepeatMode.Restart),
            label = "phase",
        ).value
    } else {
        0.35f
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(sky)
            .drawBehind {
                val px = 3.dp.toPx()
                when (weather) {
                    "rain", "storm" -> for (i in 0 until 22) {
                        val x = size.width * ((i * 37 % 100) / 100f)
                        val y = size.height * (((i * 53 % 100) / 100f + phase) % 1f)
                        drawRect(Color(0xFF4F7FAE), Offset(x, y - 3 * px), Size(px * 0.8f, px * 3))
                    }

                    "cold" -> for (i in 0 until 16) {
                        val x = size.width * ((i * 41 % 100) / 100f) + px * 2 * kotlin.math.sin((phase + i * 0.13f) * 6.28f)
                        val y = size.height * (((i * 29 % 100) / 100f + phase) % 1f)
                        drawRect(Color.White, Offset(x, y), Size(px * 1.4f, px * 1.4f))
                    }

                    "heat" -> {
                        val c = Offset(size.width - 14 * px, 12 * px)
                        for (k in 0 until 8) {
                            val a = (k / 8f + phase / 8f) * 6.28f
                            val r = 7 * px + px * (if (k % 2 == 0) 2f else 1f)
                            drawRect(
                                Color(0xFFF2A93B),
                                Offset(c.x + kotlin.math.cos(a) * r - px / 2, c.y + kotlin.math.sin(a) * r - px / 2),
                                Size(px, px),
                            )
                        }
                        drawCircle(Color(0xFFF2C94C), radius = 5 * px, center = c)
                    }
                }
                // Молния — короткая вспышка в конце каждого круга.
                if (weather == "storm" && phase > 0.85f) {
                    drawRect(Color(0x55FFFFFF), Offset.Zero, size)
                }
            }
            .padding(8.dp),
    ) { content() }
}

/**
 * Рука гладит питомца: три неторопливых прохода по голове, между ними —
 * пауза. При выключенных движениях рука лежит на голове.
 */
@Composable
private fun StrokingHand(art: ru.onefortwo.finny.content.PixelArt) {
    val motion = motionAllowed()
    val x = if (motion) {
        val anim = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            repeat(3) {
                anim.animateTo(1f, tween(700))
                anim.animateTo(0f, tween(700))
                kotlinx.coroutines.delay(300)
            }
        }
        anim.value
    } else {
        0.5f
    }
    // Ладонь лежит на макушке и проходит от лба к затылку.
    Sprite(art, "hand_stroke", Modifier.offset(x = (22 + 26 * x).dp, y = (30 - 4 * kotlin.math.sin(x * 3.14f)).dp), cell = 3.dp)
}

/** Значок погоды события. */
private fun weatherIcon(weather: String?): String? = when (weather) {
    "rain" -> "item_cloud_rain"
    "cold" -> "item_snowflake"
    "heat" -> "item_sun"
    "storm" -> "item_storm"
    else -> null
}

/**
 * Окно события дня. До ответа закрыть его нельзя: событие обязательно.
 * После ответа — эмоция питомца в облачке и, у затратных, подсказка;
 * эмоция не повторяет подсказку: подсказка учит словами, эмоция — чувством.
 */
@Composable
internal fun EventDialog(
    state: AppState,
    parts: PetPartsContent,
    event: EventContent,
    result: EventResult?,
    onAnswer: (Boolean) -> Unit,
    onClose: (Boolean) -> Unit,
) {
    val art = rememberPixelArt()
    val colors = remember(art) { YardColors(art) }
    val petName = state.profile?.petName ?: "Питомец"
    val period = state.game.period.number
    val title = Season.titleOf(event, period).withPetName(petName)
    val stroking = result?.accepted == true && event.id == "pet_stroke"
    // Друг в гостях или на дне рождения — рядом второй зверёнок; двигаются оба.
    val friend = event.id in FRIEND_EVENTS
    val motion = motionAllowed()
    var hop by remember { mutableLongStateOf(1L) }
    LaunchedEffect(friend, motion) {
        if (friend && motion) while (true) {
            kotlinx.coroutines.delay(1600)
            hop++
        }
    }
    FinnyDialog(
        title = if (result == null) title else if (result.accepted) "Готово!" else "Решение принято",
        onDismiss = { if (result != null) onClose(false) },
        content = {
            if (result != null) {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = FinnyTheme.colors.onSurfaceMuted)
                Spacer(modifier = Modifier.height(8.dp))
                // Реплика питомца — над ним, уголок вниз к питомцу (он слева).
                result.emotion?.let {
                    SpeechBubble(colors, it, modifier = Modifier.padding(bottom = 2.dp), tailStart = if (stroking) 60.dp else 32.dp)
                }
            }
            WeatherBackdrop(event.weather, Modifier.fillMaxWidth()) { Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) { contentDescription = event.scene.withPetName(petName) },
            ) {
                Box {
                    ProfilePet(
                        state = state,
                        parts = parts,
                        // Когда гладят — питомец крупнее, чтобы рука на голове была хорошо видна.
                        size = if (stroking) 150.dp else 96.dp,
                        // До ответа питомец радуется гостям и играм, но не дождю и холоду.
                        happy = result?.accepted ?: (event.kind != ru.onefortwo.finny.content.EventKind.INTERNAL && event.weather == null),
                        reaction = when {
                            friend && motion -> PetReaction(PetReactions.PLAY, id = hop * 2)
                            result?.accepted == true && event.id != "pet_stroke" -> PetReaction(PetReactions.PLAY, id = 7L)
                            else -> null
                        },
                    )
                    if (stroking) StrokingHand(art)
                }
                if (friend) {
                    Spacer(modifier = Modifier.width(4.dp))
                    FriendPet(state, parts, if (motion) PetReaction(PetReactions.PLAY, id = hop * 2 + 1) else null)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    weatherIcon(event.weather)?.let { Sprite(art, it, cell = 3.dp) }
                    event.icon?.let { Sprite(art, it, cell = 4.dp) }
                }
            } }
            if (result == null) {
                if (event.cost) {
                    Text(
                        text = "Стоит ${Explanations.coins(event.price)}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            } else {
                result.hint?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .pixelPanel(colors.card, colors.card, colors.cardShadow, colors.outline, 2)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                }
                if (result.transfer > 0) {
                    Text(
                        text = "Хочешь перевести ${Explanations.coinsAccusative(result.transfer)} из «Хочу» в копилку? Тогда мечта станет ближе!",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }
        },
        actions = {
            if (result == null) {
                PrimaryButton(text = event.yes, onClick = { onAnswer(true) })
                event.no?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    SecondaryButton(text = it, onClick = { onAnswer(false) })
                }
            } else if (result.transfer > 0) {
                PrimaryButton(text = "Перевести в копилку", onClick = { onClose(true) })
                Spacer(modifier = Modifier.height(8.dp))
                SecondaryButton(text = "Оставить на балансе", onClick = { onClose(false) })
            } else {
                PrimaryButton(text = "Дальше", onClick = { onClose(false) })
            }
        },
    )
}

/** В банках не хватает: взять недостающее из копилки — с показом, как изменятся сумма и срок (ТЗ 2.5.7). */
@Composable
internal fun SavingsAskDialog(
    ask: SavingsAsk,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    /** Что покупают — «корм»; `null` — без названия. */
    what: String? = null,
    /** Мечта, на которую копят, — «самокат». */
    dream: String? = null,
) {
    val p = ask.preview
    FinnyDialog(
        title = "Не хватает монет",
        onDismiss = onCancel,
        content = {
            Text(
                text = (if (what != null) "Чтобы купить $what, тебе" else "Тебе") +
                    " не хватает ${Explanations.coins(ask.payment.fromSavings)}.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "Можно взять их из копилки — но тогда " +
                    (if (dream != null) "на $dream останется меньше." else "мечта станет чуть дальше."),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 6.dp),
            )
            Spacer(modifier = Modifier.height(10.dp))
            LabeledValue("В копилке сейчас", Explanations.coins(p.savedBefore))
            LabeledValue("Станет", Explanations.coins(p.savedAfter))
        },
        actions = {
            PrimaryButton(text = "Да, взять из копилки", onClick = onConfirm)
            Spacer(modifier = Modifier.height(8.dp))
            SecondaryButton(text = "Не покупать", onClick = onCancel)
        },
    )
}

/** События, где рядом с питомцем стоит друг. */
private val FRIEND_EVENTS = setOf("friend_visit", "friend_birthday")

/** Друг питомца — зверёнок другого вида и окраса; радуется вместе с ним. */
@Composable
private fun FriendPet(state: AppState, parts: PetPartsContent, reaction: PetReaction?) {
    val mine = state.profile?.appearance?.speciesId
    val species = parts.species.firstOrNull { it.id != mine } ?: return
    val color = parts.colors.firstOrNull { it.id != state.profile?.appearance?.colorId } ?: parts.colors.first()
    PetFigure(
        petName = species.title,
        speciesId = species.id,
        speciesTitle = species.title,
        accessoryId = "none",
        accessoryTitle = "",
        colorHex = color.hex,
        stage = GrowthStage.BABY,
        care = ru.onefortwo.finny.economy.StatLevel.HIGH,
        joy = ru.onefortwo.finny.economy.StatLevel.HIGH,
        size = 80.dp,
        caption = false,
        plain = true,
        reaction = reaction,
        modifier = Modifier.width(80.dp),
    )
}

/** Сюрприз за задания: в лавке появился новый товар. */
@Composable
internal fun SurpriseDialog(
    item: ShopItemContent,
    solved: Int,
    petName: String,
    onDismiss: () -> Unit,
    /** Товары, открытые вместе с главным сюрпризом: еда, билеты, аптечка. */
    also: List<ShopItemContent> = emptyList(),
) {
    val art = rememberPixelArt()
    val all = listOf(item) + also
    FinnyDialog(
        title = "Сюрприз!",
        onDismiss = onDismiss,
        content = {
            Text(text = "Ты выполнил ${Explanations.tasks(Season.TASKS_PER_SURPRISE)}!", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "В лавке появилось: " + all.joinToString(" и ") { "«${it.title}»" },
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 6.dp),
            )
            Row(modifier = Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                all.forEach { shown ->
                    shown.icon?.let { Sprite(art, it, cell = 4.dp) }
                    Spacer(modifier = Modifier.width(12.dp))
                }
            }
            Text(
                text = "Теперь ты сможешь купить их для своего питомца!",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 10.dp),
            )
        },
        actions = { PrimaryButton(text = "Ура!", onClick = onDismiss) },
    )
}

/**
 * Экранное время на сегодня вышло. Питомец прощается до завтра;
 * незаконченные дела дня продолжатся с того же места.
 */
@Composable
internal fun TimeUpDialog(state: AppState, parts: PetPartsContent, onClose: () -> Unit) {
    val art = rememberPixelArt()
    val colors = remember(art) { YardColors(art) }
    FinnyDialog(
        title = "На сегодня всё!",
        onDismiss = onClose,
        content = {
            // Реплика — над питомцем, уголок вниз к нему.
            SpeechBubble(colors, PetVoice.of(state.game.stage, "Пока-пока! Увидимся завтра!"), modifier = Modifier.padding(bottom = 2.dp), tailStart = 56.dp)
            ProfilePet(state, parts, 140.dp)
            Text(
                text = "Экранное время на сегодня закончилось. Приходи завтра — продолжим с того же места.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 10.dp),
            )
        },
        actions = { PrimaryButton(text = "Пока-пока!", onClick = onClose) },
    )
}

/** Что говорит питомец, когда ребёнок вернулся после пропущенного сезона. */
private val MISSED_PHRASES = listOf(
    "Я тебя ждал.",
    "Я так скучал!",
    "Я же тебя жду!",
    "Не забывай про меня!",
    "Я хочу с тобой играть!",
)

/** Сезон закрылся сам — ребёнок не приходил шесть дней. */
@Composable
internal fun MissedDialog(state: AppState, parts: PetPartsContent, onDismiss: () -> Unit) {
    val art = rememberPixelArt()
    val colors = remember(art) { YardColors(art) }
    FinnyDialog(
        title = "С возвращением!",
        onDismiss = onDismiss,
        content = {
            // Питомец не говорит о монетах: только о том, что ждал (разные слова от сезона к сезону).
            val missed = MISSED_PHRASES[(state.extras.season - 1).mod(MISSED_PHRASES.size)]
            SpeechBubble(colors, "$missed Давай начнём новый сезон?", modifier = Modifier.padding(bottom = 2.dp), tailStart = 32.dp)
            ProfilePet(state, parts, 96.dp)
            // О монетах — отдельной строкой, а не словами питомца.
            Text(
                text = "Пока тебя не было, новые сезоны не начинались и монеты не приходили. " +
                    "Заходи регулярно, чтобы не пропускать сезоны.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 10.dp),
            )
        },
        actions = { PrimaryButton(text = "Давай!", onClick = onDismiss) },
    )
}

/**
 * Монеты перелетают из банков к мечте: [count] монет по дуге, одна за
 * другой. Без движений — сразу [onDone].
 */
@Composable
internal fun CoinTransfer(fromIcon: String, toIcon: String, count: Int, playing: Boolean, onDone: () -> Unit) {
    val art = rememberPixelArt()
    val motion = motionAllowed()
    val coins = count.coerceIn(1, 8)
    val progress = remember(coins) { List(coins) { Animatable(0f) } }
    LaunchedEffect(playing) {
        if (!playing) return@LaunchedEffect
        if (!motion) {
            onDone()
            return@LaunchedEffect
        }
        coroutineScopeLaunchAll(progress)
        onDone()
    }
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(72.dp)) {
        val width = maxWidth
        Sprite(art, fromIcon, Modifier.align(Alignment.BottomStart), cell = 3.dp)
        Sprite(art, toIcon, Modifier.align(Alignment.BottomEnd), cell = 3.dp)
        if (playing) {
            progress.forEach { p ->
                val t = p.value
                if (t in 0.001f..0.999f) {
                    Sprite(
                        art, "coin_0",
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = (width - 36.dp) * t + 12.dp, y = -(40.dp * sin(PI * t).toFloat())),
                        cell = 2.dp,
                    )
                }
            }
        }
    }
}

/** Монеты летят по очереди: следующая стартует через 110 мс после предыдущей. */
private suspend fun coroutineScopeLaunchAll(progress: List<Animatable<Float, *>>) {
    kotlinx.coroutines.coroutineScope {
        progress.forEachIndexed { i, p ->
            launch {
                delay(i * 110L)
                p.snapTo(0f)
                p.animateTo(1f, tween(650))
            }
        }
    }
}

/**
 * Итоги сезона: план и факт по банкам, что осталось и что накоплено.
 * Остаток можно перевести в копилку — монеты перелетают к мечте. Затем
 * «Поиграть» или «Уложить спать»: питомец прощается до нового сезона.
 */
@Composable
fun SeasonResultScreen(
    state: AppState,
    parts: PetPartsContent,
    goal: GoalContent?,
    onTransfer: () -> Unit,
    onPlay: () -> Unit,
    onSleep: () -> Unit,
) {
    val art = rememberPixelArt()
    val colors = remember(art) { YardColors(art) }
    val x = state.extras
    val palette = LocalBudgetColors.current
    val leftover = x.needsJar + x.wantsJar + state.freeCoins
    var offerClosed by rememberSaveable { mutableStateOf(false) }
    var flying by remember { mutableStateOf(false) }
    var saying by remember { mutableStateOf<String?>("Все дела сделаны!") }
    var reactionId by remember { mutableLongStateOf(0L) }
    val scope = rememberCoroutineScope()

    ScreenScaffold(
        eyebrow = "Сезон ${x.season}",
        title = "Итоги сезона",
        balance = state.game.balance,
        onBack = null,
        bottomPadding = 16.dp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // План и факт — цветными полосами, без текстовых пояснений: полосы читаются сразу.
            SectionCard(
                title = "Мой бюджет",
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                bottomSpacing = 0.dp,
            ) {
                BudgetBars(
                    plannedNeeds = x.plannedNeeds, spentNeeds = x.spentNeeds,
                    plannedWants = x.plannedWants, spentWants = x.spentWants,
                    plannedSavings = x.plannedSavings, deposited = x.deposited,
                )
            }
            SectionCard(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                bottomSpacing = 0.dp,
            ) {
                Column {
                    LabeledValue("Осталось монет всего", Explanations.coins(state.game.balance))
                    LabeledValue("Накоплено за сезон", Explanations.coins(x.deposited))
                    LabeledValue("Осталось на нужное и «Хочу»", Explanations.coins(leftover))
                }
            }

            if (leftover > 0 && goal != null && !offerClosed) {
                SectionCard(
                    title = "Отложить остаток?",
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    bottomSpacing = 0.dp,
                ) {
                    Column {
                        Text(
                            text = "Остаток — ${Explanations.coins(leftover)}. Ты планировал потратить эти монеты на нужное " +
                                "и «Хочу», но не потратил. Хочешь перевести их в копилку? Тогда твоя мечта станет ближе!",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        CoinTransfer(
                            fromIcon = "item_moneybag",
                            toIcon = goalIcon(goal.id),
                            count = leftover,
                            playing = flying,
                            onDone = {
                                onTransfer()
                                flying = false
                                offerClosed = true
                                saying = "Ура! Мечта стала ближе!"
                                reactionId++
                            },
                        )
                        PrimaryButton(text = "Да, в копилку", enabled = !flying, onClick = { flying = true })
                        Spacer(modifier = Modifier.height(8.dp))
                        SecondaryButton(text = "Оставить на балансе", onClick = { offerClosed = true })
                    }
                }
            }

            // Реплика — над питомцем, уголок вниз к нему.
            saying?.let { SpeechBubble(colors, PetVoice.of(state.game.stage, it), modifier = Modifier.padding(bottom = 2.dp), tailStart = 40.dp) }
            ProfilePet(
                state = state,
                parts = parts,
                size = 110.dp,
                reaction = if (reactionId > 0) PetReaction(PetReactions.PLAY, id = reactionId) else null,
            )
            SecondaryButton(
                text = "Играть",
                onClick = {
                    onPlay()
                    reactionId++
                    saying = "Ура! Как весело!"
                },
            )
            PrimaryButton(
                text = "Уложить спать",
                enabled = !flying,
                onClick = {
                    saying = "Пока-пока! Увидимся в новом сезоне!"
                    scope.launch {
                        delay(1600)
                        onSleep()
                    }
                },
            )
        }
    }
}

/** Картинка мечты для перелёта монет. */
private fun goalIcon(goalId: String): String = when (goalId) {
    "scooter" -> "item_scooter"
    "aquarium" -> "item_fish"
    else -> "item_present"
}

/**
 * Праздник роста в три сцены: радость, превращение, поздравление.
 * Показывается один раз на стадию.
 */
@Composable
fun GrowthCelebrationScreen(state: AppState, parts: PetPartsContent, onDone: () -> Unit) {
    val art = rememberPixelArt()
    val colors = remember(art) { YardColors(art) }
    var scene by rememberSaveable { mutableIntStateOf(0) }
    val stage = state.game.stage
    val previous = if (stage == GrowthStage.ADULT) GrowthStage.TEEN else GrowthStage.BABY
    // Движения выключены в разделе для взрослого — питомец сразу большой и не прыгает.
    val motion = motionAllowed()
    val grow = remember { Animatable(if (motion) 0f else 1f) }
    var jumps by remember { mutableLongStateOf(1L) }
    LaunchedEffect(scene) {
        if (scene == 1 && motion) {
            grow.snapTo(0f)
            grow.animateTo(1f, tween(1200))
        }
        jumps++
    }
    MeadowBackground(art, grassFrom = 0.55f) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(40.dp))
                Row {
                    Sprite(art, "yard_balloon_red", cell = 3.dp)
                    Spacer(modifier = Modifier.width(40.dp))
                    Sprite(art, "yard_balloon_yellow", cell = 3.dp)
                    Spacer(modifier = Modifier.width(40.dp))
                    Sprite(art, "yard_balloon_blue", cell = 3.dp)
                }
                val bubble = when (scene) {
                    0 -> if (stage == GrowthStage.ADULT) "Ура! Ты накопил на все три мечты!" else "Ура! Ты накопил на целых две мечты!"
                    1 -> "Смотри, как я вырос!"
                    else -> null
                }
                bubble?.let { SpeechBubble(colors, it, modifier = Modifier.padding(top = 12.dp)) }
                Box(modifier = Modifier.height(210.dp), contentAlignment = Alignment.BottomCenter) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        if (scene >= 1) {
                            Sprite(art, "item_backpack", Modifier.graphicsLayer { alpha = if (scene == 1) grow.value else 1f }, cell = 3.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Box(
                            modifier = Modifier.graphicsLayer {
                                val s = if (scene == 1) 0.8f + 0.2f * grow.value else 1f
                                scaleX = s
                                scaleY = s
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                            },
                        ) {
                            ProfilePet(
                                state = state,
                                parts = parts,
                                size = 170.dp,
                                stage = if (scene == 0) previous else stage,
                                reaction = if (motion) PetReaction(PetReactions.PLAY, id = jumps) else null,
                            )
                        }
                        if (scene >= 1) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Sprite(art, "item_bow", Modifier.graphicsLayer { alpha = if (scene == 1) grow.value else 1f }, cell = 3.dp)
                                Sprite(art, "item_notebook", Modifier.graphicsLayer { alpha = if (scene == 1) grow.value else 1f }, cell = 3.dp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (scene == 2) {
                    val who = if (stage == GrowthStage.ADULT) "взрослым" else "подростком"
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pixelPanel(colors.card, colors.card, colors.cardShadow, colors.outline, 2)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    ) {
                        Text(
                            text = "Твой питомец вырос!",
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().semantics { heading() },
                        )
                        Text(
                            text = "Ты накопил на мечты — и твой питомец стал $who! Он уже большой и ходит в школу. " +
                                "Теперь он хочет учиться и узнавать новое. Впереди — новые события и новые мечты!",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        Text(
                            text = "Мама-питомец тобой гордится!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = FinnyTheme.colors.onSurfaceMuted,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    PixelButton(text = "К новым приключениям!", onClick = onDone, pulse = true)
                } else {
                    PixelButton(text = "Дальше", onClick = { scene += 1 }, pulse = true)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
