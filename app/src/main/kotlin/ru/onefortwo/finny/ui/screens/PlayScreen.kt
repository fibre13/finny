package ru.onefortwo.finny.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch
import ru.onefortwo.finny.content.Accessories
import ru.onefortwo.finny.content.PetPartsContent
import ru.onefortwo.finny.content.PixelArt
import ru.onefortwo.finny.economy.GrowthStage
import ru.onefortwo.finny.economy.StatLevel
import ru.onefortwo.finny.ui.common.PetFigure
import ru.onefortwo.finny.ui.common.motionAllowed
import ru.onefortwo.finny.ui.common.rememberPixelArt
import ru.onefortwo.finny.ui.state.AppState
import ru.onefortwo.finny.ui.state.PetReaction
import ru.onefortwo.finny.ui.state.PetReactions
import ru.onefortwo.finny.ui.state.PetVoice

/*
 * «Поиграть»: полноэкранная игра с питомцем без условий и без награды.
 * Сверху — питомец и облачко, снизу — четыре предмета, каждый заход —
 * случайные четыре из доступных. Нажатие на предмет запускает неторопливую
 * сценку, питомец отвечает репликой, затем снова можно выбрать предмет.
 * При выключенных движениях сразу показывается итоговая картинка сценки.
 */

/** Высота сцены с питомцем. */
private val STAGE_HEIGHT = 300.dp

/** Размер фигуры питомца: холст 48 × 48 при клетке 4 dp. */
private val PET_SIZE = 192.dp
private val PET_CELL = 4.dp

/** Линия земли, на которой стоят лапы питомца и лежат предметы. */
private val FEET = 276.dp

/** Нижний ряд фигуры на холсте 48 × 48. */
private const val PET_FEET_ROW = 46

/** Игра с питомцем: подпись, длительность сценки и доля, с которой звучит реплика. */
private enum class PlayGame(val title: String, val durationMs: Int, val replyAt: Float) {
    BALL("Мячик", 5200, 0.6f),
    STROKE("Погладить", 7000, 0.32f),
    BOW("Бантик", 5200, 0.5f),
    YARN("Клубок", 5600, 0.62f),
    SCOOTER("Самокат", 5600, 0.45f),
    TENT("Палатка", 6000, 0.55f),
}

/** Предметы, доступные ребёнку: четыре всегда, самокат и палатка — после покупки. */
private fun availableGames(scooter: Boolean, tent: Boolean): List<PlayGame> = buildList {
    add(PlayGame.BALL)
    add(PlayGame.STROKE)
    add(PlayGame.BOW)
    add(PlayGame.YARN)
    if (scooter) add(PlayGame.SCOOTER)
    if (tent) add(PlayGame.TENT)
}

private fun replyFor(game: PlayGame, speciesId: String?): String = when (game) {
    PlayGame.BALL -> "Ура! Догнал!"
    PlayGame.STROKE -> when (speciesId) {
        "cat" -> "Мур-мур-мур"
        "dog" -> "Гав-гав!"
        else -> "Как приятно!"
    }
    PlayGame.BOW -> "Как мне идёт?"
    PlayGame.YARN -> "Как весело!"
    PlayGame.SCOOTER -> "Юху! С ветерком!"
    PlayGame.TENT -> "Ззз…"
}

private fun sceneFor(game: PlayGame?, name: String): String = when (game) {
    null -> "$name ждёт, во что с ним поиграют"
    PlayGame.BALL -> "Мячик катится, $name бежит за ним и догоняет"
    PlayGame.STROKE -> "Рука гладит $name по голове, $name закрывает глазки"
    PlayGame.BOW -> "$name надевает бантик и крутится"
    PlayGame.YARN -> "Клубок катится, $name бежит за ним"
    PlayGame.SCOOTER -> "$name едет на самокате, сзади ветерок"
    PlayGame.TENT -> "$name заходит в палатку и засыпает"
}

/** Доля отрезка [from]..[to], пройденная к моменту [p], от 0 до 1. */
private fun seg(p: Float, from: Float, to: Float): Float = ((p - from) / (to - from)).coerceIn(0f, 1f)

/** Плавный разгон и торможение. */
private fun smooth(x: Float): Float = x * x * (3 - 2 * x)

