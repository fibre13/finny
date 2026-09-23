package ru.onefortwo.finny.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.onefortwo.finny.economy.Difficulty

/**
 * Модели финансовых заданий (ТЗ 2.5.8).
 *
 * Задание — игровая ситуация с выбором и последствиями. Поддерживаются
 * четыре типа взаимодействия, поэтому задания не сводятся к выбору ответа
 * из готовых вариантов: распределение суммы, отметка в списке, ввод числа
 * и выбор решения с описанными последствиями.
 *
 * Новое задание добавляется записью в `tasks.json` без изменения кода
 * приложения (ТЗ 2.5.14).
 */

/** Тема задания. Обязательный минимум — первые три темы (ТЗ 2.5.8). */
@Serializable
enum class TaskTopic(val displayName: String) {
    @SerialName("budget_planning")
    BUDGET_PLANNING("Планирование бюджета"),

    @SerialName("savings")
    SAVINGS("Формирование сбережений"),

    @SerialName("payments")
    PAYMENTS("Платежи и покупки"),

    @SerialName("recovery")
    RECOVERY("Как всё исправить"),
}

/**
 * Уровень сложности задания (ТЗ 2.5.8: сложность вычислений соответствует
 * возрасту). Привязан к выбранному уровню, а не к возрасту: приложение
 * о возрасте не спрашивает.
 */
@Serializable
enum class TaskLevel {
    /** «Попроще»: счёт в пределах 10–20, без деления. */
    @SerialName("easy")
    EASY,

    /** «Посложнее»: двузначные числа, деление. */
    @SerialName("normal")
    NORMAL,

    /** Задание без вычислений, подходит любому уровню. */
    @SerialName("any")
    ANY;

    /** Подходит ли задание выбранному уровню сложности. */
    fun suits(difficulty: Difficulty): Boolean = when (this) {
        ANY -> true
        EASY -> difficulty == Difficulty.SIMPLE
        NORMAL -> difficulty == Difficulty.HARDER
    }
}

/** Направление плана бюджета в формате учебного контента. */
@Serializable
enum class PlanCategory {
    @SerialName("needs")
    NEEDS,

    @SerialName("wants")
    WANTS,

    @SerialName("savings")
    SAVINGS,
}

/** Общие поля любого задания. */
@Serializable
sealed interface TaskContent {
    val id: String
    val topic: TaskTopic

    /** Для какого класса подходит задание. */
    val level: TaskLevel

    val title: String

    /** Условие задания короткими фразами, посильными для 7–11 лет. */
    val prompt: String

    /** Объяснение при верном решении. */
    val explanationCorrect: String

    /**
     * Объяснение при ошибочном решении. Показывается наравне с верным:
     * ребёнок получает объяснение независимо от правильности (ТЗ 2.5.8).
     */
    val explanationWrong: String

    /**
     * Переменные числа задания либо `null`, если числа постоянные.
     * Подстановка описана в [TaskVariation].
     */
    val vary: TaskVariation?
}

/** Ограничение на сумму по направлению в задании на распределение. */
@Serializable
data class AllocateRule(
    val category: PlanCategory,
    val min: Int = 0,
    val max: Int? = null,
)

/** Распределение заданной суммы по направлениям бюджета. */
@Serializable
@SerialName("allocate")
data class AllocateTask(
    override val id: String,
    override val topic: TaskTopic,
    override val level: TaskLevel = TaskLevel.ANY,
    override val title: String,
    override val prompt: String,
    @SerialName("explanation_correct")
    override val explanationCorrect: String,
    @SerialName("explanation_wrong")
    override val explanationWrong: String,
    override val vary: TaskVariation? = null,
    /** Сумма, которую нужно распределить полностью. */
    val amount: Int,
    val rules: List<AllocateRule> = emptyList(),
) : TaskContent

/** Вариант для отметки в списке. */
@Serializable
data class PickOption(
    val id: String,
    val title: String,
    val price: Int? = null,
)

/** Отметить в списке подходящие элементы. */
@Serializable
@SerialName("pick")
data class PickTask(
    override val id: String,
    override val topic: TaskTopic,
    override val level: TaskLevel = TaskLevel.ANY,
    override val title: String,
    override val prompt: String,
    @SerialName("explanation_correct")
    override val explanationCorrect: String,
    @SerialName("explanation_wrong")
    override val explanationWrong: String,
    override val vary: TaskVariation? = null,
    val options: List<PickOption>,
    val correct: List<String>,
) : TaskContent

/** Ввести число: расчёт суммы, сдачи или срока. */
@Serializable
@SerialName("number")
data class NumberTask(
    override val id: String,
    override val topic: TaskTopic,
    override val level: TaskLevel = TaskLevel.ANY,
    override val title: String,
    override val prompt: String,
    @SerialName("explanation_correct")
    override val explanationCorrect: String,
    @SerialName("explanation_wrong")
    override val explanationWrong: String,
    override val vary: TaskVariation? = null,
    val answer: Int,
    val unit: String = "монет",
) : TaskContent

/** Вариант решения с описанным последствием. */
@Serializable
data class ChoiceOption(
    val id: String,
    val title: String,
    /** Что произойдёт после этого выбора; показывается сразу после ответа. */
    val outcome: String,
    /**
     * Вариант ведёт к разумному результату. Допустимо несколько таких
     * вариантов: у задачи может быть более одного хорошего решения.
     */
    val recommended: Boolean = false,
)

/** Выбор решения в игровой ситуации с последствиями. */
@Serializable
@SerialName("choice")
data class ChoiceTask(
    override val id: String,
    override val topic: TaskTopic,
    override val level: TaskLevel = TaskLevel.ANY,
    override val title: String,
    override val prompt: String,
    @SerialName("explanation_correct")
    override val explanationCorrect: String,
    @SerialName("explanation_wrong")
    override val explanationWrong: String,
    override val vary: TaskVariation? = null,
    val options: List<ChoiceOption>,
) : TaskContent
