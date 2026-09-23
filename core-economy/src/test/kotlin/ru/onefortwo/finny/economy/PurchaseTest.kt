package ru.onefortwo.finny.economy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Правила покупок и списания средств (ТЗ 2.5.6, 3.4).
 */
class PurchaseTest {

    private val state = GameState.newProfile()

    @Test
    fun `покупка уменьшает баланс на цену товара`() {
        val result = state.buy(TestItems.food).orFail()

        assertEquals(Coins(40), result.state.balance)
    }

    @Test
    fun `покупка повышает связанный показатель питомца`() {
        val result = state.buy(TestItems.food).orFail()

        assertEquals(PetStatKind.CARE, result.stat)
        assertEquals(60, result.statBefore.value)
        assertEquals(90, result.statAfter.value)
        assertEquals(90, result.state.pet.care.value)
    }

    @Test
    fun `необязательная покупка влияет на радость, а не на заботу`() {
        val result = state.buy(TestItems.ball).orFail()

        assertEquals(PetStatKind.JOY, result.stat)
        assertEquals(75, result.state.pet.joy.value)
        assertEquals(60, result.state.pet.care.value)
    }

    @Test
    fun `покупка сохраняется в истории текущего периода`() {
        val result = state.buy(TestItems.food).orFail()

        assertEquals(1, result.state.period.purchases.size)
        assertEquals("food", result.state.period.purchases.single().itemId)
        assertEquals(Coins(10), result.state.period.spent(BudgetCategory.NEEDS))
    }

    @Test
    fun `при нехватке монет покупка отклоняется с указанием недостающей суммы`() {
        val poor = state.copy(balance = Coins(8))

        val result = poor.buy(TestItems.ball)

        assertTrue(result is PurchaseResult.NotEnoughCoins)
        result as PurchaseResult.NotEnoughCoins
        assertEquals(Coins(12), result.price)
        assertEquals(Coins(8), result.balance)
        assertEquals(Coins(4), result.shortfall)
    }

    @Test
    fun `отклонённая покупка не меняет баланс и состояние питомца`() {
        val poor = state.copy(balance = Coins(8))

        poor.buy(TestItems.ball)

        assertEquals(Coins(8), poor.balance)
        assertEquals(60, poor.pet.joy.value)
        assertTrue(poor.period.purchases.isEmpty())
    }

    @Test
    fun `покупка ровно на весь остаток допустима и обнуляет баланс`() {
        val exact = state.copy(balance = Coins(12))

        val result = exact.buy(TestItems.ball).orFail()

        assertEquals(Coins.ZERO, result.state.balance)
    }

    @Test
    fun `превышение плана не блокирует покупку, но отмечается`() {
        val planned = state.confirmPlan(
            BudgetPlan(needs = Coins(10), wants = Coins(5), savings = Coins.ZERO),
        ).orFail().state

        val result = planned.buy(TestItems.ball).orFail()

        assertTrue(result.exceedsPlan)
        assertEquals(Coins(38), result.state.balance)
    }

    @Test
    fun `покупка в пределах плана не отмечается как превышение`() {
        val planned = state.confirmPlan(
            BudgetPlan(needs = Coins(20), wants = Coins(15), savings = Coins.ZERO),
        ).orFail().state

        val result = planned.buy(TestItems.ball).orFail()

        assertFalse(result.exceedsPlan)
    }

    @Test
    fun `показатель питомца не превышает верхнюю границу шкалы`() {
        val afterFirst = state.buy(TestItems.food).orFail().state

        val afterSecond = afterFirst.buy(TestItems.food).orFail()

        assertEquals(100, afterSecond.state.pet.care.value)
    }

    @Test
    fun `накопления не относятся к направлениям покупок`() {
        val error = runCatching {
            ShopItem("wrong", Coins(5), BudgetCategory.SAVINGS, PetStatKind.CARE, 10)
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
    }
}
