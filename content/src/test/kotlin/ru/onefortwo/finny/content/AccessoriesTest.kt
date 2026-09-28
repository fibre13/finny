package ru.onefortwo.finny.content

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** ТЕСТ: питомец носит несколько украшений сразу. */
class AccessoriesTest {

    private val parts = ContentParser.parsePetParts(File("src/main/assets/pet_parts.json").readText())
    private val order = parts.accessories.map { it.id }

    @Test
    fun `украшения надеваются и снимаются по одному, порядок — как в каталоге`() {
        var key = Accessories.NONE
        key = Accessories.toggle(key, "hat", order)
        key = Accessories.toggle(key, "bow", order)
        assertEquals("bow,hat", key)
        assertTrue(Accessories.isOn(key, "hat"))
        assertFalse(Accessories.isOn(key, Accessories.NONE))

        key = Accessories.toggle(key, "hat", order)
        assertEquals("bow", key)
        // «Без украшения» снимает всё.
        assertEquals(Accessories.NONE, Accessories.toggle("bow,scarf", Accessories.NONE, order))
        assertTrue(Accessories.isOn(Accessories.NONE, Accessories.NONE))
    }

    @Test
    fun `подпись набора — названия через «и»`() {
        assertEquals("бантик и шапочка", Accessories.title(parts, "bow,hat"))
        assertEquals("без украшения", Accessories.title(parts, Accessories.NONE))
        // Старое хранение одним кодом читается так же.
        assertEquals(listOf("scarf"), Accessories.list("scarf"))
    }

    @Test
    fun `шапочка есть в каталоге украшений`() {
        assertTrue(parts.accessories.any { it.id == "hat" })
    }
}
