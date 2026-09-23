package ru.onefortwo.finny.ui.common

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import ru.onefortwo.finny.economy.GrowthStage
import ru.onefortwo.finny.economy.StatLevel

/**
 * Отрисовка питомца по послойной схеме из визуальной системы
 * (docs/design/design-system/components/pet.html).
 *
 * Слои в порядке отрисовки: хвост, уши, корпус, голова, морда, выражение,
 * украшение. Сочетаний вида, окраса, украшения и стадии — 81, поэтому
 * фигура не хранится изображениями: силуэт считается по пропорциям стадии,
 * окрас подставляется значением, украшение и выражение рисуются поверх.
 *
 * Выбрана отрисовка на Canvas, а не набор Vector Drawable: спецификация
 * задана формулами (размеры головы и корпуса на каждой стадии, вынос уха
 * как доля от ширины головы), и в коде они выражаются напрямую, без девяти
 * отдельных файлов.
 *
 * Система координат 200 × 200, как в спецификации; при отрисовке
 * масштабируется под фактический размер.
 */

/** Размер холста спецификации. Все координаты ниже — в этих единицах. */
private const val CANVAS = 200f

/**
 * Цвет лица: глаза, рот, нос. Постоянный, потому что рисуется по окрасу
 * питомца, а не по фону: окрасы светлые, и тёмные черты на них читаются
 * при любой паре.
 *
 * Контур силуэта — другое дело: он проходит по границе с фоном и цвет
 * берёт у пары, поэтому передаётся отдельно ([drawPet]).
 */
private val Ink = Color(0xFF23201A)

/**
 * Пропорции фигуры на каждой стадии. Стадия меняет соотношение головы и
 * корпуса и общий размер, но не персонажа: у малыша голова крупнее,
 * у взрослого корпус вытянутее.
 */
private data class StageMetrics(
    val headRx: Float,
    val headRy: Float,
    val headCy: Float,
    val bodyRx: Float,
    val bodyRy: Float,
    val bodyCy: Float,
    val figureScale: Float,
    /** Уши растут мягче фигуры, иначе у малыша выглядят лопухами. */
    val earScale: Float,
)

/**
 * Толщина контура в единицах холста. Соответствует 2,5 dp направления 1b
 * при размере фигуры 140 dp: 2,5 / 140 × 200.
 */
private const val STROKE = 3.6f

/**
 * Насколько корпус поднимается под голову.
 *
 * По числам спецификации голова и корпус только соприкасаются: низ головы
 * малыша на 109,4, верх корпуса на 110. Две обведённые окружности,
 * касающиеся в точке, читаются как снеговик, поэтому перекрытие нужно.
 *
 * Значение подобрано на устройстве дважды. Первое, 16, оказалось велико:
 * голова заметно заезжала на корпус, и шея пропадала. Восемь дают тот же
 * цельный силуэт, но линия головы поверх корпуса остаётся видимой.
 *
 * Это именно перекрытие, а не поправка к центру корпуса. Раньше восемь
 * вычитались из центра, и при разной геометрии стадий перекрытие выходило
 * разным: 7,4 у малыша, 12,4 у подростка, 15,4 у взрослого — настраивали
 * на малыше, а у взрослого голова заезжала вдвое глубже. Теперь центр
 * корпуса выводится из размеров головы и корпуса, и перекрытие на всех
 * стадиях одинаково.
 */
private const val BODY_OVERLAP = 8f

/**
 * Метрики стадии. Центр корпуса не задаётся числом, а считается: низ
 * головы плюс полувысота корпуса минус перекрытие.
 */
private fun figure(
    headRx: Float,
    headRy: Float,
    headCy: Float,
    bodyRx: Float,
    bodyRy: Float,
    figureScale: Float,
    earScale: Float,
): StageMetrics = StageMetrics(
    headRx = headRx,
    headRy = headRy,
    headCy = headCy,
    bodyRx = bodyRx,
    bodyRy = bodyRy,
    bodyCy = headCy + headRy + bodyRy - BODY_OVERLAP,
    figureScale = figureScale,
    earScale = earScale,
)

