package ru.onefortwo.finny.ui.state

/*
 * Словарик — игра с карточками. Каждый прожитый игровой день открывает
 * одну карточку по порядку словаря: за сезон из трёх дней — три слова.
 * Пропущенные календарные дни карточки не сжигают и новых не добавляют:
 * засчитываются только дни, в которые ребёнок играл. Открытая карточка
 * уходит из сетки в список под ней.
 *
 * Над словариком на дворе появляется облачко — не раньше, чем выбрана
 * мечта, разложены монеты и решены два события дня: словарик не главное
 * в игре и не перебивает другие дела. Облачко не показывается в день,
 * когда ребёнок уже заходил в словарик.
 *
 * Слово за действием. Слово словаря считается встреченным, когда ребёнок
 * впервые столкнулся с ним в игре: выбрал цель — «Цель» и «Копилка», утвердил
 * план — «Бюджет» и «План», купил — «Нужное и желаемое» и «Расход».
 *
 * Встреченность и счёт дней выводятся из состояния игры, а не отмечаются
 * в каждом действии: правило одно на всё приложение.
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
     * Состояние с отмеченными новыми словами и засчитанным игровым днём или
     * то же состояние, если отмечать нечего. Слово отмечается встреченным
     * только при первой встрече.
     */
    fun note(state: AppState): AppState {
        var result = state
        val found = encountered(state) - state.knownTerms
        if (found.isNotEmpty()) {
            result = result.copy(knownTerms = state.knownTerms + found, newTerms = state.newTerms + found)
        }
        val day = state.game.period.number
        val x = result.extras
        if (state.profile != null && x.wordDay != day) {
            result = result.copy(extras = x.copy(wordDay = day, wordDays = x.wordDays + 1))
        }
        return result
    }

    /** Облачко над словариком, когда есть карточка, которую можно открыть. */
    const val HINT_NEW = "Есть новое слово! Загляни в словарик."

    /** Облачко над словариком, когда новых карточек нет, а словарик давно не открывали. */
    const val HINT_RECALL = "Загляни в словарик — вспомни, что ты уже знаешь"

    /** Сколько карточек можно открыть всего: по одной за игровой день, не меньше одной. */
    fun availableCount(state: AppState): Int = maxOf(1, state.extras.wordDays)

    /** Термины [terms], карточки которых уже можно открыть, — по порядку словаря. */
    fun available(state: AppState, terms: List<String>): List<String> = terms.take(availableCount(state))

    /** Доступные, но ещё не открытые карточки. */
    fun closed(state: AppState, terms: List<String>): List<String> =
        available(state, terms).filter { it !in state.extras.wordsOpened }

    /**
     * Текст облачка над словариком или `null`. Облачко — только после
     * мечты, плана сезона и двух решённых событий дня и не в день, когда
     * ребёнок уже заходил в словарик. Новое слово важнее; без новых слов
     * облачко напоминает о словарике раз в два игровых дня без захода.
     */
    fun hint(state: AppState, terms: List<String>): String? {
        val x = state.extras
        val day = state.game.period.number
        if (state.profile == null || state.game.savings.goal == null || !x.planned) return null
        if (x.eventsDay != day || x.answered < minOf(2, x.dayEvents.size)) return null
        if (x.wordVisit == day) return null
        if (closed(state, terms).isNotEmpty()) return HINT_NEW
        val since = day - x.wordVisit
        return if (x.wordVisit > 0 && since >= 2 && since % 2 == 0) HINT_RECALL else null
    }
}
