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
    BUDGET_PLANNING("Планирование"),

    @SerialName("savings")
    SAVINGS("Накопления"),

    @SerialName("payments")
    PAYMENTS("Покупки"),

    /** Задания на время — «деньги и время связаны». */
    @SerialName("time")
    TIME("Время"),

    /** Финансовая безопасность: пароли, личные данные, уловки обманщиков. */
    @SerialName("safety")
    SAFETY("Безопасность"),

    @SerialName("recovery")
    RECOVERY("Помощь питомцу"),
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

    /** Реплика питомца в облачке после верного ответа. */
    val petCorrect: String?

    /** Реплика питомца после ошибки — поддержка, без упрёка. */
    val petWrong: String?

    /** Значок задания в списке — код спрайта из `art/pixel`. */
    val icon: String?

    /** Игровое событие над условием: «{name} проголодался! …». */
    val context: String?

    /** Короткая подсказка под вариантами: «Сравни цену с планом». */
    val hint: String?
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
    @SerialName("pet_correct")
    override val petCorrect: String? = null,
    @SerialName("pet_wrong")
    override val petWrong: String? = null,
    override val icon: String? = null,
    override val context: String? = null,
    override val hint: String? = null,
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
    /** Картинка карточки — код спрайта. */
    val icon: String? = null,
    /** Пометка под названием: «нужное», «хочу». */
    val note: String? = null,
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
    @SerialName("pet_correct")
    override val petCorrect: String? = null,
    @SerialName("pet_wrong")
    override val petWrong: String? = null,
    override val icon: String? = null,
    override val context: String? = null,
    override val hint: String? = null,
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
    @SerialName("pet_correct")
    override val petCorrect: String? = null,
    @SerialName("pet_wrong")
    override val petWrong: String? = null,
    override val icon: String? = null,
    override val context: String? = null,
    override val hint: String? = null,
    val answer: Int,
    val unit: String = "монет",
    /** Циферблат — иллюстрация или способ ответа. */
    val clock: ClockSpec? = null,
    /** Монеты рисунком: сколько дал и сколько стоит покупка. */
    val coins: CoinsSpec? = null,
) : TaskContent

/**
 * Циферблат в задании на время. [from] — сколько сейчас, [to] —
 * до какого часа выделить отрезок. Если заданы [choices], ответ выбирается
 * нажатием на один из этих часов, остальные часы не нажимаются.
 */
@Serializable
data class ClockSpec(
    val from: Int,
    val to: Int? = null,
    val choices: List<Int> = emptyList(),
)

/** Монеты рисунком в задании на сдачу. */
@Serializable
data class CoinsSpec(
    val paid: Int,
    val price: Int,
)

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
    /** Картинка карточки — код спрайта. */
    val icon: String? = null,
    /** Пометка под названием: «нужное», «хочу». */
    val note: String? = null,
    /** Реплика питомца именно на этот выбор. */
    val pet: String? = null,
    /** Картинка рядом с питомцем в ответе: полная или пустая миска, мечта. */
    @SerialName("pet_icon")
    val petIcon: String? = null,
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
    @SerialName("pet_correct")
    override val petCorrect: String? = null,
    @SerialName("pet_wrong")
    override val petWrong: String? = null,
    override val icon: String? = null,
    override val context: String? = null,
    override val hint: String? = null,
    val options: List<ChoiceOption>,
) : TaskContent
