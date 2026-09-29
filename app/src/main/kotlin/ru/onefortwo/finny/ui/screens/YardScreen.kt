package ru.onefortwo.finny.ui.screens

import ru.onefortwo.finny.content.EventContent
import ru.onefortwo.finny.content.ShopItemContent
import ru.onefortwo.finny.economy.GrowthStage
import ru.onefortwo.finny.ui.state.Season
import ru.onefortwo.finny.ui.state.SEASON_DAYS
import androidx.compose.foundation.layout.wrapContentHeight
import ru.onefortwo.finny.content.Accessories
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutCubic
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.only
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
import androidx.lifecycle.compose.LifecycleResumeEffect
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
import androidx.compose.ui.layout.LayoutCoordinates
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import ru.onefortwo.finny.ui.common.FinnyDialog
import ru.onefortwo.finny.ui.common.PrimaryButton
import ru.onefortwo.finny.ui.state.FeedbackMessage
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.layout.layout
import ru.onefortwo.finny.ui.state.PetSpeech
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.unit.Constraints
import ru.onefortwo.finny.ui.state.GuideStep
import ru.onefortwo.finny.ui.state.GuideTarget
import ru.onefortwo.finny.ui.state.Reminder
import ru.onefortwo.finny.ui.common.MinTouchTarget
import androidx.compose.foundation.layout.wrapContentHeight
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * Главная — двор питомца целиком в пиксельной
 * графике. Разделы — предметы на лугу с табличками: лавка «Покупки»,
 * школьная доска «Задания» (следующее задание мелом), доска объявлений
 * «План», книга на пеньке «Словарик», сундучок «Копилка». Нажатие на
 * предмет — питомец подбегает к нему, затем открывается раздел; на
 * питомца — его раздел (гардероб); на табличку «День N · имя» — «Мой
 * прогресс». Нижней панели нет. Текст — Inter: читается и масштабируется.
 */

/** Размер клетки предметов и рамок. */
internal val YARD_CELL = 2.dp

/** Клетка питомца — крупнее предметов: он главный на сцене. */
private val PET_CELL = 3.dp

/** Ширина двора: на планшете и в альбомной ориентации — колонка по центру. */
private val YARD_MAX_WIDTH = 480.dp

/**
 * Полные границы узла на экране. boundsInRoot() обрезает их видимой частью
 * прокрутки: у предмета за краем экрана получалась полоска у границы.
 */
private fun LayoutCoordinates.fullBounds(): Rect =
    // Через оба угла: на планшете луг увеличен, и собственный размер узла
    // меньше, чем он занимает на экране.
    Rect(localToRoot(Offset.Zero), localToRoot(Offset(size.width.toFloat(), size.height.toFloat())))

/** Место над предметом (или под ним), которое нужно облачку подсказки. */
private val GUIDE_ROOM = 96.dp

/**
 * Во сколько раз увеличить луг. На планшете в портрете двор шириной
 * 480 dp оставлял пустой половину экрана — луг растёт до 1,25 или 1,5 раза.
 * Шаг — четверть: клетка 2 dp при плотности 2 становится 5 или 6 пикселями,
 * рисунок остаётся ровным. На телефоне и в альбомной ориентации — 1.
 */
internal fun yardScaleFor(width: Dp, height: Dp): Float {
    if (width < 600.dp || height <= width) return 1f
    val quarters = floor(width / YARD_MAX_WIDTH * 4f)
    return (quarters / 4f).coerceIn(1f, 1.5f)
}

/** Увеличивает содержимое в [k] раз вместе с местом, которое оно занимает. */
private fun Modifier.scaledBy(k: Float): Modifier = if (k == 1f) {
    this
} else {
    layout { measurable, constraints ->
        val inner = Constraints(
            maxWidth = if (constraints.hasBoundedWidth) (constraints.maxWidth / k).toInt() else Constraints.Infinity,
            maxHeight = if (constraints.hasBoundedHeight) (constraints.maxHeight / k).toInt() else Constraints.Infinity,
        )
        val p = measurable.measure(inner)
        layout((p.width * k).roundToInt(), (p.height * k).roundToInt()) {
            p.placeWithLayer(0, 0) {
                scaleX = k
                scaleY = k
                transformOrigin = TransformOrigin(0f, 0f)
            }
        }
    }
}

/** Бег питомца к предмету и остановка у него перед открытием раздела. */
private const val RUN_MS = 700
private const val ARRIVE_PAUSE_MS = 150L

