package ru.onefortwo.finny

import java.io.File
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.onefortwo.finny.content.AssetSource
import ru.onefortwo.finny.content.ContentRepository
import ru.onefortwo.finny.content.ItemCategory
import ru.onefortwo.finny.content.NumberTask
import ru.onefortwo.finny.content.PetAppearance
import ru.onefortwo.finny.content.TaskAnswer
import ru.onefortwo.finny.content.withNumbers
import ru.onefortwo.finny.economy.Difficulty
import ru.onefortwo.finny.economy.GrowthStage
import ru.onefortwo.finny.ui.state.DateProvider
import ru.onefortwo.finny.ui.state.GameViewModel
import ru.onefortwo.finny.ui.state.Growth
import ru.onefortwo.finny.ui.state.Season
import ru.onefortwo.finny.ui.state.SeasonExtras

/**
 * ТЕСТ 3: сезон из трёх дней, банки, события дня, сюрпризы за задания,
 * рост за мечты и задания. Контент читается из файлов модуля `content`.
 */
class SeasonTest {

    private val content = ContentRepository(
        AssetSource { name -> File("../content/src/main/assets/$name").readText() },
    )

    private var day = java.time.LocalDate.of(2026, 9, 28)

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun model(demo: Boolean = false): GameViewModel = GameViewModel(
        content,
        repository = null,
        dates = DateProvider { day.toString() },
    ).apply {
        if (demo) startDemo()
        createProfile("Финни", PetAppearance("cat", "ginger", "none"), Difficulty.SIMPLE)
        dismissArrival()
        if (state.value.game.savings.goal == null) chooseGoal("scooter")
    }

    /** Отвечает на все события дня первой кнопкой. */
    private fun GameViewModel.answerDay(accept: Boolean = true) {
        repeat(3) {
            if (pendingEvent() == null) return
            answerEvent(accept)
            if (state.value.savingsAsk != null) confirmSavingsAsk()
            closeEventResult(false)
        }
    }

    @Test
    fun `в дне три события — обязательное, затратное и спокойное, затратные не подряд`() {
        val events = content.events()
        for (period in 1..9) {
            val ids = Season.pickDay(events, period, emptyList(), null, emptySet(), emptySet())
            assertEquals(3, ids.size)
            val picked = ids.map { id -> events.first { it.id == id } }
            val mandatory = picked.filter { it.mandatory }
            assertEquals(1, mandatory.size)
            assertEquals(if (Season.dayOf(period) == 2) "thirst" else "hunger", mandatory.single().group)
            assertEquals(1, picked.count { !it.cost })
            assertFalse("Затратные подряд: $ids", picked[0].cost && picked[1].cost || picked[1].cost && picked[2].cost)
            assertEquals(ids, Season.pickDay(events, period, emptyList(), null, emptySet(), emptySet()))
        }
    }

    @Test
    fun `после отказа от зонта бывает продолжение «промок»`() {
        val ids = Season.pickDay(content.events(), 2, listOf("rain_no_tent"), "rain_no_tent", emptySet(), emptySet())
        assertTrue(ids.toString(), "wet_after_rain" in ids)
    }

    @Test
    fun `нужное берётся из «Нужного», потом из «Хочу», потом из копилки, желаемое — только из «Хочу»`() {
        val x = SeasonExtras(planned = true, needsJar = 4, wantsJar = 3)
        val needs = Season.payment(10, ItemCategory.NEEDS, balance = 7, saved = 20, x = x)
        assertEquals(4, needs.fromNeeds)
        assertEquals(3, needs.fromWants)
        assertEquals(3, needs.fromSavings)
        assertTrue(needs.needsSavings)
        val wants = Season.payment(5, ItemCategory.WANTS, balance = 7, saved = 20, x = x)
        assertTrue(wants.impossible)
    }

    @Test
    fun `план раскладывает все монеты по банкам, копилка пополняется сразу`() {
        val model = model()
        assertFalse(model.confirmPlan(20, 10, 10))
        assertTrue(model.confirmPlan(20, 15, 15))
        val state = model.state.value
        assertEquals(20, state.extras.needsJar)
        assertEquals(15, state.extras.wantsJar)
        assertEquals(15, state.game.savings.saved.amount)
        assertEquals(35, state.game.balance.amount)
        assertEquals(0, state.freeCoins)
        assertEquals(3, state.extras.dayEvents.size)
    }

    @Test
    fun `план можно поправить — монеты перекладываются между банками`() {
        val model = model()
        model.confirmPlan(20, 15, 15)
        assertTrue(model.confirmPlan(25, 5, 5))
        val x = model.state.value.extras
        assertEquals(25, x.needsJar)
        assertEquals(5, x.wantsJar)
        assertEquals(20, model.state.value.game.savings.saved.amount)
        assertEquals(20, x.plannedSavings)
    }

    @Test
    fun `затратное событие платится из банка и меняет питомца`() {
        val model = model()
        model.confirmPlan(20, 15, 15)
        val event = model.pendingEvent()!!
        val careBefore = model.state.value.game.pet.care.value
        val jarsBefore = model.state.value.extras.needsJar + model.state.value.extras.wantsJar
        model.answerEvent(true)
        if (model.state.value.savingsAsk != null) model.confirmSavingsAsk()
        val state = model.state.value
        assertNotNull(state.eventResult)
        if (event.cost) {
            assertEquals(jarsBefore - event.price, state.extras.needsJar + state.extras.wantsJar)
            if (event.category == ItemCategory.NEEDS) assertTrue(state.game.pet.care.value >= careBefore)
        }
        assertEquals(1, state.extras.answered)
    }

