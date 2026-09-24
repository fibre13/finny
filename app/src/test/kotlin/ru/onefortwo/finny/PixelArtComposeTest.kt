package ru.onefortwo.finny

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.onefortwo.finny.content.AssetSource
import ru.onefortwo.finny.content.ContentRepository
import ru.onefortwo.finny.economy.GrowthStage
import ru.onefortwo.finny.economy.StatLevel
import ru.onefortwo.finny.ui.common.SceneStats
import ru.onefortwo.finny.ui.common.composeGoal
import ru.onefortwo.finny.ui.common.composeStats
import ru.onefortwo.finny.ui.common.sceneFullWidth
import ru.onefortwo.finny.ui.common.composePet
import ru.onefortwo.finny.ui.common.composeScene

/** Сборка изображений питомца и сцены из спрайтов пиксельной графики. */
class PixelArtComposeTest {

    private val content = ContentRepository(
        AssetSource { name -> File("../content/src/main/assets/$name").readText() },
    )
    private val art = content.pixelArt()
    private val parts = content.petParts()

    private fun argb(hex: String) = (0xFF000000 or hex.removePrefix("#").toLong(16)).toInt()

    @Test
    fun `каждое сочетание внешности собирается и окрашено своим окрасом`() {
        parts.species.forEach { species ->
            GrowthStage.entries.forEach { stage ->
                parts.colors.forEach { color ->
                    parts.accessories.forEach { accessory ->
                        val pixels = composePet(
                            art, species.id, stage, color.hex, accessory.id,
                            StatLevel.MEDIUM, StatLevel.MEDIUM,
                        )
                        val combo = "${species.id}/${stage.name}/${color.id}/${accessory.id}"
                        assertEquals(art.petSize * art.petSize, pixels.size)
                        assertTrue("Нет основного цвета окраса: $combo", argb(color.hex) in pixels)
                    }
                }
            }
        }
    }

    @Test
    fun `украшение и выражение меняют изображение`() {
        fun pet(accessory: String, care: StatLevel, joy: StatLevel) = composePet(
            art, "cat", GrowthStage.BABY, parts.colors.first().hex, accessory, care, joy,
        )
        val plain = pet("none", StatLevel.MEDIUM, StatLevel.MEDIUM)
        assertFalse(plain.contentEquals(pet("bow", StatLevel.MEDIUM, StatLevel.MEDIUM)))
        assertFalse(plain.contentEquals(pet("scarf", StatLevel.MEDIUM, StatLevel.MEDIUM)))
        assertFalse(plain.contentEquals(pet("none", StatLevel.MEDIUM, StatLevel.HIGH)))
        assertFalse(plain.contentEquals(pet("none", StatLevel.LOW, StatLevel.MEDIUM)))
    }

    @Test
    fun `палатка и полученная цель появляются в сцене`() {
        val pet = composePet(
            art, "dog", GrowthStage.ADULT, parts.colors.first().hex, "none", StatLevel.MEDIUM, StatLevel.MEDIUM,
            inScene = true,
        )
        val bare = composeScene(art, house = false, goalId = null, pet = pet)
        assertEquals(sceneFullWidth(art) * art.sceneHeight, bare.size)
        assertFalse(bare.contentEquals(composeScene(art, house = true, goalId = null, pet = pet)))
        content.goals().forEach { goal ->
            assertFalse(
                "Цель ${goal.id} не видна в сцене",
                bare.contentEquals(composeScene(art, house = false, goalId = goal.id, pet = pet)),
            )
            assertTrue("Нет иллюстрации цели ${goal.id}", composeGoal(art, goal.id) != null)
        }
    }

    @Test
    fun `панель самочувствия показывает уровень числом делений, низкий — коралловым`() {
        val coral = art.colors[art.indexOf('R')]
        val green = art.colors[art.indexOf('E')]
        val empty = art.colors[art.indexOf('z')]
        // Деление 3 × 4 клетки: считается число клеток нужного цвета.
        val cellsPerSegment = 12
        val full = composeStats(art, SceneStats(care = 100, careLow = false, joy = 100, joyLow = false))
        val low = composeStats(art, SceneStats(care = 20, careLow = true, joy = 60, joyLow = false))

        assertEquals(0, full.count { it == empty })
        assertTrue("Нет зелёных делений заботы", full.count { it == green } >= 5 * cellsPerSegment)
        assertEquals("Одно деление низкой заботы", cellsPerSegment, low.count { it == coral })
        // Забота 20 — одно деление из пяти, радость 60 — три: пустых 4 + 2.
        assertEquals(6 * cellsPerSegment, low.count { it == empty })
    }
}
