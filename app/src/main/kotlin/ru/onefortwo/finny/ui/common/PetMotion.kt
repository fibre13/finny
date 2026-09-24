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
import ru.onefortwo.finny.economy.StatLevel
import kotlin.random.Random
import kotlin.random.nextInt

/**
 * Постоянные движения питомца (`art/pixel/src/animation.json`): дыхание,
 * моргание, прыжки радостного питомца и облака в сцене. Реакции на
 * события игры сюда не входят.
 *
 * Ничто не мигает чаще трёх раз в секунду: моргание длится один такт
 * 125 мс и повторяется не чаще раза в три секунды.
 */

/** Кадр движения: кадр дыхания, моргание, смещение прыжка, кадр неба. */
data class MotionFrame(
    val breath: Int = 0,
    val blink: Boolean = false,
    val dy: Int = 0,
    val sky: Int = 0,
)

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
 * кадр: дыхание 0, без моргания и прыжков, первое небо.
 *
 * Такты идут через `withInfiniteAnimationFrameNanos`: в тестах интерфейса
 * бесконечная анимация останавливается, и экран не ждёт её окончания.
 */
@Composable
fun rememberPetMotion(animation: PixelAnimation, care: StatLevel, joy: StatLevel): MotionFrame {
    val context = LocalContext.current
    val enabled = remember { animationsEnabled(context) }
    var frame by remember { mutableStateOf(MotionFrame()) }
    val state by rememberUpdatedState(motionStateFor(care, joy))

    if (enabled) {
        LaunchedEffect(animation) {
            val clock = PetMotionClock(animation)
            var tick = 0
            while (true) {
                withInfiniteAnimationFrameNanos { }
                frame = clock.frameAt(tick++, state)
                delay(animation.tickMs)
            }
        }
    }
    return frame
}
