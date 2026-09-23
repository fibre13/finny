package ru.onefortwo.finny.content

import java.io.File
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.onefortwo.finny.economy.Difficulty
import ru.onefortwo.finny.economy.IncomeSource

/**
 * Проверки фактического файла заданий: объём и темы соответствуют ТЗ 2.6
 * и 2.5.8, каждое задание разбирается и содержит оба объяснения.
 */
class TaskContentTest {

    private val tasks = ContentParser.parseTasks(File("src/main/assets/tasks.json").readText())

    private fun task(id: String): TaskContent =
        tasks.firstOrNull { it.id == id } ?: error("В контенте нет задания $id")

    @Test
    fun `заданий не менее шести`() {
        assertTrue("Заданий: ${tasks.size}", tasks.size >= 6)
    }

    @Test
    fun `представлены все три обязательные темы`() {
        val topics = tasks.map { it.topic }.toSet()

        assertTrue(topics.contains(TaskTopic.BUDGET_PLANNING))
        assertTrue(topics.contains(TaskTopic.SAVINGS))
        assertTrue(topics.contains(TaskTopic.PAYMENTS))
    }

    @Test
    fun `в каждой обязательной теме есть хотя бы два задания`() {
        listOf(TaskTopic.BUDGET_PLANNING, TaskTopic.SAVINGS, TaskTopic.PAYMENTS).forEach { topic ->
            val count = tasks.count { it.topic == topic }
            assertTrue("В теме ${topic.displayName} заданий: $count", count >= 2)
        }
    }

    @Test
    fun `задания не сводятся к выбору ответа из вариантов`() {
        val kinds = tasks.map { it::class.simpleName }.toSet()

        // Требование ТЗ 2.5.8: помимо выбора варианта есть распределение
        // суммы, отметка в списке и ввод числа.
        assertTrue(kinds.contains("AllocateTask"))
        assertTrue(kinds.contains("PickTask"))
        assertTrue(kinds.contains("NumberTask"))
        assertTrue(kinds.contains("ChoiceTask"))
    }

    @Test
    fun `на каждом уровне сложности не меньше шести заданий`() {
        Difficulty.entries.forEach { difficulty ->
            val available = tasks.filter { it.level.suits(difficulty) }
            assertTrue(
                "Для уровня «${difficulty.displayName}» доступно заданий: ${available.size}",
                available.size >= 6,
            )
        }
    }

    @Test
    fun `на каждом уровне сложности представлены все три обязательные темы`() {
        Difficulty.entries.forEach { difficulty ->
            val topics = tasks.filter { it.level.suits(difficulty) }.map { it.topic }.toSet()
            listOf(
                TaskTopic.BUDGET_PLANNING,
                TaskTopic.SAVINGS,
                TaskTopic.PAYMENTS,
            ).forEach { topic ->
                assertTrue(
                    "На уровне «${difficulty.displayName}» нет темы ${topic.displayName}",
                    topic in topics,
                )
            }
        }
    }

    @Test
    fun `на простом уровне нет заданий на деление`() {
        val easy = tasks.filter { it.level.suits(Difficulty.SIMPLE) }

        // Уровень «Попроще» обходится без деления, поэтому задание на срок
        // накопления в него не попадает (ТЗ 2.5.8).
        assertTrue(easy.none { it.id == "save_rate" })
    }

    @Test
    fun `числа в заданиях простого уровня не выходят за пределы двадцати`() {
        // Числа в заданиях переменные, поэтому предел проверяется не только
        // у записи в контенте, но и у наборов, которые из неё получаются.
        val easy = tasks.filter { it.level == TaskLevel.EASY }
            .flatMap { task -> listOf(task) + (1..100).map { task.withNumbers(Random(it)) } }

        easy.filterIsInstance<AllocateTask>().forEach {
            assertTrue("Сумма ${it.amount} велика для ${it.id}", it.amount <= 20)
        }
        easy.filterIsInstance<NumberTask>().forEach {
            assertTrue("Ответ ${it.answer} велик для ${it.id}", it.answer <= 20)
        }
        easy.filterIsInstance<PickTask>().forEach { task ->
            task.options.mapNotNull { it.price }.forEach { price ->
                assertTrue("Цена $price велика для ${task.id}", price <= 20)
            }
        }
    }

