package ru.onefortwo.finny.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import ru.onefortwo.finny.content.PetPartsContent
import ru.onefortwo.finny.ui.common.PetFigure
import ru.onefortwo.finny.economy.StatLevel
import ru.onefortwo.finny.ui.common.motionAllowed
import ru.onefortwo.finny.ui.common.rememberPixelArt
import ru.onefortwo.finny.ui.state.AppState
import ru.onefortwo.finny.ui.state.PetReaction
import ru.onefortwo.finny.ui.state.PetReactions
import ru.onefortwo.finny.ui.state.PetVoice

/*
 * Мечта получена — питомец ею пользуется: катается на самокате, несёт
 * аквариум и разглядывает рыбок, празднует с друзьями. Это награда за
 * сезоны накоплений, поэтому сцена идёт неторопливо и повторяется, пока
 * ребёнок сам не нажмёт «Дальше». При выключенных движениях — одна
 * неподвижная картинка.
 */

/** Длительность одного круга сцены, мс. */
private const val DREAM_LOOP_MS = 5200

@Composable
fun DreamScene(state: AppState, parts: PetPartsContent, goalId: String, goalTitle: String, onDone: () -> Unit) {
    val art = rememberPixelArt()
    val colors = remember(art) { YardColors(art) }
    val petName = state.profile?.petName ?: "Питомец"
    val motion = motionAllowed()
    val t = if (motion) {
        val loop = rememberInfiniteTransition(label = "dream")
        val value by loop.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(DREAM_LOOP_MS, easing = LinearEasing), RepeatMode.Restart),
            label = "t",
        )
        value
    } else {
        0.6f
    }
    val saying = when (goalId) {
        "scooter" -> "Юху! Как классно! Спасибо!"
        "aquarium" -> "Какие красивые рыбки! Спасибо!"
        else -> "Ура! Праздник с друзьями! Спасибо!"
    }
    val scene = when (goalId) {
        "scooter" -> "$petName катается на самокате и радуется"
        "aquarium" -> "$petName несёт аквариум, ставит его и разглядывает рыбок"
        else -> "$petName празднует с друзьями: шарики, угощение, все прыгают от радости"
    }

    MeadowBackground(art, grassFrom = 0.5f) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "«$goalTitle» — твой!",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().semantics { heading() },
                )
                SpeechBubble(colors, PetVoice.of(state.game.stage, saying), modifier = Modifier.padding(top = 12.dp))
                Spacer(modifier = Modifier.height(24.dp))
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .semantics { contentDescription = scene },
                ) {
                    val w = maxWidth
                    when (goalId) {
                        "scooter" -> ScooterRide(state, parts, goalId, t, w)
                        "aquarium" -> AquariumCarry(state, parts, goalId, t, w)
                        else -> FriendsParty(state, parts, goalId, t, w, motion)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                PixelButton(text = "Дальше", onClick = onDone)
            }
        }
    }
}

/** Питомец ребёнка без плитки. */
@Composable
private fun Me(state: AppState, parts: PetPartsContent, size: Dp, reaction: PetReaction? = null) {
    ProfilePet(state = state, parts = parts, size = size, reaction = reaction)
}

/** Самокат едет через луг, питомец стоит на нём; позади — полоски ветра. */
@Composable
private fun ScooterRide(state: AppState, parts: PetPartsContent, goalId: String, t: Float, w: Dp) {
    // Два проезда за круг: туда быстро, обратно — медленно и ближе.
    val pass = if (t < 0.5f) t / 0.5f else (t - 0.5f) / 0.5f
    val x = (w + 200.dp) * pass - 200.dp
    val bob = (sin(t * PI * 16).toFloat() * 3).dp
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize().clearAndSetSemantics { }) {
            val c = 3.dp.toPx()
            for (i in 0 until 4) {
                val lx = x.toPx() - (30 + i * 14) * c / 3
                val ly = size.height - (60 + i * 22).dp.toPx()
                drawRect(Color.White.copy(alpha = 0.8f), Offset(lx - 16 * c, ly), Size(10 * c, c))
            }
        }
        // Самокат развёрнут по ходу движения; питомец стоит на деке.
        Box(modifier = Modifier.offset(x = x, y = 110.dp + bob)) {
            GoalPicture(
                goalId = goalId,
                size = 170.dp,
                container = Color.Transparent,
                modifier = Modifier.offset(y = 40.dp).graphicsLayer { scaleX = -1f },
            )
            Box(modifier = Modifier.offset(x = 30.dp, y = 22.dp)) { Me(state, parts, 130.dp) }
        }
    }
}

