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
    CLOSE("M18 6 6 18", "m6 6 12 12"),
    GOAL("M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z", "M4 22v-7"),
    CHEVRON("m9 18 6-6-6-6"),
    BACK("m12 19-7-7 7-7", "M19 12H5"),

    // Направления плана: нужное, желаемое (копилка — SAVINGS).
    NEEDS(
        "M12 21a9 9 0 0 0 9-9H3a9 9 0 0 0 9 9Z", "M7 21h10", "M19.5 12 22 6",
        "M16.25 3c.27.1.8.53.75 1.36-.06.83-.93 1.2-1 2.02-.05.78.34 1.24.73 1.62",
        "M11.25 3c.27.1.8.53.74 1.36-.05.83-.93 1.2-.98 2.02-.06.78.33 1.24.72 1.62",
        "M6.25 3c.27.1.8.53.75 1.36-.06.83-.93 1.2-1 2.02-.05.78.34 1.24.74 1.62",
    ),
    WANTS(
        "M11.525 2.295a.53.53 0 0 1 .95 0l2.31 4.679a2.123 2.123 0 0 0 1.595 1.16l5.166.756a.53.53 0 0 1 .294.904l-3.736 3.638a2.123 2.123 0 0 0-.611 1.878l.882 5.14a.53.53 0 0 1-.771.56l-4.618-2.428a2.122 2.122 0 0 0-1.973 0L6.396 21.01a.53.53 0 0 1-.77-.56l.881-5.139a2.122 2.122 0 0 0-.611-1.879L2.16 9.795a.53.53 0 0 1 .294-.906l5.165-.755a2.122 2.122 0 0 0 1.597-1.16z",
    ),

    // Товары каталога покупок.
    WATER("M12 22a7 7 0 0 0 7-7c0-2-1-3.9-3-5.5s-3.5-4-4-6.5c-.5 2.5-2 4.9-4 6.5C6 11.1 5 13 5 15a7 7 0 0 0 7 7z"),
    VITAMINS("m10.5 20.5 10-10a4.95 4.95 0 1 0-7-7l-10 10a4.95 4.95 0 1 0 7 7Z", "m8.5 8.5 7 7"),
    GROOMING(
        "M9.937 15.5A2 2 0 0 0 8.5 14.063l-6.135-1.582a.5.5 0 0 1 0-.962L8.5 9.936A2 2 0 0 0 9.937 8.5l1.582-6.135a.5.5 0 0 1 .963 0L14.063 8.5A2 2 0 0 0 15.5 9.937l6.135 1.581a.5.5 0 0 1 0 .964L15.5 14.063a2 2 0 0 0-1.437 1.437l-1.582 6.135a.5.5 0 0 1-.963 0z",
        "M20 3v4", "M22 5h-4", "M4 17v2", "M5 18H3",
    ),
    BALL(
        "M12 2a10 10 0 1 0 0 20a10 10 0 1 0 0-20z", "M11.1 7.1a16.55 16.55 0 0 1 10.9 4",
        "M12 12a12.6 12.6 0 0 1-8.7 5", "M16.8 13.6a16.55 16.55 0 0 1-9 7.5",
        "M20.7 17a12.8 12.8 0 0 0-8.7-5 13.3 13.3 0 0 1 0-10", "M6.3 3.8a16.55 16.55 0 0 0 1.9 11.5",
    ),
    BOW(
        "M12 11.22C11 9.997 10 9 10 8a2 2 0 0 1 4 0c0 1-.998 2.002-2.01 3.22", "m12 18 2.57-3.5",
        "M6.243 9.016a7 7 0 0 1 11.507-.009", "M9.35 14.53 12 11.22",
        "M9.35 14.53C7.728 12.246 6 10.221 6 7a6 5 0 0 1 12 0c-.005 3.22-1.778 5.235-3.43 7.5l3.557 4.527a1 1 0 0 1-.203 1.43l-1.894 1.36a1 1 0 0 1-1.384-.215L12 18l-2.679 3.593a1 1 0 0 1-1.39.213l-1.865-1.353a1 1 0 0 1-.203-1.422z",
    ),
    TENT("M3.5 21 14 3", "M20.5 21 10 3", "M15.5 21 12 15l-3.5 6", "M2 21h20"),
    STICKERS(
        "M15.5 3H5a2 2 0 0 0-2 2v14c0 1.1.9 2 2 2h14a2 2 0 0 0 2-2V8.5L15.5 3Z", "M14 3v4a2 2 0 0 0 2 2h4",
        "M8 13h.01", "M16 13h.01", "M10 16s.8 1 2 1c1.3 0 2-1 2-1",
    ),
    CLOTHES(
        "M20.38 3.46 16 2a4 4 0 0 1-8 0L3.62 3.46a2 2 0 0 0-1.34 2.23l.58 3.47a1 1 0 0 0 .99.84H6v10c0 1.1.9 2 2 2h8a2 2 0 0 0 2-2V10h2.15a1 1 0 0 0 .99-.84l.58-3.47a2 2 0 0 0-1.34-2.23z",
    ),
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
