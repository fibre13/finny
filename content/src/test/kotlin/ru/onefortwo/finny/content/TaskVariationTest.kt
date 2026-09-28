package ru.onefortwo.finny.content

import java.io.File
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Проверка переменных чисел в заданиях (решение по итогам проверки
 * на устройстве: числа берутся заново при каждом прохождении).
 *
 * Главное, что здесь проверяется, — согласованность: ребёнок видит
 * условие с одними числами, а проверка ответа идёт по тем же числам,
 * и в тексте не остаётся мест подстановки.
 */
class TaskVariationTest {

    private val tasks = ContentParser.parseTasks(File("src/main/assets/tasks.json").readText())

    private fun task(id: String): TaskContent =
        tasks.firstOrNull { it.id == id } ?: error("В контенте нет задания $id")

    /** Десять независимых наборов чисел одного задания. */
    private fun draws(id: String, count: Int = 10): List<TaskContent> =
        (1..count).map { seed -> task(id).withNumbers(Random(seed)) }

    // --- выражения --------------------------------------------------------

    @Test
    fun `выражение считается слева направо без приоритета действий`() {
        val values = mapOf("a" to 2, "b" to 3, "c" to 4)

        assertEquals(20, evaluate("a+b*c", values))
        assertEquals(24, evaluate("a*b*c", values))
        assertEquals(1, evaluate("c-b", values))
        assertEquals(6, evaluate("c/a*b", values))
    }

    @Test
    fun `в выражении можно смешивать имена и числа`() {
        assertEquals(11, evaluate("plan+4", mapOf("plan" to 7)))
        assertEquals(7, evaluate("10-3", emptyMap()))
    }

    @Test
    fun `неизвестное имя в выражении останавливает разбор`() {
        val error = runCatching { evaluate("plan+bonus", mapOf("plan" to 7)) }.exceptionOrNull()

        assertNotNull(error)
        assertTrue(error!!.message!!.contains("bonus"))
    }

    // --- подстановка ------------------------------------------------------

    @Test
    fun `подстановка без слова даёт само число`() {
        assertEquals("осталось 5", "осталось {gap}".fill(mapOf("gap" to 5)))
    }

    @Test
    fun `слово рядом с числом склоняется`() {
        val text = "{n:монет}"

        assertEquals("1 монета", text.fill(mapOf("n" to 1)))
        assertEquals("2 монеты", text.fill(mapOf("n" to 2)))
        assertEquals("4 монеты", text.fill(mapOf("n" to 4)))
        assertEquals("5 монет", text.fill(mapOf("n" to 5)))
        assertEquals("11 монет", text.fill(mapOf("n" to 11)))
        assertEquals("14 монет", text.fill(mapOf("n" to 14)))
        assertEquals("21 монета", text.fill(mapOf("n" to 21)))
        assertEquals("22 монеты", text.fill(mapOf("n" to 22)))
        assertEquals("25 монет", text.fill(mapOf("n" to 25)))
    }

    @Test
    fun `дни склоняются так же`() {
        val text = "за {n:дней}"

        assertEquals("за 1 день", text.fill(mapOf("n" to 1)))
        assertEquals("за 3 дня", text.fill(mapOf("n" to 3)))
        assertEquals("за 5 дней", text.fill(mapOf("n" to 5)))
    }

    @Test
    fun `место подстановки без значения останавливает разбор`() {
        val error = runCatching { "{gap}".fill(emptyMap()) }.exceptionOrNull()

        assertNotNull(error)
        assertTrue(error!!.message!!.contains("gap"))
    }

    // --- задания целиком --------------------------------------------------

    @Test
    fun `в текстах заданий не остаётся мест подстановки`() {
        tasks.forEach { source ->
            repeat(20) { seed ->
                // Имя питомца подставляется отдельно — перед показом.
                val task = source.withNumbers(Random(seed)).named("Тест")
                val options = when (task) {
                    is PickTask -> task.options.flatMap { listOfNotNull(it.title, it.note) }
                    is ChoiceTask -> task.options.flatMap { listOfNotNull(it.title, it.outcome, it.note, it.pet) }
                    else -> emptyList()
                }
                (
                    listOfNotNull(
                        task.title, task.prompt, task.explanationCorrect, task.explanationWrong,
                        task.context, task.hint, task.petCorrect, task.petWrong,
                    ) + options
                ).forEach { text ->
                    assertFalse(
                        "Задание ${task.id}: в тексте осталось место подстановки — $text",
                        text.contains('{'),
                    )
                }
            }
        }
    }

