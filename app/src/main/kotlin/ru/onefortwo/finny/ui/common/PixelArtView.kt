package ru.onefortwo.finny.ui.common

import ru.onefortwo.finny.content.Accessories
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import ru.onefortwo.finny.content.ContentRepository
import ru.onefortwo.finny.content.PixelArt
import ru.onefortwo.finny.content.PixelPoint
import ru.onefortwo.finny.content.PixelSprite
import ru.onefortwo.finny.economy.GrowthStage
import ru.onefortwo.finny.economy.StatLevel
import kotlin.math.atan2
import kotlin.math.ceil
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
    // Несколько украшений сразу; шапочка — поверх, на макушке.
    val worn = Accessories.list(accessoryId)
    if ("scarf" in worn) place("scarf_${speciesId}_${stageId(shownStage)}", "scarf")
    if ("bow" in worn) place("bow_$speciesId", "bow")
    // Подросток и взрослый ходят в школу — на них школьная кепка, если не надета шапочка.
    if ("hat" in worn) place("hat", "hat") else if (shownStage != GrowthStage.BABY) place("cap", "hat")

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
 * Самочувствие питомца для панели в небе сцены: значения 0…100 и признак
 * низкого уровня (деления тогда коралловые).
 */
data class SceneStats(val care: Int, val careLow: Boolean, val joy: Int, val joyLow: Boolean)

/** Ширина и высота панели самочувствия в клетках. */
const val STATS_PANEL_WIDTH = 31
const val STATS_PANEL_HEIGHT = 20

/**
 * Панель самочувствия отдельным изображением: на экране она выводится
 * вдвое крупнее сцены, иначе деления при увеличении сцены ×3 — около
 * 4 × 6 dp и ребёнку не читаются.
 */
fun composeStats(art: PixelArt, stats: SceneStats): IntArray {
    val canvas = PixelCanvas(STATS_PANEL_WIDTH, STATS_PANEL_HEIGHT)
    canvas.drawStats(art, 0, 0, stats)
    return canvas.pixels
}

/** Деления полоски: 5 штук, заполнено по 20 единиц на деление. */
private const val STAT_SEGMENTS = 5

/**
 * Панель «Забота» и «Радость» 31 × 20 клеток: миска и сердечко, рядом —
 * по 5 делений. Уровень читается по числу закрашенных делений, а не
 * только по цвету; словами его произносит программа чтения с экрана.
 */
private fun PixelCanvas.drawStats(art: PixelArt, left: Int, top: Int, stats: SceneStats) {
    val c = { ch: Char -> art.colors[art.indexOf(ch)] }
    val panelWidth = STATS_PANEL_WIDTH
    val panelHeight = STATS_PANEL_HEIGHT
    for (y in 0 until panelHeight) {
        for (x in 0 until panelWidth) {
            val corner = (x == 0 || x == panelWidth - 1) && (y == 0 || y == panelHeight - 1)
            if (corner) continue
            val border = x == 0 || y == 0 || x == panelWidth - 1 || y == panelHeight - 1
            val tx = left + x
            val ty = top + y
            if (tx in 0 until width && ty in 0 until height) {
                pixels[ty * width + tx] = if (border) c('K') else c('c')
            }
        }
    }
    fun row(iconId: String, value: Int, low: Boolean, fill: Char, y: Int) {
        art.sprite(iconId)?.let { draw(it, left + 2, y, { index -> art.colors[index] }) }
        val filled = ((value + 19) / 20).coerceIn(0, STAT_SEGMENTS)
        repeat(STAT_SEGMENTS) { i ->
            val color = when {
                i >= filled -> c('z')
                low -> c('R')
                else -> c(fill)
            }
            for (dy in 0 until 4) for (dx in 0 until 3) {
                val tx = left + 10 + i * 4 + dx
                val ty = y + 1 + dy
                if (tx in 0 until width && ty in 0 until height) pixels[ty * width + tx] = color
            }
        }
    }
    row("icon_care", stats.care, stats.careLow, 'E', top + 2)
    row("heart_0", stats.joy, stats.joyLow, 'P', top + 11)
}

/** Левый край основной сцены 180 × 100 в изображении с боковыми полосами. */
fun sceneCoreLeft(art: PixelArt): Int = art.sprite("edge_left")?.width ?: 0

/** Ширина сцены вместе с боковыми полосами. */
fun sceneFullWidth(art: PixelArt): Int = sceneCoreLeft(art) + art.sceneWidth + (art.sprite("edge_right")?.width ?: 0)

/**
 * Сцена 180 × 100 с боковыми полосами по 40 клеток: небо, земля, палатка
 * (после покупки домика), предмет полученной цели и питомец в правой
 * части. Полосы продолжают холмы, луг и крону дерева, когда окно шире
 * основной сцены; координаты якорей — в клетках основной сцены.
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
    val ox = sceneCoreLeft(art)
    val canvas = PixelCanvas(sceneFullWidth(art), art.sceneHeight)
    val plain: (Int) -> Int = { art.colors[it] }

    art.sprite("edge_left")?.let { canvas.draw(it, 0, 0, plain) }
    art.sprite("edge_right")?.let { canvas.draw(it, ox + art.sceneWidth, 0, plain) }
    val sky = art.animation.skyFrames.getOrNull(skyFrame) ?: "sky"
    (art.sprite(sky) ?: art.sprite("sky"))?.let { canvas.draw(it, ox, 0, plain) }
    art.sprite("ground")?.let { canvas.draw(it, ox, 0, plain) }
    if (house) {
        val at = art.sceneAnchors["tent"]
        art.sprite("tent")?.let { if (at != null) canvas.draw(it, ox + at.x, at.y, plain) }
        if (stickers) art.sprite("tent_stickers")?.let { if (at != null) canvas.draw(it, ox + at.x, at.y, plain) }
    }
    if (goalId != null) {
        val at = art.sceneAnchors["goal"]
        art.sprite(goalId)?.let { if (at != null) canvas.draw(it, ox + at.x - it.pivotX, at.y - it.pivotY, plain) }
    }
    art.sceneAnchors["pet"]?.let { at ->
        // Точка привязки фигуры — середина базовой линии холста 48 × 48.
        canvas.drawAll(pet, art.petSize, ox + at.x - art.petSize / 2, at.y - art.baseline)
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

/**
 * Пиксельный значок интерфейса: спрайт [id] с клеткой [cell] без замены
 * цветов. Не озвучивается: смысл несёт подпись рядом или описание кнопки
 * (ТЗ 3.6).
 */
