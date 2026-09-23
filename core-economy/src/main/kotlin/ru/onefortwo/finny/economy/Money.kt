package ru.onefortwo.finny.economy

/**
 * Игровая валюта — монеты. Только целые неотрицательные значения:
 * дробных монет в интерфейсе для детей 7–11 лет не предусмотрено,
 * отрицательный баланс запрещён требованием ТЗ 2.5.6.
 */
@JvmInline
value class Coins(val amount: Int) : Comparable<Coins> {

    init {
        require(amount >= 0) { "Количество монет не может быть отрицательным: $amount" }
    }

    operator fun plus(other: Coins): Coins = Coins(amount + other.amount)

    /**
     * Вычитание, недопускающее отрицательного результата.
     * @throws IllegalArgumentException если результат отрицателен; вызывающий код
     * обязан заранее проверить достаточность средств через [canAfford].
     */
    operator fun minus(other: Coins): Coins = Coins(amount - other.amount)

    operator fun times(factor: Int): Coins = Coins(amount * factor)

    /** Половина суммы с округлением вниз: монета неделима. */
    fun half(): Coins = Coins(amount / 2)

    override fun compareTo(other: Coins): Int = amount.compareTo(other.amount)

    override fun toString(): String = "$amount монет"

    companion object {
        val ZERO = Coins(0)
    }
}

/** Хватает ли [this] монет на сумму [price]. */
fun Coins.canAfford(price: Coins): Boolean = this >= price

/** Сколько монет не хватает до [price]; ноль, если хватает. */
fun Coins.shortfall(price: Coins): Coins =
    if (canAfford(price)) Coins.ZERO else Coins(price.amount - amount)