    @Test
    fun `задание без переменных чисел возвращается без изменений`() {
        val constant = tasks.first { it.vary == null }

        assertSame(constant, constant.withNumbers(Random(1)))
    }

    @Test
    fun `числа меняются от прохождения к прохождению`() {
        tasks.filter { it.vary != null }.forEach { source ->
            // Числа могут стоять в условии или в игровом событии над ним.
            val prompts = draws(source.id, count = 20).map { it.prompt + it.context.orEmpty() }.toSet()

            assertTrue(
                "Задание ${source.id}: условие ни разу не изменилось",
                prompts.size > 1,
            )
        }
    }

    @Test
    fun `верный ответ на задание с вводом числа сходится с условием`() {
        tasks.filterIsInstance<NumberTask>().filter { it.vary != null }.forEach { source ->
            draws(source.id, count = 50).forEach { drawn ->
                val task = drawn as NumberTask

                assertTrue("Задание ${task.id}: ответ ${task.answer}", task.answer > 0)
                assertTrue(
                    "Задание ${task.id}: верный ответ не принят",
                    task.check(TaskAnswer.Number(task.answer)).isCorrect,
                )
                // Ответ назван в объяснении — ребёнок видит, откуда он взялся.
                assertTrue(
                    "Задание ${task.id}: в объяснении нет ответа — ${task.explanationCorrect}",
                    task.explanationCorrect.contains(task.answer.toString()),
                )
            }
        }
    }

    @Test
    fun `в задании на распределение суммы хватает на обязательное`() {
        tasks.filterIsInstance<AllocateTask>().filter { it.vary != null }.forEach { source ->
            draws(source.id, count = 50).forEach { drawn ->
                val task = drawn as AllocateTask
                val required = task.rules.sumOf { it.min }

                assertTrue(
                    "Задание ${task.id}: сумма ${task.amount} меньше обязательного $required",
                    task.amount > required,
                )
                // ТЕСТ: слагаемые (корм и вода) — в событии, сумма — в объяснении.
                assertTrue(
                    "Задание ${task.id}: обязательное $required не названо в объяснении",
                    task.explanationCorrect.contains(required.toString()),
                )
            }
        }
    }

    @Test
    fun `в задании с отметкой все цены остаются положительными`() {
        tasks.filterIsInstance<PickTask>().filter { it.vary != null }.forEach { source ->
            draws(source.id, count = 50).forEach { drawn ->
                val task = drawn as PickTask

                task.options.forEach { option ->
                    assertTrue(
                        "Задание ${task.id}: цена «${option.title}» — ${option.price}",
                        (option.price ?: 1) > 0,
                    )
                }
            }
        }
    }

    @Test
    fun `в задании про план самый дорогой вариант и есть верный`() {
        // Смысл задания держится не на числе, а на связи: верный вариант
        // построен как «план плюс несколько монет», поэтому при любых
        // числах он остаётся тем, что в план не помещается.
        listOf("plan_vs_fact_easy", "plan_vs_fact").forEach { id ->
            draws(id, count = 50).forEach { drawn ->
                val task = drawn as PickTask
                val dearest = task.options.maxBy { it.price ?: 0 }

                assertEquals("Задание $id", task.correct, listOf(dearest.id))
                assertTrue(
                    "Задание $id: верный ответ принят",
                    task.check(TaskAnswer.Picked(task.correct.toSet())).isCorrect,
                )
            }
        }
    }

    @Test
    fun `в задании про корзину нужное дешевле желаемого`() {
        // Ребёнок откладывает мячик не потому, что он дороже, а потому,
        // что корм — нужное; но цены должны оставаться правдоподобными.
        listOf("cart_fit_easy", "cart_fit").forEach { id ->
            draws(id, count = 50).forEach { drawn ->
                val task = drawn as PickTask
                val food = task.options.first { it.id == "food" }.price!!
                val ball = task.options.first { it.id == "ball" }.price!!

                assertTrue("Задание $id: корм $food, мячик $ball", food < ball)
                // ТЕСТ: сумму ребёнок не считает — цены названы в условии.
                assertTrue(
                    "Задание $id: в условии нет цен $food и $ball",
                    task.prompt.contains(food.toString()) && task.prompt.contains(ball.toString()),
                )
            }
        }
    }
}
