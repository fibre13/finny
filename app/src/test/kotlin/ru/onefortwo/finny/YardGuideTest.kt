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
import ru.onefortwo.finny.content.NumberTask
import ru.onefortwo.finny.content.PetAppearance
import ru.onefortwo.finny.content.TaskAnswer
import ru.onefortwo.finny.content.withNumbers
import ru.onefortwo.finny.economy.Difficulty
import ru.onefortwo.finny.ui.state.DateProvider
import ru.onefortwo.finny.ui.state.GameViewModel
import ru.onefortwo.finny.ui.state.GlossaryTerms
import ru.onefortwo.finny.ui.state.GuideTarget
import ru.onefortwo.finny.ui.state.Reminder
import ru.onefortwo.finny.ui.state.YardGuide

/**
 * Одна цель на экране: подсказки двора в сезоне. Мечта → план сезона →
 * события дня → напоминания по порядку дня; день = календарные сутки.
 */
class YardGuideTest {

    private val content = ContentRepository(
        AssetSource { name -> File("../content/src/main/assets/$name").readText() },
    )
    private var today = "2026-09-28"

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun model() = GameViewModel(content, repository = null, dates = DateProvider { today })

    private fun GameViewModel.guide() = YardGuide.step(state.value, today)

    private fun GameViewModel.solve(id: String) {
        val task = content.task(id)!!.withNumbers(Random(1)) as NumberTask
        answerTask(task, TaskAnswer.Number(task.answer))
    }

    /** «Помоги своему питомцу» — единственное задание сезона с монетами. */
    private fun GameViewModel.helpPet() {
        answerTask(content.task("recover_help")!!, TaskAnswer.Chosen("food"))
    }

    /** Отказ во всех событиях дня, вместе с продолжениями отказов: монеты не тратятся и не приходят. */
    private fun GameViewModel.declineDay() {
        repeat(6) {
            if (pendingEvent() == null) return
            answerEvent(false)
            closeEventResult(false)
        }
    }

    private fun nextDay() {
        today = java.time.LocalDate.parse(today).plusDays(1).toString()
    }

    /** Профиль с мечтой и планом сезона; события первого дня ещё не решены. */
    private fun plannedModel(): GameViewModel {
        val model = model()
        model.createProfile("Финни", PetAppearance("cat", "ginger", "bow"), Difficulty.HARDER)
        model.dismissArrival()
        model.chooseGoal("scooter")
        assertTrue(model.confirmPlan(needs = 20, wants = 15, savings = 15))
        return model
    }

    /** Первый день прожит, наступили вторые сутки, события второго дня решены. */
    private fun secondDayEventsDone(): GameViewModel {
        val model = plannedModel()
        model.declineDay()
        model.finishPeriod()
        nextDay()
        model.startNewDayIfDue()
        model.declineDay()
        assertTrue(model.state.value.dayEventsDone)
        assertEquals(2, model.state.value.seasonDay)
        return model
    }

    @Test
    fun `первый день сезона ведёт от мечты к плану, событиям и напоминаниям`() {
        val model = model()
        assertNull("до знакомства подсказок нет", model.guide())

        model.createProfile("Финни", PetAppearance("cat", "ginger", "bow"), Difficulty.HARDER)
        // Сначала — окно «Пришли монеты», подсветки нет.
        assertNotNull(model.state.value.arrival)
        assertNull(model.guide())
        model.dismissArrival()

        // Мечта — раньше плана: без цели монеты в копилку не отложить.
        assertEquals(GuideTarget.SAVINGS, model.guide()?.target)
        model.chooseGoal("scooter")
        assertEquals(GuideTarget.PLAN, model.guide()?.target)
        assertTrue(model.guide()!!.text.contains("50 монет"))

        model.confirmPlan(needs = 20, wants = 15, savings = 15)
        // Пока события дня не решены, двор ничего не выделяет: событие — в своём окне.
        assertNotNull(model.pendingEvent())
        assertNull(model.guide())

        model.declineDay()
        assertEquals(Reminder.TASK, model.guide()?.reminder)
        assertEquals(GuideTarget.TASKS, model.guide()?.target)
        assertEquals(0, model.guide()?.number)

        model.solve("save_rate")
        // О словарике зовёт своё облачко без кнопок — не напоминание по порядку дня.
        assertEquals(Reminder.PLAN_FIX, model.guide()?.reminder)
        model.dismissReminder(Reminder.PLAN_FIX)
        assertEquals(GuideTarget.FINISH, model.guide()?.target)

        model.finishPeriod()
        assertNull("питомец спит — подсказок нет", model.guide())
    }

