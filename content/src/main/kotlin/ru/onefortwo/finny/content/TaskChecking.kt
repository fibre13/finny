package ru.onefortwo.finny.content

import ru.onefortwo.finny.economy.IncomeSource

/**
 * Проверка ответов на задания. Правило проверки задаётся данными задания,
 * поэтому добавление нового задания не требует изменений в коде (ТЗ 2.5.14).
 */

/** Ответ пользователя. Тип ответа соответствует типу задания. */
sealed interface TaskAnswer {

    /** Распределение суммы по трём направлениям. */
    data class Allocation(val needs: Int, val wants: Int, val savings: Int) : TaskAnswer {
        val total: Int get() = needs + wants + savings

        operator fun get(category: PlanCategory): Int = when (category) {
            PlanCategory.NEEDS -> needs
            PlanCategory.WANTS -> wants
            PlanCategory.SAVINGS -> savings
        }
    }

    /** Отмеченные в списке элементы. */
    data class Picked(val ids: Set<String>) : TaskAnswer

    /** Введённое число. */
    data class Number(val value: Int) : TaskAnswer

    /** Выбранный вариант решения. */
    data class Chosen(val optionId: String) : TaskAnswer
}

/**
 * Результат проверки. Объяснение выдаётся всегда, независимо от
 * правильности решения (ТЗ 2.5.8).
 */
data class TaskCheck(
    val isCorrect: Boolean,
    val explanation: String,
    /** Последствие выбранного варианта; заполняется для заданий с выбором. */
    val outcome: String? = null,
    /** Источник награды за выполненное задание. */
    val reward: IncomeSource,
)

/**
 * Проверяет ответ и формирует объяснение.
 *
 * @throws IllegalArgumentException если тип ответа не соответствует типу задания.
 */
fun TaskContent.check(answer: TaskAnswer): TaskCheck = when (this) {
    is AllocateTask -> checkAllocate(answer.require<TaskAnswer.Allocation>(id))
    is PickTask -> checkPick(answer.require<TaskAnswer.Picked>(id))
    is NumberTask -> checkNumber(answer.require<TaskAnswer.Number>(id))
    is ChoiceTask -> checkChoice(answer.require<TaskAnswer.Chosen>(id))
}

private inline fun <reified T : TaskAnswer> TaskAnswer.require(taskId: String): T =
    this as? T
        ?: throw IllegalArgumentException(
            "Ответ типа ${this::class.simpleName} не подходит заданию $taskId",
        )

private fun AllocateTask.checkAllocate(answer: TaskAnswer.Allocation): TaskCheck {
    val distributedFully = answer.total == amount
    val noNegative = answer.needs >= 0 && answer.wants >= 0 && answer.savings >= 0
    val rulesMet = rules.all { rule ->
        val value = answer[rule.category]
        value >= rule.min && (rule.max == null || value <= rule.max)
    }

    return result(distributedFully && noNegative && rulesMet)
}

private fun PickTask.checkPick(answer: TaskAnswer.Picked): TaskCheck =
    result(answer.ids == correct.toSet())

private fun NumberTask.checkNumber(answer: TaskAnswer.Number): TaskCheck =
    result(answer.value == this.answer)

private fun ChoiceTask.checkChoice(answer: TaskAnswer.Chosen): TaskCheck {
    val option = options.firstOrNull { it.id == answer.optionId }
        ?: throw IllegalArgumentException(
            "В задании $id нет варианта ${answer.optionId}",
        )

    return result(option.recommended, outcome = option.outcome)
}

/** Собирает результат проверки с объяснением и наградой. */
private fun TaskContent.result(isCorrect: Boolean, outcome: String? = null): TaskCheck = TaskCheck(
    isCorrect = isCorrect,
    explanation = if (isCorrect) explanationCorrect else explanationWrong,
    outcome = outcome,
    reward = when {
        !isCorrect -> IncomeSource.TASK_PARTIAL
        topic == TaskTopic.RECOVERY -> IncomeSource.RECOVERY_TASK
        else -> IncomeSource.TASK_CORRECT
    },
)
