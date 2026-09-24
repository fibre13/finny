package ru.onefortwo.finny.content

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Пиксельная графика: питомец, части лица, украшения, сцена и цели.
 *
 * Исходники и сборка — `art/pixel` (см. его README); в приложение кладётся
 * собранный `pixel_art.json`. Здесь только разбор: как из спрайтов
 * складывается изображение, решает интерфейс.
 *
 * @property colors цвета палитры в формате ARGB по индексу.
 * @property furIndices индексы шерсти: основной, тень, свет — подменяются
 * рампой окраса.
 * @property ramps рампы окрасов по идентификатору из `pet_parts.json`:
 * основной, тень, свет (ARGB).
 * @property shadowIndex индекс тени под фигурой; цвет берётся по фону.
 * @property anchors якоря пары «вид_стадия» по кадрам дыхания (0 и 1):
 * имя точки → клетка. Во втором кадре голова опущена на клетку, и точки
 * лица и украшений ниже на единицу.
 * @property sceneAnchors якоря сцены: `pet`, `goal`, `tent`.
 * @property baseline строка, на которой стоит фигура: точка привязки
 * питомца — середина этой строки.
 */
class PixelArt(
    val petSize: Int,
    val baseline: Int,
    val sceneWidth: Int,
    val sceneHeight: Int,
    val colors: IntArray,
    val chars: String,
    val furIndices: IntArray,
    val ramps: Map<String, IntArray>,
    val shadowIndex: Int,
    val shadowOnCard: Int,
    val shadowInScene: Int,
    val sprites: Map<String, PixelSprite>,
    val anchors: Map<String, List<Map<String, PixelPoint>>>,
    val sceneAnchors: Map<String, PixelPoint>,
    val animation: PixelAnimation,
) {
    fun sprite(id: String): PixelSprite? = sprites[id]

    /** Индекс символа палитры, например `A` — небо. */
    fun indexOf(char: Char): Int = chars.indexOf(char)
}

/**
 * Спрайт: индексы палитры построчно, `-1` — прозрачная клетка.
 * Точка привязки [pivotX], [pivotY] совмещается с якорем.
 */
class PixelSprite(
    val width: Int,
    val height: Int,
    val pivotX: Int,
    val pivotY: Int,
    val pixels: IntArray,
)

data class PixelPoint(val x: Int, val y: Int)

/**
 * Постоянные движения питомца и сцены (`animation.json`). Такт — [tickMs];
 * интервалы заданы в тактах.
 *
 * @property states состояние `idle`, `happy` или `tired` → его движения.
 * @property skyFrames кадры неба, сменяются раз в [skyTicks] тактов.
 * @property reactions реакции на события игры: `eat`, `play`, `save`,
 * `reward`, `grow`.
 * @property fx кадры частиц по имени: `coin`, `heart`, `sparkle`, `note`,
 * `crumb` — от яркого к тусклому.
 */
class PixelAnimation(
    val tickMs: Long,
    val states: Map<String, PixelMotion>,
    val skyFrames: List<String>,
    val skyTicks: Int,
    val reactions: Map<String, PixelReaction>,
    val fx: Map<String, List<String>>,
)

/**
 * Реакция длиной [ticks] тактов. Всё, что не задано, берётся из
 * постоянного состояния питомца.
 *
 * @property eyes глаза на всю реакцию.
 * @property mouth кадры рта на отрезке тактов.
 * @property jumps прыжки: такт начала и смещения по тактам.
 * @property flashAt такт светлой вспышки силуэта длиной [flashTicks].
 * @property swapStage такт, с которого рисуется новая стадия (рост).
 */
class PixelReaction(
    val ticks: Int,
    val eyes: String?,
    val mouth: PixelMouthTrack?,
    val jumps: List<Pair<Int, IntArray>>,
    val emits: List<PixelEmit>,
    val flashAt: Int?,
    val flashTicks: Int,
    val swapStage: Int?,
)

/** Кадры рта [frames] сменяются каждые [ticks] тактов на отрезке [from]…[to). */
class PixelMouthTrack(val from: Int, val to: Int, val frames: List<String>, val ticks: Int)