    @Test
    fun `после закрытия дня питомец спит, новые монеты приходят только с новым сезоном`() {
        val model = plannedModel()
        model.declineDay()
        val before = model.state.value.game.balance.amount

        model.finishPeriod()
        assertTrue(model.state.value.isSleeping(today))
        assertNull(model.guide())

        // В те же сутки новый день не начинается.
        model.startNewDayIfDue()
        assertTrue(model.state.value.isSleeping(today))
        assertNull(model.guide())

        // Вторые сутки: день второй, монет внутри сезона не приходит — окна «Пришли монеты» нет.
        nextDay()
        model.startNewDayIfDue()
        var state = model.state.value
        assertFalse(state.isSleeping(today))
        assertEquals(before, state.game.balance.amount)
        assertNull(state.arrival)
        assertTrue(state.message!!.text.contains("День 2 из 3"))
        // Пока не решены события второго дня, двор ничего не выделяет.
        assertNull(model.guide())

        model.declineDay()
        model.finishPeriod()
        nextDay()
        model.startNewDayIfDue()
        model.declineDay()
        model.finishPeriod()
        // Третий день закрыт: итоги сезона, подсказок нет.
        assertTrue(model.state.value.extras.seasonDone)
        assertNull(model.guide())

        // Новые сутки: итоги закрыты — пришли 50 монет нового сезона, двор зовёт к плану.
        nextDay()
        model.startNewDayIfDue()
        assertNull(model.guide())
        model.closeSeason()
        state = model.state.value
        assertEquals(2, state.extras.season)
        assertEquals(before + 50, state.game.balance.amount)
        assertNotNull("о монетах сказано окном", state.arrival)
        assertNull(model.guide())
        model.dismissArrival()
        assertEquals(GuideTarget.PLAN, model.guide()?.target)
        assertTrue(model.guide()!!.text.startsWith("Новый сезон"))
    }

    @Test
    fun `после событий дня двор напоминает о делах по порядку дня`() {
        val model = secondDayEventsDone()

        assertEquals(Reminder.TASK, model.guide()?.reminder)
        assertEquals(GuideTarget.TASKS, model.guide()?.target)
        model.solve("save_rate")

        // Раз в сезон после дел дня — вопрос о поправке плана.
        assertEquals(Reminder.PLAN_FIX, model.guide()?.reminder)
        assertEquals(GuideTarget.PLAN, model.guide()?.target)
        model.dismissReminder(Reminder.PLAN_FIX)

        assertEquals(Reminder.FINISH, model.guide()?.reminder)
        model.finishPeriod()
        assertNull("питомец спит — напоминаний нет", model.guide())
    }

    @Test
    fun `«Не сейчас» откладывает напоминание до конца игрового дня, вопрос о плане — до конца сезона`() {
        val model = secondDayEventsDone()
        assertEquals(Reminder.TASK, model.guide()?.reminder)

        model.dismissReminder(Reminder.TASK)
        assertEquals("следующее дело по порядку", Reminder.PLAN_FIX, model.guide()?.reminder)
        model.dismissReminder(Reminder.PLAN_FIX)
        assertEquals(Reminder.FINISH, model.guide()?.reminder)

        // Новый игровой день — отложенные снова напоминаются, а о плане в этом сезоне больше не спрашивают.
        model.finishPeriod()
        nextDay()
        model.startNewDayIfDue()
        model.declineDay()
        assertEquals(3, model.state.value.seasonDay)
        assertEquals(Reminder.TASK, model.guide()?.reminder)
        model.dismissReminder(Reminder.TASK)
        assertEquals(Reminder.FINISH, model.guide()?.reminder)
    }

    @Test
    fun `про задание не напоминают, когда экранное время вышло`() {
        val model = secondDayEventsDone()
        assertEquals(Reminder.TASK, YardGuide.reminder(model.state.value, today))
        val timeUp = model.state.value.copy(usageDate = today, usageMinutes = 20)

        assertTrue(timeUp.isTimeUp(today))
        assertEquals(Reminder.PLAN_FIX, YardGuide.reminder(timeUp, today))
        // Взрослый снял ограничение — напоминание о задании возвращается.
        assertEquals(Reminder.TASK, YardGuide.reminder(timeUp.copy(timeLimitEnabled = false), today))
    }

