package ru.onefortwo.finny.economy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Сквозные проверки игрового цикла. Подтверждают, что заложенные числа
 * позволяют пройти обязательный сценарий: пять последовательных периодов,
 * три стадии развития и достижение финансовой цели (ТЗ 2.6, Приложение А).
 */
class GameFlowTest {

    /**
     * Разумный план периода: обязательное, желаемое и накопления.
     *
     * Сумма укладывается в карманные монеты периода — 25. Ребёнок,
     * который держит питомца довольным, откладывает меньше, чем тот,
     * кто пропускает желаемое: в этом и состоит выбор.
     */
    private val plan = BudgetPlan(needs = Coins(10), wants = Coins(12), savings = Coins(3))

    /**
     * План того, кто копит быстрее: желаемое пропускается, освободившиеся
     * монеты уходят в копилку. Та же сумма 25, другое распределение.
     */
    private val savingPlan = BudgetPlan(needs = Coins(10), wants = Coins.ZERO, savings = Coins(15))

    /** Один период с продуманными решениями: корм, мячик, пополнение копилки. */
    private fun GameState.playCarefulPeriod(): PeriodCompletion.Success =
        confirmPlan(plan).orFail().state
            .buy(TestItems.food).orFail().state
            .buy(TestItems.ball).orFail().state
            .finishPeriod().orFail()

    /** Один период без желаемого: копилка пополняется быстрее. */
    private fun GameState.playSavingPeriod(): PeriodCompletion.Success =
        confirmPlan(savingPlan).orFail().state
            .buy(TestItems.food).orFail().state
            .finishPeriod().orFail()

    @Test
    fun `пять периодов проходятся подряд и доводят питомца до третьей стадии`() {
        var state = GameState.newProfile().chooseGoal(TestGoals.scooter)
        val stages = mutableListOf<GrowthStage>()

        repeat(5) {
            val completion = state.playCarefulPeriod()
            stages += completion.outcome.stageAfter
            state = completion.state
        }

        assertEquals(5, state.history.size)
        assertEquals(6, state.period.number)
        assertEquals(15, state.growthPoints)
        assertEquals(
            listOf(
                GrowthStage.BABY,
                GrowthStage.TEEN,
                GrowthStage.TEEN,
                GrowthStage.ADULT,
                GrowthStage.ADULT,
            ),
            stages,
        )
        assertEquals(GrowthStage.ADULT, state.stage)
    }

    @Test
    fun `баланс остаётся неотрицательным на всём сценарии`() {
        var state = GameState.newProfile().chooseGoal(TestGoals.scooter)

        repeat(5) {
            state = state.playCarefulPeriod().state
            assertTrue("Баланс ушёл в минус", state.balance.amount >= 0)
        }
    }

    @Test
    fun `регулярные накопления приводят к достижению цели`() {
        var state = GameState.newProfile().chooseGoal(TestGoals.scooter)

        // Ребёнок отказывается от желаемого ради цели: по 15 монет за период.
        repeat(3) { state = state.playSavingPeriod().state }

        // За три периода отложено 45 из 60, срок при темпе 15 — один период.
        assertEquals(Coins(45), state.savings.saved)
        assertEquals(GoalForecast.Periods(1), state.goalForecast())

        // Четвёртый период закрывает цель.
        state = state.confirmPlan(savingPlan).orFail().state
            .buy(TestItems.food).orFail().state

        assertEquals(Coins(60), state.savings.saved)
        assertEquals(GoalForecast.Reached, state.goalForecast())
        assertEquals(Coins.ZERO, state.savings.remaining)
    }

    @Test
    fun `состояние питомца остаётся благополучным при регулярной заботе`() {
        var state = GameState.newProfile().chooseGoal(TestGoals.scooter)

        repeat(5) { state = state.playCarefulPeriod().state }

        assertEquals(StatLevel.HIGH, state.pet.care.level)
        assertEquals(StatLevel.HIGH, state.pet.joy.level)
    }

