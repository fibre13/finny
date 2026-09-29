package ru.onefortwo.finny.content

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.onefortwo.finny.economy.BudgetCategory

/**
 * Проверки фактических файлов учебного контента: формат разбирается,
 * позиции переводятся в доменные типы, объём соответствует минимуму
 * из ТЗ 2.6. Тест выполняется на JVM, эмулятор не требуется.
 */
class ContentFilesTest {

    private fun asset(name: String): String {
        val file = File("src/main/assets/$name")
        assertTrue("Файл контента не найден: ${file.absolutePath}", file.exists())
        return file.readText()
    }

    private val shop = ContentParser.parseShop(asset("shop.json"))
    private val goals = ContentParser.parseGoals(asset("goals.json"))

    @Test
    fun `каталог покупок содержит не менее восьми позиций`() {
        assertTrue("Позиций в каталоге: ${shop.size}", shop.size >= 8)
    }

    @Test
    fun `в каталоге представлены оба типа расходов`() {
        val categories = shop.map { it.category }.toSet()

        assertEquals(setOf(ItemCategory.NEEDS, ItemCategory.WANTS), categories)
    }

    @Test
    fun `целей накопления не менее трёх`() {
        assertTrue("Целей: ${goals.size}", goals.size >= 3)
    }

    @Test
    fun `идентификаторы позиций каталога уникальны`() {
        val ids = shop.map { it.id }

        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `идентификаторы целей уникальны`() {
        val ids = goals.map { it.id }

        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `каждая позиция каталога переводится в доменный тип`() {
        shop.forEach { item ->
            val domain = item.toDomain()

            assertEquals(item.id, domain.id)
            assertEquals(item.price, domain.price.amount)
            assertTrue("Цена должна быть положительной: ${item.id}", domain.price.amount > 0)
        }
    }

    @Test
    fun `обязательные позиции влияют на заботу, необязательные на радость`() {
        shop.forEach { item ->
            val domain = item.toDomain()
            val expected = when (domain.category) {
                BudgetCategory.NEEDS -> PetStat.CARE
                BudgetCategory.WANTS -> PetStat.JOY
                BudgetCategory.SAVINGS -> error("Позиция каталога не может быть накоплением")
            }

            assertEquals("Несоответствие показателя у позиции ${item.id}", expected, item.stat)
        }
    }

    @Test
    fun `каждая позиция каталога объясняет влияние на питомца`() {
        shop.forEach { item ->
            assertTrue("Пустое пояснение у позиции ${item.id}", item.effect.isNotBlank())
        }
    }

    @Test
    fun `пояснение покупки называет питомца его именем`() {
        shop.forEach { item ->
            // Имя подставляется, а не вписано: ребёнок мог назвать питомца иначе.
            assertTrue("Нет {name} в пояснении ${item.id}", item.effect.contains("{name}"))
            assertTrue("Имя вписано в пояснение ${item.id}", !item.effect.contains("Финни"))
            assertTrue(item.effectFor("Мурзик").startsWith("Мурзик"))
        }
    }

    @Test
    fun `каждая цель переводится в доменный тип`() {
        goals.forEach { goal ->
            val domain = goal.toDomain()

            assertEquals(goal.id, domain.id)
            assertTrue("Стоимость цели должна быть положительной", domain.price.amount > 0)
        }
    }

    @Test
    fun `украшения гардероба покупаются в каталоге`() {
        val unlocks = shop.mapNotNull { it.unlocksAccessory }.toSet()

        // Надеть украшение можно только после покупки, поэтому каждое
        // украшение, кроме варианта «без украшения», должно продаваться.
        assertTrue("Украшения в каталоге: $unlocks", unlocks.containsAll(setOf("bow", "scarf")))
        // Украшения — желаемое. Шарфик — тёплая вещь: в холод он нужен
        // (событие «Стало холодно»), поэтому он — нужное.
        shop.filter { it.unlocksAccessory != null && it.id != "scarf" }.forEach {
            assertEquals(ItemCategory.WANTS, it.category)
        }
        assertEquals(ItemCategory.NEEDS, shop.first { it.id == "scarf" }.category)
    }

    @Test
    fun `обстановка появляется на фоне только после покупки`() {
        val scenery = shop.mapNotNull { it.unlocksScenery }.toSet()

        // Дом на фоне главного экрана — следствие покупки «Домика-палатки»,
        // наклейки на нём — покупки «Наклеек», а не украшение интерфейса:
        // без покупки их на экране нет.
        assertEquals(setOf("house", "stickers"), scenery)
        val tent = shop.first { it.unlocksScenery == "house" }
        assertEquals("tent", tent.id)
        assertEquals(ItemCategory.WANTS, tent.category)
        val stickers = shop.first { it.unlocksScenery == "stickers" }
        assertEquals("stickers", stickers.id)
        assertEquals(ItemCategory.WANTS, stickers.category)
    }

    @Test
    fun `в каталоге есть обязательная позиция дешевле карманных монет`() {
        val cheapest = shop.filter { it.category == ItemCategory.NEEDS }.minOf { it.price }

        // Ребёнок должен иметь возможность закрыть обязательные расходы
        // из периодического дохода, иначе сценарий заходит в тупик.
        assertTrue("Самая дешёвая обязательная позиция: $cheapest", cheapest <= 30)
    }
}