/**
 * Частица [fx], появляется на такте [at] у точки [from] (якорь, `base` или
 * `outline` — точка [point] из [of] долей контура), сдвиг [dx], [dy],
 * скорость [vx], [vy] клеток за такт, живёт [life] тактов.
 */
class PixelEmit(
    val at: Int,
    val fx: String,
    val from: String,
    val dx: Int,
    val dy: Int,
    val vx: Double,
    val vy: Double,
    val life: Int,
    val point: Int,
    val of: Int,
)

/**
 * Движения одного состояния.
 *
 * @property breathTicks сколько тактов держится кадр дыхания.
 * @property blinkEyes глаза на время моргания; [blinkTicks] — его длина,
 * следующее — через случайное число тактов из [blinkInterval].
 * @property jumpOffsets смещения фигуры вверх по тактам прыжка; `null` —
 * состояние без прыжков.
 */
class PixelMotion(
    val breathTicks: Int,
    val blinkEyes: String,
    val blinkTicks: Int,
    val blinkInterval: IntRange,
    val jumpOffsets: IntArray?,
    val jumpInterval: IntRange?,
)

/** Разбор `pixel_art.json` формата 1. */
object PixelArtParser {

    fun parse(text: String): PixelArt {
        val root = Json.parseToJsonElement(text).jsonObject
        require(root.int("format") == 1) { "Неизвестный формат пиксельной графики" }

        val grid = root.obj("grid")
        val palette = root.obj("palette")
        val chars = palette.str("chars")
        val colors = palette.getValue("colors").jsonArray.map { argb(it.jsonPrimitive.content) }.toIntArray()
        val fur = palette.obj("fur")
        val shadow = palette.obj("shadow")

        val sprites = root.getValue("sprites").jsonArray.associate { element ->
            val s = element.jsonObject
            val w = s.int("w")
            val h = s.int("h")
            val pivot = s["pivot"]?.jsonArray
            s.str("id") to PixelSprite(
                width = w,
                height = h,
                pivotX = pivot?.get(0)?.jsonPrimitive?.int ?: 0,
                pivotY = pivot?.get(1)?.jsonPrimitive?.int ?: 0,
                pixels = decodeRows(s.getValue("rows").jsonArray, w, h, chars),
            )
        }

        val anchorsRoot = root.obj("anchors")
        val anchors = anchorsRoot.filterKeys { it != "scene" }.mapValues { (_, pair) ->
            listOf(points(pair.jsonObject.obj("0")), points(pair.jsonObject.obj("1")))
        }

        return PixelArt(
            petSize = grid.getValue("pet").jsonArray[0].jsonPrimitive.int,
            baseline = grid.int("baseline"),
            sceneWidth = grid.getValue("scene").jsonArray[0].jsonPrimitive.int,
            sceneHeight = grid.getValue("scene").jsonArray[1].jsonPrimitive.int,
            colors = colors,
            chars = chars,
            furIndices = intArrayOf(fur.int("F"), fur.int("f"), fur.int("G")),
            ramps = palette.obj("ramps").mapValues { (_, ramp) ->
                ramp.jsonArray.map { argb(it.jsonPrimitive.content) }.toIntArray()
            },
            shadowIndex = shadow.int("index"),
            shadowOnCard = argb(shadow.str("card")),
            shadowInScene = argb(shadow.str("scene")),
            sprites = sprites,
            anchors = anchors,
            sceneAnchors = points(anchorsRoot.obj("scene")),
            animation = parseAnimation(root.obj("animation")),
        )
    }

    private fun parseAnimation(obj: JsonObject): PixelAnimation {
        val sky = obj.obj("sky")
        return PixelAnimation(
            tickMs = obj.int("tick_ms").toLong(),
            states = obj.obj("states").mapValues { (_, value) ->
                val state = value.jsonObject
                val blink = state.obj("blink")
                val jump = state["jump"]?.jsonObject
                PixelMotion(
                    breathTicks = state.obj("breath").int("ticks"),
                    blinkEyes = blink.str("eyes"),
                    blinkTicks = blink.int("ticks"),
                    blinkInterval = range(blink.getValue("interval").jsonArray),
                    jumpOffsets = jump?.getValue("offsets")?.jsonArray?.map { it.jsonPrimitive.int }?.toIntArray(),
                    jumpInterval = jump?.let { range(it.getValue("interval").jsonArray) },
                )
            },
            skyFrames = sky.getValue("frames").jsonArray.map { it.jsonPrimitive.content },
            skyTicks = sky.int("ticks"),
            reactions = obj.obj("reactions").mapValues { (_, value) -> parseReaction(value.jsonObject) },
            fx = obj.obj("fx").mapValues { (_, frames) -> frames.jsonArray.map { it.jsonPrimitive.content } },
        )
    }

