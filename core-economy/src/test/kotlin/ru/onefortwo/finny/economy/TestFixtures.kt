package ru.onefortwo.finny.economy

/**
 * Общие данные для тестов. Цены и влияние на показатели совпадают
 * с каталогом из docs/01-проектные-решения.md, чтобы тесты проверяли
 * ту же экономику, которая заложена в учебный контент.
 */
object TestItems {
    val food = ShopItem("food", Coins(10), BudgetCategory.NEEDS, PetStatKind.CARE, 30)
    val water = ShopItem("water", Coins(5), BudgetCategory.NEEDS, PetStatKind.CARE, 15)
    val vitamins = ShopItem("vitamins", Coins(8), BudgetCategory.NEEDS, PetStatKind.CARE, 20)
    val ball = ShopItem("ball", Coins(12), BudgetCategory.WANTS, PetStatKind.JOY, 15)
    val stickers = ShopItem("stickers", Coins(4), BudgetCategory.WANTS, PetStatKind.JOY, 5)
    val tent = ShopItem("tent", Coins(25), BudgetCategory.WANTS, PetStatKind.JOY, 30)

    /** Весь каталог: нужен, чтобы проверять ограниченность ресурсов. */
    val all = listOf(food, water, vitamins, ball, stickers, tent)
}

object TestGoals {
    val scooter = Goal("scooter", Coins(60))
    val party = Goal("party", Coins(120))
}

/** Разворачивает успешный результат покупки или проваливает тест. */
fun PurchaseResult.orFail(): PurchaseResult.Success = when (this) {
    is PurchaseResult.Success -> this
    is PurchaseResult.NotEnoughCoins ->
        throw AssertionError("Ожидалась успешная покупка, не хватило ${shortfall.amount} монет")
}

/** Разворачивает успешное подтверждение плана или проваливает тест. */
fun PlanConfirmation.orFail(): PlanConfirmation.Success = when (this) {
    is PlanConfirmation.Success -> this
    else -> throw AssertionError("Ожидалось подтверждение плана, получено: $this")
}

/** Разворачивает успешное пополнение копилки или проваливает тест. */
fun DepositResult.orFail(): DepositResult.Success = when (this) {
    is DepositResult.Success -> this
    else -> throw AssertionError("Ожидалось пополнение копилки, получено: $this")
}

/** Разворачивает успешное завершение периода или проваливает тест. */
fun PeriodCompletion.orFail(): PeriodCompletion.Success = when (this) {
    is PeriodCompletion.Success -> this
    else -> throw AssertionError("Ожидалось завершение периода, получено: $this")
}
