package ru.onefortwo.finny.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
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
import ru.onefortwo.finny.ui.theme.LocalBudgetColors

/**
 * Пиктограммы направлений, уровней показателя и результата задания.
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

/** Цвет монеты в прорези копилки. */
private val Gold = Color(0xFFF2B705)

/**
 * Цвет копилки.
 *
 * Единственная пиктограмма, которая рисует предмет с узнаваемым
 * собственным цветом: синяя свинка читается как ошибка. У миски и звезды
 * такого цвета нет, они остаются в цвете направления.
 *
 * Направление «Копилка» при этом никуда не девается: синим окрашены
 * кромка карточки и полоса, а рядом стоит подпись.
 *
 * Порог 3:1 к заливке не применяется — границу фигуры задаёт контур
 * чернилами, как у окраса питомца «белый».
 */
private val PiggyPink = Color(0xFFE07B96)

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
 * Размер пиктограммы направления. Больше строчной: миска, звезда и копилка
 * состоят из нескольких частей и при 28 dp сливаются в пятно — проверено
 * на устройстве. Спецификация направления задаёт для них 56 dp; здесь
 * взято меньшее значение, чтобы пиктограмма не перевешивала заголовок
 * раздела, но деталь оставалась различимой.
 */
val DirectionPictogramSize = 48.dp

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
 * Направление плана бюджета: миска с паром для обязательного, звезда для
 * желаемого, копилка-свинка для накоплений.
 */