    private fun parseReaction(obj: JsonObject): PixelReaction {
        val mouth = obj["mouth"]?.jsonObject
        val flash = obj["flash"]?.jsonObject
        return PixelReaction(
            ticks = obj.int("ticks"),
            eyes = obj["eyes"]?.jsonPrimitive?.content,
            mouth = mouth?.let {
                PixelMouthTrack(
                    from = it.int("from"),
                    to = it.int("to"),
                    frames = it.getValue("frames").jsonArray.map { f -> f.jsonPrimitive.content },
                    ticks = it.int("ticks"),
                )
            },
            jumps = obj["jump"]?.jsonArray?.map { j ->
                j.jsonObject.int("at") to j.jsonObject.getValue("offsets").jsonArray.map { it.jsonPrimitive.int }.toIntArray()
            }.orEmpty(),
            emits = obj["emit"]?.jsonArray?.map { e ->
                val emit = e.jsonObject
                PixelEmit(
                    at = emit.int("at"),
                    fx = emit.str("fx"),
                    from = emit.str("from"),
                    dx = emit["dx"]?.jsonPrimitive?.int ?: 0,
                    dy = emit["dy"]?.jsonPrimitive?.int ?: 0,
                    vx = emit["vx"]?.jsonPrimitive?.double ?: 0.0,
                    vy = emit["vy"]?.jsonPrimitive?.double ?: 0.0,
                    life = emit.int("life"),
                    point = emit["point"]?.jsonPrimitive?.int ?: 0,
                    of = emit["of"]?.jsonPrimitive?.int ?: 1,
                )
            }.orEmpty(),
            flashAt = flash?.int("at"),
            flashTicks = flash?.int("ticks") ?: 0,
            swapStage = obj["swap_stage"]?.jsonPrimitive?.int,
        )
    }

    private fun range(pair: JsonArray): IntRange = pair[0].jsonPrimitive.int..pair[1].jsonPrimitive.int

    /**
     * Строка спрайта — либо строка символов палитры (`.` — прозрачная
     * клетка), либо серии `[индекс, длина]` с индексом `-1` для прозрачных.
     */
    private fun decodeRows(rows: JsonArray, width: Int, height: Int, chars: String): IntArray {
        require(rows.size == height) { "Число строк спрайта не совпадает с высотой" }
        val pixels = IntArray(width * height)
        rows.forEachIndexed { y, row ->
            var x = 0
            if (row is JsonPrimitive) {
                row.content.forEach { char ->
                    pixels[y * width + x++] = if (char == '.') -1 else chars.indexOf(char).also {
                        require(it >= 0) { "Символ вне палитры: $char" }
                    }
                }
            } else {
                row.jsonArray.forEach { run ->
                    val index = run.jsonArray[0].jsonPrimitive.int
                    repeat(run.jsonArray[1].jsonPrimitive.int) { pixels[y * width + x++] = index }
                }
            }
            require(x == width) { "Длина строки спрайта не совпадает с шириной" }
        }
        return pixels
    }

    private fun points(obj: JsonObject): Map<String, PixelPoint> = obj.mapValues { (_, p) ->
        PixelPoint(p.jsonObject.int("x"), p.jsonObject.int("y"))
    }

    private fun argb(hex: String): Int = (0xFF000000 or hex.removePrefix("#").toLong(16)).toInt()

    private fun JsonObject.obj(key: String): JsonObject = getValue(key).jsonObject
    private fun JsonObject.str(key: String): String = getValue(key).jsonPrimitive.content
    private fun JsonObject.int(key: String): Int = getValue(key).jsonPrimitive.int
}
