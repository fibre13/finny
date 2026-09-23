package ru.onefortwo.finny.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.onefortwo.finny.economy.BudgetCategory
import ru.onefortwo.finny.economy.StatLevel
import ru.onefortwo.finny.ui.theme.LocalAppliqueDecor
import ru.onefortwo.finny.ui.theme.LocalBudgetColors
import ru.onefortwo.finny.ui.theme.LocalHighContrast

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

/** Цвет монеты. Один и тот же в счётчике баланса и в копилке. */
private val Gold = Color(0xFFF2B705)

/**
 * Цвет копилки.
 *
 * Единственная пиктограмма, которая рисует предмет с узнаваемым
 * собственным цветом: синяя свинка читается как ошибка. У миски и звезды
 * такого цвета нет, они остаются в цвете направления.
 *
 * Направление «Копилка» при этом никуда не девается: синим окрашены
 * кромка карточки, полоса и ползунок, а рядом стоит подпись.
 *
 * Порог 3:1 к заливке не применяется — границу фигуры задаёт контур
 * чернилами, как у окраса питомца «белый».
 */
private val PiggyPink = Color(0xFFE07B96)

/** Размер сетки построения. Все координаты ниже — в этих единицах. */
private const val GRID = 24f

/**
 * Толщина обводки пиктограммы на экране. Совпадает с контуром направления
 * 1b, поэтому пиктограмма не выглядит ни легче, ни тяжелее рамки рядом.
 *
 * Задана в dp, а не в единицах сетки: пиктограммы выводятся в двух
 * размерах, и постоянная в единицах сетки давала бы на крупной вдвое
 * более толстую линию — звезда и копилка расплывались в пятно.
 */
private val StrokeWidth = 2.5.dp

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

/** Размер пиктограммы на кнопке раздела главного экрана. */
val MenuPictogramSize = 26.dp

/**
 * Разделы главного экрана. Пиктограмма помогает узнать раздел, но ничего
 * не заменяет: подпись на кнопке остаётся и читается сама по себе
 * (ТЗ 3.6). Рисунки построены одними чернилами, без заливки цветом,
 * поэтому одинаково работают в обеих парах и в чёрно-белом режиме.
 */
enum class MenuSection {
    PLAN,
    SHOP,
    SAVINGS,
    WARDROBE,
    PROGRESS,
    GLOSSARY,
    HELP,
    ADULT,
}

/** Пиктограмма раздела на кнопке главного экрана. */
@Composable
fun MenuIcon(
    section: MenuSection,
    modifier: Modifier = Modifier,
    size: Dp = MenuPictogramSize,
) {
    val ink = LocalAppliqueDecor.current.ink

    Canvas(modifier = modifier.size(size)) {
        onGrid { stroke ->
            val outline = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            when (section) {
                MenuSection.PLAN -> drawPlanSheet(ink, outline)
                MenuSection.SHOP -> drawShoppingBag(ink, outline)
                MenuSection.SAVINGS -> drawPiggy(ink, stroke)
                MenuSection.WARDROBE -> drawHanger(ink, outline)
                MenuSection.PROGRESS -> drawGrowingBars(ink, outline)
                MenuSection.GLOSSARY -> drawOpenBook(ink, outline)
                MenuSection.HELP -> drawQuestion(ink, outline)
                MenuSection.ADULT -> drawLock(ink, outline)
            }
        }
    }
}

/** План: лист со строками — то, что заполняют до начала дня. */
private fun DrawScope.drawPlanSheet(ink: Color, outline: Stroke) {
    drawPath(
        Path().apply {
            addRoundRect(
                RoundRect(
                    Rect(Offset(5f, 3f), Size(14f, 18f)),
                    CornerRadius(2.5f, 2.5f),
                ),
            )
        },
        ink,
        style = outline,
    )
    listOf(8.5f, 12f, 15.5f).forEachIndexed { index, y ->
        // Строки разной длины: лист выглядит заполненным, а не разлинованным.
        val right = if (index == 2) 14.5f else 16f
        drawLine(ink, Offset(8f, y), Offset(right, y), strokeWidth = outline.width, cap = StrokeCap.Round)
    }
}

