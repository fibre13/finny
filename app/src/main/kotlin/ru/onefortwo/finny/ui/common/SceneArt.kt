package ru.onefortwo.finny.ui.common

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser

/**
 * Фон главного экрана: небо, холмы с деревом и дом питомца.
 *
 * Разметка взята из канвы экранов (секции 3 и 4), где сцена собрана
 * слоями: `scene-bg` — земля и дерево, `sky-day` и `sky-night` — небо,
 * `pet-house` — дом. Небо подменяется при смене темы, дом появляется
 * после покупки «Домика-палатки».
 *
 * Рисуется на Canvas, а не набором Vector Drawable, потому что цвета
 * в исходнике заданы переменными темы. Статический ресурс вынудил бы
 * запечь их и завести отдельный файл на каждую пару, а так тёмная пара
 * получается подстановкой значений.
 *
 * Данные путей перенесены из исходника дословно и разбираются
 * `PathParser`: переписывать их в вызовы рисования значило бы вносить
 * расхождение с макетом на каждой правке.
 */

/** Система координат сцены, как в канве. */
private const val SCENE_WIDTH = 360f
private const val SCENE_HEIGHT = 200f

/**
 * Срез верхней полосы неба в альбомной ориентации.
 *
 * В альбомной высота экрана мала, и сцена в полный рост занимает её
 * заметную часть. Полоса сверху пустая: ни одна фигура в неё не заходит,
 * поэтому окно берётся ниже, а не уменьшается вся сцена — фигуры остаются
 * прежнего размера и ничего не режется. Значение из канвы, экран 4c:
 * `viewBox="0 14 360 186"`.
 */
const val SCENE_TOP_CROP_LANDSCAPE = 14f

/** Соотношение сторон сцены при заданном срезе верха. */
fun sceneAspect(topCrop: Float = 0f): Float = SCENE_WIDTH / (SCENE_HEIGHT - topCrop)

private fun path(data: String): Path = PathParser().parsePathString(data).toPath()

/**
 * Рисует сцену целиком.
 *
 * @param night ночное небо вместо дневного.
 * @param house показывать дом питомца.
 */
fun DrawScope.drawScene(
    primary: Color,
    secondary: Color,
    surface: Color,
    onBackground: Color,
    ink: Color,
    night: Boolean,
    house: Boolean,
    topCrop: Float = 0f,
    pet: (DrawScope.() -> Unit)? = null,
) {
    val unit = size.width / SCENE_WIDTH

    scale(scale = unit, pivot = Offset.Zero) {
        translate(top = -topCrop) {
            if (night) {
                drawNightSky(secondary, onBackground, ink)
            } else {
                drawDaySky(secondary, surface, ink)
            }
            drawGround(primary, surface, ink)
            if (house) {
                // Дом нарисован в своей системе координат 120 × 100
                // и ставится в сцену сдвигом, как в канве.
                translate(left = 20f, top = 72f) {
                    drawHouse(primary, secondary, surface, ink)
                }
            }
            if (pet != null) {
                // Питомец стоит внутри сцены, а не поверх неё: дом занимает
                // левую часть, дерево середину, питомец правую. Положение
                // и уменьшение взяты из канвы.
                translate(left = PET_LEFT, top = PET_TOP) {
                    scale(scale = PET_SCALE, pivot = Offset.Zero) { pet() }
                }
            }
        }
    }
}

/**
 * Светило стоит по середине сетки, а не в правом верхнем углу.
 *
 * Причина: фигура питомца считается по пропорциям стадии, и постоянной
 * верхней границы у ушей нет. У котёнка-малыша верх уха приходится на 15
 * по сетке 200, у взрослого — почти на её край, а уши кролика за сетку
 * выходят. В правом верхнем углу светило пересекалось бы с ушами на
 * большинстве из девяти сочетаний вида и стадии.
 *
 * Значения перенесены из канвы: солнце на 196, месяц от 200.
 */
private const val SUN_X = 196f

/** Положение и размер фигуры питомца внутри сцены, как в канве. */
private const val PET_LEFT = 196f
private const val PET_TOP = 44f
private const val PET_SCALE = 0.7f

// --- Небо ----------------------------------------------------------------

