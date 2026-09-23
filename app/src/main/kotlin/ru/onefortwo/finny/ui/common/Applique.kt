package ru.onefortwo.finny.ui.common

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import ru.onefortwo.finny.ui.theme.AppliqueDecor

/**
 * Оформление направления 1b «Аппликация»: контур и сплошное смещение
 * вместо размытой тени.
 *
 * Граница элемента задаётся контуром, а не цветовым контрастом с фоном,
 * поэтому форма читается и при слабом различении оттенков (ТЗ 3.6).
 *
 * При нажатии тень убирается, а элемент смещается ровно на её место:
 * бумажная фигура как будто прижимается к странице. Это заменяет
 * подсветку нажатия и работает без анимации, которой в приложении нет.
 *
 * Неактивный элемент оформляется иначе: пунктирный контур приглушённым
 * цветом и без тени. Он лежит на странице, а не приподнят над ней, и по
 * одному силуэту видно, что нажать его нельзя — не полагаясь на цвет.
 *
 * @param pressed элемент нажат. Для неинтерактивных элементов остаётся
 * `false`.
 * @param enabled элемент доступен.
 */
fun Modifier.applique(
    shape: Shape,
    decor: AppliqueDecor,
    pressed: Boolean = false,
    enabled: Boolean = true,
): Modifier {
    if (!enabled) {
        return this.drawBehind {
            val outline = shape.createOutline(size, layoutDirection, this)
            val dash = decor.strokeWidth.toPx() * 2f
            drawOutline(
                outline = outline,
                color = decor.disabledOutline,
                style = Stroke(
                    width = decor.strokeWidth.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)),
                ),
            )
        }
    }

    val shift = if (pressed) decor.pressShift else 0.dp

    return this
        .offset(x = shift, y = shift)
        .drawBehind {
            if (pressed) return@drawBehind

            val offset = decor.shadowOffset.toPx()
            translate(left = offset, top = offset) {
                drawOutline(
                    outline = shape.createOutline(size, layoutDirection, this),
                    color = decor.ink,
                )
            }
        }
        .border(width = decor.strokeWidth, color = decor.ink, shape = shape)
}
