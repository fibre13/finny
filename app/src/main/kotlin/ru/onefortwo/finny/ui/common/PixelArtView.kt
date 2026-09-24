package ru.onefortwo.finny.ui.common

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import ru.onefortwo.finny.content.ContentRepository
import ru.onefortwo.finny.content.PixelArt
import ru.onefortwo.finny.content.PixelSprite
import ru.onefortwo.finny.economy.GrowthStage
import ru.onefortwo.finny.economy.StatLevel
import kotlin.math.floor
import kotlin.math.min

/**
 * Пиксельная графика: сборка изображения питомца и сцены из спрайтов
 * `pixel_art.json` и вывод с целым увеличением без сглаживания.
 *
 * Изображение собирается в массив цветов логического размера (48 × 48
 * для питомца, 180 × 100 для сцены) и переносится на экран увеличением
 * в целое число раз: так клетки остаются одинаковыми квадратами на любом
 * экране. Остаток места — поля по центру.
 */

/** Глаза по показателям: усталые при низкой заботе, радостные при высокой радости. */
fun eyesFor(care: StatLevel, joy: StatLevel): String = when {
    care == StatLevel.LOW -> "tired"
    joy == StatLevel.HIGH -> "happy"
    else -> "normal"
}

/** Рот — по радости. */
fun mouthFor(joy: StatLevel): String = when (joy) {
    StatLevel.HIGH -> "smile"
    StatLevel.MEDIUM -> "neutral"
    StatLevel.LOW -> "sad"
}

private fun stageId(stage: GrowthStage): String = stage.name.lowercase()

/** Холст логического размера; `0` — прозрачная клетка. */
private class PixelCanvas(val width: Int, val height: Int) {
    val pixels = IntArray(width * height)

    /**
     * Кладёт спрайт левым верхним углом в клетку [left], [top]. Клетки
     * с индексом [fixedIndex] остаются на месте, остальные сдвигаются
     * на [dy]: так при прыжке тень под фигурой остаётся на земле.
     */
    fun draw(
        sprite: PixelSprite,
        left: Int,
        top: Int,
        color: (Int) -> Int,
        dy: Int = 0,
        fixedIndex: Int = Int.MIN_VALUE,
    ) {
        for (y in 0 until sprite.height) {
            for (x in 0 until sprite.width) {
                val index = sprite.pixels[y * sprite.width + x]
                if (index < 0) continue
                val tx = left + x
                val ty = top + y + if (index == fixedIndex) 0 else dy
                if (tx in 0 until width && ty in 0 until height) pixels[ty * width + tx] = color(index)
            }
        }
    }

    fun drawAll(other: IntArray, otherWidth: Int, left: Int, top: Int) {
        val otherHeight = other.size / otherWidth
        for (y in 0 until otherHeight) {
            val ty = top + y
            if (ty !in 0 until height) continue
            for (x in 0 until otherWidth) {
                val tx = left + x
                if (tx !in 0 until width) continue
                val c = other[y * otherWidth + x]
                if (c != 0) pixels[ty * width + tx] = c
            }
        }
    }
}

/**
 * Питомец 48 × 48: фигура, глаза и рот по показателям, украшение.
 * Шерсть окрашивается рампой окраса; рампа находится по основному цвету,
 * который совпадает со значением окраса в `pet_parts.json`.
 *
 * @param inScene тень под фигурой цвета луга, а не карточки.
 * @param frame кадр дыхания: 0 или 1 (голова опущена на клетку).
 * @param blink глаза закрыты — такт моргания.
 * @param dy смещение фигуры по вертикали при прыжке, в клетках (вверх —
 * отрицательное); тень остаётся на месте.
 */
fun composePet(
    art: PixelArt,
    speciesId: String,
    stage: GrowthStage,
    colorHex: String,
    accessoryId: String,
    care: StatLevel,
    joy: StatLevel,
    inScene: Boolean = false,
    frame: Int = 0,
    blink: Boolean = false,
    dy: Int = 0,
): IntArray {
    val base = argbOf(colorHex)
    val ramp = art.ramps.values.firstOrNull { it[0] == base } ?: art.ramps.values.first()
    val shadow = if (inScene) art.shadowInScene else art.shadowOnCard
    val color: (Int) -> Int = { index ->
        when (index) {
            art.furIndices[0] -> ramp[0]
            art.furIndices[1] -> ramp[1]
            art.furIndices[2] -> ramp[2]
            art.shadowIndex -> shadow
            else -> art.colors[index]
        }
    }

    val key = "${speciesId}_${stageId(stage)}"
    val canvas = PixelCanvas(art.petSize, art.petSize)
    val anchors = art.anchors[key]?.getOrNull(frame).orEmpty()

    fun place(spriteId: String, anchor: String) {
        val sprite = art.sprite(spriteId) ?: return
        val point = anchors[anchor] ?: return
        canvas.draw(sprite, point.x - sprite.pivotX, point.y - sprite.pivotY + dy, color)
    }

    // Фигура занимает весь холст 48 × 48 и кладётся без сдвига.
    (art.sprite("${key}_$frame") ?: art.sprite("${key}_0"))?.let {
        canvas.draw(it, 0, 0, color, dy = dy, fixedIndex = art.shadowIndex)
    }
    place("${speciesId}_eyes_${if (blink) "blink" else eyesFor(care, joy)}", "eyes")
    place("${speciesId}_mouth_${mouthFor(joy)}", "mouth")
    when (accessoryId) {
        "bow" -> place("bow_$speciesId", "bow")
        "scarf" -> place("scarf_${speciesId}_${stageId(stage)}", "scarf")
    }
    return canvas.pixels
}