private fun metricsFor(stage: GrowthStage): StageMetrics = when (stage) {
    GrowthStage.BABY -> figure(45f, 41f, 68.4f, 40f, 35f, 0.92f, 0.90f)

    GrowthStage.TEEN -> figure(42f, 38.5f, 65.9f, 45f, 40f, 0.99f, 0.96f)

    GrowthStage.ADULT -> figure(39f, 36f, 63.4f, 50f, 44f, 1.048f, 1.00f)
}

/**
 * Рисует питомца целиком.
 *
 * @param speciesId вид из `pet_parts.json`: `cat`, `dog`, `rabbit`.
 * @param accessoryId украшение: `none`, `bow`, `scarf`.
 * @param care уровень заботы; задаёт прищур при низком значении.
 * @param joy уровень радости; задаёт форму глаз и рта.
 */
fun DrawScope.drawPet(
    speciesId: String,
    baseColor: Color,
    accessoryId: String,
    stage: GrowthStage,
    care: StatLevel,
    joy: StatLevel,
    /**
     * Вписывать фигуру в размер холста. Для отрисовки внутри сцены
     * выключается: там координаты 200 × 200 уже установлены сдвигом
     * и уменьшением.
     */
    fitToCanvas: Boolean = true,
    /**
     * Цвет контура силуэта. Берётся у пары: на тёмной роль чернил играет
     * светлый цвет, иначе контур совпадает с фоном и граница фигуры
     * пропадает — а он для того и нужен, чтобы граница не опиралась
     * на цветовой контраст (ТЗ 3.6).
     */
    outlineColor: Color = Ink,
) {
    val m = metricsFor(stage)

    // Затенение и светлая часть выводятся из окраса смешиванием, поэтому
    // новый окрас стоит одного значения и не требует своей палитры.
    val shade = lerp(baseColor, Ink, 0.18f)
    val light = lerp(baseColor, Color.White, 0.55f)

    // Вписывание в холст выполняется только при самостоятельной
    // отрисовке. Внутри сцены систему координат задаёт вызывающий:
    // иначе фигура пыталась бы заполнить всю сцену и вылезала за край.
    val unit = if (fitToCanvas) size.minDimension / CANVAS else 1f

    scale(scale = unit, pivot = Offset.Zero) {
        scale(scale = m.figureScale, pivot = Offset(CANVAS / 2f, CANVAS / 2f)) {
            drawTail(m, speciesId, shade, outlineColor)
            drawEars(m, speciesId, baseColor, shade, outlineColor)
            drawBody(m, baseColor, light)
            drawHead(m, speciesId, baseColor, light)
            // Контур поверх заливок: он задаёт границу фигуры, не опираясь
            // на цветовой контраст с фоном (ТЗ 3.6). Рисуется до лица
            // и украшения, чтобы не обводить их дважды.
            drawSilhouetteStroke(m, speciesId, outlineColor)
            drawFace(m, care, joy)
            drawAccessory(m, accessoryId)
        }
    }
}

// --- Слой 1. Силуэт ------------------------------------------------------

private fun DrawScope.drawTail(
    m: StageMetrics,
    speciesId: String,
    shade: Color,
    outline: Color,
) {
    if (speciesId == "rabbit") {
        // У крольчонка хвост — круг, а не вытянутый эллипс.
        val center = Offset(CANVAS / 2f + m.bodyRx * 0.86f, m.bodyCy + m.bodyRy * 0.42f)
        drawCircle(color = shade, radius = 13f, center = center)
        drawCircle(color = outline, radius = 13f, center = center, style = Stroke(width = STROKE))
        return
    }

    val cx = CANVAS / 2f + m.bodyRx * 0.82f
    val cy = m.bodyCy + m.bodyRy * 0.18f
    rotate(degrees = -28f, pivot = Offset(cx, cy)) {
        drawOval(color = shade, topLeft = Offset(cx - 9f, cy - 30f), size = Size(18f, 60f))
        drawOval(
            color = outline,
            topLeft = Offset(cx - 9f, cy - 30f),
            size = Size(18f, 60f),
            style = Stroke(width = STROKE),
        )
    }
}

