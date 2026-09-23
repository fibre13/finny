package ru.onefortwo.finny.economy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Подтверждение плана, итоги игрового периода и начисление очков роста
 * (ТЗ 2.5.5, 2.5.10).
 */
class PeriodTest {

    private val base = GameState.newProfile().chooseGoal(TestGoals.scooter)

    private fun GameState.withPlan(needs: Int, wants: Int, savings: Int): GameState =
        confirmPlan(BudgetPlan(Coins(needs), Coins(wants), Coins(savings))).orFail().state

    @Test
    fun `подтверждение плана сразу переводит сумму в копилку`() {
        val result = base.confirmPlan(BudgetPlan(Coins(20), Coins(15), Coins(10))).orFail()

        assertEquals(Coins(10), result.movedToSavings)
        assertEquals(Coins(10), result.state.savings.saved)
        assertEquals(Coins(40), result.state.balance)
    }

    @Test
    fun `план сверх баланса не подтверждается`() {
        val result = base.confirmPlan(BudgetPlan(Coins(40), Coins(20), Coins(10)))

        assertTrue(result is PlanConfirmation.ExceedsBudget)
        assertEquals(Coins(20), (result as PlanConfirmation.ExceedsBudget).excess)
    }

    @Test
    fun `повторное подтверждение плана отклоняется`() {
        val planned = base.withPlan(needs = 20, wants = 10, savings = 10)

        val result = planned.confirmPlan(BudgetPlan(Coins(30), Coins(5), Coins(5)))

        assertEquals(PlanConfirmation.AlreadyConfirmed, result)
    }

    @Test
    fun `направить в копилку без выбранной цели нельзя`() {
        val result = GameState.newProfile()
            .confirmPlan(BudgetPlan(Coins(20), Coins(10), Coins(10)))

        assertEquals(PlanConfirmation.NoGoalSelected, result)
    }

    @Test
    fun `период без подтверждённого плана не завершается`() {
        assertEquals(PeriodCompletion.PlanNotConfirmed, base.finishPeriod())
    }

    @Test
    fun `период без покупок и без отложенного не завершается`() {
        val planned = base.withPlan(needs = 20, wants = 10, savings = 0)

        assertEquals(PeriodCompletion.NoDecision, planned.finishPeriod())
    }

    @Test
    fun `период только с отложенными монетами завершается`() {
        // Отложить — такое же решение, как потратить: ребёнок, который
        // весь день копил и ничего не купил, обязан иметь возможность
        // закончить день.
        val planned = base.withPlan(needs = 20, wants = 10, savings = 10)

        assertTrue(planned.period.canFinish)
        assertTrue(planned.finishPeriod() is PeriodCompletion.Success)
    }

    @Test
    fun `выполнение всех трёх условий даёт три очка роста`() {
        val state = base.withPlan(needs = 20, wants = 15, savings = 10)
            .buy(TestItems.food).orFail().state
            .buy(TestItems.ball).orFail().state

        val outcome = state.finishPeriod().orFail().outcome

        assertEquals(3, outcome.earnedPoints)
        assertEquals(
            setOf(
                GrowthCondition.NEEDS_COVERED,
                GrowthCondition.WITHIN_PLAN,
                GrowthCondition.SAVED_SOMETHING,
            ),
            outcome.metConditions,
        )
        assertFalse(outcome.isSetback)
    }

    @Test
    fun `без обязательной покупки очко за обязательные расходы не начисляется`() {
        val state = base.withPlan(needs = 20, wants = 15, savings = 10)
            .buy(TestItems.ball).orFail().state

        val outcome = state.finishPeriod().orFail().outcome

        assertFalse(outcome.metConditions.contains(GrowthCondition.NEEDS_COVERED))
        assertEquals(2, outcome.earnedPoints)
    }

    @Test
    fun `перерасход по направлению лишает очка за соответствие плану`() {
        val state = base.withPlan(needs = 20, wants = 5, savings = 10)
            .buy(TestItems.food).orFail().state
            .buy(TestItems.ball).orFail().state

        val outcome = state.finishPeriod().orFail().outcome

        assertFalse(outcome.metConditions.contains(GrowthCondition.WITHIN_PLAN))
        assertEquals(12, outcome.spentWants.amount)
        assertEquals(7, outcome.deviation(BudgetCategory.WANTS))
    }