@Composable
fun PlayScreen(state: AppState, parts: PetPartsContent, onBack: () -> Unit, onPlayed: () -> Unit) {
    val art = rememberPixelArt()
    val colors = remember(art) { YardColors(art) }
    val motion = motionAllowed()
    val scope = rememberCoroutineScope()
    val petName = state.profile?.petName ?: "Питомец"
    val speciesId = state.profile?.appearance?.speciesId
    // Четыре случайных предмета — один раз на вход в раздел.
    val games = remember {
        availableGames("scooter" in state.achievedGoalIds, state.hasScenery("house")).shuffled().take(4)
    }
    var game by remember { mutableStateOf<PlayGame?>(null) }
    var reply by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var played by remember { mutableStateOf(false) }
    var round by remember { mutableLongStateOf(0L) }
    val progress = remember { Animatable(0f) }
    val greeting = PetVoice.of(state.game.stage, "Привет! Давай поиграем!")

    fun play(chosen: PlayGame) {
        if (busy) return
        if (!played) {
            played = true
            onPlayed()
        }
        round++
        val text = PetVoice.of(state.game.stage, replyFor(chosen, speciesId))
        game = chosen
        if (!motion) {
            scope.launch { progress.snapTo(chosen.replyAt) }
            reply = text
            return
        }
        busy = true
        reply = null
        scope.launch {
            try {
                progress.snapTo(0f)
                progress.animateTo(
                    chosen.replyAt,
                    tween((chosen.durationMs * chosen.replyAt).toInt(), easing = LinearEasing),
                )
                reply = text
                progress.animateTo(
                    1f,
                    tween((chosen.durationMs * (1 - chosen.replyAt)).toInt(), easing = LinearEasing),
                )
                game = null
            } finally {
                busy = false
            }
        }
    }

    MeadowBackground(art, grassFrom = 0.4f) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .pixelPanel(colors.card, colors.card, colors.cardShadow, colors.outline, 2)
                        .clickable(role = Role.Button, onClick = onBack)
                        .semantics { contentDescription = "Назад" },
                    contentAlignment = Alignment.Center,
                ) {
                    Sprite(art, "yard_icon_back")
                }
            }
            Column(
                modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PlayStage(
                    state = state,
                    parts = parts,
                    art = art,
                    colors = colors,
                    game = game,
                    p = progress.value,
                    bubble = when {
                        game != null && busy && reply == null -> null
                        else -> reply ?: greeting
                    },
                    round = round,
                    motion = motion,
                    description = sceneFor(game, petName),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    games.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { item ->
                                PlayCard(
                                    art = art,
                                    colors = colors,
                                    game = item,
                                    enabled = !busy,
                                    onClick = { play(item) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/** Крупная карточка предмета: пиксельный значок и подпись. */
@Composable
private fun PlayCard(
    art: PixelArt,
    colors: YardColors,
    game: PlayGame,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(124.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.6f }
            .pixelPanel(colors.card, colors.card, colors.cardShadow, colors.outline, 2)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = "поиграть", onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(modifier = Modifier.height(72.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            PlayIcon(art, game)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = game.title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun PlayIcon(art: PixelArt, game: PlayGame) {
    when (game) {
        PlayGame.BALL -> Sprite(art, "item_ball", cell = 4.dp)
        PlayGame.STROKE -> Sprite(art, "hand_stroke", cell = 3.dp)
        PlayGame.BOW -> Sprite(art, "item_bow", cell = 4.dp)
        PlayGame.YARN -> PixelRows(YARN_ROWS, cell = 4.dp)
        PlayGame.SCOOTER -> GoalPicture(goalId = "scooter", size = 72.dp, container = Color.Transparent, inset = 0.dp)
        PlayGame.TENT -> Sprite(art, "item_tent", cell = 4.dp)
    }
}

/** Питомец ребёнка на сцене игры: можно надеть другое украшение и закрыть глазки. */
@Composable
private fun StagePet(
    state: AppState,
    parts: PetPartsContent,
    size: Dp,
    modifier: Modifier = Modifier,
    accessoryId: String? = null,
    sleeping: Boolean = false,
    reaction: PetReaction? = null,
) {
    val profile = state.profile ?: return
    val species = parts.species.firstOrNull { it.id == profile.appearance.speciesId }
    val color = parts.colors.firstOrNull { it.id == profile.appearance.colorId }
    val accessory = accessoryId ?: profile.appearance.accessoryId
    PetFigure(
        petName = profile.petName,
        speciesId = profile.appearance.speciesId,
        speciesTitle = species?.title ?: "Питомец",
        accessoryId = accessory,
        accessoryTitle = Accessories.title(parts, accessory),
        colorHex = color?.hex ?: "#CCCCCC",
        stage = state.game.stage,
        care = StatLevel.HIGH,
        joy = StatLevel.HIGH,
        size = size,
        caption = false,
        plain = true,
        sleeping = sleeping,
        reaction = reaction,
        modifier = modifier.width(size),
    )
}

/**
 * Сцена: питомец крупно, облачко над ним и сценка выбранной игры в
 * момент [p] от 0 до 1. Всё рисуется от линии земли [FEET].
 */
@Composable
private fun PlayStage(
    state: AppState,
    parts: PetPartsContent,
    art: PixelArt,
    colors: YardColors,
    game: PlayGame?,
    p: Float,
    bubble: String?,
    round: Long,
    motion: Boolean,
    description: String,
) {
    val speciesId = state.profile?.appearance?.speciesId
    val headRow = if (state.game.stage == GrowthStage.BABY) 18 else 10
    val petTop = FEET - PET_CELL * PET_FEET_ROW
    val headTop = petTop + PET_CELL * headRow
    val happy = PetReaction(PetReactions.PLAY, id = round * 10 + 1)

    BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(STAGE_HEIGHT)) {
        val w = maxWidth
        val cx = w / 2
        // Насколько питомец отбегает за мячиком или клубком: до края сцены.
        val reach = (cx - 100.dp).coerceIn(30.dp, 140.dp)

        // Положение питомца: сдвиг, подскок, прозрачность, масштаб, разворот, наклон.
        var dx = 0.dp
        var hop = 0.dp
        var petAlpha = 1f
        var petScale = 1f
        var flip = 1f
        var tilt = 0f
        var accessory: String? = null
        var sleeping = false
        var reaction: PetReaction? = null
        val g = game

        fun running(moving: Boolean) {
            if (motion && moving) hop = (abs(sin(p * PI * 18)).toFloat() * 10).dp
        }

        when (g) {
            PlayGame.BALL -> {
                val run = smooth(seg(p, 0.15f, 0.52f))
                val back = smooth(seg(p, 0.78f, 0.98f))
                dx = reach * run * (1 - back)
                running(p in 0.15f..0.52f || p in 0.78f..0.98f)
                if (motion && p in 0.52f..0.78f) reaction = happy
            }

            PlayGame.YARN -> {
                val run = smooth(seg(p, 0.2f, 0.55f))
                val back = smooth(seg(p, 0.8f, 0.98f))
                dx = -reach * run * (1 - back)
                // Сначала питомец толкает клубок лапкой — маленький подскок.
                if (motion && p < 0.1f) hop = (sin(seg(p, 0f, 0.1f) * PI).toFloat() * 6).dp
                running(p in 0.2f..0.55f || p in 0.8f..0.98f)
                if (motion && p in 0.55f..0.8f) reaction = happy
            }

            PlayGame.STROKE -> {
                val stroking = p in 0.2f..0.9f
                when (speciesId) {
                    "dog" -> {
                        // Щенок наклоняет голову и подставляет ухо.
                        tilt = -8f * smooth(seg(p, 0.2f, 0.3f)) * (1 - smooth(seg(p, 0.86f, 0.94f)))
                    }
                    "rabbit" -> {
                        // Крольчонок жмурится и чуть подрагивает от радости.
                        sleeping = stroking
                        if (motion && stroking) hop = (abs(sin(p * PI * 24)).toFloat() * 2).dp
                    }
                    else -> sleeping = stroking
                }
                if (!motion) {
                    sleeping = speciesId != "dog"
                    tilt = if (speciesId == "dog") -8f else 0f
                }
            }

            PlayGame.BOW -> {
                if (p >= 0.3f) accessory = "bow"
                if (motion) {
                    val q = seg(p, 0.34f, 0.84f)
                    flip = cos(q * 2 * PI * 2).toFloat()
                    if (p > 0.86f) reaction = happy
                }
            }

            PlayGame.SCOOTER -> {
                // Питомец на самокате; на месте он появляется в самом конце.
                petAlpha = if (motion) seg(p, 0.9f, 1f) else 0f
            }

            PlayGame.TENT -> {
                val tentLeft = (w - 116.dp).coerceAtLeast(cx + 20.dp)
                val toTent = tentLeft + 56.dp - cx
                if (p < 0.9f || !motion) {
                    val walk = smooth(seg(p, 0.12f, 0.45f))
                    dx = toTent * walk
                    petScale = 1f - 0.35f * walk
                    petAlpha = 1f - seg(p, 0.38f, 0.5f)
                    running(p in 0.12f..0.45f)
                } else {
                    petAlpha = seg(p, 0.9f, 1f)
                }
            }

            null -> Unit
        }

        // Графика сцены озвучивается одной фразой; облачко — отдельно.
        Box(modifier = Modifier.matchParentSize().clearAndSetSemantics { contentDescription = description }) {
            // Питомец.
            Box(
                modifier = Modifier
                    .offset(x = cx - PET_SIZE / 2 + dx, y = petTop - hop)
                    .graphicsLayer {
                        alpha = petAlpha
                        scaleX = flip * petScale
                        scaleY = petScale
                        rotationZ = tilt
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, PET_FEET_ROW / 48f)
                    },
            ) {
                StagePet(state, parts, PET_SIZE, accessoryId = accessory, sleeping = sleeping, reaction = reaction)
            }

            when (g) {
                PlayGame.BALL -> BallProps(art, p, cx, w)
                PlayGame.YARN -> YarnProps(art, colors, p, cx)
                PlayGame.STROKE -> StrokeHand(art, p, cx, headTop, motion)
                PlayGame.BOW -> if (p < 0.3f) {
                    val fall = smooth(seg(p, 0.02f, 0.3f))
                    Sprite(
                        art, "item_bow",
                        Modifier.offset(x = cx - 24.dp, y = (headTop - 20.dp) * fall - 40.dp * (1 - fall)),
                        cell = 3.dp,
                    )
                }
                PlayGame.SCOOTER -> ScooterProps(state, parts, p, w, motion)
                PlayGame.TENT -> TentProps(art, p, cx, w, motion)
                null -> Unit
            }
        }

        if (bubble != null) {
            val bubbleY = if (g == PlayGame.STROKE) 0.dp else (headTop - 90.dp).coerceAtLeast(0.dp)
            SpeechBubble(
                colors = colors,
                text = bubble,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = bubbleY)
                    .widthIn(max = 300.dp)
                    .zIndex(1f),
            )
        }
    }
}

/** Мячик катится вправо и крутится; когда питомец возвращается — исчезает. */
@Composable
private fun BallProps(art: PixelArt, p: Float, cx: Dp, w: Dp) {
    val roll = smooth(seg(p, 0.05f, 0.4f))
    val start = cx + 30.dp
    val end = w - 52.dp
    Sprite(
        art, "item_ball",
        Modifier
            .offset(x = start + (end - start) * roll, y = FEET - 42.dp)
            .graphicsLayer {
                rotationZ = 720f * roll
                alpha = 1f - seg(p, 0.8f, 0.95f)
            },
        cell = 3.dp,
    )
}

/** Клубок катится влево, за ним по земле тянется нитка. */
@Composable
private fun YarnProps(art: PixelArt, colors: YardColors, p: Float, cx: Dp) {
    val roll = smooth(seg(p, 0.08f, 0.45f))
    val start = cx - 78.dp
    val end = 4.dp
    val x = start + (end - start) * roll
    val fade = 1f - seg(p, 0.82f, 0.96f)
    Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = fade }) {
        val c = 3.dp.toPx()
        val from = (start + 24.dp).toPx()
        val to = (x + 24.dp).toPx()
        val y = (FEET - 4.dp).toPx()
        var px = to
        var i = 0
        while (px < from) {
            val wave = if ((i / 3) % 2 == 0) 0f else c
            drawRect(colors.pink, Offset(px, y - wave), Size(c, c))
            px += c
            i++
        }
    }
    PixelRows(
        YARN_ROWS,
        cell = 3.dp,
        modifier = Modifier
            .offset(x = x, y = FEET - 45.dp)
            .graphicsLayer {
                rotationZ = -720f * roll
                alpha = fade
            },
    )
}

/**
 * Крупная рука опускается сверху на голову, гладит её три раза медленно
 * и поднимается. При выключенных движениях — лежит на голове.
 */
@Composable
private fun StrokeHand(art: PixelArt, p: Float, cx: Dp, headTop: Dp, motion: Boolean) {
    val handCell = 4.dp
    val rest = headTop - handCell * 11
    val descend = if (motion) smooth(seg(p, 0f, 0.2f)) else 1f
    val lift = if (motion) smooth(seg(p, 0.9f, 1f)) else 0f
    val q = seg(p, 0.22f, 0.88f)
    val swing = if (motion) (18 - 36 * (0.5f - 0.5f * cos(q * 3 * 2 * PI).toFloat())).dp else 0.dp
    val press = if (motion) (abs(sin(q * 3 * PI)).toFloat() * 3).dp else 0.dp
    Sprite(
        art, "hand_stroke",
        Modifier
            .offset(
                x = cx - handCell * 13 + swing,
                y = rest - 140.dp * (1 - descend) - 140.dp * lift - press,
            )
            .graphicsLayer { alpha = descend * (1 - lift) },
        cell = handCell,
    )
}

/** Питомец едет на самокате слева направо, позади — полоски ветра. */
@Composable
private fun ScooterProps(state: AppState, parts: PetPartsContent, p: Float, w: Dp, motion: Boolean) {
    val pass = seg(p, 0.02f, 0.9f)
    val x = (w + 190.dp) * pass - 180.dp
    val bob = if (motion) (sin(p * PI * 20).toFloat() * 3).dp else 0.dp
    val top = FEET - 176.dp
    if (motion && pass >= 1f) return
    Canvas(modifier = Modifier.fillMaxSize()) {
        val c = 3.dp.toPx()
        for (i in 0 until 4) {
            val lx = x.toPx() - (10 + i * 14) * c / 3
            val ly = (top + 70.dp + (i * 22).dp).toPx()
            drawRect(Color.White.copy(alpha = 0.85f), Offset(lx - 16 * c, ly), Size(10 * c, c))
        }
    }
    Box(modifier = Modifier.offset(x = x, y = top + bob)) {
        GoalPicture(
            goalId = "scooter",
            size = 170.dp,
            container = Color.Transparent,
            modifier = Modifier.offset(y = 40.dp).graphicsLayer { scaleX = -1f },
        )
        Box(modifier = Modifier.offset(x = 30.dp, y = 22.dp)) { StagePet(state, parts, 130.dp) }
    }
}

/** Палатка справа; питомец заходит внутрь, над палаткой поднимаются «ззз». */
@Composable
private fun TentProps(art: PixelArt, p: Float, cx: Dp, w: Dp, motion: Boolean) {
    val tentCell = 7.dp
    val left = (w - 116.dp).coerceAtLeast(cx + 20.dp)
    val top = FEET - tentCell * 15
    val alpha = if (motion) seg(p, 0f, 0.12f) * (1f - seg(p, 0.92f, 1f)) else 1f
    Sprite(art, "item_tent", Modifier.offset(x = left, y = top).graphicsLayer { this.alpha = alpha }, cell = tentCell)
    if (p >= 0.5f && (p < 0.92f || !motion)) {
        for (i in 0 until 3) {
            val phase = if (motion) ((seg(p, 0.5f, 0.92f) * 2.5f + i / 3f) % 1f) else i / 3f
            Sprite(
                art, "yard_z",
                Modifier
                    .offset(x = left + 76.dp + 18.dp * phase + (i * 4).dp, y = top - 6.dp - 56.dp * phase)
                    .graphicsLayer { this.alpha = if (motion) 1f - phase else 1f },
                cell = 3.dp,
            )
        }
    }
}