@Composable
fun BudgetDirectionIcon(
    category: BudgetCategory,
    modifier: Modifier = Modifier,
    size: Dp = DirectionPictogramSize,
) {
    val palette = LocalBudgetColors.current
    val ink = FinnyTheme.colors.onSurface
    val fill = when (category) {
        BudgetCategory.NEEDS -> palette.needs
        BudgetCategory.WANTS -> palette.wants
        BudgetCategory.SAVINGS -> palette.savings
    }

    Canvas(modifier = modifier.size(size)) {
        onGrid { stroke ->
            when (category) {
                BudgetCategory.NEEDS -> drawBowl(fill, ink, stroke)
                BudgetCategory.WANTS -> drawStar(fill, ink, stroke)
                BudgetCategory.SAVINGS -> drawPiggy(ink, stroke)
            }
        }
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

// --- Рисунки направлений -------------------------------------------------

private fun DrawScope.drawBowl(fill: Color, ink: Color, stroke: Float) {
    val outline = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)

    // Два завитка пара над миской.
    listOf(9f, 15f).forEach { x ->
        val steam = Path().apply {
            moveTo(x, 9.6f)
            quadraticTo(x - 2.4f, 6.6f, x, 3.4f)
        }
        drawPath(steam, ink, style = outline)
    }

    // Чаша: глубокая, иначе при обводке 2,6 от неё остаётся одна линия.
    val bowl = Path().apply {
        moveTo(4.4f, 13f)
        quadraticTo(12f, 22.4f, 19.6f, 13f)
        close()
    }
    drawPath(bowl, fill)
    drawPath(bowl, ink, style = outline)

    // Бортик.
    drawLine(
        color = ink,
        start = Offset(2.8f, 13f),
        end = Offset(21.2f, 13f),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawStar(fill: Color, ink: Color, stroke: Float) {
    val outline = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val star = Path()
    val cx = 12f
    val cy = 12.6f
    val outer = 9.4f
    val inner = 4.1f

    for (i in 0 until 10) {
        val r = if (i % 2 == 0) outer else inner
        // Первый луч направлен вверх: -90° и шаг 36°.
        val angle = Math.toRadians((-90 + i * 36).toDouble())
        val x = cx + r * kotlin.math.cos(angle).toFloat()
        val y = cy + r * kotlin.math.sin(angle).toFloat()
        if (i == 0) star.moveTo(x, y) else star.lineTo(x, y)
    }
    star.close()

    drawPath(star, fill)
    drawPath(star, ink, style = outline)
}

/**
 * Копилка-свинка: монета над прорезью, пятачок сбоку, завиток хвоста.
 *
 * Части крупные и скруглённые: четыре тонкие ножки и острое треугольное
 * ухо при 48 dp сливались в тёмную бахрому, а знак выходил колючим.
 * Две широкие ножки и скруглённое ухо читаются на обоих размерах.
 *
 * Силуэт собирается объединением путей, а не рисуется частями поверх
 * друг друга: иначе на стыке ножек и пятачка с туловищем остаётся линия
 * контура, и фигура распадается на слепленные куски.
 */
private fun DrawScope.drawPiggy(ink: Color, stroke: Float) {
    val outline = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)

    // Хвост-завиток рисуется первым: он уходит за туловище.
    val tail = Path().apply {
        moveTo(4.2f, 12.6f)
        quadraticTo(0.8f, 11.8f, 2.0f, 9.8f)
        quadraticTo(3.0f, 8.6f, 4.2f, 10.4f)
    }
    drawPath(tail, ink, style = outline)

    val silhouette = Path().apply {
        addOval(Rect(left = 3.0f, top = 6.8f, right = 19.4f, bottom = 20.0f))
    }

    // Ухо, пятачок и ножки приращиваются к туловищу, а не кладутся поверх.
    listOf(
        Path().apply {
            moveTo(14.4f, 8.2f)
            quadraticTo(15.6f, 4.4f, 18.2f, 5.6f)
            quadraticTo(18.8f, 7.6f, 18.2f, 9.2f)
            close()
        },
        Path().apply {
            addRoundRect(
                RoundRect(
                    left = 17.0f,
                    top = 10.6f,
                    right = 22.2f,
                    bottom = 16.2f,
                    cornerRadius = CornerRadius(2.4f, 2.4f),
                ),
            )
        },
        legPath(6.0f),
        legPath(13.4f),
    ).forEach { part ->
        silhouette.op(silhouette, part, PathOperation.Union)
    }

    drawPath(silhouette, PiggyPink)
    drawPath(silhouette, ink, style = outline)

    // Прорезь для монет на спине.
    val slot = Path().apply {
        moveTo(6.2f, 9.9f)
        quadraticTo(10.6f, 8.5f, 15.0f, 9.9f)
    }
    drawPath(slot, ink, style = outline)

    // Монета в прорези.
    drawCircle(Gold, radius = 2.7f, center = Offset(10.6f, 3.9f))
    drawCircle(color = ink, radius = 2.7f, center = Offset(10.6f, 3.9f), style = outline)

    // Глаз и ноздря.
    drawCircle(ink, radius = 1.1f, center = Offset(16.4f, 11.8f))
    drawCircle(ink, radius = 0.95f, center = Offset(20.7f, 13.3f))
}

/** Ножка копилки: скруглённый столбик, приращиваемый к туловищу. */
private fun legPath(left: Float): Path = Path().apply {
    addRoundRect(
        RoundRect(
            left = left,
            top = 17.2f,
            right = left + 3.4f,
            bottom = 21.2f,
            cornerRadius = CornerRadius(1.7f, 1.7f),
        ),
    )
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
fun ArrowBackIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    LineIcon(LineGlyph.BACK, color, modifier, size)
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

/** Шеврон «открыть» в конце нажимаемой карточки. */
@Composable
fun ChevronIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    LineIcon(LineGlyph.CHEVRON, color, modifier, size)
}

/** Вкладки нижней навигации. */
enum class NavSection(val glyph: LineGlyph) {
    HOME(LineGlyph.HOME),
    TASKS(LineGlyph.TASKS),
    PET(LineGlyph.PET),
    PROGRESS(LineGlyph.PROGRESS),
}

/**
 * Пиктограмма вкладки. Подпись вкладки видна всегда, рисунок её не
 * заменяет (ТЗ 3.6).
 */
@Composable
fun NavIcon(section: NavSection, color: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    LineIcon(section.glyph, color, modifier, size)
}
