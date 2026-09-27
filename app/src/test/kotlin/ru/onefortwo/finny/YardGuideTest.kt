package ru.onefortwo.finny

import java.io.File
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import ru.onefortwo.finny.content.AssetSource
import ru.onefortwo.finny.content.ContentRepository
import ru.onefortwo.finny.content.NumberTask
import ru.onefortwo.finny.content.PetAppearance
import ru.onefortwo.finny.content.TaskAnswer
import ru.onefortwo.finny.content.withNumbers
import ru.onefortwo.finny.economy.Difficulty
import ru.onefortwo.finny.ui.state.GameViewModel
import ru.onefortwo.finny.ui.state.GuideTarget
import ru.onefortwo.finny.ui.state.YardGuide

/** ТЕСТ (ветка test/kopilka-a): подсказки первого дня ведут по шагам. */
class YardGuideTest {

    private val content = ContentRepository(
        AssetSource { name -> File("../content/src/main/assets/$name").readText() },
    )
    private val housePrice = content.shopItems().first { it.unlocksScenery == YardGuide.HOUSE }.price

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun GameViewModel.guide() = YardGuide.step(state.value, housePrice)

    private fun GameViewModel.solve(id: String) {
        val task = content.task(id)!!.withNumbers(Random(1)) as NumberTask
        answerTask(task, TaskAnswer.Number(task.answer))
    }

    @Test
    fun `подсказки идут по шагам и исчезают после первого дня`() {
        val model = GameViewModel(content, repository = null)
        assertNull("до знакомства подсказок нет", model.guide())

        model.createProfile("Финни", PetAppearance("cat", "ginger", "bow"), Difficulty.HARDER)
        assertEquals(1, model.guide()?.number)
        assertEquals(GuideTarget.PLAN, model.guide()?.target)

        model.confirmPlan(needs = 10, wants = 25, savings = 0)
        assertEquals(GuideTarget.SHOP, model.guide()?.target)
        assertEquals(2, model.guide()?.number)

        model.buy("food")
        assertEquals(GuideTarget.TASKS, model.guide()?.target)
        assertEquals(3, model.guide()?.number)

        model.solve("save_rate")
        // 50 − 10 + 10 = 50: на домик хватает — пора в лавку.
        assertEquals(4, model.guide()?.number)
        assertEquals(GuideTarget.SHOP, model.guide()?.target)

        model.buy("tent")
        assertEquals(GuideTarget.SAVINGS, model.guide()?.target)

        model.chooseGoal("scooter")
        model.deposit(5)
        assertEquals(GuideTarget.FINISH, model.guide()?.target)
        assertEquals(6, model.guide()?.number)

        model.finishPeriod()
        assertNull("со второго дня подсказок нет", model.guide())
    }

    @Test
    fun `если на домик не хватает, подсказка ведёт к заданиям`() {
        val model = GameViewModel(content, repository = null)
        model.createProfile("Финни", PetAppearance("cat", "ginger", "bow"), Difficulty.HARDER)
        model.confirmPlan(needs = 10, wants = 10, savings = 0)
        model.buy("food")
        model.buy("ball")
        model.buy("bow")
        model.buy("stickers")
        model.solve("save_rate")
        // 50 − 10 − 12 − 6 − 4 + 10 = 28 ≥ 25 — поэтому тратится ещё.
        model.buy("vitamins")

        val step = model.guide()
        assertEquals(4, step?.number)
        assertEquals(GuideTarget.TASKS, step?.target)
    }
}