private fun DrawScope.drawDaySky(secondary: Color, surface: Color, ink: Color) {
    val outline = Stroke(width = 2.5f)

    drawCircle(secondary, radius = 22f, center = Offset(SUN_X, 40f))
    drawCircle(ink, radius = 22f, center = Offset(SUN_X, 40f), style = outline)

    cloud(surface, ink, outline)
}

private fun DrawScope.drawNightSky(secondary: Color, onBackground: Color, ink: Color) {
    val outline = Stroke(width = 2.5f, join = StrokeJoin.Round)
    val starOutline = Stroke(width = 2f, join = StrokeJoin.Round)

    val moon = path("M200 18 A22 22 0 1 0 200 62 A34 34 0 0 1 200 18 Z")
    drawPath(moon, secondary)
    drawPath(moon, ink, style = outline)

    listOf(
        "M246 23 L248.5 29.5 L255 32 L248.5 34.5 L246 41 L243.5 34.5 L237 32 L243.5 29.5 Z",
        "M336 57 L338 62 L343 64 L338 66 L336 71 L334 66 L329 64 L334 62 Z",
    ).forEach { data ->
        val star = path(data)
        drawPath(star, secondary)
        drawPath(star, ink, style = starOutline)
    }

    // Ночью облака приглушены, а не убраны: небо остаётся тем же местом.
    cloud(onBackground.copy(alpha = 0.22f), ink, outline)
}

private fun DrawScope.cloud(fill: Color, ink: Color, outline: Stroke) {
    listOf(
        Triple(62f, 44f, 15f),
        Triple(86f, 48f, 11f),
        Triple(40f, 50f, 10f),
    ).forEach { (cx, cy, r) ->
        drawCircle(fill, radius = r, center = Offset(cx, cy))
        drawCircle(ink, radius = r, center = Offset(cx, cy), style = outline)
    }
}

// --- Земля ---------------------------------------------------------------

private fun DrawScope.drawGround(primary: Color, surface: Color, ink: Color) {
    drawPath(
        path("M0 132 Q70 88 138 126 Q206 162 262 122 Q312 86 360 124 L360 200 L0 200 Z"),
        primary.copy(alpha = 0.20f),
    )
    drawPath(
        path("M0 158 Q86 120 168 154 Q248 186 300 152 Q334 130 360 146 L360 200 L0 200 Z"),
        primary.copy(alpha = 0.34f),
    )
    drawPath(
        path("M0 166 Q86 128 168 162 Q248 194 300 160 Q334 138 360 154"),
        ink,
        style = Stroke(width = 2.5f),
    )

    // Дерево стоит правее середины: место слева оставлено дому.
    translate(left = 150f, top = 92f) {
        drawLine(
            color = ink,
            start = Offset(8f, 62f),
            end = Offset(8f, 30f),
            strokeWidth = 7f,
            cap = StrokeCap.Round,
        )
        drawCircle(primary, radius = 19f, center = Offset(8f, 18f))
        drawCircle(ink, radius = 19f, center = Offset(8f, 18f), style = Stroke(width = 2.5f))
        drawCircle(surface.copy(alpha = 0.35f), radius = 6f, center = Offset(1f, 12f))
    }
}

// --- Дом -----------------------------------------------------------------

private fun DrawScope.drawHouse(primary: Color, secondary: Color, paper: Color, ink: Color) {
    val outline = Stroke(width = 2.5f)
    val joined = Stroke(width = 2.5f, join = StrokeJoin.Round)

    drawOval(
        color = ink.copy(alpha = 0.08f),
        topLeft = Offset(14f, 81f),
        size = androidx.compose.ui.geometry.Size(92f, 14f),
    )
    drawLine(
        color = ink,
        start = Offset(60f, 18f),
        end = Offset(60f, 4f),
        strokeWidth = 3.5f,
        cap = StrokeCap.Round,
    )

    val flag = path("M60 2 L84 9 L60 16 Z")
    drawPath(flag, secondary)
    drawPath(flag, ink, style = outline)

    val body = path("M60 18 L110 88 L10 88 Z")
    drawPath(body, primary)
    drawPath(body, ink, style = joined)

    drawPath(path("M60 18 L78 88 L42 88 Z"), ink.copy(alpha = 0.10f))

    val door = path("M40 88 Q40 56 60 56 Q80 56 80 88 Z")
    drawPath(door, paper)
    drawPath(door, ink, style = outline)

    drawCircle(ink.copy(alpha = 0.18f), radius = 5f, center = Offset(60f, 72f))
}