    @Test
    fun `без пополнения копилки очко за накопления не начисляется`() {
        val state = base.withPlan(needs = 20, wants = 15, savings = 0)
            .buy(TestItems.food).orFail().state

        val outcome = state.finishPeriod().orFail().outcome

        assertFalse(outcome.metConditions.contains(GrowthCondition.SAVED_SOMETHING))
        assertEquals(2, outcome.earnedPoints)
    }

    @Test
    fun `по итогам периода показатели снижаются на естественную потребность`() {
        val state = base.withPlan(needs = 20, wants = 15, savings = 10)
            .buy(TestItems.food).orFail().state

        val outcome = state.finishPeriod().orFail().outcome

        // Забота: 60 + 30 за корм - 20 за период. Радость: 60 - 10 за период.
        assertEquals(70, outcome.petAfter.care.value)
        assertEquals(50, outcome.petAfter.joy.value)
    }

    @Test
    fun `следующий период открывается с карманными монетами`() {
        val state = base.withPlan(needs = 20, wants = 15, savings = 10)
            .buy(TestItems.food).orFail().state

        val completion = state.finishPeriod().orFail()

        assertEquals(2, completion.state.period.number)
        assertEquals(IncomeSource.POCKET_MONEY, completion.outcome.nextPeriodIncome?.source)
        // План списывает только сумму копилки, остальное расходуется по факту:
        // 50 - 10 в копилку - 10 за корм + 25 карманных монет.
        assertEquals(Coins(55), completion.state.balance)
    }

    @Test
    fun `новый период начинается без плана и без покупок`() {
        val state = base.withPlan(needs = 20, wants = 15, savings = 10)
            .buy(TestItems.food).orFail().state

        val next = state.finishPeriod().orFail().state

        assertFalse(next.period.isPlanConfirmed)
        assertTrue(next.period.purchases.isEmpty())
        assertEquals(Coins.ZERO, next.period.depositedToSavings)
    }

    @Test
    fun `итог периода попадает в историю`() {
        val state = base.withPlan(needs = 20, wants = 15, savings = 10)
            .buy(TestItems.food).orFail().state

        val next = state.finishPeriod().orFail().state

        assertEquals(1, next.history.size)
        assertEquals(1, next.history.single().number)
    }

    @Test
    fun `период без очков признаётся неудачным`() {
        // План нулевой по расходам: любая покупка выходит за план,
        // обязательных покупок нет, в копилку не отложено.
        val state = base.withPlan(needs = 0, wants = 0, savings = 0)
            .buy(TestItems.ball).orFail().state

        val outcome = state.finishPeriod().orFail().outcome

        assertEquals(0, outcome.earnedPoints)
        assertTrue(outcome.isSetback)
    }

    @Test
    fun `низкая забота признаётся неудачным периодом даже при начисленных очках`() {
        val hungry = base.copy(pet = PetState(care = StatValue.of(20), joy = StatValue.of(60)))
            .withPlan(needs = 20, wants = 15, savings = 10)
            .buy(TestItems.water).orFail().state

        val outcome = hungry.finishPeriod().orFail().outcome

        // Забота: 20 + 15 за воду - 20 за период = 15, это низкий уровень.
        assertEquals(15, outcome.petAfter.care.value)
        assertEquals(StatLevel.LOW, outcome.petAfter.care.level)
        assertTrue(outcome.earnedPoints > 0)
        assertTrue(outcome.isSetback)
    }

    @Test
    fun `неудачный период не обнуляет накопленные очки роста`() {
        val experienced = base.copy(growthPoints = 6)
            .withPlan(needs = 0, wants = 0, savings = 0)
            .buy(TestItems.ball).orFail().state

        val completion = experienced.finishPeriod().orFail()

        assertEquals(6, completion.state.growthPoints)
        assertEquals(GrowthStage.TEEN, completion.state.stage)
        assertEquals(GrowthStage.TEEN, completion.outcome.stageAfter)
    }

    @Test
    fun `стадия развития повышается по накоплению очков`() {
        val almost = base.copy(growthPoints = 4)
            .withPlan(needs = 20, wants = 15, savings = 10)
            .buy(TestItems.food).orFail().state
            .buy(TestItems.ball).orFail().state

        val outcome = almost.finishPeriod().orFail().outcome

        assertEquals(GrowthStage.BABY, outcome.stageBefore)
        assertEquals(GrowthStage.TEEN, outcome.stageAfter)
        assertTrue(outcome.stageAdvanced)
    }
}
