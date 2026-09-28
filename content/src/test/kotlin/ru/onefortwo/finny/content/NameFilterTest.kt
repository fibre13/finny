package ru.onefortwo.finny.content

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Проверка игрового имени: цензура без лишних ограничений. */
class NameFilterTest {

    private val filter = NameFilter(
        ContentParser.parseForbiddenWords(File("src/main/assets/forbidden_words.json").readText()),
    )

    @Test
    fun `обзывательства, мат и слова про какашки не проходят`() {
        listOf(
            "дурак", "Дебил", "идиот", "Лох", "тупой", "Придурок", "Придурок228",
            "дурааак", "дееебил", "КаКаАа", "Какашка", "Фекалий", "Попа", "попу", "Писюн", "пися",
            "Жопа", "говнюк", "Сука", "Мудак",
        ).forEach { assertFalse("«$it» должно быть запрещено", filter.isAllowed(it)) }
    }

    @Test
    fun `нормальные, странные и иностранные имена проходят`() {
        listOf(
            "Барсик", "Мурзик", "Кузя", "Пупсик", "Пупик", "Рекс", "Зефирка",
            "Камень", "Тучка", "Носк", "Max", "Bella", "Бим007", "12345", "🐶", "Хлебушек",
            "Попугай", "Попкорн", "Какаду", "Лохматик", "Мандарин", "Хохлатка", "Книга",
        ).forEach { assertTrue("«$it» должно проходить", filter.isAllowed(it)) }
    }

    @Test
    fun `подмена букв латиницей и цифрами не помогает`() {
        // «пoпa» — с латинскими «o» и «a»; «д0лб0еб» — с нулями.
        assertFalse(filter.isAllowed("пoпa"))
        assertFalse(filter.isAllowed("д0лб0еб"))
        assertFalse(filter.isAllowed("дурaк"))
    }

    @Test
    fun `короткие слова не стираются посреди ввода нормального имени`() {
        // Корни проверяются на каждом нажатии, короткие слова — по окончании
        // ввода: «Попу» — начало «Попугая», «Лох» — начало «Лохматика».
        listOf("Попу", "Лох", "Кака", "Манда").forEach {
            assertFalse("«$it» не должно стираться на лету", filter.hasForbiddenRoot(it))
            assertTrue("«$it» запрещено как имя целиком", filter.isForbiddenWord(it))
        }
        // Запрещённый корень стирается сразу.
        assertTrue(filter.hasForbiddenRoot("Дурак"))
    }
}