private fun DrawScope.drawEars(
    m: StageMetrics,
    speciesId: String,
    base: Color,
    shade: Color,
    outline: Color,
) {
    val cx = CANVAS / 2f
    val crown = m.headCy - m.headRy

    when (speciesId) {
        "dog" -> {
            // Висячие уши по бокам головы: вынос почти во всю её ширину.
            val dx = m.headRx * 0.94f
            val cy = m.headCy + m.headRy * 0.05f
            val rx = 14f * m.earScale
            val ry = 30f * m.earScale
            listOf(-1f to -26f, 1f to 26f).forEach { (side, angle) ->
                val ex = cx + dx * side
                rotate(degrees = angle, pivot = Offset(ex, cy)) {
                    drawOval(base, Offset(ex - rx, cy - ry), Size(rx * 2, ry * 2))
                    drawOval(
                        color = shade,
                        topLeft = Offset(ex - rx * 0.5f, cy - ry * 0.62f),
                        size = Size(rx, ry * 1.3f),
                    )
                    drawOval(
                        color = outline,
                        topLeft = Offset(ex - rx, cy - ry),
                        size = Size(rx * 2, ry * 2),
                        style = Stroke(width = STROKE),
                    )
                }
            }
        }

        "rabbit" -> {
            // Длинные уши: посадка чуть ниже макушки, чтобы не отрывались.
            val dx = m.headRx * 0.52f
            val rx = 10f * m.earScale
            val ry = 34f * m.earScale
            val cy = crown + 2f - ry * 0.55f
            listOf(-1f to -12f, 1f to 12f).forEach { (side, angle) ->
                val ex = cx + dx * side
                rotate(degrees = angle, pivot = Offset(ex, cy + ry)) {
                    drawOval(base, Offset(ex - rx, cy - ry), Size(rx * 2, ry * 2))
                    drawOval(
                        color = lerp(base, Color.White, 0.42f),
                        topLeft = Offset(ex - rx * 0.55f, cy - ry * 0.72f),
                        size = Size(rx * 1.1f, ry * 1.5f),
                    )
                    drawOval(
                        color = outline,
                        topLeft = Offset(ex - rx, cy - ry),
                        size = Size(rx * 2, ry * 2),
                        style = Stroke(width = STROKE),
                    )
                }
            }
        }

        else -> {
            // Котёнок: треугольные уши над макушкой.
            val dx = m.headRx * 0.70f
            val h = 22f * m.earScale
            val w = 17f * m.earScale
            listOf(-1f, 1f).forEach { side ->
                val ex = cx + dx * side
                val outer = Offset(ex + w * side, crown + h * 0.5f)
                val tip = Offset(ex + w * 0.35f * side, crown - h)
                val inner = Offset(ex - w * 0.55f * side, crown + h * 0.35f)
                drawPath(trianglePath(outer, tip, inner), base)
                drawPath(
                    trianglePath(
                        Offset(outer.x - w * 0.35f * side, outer.y - 2f),
                        Offset(tip.x, tip.y + h * 0.42f),
                        Offset(inner.x + w * 0.2f * side, inner.y),
                    ),
                    shade,
                )
                drawPath(trianglePath(outer, tip, inner), outline, Stroke(width = STROKE))
            }
        }
    }
}

