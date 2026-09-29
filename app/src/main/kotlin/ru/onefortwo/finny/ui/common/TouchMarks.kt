package ru.onefortwo.finny.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/** Сколько отметка касания остаётся на экране, не меньше, мс. */
private const val MARK_MIN_MS = 1000L

/** Время угасания отметки после отпускания, мс. */
private const val MARK_FADE_MS = 300L

/** Включены ли отметки касаний: окна-диалоги читают значение и рисуют отметки у себя. */
val LocalTouchMarks = staticCompositionLocalOf { false }

/** Отметка одного касания: место, момент нажатия и отпускания. */
private data class TouchMark(val position: Offset, val downAt: Long, val upAt: Long? = null)

/**
 * Отметки касаний для записи видео с эмулятора, где системные отметки
 * («Показывать нажатия») не попадают в запись. Круг стоит на месте
 * касания не меньше секунды и затем угасает. Касания не перехватываются:
 * события читаются на первом проходе и передаются экранам как есть.
 */
@Composable
fun TouchMarks(enabled: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    if (!enabled) {
        content()
        return
    }
    val marks = remember { mutableStateMapOf<Long, TouchMark>() }
    var now by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis { frame ->
                now = frame
                marks.entries.removeAll { (_, m) ->
                    val up = m.upAt ?: return@removeAll false
                    frame > maxOf(up, m.downAt + MARK_MIN_MS) + MARK_FADE_MS
                }
            }
        }
    }
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        event.changes.forEach { change ->
                            val id = change.id.value
                            val mark = marks[id]
                            when {
                                change.pressed && mark == null ->
                                    marks[id] = TouchMark(change.position, now)
                                change.pressed && mark != null && mark.upAt == null ->
                                    marks[id] = mark.copy(position = change.position)
                                !change.pressed && mark != null && mark.upAt == null ->
                                    marks[id] = mark.copy(upAt = now)
                            }
                        }
                    }
                }
            },
    ) {
        CompositionLocalProvider(LocalTouchMarks provides true) { content() }
        Canvas(modifier = Modifier.matchParentSize()) {
            val radius = 30.dp.toPx()
            marks.values.forEach { m ->
                val hold = maxOf(m.upAt ?: now, m.downAt + MARK_MIN_MS)
                val alpha = if (m.upAt != null && now > hold) {
                    (1f - (now - hold).toFloat() / MARK_FADE_MS).coerceIn(0f, 1f)
                } else {
                    1f
                }
                drawCircle(Color.White.copy(alpha = 0.5f * alpha), radius, m.position)
                drawCircle(Color(0xFF1F2A24).copy(alpha = 0.85f * alpha), radius, m.position, style = Stroke(4.dp.toPx()))
            }
        }
    }
}
