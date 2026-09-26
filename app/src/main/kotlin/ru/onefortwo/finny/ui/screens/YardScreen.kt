package ru.onefortwo.finny.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.onefortwo.finny.content.GoalContent
import ru.onefortwo.finny.content.PetPartsContent
import ru.onefortwo.finny.content.PixelArt
import ru.onefortwo.finny.content.TaskContent
import ru.onefortwo.finny.economy.PetStatKind
import ru.onefortwo.finny.economy.StatLevel
import ru.onefortwo.finny.ui.common.FeedbackCard
import ru.onefortwo.finny.ui.common.PetFigure
import ru.onefortwo.finny.ui.common.PixelImage
import ru.onefortwo.finny.ui.common.STATS_PANEL_HEIGHT
import ru.onefortwo.finny.ui.common.STATS_PANEL_WIDTH
import ru.onefortwo.finny.ui.common.SceneStats
import ru.onefortwo.finny.ui.common.composeGoal
import ru.onefortwo.finny.ui.common.composeScene
import ru.onefortwo.finny.ui.common.composeStats
import ru.onefortwo.finny.ui.common.motionAllowed
import ru.onefortwo.finny.ui.common.pixelImage
import ru.onefortwo.finny.ui.common.rememberPetMotion
import ru.onefortwo.finny.ui.common.rememberPixelArt
import ru.onefortwo.finny.ui.common.rememberPulse
import ru.onefortwo.finny.ui.common.sceneCoreLeft
import ru.onefortwo.finny.ui.common.sceneFullWidth
import ru.onefortwo.finny.ui.state.AppState
import ru.onefortwo.finny.ui.state.Explanations
import ru.onefortwo.finny.ui.state.PetReaction
import ru.onefortwo.finny.ui.state.PetReactions
import ru.onefortwo.finny.ui.theme.FinnyTheme
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * ТЕСТ (ветка test/kopilka-a): главная — двор питомца целиком в пиксельной
 * графике. Разделы — предметы на лугу с табличками: лавка «Покупки»,
 * школьная доска «Задания» (следующее задание мелом), доска объявлений
 * «План», книга на пеньке «Словарик», сундучок «Копилка». Нажатие на
 * предмет — питомец подбегает к нему, затем открывается раздел; на
 * питомца — его раздел (гардероб); на табличку «День N · имя» — «Мой
 * прогресс». Нижней панели нет. Текст — Inter: читается и масштабируется.
 */

/** Размер клетки предметов и рамок. */
private val CELL = 2.dp

/** Клетка питомца — крупнее предметов: он главный на сцене. */
private val PET_CELL = 3.dp

/** Цвета рамок из палитры пиксельной графики. */
private class YardColors(private val art: PixelArt) {
    private fun c(ch: Char) = Color(art.colors[art.indexOf(ch)])
    val outline = c('K')
    val card = c('c')
    val cardShadow = c('z')
    val sky = c('A')
    val grass = c('g')
    val grassDark = c('e')
    val green = c('E')
    val greenLight = c('g')
    val greenDark = c('D')
    val paper = c('y')
    val paperShadow = c('O')
    val coral = c('R')
    val coin = c('Y')
}

/**
 * Пиксельная рамка: контур со срезанными углами, светлая кромка сверху
 * и тёмная снизу — объёмная табличка или кнопка.
 */
private fun Modifier.pixelPanel(
    fill: Color,
    light: Color,
    shadow: Color,
    outline: Color,
    shadowCells: Int = 1,
): Modifier = drawBehind {
    val c = max(1f, floor(CELL.toPx()))
    val w = size.width
    val h = size.height
    drawRect(outline, Offset(c, 0f), Size(w - 2 * c, c))
    drawRect(outline, Offset(c, h - c), Size(w - 2 * c, c))
    drawRect(outline, Offset(0f, c), Size(c, h - 2 * c))
    drawRect(outline, Offset(w - c, c), Size(c, h - 2 * c))
    drawRect(fill, Offset(c, c), Size(w - 2 * c, h - 2 * c))
    drawRect(light, Offset(c, c), Size(w - 2 * c, c))
    drawRect(shadow, Offset(c, h - c - shadowCells * c), Size(w - 2 * c, shadowCells * c))
}

