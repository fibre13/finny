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
    /** Есть монеты вне банков — подарок или снятое из копилки. */
    FREE("free", GuideTarget.PLAN, "Есть свободные монеты. Разложи их по банкам!", "В план"),
    TASK("task", GuideTarget.TASKS, "Реши задание — до сюрприза в лавке ближе!", "К заданию"),
    WORD("word", GuideTarget.GLOSSARY, "Есть новое слово. Загляни в словарик!", "В словарик"),
    /** Раз в сезон после дел дня. */
    PLAN_FIX("plan_fix", GuideTarget.PLAN, "Хочешь скорректировать план?", "В план"),
    FINISH("finish", GuideTarget.FINISH, "Все дела сделаны! Поиграй со мной или уложи спать.", null),
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
        if (state.profile == null || state.arrival != null || state.isSleeping(today) || state.extras.seasonDone) return null
        // Мечта → план сезона → события дня → напоминания.
        return when {
            game.savings.goal == null ->
                GuideStep(0, GuideTarget.SAVINGS, "Выбери мечту — на что будем копить?")
            !state.extras.planned ->
                GuideStep(0, GuideTarget.PLAN, "Новый сезон: разложи ${Explanations.coins(game.balance)} по банкам")
            !state.dayEventsDone -> null
            else -> reminder(state, today)?.let { GuideStep(0, it.target, it.text, it) }
        }
    }

    /**
     * Первое неотложенное напоминание по порядку дня или `null`. Про задание
     * не напоминается, когда экранное время на сегодня вышло: приложение не
     * подталкивает продолжать сверх предела.
     */
    fun reminder(state: AppState, today: String): Reminder? {
        val x = state.extras
        return Reminder.entries.firstOrNull { reminder ->
            !state.isReminderDismissed(reminder) && when (reminder) {
                Reminder.NEEDS -> false
                Reminder.FREE -> state.freeCoins > 0
                Reminder.TASK -> (x.tasksDay != state.game.period.number || x.tasksToday == 0) && !state.isTimeUp(today)
                Reminder.WORD -> state.newTerms.isNotEmpty()
                Reminder.PLAN_FIX -> !x.correctionAsked && state.dayEventsDone
                Reminder.FINISH -> state.dayEventsDone
            }
        }
    }
}
