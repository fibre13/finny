package ru.onefortwo.finny.ui.state

import ru.onefortwo.finny.economy.BudgetCategory

/*
 * Одна цель на экране. Над предметом, к
 * которому пора идти, появляется облачко; остальные предметы спокойны.
 * Шаг выводится из состояния игры, а не хранится отдельно: подсказка сама
 * переходит дальше, когда ребёнок сделал нужное.
 *
 * Первый день ведётся по шагам «Шаг N из 6» в порядке Приложения А ТЗ:
 * мечта → план → нужное → задание → радость → конец дня. Со второго дня
 * выделяется доска плана, пока план не составлен, а после плана — по одному
 * напоминанию о деле, которое ребёнок мог забыть (см. [Reminder]).
 */

/** Куда указывает подсказка. */
enum class GuideTarget { PLAN, SHOP, TASKS, SAVINGS, GLOSSARY, FINISH }

/**
 * Напоминание двора со второго дня. Порядок — порядок игрового дня:
 * нужное, задание, новое слово, конец дня. Каждое откладывается кнопкой
 * «Не сейчас» до конца игрового дня: напоминание зовёт, но не давит
 * (ТЗ 3.5). Системных уведомлений нет — только на дворе, когда ребёнок
 * сам открыл приложение.
 */
enum class Reminder(
    val id: String,
    val target: GuideTarget,
    val text: String,
    /** Кнопка действия в облачке; у конца дня её нет — день закрывает кнопка внизу. */
    val action: String?,
) {
    NEEDS("needs", GuideTarget.SHOP, "Я проголодался. Купишь нужное в лавке?", "В лавку"),
    TASK("task", GuideTarget.TASKS, "Стало скучно. Разгадаешь загадку?", "К заданию"),
    WORD("word", GuideTarget.GLOSSARY, "Есть новое слово. Загляни в словарик!", "В словарик"),
    FINISH("finish", GuideTarget.FINISH, "Все дела готовы. Можно заканчивать день!", null),
}

/**
 * Подсказка: номер шага (0 — без номера, со второго дня), предмет и текст.
 * У напоминания есть [reminder]: его можно отложить «Не сейчас».
 */
data class GuideStep(
    val number: Int,
    val target: GuideTarget,
    val text: String,
    val reminder: Reminder? = null,
) {
    val total: Int get() = YardGuide.TOTAL
}

object YardGuide {
    const val TOTAL = 6

    /**
     * Шаг подсказки для [state] на дату [today] или `null`, если выделять
     * нечего: пока открыто окно «Пришли монеты», пока питомец спит и когда
     * всё на день сделано.
     *
     * Мечта — раньше плана: без цели монеты в копилку не отложить, и ребёнок
     * сначала узнаёт, ради чего экономить (Прил. А, шаг 4). План — раньше
     * покупок: распределение делается до начала периода (ТЗ 2.5.5).
     */
    fun step(state: AppState, today: String): GuideStep? {
        val game = state.game
        if (state.profile == null || state.arrival != null || state.isSleeping(today)) return null
        val period = game.period

        val firstDay = period.number == 1 && game.history.isEmpty()
        if (!firstDay) {
            return if (!period.isPlanConfirmed) {
                GuideStep(0, GuideTarget.PLAN, "Новый день: раздели монеты в «Плане»")
            } else {
                reminder(state, today)?.let { GuideStep(0, it.target, it.text, it) }
            }
        }

        return when {
            game.savings.goal == null ->
                GuideStep(1, GuideTarget.SAVINGS, "Выбери мечту — на что будем копить?")
            !period.isPlanConfirmed ->
                GuideStep(2, GuideTarget.PLAN, "Раздели монеты: на нужное, на радость и в копилку")
            period.purchases.none { it.category == BudgetCategory.NEEDS } ->
                GuideStep(3, GuideTarget.SHOP, "Пора покормить питомца: купи корм в лавке")
            state.completedTaskIds.isEmpty() ->
                GuideStep(4, GuideTarget.TASKS, "Реши задание — заработаешь монеты")
            period.purchases.none { it.category == BudgetCategory.WANTS } ->
                GuideStep(5, GuideTarget.SHOP, "Купи что-нибудь для радости")
            period.canFinish ->
                GuideStep(6, GuideTarget.FINISH, "Всё сделано! Нажми «Закончить день»")
            else -> null
        }
    }

    /**
     * Первое неотложенное напоминание по порядку дня или `null`. Про задание
     * не напоминается, когда экранное время на сегодня вышло: приложение не
     * подталкивает продолжать сверх предела.
     */
    fun reminder(state: AppState, today: String): Reminder? {
        val period = state.game.period
        return Reminder.entries.firstOrNull { reminder ->
            !state.isReminderDismissed(reminder) && when (reminder) {
                Reminder.NEEDS -> period.purchases.none { it.category == BudgetCategory.NEEDS }
                Reminder.TASK -> state.paidTasksToday == 0 && !state.isTimeUp(today)
                Reminder.WORD -> state.newTerms.isNotEmpty()
                Reminder.FINISH -> period.canFinish
            }
        }
    }
}
