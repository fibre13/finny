package ru.onefortwo.finny.economy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Проверки правил бюджета и монет. Соответствуют требованию ТЗ 3.4
 * о покрытии ключевой логики бюджета автоматизированными тестами.
 */
class BudgetTest {

    @Test
    fun `план в пределах бюджета оставляет остаток`() {
        val plan = BudgetPlan(needs = Coins(20), wants = Coins(10), savings = Coins(10))

        val result = validatePlan(plan, available = Coins(50))

        assertEquals(PlanValidation.Valid(Coins(10)), result)
    }

    @Test
    fun `план ровно на весь бюджет допустим и остаток нулевой`() {
        val plan = BudgetPlan(needs = Coins(30), wants = Coins(10), savings = Coins(10))

        val result = validatePlan(plan, available = Coins(50))

        assertEquals(PlanValidation.Valid(Coins.ZERO), result)
    }

    @Test
    fun `план сверх бюджета сообщает величину превышения`() {
        val plan = BudgetPlan(needs = Coins(40), wants = Coins(20), savings = Coins(10))

        val result = validatePlan(plan, available = Coins(50))

        assertEquals(PlanValidation.ExceedsBudget(Coins(20)), result)
    }

    @Test
    fun `нехватка монет считается как разница до цены`() {
        val balance = Coins(8)

        assertFalse(balance.canAfford(Coins(12)))
        assertEquals(Coins(4), balance.shortfall(Coins(12)))
    }

    @Test
    fun `при достаточном балансе нехватки нет`() {
        val balance = Coins(20)

        assertTrue(balance.canAfford(Coins(12)))
        assertEquals(Coins.ZERO, balance.shortfall(Coins(12)))
    }

    @Test
    fun `отрицательный баланс создать нельзя`() {
        val error = runCatching { Coins(-1) }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
    }
}