/** Сколько висит приветствие и сколько питомец прощается перед сном. */
private const val SPEECH_MS = 4500L
private const val FAREWELL_MS = 1600L

/** Насколько облачко реплики опускается в рамку питомца: над головой в спрайте пустые ряды. */
private val SPEECH_DROP = 44.dp

/** Цвета рамок из палитры пиксельной графики. */
internal class YardColors(private val art: PixelArt) {
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
    val cardLight = c('l')
    val blue = c('u')
    val pink = c('P')
}

/**
 * Пиксельная рамка: контур со срезанными углами, светлая кромка сверху
 * и тёмная снизу — объёмная табличка или кнопка.
 */
internal fun Modifier.pixelPanel(
    fill: Color,
    light: Color,
    shadow: Color,
    outline: Color,
    shadowCells: Int = 1,
): Modifier = drawBehind {
    val c = max(1f, floor(YARD_CELL.toPx()))
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
internal fun Sprite(art: PixelArt, id: String, modifier: Modifier = Modifier, cell: Dp = YARD_CELL) {
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
    guide: GuideStep? = null,
    speech: String? = null,
    onSpeechShown: () -> Unit = {},
    onSleepingTap: () -> Unit = {},
    onArrivalShown: () -> Unit = {},
    /** Открыть следующее задание сразу — для напоминания о задании. */
    onOpenTask: (() -> Unit)? = null,
    /** «Не сейчас» у напоминания двора. */
    onDismissReminder: (Reminder) -> Unit = {},
    /** Событие дня, ждущее решения, и событие, по которому показан итог. */
    pendingEvent: EventContent? = null,
    resultEvent: EventContent? = null,
    onAnswerEvent: (Boolean) -> Unit = {},
    onCloseEvent: (Boolean) -> Unit = {},
    onConfirmSavings: () -> Unit = {},
    onCancelSavings: () -> Unit = {},
    surprise: ShopItemContent? = null,
    onDismissSurprise: () -> Unit = {},
    onMissedShown: () -> Unit = {},
    onPlay: () -> Unit = {},
) {
    val profile = state.profile ?: return
    val game = state.game
    val art = rememberPixelArt()
    val colors = remember(art) { YardColors(art) }
    val scope = rememberCoroutineScope()
    val motion = motionAllowed()
    val density = LocalDensity.current

    // Положения предметов и питомца на экране — для перебежки.
    val places = remember { HashMap<String, Offset>() }
    // Границы предметов для облачка подсказки — в координатах экрана.
    val bounds = remember { mutableStateMapOf<GuideTarget, Rect>() }
    var yardOrigin by remember { mutableStateOf(Offset.Zero) }
    var petCenter by remember { mutableStateOf(Offset.Zero) }
    val hop = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val petAlpha = remember { Animatable(1f) }
    var busy by remember { mutableStateOf(false) }
    var hopping by remember { mutableStateOf(false) }
    var snoring by remember { mutableStateOf(false) }
    var farewell by remember { mutableStateOf(false) }
    // Реплика висит несколько секунд, потом облачко убирается само.
    LaunchedEffect(speech) {
        if (speech != null) {
            delay(SPEECH_MS)
            onSpeechShown()
        }
    }
    // Во сколько раз увеличен луг: на планшете в портрете — крупнее.
    var yardScale by remember { mutableFloatStateOf(1f) }
    val scroll = rememberScrollState()
    // Видимая часть двора (без нижней панели) — в координатах экрана.
    var viewport by remember { mutableStateOf(Rect.Zero) }
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
            // Бег плавный: разгон и торможение, у предмета — короткая
            // остановка. Назад в центр питомец возвращается, только когда
            // ребёнок снова на дворе (см. LifecycleResumeEffect ниже): иначе
            // он скакал бы обратно, пока открывается раздел.
            hop.animateTo((target - petCenter) * 0.85f / yardScale, tween(RUN_MS, easing = EaseInOutCubic))
            hopping = false
            delay(ARRIVE_PAUSE_MS)
            action()
        }
    }

    // Возвращение на двор: питомец снова в центре, готов бежать.
    LifecycleResumeEffect(Unit) {
        scope.launch {
            hop.snapTo(Offset.Zero)
            petAlpha.snapTo(1f)
        }
        hopping = false
        snoring = false
        farewell = false
        busy = false
        onPauseOrDispose { }
    }

    val dayFinished = state.isDayFinished(today)
    // День закрывается после событий дня.
    val reason = when {
        dayFinished -> "Уложить спать можно завтра."
        state.game.savings.goal == null && !state.extras.planned -> "Сначала выбери мечту и составь план."
        !state.extras.planned -> "Сначала разложи монеты по банкам в «Плане»."
        !state.dayEventsDone -> "Сначала реши события дня."
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
            // Сначала питомец прощается, потом идёт спать.
            farewell = true
            delay(FAREWELL_MS)
            farewell = false
            if (house && tentDoor[0] >= 0) {
                hopping = true
                val door = Offset(sceneOrigin[0] + tentDoor[0], sceneOrigin[1] + tentDoor[1])
                hop.animateTo((door - petCenter) / yardScale, tween(RUN_MS + 150, easing = EaseInOutCubic))
                petAlpha.animateTo(0f, tween(220))
                hopping = false
            }
            snoring = true
            delay(1600)
            onFinishPeriod()
            // Экрана итогов дня нет — двор остаётся, и питомец
            // возвращается в центр сам, а не при возвращении на экран.
            snoring = false
            hop.snapTo(Offset.Zero)
            petAlpha.snapTo(1f)
            busy = false
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.grass)
            .onGloballyPositioned { yardOrigin = it.boundsInRoot().topLeft },
    ) {
        val screenWidth = maxWidth
        val k = yardScaleFor(maxWidth, maxHeight)
        SideEffect { yardScale = k }
        // Полоса неба под строкой состояния закреплена над прокруткой: двор,
        // прокрученный к подсказке, уходит под неё, а не под значки системы.
        val topInset = WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
        Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.fillMaxWidth().background(colors.sky).windowInsetsTopHeight(topInset))
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .consumeWindowInsets(topInset)
                .onGloballyPositioned { viewport = it.boundsInRoot() }
                .verticalScroll(scroll),
        ) {
            // --- Сцена без питомца; шапка закреплена поверх её неба ------------
            // Высота сцены — по ширине двора, а не экрана: на планшете сцена
            // не растёт во весь экран, по бокам продолжаются холмы и луг.
            val yardWidth = minOf(screenWidth, YARD_MAX_WIDTH * k)
            Backdrop(
                art = art,
                height = yardWidth * art.sceneHeight / art.sceneWidth,
                state = state,
                snoring = (snoring || dayFinished) && state.hasScenery("house"),
                tentDoor = tentDoor,
                origin = sceneOrigin,
                scale = k,
            )

            // --- Луг: предметы-разделы и питомец -------------------------------
            // На планшете в портрете луг увеличен целиком (см. yardScaleFor).
            Column(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .scaledBy(k)
                    .widthIn(max = YARD_MAX_WIDTH)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    YardObject(
                        art, colors, "yard_shop", "Покупки",
                        description = "Покупки: лавка для питомца",
                        onPlaced = { places["shop"] = it.center; bounds[GuideTarget.SHOP] = it },
                        onClick = { if (dayFinished) onSleepingTap() else runTo(places["shop"], onOpenShop) },
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    val board = when {
                        state.isTimeUp(today) -> "На сегодня хватит. Приходи завтра!"
                        dayFinished -> "Задания — завтра."
                        activeTask != null -> "Задание: ${activeTask.title}"
                        else -> "Заданий пока нет."
                    }
                    YardObject(
                        art, colors, "yard_tasks", "Задания",
                        description = "Задания. На доске: $board",
                        onPlaced = { places["tasks"] = it.center; bounds[GuideTarget.TASKS] = it },
                        onClick = { if (dayFinished) onSleepingTap() else runTo(places["tasks"], onOpenTasks) },
                        overlay = {
                            Text(
                                text = board,
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.card,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .padding(start = YARD_CELL * 6, top = YARD_CELL * 5, end = YARD_CELL * 5)
                                    .width(YARD_CELL * 54),
                            )
                        },
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().zIndex(1f),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    // Отметка «!» — только когда подсказки нет: иначе на плане
                    // два призыва сразу.
                    val planMissing = !state.extras.planned && !dayFinished && guide == null
                    YardObject(
                        art, colors, "yard_plan", "План",
                        description = "План на день",
                        state = if (planMissing) "не составлен" else null,
                        onPlaced = { places["plan"] = it.center; bounds[GuideTarget.PLAN] = it },
                        onClick = { if (dayFinished) onSleepingTap() else runTo(places["plan"], onOpenPlan) },
                        overlay = { if (planMissing) Badge(colors, Modifier.align(Alignment.TopEnd)) },
                    )
                    // Питомец рисуется поверх соседей по ряду: иначе, подбежав к
                    // словарику, он оказывался за книгой.
                    Box(modifier = Modifier.weight(1f).zIndex(1f), contentAlignment = Alignment.BottomCenter) {
                        Pet(
                            state = state,
                            parts = parts,
                            reaction = reaction,
                            onReactionPlayed = onReactionPlayed,
                            hopping = hopping,
                            sleeping = snoring || dayFinished,
                            // Облачко-желание — только без подсказки: подсказка
                            // уже говорит, куда идти.
                            desire = if (guide != null || dayFinished) null else desireOf(game.pet.care.level, game.pet.joy.level),
                            speech = if (farewell) PetSpeech.FAREWELL else speech,
                            onSpeechClick = onSpeechShown,
                            art = art,
                            colors = colors,
                            modifier = Modifier
                                .offset { IntOffset(hop.value.x.roundToInt(), hop.value.y.roundToInt()) }
                                .alpha(if (dayFinished && state.hasScenery("house")) 0f else petAlpha.value)
                                .onGloballyPositioned { petCenter = it.boundsInRoot().center },
                            onClick = { if (!busy) onOpenPet() },
                            snoringHere = (snoring || dayFinished) && !state.hasScenery("house"),
                        )
                    }
                    YardObject(
                        art, colors, "yard_glossary", "Словарик",
                        description = "Словарик: финансовые слова",
                        onPlaced = { places["glossary"] = it.center; bounds[GuideTarget.GLOSSARY] = it },
                        onClick = { runTo(places["glossary"], onOpenGlossary) },
                    )
                }
                ChestRow(
                    art = art,
                    colors = colors,
                    goal = goal,
                    saved = game.savings.saved.amount,
                    price = game.savings.goal?.price?.amount,
                    onPlaced = { places["savings"] = it.center; bounds[GuideTarget.SAVINGS] = it },
                    onClick = { runTo(places["savings"], onOpenSavings) },
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        // «Закончить день» закреплена внизу, вне прокрутки: видна на
        // любом экране — и на низком телефоне, и на планшете в альбомной.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.grass)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 10.dp, end = 10.dp, bottom = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(modifier = Modifier.widthIn(max = YARD_MAX_WIDTH * k).fillMaxWidth()) {
                // Экранное время: предупреждение за 5 минут и сообщение, что
                // время на сегодня вышло (основание — docs/01, «Экранное время»).
                val timeNote = when {
                    state.isTimeUp(today) -> "На сегодня время вышло. Приходи завтра!"
                    state.isTimeRunningOut(today) -> "Осталось ${Explanations.minutes(state.minutesLeft(today))} на сегодня."
                    else -> null
                }
                if (timeNote != null) {
                    Text(
                        text = timeNote,
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.outline,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                if (dayFinished) {
                    SleepPlate(colors = colors, petName = profile.petName)
                } else {
                    // После дел дня — «Поиграть» или «Уложить спать».
                    if (reason == null && state.extras.playedDay != game.period.number) {
                        Text(
                            text = "Поиграть",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.outline,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                                .heightIn(min = MinTouchTarget)
                                .pixelPanel(colors.card, colors.card, colors.cardShadow, colors.outline, 2)
                                .clickable(role = Role.Button, onClick = onPlay)
                                .wrapContentHeight(Alignment.CenterVertically),
                        )
                    }
                    FinishDayButton(
                        colors = colors,
                        reason = reason,
                        // Кнопка пульсирует, только когда до неё дошла очередь.
                        highlight = guide == null || guide.target == GuideTarget.FINISH,
                        onClick = { finishDay() },
                        onPlaced = { bounds[GuideTarget.FINISH] = it },
                    )
                }
            }
        }
        }

        // Шапка закреплена над двором: день, имя и баланс видны при любой
        // прокрутке, прокрученный луг уходит под неё. В начальном положении
        // она стоит на полосе неба, которую сцена оставляет под шапку.
        Box(
            modifier = Modifier.fillMaxWidth().windowInsetsPadding(topInset),
            contentAlignment = Alignment.TopCenter,
        ) {
            // На планшете в портрете шапка увеличена вместе с лугом.
            Box(modifier = Modifier.scaledBy(k).widthIn(max = YARD_MAX_WIDTH)) {
                Header(
                    art = art,
                    colors = colors,
                    // Пока питомец спит, идёт ещё прошлый день: новый начнётся завтра.
                    day = if (dayFinished) game.period.number - 1 else game.period.number,
                    stage = game.stage,
                    demo = state.isDemo,
                    petName = profile.petName,
                    balance = game.balance.amount,
                    onOpenProgress = onOpenProgress,
                    onOpenHelp = onOpenHelp,
                    onOpenAdult = onOpenAdult,
                )
            }
        }

        // Подсказка на предмет за краем экрана: двор сам прокручивается к нему.
        LaunchedEffect(guide?.number, guide?.target) {
            val step = guide ?: return@LaunchedEffect
            if (step.target == GuideTarget.FINISH) return@LaunchedEffect
            // Ждём, пока предмет размещён: на холодном запуске первая
            // раскладка бывает дольше короткой паузы.
            snapshotFlow { bounds[step.target]?.takeIf { viewport.height > 0f } }.filterNotNull().first()
            delay(250)
            val t = bounds[step.target] ?: return@LaunchedEffect
            val room = with(density) { GUIDE_ROOM.toPx() }
            val below = step.target == GuideTarget.SAVINGS
            val top = t.top - if (below) 0f else room
            val bottom = t.bottom + if (below) room else 0f
            val shift = when {
                top < viewport.top -> top - viewport.top
                bottom > viewport.bottom -> bottom - viewport.bottom
                else -> 0f
            }
            if (shift != 0f) scroll.animateScrollTo((scroll.value + shift).roundToInt())
        }

        // Подсказка первого дня — облачко над предметом, к которому пора идти.
        // Нажатие на облачко — как на сам предмет; конец дня облачко не
        // запускает: это решение ребёнок принимает кнопкой.
        val target = guide?.let { bounds[it.target] }
        // Облачко не висит над предметом, который сейчас за краем экрана.
        val visible = target != null && (guide?.target == GuideTarget.FINISH || target.overlaps(viewport))
        if (guide != null && target != null && visible && !busy && speech == null) {
            GuideBubble(
                colors = colors,
                step = guide,
                target = target.translate(-yardOrigin),
                // Сундучок стоит под питомцем: облачко над ним закрыло бы
                // питомца, поэтому оно снизу.
                below = guide.target == GuideTarget.SAVINGS,
                onClick = when (guide.target) {
                    GuideTarget.PLAN -> ({ runTo(places["plan"], onOpenPlan) })
                    GuideTarget.SHOP -> ({ runTo(places["shop"], onOpenShop) })
                    // Напоминание о задании ведёт сразу к заданию, а не к списку.
                    GuideTarget.TASKS -> {
                        val open = if (guide.reminder == Reminder.TASK && onOpenTask != null) onOpenTask else onOpenTasks
                        ({ runTo(places["tasks"], open) })
                    }
                    GuideTarget.SAVINGS -> ({ runTo(places["savings"], onOpenSavings) })
                    GuideTarget.GLOSSARY -> ({ runTo(places["glossary"], onOpenGlossary) })
                    GuideTarget.FINISH -> null
                },
                onDismiss = guide.reminder?.let { reminder -> { onDismissReminder(reminder) } },
            )
        }

        // Пришли монеты — отдельным окном: это первое, что видит ребёнок.
        val arrival = state.arrival
        if (arrival != null) {
            ArrivalDialog(art = art, message = arrival, onDismiss = onArrivalShown)
        }

        // Событие дня — окном поверх двора, чуть погодя после
        // возвращения: сначала ребёнок видит двор. Закрыть до ответа нельзя.
        var eventReady by remember { mutableStateOf(false) }
        LaunchedEffect(pendingEvent?.id, arrival == null) {
            eventReady = false
            if (pendingEvent != null && arrival == null) {
                delay(EVENT_DELAY_MS)
                eventReady = true
            }
        }
        val shownEvent = resultEvent ?: pendingEvent?.takeIf { eventReady && arrival == null && !dayFinished }
        if (shownEvent != null) {
            EventDialog(
                state = state,
                parts = parts,
                event = shownEvent,
                result = state.eventResult,
                onAnswer = onAnswerEvent,
                onClose = onCloseEvent,
            )
        }
        state.savingsAsk?.let { ask ->
            if (ask.eventId != null) SavingsAskDialog(ask, onConfirm = onConfirmSavings, onCancel = onCancelSavings)
        }
        if (surprise != null && arrival == null && shownEvent == null) {
            SurpriseDialog(surprise, state.extras.tasksSolved, profile.petName, onDismissSurprise)
        }
        if (state.extras.missed && arrival == null && shownEvent == null) {
            MissedDialog(state, parts, onMissedShown)
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

/** Пауза перед окном события — сначала виден двор. */
private const val EVENT_DELAY_MS = 1200L

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
    stage: GrowthStage = GrowthStage.BABY,
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
                    contentDescription = "Мой прогресс. Сезон ${Season.seasonOf(day)}, день ${Season.dayOf(day)}" +
                        (if (demo) ", демо" else "") + ", $petName, ${stage.displayName.lowercase()}"
                }
                .padding(start = 10.dp, end = 8.dp, top = 4.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    // Режим «демо» назван водяным знаком и для TalkBack; в строке — только сезон и день.
                    // Сезон назван в «Моём прогрессе» и для TalkBack; в шапке — день сезона.
                    text = "День ${Season.dayOf(day)} из $SEASON_DAYS",
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
                // Ступень роста — три деления, пройденные залиты.
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.padding(top = 2.dp)) {
                    GrowthStage.entries.forEach { s ->
                        val fill = if (s <= stage) colors.green else colors.cardLight
                        Box(modifier = Modifier.size(width = 16.dp, height = 6.dp).pixelPanel(fill, fill, fill, colors.outline))
                    }
                }
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
        Sprite(art, icon, modifier = Modifier.padding(bottom = YARD_CELL))
    }
}

/** Сцена без питомца; при храпе над палаткой поднимаются «Z». */
@Composable
private fun Backdrop(
    art: PixelArt,
    height: Dp,
    state: AppState,
    snoring: Boolean,
    tentDoor: FloatArray,
    origin: FloatArray,
    scale: Float = 1f,
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
    val zMoving by zz.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart), label = "z")
    // Без движений «Z» стоят на месте.
    val zPhase = if (motionAllowed()) zMoving else 0.2f
    val careLabel = Explanations.statLabel(PetStatKind.CARE, game.pet.care)
    val joyLabel = Explanations.statLabel(PetStatKind.JOY, game.pet.joy)

    Box(modifier = Modifier.background(Color(art.colors[art.indexOf('A')]))) {
      Column {
        // Полоса неба под шапку: шапка не закрывает панель самочувствия.
        // Отступ строки состояния — у закреплённой полосы над прокруткой.
        Spacer(modifier = Modifier.height(58.dp * scale))
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
            extendEdges = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
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
    }
}

