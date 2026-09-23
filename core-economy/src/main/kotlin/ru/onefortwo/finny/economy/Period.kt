package ru.onefortwo.finny.economy

/**
 * Состояние текущего игрового периода: подтверждённый план и факт
 * его исполнения. План составляется до начала периода, после подтверждения
 * служит основой для сравнения с фактом (ТЗ 2.5.5).
 */
data class PeriodState(
    val number: Int,
    val plan: BudgetPlan? = null,
    val purchases: List<PurchaseRecord> = emptyList(),
    val depositedToSavings: Coins = Coins.ZERO,
) {
    init {
        require(number >= 1) { "Номер игрового периода начинается с единицы: $number" }
    }

    /** Фактические расходы по направлению за период. */
    fun spent(category: BudgetCategory): Coins =
        purchases.filter { it.category == category }
            .fold(Coins.ZERO) { sum, record -> sum + record.price }

    /** План подтверждён и период можно вести. */
    val isPlanConfirmed: Boolean get() = plan != null

    /**
     * Период можно завершить: план подтверждён и в нём есть решение —
     * покупка либо отложенные в копилку монеты.
     *
     * Раньше требовалась именно покупка, и ребёнок, отложивший все монеты
     * в копилку, завершить день не мог. Это противоречит тому, чему учит
     * приложение: отложить — такое же решение, как потратить.
     */
    val canFinish: Boolean
        get() = isPlanConfirmed &&
            (purchases.isNotEmpty() || depositedToSavings > Coins.ZERO)
}

/** Условие начисления очка роста по итогам периода. */
enum class GrowthCondition {
    /** За период куплена хотя бы одна обязательная позиция. */
    NEEDS_COVERED,

    /** Фактические расходы не превысили план ни по одному направлению. */
    WITHIN_PLAN,

    /** В копилку направлена ненулевая сумма. */
    SAVED_SOMETHING,
}

/**
 * Итог игрового периода. Содержит всё, что нужно показать ребёнку:
 * сравнение плана с фактом, изменение показателей и причину изменения
 * стадии развития (ТЗ 2.5.9, 2.5.10).
 */
data class PeriodOutcome(
    val number: Int,
    val plan: BudgetPlan,
    val spentNeeds: Coins,
    val spentWants: Coins,
    val deposited: Coins,
    val metConditions: Set<GrowthCondition>,
    val petBefore: PetState,
    val petAfter: PetState,
    val pointsBefore: Int,
    val pointsAfter: Int,
    val stageBefore: GrowthStage,
    val stageAfter: GrowthStage,
    /** Начисление в начале следующего периода; null для последнего шага. */
    val nextPeriodIncome: IncomeEvent?,
) {
    /** Сколько очков роста начислено за период: от 0 до 3. */
    val earnedPoints: Int get() = metConditions.size

    /** Стадия развития повысилась по итогам периода. */
    val stageAdvanced: Boolean get() = stageAfter != stageBefore

    /**
     * Период неудачный: очков не начислено либо «Забота» опустилась
     * до низкого уровня. Это не обнуляет прогресс, а открывает понятный
     * путь восстановления (ТЗ 2.2, 2.5.9).
     */
    val isSetback: Boolean
        get() = earnedPoints == 0 || petAfter.care.level == StatLevel.LOW

    /** Фактические расходы по направлению. */
    fun spent(category: BudgetCategory): Coins = when (category) {
        BudgetCategory.NEEDS -> spentNeeds
        BudgetCategory.WANTS -> spentWants
        BudgetCategory.SAVINGS -> deposited
    }

    /**
     * Отклонение факта от плана по направлению: положительное значение —
     * перерасход, отрицательное — экономия.
     */
    fun deviation(category: BudgetCategory): Int =
        spent(category).amount - plan[category].amount
}

/** Результат попытки завершить игровой период. */
sealed interface PeriodCompletion {

    data class Success(val state: GameState, val outcome: PeriodOutcome) : PeriodCompletion

    /** План текущего периода ещё не подтверждён. */
    data object PlanNotConfirmed : PeriodCompletion

    /**
     * За период не принято ни одного решения: ни покупки, ни отложенных
     * в копилку монет.
     */
    data object NoDecision : PeriodCompletion
}
