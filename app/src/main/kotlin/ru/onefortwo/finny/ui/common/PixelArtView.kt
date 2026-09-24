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
import ru.onefortwo.finny.content.PixelPoint
import ru.onefortwo.finny.content.PixelSprite
import ru.onefortwo.finny.economy.GrowthStage
import ru.onefortwo.finny.economy.StatLevel
import kotlin.math.atan2
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
 * @param reaction идущая реакция (`eat`, `play`, `save`, `reward`, `grow`)
 * и её такт [reactionTick]: рот, глаза, вспышка и частицы по её описанию.
 * @param stageBefore стадия до роста: реакция `grow` рисует её до такта
 * смены.
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
    reaction: String? = null,
    reactionTick: Int = 0,
    stageBefore: GrowthStage? = null,
): IntArray {
    val spec = reaction?.let { art.animation.reactions[it] }
    val swap = spec?.swapStage
    fun stageAt(tick: Int) = if (swap != null && stageBefore != null && tick < swap) stageBefore else stage
    val shownStage = stageAt(reactionTick)
    val flash = spec?.flashAt?.let { reactionTick in it until it + spec.flashTicks } ?: false

    val base = argbOf(colorHex)
    val ramp = art.ramps.values.firstOrNull { it[0] == base } ?: art.ramps.values.first()
    val shadow = if (inScene) art.shadowInScene else art.shadowOnCard
    val ink = art.indexOf('K')
    val light = art.colors[art.indexOf('c')]
    val color: (Int) -> Int = { index ->
        when {
            index == art.shadowIndex -> shadow
            // Вспышка роста: всё, кроме контура, светлым цветом карточки.
            flash && index != ink -> light
            index == art.furIndices[0] -> ramp[0]
            index == art.furIndices[1] -> ramp[1]
            index == art.furIndices[2] -> ramp[2]
            else -> art.colors[index]
        }
    }

    val key = "${speciesId}_${stageId(shownStage)}"
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
    val eyes = spec?.eyes ?: if (blink) "blink" else eyesFor(care, joy)
    val mouth = spec?.mouth
        ?.takeIf { reactionTick in it.from until it.to }
        ?.let { it.frames[((reactionTick - it.from) / it.ticks) % it.frames.size] }
        ?: mouthFor(joy)
    place("${speciesId}_eyes_$eyes", "eyes")
    place("${speciesId}_mouth_$mouth", "mouth")
    when (accessoryId) {
        "bow" -> place("bow_$speciesId", "bow")
        "scarf" -> place("scarf_${speciesId}_${stageId(shownStage)}", "scarf")
    }

    if (spec != null) {
        val plain: (Int) -> Int = { art.colors[it] }
        spec.emits.forEach { emit ->
            val age = reactionTick - emit.at
            if (age !in 0 until emit.life) return@forEach
            val frames = art.animation.fx[emit.fx] ?: return@forEach
            val spawnKey = "${speciesId}_${stageId(stageAt(emit.at))}"
            val spawnDy = spec.jumps.firstNotNullOfOrNull { (at, offsets) -> offsets.getOrNull(emit.at - at) } ?: 0
            val origin = when (emit.from) {
                "outline" -> art.sprite("${spawnKey}_0")?.let { outlinePoint(art, it, emit.point, emit.of) }
                "base" -> PixelPoint(art.petSize / 2, art.baseline)
                else -> art.anchors[spawnKey]?.getOrNull(0)?.get(emit.from)?.let { it.copy(y = it.y + spawnDy) }
            } ?: return@forEach
            val x = floor(origin.x + emit.dx + emit.vx * age).toInt()
            val y = floor(origin.y + emit.dy + emit.vy * age).toInt()
            val sprite = art.sprite(frames[min(frames.size - 1, age * frames.size / emit.life)]) ?: return@forEach
            canvas.draw(sprite, x - sprite.pivotX, y - sprite.pivotY, plain)
        }
    }
    return canvas.pixels
}

/**
 * Точка контура фигуры для частиц роста: клетки на границе с прозрачным,
 * упорядоченные по углу от центра фигуры; берётся [point] из [of] равных
 * долей.
 */
private fun outlinePoint(art: PixelArt, sprite: PixelSprite, point: Int, of: Int): PixelPoint? {
    fun solid(x: Int, y: Int): Boolean {
        if (x !in 0 until sprite.width || y !in 0 until sprite.height) return false
        val index = sprite.pixels[y * sprite.width + x]
        return index >= 0 && index != art.shadowIndex
    }
    val cells = ArrayList<Pair<Int, Int>>()
    var sx = 0.0
    var sy = 0.0
    var total = 0
    for (y in 0 until sprite.height) {
        for (x in 0 until sprite.width) {
            if (!solid(x, y)) continue
            sx += x
            sy += y
            total++
            if (!solid(x - 1, y) || !solid(x + 1, y) || !solid(x, y - 1) || !solid(x, y + 1)) cells += x to y
        }
    }
    if (cells.isEmpty()) return null
    val cx = sx / total
    val cy = sy / total
    cells.sortBy { (x, y) -> atan2(y - cy, x - cx) }
    val (x, y) = cells[(point * cells.size / of.coerceAtLeast(1)).coerceIn(0, cells.size - 1)]
    return PixelPoint(x, y)
}

/**
 * Сцена 180 × 100: небо, земля, палатка (после покупки домика), предмет
 * полученной цели и питомец в правой части.
 *
 * @param skyFrame кадр неба: облака сдвинуты на клетку во втором.
 * @param stickers наклейки на палатке (после покупки «Наклеек»).
 */
fun composeScene(
    art: PixelArt,
    house: Boolean,
    goalId: String?,
    pet: IntArray,
    skyFrame: Int = 0,
    stickers: Boolean = false,
): IntArray {
    val canvas = PixelCanvas(art.sceneWidth, art.sceneHeight)
    val plain: (Int) -> Int = { art.colors[it] }

    val sky = art.animation.skyFrames.getOrNull(skyFrame) ?: "sky"
    (art.sprite(sky) ?: art.sprite("sky"))?.let { canvas.draw(it, 0, 0, plain) }
    art.sprite("ground")?.let { canvas.draw(it, 0, 0, plain) }
    if (house) {
        val at = art.sceneAnchors["tent"]
        art.sprite("tent")?.let { if (at != null) canvas.draw(it, at.x, at.y, plain) }
        if (stickers) art.sprite("tent_stickers")?.let { if (at != null) canvas.draw(it, at.x, at.y, plain) }
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