@Composable
private fun YardObject(
    art: PixelArt,
    colors: YardColors,
    sprite: String,
    label: String,
    description: String,
    onPlaced: (Rect) -> Unit,
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
            .onGloballyPositioned { onPlaced(it.fullBounds()) }
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
    speech: String? = null,
    onSpeechClick: () -> Unit = {},
) {
    val profile = state.profile ?: return
    val game = state.game
    val species = parts.species.firstOrNull { it.id == profile.appearance.speciesId }
    val color = parts.colors.firstOrNull { it.id == profile.appearance.colorId }
    var hopRound by remember { mutableLongStateOf(0L) }
    // Во время перебежки питомец прыгает; иначе — реакция из очереди.
    val shown = if (hopping) PetReaction(PetReactions.PLAY, id = -1 - hopRound) else reaction
    val zz = rememberInfiniteTransition(label = "snore-here")
    val zMoving by zz.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "z")
    // Без движений «Z» стоят на месте.
    val zPhase = if (motionAllowed()) zMoving else 0.2f

    Box(modifier = modifier, contentAlignment = Alignment.BottomCenter) {
        PetFigure(
            petName = profile.petName,
            speciesId = profile.appearance.speciesId,
            speciesTitle = species?.title ?: "Питомец",
            accessoryId = profile.appearance.accessoryId,
            accessoryTitle = Accessories.title(parts, profile.appearance.accessoryId),
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
        if (speech != null) {
            // Облачко над головой: не шире 220 dp, может выходить за ряд.
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(2f)
                    .layout { measurable, constraints ->
                        val max = 220.dp.roundToPx()
                        val p = measurable.measure(
                            constraints.copy(minWidth = 0, maxWidth = max, minHeight = 0, maxHeight = Constraints.Infinity),
                        )
                        layout(0, 0) { p.place(-p.width / 2, -p.height + SPEECH_DROP.roundToPx()) }
                    },
            ) {
                SpeechBubble(colors = colors, text = speech, onClick = onSpeechClick)
            }
        } else if (desire != null && !sleeping) {
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

/**
 * Облачко с репликой питомца: табличка с текстом и хвостик вниз, к нему.
 * TalkBack зачитывает реплику сам, с именем говорящего.
 */
@Composable
internal fun SpeechBubble(colors: YardColors, text: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .pixelPanel(colors.card, colors.card, colors.cardShadow, colors.outline, 2)
                .then(if (onClick != null) Modifier.clickable(interactionSource = null, indication = null, onClick = onClick) else Modifier)
                .semantics { liveRegion = LiveRegionMode.Polite }
                .padding(horizontal = 14.dp, vertical = 8.dp),
        )
        Box(
            modifier = Modifier
                .offset(y = -YARD_CELL * 2)
                .size(YARD_CELL * 8, YARD_CELL * 4)
                .drawBehind {
                    val c = max(1f, floor(YARD_CELL.toPx()))
                    for (row in 0 until 4) {
                        val to = 7 - row
                        drawRect(colors.outline, Offset(row * c, row * c), Size((to - row + 1) * c, c))
                        if (to - row >= 2) {
                            drawRect(colors.card, Offset((row + 1) * c, row * c), Size((to - row - 1) * c, c))
                        }
                    }
                },
        )
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
    onPlaced: (Rect) -> Unit,
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
            .onGloballyPositioned { onPlaced(it.fullBounds()) }
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
                                val c = max(1f, floor(YARD_CELL.toPx()))
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
private fun FinishDayButton(
    colors: YardColors,
    reason: String?,
    highlight: Boolean,
    onClick: () -> Unit,
    onPlaced: (Rect) -> Unit,
) {
    val enabled = reason == null
    val pulsing = enabled && highlight
    val pulse = rememberPulse()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .onGloballyPositioned { onPlaced(it.fullBounds()) },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .graphicsLayer {
                    val s = if (pulsing) pulse.value else 1f
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
                text = "Уложить спать",
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

/**
 * Облачко подсказки над предметом [target] (в координатах двора): табличка
 * «Шаг N из 6» с текстом и хвостик вниз, к предмету. Облачко не выходит за
 * края экрана; хвостик всегда над серединой предмета. Слегка покачивается,
 * если движения включены. TalkBack зачитывает новый шаг сам.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GuideBubble(
    colors: YardColors,
    step: GuideStep,
    target: Rect,
    onClick: (() -> Unit)?,
    below: Boolean = false,
    /** «Не сейчас» — только у напоминаний со второго дня. */
    onDismiss: (() -> Unit)? = null,
) {
    val motion = motionAllowed()
    val bob = rememberInfiniteTransition(label = "guide")
    val phase by bob.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "bob")
    val lift = if (motion) sin(phase * 2 * PI.toFloat()) * 3f else 0f
    val numbered = step.number > 0
    val spoken = when {
        numbered -> "Подсказка, шаг ${step.number} из ${step.total}: ${step.text}"
        step.reminder != null -> "Напоминание: ${step.text}"
        else -> "Подсказка: ${step.text}"
    }
    // У напоминания действия — отдельными кнопками: нажатие мимо них ничего
    // не делает, и «Не сейчас» не срабатывает случайно.
    val reminder = step.reminder
    Layout(
        modifier = Modifier.fillMaxSize(),
        content = {
            Column(
                modifier = Modifier
                    .graphicsLayer { translationY = lift.dp.toPx() }
                    .pixelPanel(colors.card, colors.card, colors.cardShadow, colors.outline, 2)
                    .then(
                        if (onClick != null && reminder == null) {
                            Modifier.clickable(role = Role.Button, onClick = onClick)
                        } else {
                            Modifier
                        },
                    )
                    .semantics(mergeDescendants = true) {
                        contentDescription = spoken
                        liveRegion = LiveRegionMode.Polite
                    }
                    .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 10.dp),
            ) {
                if (numbered) {
                    Text(
                        text = "Шаг ${step.number} из ${step.total}",
                        style = MaterialTheme.typography.labelMedium,
                        color = FinnyTheme.colors.onSurfaceMuted,
                    )
                }
                Text(text = step.text, style = MaterialTheme.typography.titleSmall)
                if (reminder != null) {
                    FlowRow(
                        modifier = Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        val action = reminder.action
                        if (action != null && onClick != null) {
                            Text(
                                text = action,
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.card,
                                modifier = Modifier
                                    .heightIn(min = MinTouchTarget)
                                    .pixelPanel(colors.green, colors.greenLight, colors.greenDark, colors.outline, 2)
                                    .clickable(role = Role.Button, onClick = onClick)
                                    .wrapContentHeight(Alignment.CenterVertically)
                                    .padding(horizontal = 12.dp),
                            )
                        }
                        if (onDismiss != null) {
                            Text(
                                text = "Не сейчас",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.outline,
                                modifier = Modifier
                                    .heightIn(min = MinTouchTarget)
                                    .pixelPanel(colors.card, colors.card, colors.cardShadow, colors.outline, 2)
                                    .clickable(role = Role.Button, onClick = onDismiss)
                                    .wrapContentHeight(Alignment.CenterVertically)
                                    .padding(horizontal = 12.dp),
                            )
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .graphicsLayer { translationY = lift.dp.toPx() }
                    .size(YARD_CELL * 8, YARD_CELL * 5)
                    .drawBehind {
                        // Хвостик ступеньками: верхний ряд закрывает нижнюю
                        // кромку таблички, и облачко выглядит цельным.
                        val c = max(1f, floor(YARD_CELL.toPx()))
                        // Под предметом хвостик смотрит вверх: широкий ряд — внизу.
                        for (row in 0 until 4) {
                            val from = row
                            val to = 7 - row
                            val y = (if (below) 3 - row else row) * c
                            drawRect(colors.outline, Offset(from * c, y), Size((to - from + 1) * c, c))
                            if (to - from >= 2) {
                                drawRect(colors.card, Offset((from + 1) * c, y), Size((to - from - 1) * c, c))
                            }
                        }
                    },
            )
        },
    ) { measurables, constraints ->
        val margin = 8.dp.roundToPx()
        // У напоминания две кнопки — облачко шире, чтобы они встали в ряд.
        val widest = if (reminder != null) 300.dp else 220.dp
        val maxWidth = minOf(constraints.maxWidth - 2 * margin, widest.roundToPx()).coerceAtLeast(0)
        val panel = measurables[0].measure(Constraints(maxWidth = maxWidth))
        val tail = measurables[1].measure(Constraints())
        val overlap = max(1f, floor(YARD_CELL.toPx())).roundToInt() * 2
        layout(constraints.maxWidth, constraints.maxHeight) {
            val cx = target.center.x.roundToInt()
            val right = (constraints.maxWidth - margin - panel.width).coerceAtLeast(margin)
            val x = (cx - panel.width / 2).coerceIn(margin, right)
            if (below) {
                val tailTop = target.bottom.roundToInt() - overlap
                val panelTop = (tailTop + tail.height - overlap).coerceAtMost(constraints.maxHeight - panel.height)
                panel.place(x, panelTop)
                tail.place(cx - tail.width / 2, panelTop - tail.height + overlap, zIndex = 1f)
            } else {
                val tailTop = target.top.roundToInt() - tail.height + overlap
                val panelTop = (tailTop - panel.height + overlap).coerceAtLeast(0)
                panel.place(x, panelTop)
                tail.place(cx - tail.width / 2, panelTop + panel.height - overlap, zIndex = 1f)
            }
        }
    }
}

/** Вместо «Закончить день», пока питомец спит до новых суток. */
@Composable
private fun SleepPlate(colors: YardColors, petName: String) {
    Text(
        text = "$petName спит. Новый день начнётся завтра",
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .pixelPanel(colors.card, colors.card, colors.cardShadow, colors.outline, 2)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    )
}

/** Окно «Пришли монеты» — стартовые или карманные на новый день. */
@Composable
private fun ArrivalDialog(art: PixelArt, message: FeedbackMessage, onDismiss: () -> Unit) {
    FinnyDialog(
        title = "Пришли монеты",
        onDismiss = onDismiss,
        content = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Sprite(art, "coin_0", cell = 4.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = message.text, style = MaterialTheme.typography.titleMedium)
            }
            message.nextStep?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 10.dp))
            }
        },
        actions = { PrimaryButton(text = "Дальше", onClick = onDismiss) },
    )
}