/** Спрайт из `art/pixel` в клетках [cell]. Декоративный: смысл несёт кнопка. */
@Composable
private fun Sprite(art: PixelArt, id: String, modifier: Modifier = Modifier, cell: Dp = CELL) {
    val sprite = art.sprite(id) ?: return
    val image = remember(art, id) { composeGoal(art, id)?.let { pixelImage(it, sprite.width) } } ?: return
    PixelImage(
        image = image,
        modifier = modifier.size(cell * sprite.width, cell * sprite.height).clearAndSetSemantics { },
    )
}

@Composable
fun YardScreen(
    state: AppState,
    parts: PetPartsContent,
    activeTask: TaskContent?,
    goal: GoalContent?,
    today: String,
    onDismissMessage: () -> Unit,
    onOpenPlan: () -> Unit,
    onOpenShop: () -> Unit,
    onOpenSavings: () -> Unit,
    onOpenGlossary: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenPet: () -> Unit,
    onOpenProgress: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenAdult: () -> Unit,
    onFinishPeriod: () -> Unit,
    reaction: PetReaction? = null,
    onReactionPlayed: (Long) -> Unit = {},
) {
    val profile = state.profile ?: return
    val game = state.game
    val art = rememberPixelArt()
    val colors = remember(art) { YardColors(art) }
    val scope = rememberCoroutineScope()
    val motion = motionAllowed()

    // Положения предметов и питомца на экране — для перебежки.
    val places = remember { HashMap<String, Offset>() }
    var petCenter by remember { mutableStateOf(Offset.Zero) }
    val hop = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val petAlpha = remember { Animatable(1f) }
    var busy by remember { mutableStateOf(false) }
    var hopping by remember { mutableStateOf(false) }
    var snoring by remember { mutableStateOf(false) }
    // Вход в палатку в координатах сцены и положение сцены на экране:
    // запоминаются при рисовании и раскладке, нужны только по нажатию.
    val tentDoor = remember { floatArrayOf(-1f, -1f) }
    val sceneOrigin = remember { floatArrayOf(0f, 0f) }

    fun runTo(target: Offset?, action: () -> Unit) {
        if (busy) return
        if (!motion || target == null) {
            action()
            return
        }
        busy = true
        hopping = true
        scope.launch {
            hop.animateTo((target - petCenter) * 0.85f, tween(480, easing = FastOutSlowInEasing))
            action()
            hop.snapTo(Offset.Zero)
            hopping = false
            busy = false
        }
    }

    val dayFinished = state.isDayFinished(today)
    val reason = when {
        dayFinished -> "Закончить день можно завтра."
        !game.period.isPlanConfirmed -> "Сначала составь план."
        !game.period.canFinish -> "Сначала купи или отложи монеты."
        else -> null
    }

    fun finishDay() {
        if (busy) return
        val house = state.hasScenery("house")
        if (!motion) {
            onFinishPeriod()
            return
        }
        busy = true
        scope.launch {
            if (house && tentDoor[0] >= 0) {
                hopping = true
                val door = Offset(sceneOrigin[0] + tentDoor[0], sceneOrigin[1] + tentDoor[1])
                hop.animateTo(door - petCenter, tween(650, easing = FastOutSlowInEasing))
                petAlpha.animateTo(0f, tween(220))
                hopping = false
            }
            snoring = true
            delay(1600)
            onFinishPeriod()
            snoring = false
            hop.snapTo(Offset.Zero)
            petAlpha.snapTo(1f)
            busy = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.grass),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            // --- Сцена без питомца, шапка — поверх её неба ---------------------
            Backdrop(
                art = art,
                state = state,
                snoring = snoring && state.hasScenery("house"),
                tentDoor = tentDoor,
                origin = sceneOrigin,
            ) {
                Header(
                    art = art,
                    colors = colors,
                    day = game.period.number,
                    demo = state.isDemo,
                    petName = profile.petName,
                    balance = game.balance.amount,
                    onOpenProgress = onOpenProgress,
                    onOpenHelp = onOpenHelp,
                    onOpenAdult = onOpenAdult,
                )
            }

            // --- Луг: предметы-разделы и питомец -------------------------------
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    YardObject(
                        art, colors, "yard_shop", "Покупки",
                        description = "Покупки: лавка для питомца",
                        onPlaced = { places["shop"] = it },
                        onClick = { runTo(places["shop"], onOpenShop) },
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    val board = when {
                        state.isTimeUp(today) -> "На сегодня хватит. Приходи завтра!"
                        activeTask != null -> activeTask.title
                        else -> "Заданий пока нет."
                    }
                    YardObject(
                        art, colors, "yard_tasks", "Задания",
                        description = "Задания. На доске: $board",
                        onPlaced = { places["tasks"] = it },
                        onClick = { runTo(places["tasks"], onOpenTasks) },
                        overlay = {
                            Text(
                                text = board,
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.card,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .padding(start = CELL * 6, top = CELL * 5, end = CELL * 5)
                                    .width(CELL * 54),
                            )
                        },
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().zIndex(1f),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    val planMissing = !game.period.isPlanConfirmed && !dayFinished
                    YardObject(
                        art, colors, "yard_plan", "План",
                        description = "План на день",
                        state = if (planMissing) "не составлен" else null,
                        onPlaced = { places["plan"] = it },
                        onClick = { runTo(places["plan"], onOpenPlan) },
                        overlay = { if (planMissing) Badge(colors, Modifier.align(Alignment.TopEnd)) },
                    )
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.BottomCenter) {
                        Pet(
                            state = state,
                            parts = parts,
                            reaction = reaction,
                            onReactionPlayed = onReactionPlayed,
                            hopping = hopping,
                            sleeping = snoring,
                            desire = desireOf(game.pet.care.level, game.pet.joy.level),
                            art = art,
                            colors = colors,
                            modifier = Modifier
                                .offset { IntOffset(hop.value.x.roundToInt(), hop.value.y.roundToInt()) }
                                .alpha(petAlpha.value)
                                .onGloballyPositioned { petCenter = it.boundsInRoot().center },
                            onClick = { if (!busy) onOpenPet() },
                            snoringHere = snoring && !state.hasScenery("house"),
                        )
                    }
                    YardObject(
                        art, colors, "yard_glossary", "Словарик",
                        description = "Словарик: финансовые слова",
                        onPlaced = { places["glossary"] = it },
                        onClick = { runTo(places["glossary"], onOpenGlossary) },
                    )
                }
                ChestRow(
                    art = art,
                    colors = colors,
                    goal = goal,
                    saved = game.savings.saved.amount,
                    price = game.savings.goal?.price?.amount,
                    onPlaced = { places["savings"] = it },
                    onClick = { runTo(places["savings"], onOpenSavings) },
                )
                FinishDayButton(colors = colors, reason = reason, onClick = { finishDay() })
                Spacer(modifier = Modifier.height(10.dp))
                Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }

        val message = state.message
        if (message != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 18.dp, vertical = 8.dp),
            ) {
                FeedbackCard(message = message, onDismiss = onDismissMessage)
            }
        }
    }
}

