package ru.onefortwo.finny.economy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Накопления, финансовая цель и расчёт срока её достижения (ТЗ 2.5.7).
 */
class SavingsTest {

    private val withGoal = GameState.newProfile().chooseGoal(TestGoals.scooter)

    @Test
    fun `пополнение переносит монеты с баланса в копилку`() {
        val result = withGoal.deposit(Coins(15)).orFail()

        assertEquals(Coins(35), result.state.balance)
        assertEquals(Coins(15), result.state.savings.saved)
        assertEquals(Coins(15), result.savedAfter)
    }

    @Test
    fun `накопленная цель получается и освобождает место следующей`() {
        val saved = withGoal.copy(
            savings = withGoal.savings.copy(saved = Coins(70)),
        )

        val (state, goal) = saved.claimGoal()!!

        assertEquals(TestGoals.scooter.id, goal.id)
        // Стоимость списана, остаток сверх неё сохранён для следующей цели.
        assertEquals(Coins(10), state.savings.saved)
        assertEquals(null, state.savings.goal)
    }

    @Test
    fun `недонакопленную цель получить нельзя`() {
        val saved = withGoal.copy(savings = withGoal.savings.copy(saved = Coins(59)))

        assertEquals(null, saved.claimGoal())
    }

    @Test
    fun `без выбранной цели получать нечего`() {
        assertEquals(null, GameState.newProfile().claimGoal())
    }

    @Test
    fun `остаток до цели уменьшается на сумму пополнения`() {
        val result = withGoal.deposit(Coins(15)).orFail()

        assertEquals(Coins(45), result.state.savings.remaining)
    }

    @Test
    fun `пополнение сверх баланса отклоняется`() {
        val result = withGoal.deposit(Coins(80))

        assertTrue(result is DepositResult.NotEnoughCoins)
        result as DepositResult.NotEnoughCoins
        assertEquals(Coins(30), result.shortfall)
    }

    @Test
    fun `без выбранной цели пополнение недоступно`() {
        val result = GameState.newProfile().deposit(Coins(10))

        assertEquals(DepositResult.NoGoalSelected, result)
    }

    @Test
    fun `без пополнений срок достижения цели не рассчитывается`() {
        assertEquals(GoalForecast.NotEnoughData, withGoal.goalForecast())
    }

    @Test
    fun `без выбранной цели прогноз не строится`() {
        assertEquals(GoalForecast.NoGoal, GameState.newProfile().goalForecast())
    }

    @Test
    fun `срок считается по средней сумме пополнения`() {
        // Цель 60, накоплено 15, осталось 45 при среднем пополнении 15 за период.
        val state = withGoal.deposit(Coins(15)).orFail().state

        assertEquals(GoalForecast.Periods(3), state.goalForecast())
    }

    @Test
    fun `неполный период округляется в большую сторону`() {
        // Цель 60, накоплено 10, осталось 50 при среднем пополнении 15: нужно 4 периода.
        val state = withGoal.deposit(Coins(10)).orFail().state
            .let { it.copy(savings = it.savings.copy(depositsByPeriod = listOf(Coins(20)))) }

        // Средняя по двум значениям: (20 + 10) / 2 = 15, остаток 50 -> 4 периода.
        assertEquals(GoalForecast.Periods(4), state.goalForecast())
    }

    @Test
    fun `усреднение учитывает только последние девять периодов — три сезона`() {
        val state = withGoal.copy(
            savings = withGoal.savings.copy(
                saved = Coins(30),
                // Первое крупное пополнение должно выпасть из окна усреднения.
                depositsByPeriod = listOf(Coins(90)) + List(9) { Coins(10) },
            ),
        )

        // Осталось 30 при среднем 10 за последние девять периодов.
        assertEquals(GoalForecast.Periods(3), state.goalForecast())
    }

    @Test
    fun `достигнутая цель отражается в прогнозе`() {
        val state = withGoal.copy(savings = withGoal.savings.copy(saved = Coins(60)))

        assertEquals(GoalForecast.Reached, state.goalForecast())
        assertEquals(Coins.ZERO, state.savings.remaining)
    }

    @Test
    fun `снятие показывает новую сумму и новый срок до подтверждения`() {
        val state = withGoal.deposit(Coins(30)).orFail().state

        val preview = state.previewWithdrawal(Coins(20))

        assertEquals(Coins(30), preview.savedBefore)
        assertEquals(Coins(10), preview.savedAfter)
        // Было: осталось 30 при среднем 30 -> 1 период. Станет: осталось 50 -> 2 периода.
        assertEquals(GoalForecast.Periods(1), preview.forecastBefore)
        assertEquals(GoalForecast.Periods(2), preview.forecastAfter)
    }

    @Test
    fun `предварительный показ снятия не меняет состояние`() {
        val state = withGoal.deposit(Coins(30)).orFail().state

        state.previewWithdrawal(Coins(20))

        assertEquals(Coins(30), state.savings.saved)
        assertEquals(Coins(20), state.balance)
    }

    @Test
    fun `снятие возвращает монеты на баланс`() {
        val state = withGoal.deposit(Coins(30)).orFail().state

        val result = state.withdraw(Coins(20))

        assertTrue(result is WithdrawalResult.Success)
        result as WithdrawalResult.Success
        assertEquals(Coins(40), result.state.balance)
        assertEquals(Coins(10), result.state.savings.saved)
    }

    @Test
    fun `снятие сверх накопленного отклоняется`() {
        val state = withGoal.deposit(Coins(10)).orFail().state

        val result = state.withdraw(Coins(25))

        assertTrue(result is WithdrawalResult.NotEnoughSavings)
        result as WithdrawalResult.NotEnoughSavings
        assertEquals(Coins(10), result.saved)
    }
}
