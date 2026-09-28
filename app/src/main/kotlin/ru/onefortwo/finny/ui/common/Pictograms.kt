package ru.onefortwo.finny.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.onefortwo.finny.economy.BudgetCategory
import ru.onefortwo.finny.economy.StatLevel
import ru.onefortwo.finny.ui.theme.FinnyTheme

/**
 * Пиктограммы уровней показателя, результата задания и монеты.
 * Направления плана и служебные знаки — пиксельные значки [PixelIcon]
 * в стиле двора.
 *
 * Все построены на сетке 24 × 24 из простой геометрии: линии, окружности,
 * дуги. Толщина обводки задана в dp и переводится в единицы сетки при
 * отрисовке, поэтому совпадает с контуром направления 1b при любом
 * размере пиктограммы.
 *
 * **Пиктограмма ничего не заменяет.** Рядом с ней всегда стоит подпись:
 * «Забота: В порядке», «Нужное», «Верно». Состояние обязано читаться
 * текстом, а рисунок только помогает его узнать (ТЗ 3.6). Поэтому
 * пиктограммы не озвучиваются: программа чтения с экрана произносит
 * подпись, а не описание рисунка.
 */

/** Размер сетки построения. Все координаты ниже — в этих единицах. */
private const val GRID = 24f

/**
 * Толщина обводки пиктограммы на экране.
 *
 * Задана в dp, а не в единицах сетки: пиктограммы выводятся в двух
 * размерах, и постоянная в единицах сетки давала бы на крупной вдвое
 * более толстую линию — звезда и копилка расплывались в пятно.
 */
private val StrokeWidth = 2.dp

/** Размер пиктограммы в строке текста: рядом с подписью показателя. */
val PictogramSize = 28.dp

/**
 * Уровень показателя: лицо вместо прежних символов `!`, `~`, `+`.
 * Низкий — опущенные уголки, средний — ровная линия, высокий — улыбка
 * и крупные глаза.
 */
@Composable
fun StatLevelIcon(level: StatLevel, modifier: Modifier = Modifier, size: Dp = PictogramSize) {
    val ink = FinnyTheme.colors.onSurface

    Canvas(modifier = modifier.size(size)) {
        onGrid { stroke ->
            val outline = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            drawCircle(color = ink, radius = 10f, center = Offset(12f, 12f), style = outline)

            val eyeR = if (level == StatLevel.HIGH) 1.9f else 1.5f
            drawCircle(ink, radius = eyeR, center = Offset(8.6f, 10f))
            drawCircle(ink, radius = eyeR, center = Offset(15.4f, 10f))

            val mouth = when (level) {
                // Уголки вниз: питомцу чего-то не хватает.
                StatLevel.LOW -> arc(12f, 17f, 4.2f, -2.6f)
                // Ровная линия с едва приподнятыми краями.
                StatLevel.MEDIUM -> arc(12f, 15.6f, 4.2f, 0.7f)
                // Широкая улыбка.
                StatLevel.HIGH -> arc(12f, 14.6f, 5f, 3.2f)
            }
            drawPath(mouth, ink, style = outline)
        }
    }
}

/**
 * Направление плана бюджета: миска корма для нужного, мячик для
 * желаемого, монета для копилки — те же пиксельные значки, что в правилах
 * на экране подарка.
 */
@Composable
fun BudgetDirectionIcon(
    category: BudgetCategory,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
) {
    val id = when (category) {
        BudgetCategory.NEEDS -> "icon_care"
        BudgetCategory.WANTS -> "yard_ball"
        BudgetCategory.SAVINGS -> "coin_0"
    }
    // Значки 7 × 7 клеток: клетка — восьмая часть размера, с полем по краям.
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        PixelIcon(id, cell = size / 8)
    }
}

/** Итог задания: галочка при верном решении, знак внимания при ошибке. */
@Composable
fun TaskResultIcon(correct: Boolean, modifier: Modifier = Modifier, size: Dp = PictogramSize) {
    val ink = if (correct) FinnyTheme.colors.successText else FinnyTheme.colors.warningText

    Canvas(modifier = modifier.size(size)) {
        onGrid { stroke ->
            val outline = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            if (correct) {
                val check = Path().apply {
                    moveTo(5f, 12.6f)
                    lineTo(10f, 17.4f)
                    lineTo(19f, 7f)
                }
                drawPath(check, ink, style = outline)
            } else {
                drawCircle(color = ink, radius = 9.4f, center = Offset(12f, 12f), style = outline)
                drawLine(
                    color = ink,
                    start = Offset(12f, 6.8f),
                    end = Offset(12f, 13.4f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                drawCircle(ink, radius = 1.5f, center = Offset(12f, 17f))
            }
        }
    }
}

/** Монета: круг с внутренним кругом. */
@Composable
fun CoinIcon(modifier: Modifier = Modifier, size: Dp = PictogramSize) {
    val ink = FinnyTheme.colors.warning
    val gold = FinnyTheme.colors.coin

    Canvas(modifier = modifier.size(size)) {
        onGrid { stroke ->
            val outline = Stroke(width = stroke, cap = StrokeCap.Round)
            drawCircle(gold, radius = 9.4f, center = Offset(12f, 12f))
            drawCircle(color = ink, radius = 9.4f, center = Offset(12f, 12f), style = outline)
            drawCircle(color = ink, radius = 4.6f, center = Offset(12f, 12f), style = outline)
        }
    }
}

// --- Вспомогательное -----------------------------------------------------

/** Переводит холст в сетку 24 × 24 спецификации. */
private fun DrawScope.onGrid(block: DrawScope.(stroke: Float) -> Unit) {
    val unit = size.minDimension / GRID
    // Толщина переводится в единицы сетки обратным масштабом: на экране
    // она остаётся прежней при любом размере пиктограммы.
    val stroke = StrokeWidth.toPx() / unit
    scale(scale = unit, pivot = Offset.Zero) { block(stroke) }
}

/**
 * Дуга шириной [halfWidth] в обе стороны от точки. Положительный [rise]
 * поднимает середину вверх, отрицательный опускает.
 */
private fun arc(cx: Float, cy: Float, halfWidth: Float, rise: Float): Path = Path().apply {
    moveTo(cx - halfWidth, cy)
    quadraticTo(cx, cy + rise * 2f, cx + halfWidth, cy)
}

// --- Навигация и служебные знаки -----------------------------------------

/** Стрелка «назад». Описание задаёт кнопка, сама стрелка не озвучивается. */
@Composable
fun ArrowBackIcon(modifier: Modifier = Modifier) {
    PixelIcon("yard_icon_back", modifier, cell = 2.dp)
}

/** Галочка: отметка выбранного варианта. */
@Composable
fun CheckIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Canvas(modifier = modifier.size(size)) {
        onGrid { stroke ->
            drawPath(
                Path().apply {
                    moveTo(5f, 12.6f)
                    lineTo(10f, 17.4f)
                    lineTo(19f, 7f)
                },
                color,
                style = Stroke(width = stroke * 1.4f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

/** Стрелка «открыть» в конце нажимаемой карточки: 8 × 10 клеток. */
@Composable
fun ChevronIcon(modifier: Modifier = Modifier, size: Dp = 20.dp) {
    PixelIcon("ui_chevron", modifier, cell = size / 10)
}

/** Разделы, между которыми переключается приложение. */
enum class NavSection { HOME, TASKS, PET, PROGRESS }
