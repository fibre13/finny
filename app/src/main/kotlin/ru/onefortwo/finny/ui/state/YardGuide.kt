package ru.onefortwo.finny.ui.state

import ru.onefortwo.finny.economy.BudgetCategory

/*
 * ТЕСТ (ветка test/kopilka-a): подсказки первого дня на дворе. Над
 * предметом, к которому пора идти, появляется облачко «Шаг N из 6».
 * Шаг выводится из состояния игры, а не хранится отдельно: подсказка
 * сама переходит дальше, когда ребёнок сделал нужное, и исчезает после
 * первого дня.
 */

/** Куда указывает подсказка. */
enum class GuideTarget { PLAN, SHOP, TASKS, SAVINGS, FINISH }

/** Подсказка: номер шага, предмет и текст. */
data class GuideStep(val number: Int, val target: GuideTarget, val text: String) {
    val total: Int get() = YardGuide.TOTAL
}

object YardGuide {
    const val TOTAL = 6

    /** Обстановка, которую подсказка предлагает купить: домик-палатка. */
    const val HOUSE = "house"

    /**
     * Шаг подсказки для [state] или `null`, если подсказывать нечего.
     *
     * Порядок следует правилам игры: сначала план — период начинается его
     * подтверждением (ТЗ 2.5.5), и покупка до плана не попала бы в сравнение
     * плана с фактом честно. Потом корм, задание, домик, копилка и конец дня.
     * [housePrice] — цена домика из каталога.
     */
    fun step(state: AppState, housePrice: Int?): GuideStep? {
        val game = state.game
        if (state.profile == null || game.period.number != 1 || game.history.isNotEmpty()) return null
        val period = game.period
        val balance = game.balance.amount

        if (!period.isPlanConfirmed) {
            return GuideStep(1, GuideTarget.PLAN, "Начни здесь: составь план на день")
        }
        if (period.purchases.none { it.category == BudgetCategory.NEEDS }) {
            return GuideStep(2, GuideTarget.SHOP, "Пора покормить питомца: купи корм в лавке")
        }
        if (state.completedTaskIds.isEmpty()) {
            return GuideStep(3, GuideTarget.TASKS, "Реши задание — получишь монеты")
        }
        if (housePrice != null && !state.hasScenery(HOUSE)) {
            return if (balance < housePrice) {
                GuideStep(4, GuideTarget.TASKS, "Реши ещё задание: домик стоит ${Explanations.coins(housePrice)}")
            } else {
                GuideStep(4, GuideTarget.SHOP, "Купи домик-палатку: в нём питомец будет спать")
            }
        }
        if (game.savings.saved.amount == 0 && balance > 0) {
            return GuideStep(5, GuideTarget.SAVINGS, "Отложи монеты в сундучок — на мечту")
        }
        if (period.canFinish) {
            return GuideStep(6, GuideTarget.FINISH, "Всё сделано! Нажми «Закончить день»")
        }
        return null
    }
}
