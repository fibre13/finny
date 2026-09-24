package ru.onefortwo.finny.ui.common

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import ru.onefortwo.finny.content.PixelAnimation
import ru.onefortwo.finny.economy.GrowthStage
import ru.onefortwo.finny.economy.StatLevel
import ru.onefortwo.finny.ui.state.PetReaction
import kotlin.random.Random
import kotlin.random.nextInt

/**
 * Движения питомца (`art/pixel/src/animation.json`): постоянные — дыхание,
 * моргание, прыжки радостного питомца, облака в сцене — и реакции на
 * события игры.
 *
 * Ничто не мигает чаще трёх раз в секунду: моргание длится один такт
 * 125 мс и повторяется не чаще раза в три секунды.
 */

/**
 * Кадр движения: кадр дыхания, моргание, смещение прыжка, кадр неба и,
 * если идёт реакция, её имя, такт и стадия до роста.
 */
data class MotionFrame(
    val breath: Int = 0,
    val blink: Boolean = false,
    val dy: Int = 0,
    val sky: Int = 0,
    val reaction: String? = null,
    val reactionTick: Int = 0,
    val stageBefore: GrowthStage? = null,
)

/** Шаг хода движений: кадр и реакция, закончившаяся на этом шаге. */
class MotionStep(val frame: MotionFrame, val finishedReaction: Long?)

/** Состояние по показателям: уставший при низкой заботе, радостный при высокой радости. */
fun motionStateFor(care: StatLevel, joy: StatLevel): String = when {
    care == StatLevel.LOW -> "tired"
    joy == StatLevel.HIGH -> "happy"
    else -> "idle"
}

/**
 * Ход движений по тактам. Моргание и прыжки повторяются через случайное
 * число тактов из интервала состояния, дыхание и облака — равномерно.
 */
class PetMotionClock(
    private val animation: PixelAnimation,
    private val random: Random = Random.Default,
) {
    private var nextBlink = -1
    private var blinkEnd = -1
    private var nextJump = -1
    private var jumpStart = -1
    private var active: PetReaction? = null
    private var reactionStart = 0

    /**
     * Последняя законченная реакция. Очередь обновляется не мгновенно, и
     * на следующем такте та же реакция ещё передаётся: без этой отметки
     * она началась бы снова.
     */
    private var lastFinished: Long? = null

    /**
     * Кадр на такте [tick]. Реакция [reaction] начинается, если другой
     * сейчас нет; пока она идёт, прыжки состояния не начинаются, а глаза
     * и прыжки берутся из неё.
     */
    fun step(tick: Int, state: String, reaction: PetReaction?): MotionStep {
        var frame = frameAt(tick, state)
        var finished: Long? = null

        if (active == null && reaction != null && reaction.id != lastFinished) {
            active = reaction
            reactionStart = tick
        }
        val current = active
        if (current != null) {
            val spec = animation.reactions[current.kind]
            val rt = tick - reactionStart
            if (spec == null || rt >= spec.ticks) {
                finished = current.id
                lastFinished = current.id
                active = null
            } else {
                val jump = spec.jumps.firstNotNullOfOrNull { (at, offsets) -> offsets.getOrNull(rt - at) }
                frame = frame.copy(
                    dy = jump ?: 0,
                    blink = frame.blink && spec.eyes == null,
                    reaction = current.kind,
                    reactionTick = rt,
                    stageBefore = current.stageBefore,
                )
            }
        }
        return MotionStep(frame, finished)
    }

    fun frameAt(tick: Int, state: String): MotionFrame {
        val motion = animation.states[state] ?: animation.states.getValue("idle")

        if (nextBlink < 0) nextBlink = tick + random.nextInt(motion.blinkInterval)
        if (tick >= nextBlink) {
            blinkEnd = tick + motion.blinkTicks - 1
            nextBlink = blinkEnd + 1 + random.nextInt(motion.blinkInterval)
        }

        var dy = 0
        val offsets = motion.jumpOffsets
        val interval = motion.jumpInterval
        if (offsets != null && interval != null) {
            if (nextJump < 0) nextJump = tick + random.nextInt(interval)
            if (tick >= nextJump) {
                jumpStart = tick
                nextJump = tick + offsets.size + random.nextInt(interval)
            }
            val step = tick - jumpStart
            if (jumpStart >= 0 && step in offsets.indices) dy = offsets[step]
        }

        return MotionFrame(
            breath = (tick / motion.breathTicks) % 2,
            blink = tick <= blinkEnd,
            dy = dy,
            sky = (tick / animation.skyTicks) % animation.skyFrames.size,
        )
    }
}

/**
 * Анимация включена, если в системных настройках Android не выбрано
 * «Отключить анимацию» (масштаб длительности анимации не ноль).
 */
fun animationsEnabled(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f

/**
 * Текущий кадр движений питомца. При отключённой анимации — неподвижный
 * кадр: дыхание 0, без моргания и прыжков, первое небо; реакция не
 * показывается и сразу считается проигранной.
 *
 * Такты идут через `withInfiniteAnimationFrameNanos`: в тестах интерфейса
 * бесконечная анимация останавливается, и экран не ждёт её окончания.
 *
 * @param reaction реакция, которую нужно проиграть; по окончании
 * вызывается [onReactionEnd] с её идентификатором.
 */
@Composable
fun rememberPetMotion(
    animation: PixelAnimation,
    care: StatLevel,
    joy: StatLevel,
    reaction: PetReaction? = null,
    onReactionEnd: (Long) -> Unit = {},
): MotionFrame {
    val context = LocalContext.current
    val enabled = remember { animationsEnabled(context) }
    var frame by remember { mutableStateOf(MotionFrame()) }
    val state by rememberUpdatedState(motionStateFor(care, joy))
    val currentReaction by rememberUpdatedState(reaction)
    val onEnd by rememberUpdatedState(onReactionEnd)

    if (enabled) {
        LaunchedEffect(animation) {
            val clock = PetMotionClock(animation)
            var tick = 0
            while (true) {
                withInfiniteAnimationFrameNanos { }
                val step = clock.step(tick++, state, currentReaction)
                frame = step.frame
                step.finishedReaction?.let(onEnd)
                delay(animation.tickMs)
            }
        }
    } else if (reaction != null) {
        LaunchedEffect(reaction.id) { onEnd(reaction.id) }
    }
    return frame
}