private fun DrawScope.drawBody(m: StageMetrics, base: Color, light: Color) {
    drawOval(
        color = base,
        topLeft = Offset(CANVAS / 2f - m.bodyRx, m.bodyCy - m.bodyRy),
        size = Size(m.bodyRx * 2, m.bodyRy * 2),
    )
    // Светлый живот: отделяет корпус от головы, когда они одного цвета.
    drawOval(
        color = light,
        topLeft = Offset(CANVAS / 2f - m.bodyRx * 0.52f, m.bodyCy - m.bodyRy * 0.18f),
        size = Size(m.bodyRx * 1.04f, m.bodyRy * 1.05f),
    )
}

private fun DrawScope.drawHead(m: StageMetrics, speciesId: String, base: Color, light: Color) {
    drawOval(
        color = base,
        topLeft = Offset(CANVAS / 2f - m.headRx, m.headCy - m.headRy),
        size = Size(m.headRx * 2, m.headRy * 2),
    )
    if (speciesId == "dog") {
        // Морда: у щенка она выражена, рот ложится на неё.
        drawOval(
            color = light,
            topLeft = Offset(CANVAS / 2f - m.headRx * 0.46f, m.headCy + m.headRy * 0.14f),
            size = Size(m.headRx * 0.92f, m.headRy * 0.62f),
        )
    }
}

/**
 * Контур силуэта. Обводятся корпус и голова: внутренняя линия на их стыке
 * читается как шея и фигуру не разрывает.
 */
private fun DrawScope.drawSilhouetteStroke(
    m: StageMetrics,
    speciesId: String,
    color: Color,
) {
    val outline = Stroke(width = STROKE)

    drawOval(
        color = color,
        topLeft = Offset(CANVAS / 2f - m.bodyRx, m.bodyCy - m.bodyRy),
        size = Size(m.bodyRx * 2, m.bodyRy * 2),
        style = outline,
    )
    drawOval(
        color = color,
        topLeft = Offset(CANVAS / 2f - m.headRx, m.headCy - m.headRy),
        size = Size(m.headRx * 2, m.headRy * 2),
        style = outline,
    )
    if (speciesId == "dog") {
        drawOval(
            color = color,
            topLeft = Offset(CANVAS / 2f - m.headRx * 0.46f, m.headCy + m.headRy * 0.14f),
            size = Size(m.headRx * 0.92f, m.headRy * 0.62f),
            style = outline,
        )
    }
}

// --- Слой 4. Выражение ---------------------------------------------------

private fun DrawScope.drawFace(m: StageMetrics, care: StatLevel, joy: StatLevel) {
    val cx = CANVAS / 2f
    val eyeY = m.headCy - m.headRy * 0.10f
    val eyeDx = m.headRx * 0.29f
    val mouthY = m.headCy + m.headRy * 0.42f
    val stroke = Stroke(width = 4f, cap = StrokeCap.Round)

    // Нос — общая деталь всех выражений.
    drawPath(
        trianglePath(
            Offset(cx - 3f, mouthY - 10f),
            Offset(cx + 3f, mouthY - 10f),
            Offset(cx, mouthY - 5f),
        ),
        Ink.copy(alpha = 0.75f),
    )

    when {
        // Низкая забота: прищур. Питомец голоден — это видно по глазам.
        care == StatLevel.LOW -> {
            listOf(-1f, 1f).forEach { side ->
                drawLine(
                    color = Ink,
                    start = Offset(cx + eyeDx * side - 6f, eyeY),
                    end = Offset(cx + eyeDx * side + 6f, eyeY),
                    strokeWidth = 4f,
                    cap = StrokeCap.Round,
                )
            }
            drawPath(arcPath(cx, mouthY + 4f, 10f, -9f), Ink, style = stroke)
        }

        // Низкая радость: опущенные веки и опущенный уголок рта.
        joy == StatLevel.LOW -> {
            listOf(-1f, 1f).forEach { side ->
                drawPath(arcPath(cx + eyeDx * side, eyeY - 2f, 6f, 6f), Ink, style = stroke)
            }
            drawPath(arcPath(cx, mouthY + 4f, 10f, -9f), Ink, style = stroke)
        }

        joy == StatLevel.HIGH -> {
            listOf(-1f, 1f).forEach { side ->
                drawPath(arcPath(cx + eyeDx * side, eyeY, 6f, -8f), Ink, style = stroke)
            }
            drawPath(arcPath(cx, mouthY, 10f, 10f), Ink, style = stroke)
        }

        else -> {
            listOf(-1f, 1f).forEach { side ->
                drawCircle(Ink, radius = 4.5f, center = Offset(cx + eyeDx * side, eyeY))
            }
            drawLine(
                color = Ink,
                start = Offset(cx - 9f, mouthY),
                end = Offset(cx + 9f, mouthY),
                strokeWidth = 4f,
                cap = StrokeCap.Round,
            )
        }
    }
}