    @Test
    fun `идентификаторы заданий уникальны`() {
        val ids = tasks.map { it.id }

        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `каждое задание содержит условие и оба объяснения`() {
        tasks.forEach { t ->
            assertTrue("Пустое условие в задании ${t.id}", t.prompt.isNotBlank())
            assertTrue("Нет объяснения при верном ответе в ${t.id}", t.explanationCorrect.isNotBlank())
            assertTrue("Нет объяснения при ошибке в ${t.id}", t.explanationWrong.isNotBlank())
        }
    }

    @Test
    fun `в заданиях с выбором есть хотя бы один разумный вариант и описаны последствия`() {
        tasks.filterIsInstance<ChoiceTask>().forEach { t ->
            assertTrue("Нет разумного варианта в ${t.id}", t.options.any { it.recommended })
            t.options.forEach { option ->
                assertTrue("Не описано последствие ${option.id} в ${t.id}", option.outcome.isNotBlank())
            }
        }
    }

    @Test
    fun `в заданиях с отметкой верные варианты есть среди предложенных`() {
        tasks.filterIsInstance<PickTask>().forEach { t ->
            val ids = t.options.map { it.id }.toSet()
            assertTrue("Верный вариант отсутствует в списке ${t.id}", ids.containsAll(t.correct))
            assertTrue("Не указан верный вариант в ${t.id}", t.correct.isNotEmpty())
        }
    }

    @Test
    fun `вычисления в числовых заданиях посильны для возраста`() {
        tasks.filterIsInstance<NumberTask>().forEach { t ->
            assertTrue("Слишком большой ответ в ${t.id}: ${t.answer}", t.answer in 0..100)
        }
    }

    @Test
    fun `есть задание восстановления после неудачного периода`() {
        val recovery = tasks.filter { it.topic == TaskTopic.RECOVERY }

        assertTrue("Задание восстановления не найдено", recovery.isNotEmpty())
    }

    // --- Проверка ответов ------------------------------------------------

    @Test
    fun `распределение всей суммы с соблюдением правила засчитывается`() {
        val t = task("plan_split")

        val check = t.check(TaskAnswer.Allocation(needs = 10, wants = 10, savings = 10))

        assertTrue(check.isCorrect)
        assertEquals(t.explanationCorrect, check.explanation)
        assertEquals(IncomeSource.TASK_CORRECT, check.reward)
    }

    @Test
    fun `нехватка на обязательное направление не засчитывается`() {
        val t = task("plan_split")

        val check = t.check(TaskAnswer.Allocation(needs = 5, wants = 15, savings = 10))

        assertFalse(check.isCorrect)
        assertEquals(t.explanationWrong, check.explanation)
        assertEquals(IncomeSource.TASK_PARTIAL, check.reward)
    }

    @Test
    fun `распределена должна быть вся сумма`() {
        val t = task("plan_split")

        val check = t.check(TaskAnswer.Allocation(needs = 10, wants = 5, savings = 5))

        assertFalse(check.isCorrect)
    }

    @Test
    fun `отметка верной покупки засчитывается`() {
        val check = task("plan_vs_fact").check(TaskAnswer.Picked(setOf("tent")))

        assertTrue(check.isCorrect)
    }

    @Test
    fun `лишняя отметка не засчитывается`() {
        val check = task("plan_vs_fact").check(TaskAnswer.Picked(setOf("tent", "ball")))

        assertFalse(check.isCorrect)
    }

    @Test
    fun `верное число засчитывается`() {
        val check = task("save_rate").check(TaskAnswer.Number(15))

        assertTrue(check.isCorrect)
        assertEquals(IncomeSource.TASK_CORRECT, check.reward)
    }

    @Test
    fun `неверное число сопровождается разбором`() {
        val t = task("save_rate")

        val check = t.check(TaskAnswer.Number(20))

        assertFalse(check.isCorrect)
        assertEquals(t.explanationWrong, check.explanation)
        assertEquals(IncomeSource.TASK_PARTIAL, check.reward)
    }

    @Test
    fun `выбор с последствиями возвращает описание последствия`() {
        val check = task("save_temptation").check(TaskAnswer.Chosen("withdraw"))

        assertFalse(check.isCorrect)
        assertNotNull(check.outcome)
        assertTrue(check.outcome!!.isNotBlank())
    }

    @Test
    fun `у задания может быть несколько разумных решений`() {
        val t = task("save_temptation")

        assertTrue(t.check(TaskAnswer.Chosen("wait")).isCorrect)
        assertTrue(t.check(TaskAnswer.Chosen("cheaper")).isCorrect)
    }

    @Test
    fun `задание восстановления даёт соответствующую награду`() {
        val check = task("recover_help").check(TaskAnswer.Chosen("food"))

        assertTrue(check.isCorrect)
        assertEquals(IncomeSource.RECOVERY_TASK, check.reward)
    }

    @Test
    fun `несуществующий вариант выбора отклоняется`() {
        val error = runCatching {
            task("recover_help").check(TaskAnswer.Chosen("unknown"))
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
    }

    @Test
    fun `ответ неподходящего типа отклоняется`() {
        val error = runCatching {
            task("save_rate").check(TaskAnswer.Picked(setOf("food")))
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
    }
}