/** Питомец несёт аквариум, ставит его и разглядывает рыбок; из аквариума поднимаются пузырьки. */
@Composable
private fun AquariumCarry(state: AppState, parts: PetPartsContent, goalId: String, t: Float, w: Dp) {
    val walk = (t / 0.45f).coerceAtMost(1f)
    val carrying = t < 0.45f
    val petX = (w / 2 - 120.dp) * walk - 60.dp * (1 - walk)
    val step = if (carrying) abs(sin(t * PI * 18).toFloat()) * 6 else 0f
    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.offset(x = petX, y = 150.dp - step.dp)) { Me(state, parts, 120.dp) }
        if (carrying) {
            // Аквариум — в лапах над головой: низ картинки касается макушки.
            GoalPicture(goalId = goalId, size = 96.dp, container = Color.Transparent, modifier = Modifier.offset(x = petX + 12.dp, y = 118.dp - step.dp))
        } else {
            val aqX = w / 2 - 5.dp
            GoalPicture(goalId = goalId, size = 130.dp, container = Color.Transparent, modifier = Modifier.offset(x = aqX, y = 150.dp))
            Canvas(modifier = Modifier.fillMaxSize().clearAndSetSemantics { }) {
                val r = 3.dp.toPx()
                val phase = (t - 0.45f) / 0.55f
                for (i in 0 until 5) {
                    val p = (phase * 1.6f + i * 0.21f) % 1f
                    val bx = (aqX + 40.dp + (i * 13).dp).toPx()
                    val by = (180.dp).toPx() - p * 110.dp.toPx()
                    drawCircle(Color(0xFF8EC9F0).copy(alpha = 1f - p), radius = r * (1 + i % 2), center = Offset(bx, by))
                }
            }
        }
    }
}

/** Праздник: шарики, друзья-зверята прыгают вокруг питомца, сверху падают конфетти. */
@Composable
private fun FriendsParty(state: AppState, parts: PetPartsContent, goalId: String, t: Float, w: Dp, motion: Boolean) {
    val art = rememberPixelArt()
    val mine = state.profile?.appearance?.speciesId
    // Друзья — другие зверята и другие окрасы.
    val friends = parts.species.filter { it.id != mine }.take(2).mapIndexed { i, s -> s to parts.colors[(i + 1) % parts.colors.size] }
    val jump = (t * 4).toInt().toLong()
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize().clearAndSetSemantics { }) {
            val c = 3.dp.toPx()
            val tints = listOf(Color(0xFFE8604C), Color(0xFFF2C94C), Color(0xFF5B8DB8), Color(0xFF6FB36A), Color(0xFFE88BB0))
            for (i in 0 until 18) {
                val fx = (i * 37 % 100) / 100f
                val fy = ((i * 53 % 100) / 100f + t) % 1f
                drawRect(tints[i % tints.size], Offset(size.width * fx, size.height * fy * 0.7f), Size(2 * c, c))
            }
        }
        val sway = (sin(t * PI * 4).toFloat() * 6).dp
        Sprite(art, "yard_balloon_red", Modifier.offset(x = 6.dp, y = 10.dp + sway), cell = 3.dp)
        Sprite(art, "yard_balloon_yellow", Modifier.offset(x = w - 50.dp, y = 4.dp - sway), cell = 3.dp)
        Sprite(art, "yard_balloon_blue", Modifier.offset(x = w - 100.dp, y = 24.dp + sway), cell = 3.dp)
        GoalPicture(goalId = goalId, size = 90.dp, container = Color.Transparent, modifier = Modifier.offset(x = w / 2 + 10.dp, y = 215.dp))
        friends.forEachIndexed { i, (species, color) ->
            val x = if (i == 0) 0.dp else w - 100.dp
            PetFigure(
                petName = species.title,
                speciesId = species.id,
                speciesTitle = species.title,
                accessoryId = "none",
                accessoryTitle = "",
                colorHex = color.hex,
                stage = ru.onefortwo.finny.economy.GrowthStage.BABY,
                care = StatLevel.HIGH,
                joy = StatLevel.HIGH,
                size = 100.dp,
                caption = false,
                plain = true,
                reaction = if (motion) PetReaction(PetReactions.PLAY, id = jump * 3 + i) else null,
                modifier = Modifier.offset(x = x, y = 150.dp).width(100.dp),
            )
        }
        Box(modifier = Modifier.offset(x = w / 2 - 65.dp, y = 110.dp)) {
            Me(state, parts, 130.dp, if (motion) PetReaction(PetReactions.PLAY, id = jump * 3 + 2) else null)
        }
    }
}
