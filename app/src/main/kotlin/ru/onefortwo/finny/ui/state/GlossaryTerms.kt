package ru.onefortwo.finny.ui.state

/*
 * Слово за действием. Слово словарика считается встреченным, когда ребёнок
 * впервые столкнулся с ним в игре: выбрал цель — «Цель» и «Копилка», утвердил
 * план — «Бюджет» и «План», купил — «Нужное и желаемое» и «Расход».
 * Встреченное слово до прочтения отмечено в словарике «Новое», и двор
 * напоминает о нём (см. [Reminder.WORD]).
 *
 * Словарик при этом открыт целиком: все слова читаются с первого дня, это
 * справочный раздел (ТЗ 2.5.11). Отметка «Новое» только подсказывает, какое
 * слово связано с тем, что ребёнок сейчас сделал.
 *
 * Встреченность выводится из состояния игры, а не отмечается в каждом
 * действии: правило одно на всё приложение, и новое действие не забудет
 * отметить слово.
 */
object GlossaryTerms {
    /** Задания, в которых встречается слово «Сдача». */
    private val CHANGE_TASKS = setOf("change_count", "change_visual")

    /** Задание о запасе на непредвиденный случай. */
    private const val RAINY_DAY_TASK = "rainy_day"

    /** Слова, которые по состоянию [state] ребёнок уже встретил. */
    fun encountered(state: AppState): Set<String> {
        if (state.profile == null) return emptySet()
        val game = state.game
        val period = game.period
        val dayLived = game.history.isNotEmpty()

        return buildSet {
            if (game.savings.goal != null || state.achievedGoalIds.isNotEmpty()) {
                add("Цель")
                add("Копилка")
            }
            if (state.extras.planned || period.isPlanConfirmed || dayLived) {
                add("Бюджет")
                add("План")
            }
            if (game.savings.saved.amount > 0) add("Накопления")
            if (period.purchases.isNotEmpty() || dayLived) {
                add("Нужное и желаемое")
                add("Расход")
            }
            // Доход — награда за задание или карманные монеты нового дня.
            if (state.completedTaskIds.isNotEmpty() || dayLived) add("Доход")
            // План и факт сравниваются на экране итогов закрытого дня.
            if (dayLived) add("Факт")
            if (RAINY_DAY_TASK in state.completedTaskIds) add("Подушка безопасности")
            if (state.completedTaskIds.any { it in CHANGE_TASKS }) add("Сдача")
        }
    }

    /**
     * Состояние с отмеченными новыми словами или то же состояние, если
     * новых встреч нет. Слово отмечается «Новое» только при первой встрече:
     * прочитанное слово второй раз новым не становится.
     */
    fun note(state: AppState): AppState {
        val found = encountered(state) - state.knownTerms
        if (found.isEmpty()) return state
        return state.copy(knownTerms = state.knownTerms + found, newTerms = state.newTerms + found)
    }
}
