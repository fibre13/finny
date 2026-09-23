package ru.onefortwo.finny.economy

/**
 * Позиция каталога покупок в доменном виде. Заполняется из учебного контента,
 * сам контент модулю экономики не известен (ТЗ 3.4: разделение компонентов).
 */
data class ShopItem(
    val id: String,
    val price: Coins,
    val category: BudgetCategory,
    val stat: PetStatKind,
    val statDelta: Int,
) {
    init {
        require(category != BudgetCategory.SAVINGS) {
            "Покупка относится к расходам, направление «${BudgetCategory.SAVINGS.displayName}» недопустимо"
        }
        require(statDelta > 0) { "Покупка не может ухудшать состояние питомца: $statDelta" }
    }
}

/** Запись о покупке в истории текущего игрового периода (ТЗ 2.5.6). */
data class PurchaseRecord(
    val itemId: String,
    val price: Coins,
    val category: BudgetCategory,
)

/** Результат попытки покупки. */
sealed interface PurchaseResult {

    /** Покупка выполнена: баланс уменьшен, показатель питомца изменён. */
    data class Success(
        val state: GameState,
        val record: PurchaseRecord,
        val stat: PetStatKind,
        val statBefore: StatValue,
        val statAfter: StatValue,
        /** Превышен ли план по направлению после этой покупки. */
        val exceedsPlan: Boolean,
    ) : PurchaseResult

    /**
     * Монет не хватает. Отрицательный баланс не допускается; приложение
     * объясняет, чего не хватает, и предлагает варианты (ТЗ 2.5.6).
     */
    data class NotEnoughCoins(
        val price: Coins,
        val balance: Coins,
        val shortfall: Coins,
    ) : PurchaseResult
}