/** Покупки: сумка с ручкой. */
private fun DrawScope.drawShoppingBag(ink: Color, outline: Stroke) {
    drawPath(
        Path().apply {
            moveTo(5.5f, 8.5f)
            lineTo(18.5f, 8.5f)
            lineTo(17.3f, 20.5f)
            lineTo(6.7f, 20.5f)
            close()
        },
        ink,
        style = outline,
    )
    // Ручка: полуокружность над краем сумки.
    drawPath(
        Path().apply {
            moveTo(9f, 8.5f)
            quadraticTo(9f, 3.5f, 12f, 3.5f)
            quadraticTo(15f, 3.5f, 15f, 8.5f)
        },
        ink,
        style = outline,
    )
}

/** Гардероб: плечики. */
private fun DrawScope.drawHanger(ink: Color, outline: Stroke) {
    drawPath(
        Path().apply {
            moveTo(12f, 8f)
            quadraticTo(12f, 5f, 14f, 5f)
            quadraticTo(15.8f, 5f, 15.4f, 7f)
        },
        ink,
        style = outline,
    )
    drawPath(
        Path().apply {
            moveTo(12f, 8.5f)
            lineTo(3.5f, 16.5f)
            quadraticTo(2.6f, 17.6f, 4.1f, 18.2f)
            lineTo(19.9f, 18.2f)
            quadraticTo(21.4f, 17.6f, 20.5f, 16.5f)
            close()
        },
        ink,
        style = outline,
    )
}

/** Прогресс: три растущих столбика. */
private fun DrawScope.drawGrowingBars(ink: Color, outline: Stroke) {
    drawLine(ink, Offset(4f, 20f), Offset(20f, 20f), strokeWidth = outline.width, cap = StrokeCap.Round)
    listOf(7f to 14.5f, 12f to 10f, 17f to 5.5f).forEach { (x, top) ->
        drawLine(ink, Offset(x, 20f), Offset(x, top), strokeWidth = outline.width * 1.8f, cap = StrokeCap.Round)
    }
}

/** Словарик: раскрытая книга. */
private fun DrawScope.drawOpenBook(ink: Color, outline: Stroke) {
    drawPath(
        Path().apply {
            moveTo(12f, 7f)
            quadraticTo(8.5f, 4.6f, 3.5f, 5.6f)
            lineTo(3.5f, 18.4f)
            quadraticTo(8.5f, 17.4f, 12f, 19.6f)
        },
        ink,
        style = outline,
    )
    drawPath(
        Path().apply {
            moveTo(12f, 7f)
            quadraticTo(15.5f, 4.6f, 20.5f, 5.6f)
            lineTo(20.5f, 18.4f)
            quadraticTo(15.5f, 17.4f, 12f, 19.6f)
        },
        ink,
        style = outline,
    )
    drawLine(ink, Offset(12f, 7f), Offset(12f, 19.6f), strokeWidth = outline.width, cap = StrokeCap.Round)
}

/** Как играть: вопрос в круге. */
private fun DrawScope.drawQuestion(ink: Color, outline: Stroke) {
    drawCircle(ink, radius = 9f, center = Offset(12f, 12f), style = outline)
    drawPath(
        Path().apply {
            moveTo(9.2f, 9.6f)
            quadraticTo(9.2f, 6.6f, 12f, 6.6f)
            quadraticTo(14.9f, 6.6f, 14.9f, 9.4f)
            quadraticTo(14.9f, 11.6f, 12f, 12.6f)
            lineTo(12f, 14.4f)
        },
        ink,
        style = outline,
    )
    drawCircle(ink, radius = outline.width * 0.62f, center = Offset(12f, 17.2f))
}