    @Test
    fun `слово становится новым при первой встрече в сезоне и один раз`() {
        val model = model()
        model.createProfile("Финни", PetAppearance("cat", "ginger", "bow"), Difficulty.HARDER)
        assertTrue("до первых действий новых слов нет", model.state.value.newTerms.isEmpty())

        model.chooseGoal("scooter")
        assertEquals(setOf("Цель", "Копилка"), model.state.value.newTerms)

        model.markTermsRead()
        assertTrue(model.state.value.newTerms.isEmpty())

        // Прочитанное слово не становится новым снова; новое — только встреченное впервые.
        // План сезона сразу пополняет копилку — отсюда «Накопления».
        model.confirmPlan(needs = 20, wants = 15, savings = 15)
        assertEquals(setOf("Бюджет", "План", "Накопления"), model.state.value.newTerms)
        assertTrue("Цель" in model.state.value.knownTerms)

        model.solve("change_count")
        assertTrue("Сдача" in model.state.value.newTerms)
        assertTrue("Доход" in model.state.value.newTerms)

        // Прожитый день приносит «Факт» — план и факт сравниваются в итогах.
        model.markTermsRead()
        model.declineDay()
        model.finishPeriod()
        assertTrue("Факт" in model.state.value.newTerms)
        assertFalse("Цель" in model.state.value.newTerms)
    }

    @Test
    fun `карточка словарика открывается одна за игровой день, пропуск дней новых не добавляет`() {
        val terms = content.glossary().map { it.term }
        val model = plannedModel()
        assertEquals("в первый день — одна карточка", listOf(terms[0]), GlossaryTerms.available(model.state.value, terms))

        model.declineDay()
        model.finishPeriod()
        // Ребёнок не приходил три календарных дня — засчитывается только следующий день игры.
        repeat(3) { nextDay() }
        model.startNewDayIfDue()
        assertEquals(terms.take(2), GlossaryTerms.available(model.state.value, terms))
        assertEquals(2, GlossaryTerms.closed(model.state.value, terms).size)

        // Открытая карточка уходит из закрытых; неоткрытая не сгорает.
        model.openTerm(terms[0])
        assertEquals(listOf(terms[1]), GlossaryTerms.closed(model.state.value, terms))
        assertEquals(listOf(terms[0]), model.state.value.extras.wordsOpened)
    }

    @Test
    fun `облачко над словариком — после мечты, плана и двух событий и не в день захода`() {
        val terms = content.glossary().map { it.term }
        val model = plannedModel()
        assertNull("события дня ещё не решены", GlossaryTerms.hint(model.state.value, terms))

        model.declineDay()
        assertEquals(GlossaryTerms.HINT_NEW, GlossaryTerms.hint(model.state.value, terms))
        assertEquals("Есть новое слово! Загляни в словарик.", GlossaryTerms.HINT_NEW)

        // Заглянул в словарик — облачко не появляется до следующего игрового дня.
        model.markTermsRead()
        assertNull(GlossaryTerms.hint(model.state.value, terms))
    }

    @Test
    fun `встреченные слова есть в словарике`() {
        val terms = content.glossary().map { it.term }.toSet()
        val model = secondDayEventsDone()
        model.buy("food")
        model.solve("change_count")

        assertTrue(model.state.value.knownTerms.isNotEmpty())
        assertTrue(
            "каждое встреченное слово должно быть в glossary.json: ${model.state.value.knownTerms - terms}",
            terms.containsAll(model.state.value.knownTerms),
        )
    }

    @Test
    fun `монеты только за «Помоги своему питомцу», двор зовёт разложить их по банкам`() {
        val model = plannedModel()
        model.declineDay()
        val start = model.state.value.game.balance.amount

        // Обычное задание монет не приносит — только приближает сюрприз.
        model.solve("save_rate")
        assertEquals(start, model.state.value.game.balance.amount)
        assertTrue(model.state.value.message!!.text.contains("Задание засчитано"))
        assertEquals(0, model.state.value.freeCoins)

        // «Помоги своему питомцу» — 10 монет, повтор — половина.
        model.helpPet()
        assertEquals(start + 10, model.state.value.game.balance.amount)
        model.helpPet()
        assertEquals(start + 15, model.state.value.game.balance.amount)

        // Монеты вне банков — двор первым делом зовёт разложить их в «Плане».
        assertEquals(15, model.state.value.freeCoins)
        assertEquals(Reminder.FREE, model.guide()?.reminder)
        assertEquals(GuideTarget.PLAN, model.guide()?.target)

        val x = model.state.value.extras
        assertTrue(model.confirmPlan(needs = x.needsJar, wants = x.wantsJar + 15, savings = 0))
        assertEquals(0, model.state.value.freeCoins)
        assertFalse(model.guide()?.reminder == Reminder.FREE)
    }
}
