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
import ru.onefortwo.finny.ui.state.GuideTarget
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
