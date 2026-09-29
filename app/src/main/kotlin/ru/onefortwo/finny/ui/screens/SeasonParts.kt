package ru.onefortwo.finny.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import ru.onefortwo.finny.ui.state.PetReactions
import ru.onefortwo.finny.ui.state.SavingsAsk
import ru.onefortwo.finny.ui.state.Season
import ru.onefortwo.finny.ui.theme.FinnyTheme
import ru.onefortwo.finny.ui.theme.LocalBudgetColors

/*
 * ТЕСТ 3: части сезона — окно события, «взять из копилки», сюрприз за
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
    FinnyDialog(
        title = if (result == null) title else if (result.accepted) "Готово!" else "Решение принято",
        onDismiss = { if (result != null) onClose(false) },
        content = {
            if (result != null) {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = FinnyTheme.colors.onSurfaceMuted)
                Spacer(modifier = Modifier.height(8.dp))
            }
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) { contentDescription = event.scene.withPetName(petName) },
            ) {
                ProfilePet(
                    state = state,
                    parts = parts,
                    size = 96.dp,
                    happy = result?.accepted ?: (event.kind != ru.onefortwo.finny.content.EventKind.INTERNAL),
                    reaction = if (result?.accepted == true) PetReaction(PetReactions.PLAY, id = 7L) else null,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    weatherIcon(event.weather)?.let { Sprite(art, it, cell = 3.dp) }
                    event.icon?.let { Sprite(art, it, cell = 4.dp) }
                }
            }
            if (result == null) {
                if (event.cost) {
                    val jar = if (event.category == ItemCategory.WANTS) BudgetCategory.WANTS.displayName else BudgetCategory.NEEDS.displayName
                    Text(
                        text = "Стоит ${Explanations.coins(event.price)}. Монеты берутся из банка «$jar».",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            } else {
                result.emotion?.let {
                    SpeechBubble(colors, it, modifier = Modifier.padding(top = 10.dp))
                }
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
                        text = "Хочешь перевести ${Explanations.coinsAccusative(result.transfer)} из банка «Хочу» в копилку? Тогда мечта станет ближе!",
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
internal fun SavingsAskDialog(ask: SavingsAsk, onConfirm: () -> Unit, onCancel: () -> Unit) {
    val p = ask.preview
    FinnyDialog(
        title = "Взять из копилки?",
        onDismiss = onCancel,
        content = {
            Text(
                text = "В банках не хватает ${Explanations.coins(ask.payment.fromSavings)}. " +
                    "Копилку лучше не трогать, но на нужное можно.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(10.dp))
            LabeledValue("Сейчас в копилке", Explanations.coins(p.savedBefore))
            LabeledValue("Станет", Explanations.coins(p.savedAfter))
            SupportingText(Explanations.forecast(p.forecastAfter), modifier = Modifier.padding(top = 6.dp))
        },
        actions = {
            PrimaryButton(text = "Взять из копилки", onClick = onConfirm)
            Spacer(modifier = Modifier.height(8.dp))
            SecondaryButton(text = "Не брать", onClick = onCancel)
        },
    )
}

/** Сюрприз за задания: в лавке появился новый товар. */
@Composable
internal fun SurpriseDialog(item: ShopItemContent, solved: Int, petName: String, onDismiss: () -> Unit) {
    val art = rememberPixelArt()
    FinnyDialog(
        title = "Сюрприз!",
        onDismiss = onDismiss,
        content = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                item.icon?.let { Sprite(art, it, cell = 4.dp) }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Ты выполнил ${Explanations.tasks(solved)}! В лавке появилось: «${item.title}».",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(
                text = item.effectFor(petName) + ".",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 10.dp),
            )
        },
        actions = { PrimaryButton(text = "Ура!", onClick = onDismiss) },
    )
}

/** Сезон закрылся сам — ребёнок не приходил шесть дней. */
@Composable
internal fun MissedDialog(state: AppState, parts: PetPartsContent, onDismiss: () -> Unit) {
    val art = rememberPixelArt()
    val colors = remember(art) { YardColors(art) }
    FinnyDialog(
        title = "С возвращением!",
        onDismiss = onDismiss,
        content = {
            ProfilePet(state, parts, 96.dp)
            SpeechBubble(colors, "Я тебя ждал. Давай начнём новый сезон?", modifier = Modifier.padding(top = 8.dp))
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
            SectionCard(
                title = "План и факт",
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                bottomSpacing = 0.dp,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    val (needsNote, wantsNote) = Season.borrowNotes(x)
                    PlanFactRow(BudgetCategory.NEEDS, x.plannedNeeds, x.spentNeeds, palette.needs, needsNote)
                    PlanFactRow(BudgetCategory.WANTS, x.plannedWants, x.spentWants, palette.wants, wantsNote)
                    PlanFactRow(BudgetCategory.SAVINGS, x.plannedSavings, x.deposited, palette.savings)
                }
            }
            SectionCard(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                bottomSpacing = 0.dp,
            ) {
                Column {
                    LabeledValue("Осталось монет всего", Explanations.coins(state.game.balance))
                    LabeledValue("Накоплено за сезон", Explanations.coins(x.deposited))
                    LabeledValue("Осталось в банках", Explanations.coins(leftover))
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

            Row(verticalAlignment = Alignment.Bottom) {
                ProfilePet(
                    state = state,
                    parts = parts,
                    size = 110.dp,
                    reaction = if (reactionId > 0) PetReaction(PetReactions.PLAY, id = reactionId) else null,
                )
                saying?.let { SpeechBubble(colors, it, modifier = Modifier.padding(start = 8.dp, bottom = 40.dp)) }
            }
            SecondaryButton(
                text = "Поиграть",
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
    val grow = remember { Animatable(0f) }
    var jumps by remember { mutableLongStateOf(1L) }
    LaunchedEffect(scene) {
        if (scene == 1) {
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
                    0 -> "Ура! Ты накопил на целых две мечты!"
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
                                reaction = PetReaction(PetReactions.PLAY, id = jumps),
                            )
                        }
                        if (scene >= 1) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Sprite(art, "item_cap", Modifier.graphicsLayer { alpha = if (scene == 1) grow.value else 1f }, cell = 3.dp)
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