/** Желание питомца в облачке: сначала еда, потом игра; довольному — ничего. */
private fun desireOf(care: StatLevel, joy: StatLevel): String? = when {
    care != StatLevel.HIGH -> "icon_care"
    joy != StatLevel.HIGH -> "yard_ball"
    else -> null
}

@Composable
private fun Header(
    art: PixelArt,
    colors: YardColors,
    day: Int,
    demo: Boolean,
    petName: String,
    balance: Int,
    onOpenProgress: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenAdult: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .pixelPanel(colors.card, colors.card, colors.cardShadow, colors.outline, 2)
                .clickable(role = Role.Button, onClick = onOpenProgress)
                .semantics(mergeDescendants = true) {
                    contentDescription = "Мой прогресс. День $day${if (demo) ", демо" else ""}, $petName"
                }
                .padding(start = 10.dp, end = 8.dp, top = 4.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "День $day" + if (demo) " · демо" else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = FinnyTheme.colors.onSurfaceMuted,
                )
                Text(
                    text = petName,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Sprite(art, "yard_icon_progress")
        }
        Spacer(modifier = Modifier.width(10.dp))
        IconPanelButton(art, colors, "yard_icon_help", "Как играть", onOpenHelp)
        Spacer(modifier = Modifier.width(6.dp))
        IconPanelButton(art, colors, "yard_icon_adult", "Для взрослого", onOpenAdult)
        Spacer(modifier = Modifier.width(6.dp))
        Row(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .pixelPanel(colors.card, colors.card, colors.cardShadow, colors.outline, 2)
                .semantics(mergeDescendants = true) { contentDescription = "Баланс: ${Explanations.coins(balance)}" }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Sprite(art, "coin_0", cell = 3.dp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "$balance", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun IconPanelButton(art: PixelArt, colors: YardColors, icon: String, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .pixelPanel(colors.card, colors.card, colors.cardShadow, colors.outline, 2)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Sprite(art, icon, modifier = Modifier.padding(bottom = CELL))
    }
}

/** Сцена без питомца; при храпе над палаткой поднимаются «Z». */
@Composable
private fun Backdrop(
    art: PixelArt,
    state: AppState,
    snoring: Boolean,
    tentDoor: FloatArray,
    origin: FloatArray,
    header: @Composable () -> Unit,
) {
    val game = state.game
    val motion = rememberPetMotion(art.animation, game.pet.care.level, game.pet.joy.level)
    val house = state.hasScenery("house")
    val stickers = state.hasScenery("stickers")
    val goalId = state.achievedGoalIds.lastOrNull()
    val empty = remember(art) { IntArray(art.petSize * art.petSize) }
    val image = remember(art, house, stickers, goalId, motion.sky) {
        pixelImage(composeScene(art, house, goalId, empty, skyFrame = motion.sky, stickers = stickers), sceneFullWidth(art))
    }
    val statsImage = remember(art, game.pet.care.value, game.pet.joy.value) {
        pixelImage(
            composeStats(
                art,
                SceneStats(
                    care = game.pet.care.value,
                    careLow = game.pet.care.level == StatLevel.LOW,
                    joy = game.pet.joy.value,
                    joyLow = game.pet.joy.level == StatLevel.LOW,
                ),
            ),
            STATS_PANEL_WIDTH,
        )
    }
    val statsAt = art.sceneAnchors["stats"]
    val tentAt = art.sceneAnchors["tent"]
    val tent = art.sprite("tent")
    val zImage = remember(art) { composeGoal(art, "yard_z")?.let { pixelImage(it, 5) } }
    val zz = rememberInfiniteTransition(label = "snore")
    val zPhase by zz.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart), label = "z")
    val careLabel = Explanations.statLabel(PetStatKind.CARE, game.pet.care)
    val joyLabel = Explanations.statLabel(PetStatKind.JOY, game.pet.joy)

    Box(modifier = Modifier.background(Color(art.colors[art.indexOf('A')]))) {
      Column {
        // Полоса неба под шапку: шапка не закрывает панель самочувствия.
        Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Spacer(modifier = Modifier.height(58.dp))
        PixelImage(
            image = image,
            background = Color(art.colors[art.indexOf('A')]),
            coreLeft = sceneCoreLeft(art),
            coreWidth = art.sceneWidth,
            overlay = { scale, left, top ->
                if (statsAt != null) {
                    val cell = scale * 3 / 2
                    drawImage(
                        image = statsImage,
                        srcOffset = IntOffset.Zero,
                        srcSize = IntSize(STATS_PANEL_WIDTH, STATS_PANEL_HEIGHT),
                        dstOffset = IntOffset(left + statsAt.x * scale, top + statsAt.y * scale),
                        dstSize = IntSize(STATS_PANEL_WIDTH * cell, STATS_PANEL_HEIGHT * cell),
                        filterQuality = FilterQuality.None,
                    )
                }
                if (tentAt != null && tent != null) {
                    // Вход в палатку — середина нижней части; в координатах корня.
                    val doorX = left + (tentAt.x + tent.width / 2) * scale
                    val doorY = top + (tentAt.y + tent.height - 6) * scale
                    tentDoor[0] = doorX.toFloat()
                    tentDoor[1] = doorY.toFloat()
                    if (snoring && zImage != null) {
                        for (k in 0 until 3) {
                            val p = (zPhase + k / 3f) % 1f
                            val size = (scale * (1 + k % 2)).coerceAtLeast(1)
                            drawImage(
                                image = zImage,
                                srcOffset = IntOffset.Zero,
                                srcSize = IntSize(5, 5),
                                dstOffset = IntOffset(
                                    doorX + ((10 + 8 * p + 4 * sin(p * 6f)) * scale).toInt(),
                                    doorY - ((30 + 40 * p) * scale).toInt(),
                                ),
                                dstSize = IntSize(5 * size, 5 * size),
                                alpha = (1f - p).coerceIn(0f, 1f),
                                filterQuality = FilterQuality.None,
                            )
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(art.sceneWidth.toFloat() / art.sceneHeight)
                .onGloballyPositioned {
                    origin[0] = it.boundsInRoot().left
                    origin[1] = it.boundsInRoot().top
                }
                .semantics {
                    contentDescription = "Двор. ${Explanations.statName(PetStatKind.CARE)}: $careLabel, " +
                        "${game.pet.care.value} из 100. ${Explanations.statName(PetStatKind.JOY)}: $joyLabel, " +
                        "${game.pet.joy.value} из 100."
                },
        )
      }
      header()
    }
}

@Composable
private fun YardObject(
    art: PixelArt,
    colors: YardColors,
    sprite: String,
    label: String,
    description: String,
    onPlaced: (Offset) -> Unit,
    onClick: () -> Unit,
    state: String? = null,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    Column(
        modifier = Modifier
            .clickable(interactionSource = null, indication = null, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = description
                if (state != null) stateDescription = state
            }
            .onGloballyPositioned { onPlaced(it.boundsInRoot().center) }
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Sprite(art, sprite)
            overlay()
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .pixelPanel(colors.card, colors.card, colors.cardShadow, colors.outline)
                .padding(horizontal = 12.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun Badge(colors: YardColors, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .offset(x = 8.dp, y = (-8).dp)
            .size(22.dp)
            .pixelPanel(colors.coral, colors.coral, colors.coral, colors.outline),
        contentAlignment = Alignment.Center,
    ) {
        Text("!", style = MaterialTheme.typography.labelLarge, color = colors.card)
    }
}

@Composable
private fun Pet(
    state: AppState,
    parts: PetPartsContent,
    reaction: PetReaction?,
    onReactionPlayed: (Long) -> Unit,
    hopping: Boolean,
    sleeping: Boolean,
    snoringHere: Boolean,
    desire: String?,
    art: PixelArt,
    colors: YardColors,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val profile = state.profile ?: return
    val game = state.game
    val species = parts.species.firstOrNull { it.id == profile.appearance.speciesId }
    val color = parts.colors.firstOrNull { it.id == profile.appearance.colorId }
    val accessory = parts.accessories.firstOrNull { it.id == profile.appearance.accessoryId }
    var hopRound by remember { mutableLongStateOf(0L) }
    // Во время перебежки питомец прыгает; иначе — реакция из очереди.
    val shown = if (hopping) PetReaction(PetReactions.PLAY, id = -1 - hopRound) else reaction
    val zz = rememberInfiniteTransition(label = "snore-here")
    val zPhase by zz.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "z")

    Box(modifier = modifier, contentAlignment = Alignment.BottomCenter) {
        PetFigure(
            petName = profile.petName,
            speciesId = profile.appearance.speciesId,
            speciesTitle = species?.title ?: "Питомец",
            accessoryId = profile.appearance.accessoryId,
            accessoryTitle = accessory?.title ?: "без украшения",
            colorHex = color?.hex ?: "#CCCCCC",
            stage = game.stage,
            care = game.pet.care.level,
            sleeping = sleeping,
            joy = game.pet.joy.level,
            size = PET_CELL * art.petSize,
            caption = false,
            plain = true,
            reaction = shown,
            onReactionEnd = { id -> if (id < 0) hopRound++ else onReactionPlayed(id) },
            description = "${profile.petName}" +
                when (desire) {
                    "icon_care" -> ", хочет есть"
                    "yard_ball" -> ", хочет играть"
                    else -> ""
                } + ". Открыть гардероб",
            modifier = Modifier
                .width(PET_CELL * art.petSize)
                .clickable(interactionSource = null, indication = null, role = Role.Button, onClick = onClick),
        )
        if (desire != null && !sleeping) {
            Bubble(
                art = art,
                colors = colors,
                icon = desire,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 18.dp, y = 6.dp),
            )
        }
        if (snoringHere) {
            val z = remember(art) { composeGoal(art, "yard_z")?.let { pixelImage(it, 5) } }
            if (z != null) {
                // Три «Z» поднимаются по очереди и тают.
                for (k in 0 until 3) {
                    val p = (zPhase + k / 3f) % 1f
                    PixelImage(
                        image = z,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-12 + 10 * p).dp, y = (24 - 48 * p).dp)
                            .size(if (k % 2 == 0) 20.dp else 15.dp)
                            .graphicsLayer { alpha = 1f - p },
                    )
                }
            }
        }
    }
}

/** Облачко-желание: иконка вдвое крупнее клетки, хвостик к голове питомца. */
@Composable
private fun Bubble(art: PixelArt, colors: YardColors, icon: String, modifier: Modifier = Modifier) {
    val pulse = rememberPulse()
    Column(
        modifier = modifier.graphicsLayer {
            scaleX = pulse.value
            scaleY = pulse.value
        },
        horizontalAlignment = Alignment.Start,
    ) {
        Box(
            modifier = Modifier
                .pixelPanel(colors.card, colors.card, colors.card, colors.outline)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Sprite(art, icon, cell = 4.dp)
        }
        Box(
            modifier = Modifier
                .padding(start = 8.dp)
                .size(8.dp)
                .pixelPanel(colors.card, colors.card, colors.card, colors.outline),
        )
    }
}

@Composable
private fun ChestRow(
    art: PixelArt,
    colors: YardColors,
    goal: GoalContent?,
    saved: Int,
    price: Int?,
    onPlaced: (Offset) -> Unit,
    onClick: () -> Unit,
) {
    val title = goal?.let { "Копилка · ${it.claimTitle}" } ?: "Копилка"
    val spoken = if (goal != null && price != null) {
        "Копилка: ${goal.title}, накоплено $saved из $price"
    } else {
        "Копилка: цель ещё не выбрана"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = spoken }
            .onGloballyPositioned { onPlaced(it.boundsInRoot().center) }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Sprite(art, "yard_savings")
        Spacer(modifier = Modifier.height(4.dp))
        Column(
            modifier = Modifier
                .widthIn(min = 180.dp)
                .pixelPanel(colors.card, colors.card, colors.cardShadow, colors.outline)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = title, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
            if (price != null) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Box(
                        modifier = Modifier
                            .size(width = 76.dp, height = 10.dp)
                            .drawBehind {
                                val c = max(1f, floor(CELL.toPx()))
                                drawRect(colors.outline)
                                drawRect(colors.paper, Offset(c, c), Size(size.width - 2 * c, size.height - 2 * c))
                                val fill = (saved.toFloat() / price).coerceIn(0f, 1f)
                                drawRect(colors.coin, Offset(c, c), Size((size.width - 2 * c) * fill, size.height - 2 * c))
                            },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "${minOf(saved, price)} из $price", style = MaterialTheme.typography.labelLarge)
                }
            } else {
                Text(text = "выбери цель", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun FinishDayButton(colors: YardColors, reason: String?, onClick: () -> Unit) {
    val enabled = reason == null
    val pulse = rememberPulse()
    Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .graphicsLayer {
                    val s = if (enabled) pulse.value else 1f
                    scaleX = s
                    scaleY = s
                }
                .then(
                    if (enabled) {
                        Modifier.pixelPanel(colors.green, colors.greenLight, colors.greenDark, colors.outline, 3)
                    } else {
                        Modifier.pixelPanel(colors.cardShadow, colors.card, colors.cardShadow, colors.outline, 2)
                    },
                )
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Закончить день",
                style = MaterialTheme.typography.titleLarge,
                color = if (enabled) colors.card else colors.outline,
            )
        }
        if (reason != null) {
            Text(
                text = reason,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            )
        }
    }
}
