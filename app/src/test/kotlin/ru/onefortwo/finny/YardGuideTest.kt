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
import org.junit.Ignore
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
import ru.onefortwo.finny.ui.state.GuideTarget
import ru.onefortwo.finny.ui.state.Reminder
import ru.onefortwo.finny.ui.state.YardGuide

/**
 * Одна цель на экране, день = календарные
 * сутки, монеты за первые два задания дня.
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

    @Ignore("ТЕСТ 3: механика основной версии заменена сезоном, см. SeasonTest")
    @Test
    fun `первый день ведёт по шагам в порядке Приложения А`() {
        val model = model()
        assertNull("до знакомства подсказок нет", model.guide())

        model.createProfile("Финни", PetAppearance("cat", "ginger", "bow"), Difficulty.HARDER)
        // Сначала — окно «Пришли монеты», подсветки нет.
        assertNotNull(model.state.value.arrival)
        assertNull(model.guide())
        model.dismissArrival()

        assertEquals(1, model.guide()?.number)
        assertEquals(GuideTarget.SAVINGS, model.guide()?.target)

        model.chooseGoal("scooter")
        assertEquals(GuideTarget.PLAN, model.guide()?.target)

        model.confirmPlan(needs = 10, wants = 12, savings = 5)
        assertEquals(3, model.guide()?.number)
        assertEquals(GuideTarget.SHOP, model.guide()?.target)

        model.buy("food")
        assertEquals(GuideTarget.TASKS, model.guide()?.target)

        model.solve("save_rate")
        assertEquals(5, model.guide()?.number)
        assertEquals(GuideTarget.SHOP, model.guide()?.target)

        model.buy("ball")
        assertEquals(GuideTarget.FINISH, model.guide()?.target)
    }

    @Ignore("ТЕСТ 3: механика основной версии заменена сезоном, см. SeasonTest")
    @Test
    fun `после закрытия дня питомец спит, монеты приходят в новые сутки`() {
        val model = model()
        model.createProfile("Финни", PetAppearance("cat", "ginger", "bow"), Difficulty.HARDER)
        model.dismissArrival()
        model.chooseGoal("scooter")
        model.confirmPlan(needs = 10, wants = 0, savings = 5)
        model.buy("food")
        val before = model.state.value.game.balance.amount

        model.finishPeriod()
        // Карманные не пришли: сегодня питомец спит, подсветки нет.
        assertEquals(before, model.state.value.game.balance.amount)
        assertTrue(model.state.value.isSleeping(today))
        assertNull(model.guide())

        // В те же сутки новый день не начинается.
        model.startNewDayIfDue()
        assertEquals(before, model.state.value.game.balance.amount)

        today = "2026-09-29"
        model.startNewDayIfDue()
        val state = model.state.value
        assertEquals(before + 25, state.game.balance.amount)
        assertFalse(state.isSleeping(today))
        assertNotNull("о монетах сказано окном", state.arrival)
        model.dismissArrival()
        // Со второго дня выделена только доска плана, без номера шага.
        assertEquals(GuideTarget.PLAN, model.guide()?.target)
        assertEquals(0, model.guide()?.number)
    }

    /** Первый день прожит, наступили вторые сутки, план второго дня утверждён. */
    private fun secondDayWithPlan(): GameViewModel {
        val model = model()
        model.createProfile("Финни", PetAppearance("cat", "ginger", "bow"), Difficulty.HARDER)
        model.dismissArrival()
        model.chooseGoal("scooter")
        model.confirmPlan(needs = 10, wants = 0, savings = 5)
        model.buy("food")
        model.finishPeriod()
        today = "2026-09-29"
        model.startNewDayIfDue()
        model.dismissArrival()
        model.confirmPlan(needs = 10, wants = 5, savings = 5)
        return model
    }

    @Ignore("ТЕСТ 3: механика основной версии заменена сезоном, см. SeasonTest")
    @Test
    fun `со второго дня двор напоминает о делах по порядку дня`() {
        val model = secondDayWithPlan()

        assertEquals(Reminder.NEEDS, model.guide()?.reminder)
        assertEquals(GuideTarget.SHOP, model.guide()?.target)
        model.buy("food")

        assertEquals(Reminder.TASK, model.guide()?.reminder)
        assertEquals(GuideTarget.TASKS, model.guide()?.target)
        model.solve("save_rate")

        // Слова первого дня встречены, а словарик ещё не открывали.
        assertEquals(Reminder.WORD, model.guide()?.reminder)
        assertEquals(GuideTarget.GLOSSARY, model.guide()?.target)
        model.markTermsRead()

        assertEquals(Reminder.FINISH, model.guide()?.reminder)
        model.finishPeriod()
        assertNull("питомец спит — напоминаний нет", model.guide())
    }

    @Ignore("ТЕСТ 3: механика основной версии заменена сезоном, см. SeasonTest")
    @Test
    fun `«Не сейчас» откладывает напоминание до конца игрового дня`() {
        val model = secondDayWithPlan()
        assertEquals(Reminder.NEEDS, model.guide()?.reminder)

        model.dismissReminder(Reminder.NEEDS)
        assertEquals("следующее дело по порядку", Reminder.TASK, model.guide()?.reminder)

        // Новый игровой день — отложенное снова напоминается.
        model.buy("ball")
        model.finishPeriod()
        today = "2026-09-30"
        model.startNewDayIfDue()
        model.dismissArrival()
        model.confirmPlan(needs = 10, wants = 0, savings = 5)
        assertEquals(Reminder.NEEDS, model.guide()?.reminder)
    }

    @Ignore("ТЕСТ 3: механика основной версии заменена сезоном, см. SeasonTest")
    @Test
    fun `про задание не напоминают, когда экранное время вышло`() {
        val model = secondDayWithPlan()
        model.buy("food")
        val timeUp = model.state.value.copy(usageDate = today, usageMinutes = 20)

        assertTrue(timeUp.isTimeUp(today))
        assertEquals(Reminder.WORD, YardGuide.reminder(timeUp, today))
    }

    @Ignore("ТЕСТ 3: механика основной версии заменена сезоном, см. SeasonTest")
    @Test
    fun `слово становится новым при первой встрече и один раз`() {
        val model = model()
        model.createProfile("Финни", PetAppearance("cat", "ginger", "bow"), Difficulty.HARDER)
        assertTrue("до первых действий новых слов нет", model.state.value.newTerms.isEmpty())

        model.chooseGoal("scooter")
        assertEquals(setOf("Цель", "Копилка"), model.state.value.newTerms)

        model.markTermsRead()
        assertTrue(model.state.value.newTerms.isEmpty())

        // Прочитанное слово не становится новым снова; новое — только встреченное впервые.
        model.confirmPlan(needs = 10, wants = 0, savings = 5)
        assertEquals(setOf("Бюджет", "План", "Накопления"), model.state.value.newTerms)
        assertTrue("Цель" in model.state.value.knownTerms)

        model.solve("change_count")
        assertTrue("Сдача" in model.state.value.newTerms)
        assertTrue("Доход" in model.state.value.newTerms)
    }

    @Test
    fun `встреченные слова есть в словарике`() {
        val terms = content.glossary().map { it.term }.toSet()
        val model = secondDayWithPlan()
        model.buy("food")
        model.solve("change_count")

        assertTrue(model.state.value.knownTerms.isNotEmpty())
        assertTrue(
            "каждое встреченное слово должно быть в glossary.json: ${model.state.value.knownTerms - terms}",
            terms.containsAll(model.state.value.knownTerms),
        )
    }

    @Ignore("ТЕСТ 3: механика основной версии заменена сезоном, см. SeasonTest")
    @Test
    fun `монеты — за первые два задания дня`() {
        val model = model()
        model.createProfile("Финни", PetAppearance("cat", "ginger", "bow"), Difficulty.HARDER)
        val start = model.state.value.game.balance.amount
        model.solve("save_rate")
        model.solve("change_count")
        assertEquals(start + 20, model.state.value.game.balance.amount)

        // Третье задание засчитано, но без монет — и об этом сказано.
        model.solve("save_rate")
        assertEquals(start + 20, model.state.value.game.balance.amount)
        assertTrue(model.state.value.message!!.text.contains("уже получены"))
    }
}
