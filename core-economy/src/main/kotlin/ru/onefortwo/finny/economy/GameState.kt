package ru.onefortwo.finny.economy

/**
 * Полное состояние игровой экономики одного профиля.
 *
 * Тип неизменяемый: каждая операция возвращает новое состояние вместе с
 * описанием того, что изменилось. Это делает правила проверяемыми модульными
 * тестами и позволяет интерфейсу объяснить причину любого изменения (ТЗ 2.5.9).
 */
data class GameState(
    val balance: Coins = Coins.ZERO,
    val savings: SavingsState = SavingsState(),
    val pet: PetState = PetState.INITIAL,
    val growthPoints: Int = 0,
    val period: PeriodState = PeriodState(number = 1),
    val history: List<PeriodOutcome> = emptyList(),
) {
    /** Текущая стадия развития питомца; определяется накопленными очками. */
    val stage: GrowthStage get() = GrowthStage.forPoints(growthPoints)

    /** Прогноз срока достижения цели с учётом пополнений текущего периода. */
    fun goalForecast(): GoalForecast = savings.forecast(period.depositedToSavings)

    companion object {
        /**
         * Состояние нового профиля: начислен стартовый бюджет,
         * идёт первый игровой период, план ещё не составлен.
         */
        fun newProfile(): GameState = GameState(balance = IncomeSource.START_BUDGET.amount)
    }
}

// --- Доход ---------------------------------------------------------------

/**
 * Начисляет игровую валюту. Каждое начисление сопровождается источником
 * и суммой, баланс не меняется без объяснения (ТЗ 2.5.4).
 *
 * Сумму можно задать меньше обычной для источника: повторное прохождение
 * задания приносит половину награды. Источник при этом тот же — ребёнок
 * видит, за что начислено.
 */
fun GameState.earn(
    source: IncomeSource,
    amount: Coins = source.amount,
): Pair<GameState, IncomeEvent> {
    val newBalance = balance + amount
    val event = IncomeEvent(source = source, amount = amount, balanceAfter = newBalance)
    return copy(balance = newBalance) to event
}

// --- План бюджета --------------------------------------------------------

/** Результат подтверждения плана бюджета. */
sealed interface PlanConfirmation {

    /**
     * План принят. Сумма направления «Копилка» переведена в накопления
     * сразу при подтверждении.
     */
    data class Success(val state: GameState, val movedToSavings: Coins) : PlanConfirmation

    /** Распределено больше, чем есть монет. */
    data class ExceedsBudget(val excess: Coins) : PlanConfirmation

    /** План уже подтверждён; до подтверждения его можно менять свободно. */
    data object AlreadyConfirmed : PlanConfirmation

    /** Направлять в копилку нечего: цель не выбрана. */
    data object NoGoalSelected : PlanConfirmation
}

/**
 * Подтверждает план текущего периода. До подтверждения план изменяется
 * без ограничений, после — служит основой для сравнения с фактом (ТЗ 2.5.5).
 */
fun GameState.confirmPlan(plan: BudgetPlan): PlanConfirmation {
    if (period.isPlanConfirmed) return PlanConfirmation.AlreadyConfirmed

    when (val validation = validatePlan(plan, balance)) {
        is PlanValidation.ExceedsBudget -> return PlanConfirmation.ExceedsBudget(validation.excess)
        is PlanValidation.Valid -> Unit
    }

    if (plan.savings > Coins.ZERO && savings.goal == null) {
        return PlanConfirmation.NoGoalSelected
    }

    val confirmed = copy(
        balance = balance - plan.savings,
        savings = savings.copy(saved = savings.saved + plan.savings),
        period = period.copy(
            plan = plan,
            depositedToSavings = period.depositedToSavings + plan.savings,
        ),
    )
    return PlanConfirmation.Success(confirmed, plan.savings)
}

// --- Покупки -------------------------------------------------------------

/**
 * Выполняет покупку. Отрицательный баланс и покупка при недостатке средств
 * не допускаются (ТЗ 2.5.6).
 *
 * Превышение плана по направлению покупку не блокирует: это допустимая
 * ошибка, которая стоит очка роста по итогам периода, но не наказывается
 * запретом действия.
 */
fun GameState.buy(item: ShopItem): PurchaseResult {
    if (!balance.canAfford(item.price)) {
        return PurchaseResult.NotEnoughCoins(
            price = item.price,
            balance = balance,
            shortfall = balance.shortfall(item.price),
        )
    }

    val record = PurchaseRecord(itemId = item.id, price = item.price, category = item.category)
    val statBefore = pet[item.stat]
    val newPet = pet.changed(item.stat, item.statDelta)
    val newPeriod = period.copy(purchases = period.purchases + record)

    val planned = period.plan?.get(item.category)
    val exceedsPlan = planned != null && newPeriod.spent(item.category) > planned

    return PurchaseResult.Success(
        state = copy(balance = balance - item.price, pet = newPet, period = newPeriod),
        record = record,
        stat = item.stat,
        statBefore = statBefore,
        statAfter = newPet[item.stat],
        exceedsPlan = exceedsPlan,
    )
}

// --- Накопления и цель ---------------------------------------------------

/** Выбирает финансовую цель. Ранее накопленная сумма сохраняется. */
fun GameState.chooseGoal(goal: Goal): GameState = copy(savings = savings.copy(goal = goal))

