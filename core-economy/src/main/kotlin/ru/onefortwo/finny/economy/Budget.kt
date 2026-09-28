package ru.onefortwo.finny.economy

/**
 * Направления распределения бюджета (ТЗ 2.5.5, минимум три).
 * Названия для ребёнка заданы в [displayName] согласно
 * docs/01-проектные-решения.md, раздел 3.
 */
enum class BudgetCategory(val displayName: String) {
    /** Обязательные расходы: еда, вода, уход. */
    NEEDS("Нужное"),

    /** Необязательные расходы: игрушки, украшения. */
    WANTS("Хочу"),

    /** Накопления на финансовую цель. */
    SAVINGS("Копим на мечту"),
}

/**
 * План личного бюджета на один игровой период.
 * Составляется до начала периода и после подтверждения служит
 * основой для сравнения с фактом (ТЗ 2.5.5).
 */
data class BudgetPlan(
    val needs: Coins,
    val wants: Coins,
    val savings: Coins,
) {
    /** Суммарно распределено по всем направлениям. */
    val total: Coins get() = needs + wants + savings

    operator fun get(category: BudgetCategory): Coins = when (category) {
        BudgetCategory.NEEDS -> needs
        BudgetCategory.WANTS -> wants
        BudgetCategory.SAVINGS -> savings
    }

    companion object {
        val EMPTY = BudgetPlan(Coins.ZERO, Coins.ZERO, Coins.ZERO)
    }
}

/** Результат проверки плана на соответствие доступному бюджету. */
sealed interface PlanValidation {

    /** План допустим; [remainder] — нераспределённый остаток, показывается ребёнку. */
    data class Valid(val remainder: Coins) : PlanValidation

    /** Распределено больше, чем есть; [excess] — на сколько превышен бюджет. */
    data class ExceedsBudget(val excess: Coins) : PlanValidation
}

/**
 * Проверяет, что план не превышает [available], и считает остаток.
 * Приложение обязано контролировать это до подтверждения плана (ТЗ 2.5.5).
 */
fun validatePlan(plan: BudgetPlan, available: Coins): PlanValidation =
    if (plan.total > available) {
        PlanValidation.ExceedsBudget(Coins(plan.total.amount - available.amount))
    } else {
        PlanValidation.Valid(available - plan.total)
    }