/** Для взрослого: закрытый замок — раздел за барьером. */
private fun DrawScope.drawLock(ink: Color, outline: Stroke) {
    drawPath(
        Path().apply {
            moveTo(8f, 10.5f)
            lineTo(8f, 7.8f)
            quadraticTo(8f, 4.4f, 12f, 4.4f)
            quadraticTo(16f, 4.4f, 16f, 7.8f)
            lineTo(16f, 10.5f)
        },
        ink,
        style = outline,
    )
    drawPath(
        Path().apply {
            addRoundRect(
                RoundRect(
                    Rect(Offset(5.2f, 10.5f), Size(13.6f, 9.6f)),
                    CornerRadius(2.4f, 2.4f),
                ),
            )
        },
        ink,
        style = outline,
    )
    drawCircle(ink, radius = 1.5f, center = Offset(12f, 15.3f), style = outline)
}

/**
 * Уровень показателя: лицо вместо прежних символов `!`, `~`, `+`.
 * Низкий — опущенные уголки, средний — ровная линия, высокий — улыбка
 * и крупные глаза.
 */
@Composable
fun StatLevelIcon(level: StatLevel, modifier: Modifier = Modifier, size: Dp = PictogramSize) {
    val ink = LocalAppliqueDecor.current.ink

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
    val ink = LocalAppliqueDecor.current.ink
    val paper = MaterialTheme.colorScheme.surface
    val hatched = LocalHighContrast.current
    val fill = when (category) {
        BudgetCategory.NEEDS -> palette.needs
        BudgetCategory.WANTS -> palette.wants
        BudgetCategory.SAVINGS -> palette.savings
    }

    Canvas(modifier = modifier.size(size)) {
        onGrid { stroke ->
            // В чёрно-белом режиме цвет направления не различает: все три
            // чёрные. Различает штриховка — и подпись рядом, как всегда.
            if (hatched) {
                drawHatch(category, ink, paper, stroke)
            } else {
                when (category) {
                    BudgetCategory.NEEDS -> drawBowl(fill, ink, stroke)
                    BudgetCategory.WANTS -> drawStar(fill, ink, stroke)
                    BudgetCategory.SAVINGS -> drawPiggy(ink, stroke)
                }
            }
        }
    }
}

/**
 * Обозначение направления штриховкой: квадрат с наклоном линий, своим
 * для каждого направления. Нужное — вправо вверх, Хочу — вправо вниз,
 * Копилка — вертикально.
 *
 * Вместо рисунка, а не поверх него: при предельном контуре и одном цвете
 * миска, звезда и копилка перестают различаться силуэтом.
 */
private fun DrawScope.drawHatch(
    category: BudgetCategory,
    ink: Color,
    paper: Color,
    stroke: Float,
) {
    val box = Path().apply {
        addRoundRect(
            RoundRect(
                left = 2f,
                top = 2f,
                right = 22f,
                bottom = 22f,
                cornerRadius = CornerRadius(5f, 5f),
            ),
        )
    }
    drawPath(box, paper)

    val degrees = when (category) {
        BudgetCategory.NEEDS -> -45f
        BudgetCategory.WANTS -> 45f
        BudgetCategory.SAVINGS -> 0f
    }

    clipPath(box) {
        rotate(degrees = degrees, pivot = Offset(12f, 12f)) {
            // Линии ведутся с запасом в обе стороны: после поворота
            // квадрат выходит за прежние границы.
            var x = -8f
            while (x <= 32f) {
                drawLine(
                    color = ink,
                    start = Offset(x, -8f),
                    end = Offset(x, 32f),
                    strokeWidth = stroke,
                )
                x += 4.6f
            }
        }
    }

    drawPath(box, ink, style = Stroke(width = stroke))
}

/** Итог задания: галочка при верном решении, знак внимания при ошибке. */
@Composable
fun TaskResultIcon(correct: Boolean, modifier: Modifier = Modifier, size: Dp = PictogramSize) {
    val ink = LocalAppliqueDecor.current.ink

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
    val ink = LocalAppliqueDecor.current.ink
    // В чёрно-белом режиме золотой заливки нет: контраст жёлтого к белому
    // ниже порога, и монета читалась бы одним контуром.
    val gold = if (LocalHighContrast.current) {
        MaterialTheme.colorScheme.surface
    } else {
        Gold
    }

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
