package ru.onefortwo.finny.content

/**
 * Порядок выдачи заданий (ТЗ 2.5.8: «Порядок открытия заданий команда
 * определяет самостоятельно»).
 *
 * Правила:
 *
 * 1. Новые задания идут по кругу тем: планирование бюджета, сбережения,
 *    платежи, затем время и безопасность, если на уровне есть задания этих
 *    тем, и снова с первой темы. Так за первые три задания ребёнок
 *    встречает все три обязательные темы, а не проходит сначала все задания
 *    одной темы подряд.
 * 2. Задание восстановления «Помоги своему питомцу» в общую очередь не входит.
 *    Его сюжет — питомец грустит — уместен после неудачного дня, и туда его
 *    ведёт кнопка на экране итогов. В списке оно доступно отдельным разделом
 *    всегда, как и остальные задания (ТЗ 2.5.8: все задания доступны сразу).
 * 3. Решённым считается задание, на которое уже отвечали, при любом ответе:
 *    повтор приносит половину награды.
 *
 * Функции чистые: зависят только от переданного набора и множества решённых,
 * поэтому одинаково работают на главном экране, в списке и после ответа.
 */
object TaskQueue {

    /** Задания общей очереди в порядке выдачи: по кругу тем. */
    fun ordered(tasks: List<TaskContent>): List<TaskContent> {
        val byTopic = regularTopics.map { topic -> tasks.filter { it.topic == topic } }
        val rounds = byTopic.maxOfOrNull { it.size } ?: 0
        return (0 until rounds).flatMap { round -> byTopic.mapNotNull { it.getOrNull(round) } }
    }

    /** Следующее новое задание либо `null`, если новых не осталось. */
    fun next(tasks: List<TaskContent>, solved: Set<String>): TaskContent? =
        ordered(tasks).firstOrNull { it.id !in solved }

    /**
     * Задание, которое предлагается сразу после ответа: следующее новое,
     * кроме только что решённого. Не предлагается, если [allowed] ложно —
     * так навигация передаёт, что экранное время на сегодня вышло и
     * подталкивать продолжать не следует.
     */
    fun nextAfterAnswer(
        tasks: List<TaskContent>,
        solved: Set<String>,
        answeredId: String,
        allowed: Boolean,
    ): TaskContent? = if (allowed) next(tasks, solved + answeredId) else null

    /**
     * Задание, которое предлагается решить ещё раз, когда новых не осталось.
     * Меняется с каждым игровым днём, чтобы повтор не сводился к одному
     * и тому же заданию.
     */
    fun repeatSuggestion(tasks: List<TaskContent>, periodNumber: Int): TaskContent? {
        val queue = ordered(tasks)
        if (queue.isEmpty()) return null
        return queue[(periodNumber - 1).mod(queue.size)]
    }

    /** Разделы списка заданий. */
    data class Sections(
        /** Ещё не решённые задания общей очереди, в порядке выдачи. */
        val fresh: List<TaskContent>,
        /** Решённые задания общей очереди, в том же порядке. */
        val solved: List<TaskContent>,
        /** Задания восстановления: доступны всегда, отдельным разделом. */
        val recovery: List<TaskContent>,
    )

    fun sections(tasks: List<TaskContent>, solved: Set<String>): Sections {
        val queue = ordered(tasks)
        return Sections(
            fresh = queue.filter { it.id !in solved },
            solved = queue.filter { it.id in solved },
            recovery = tasks.filter { it.topic == TaskTopic.RECOVERY },
        )
    }

    /** Темы общей очереди: все, кроме восстановления, в порядке объявления. */
    private val regularTopics = TaskTopic.entries.filter { it != TaskTopic.RECOVERY }
}
