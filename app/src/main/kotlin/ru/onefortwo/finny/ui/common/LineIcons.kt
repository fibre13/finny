package ru.onefortwo.finny.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Линейные значки интерфейса в одном стиле: сетка 24 × 24, линия 2,
 * скруглённые концы. Контуры взяты из набора Lucide (лицензия ISC,
 * см. docs/11-лицензии.md) — того же, из которого собраны значки
 * макета в Figma, поэтому приложение и макет совпадают.
 *
 * Значок ничего не заменяет: рядом с ним всегда стоит подпись или у
 * кнопки есть описание для программы чтения с экрана (ТЗ 3.6).
 */
enum class LineGlyph(vararg val paths: String) {
    PLAN(
        "M9 2h6a1 1 0 0 1 1 1v2a1 1 0 0 1-1 1H9a1 1 0 0 1-1-1V3a1 1 0 0 1 1-1z",
        "M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2",
        "M12 11h4", "M12 16h4", "M8 11h.01", "M8 16h.01",
    ),
    SHOP(
        "m15 11-1 9", "m19 11-4-7", "M2 11h20",
        "m3.5 11 1.6 7.4a2 2 0 0 0 2 1.6h9.8a2 2 0 0 0 2-1.6l1.7-7.4",
        "M4.5 15.5h15", "m5 11 4-7", "m9 11 1 9",
    ),
    SAVINGS(
        "M19 5c-1.5 0-2.8 1.4-3 2-3.5-1.5-11-.3-11 5 0 1.8 0 3 2 4.5V20h4v-2h3v2h4v-4c1-.5 1.7-1 2-2h2v-4h-2c0-1-.5-1.5-1-2V5z",
        "M2 9v1c0 1.1.9 2 2 2h1", "M16 11h.01",
    ),
    GLOSSARY(
        "M4 19.5v-15A2.5 2.5 0 0 1 6.5 2H19a1 1 0 0 1 1 1v18a1 1 0 0 1-1 1H6.5a1 1 0 0 1 0-5H20",
        "m8 13 4-7 4 7", "M9.1 11h5.7",
    ),
    HELP(
        "M12 2a10 10 0 1 0 0 20a10 10 0 1 0 0-20z",
        "M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3", "M12 17h.01",
    ),
    ADULT(
        "M5 11h14a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2z",
        "M7 11V7a5 5 0 0 1 10 0v4",
    ),
    HOME(
        "M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8",
        "M3 10a2 2 0 0 1 .709-1.528l7-5.999a2 2 0 0 1 2.582 0l7 5.999A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z",
    ),
    TASKS(
        "M12 7v14",
        "M3 18a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1h5a4 4 0 0 1 4 4 4 4 0 0 1 4-4h5a1 1 0 0 1 1 1v13a1 1 0 0 1-1 1h-6a3 3 0 0 0-3 3 3 3 0 0 0-3-3z",
    ),
    PET(
        "M12 5c.67 0 1.35.09 2 .26 1.78-2 5.03-2.84 6.42-2.26 1.4.58-.42 7-.42 7 .57 1.07 1 2.24 1 3.44C21 17.9 16.97 21 12 21s-9-3-9-7.56c0-1.25.5-2.4 1-3.44 0 0-1.89-6.42-.5-7 1.39-.58 4.72.23 6.5 2.23A9.04 9.04 0 0 1 12 5Z",
        "M8 14v.5", "M16 14v.5", "M11.25 16.25h1.5L12 17l-.75-.75Z",
    ),
    PROGRESS("M3 3v16a2 2 0 0 0 2 2h16", "M18 17V9", "M13 17V5", "M8 17v-3"),
    NEXT_TASK(
        "M19.43 12.935c.357-.967.57-1.955.57-2.935a8 8 0 0 0-16 0c0 4.993 5.539 10.193 7.399 11.799a1 1 0 0 0 1.202 0 32.197 32.197 0 0 0 .813-.728",
        "M12 7a3 3 0 1 0 0 6a3 3 0 1 0 0-6z", "m16 18 2 2 4-4",
    ),
    CLOCK("M12 2a10 10 0 1 0 0 20a10 10 0 1 0 0-20z", "M12 6v6l4 2"),
    GOAL("M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z", "M4 22v-7"),
    CHEVRON("m9 18 6-6-6-6"),
    BACK("m12 19-7-7 7-7", "M19 12H5"),
}

/** Разобранные контуры значков: разбираются один раз на всё приложение. */
private val parsed = mutableMapOf<LineGlyph, List<Path>>()

private fun pathsOf(glyph: LineGlyph): List<Path> = parsed.getOrPut(glyph) {
    glyph.paths.map { PathParser().parsePathString(it).toPath() }
}

/** Линейный значок заданного цвета. Не озвучивается: смысл несёт подпись. */
@Composable
fun LineIcon(
    glyph: LineGlyph,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
) {
    val paths = remember(glyph) { pathsOf(glyph) }

    Canvas(modifier = modifier.size(size)) {
        val unit = this.size.width / 24f
        scale(scale = unit, pivot = androidx.compose.ui.geometry.Offset.Zero) {
            val stroke = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            paths.forEach { drawPath(it, color, style = stroke) }
        }
    }
}