@Composable
fun PixelIcon(id: String, modifier: Modifier = Modifier, cell: Dp = 2.dp) {
    val art = rememberPixelArt()
    val sprite = art.sprite(id) ?: return
    val image = remember(art, id) { composeGoal(art, id)?.let { pixelImage(it, sprite.width) } } ?: return
    PixelImage(
        image = image,
        modifier = modifier.size(cell * sprite.width, cell * sprite.height).clearAndSetSemantics { },
    )
}

/** Переводит собранный массив в изображение для вывода. */
fun pixelImage(pixels: IntArray, width: Int): ImageBitmap =
    Bitmap.createBitmap(pixels, width, pixels.size / width, Bitmap.Config.ARGB_8888).asImageBitmap()

/**
 * Выводит пиксельное изображение с целым увеличением по центру области.
 *
 * @param cropTop сколько верхних строк можно не показывать (сцена в
 * альбомной ориентации): срез применяется, только если даёт увеличение
 * крупнее.
 * @param background цвет полей вокруг изображения.
 * @param extendEdges поля заполняются продолжением краёв изображения:
 * над сценой — небо, под ней — трава, по бокам — крайние столбцы
 * боковых полос (ровные небо, холм и луг).
 * @param coreLeft левый край и [coreWidth] ширина основной части
 * изображения: увеличение подбирается по ней, а по бокам показывается
 * столько боковых полос, сколько помещается по ширине.
 * @param overlay рисование поверх изображения: увеличение и положение
 * левого верхнего угла основной части на экране.
 */
@Composable
fun PixelImage(
    image: ImageBitmap,
    modifier: Modifier = Modifier,
    cropTop: Int = 0,
    background: Color = Color.Transparent,
    extendEdges: Boolean = false,
    coreLeft: Int = 0,
    coreWidth: Int = image.width,
    overlay: DrawScope.(scale: Int, left: Int, top: Int) -> Unit = { _, _, _ -> },
) {
    Canvas(modifier = modifier) {
        fun scaleFor(crop: Int) =
            floor(min(size.width / coreWidth, size.height / (image.height - crop))).toInt().coerceAtLeast(1)
        // Срез неба нужен, только если он даёт увеличение крупнее. Иначе
        // над сценой остаётся поле, и продолжение срезанной строки тянуло
        // бы солнце и облака вертикальными полосами.
        val crop = if (cropTop > 0 && scaleFor(cropTop) > scaleFor(0)) cropTop else 0
        val srcHeight = image.height - crop
        val scale = scaleFor(crop)
        val visible = min(image.width, ceil(size.width / scale).toInt())
        val srcLeft = (coreLeft + coreWidth / 2 - visible / 2).coerceIn(0, image.width - visible)
        val width = visible * scale
        val height = srcHeight * scale
        val left = ((size.width - width) / 2).toInt()
        // Срезанная сцена прижата к верху: обрез неба приходится на край
        // карточки, а свободное место снизу занимает трава.
        val top = if (crop > 0) 0 else ((size.height - height) / 2).toInt()
        val right = size.width.toInt() - left - width
        val bottom = size.height.toInt() - top - height
        if (background != Color.Transparent) drawRect(background)
        if (extendEdges) {
            fun edge(src: IntOffset, srcSize: IntSize, dst: IntOffset, dstSize: IntSize) {
                if (dstSize.width > 0 && dstSize.height > 0) {
                    drawImage(image, src, srcSize, dst, dstSize, filterQuality = FilterQuality.None)
                }
            }
            // Сначала бока на высоту изображения, затем верх и низ на всю
            // ширину области — вместе с углами.
            edge(IntOffset(srcLeft, crop), IntSize(1, srcHeight), IntOffset(0, top), IntSize(left, height))
            edge(
                IntOffset(srcLeft + visible - 1, crop), IntSize(1, srcHeight),
                IntOffset(left + width, top), IntSize(right, height),
            )
            edge(IntOffset(srcLeft, crop), IntSize(visible, 1), IntOffset(0, 0), IntSize(size.width.toInt(), top))
            edge(
                IntOffset(srcLeft, image.height - 1), IntSize(visible, 1),
                IntOffset(0, top + height), IntSize(size.width.toInt(), bottom),
            )
        }
        drawImage(
            image = image,
            srcOffset = IntOffset(srcLeft, crop),
            srcSize = IntSize(visible, srcHeight),
            dstOffset = IntOffset(left, top),
            dstSize = IntSize(width, height),
            filterQuality = FilterQuality.None,
        )
        overlay(scale, left + (coreLeft - srcLeft) * scale, top - crop * scale)
    }
}