/**
 * Получить накопленную цель: её стоимость списывается из копилки, а место
 * текущей цели освобождается, чтобы выбрать следующую (ТЗ 2.5.7).
 *
 * Без этого накопленная цель оставалась навсегда: копилка продолжала
 * расти, новую цель выбрать было нельзя, и экран становился тупиковым.
 * Остаток сверх стоимости сохраняется — он пойдёт на следующую цель.
 *
 * Возвращает `null`, если цель не выбрана или ещё не накоплена.
 */
fun GameState.claimGoal(): Pair<GameState, Goal>? {
    val goal = savings.goal ?: return null
    if (savings.saved < goal.price) return null

    val claimed = copy(
        savings = savings.copy(saved = savings.saved - goal.price, goal = null),
    )
    return claimed to goal
}

/** Переводит монеты с баланса в копилку. */
fun GameState.deposit(amount: Coins): DepositResult {
    if (savings.goal == null) return DepositResult.NoGoalSelected
    if (!balance.canAfford(amount)) {
        return DepositResult.NotEnoughCoins(
            requested = amount,
            balance = balance,
            shortfall = balance.shortfall(amount),
        )
    }

    val savedAfter = savings.saved + amount
    val state = copy(
        balance = balance - amount,
        savings = savings.copy(saved = savedAfter),
        period = period.copy(depositedToSavings = period.depositedToSavings + amount),
    )
    return DepositResult.Success(state, savedAfter)
}

/**
 * Показывает последствия снятия до его подтверждения: новую сумму копилки
 * и новый срок достижения цели (ТЗ 2.5.7).
 */
fun GameState.previewWithdrawal(amount: Coins): WithdrawalPreview {
    val savedAfter = if (savings.saved.canAfford(amount)) savings.saved - amount else Coins.ZERO
    val after = savings.copy(saved = savedAfter)
    return WithdrawalPreview(
        amount = amount,
        savedBefore = savings.saved,
        savedAfter = savedAfter,
        forecastBefore = goalForecast(),
        forecastAfter = after.forecast(period.depositedToSavings),
    )
}

/**
 * Снимает монеты с копилки. Вызывается только после отдельного подтверждения
 * пользователя, которому предварительно показан [previewWithdrawal] (ТЗ 2.5.7).
 */
fun GameState.withdraw(amount: Coins): WithdrawalResult {
    if (!savings.saved.canAfford(amount)) {
        return WithdrawalResult.NotEnoughSavings(requested = amount, saved = savings.saved)
    }

    val savedAfter = savings.saved - amount
    val depositedAfter =
        if (period.depositedToSavings.canAfford(amount)) period.depositedToSavings - amount
        else Coins.ZERO

    val state = copy(
        balance = balance + amount,
        savings = savings.copy(saved = savedAfter),
        period = period.copy(depositedToSavings = depositedAfter),
    )
    return WithdrawalResult.Success(state, savedAfter)
}

// --- Завершение игрового периода -----------------------------------------

/**
 * Завершает игровой период: начисляет очки роста, применяет естественное
 * снижение показателей, открывает следующий период и начисляет карманные
 * монеты.
 *
 * Стадия развития пересчитывается по накопленным очкам и не понижается,
 * поскольку очки только накапливаются (ТЗ 2.5.10).
 */
/**
 * [payIncome] — начислить карманные на следующий день сразу. Если `false`,
 * они начисляются позже через [startNewDay]: в обычном режиме — при первом
 * входе в новые календарные сутки.
 */
fun GameState.finishPeriod(payIncome: Boolean = true): PeriodCompletion {
    val plan = period.plan ?: return PeriodCompletion.PlanNotConfirmed
    if (!period.canFinish) return PeriodCompletion.NoDecision

    val spentNeeds = period.spent(BudgetCategory.NEEDS)
    val spentWants = period.spent(BudgetCategory.WANTS)

    val conditions = buildSet {
        if (spentNeeds > Coins.ZERO) add(GrowthCondition.NEEDS_COVERED)
        if (spentNeeds <= plan.needs && spentWants <= plan.wants) add(GrowthCondition.WITHIN_PLAN)
        if (period.depositedToSavings > Coins.ZERO) add(GrowthCondition.SAVED_SOMETHING)
    }

    val petAfter = pet.afterPeriodDecay(stage)
    val pointsAfter = growthPoints + conditions.size

    val afterDecay = copy(
        pet = petAfter,
        growthPoints = pointsAfter,
        savings = savings.copy(
            depositsByPeriod = savings.depositsByPeriod + period.depositedToSavings,
        ),
        period = PeriodState(number = period.number + 1),
    )

    val (withIncome, incomeEvent) = if (payIncome) {
        afterDecay.earn(IncomeSource.POCKET_MONEY)
    } else {
        afterDecay to null
    }

    val outcome = PeriodOutcome(
        number = period.number,
        plan = plan,
        spentNeeds = spentNeeds,
        spentWants = spentWants,
        deposited = period.depositedToSavings,
        metConditions = conditions,
        petBefore = pet,
        petAfter = petAfter,
        pointsBefore = growthPoints,
        pointsAfter = pointsAfter,
        stageBefore = stage,
        stageAfter = GrowthStage.forPoints(pointsAfter),
        nextPeriodIncome = incomeEvent,
    )

    return PeriodCompletion.Success(
        state = withIncome.copy(history = history + outcome),
        outcome = outcome,
    )
}

/** Начисляет карманные на новый день, если при закрытии прошлого они отложены. */
fun GameState.startNewDay(): Pair<GameState, IncomeEvent> = earn(IncomeSource.POCKET_MONEY)