    @Test
    fun `неудачный период не обнуляет прогресс и допускает восстановление`() {
        var state = GameState.newProfile().chooseGoal(TestGoals.scooter)

        // Два продуманных периода дают запас очков и стадию «Подросток».
        repeat(2) { state = state.playCarefulPeriod().state }
        val pointsBefore = state.growthPoints
        assertEquals(GrowthStage.TEEN, state.stage)

        // Третий период: только необязательная покупка сверх нулевого плана.
        val setback = state.confirmPlan(BudgetPlan(Coins.ZERO, Coins.ZERO, Coins.ZERO)).orFail().state
            .buy(TestItems.tent).orFail().state
            .finishPeriod().orFail()

        assertTrue(setback.outcome.isSetback)
        assertEquals(0, setback.outcome.earnedPoints)
        assertEquals(pointsBefore, setback.state.growthPoints)
        assertEquals(GrowthStage.TEEN, setback.state.stage)

        // Восстановление: задание приносит монеты, следующий период снова успешен.
        val (recovered, income) = setback.state.earn(IncomeSource.RECOVERY_TASK)
        assertEquals(IncomeSource.RECOVERY_TASK, income.source)

        val next = recovered.playCarefulPeriod()
        assertFalse(next.outcome.isSetback)
        assertEquals(pointsBefore + 3, next.state.growthPoints)
    }

    @Test
    fun `нехватка монет объясняется и не мешает продолжить период`() {
        val state = GameState.newProfile().chooseGoal(TestGoals.scooter)
            .confirmPlan(plan).orFail().state
            .buy(TestItems.food).orFail().state

        // На балансе 37 монет: палатка за 25 доступна, после неё останется 12.
        val afterTent = state.buy(TestItems.tent).orFail().state
        assertEquals(Coins(12), afterTent.balance)

        // Вторая палатка уже не по карману, и приложение называет нехватку.
        val rejected = afterTent.buy(TestItems.tent)

        assertTrue(rejected is PurchaseResult.NotEnoughCoins)
        rejected as PurchaseResult.NotEnoughCoins
        assertEquals(Coins(13), rejected.shortfall)

        // Более дешёвая позиция остаётся доступной: у ребёнка есть выход.
        val cheaper = afterTent.buy(TestItems.stickers).orFail()
        assertEquals(Coins(8), cheaper.state.balance)
    }

    @Test
    fun `каждое начисление сопровождается источником и суммой`() {
        val (state, event) = GameState.newProfile().earn(IncomeSource.TASK_CORRECT)

        assertEquals(IncomeSource.TASK_CORRECT, event.source)
        assertEquals(Coins(10), event.amount)
        assertEquals(Coins(60), event.balanceAfter)
        assertEquals(Coins(60), state.balance)
    }

    @Test
    fun `накопленный запас не убирает необходимость выбора`() {
        var state = GameState.newProfile().chooseGoal(TestGoals.scooter)

        repeat(5) { state = state.playCarefulPeriod().state }

        // ТЗ 2.1: за один игровой период нельзя купить всё сразу. Если
        // свободных монет хватает на весь каталог, выбирать между
        // обязательным, желаемым и накоплениями уже не из чего.
        val catalogue = TestItems.all.sumOf { it.price.amount }
        assertTrue(
            "Свободных монет ${state.balance.amount} при каталоге $catalogue: выбор исчез",
            state.balance.amount < catalogue,
        )
    }

    @Test
    fun `потребности растут вместе с питомцем`() {
        val baby = PetState(StatValue.of(100), StatValue.of(100)).afterPeriodDecay(GrowthStage.BABY)
        val adult = PetState(StatValue.of(100), StatValue.of(100)).afterPeriodDecay(GrowthStage.ADULT)

        assertTrue(
            "Взрослому питомцу уход обязан обходиться дороже",
            adult.care.value < baby.care.value && adult.joy.value < baby.joy.value,
        )
    }

    @Test
    fun `история хранит сравнение плана с фактом по каждому периоду`() {
        // Отдельный план с запасом по обязательному: так в истории видно
        // отклонение факта от плана, ради которого сравнение и заводится.
        val generous = BudgetPlan(needs = Coins(15), wants = Coins(12), savings = Coins(3))
        val state = GameState.newProfile().chooseGoal(TestGoals.scooter)
            .confirmPlan(generous).orFail().state
            .buy(TestItems.food).orFail().state
            .buy(TestItems.ball).orFail().state
            .finishPeriod().orFail().state

        val outcome = state.history.single()

        assertEquals(Coins(15), outcome.plan[BudgetCategory.NEEDS])
        assertEquals(Coins(10), outcome.spentNeeds)
        assertEquals(-5, outcome.deviation(BudgetCategory.NEEDS))
        assertEquals(0, outcome.deviation(BudgetCategory.WANTS))
    }
}