/**
 * Сцена 180 × 100: небо, земля, палатка (после покупки домика), предмет
 * полученной цели и питомец в правой части.
 *
 * @param skyFrame кадр неба: облака сдвинуты на клетку во втором.
 */
fun composeScene(art: PixelArt, house: Boolean, goalId: String?, pet: IntArray, skyFrame: Int = 0): IntArray {
    val canvas = PixelCanvas(art.sceneWidth, art.sceneHeight)
    val plain: (Int) -> Int = { art.colors[it] }

    val sky = art.animation.skyFrames.getOrNull(skyFrame) ?: "sky"
    (art.sprite(sky) ?: art.sprite("sky"))?.let { canvas.draw(it, 0, 0, plain) }
    art.sprite("ground")?.let { canvas.draw(it, 0, 0, plain) }
    if (house) {
        val at = art.sceneAnchors["tent"]
        art.sprite("tent")?.let { if (at != null) canvas.draw(it, at.x, at.y, plain) }
    }
    if (goalId != null) {
        val at = art.sceneAnchors["goal"]
        art.sprite(goalId)?.let { if (at != null) canvas.draw(it, at.x - it.pivotX, at.y - it.pivotY, plain) }
    }
    art.sceneAnchors["pet"]?.let { at ->
        // Точка привязки фигуры — середина базовой линии холста 48 × 48.
        canvas.drawAll(pet, art.petSize, at.x - art.petSize / 2, at.y - art.baseline)
    }
    return canvas.pixels
}

/** Иллюстрация цели 32 × 32 без замены цветов. */
fun composeGoal(art: PixelArt, goalId: String): IntArray? {
    val sprite = art.sprite(goalId) ?: return null
    val canvas = PixelCanvas(sprite.width, sprite.height)
    canvas.draw(sprite, 0, 0, color = { art.colors[it] })
    return canvas.pixels
}

private fun argbOf(hex: String): Int = (0xFF000000 or hex.removePrefix("#").toLong(16)).toInt()

/** Графика читается из ресурсов один раз на процесс. */
private object PixelArtStore {
    private var art: PixelArt? = null

    fun get(context: Context): PixelArt =
        art ?: ContentRepository(context.applicationContext).pixelArt().also { art = it }
}

@Composable
fun rememberPixelArt(): PixelArt {
    val context = LocalContext.current
    return remember { PixelArtStore.get(context) }
}

/** Переводит собранный массив в изображение для вывода. */
fun pixelImage(pixels: IntArray, width: Int): ImageBitmap =
    Bitmap.createBitmap(pixels, width, pixels.size / width, Bitmap.Config.ARGB_8888).asImageBitmap()

/**
 * Выводит пиксельное изображение с целым увеличением по центру области.
 *
 * @param cropTop сколько верхних строк не показывать (сцена в альбомной
 * ориентации).
 * @param background цвет полей вокруг изображения.
 */
@Composable
fun PixelImage(
    image: ImageBitmap,
    modifier: Modifier = Modifier,
    cropTop: Int = 0,
    background: Color = Color.Transparent,
) {
    Canvas(modifier = modifier) {
        val srcHeight = image.height - cropTop
        val scale = floor(min(size.width / image.width, size.height / srcHeight)).toInt().coerceAtLeast(1)
        val width = image.width * scale
        val height = srcHeight * scale
        if (background != Color.Transparent) drawRect(background)
        drawImage(
            image = image,
            srcOffset = IntOffset(0, cropTop),
            srcSize = IntSize(image.width, srcHeight),
            dstOffset = IntOffset(((size.width - width) / 2).toInt(), ((size.height - height) / 2).toInt()),
            dstSize = IntSize(width, height),
            filterQuality = FilterQuality.None,
        )
    }
}
