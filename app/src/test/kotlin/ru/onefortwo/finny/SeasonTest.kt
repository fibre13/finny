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
import ru.onefortwo.finny.ui.state.toAppState

/**
 * Сезон из трёх дней, банки, события дня, сюрпризы за задания,
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
    fun `в сезоне девять событий, затратные не подряд ни в дне, ни на стыке дней`() {
        val events = content.events()
        val items = content.shopItems()
        for (species in listOf("cat", "dog", "rabbit")) {
            for (season in 1..4) {
                val used = mutableListOf<String>()
                val all = mutableListOf<ru.onefortwo.finny.content.EventContent>()
                for (day in 1..3) {
                    val period = Season.firstPeriodOf(season) + day - 1
                    val ids = Season.pickDay(events, period, used, null, emptySet(), emptySet(), species, emptySet(), items, SeasonExtras(), 10)
                    assertEquals(ids.toString(), 3, ids.size)
                    assertEquals(ids, Season.pickDay(events, period, used, null, emptySet(), emptySet(), species, emptySet(), items, SeasonExtras(), 10))
                    val picked = ids.map { id -> events.first { it.id == id } }
                    val mandatory = picked.single { it.mandatory }
                    assertEquals(if (day == 2) "thirst" else "hunger", mandatory.group)
                    assertTrue("$species: ${mandatory.id}", mandatory.fits(species))
                    // День 2 — одно затратное (жажда) между двумя незатратными, игра — последней.
                    if (day == 2) {
                        assertEquals(listOf(false, true, false), picked.map { it.cost })
                        assertEquals(ru.onefortwo.finny.content.EventKind.INTERACTIVE, picked[2].kind)
                    } else {
                        assertEquals(listOf(true, false, true), picked.map { it.cost })
                    }
                    used += ids
                    all += picked
                }
                val costly = all.map { it.cost }
                assertFalse("Затратные подряд: ${all.map { it.id }}", costly.zipWithNext().any { (a, b) -> a && b })
            }
        }
    }

    @Test
    fun `третий день — событие по времени года`() {
        val events = content.events()
        val items = content.shopItems()
        // Зимой — холод, летом — жара.
        val winter = Season.pickDay(events, 3, emptyList(), null, emptySet(), emptySet(), "cat", emptySet(), items, SeasonExtras(), 1)
        assertTrue(winter.toString(), "cold_scarf" in winter)
        val summer = Season.pickDay(events, 3, emptyList(), null, emptySet(), emptySet(), "cat", emptySet(), items, SeasonExtras(), 7)
        assertTrue(summer.toString(), "heat_panama" in summer)
    }

    @Test
    fun `после отказа от зонта сразу следом — «промок», оно бесплатное`() {
        val next = Season.followUp(content.events(), "rain_no_tent", "cat")
        assertEquals("wet_after_rain", next?.id)
        assertFalse(next!!.cost)
    }

    @Test
    fun `событие с товаром бывает, только когда товар уже в лавке, и не сразу`() {
        val events = content.events()
        val items = content.shopItems()
        // Билета в кино в лавке нет — события «кино» не бывает ни в одном дне.
        val none = (1..30).flatMap { period ->
            Season.pickDay(events, period, emptyList(), null, emptySet(), emptySet(), "dog", emptySet(), items, SeasonExtras(), 10)
        }
        assertFalse("cinema" in none)
        assertFalse("bone_wish" in none)
        // Открыт за задания в день 4 — в дни 4 и 5 о нём ещё не просят, с дня 6 — можно.
        val x = SeasonExtras(unlocked = listOf("ticket_cinema", "bone"), unlockedAt = mapOf("ticket_cinema" to 4, "bone" to 4))
        assertFalse(Season.itemReady("bone", items, x, "dog", 5))
        assertTrue(Season.itemReady("bone", items, x, "dog", 6))
        // Косточка — только щенку.
        assertFalse(Season.itemReady("bone", items, x, "cat", 6))
        // Базовые товары лавки готовы сразу.
        assertTrue(Season.itemReady("chocolate", items, SeasonExtras(), "cat", 1))
    }

    @Test
    fun `«хочу мячик» — только пока мячика нет, «поиграть в мячик» — когда он есть`() {
        val events = content.events()
        val items = content.shopItems()
        val without = (1..30).flatMap { period ->
            Season.pickDay(events, period, emptyList(), null, emptySet(), emptySet(), "cat", emptySet(), items, SeasonExtras(), 10)
        }
        assertFalse("play_ball" in without)
        val with = (1..30).flatMap { period ->
            Season.pickDay(events, period, emptyList(), null, emptySet(), emptySet(), "cat", setOf("ball"), items, SeasonExtras(), 10)
        }
        assertFalse("ball_wish" in with)
    }

    @Test
    fun `лавка по виду питомца — котёнку молоко, щенку вода и косточка за задания`() {
        val items = content.shopItems()
        val cat = Season.visibleItems(items, SeasonExtras(), "cat").map { it.id }
        assertTrue("milk" in cat)
        assertFalse("water" in cat)
        val dog = Season.visibleItems(items, SeasonExtras(), "dog").map { it.id }
        assertTrue("water" in dog)
        assertFalse("milk" in dog)
        assertFalse("bone" in dog)
        // Три задания — сюрприз-одежда и еда по виду вместе.
        val opened = Season.pendingSurprise(items, SeasonExtras(tasksSolved = 3), "dog").map { it.id }
        assertEquals(listOf("pants", "bone"), opened)
        val rabbit = Season.pendingSurprise(items, SeasonExtras(tasksSolved = 3), "rabbit").map { it.id }
        assertEquals(listOf("pants", "carrot"), rabbit)
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
        repeat(60) {
            if (offered) return@repeat
            // Новый сезон — новый план: почти всё — в «Хочу».
            model.state.value.let { st ->
                if (!st.extras.planned && !st.extras.seasonDone) {
                    val available = st.freeCoins + st.extras.needsJar + st.extras.wantsJar
                    model.confirmPlan(available / 5, available - available / 5, 0)
                }
            }
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
        // Котёнку вместе со штанишками открывается колбаска.
        assertEquals(listOf("sausage"), state.extras.surpriseAlso)
        assertEquals(state.game.period.number, state.extras.unlockedAt["sausage"])
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
        assertEquals(GrowthStage.TEEN, Growth.stageFor(3, 17))
        assertEquals(GrowthStage.ADULT, Growth.stageFor(3, 18))
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
    fun `итоги называют, откуда взяты монеты на нужное сверх плана`() {
        val x = SeasonExtras(plannedNeeds = 20, spentNeeds = 42, plannedWants = 15, spentWants = 0, plannedSavings = 15, deposited = 8, wantsToNeeds = 15, savingsToNeeds = 7)
        val notes = Season.borrowNotes(x)
        assertEquals("Потрачено больше плана на 22 монеты: из банка «Хочу» взято 15 монет, из копилки — 7 монет.", notes.needs)
        assertEquals("Не потрачено на желаемое: 15 монет, из них 15 монет ушли на нужное.", notes.wants)
        assertEquals("Из копилки на нужное взято 7 монет.", notes.savings)
        assertEquals(Season.BorrowNotes(null, null, null), Season.borrowNotes(SeasonExtras(plannedNeeds = 20, spentNeeds = 10)))
    }

    @Test
    fun `профиль без состояния сезона продолжает текущий сезон и не получает праздник роста`() {
        val saved = ru.onefortwo.finny.data.SavedGame(
            petName = "Финни",
            speciesId = "cat",
            colorId = "ginger",
            accessoryId = "none",
            isDemo = false,
            game = ru.onefortwo.finny.economy.GameState(
                balance = ru.onefortwo.finny.economy.Coins(6),
                growthPoints = GrowthStage.TEEN.requiredPoints,
                period = ru.onefortwo.finny.economy.PeriodState(number = 5),
            ),
            completedTaskIds = emptySet(),
            extras = "",
        )
        val state = saved.toAppState()
        assertEquals(2, state.extras.season)
        assertEquals(GrowthStage.TEEN, state.extras.growthShown)
        assertFalse(state.extras.planned)
        assertEquals(6, state.game.balance.amount)
    }

    @Test
    fun `состояние сезона переживает запись и чтение`() {
        val x = SeasonExtras(season = 2, planned = true, needsJar = 7, dayEvents = listOf("hunger"), unlocked = listOf("pants"), growthShown = GrowthStage.TEEN)
        assertEquals(x, SeasonExtras.decode(x.encode()))
        assertEquals(SeasonExtras(), SeasonExtras.decode(""))
    }
}