// --- Слой 3. Украшение ---------------------------------------------------

private fun DrawScope.drawAccessory(m: StageMetrics, accessoryId: String) {
    // Общая точка привязки у шеи: одна для всех видов и стадий, иначе
    // украшение пришлось бы рисовать под каждое сочетание.
    val neckY = m.bodyCy - m.bodyRy + 4f
    val cx = CANVAS / 2f

    when (accessoryId) {
        "bow" -> {
            val bx = cx + m.headRx * 0.86f
            val by = m.headCy - m.headRy * 0.62f
            drawPath(
                trianglePath(Offset(bx, by), Offset(bx - 16f, by - 10f), Offset(bx - 16f, by + 12f)),
                BowColor,
            )
            drawPath(
                trianglePath(Offset(bx, by), Offset(bx + 16f, by - 10f), Offset(bx + 16f, by + 12f)),
                BowColor,
            )
            drawCircle(BowKnotColor, radius = 7f, center = Offset(bx, by + 1f))
        }

        "scarf" -> {
            val halfWidth = m.bodyRx * 0.72f
            val path = Path().apply {
                moveTo(cx - halfWidth, neckY)
                quadraticTo(cx, neckY + 20f, cx + halfWidth, neckY)
                lineTo(cx + halfWidth + 4f, neckY + 16f)
                quadraticTo(cx, neckY + 38f, cx - halfWidth - 4f, neckY + 16f)
                close()
            }
            drawPath(path, ScarfColor)
            val tail = Path().apply {
                moveTo(cx + halfWidth * 0.82f, neckY + 14f)
                lineTo(cx + halfWidth * 0.82f + 15f, neckY + 18f)
                lineTo(cx + halfWidth * 0.82f + 9f, neckY + 44f)
                lineTo(cx + halfWidth * 0.82f - 6f, neckY + 38f)
                close()
            }
            drawPath(tail, ScarfShadeColor)
        }
    }
}

private val BowColor = Color(0xFFD94F6E)
private val BowKnotColor = Color(0xFFB23A56)
private val ScarfColor = Color(0xFF3D7C47)
private val ScarfShadeColor = Color(0xFF2F6237)

// --- Вспомогательное -----------------------------------------------------

private fun trianglePath(a: Offset, b: Offset, c: Offset): Path = Path().apply {
    moveTo(a.x, a.y)
    lineTo(b.x, b.y)
    lineTo(c.x, c.y)
    close()
}

/**
 * Дуга шириной [halfWidth] в обе стороны от точки. Положительный [rise]
 * поднимает середину вверх (улыбка), отрицательный опускает.
 */
private fun arcPath(cx: Float, cy: Float, halfWidth: Float, rise: Float): Path = Path().apply {
    moveTo(cx - halfWidth, cy)
    quadraticTo(cx, cy + rise * 2f, cx + halfWidth, cy)
}

private fun DrawScope.drawPath(path: Path, color: Color, style: Stroke? = null) {
    if (style == null) drawPath(path, color, style = Fill) else drawPath(path, color, style = style)
}
