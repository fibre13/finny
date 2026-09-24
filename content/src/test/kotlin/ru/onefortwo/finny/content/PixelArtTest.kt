package ru.onefortwo.finny.content

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Пиксельная графика согласована с составом внешности и целей: на каждый
 * вид, стадию, окрас, украшение и цель из контента есть спрайт и якорь.
 */
class PixelArtTest {

    private val art = PixelArtParser.parse(File("src/main/assets/pixel_art.json").readText())
    private val parts = ContentParser.parsePetParts(File("src/main/assets/pet_parts.json").readText())
    private val goals = ContentParser.parseGoals(File("src/main/assets/goals.json").readText())
    private val stages = listOf("baby", "teen", "adult")

    @Test
    fun `у каждого окраса есть рампа с тем же основным цветом`() {
        parts.colors.forEach { color ->
            val ramp = art.ramps[color.id]
            assertNotNull("Нет рампы окраса ${color.id}", ramp)
            val base = (0xFF000000 or color.hex.removePrefix("#").toLong(16)).toInt()
            assertEquals("Основной цвет рампы ${color.id}", base, ramp!![0])
        }
    }

    @Test
    fun `на каждый вид и стадию есть фигура, лицо и украшения`() {
        parts.species.forEach { species ->
            stages.forEach { stage ->
                val key = "${species.id}_$stage"
                val figure = art.sprite("${key}_0")
                assertNotNull("Нет фигуры $key", figure)
                assertEquals(art.petSize, figure!!.width)
                assertEquals(art.petSize, figure.height)
                val anchors = art.anchors[key]
                assertNotNull("Нет якорей $key", anchors)
                listOf("eyes", "mouth", "bow", "scarf").forEach { name ->
                    assertTrue("Нет якоря $name у $key", name in anchors!!)
                }
                assertNotNull("Нет шарфика $key", art.sprite("scarf_$key"))
            }
            listOf("normal", "happy", "tired").forEach {
                assertNotNull("Нет глаз $it у ${species.id}", art.sprite("${species.id}_eyes_$it"))
            }
            listOf("smile", "neutral", "sad").forEach {
                assertNotNull("Нет рта $it у ${species.id}", art.sprite("${species.id}_mouth_$it"))
            }
            assertNotNull("Нет бантика у ${species.id}", art.sprite("bow_${species.id}"))
        }
    }

    @Test
    fun `на каждую цель есть иллюстрация, у сцены — слои и якоря`() {
        goals.forEach { assertNotNull("Нет иллюстрации цели ${it.id}", art.sprite(it.id)) }
        listOf("sky", "ground", "tent").forEach { assertNotNull("Нет слоя сцены $it", art.sprite(it)) }
        listOf("pet", "goal", "tent").forEach { assertTrue("Нет якоря сцены $it", it in art.sceneAnchors) }
        assertTrue("Нет цвета неба", art.indexOf('A') >= 0)
    }

    @Test
    fun `индексы клеток не выходят за палитру`() {
        art.sprites.forEach { (id, sprite) ->
            assertEquals("Размер спрайта $id", sprite.width * sprite.height, sprite.pixels.size)
            sprite.pixels.forEach { index ->
                assertTrue("Индекс $index вне палитры в $id", index == -1 || index in art.colors.indices)
            }
        }
    }
}
