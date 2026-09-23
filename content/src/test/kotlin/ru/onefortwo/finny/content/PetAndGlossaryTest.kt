package ru.onefortwo.finny.content

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Проверки внешности питомца и справочного раздела (ТЗ 2.5.2, 2.5.11, 2.6).
 */
class PetAndGlossaryTest {

    private val parts = ContentParser.parsePetParts(File("src/main/assets/pet_parts.json").readText())
    private val glossary = ContentParser.parseGlossary(File("src/main/assets/glossary.json").readText())

    @Test
    fun `комбинаций внешности не менее девяти`() {
        assertTrue(
            "Комбинаций: ${parts.appearanceCombinations}",
            parts.appearanceCombinations >= 9,
        )
    }

    @Test
    fun `идентификаторы частей внешности уникальны`() {
        assertEquals(parts.species.size, parts.species.map { it.id }.toSet().size)
        assertEquals(parts.colors.size, parts.colors.map { it.id }.toSet().size)
        assertEquals(parts.accessories.size, parts.accessories.map { it.id }.toSet().size)
    }

    @Test
    fun `у каждой части внешности есть название`() {
        (parts.species.map { it.title } + parts.colors.map { it.title } +
            parts.accessories.map { it.title }).forEach { title ->
            assertTrue("Пустое название части внешности", title.isNotBlank())
        }
    }

    @Test
    fun `окрасы заданы корректным значением цвета`() {
        val pattern = Regex("^#[0-9A-Fa-f]{6}$")

        parts.colors.forEach { color ->
            assertTrue("Некорректный цвет у окраса ${color.id}: ${color.hex}", pattern.matches(color.hex))
        }
    }

    @Test
    fun `предусмотрен вариант без аксессуара`() {
        assertTrue(parts.accessories.any { it.id == "none" })
    }

    @Test
    fun `справочник объясняет основные термины`() {
        val terms = glossary.map { it.term.lowercase() }

        listOf("бюджет", "план", "копилка", "цель").forEach { required ->
            assertTrue("В справочнике нет термина «$required»", terms.any { it.contains(required) })
        }
    }

    @Test
    fun `каждый термин справочника имеет объяснение`() {
        glossary.forEach { entry ->
            assertTrue("Пустой термин в справочнике", entry.term.isNotBlank())
            assertTrue("Нет объяснения термина ${entry.term}", entry.explanation.isNotBlank())
        }
    }

    @Test
    fun `объяснения терминов короткие`() {
        glossary.forEach { entry ->
            // Ориентир для возраста 7–11 лет: одно-два предложения.
            assertTrue(
                "Слишком длинное объяснение термина ${entry.term}: ${entry.explanation.length} символов",
                entry.explanation.length <= 200,
            )
        }
    }
}
