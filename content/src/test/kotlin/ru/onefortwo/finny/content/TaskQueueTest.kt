package ru.onefortwo.finny.content

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.onefortwo.finny.economy.Difficulty

/**
 * Порядок выдачи заданий на фактическом контенте: новые идут по кругу
 * тем, решённые уходят из очереди, задание восстановления в общую очередь
 * не входит, но в списке остаётся доступным.
 */
class TaskQueueTest {

    private val all = ContentParser.parseTasks(File("src/main/assets/tasks.json").readText())
    private val simple = all.filter { it.level.suits(Difficulty.SIMPLE) }

    @Test
    fun `новые задания идут по кругу тем`() {
        val topics = TaskQueue.ordered(simple).map { it.topic }

        // Первые три задания — по одному на каждую обязательную тему.
        assertEquals(
            listOf(TaskTopic.BUDGET_PLANNING, TaskTopic.SAVINGS, TaskTopic.PAYMENTS),
            topics.take(3),
        )
        assertEquals(topics.take(3), topics.drop(3).take(3))
    }

    @Test
    fun `задание восстановления в общую очередь не входит`() {
        val queue = TaskQueue.ordered(simple)

        assertTrue(queue.none { it.topic == TaskTopic.RECOVERY })
        // Все остальные задания уровня в очереди есть, без потерь и повторов.
        assertEquals(simple.count { it.topic != TaskTopic.RECOVERY }, queue.size)
        assertEquals(queue.size, queue.map { it.id }.toSet().size)
    }

    @Test
    fun `следующим предлагается первое нерешённое`() {
        val queue = TaskQueue.ordered(simple)

        assertEquals(queue[0].id, TaskQueue.next(simple, emptySet())?.id)
        assertEquals(queue[1].id, TaskQueue.next(simple, setOf(queue[0].id))?.id)
        // Решённое в середине пропускается, порядок остальных не меняется.
        assertEquals(queue[0].id, TaskQueue.next(simple, setOf(queue[1].id))?.id)
    }

    @Test
    fun `когда все решены, новых нет, а повтор меняется день ото дня`() {
        val queue = TaskQueue.ordered(simple)
        val solved = queue.map { it.id }.toSet()

        assertNull(TaskQueue.next(simple, solved))
        assertEquals(queue[0].id, TaskQueue.repeatSuggestion(simple, periodNumber = 1)?.id)
        assertEquals(queue[1].id, TaskQueue.repeatSuggestion(simple, periodNumber = 2)?.id)
        assertEquals(queue[0].id, TaskQueue.repeatSuggestion(simple, periodNumber = queue.size + 1)?.id)
    }

    @Test
    fun `после ответа предлагается следующее новое, а не только что решённое`() {
        val queue = TaskQueue.ordered(simple)

        // Решённое ещё может не попасть в множество к моменту вычисления:
        // оно исключается по идентификатору ответа.
        assertEquals(
            queue[1].id,
            TaskQueue.nextAfterAnswer(simple, emptySet(), queue[0].id, allowed = true)?.id,
        )
        // Ответ на задание восстановления не сбивает очередь.
        assertEquals(
            queue[0].id,
            TaskQueue.nextAfterAnswer(simple, emptySet(), "recover_help", allowed = true)?.id,
        )
    }

    @Test
    fun `когда время вышло, следующее задание не предлагается`() {
        assertNull(TaskQueue.nextAfterAnswer(simple, emptySet(), "recover_help", allowed = false))
    }

    @Test
    fun `после ответа на последнее новое задание предлагать нечего`() {
        val queue = TaskQueue.ordered(simple)
        val allButLast = queue.dropLast(1).map { it.id }.toSet()

        assertNull(TaskQueue.nextAfterAnswer(simple, allButLast, queue.last().id, allowed = true))
    }

    @Test
    fun `решённое задание восстановления на очередь не влияет`() {
        val queue = TaskQueue.ordered(simple)

        assertEquals(queue[0].id, TaskQueue.next(simple, setOf("recover_help"))?.id)
    }

    @Test
    fun `разделы списка делят задания без пересечений и потерь`() {
        val queue = TaskQueue.ordered(simple)
        val solvedIds = setOf(queue[0].id, queue[2].id, "recover_help")

        val sections = TaskQueue.sections(simple, solvedIds)

        assertEquals(queue.filter { it.id !in solvedIds }.map { it.id }, sections.fresh.map { it.id })
        assertEquals(listOf(queue[0].id, queue[2].id), sections.solved.map { it.id })
        assertEquals(listOf("recover_help"), sections.recovery.map { it.id })
        assertEquals(
            simple.map { it.id }.toSet(),
            (sections.fresh + sections.solved + sections.recovery).map { it.id }.toSet(),
        )
    }

    @Test
    fun `на обоих уровнях порядок одинаково устроен`() {
        Difficulty.entries.forEach { difficulty ->
            val level = all.filter { it.level.suits(difficulty) }
            val topics = TaskQueue.ordered(level).map { it.topic }
            assertEquals(
                "Уровень «${difficulty.displayName}»",
                listOf(TaskTopic.BUDGET_PLANNING, TaskTopic.SAVINGS, TaskTopic.PAYMENTS),
                topics.take(3),
            )
        }
    }
}
