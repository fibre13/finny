package ru.onefortwo.finny.content

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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
 * @property anchors якоря кадра 0 для пары «вид_стадия»: имя точки → клетка.
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
    val anchors: Map<String, Map<String, PixelPoint>>,
    val sceneAnchors: Map<String, PixelPoint>,
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
            points(pair.jsonObject.obj("0"))
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
        )
    }

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