    @Test
    fun `день не закончить, пока события не решены`() {
        val model = model()
        model.confirmPlan(20, 15, 15)
        model.finishPeriod()
        assertEquals(1, model.state.value.game.period.number)
        model.answerDay()
        assertTrue(model.state.value.dayEventsDone)
        model.finishPeriod()
        assertEquals(2, model.state.value.game.period.number)
        assertEquals(day.toString(), model.state.value.lastFinishedDate)
    }

    @Test
    fun `монеты приходят раз в сезон, в новый день сезона — нет`() {
        day = java.time.LocalDate.of(2026, 9, 28)
        val model = model()
        model.confirmPlan(20, 15, 15)
        model.answerDay()
        model.finishPeriod()
        val balance = model.state.value.game.balance.amount
        day = day.plusDays(1)
        model.startNewDayIfDue()
        assertEquals(balance, model.state.value.game.balance.amount)
        assertEquals(2, model.state.value.seasonDay)
        assertTrue(model.state.value.extras.planned)
        assertEquals(3, model.state.value.extras.dayEvents.size)
    }

    @Test
    fun `в демо сезон проходится подряд, итоги, остаток в копилку и новые 50 монет`() {
        val model = model(demo = true)
        model.confirmPlan(20, 15, 15)
        repeat(3) {
            model.answerDay()
            model.finishPeriod()
        }
        var state = model.state.value
        assertTrue(state.extras.seasonDone)
        assertEquals(4, state.game.period.number)
        val leftover = state.extras.needsJar + state.extras.wantsJar + state.freeCoins
        val savedBefore = state.game.savings.saved.amount
        model.transferLeftover()
        state = model.state.value
        assertEquals(savedBefore + leftover, state.game.savings.saved.amount)
        assertEquals(0, state.game.balance.amount)
        model.closeSeason()
        state = model.state.value
        assertEquals(2, state.extras.season)
        assertFalse(state.extras.planned)
        assertEquals(50, state.game.balance.amount)
    }

    @Test
    fun `отказ от желаемого предлагает перевести его цену в копилку`() {
        val events = content.events().filter { it.cost && it.category == ItemCategory.WANTS && !it.mandatory }
        assertTrue(events.isNotEmpty())
        // Ищем день, где есть затратное желаемое событие.
        val model = model(demo = true)
        model.confirmPlan(5, 40, 5)
        var offered = false
        repeat(12) {
            if (offered) return@repeat
            val event = model.pendingEvent()
            if (event != null && event.cost && event.category == ItemCategory.WANTS) {
                val saved = model.state.value.game.savings.saved.amount
                val jar = model.state.value.extras.wantsJar
                model.answerEvent(false)
                val transfer = model.state.value.eventResult!!.transfer
                // Перевести можно, только если в банке «Хочу» хватает монет.
                if (jar >= event.price) {
                    assertEquals(event.price, transfer)
                    model.closeEventResult(true)
                    assertEquals(saved + transfer, model.state.value.game.savings.saved.amount)
                    offered = true
                } else {
                    assertEquals(0, transfer)
                    model.closeEventResult(false)
                }
                return@repeat
            }
            if (event != null) {
                model.answerEvent(true)
                if (model.state.value.savingsAsk != null) model.confirmSavingsAsk()
                model.closeEventResult(false)
            } else {
                model.finishPeriod()
                if (model.state.value.extras.seasonDone) model.closeSeason()
            }
        }
        assertTrue("За сезон не встретилось затратное желаемое событие", offered)
    }

    @Test
    fun `задания без монет, каждые три открывают сюрприз в лавке`() {
        val model = model()
        val task = content.task("save_rate")!!.withNumbers(Random(1)) as NumberTask
        val balance = model.state.value.game.balance.amount
        repeat(3) { model.answerTask(task, TaskAnswer.Number(task.answer)) }
        val state = model.state.value
        assertEquals(balance, state.game.balance.amount)
        assertEquals(3, state.extras.tasksSolved)
        assertEquals("pants", state.extras.surprise)
        assertTrue(Season.visibleItems(content.shopItems(), state.extras).any { it.id == "pants" })
        assertFalse(Season.visibleItems(content.shopItems(), SeasonExtras()).any { it.id == "pants" })
        model.dismissSurprise()
        assertNull(model.state.value.extras.surprise)
    }

    @Test
    fun `подростком — за две мечты и девять заданий`() {
        assertEquals(GrowthStage.BABY, Growth.stageFor(2, 8))
        assertEquals(GrowthStage.BABY, Growth.stageFor(1, 20))
        assertEquals(GrowthStage.TEEN, Growth.stageFor(2, 9))
        assertEquals(GrowthStage.ADULT, Growth.stageFor(4, 18))
    }

    @Test
    fun `незаконченный сезон через шесть дней закрывается, монеты приходят`() {
        val model = model()
        model.confirmPlan(20, 15, 15)
        day = day.plusDays(6)
        model.startNewDayIfDue()
        val state = model.state.value
        assertEquals(2, state.extras.season)
        assertTrue(state.extras.missed)
        assertEquals(Season.firstPeriodOf(2), state.game.period.number)
        assertEquals(35 + 50, state.game.balance.amount)
    }

    @Test
    fun `состояние сезона переживает запись и чтение`() {
        val x = SeasonExtras(season = 2, planned = true, needsJar = 7, dayEvents = listOf("hunger"), unlocked = listOf("pants"), growthShown = GrowthStage.TEEN)
        assertEquals(x, SeasonExtras.decode(x.encode()))
        assertEquals(SeasonExtras(), SeasonExtras.decode(""))
    }
}
