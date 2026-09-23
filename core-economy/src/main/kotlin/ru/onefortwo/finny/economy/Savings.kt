package ru.onefortwo.finny.economy

/** Финансовая цель: предмет с понятной стоимостью (ТЗ 2.5.7). */
data class Goal(
    val id: String,
    val price: Coins,
) {
    init {
        require(price > Coins.ZERO) { "Стоимость цели должна быть положительной" }
    }
}

/**
 * Накопления: отложенная часть валюты, учитываемая отдельно от баланса.
 * [depositsByPeriod] хранит суммы пополнений за завершённые периоды и
 * используется для расчёта срока достижения цели.
 */
data class SavingsState(
    val saved: Coins = Coins.ZERO,
    val goal: Goal? = null,
    val depositsByPeriod: List<Coins> = emptyList(),
) {
    /** Сколько ещё нужно накопить до выбранной цели. */
    val remaining: Coins
        get() = goal?.let { if (saved >= it.price) Coins.ZERO else it.price - saved } ?: Coins.ZERO
}

/**
 * Прогноз срока достижения цели. Расчёт основан на средней сумме
 * регулярного пополнения и должен быть понятным ребёнку (ТЗ 2.5.7).
 */
sealed interface GoalForecast {

    /** Цель не выбрана, прогноз не строится. */
    data object NoGoal : GoalForecast

    /** Нужная сумма уже накоплена. */
    data object Reached : GoalForecast

    /** Пополнений ещё не было, средней суммы для расчёта нет. */
    data object NotEnoughData : GoalForecast

    /** До цели осталось [periods] игровых периодов при текущем темпе. */
    data class Periods(val periods: Int) : GoalForecast
}

/**
 * Считает срок достижения цели по средней сумме пополнения за последние
 * [WINDOW] периодов, включая незавершённый текущий период.
 *
 * Используется деление с округлением вверх: если остатка хватает не на целое
 * число периодов, нужен ещё один.
 */
fun SavingsState.forecast(currentPeriodDeposit: Coins = Coins.ZERO): GoalForecast {
    val goal = goal ?: return GoalForecast.NoGoal
    if (saved >= goal.price) return GoalForecast.Reached

    val series = buildList {
        addAll(depositsByPeriod)
        if (currentPeriodDeposit > Coins.ZERO) add(currentPeriodDeposit)
    }.takeLast(WINDOW)

    val total = series.sumOf { it.amount.toLong() }
    if (total <= 0L) return GoalForecast.NotEnoughData

    // periods = ceil(остаток / среднее) = ceil(остаток * количество / сумма)
    val numerator = remaining.amount.toLong() * series.size
    val periods = (numerator + total - 1) / total
    return GoalForecast.Periods(periods.toInt())
}

/** Ширина окна усреднения пополнений: последние три периода. */
private const val WINDOW = 3

/** Результат пополнения копилки. */
sealed interface DepositResult {

    data class Success(val state: GameState, val savedAfter: Coins) : DepositResult

    /** На балансе недостаточно монет для перевода. */
    data class NotEnoughCoins(val requested: Coins, val balance: Coins, val shortfall: Coins) :
        DepositResult

    /** Цель не выбрана: копить не на что. */
    data object NoGoalSelected : DepositResult
}

/**
 * Предварительный показ последствий снятия. До подтверждения пользователь
 * видит, как уменьшится накопленная сумма и как изменится срок (ТЗ 2.5.7).
 */
data class WithdrawalPreview(
    val amount: Coins,
    val savedBefore: Coins,
    val savedAfter: Coins,
    val forecastBefore: GoalForecast,
    val forecastAfter: GoalForecast,
)

/** Результат снятия монет с копилки. */
sealed interface WithdrawalResult {

    data class Success(val state: GameState, val savedAfter: Coins) : WithdrawalResult

    /** В копилке меньше запрошенной суммы. */
    data class NotEnoughSavings(val requested: Coins, val saved: Coins) : WithdrawalResult
}
