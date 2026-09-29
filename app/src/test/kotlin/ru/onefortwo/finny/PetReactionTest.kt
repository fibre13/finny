package ru.onefortwo.finny

import java.io.File
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
import ru.onefortwo.finny.economy.GrowthStage
import ru.onefortwo.finny.economy.StatLevel
import ru.onefortwo.finny.ui.common.PetMotionClock
import ru.onefortwo.finny.ui.common.composePet
import ru.onefortwo.finny.ui.common.composeScene
import ru.onefortwo.finny.ui.state.GameViewModel
import ru.onefortwo.finny.ui.state.PetReaction
import ru.onefortwo.finny.ui.state.PetReactions

/** Реакции питомца на события игры: очередь, ход по тактам, отрисовка. */
@OptIn(ExperimentalCoroutinesApi::class)
class PetReactionTest {

    private val content = ContentRepository(
        AssetSource { name -> File("../content/src/main/assets/$name").readText() },
    )
    private val art = content.pixelArt()

    @Before
    fun setUpMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDownMainDispatcher() {
        Dispatchers.resetMain()
    }

    private fun viewModel(): GameViewModel = GameViewModel(content, repository = null).apply {
        createProfile("Финни", PetAppearance("cat", "ginger", "none"), Difficulty.HARDER)
    }

    private fun GameViewModel.kinds() = reactions.value.map { it.kind }

    @Ignore("Механика заменена сезоном в версии 0.8.0, см. SeasonTest")
    @Test
    fun `покупки, копилка и награда ставят свои реакции`() {
        val model = viewModel()
        model.buy("food")
        model.buy("ball")
        assertEquals(listOf(PetReactions.EAT, PetReactions.PLAY), model.kinds())

        model.chooseGoal("scooter")
        model.deposit(5)
        assertEquals(PetReactions.SAVE, model.kinds().last())

        val task = content.task("save_rate")!!.withNumbers(Random(1)) as NumberTask
        model.answerTask(task, TaskAnswer.Number(task.answer))
        assertEquals(PetReactions.REWARD, model.kinds().last())
    }

    @Test
    fun `в очереди не больше трёх реакций, проигранная убирается`() {
        val model = viewModel()
        repeat(5) { model.buy("water") }
        assertEquals(PetReactions.MAX_QUEUED, model.reactions.value.size)

        val first = model.reactions.value.first()
        model.reactionPlayed(first.id)
        assertEquals(PetReactions.MAX_QUEUED - 1, model.reactions.value.size)
        assertFalse(model.reactions.value.any { it.id == first.id })
    }

    @Test
    fun `отказ в покупке реакции не вызывает`() {
        val model = viewModel()
        repeat(10) { model.buy("tent") }
        // Две палатки по 25 монет из 50; остальные попытки — отказ.
        assertEquals(2, model.reactions.value.size)
    }

    @Ignore("Механика заменена сезоном в версии 0.8.0, см. SeasonTest")
    @Test
    fun `переход на новую стадию ставит реакцию роста со стадией до роста`() {
        val model = viewModel()
        model.chooseGoal("scooter")
        var grow: PetReaction? = null
        repeat(3) {
            model.confirmPlan(needs = 10, wants = 12, savings = 3)
            model.buy("food")
            model.buy("ball")
            model.finishPeriod()
            model.reactions.value.lastOrNull { it.kind == PetReactions.GROW }?.let { grow = it }
            model.reactions.value.forEach { model.reactionPlayed(it.id) }
        }
        assertEquals(GrowthStage.BABY, grow?.stageBefore)
    }

    @Test
    fun `реакция идёт положенное число тактов, заканчивается один раз и не повторяется`() {
        val eat = art.animation.reactions.getValue(PetReactions.EAT)
        val clock = PetMotionClock(art.animation, Random(3))
        val reaction = PetReaction(PetReactions.EAT, id = 1)
        var finished = 0
        var shown = 0
        // Очередь не обновляется: та же реакция передаётся и после окончания.
        repeat(eat.ticks * 3) { tick ->
            val step = clock.step(tick, "happy", reaction)
            if (step.frame.reaction != null) shown++
            if (step.finishedReaction == 1L) finished++
        }
        assertEquals(eat.ticks, shown)
        assertEquals(1, finished)
    }

    @Test
    fun `во время реакции прыжки и глаза берутся из неё`() {
        val play = art.animation.reactions.getValue(PetReactions.PLAY)
        val clock = PetMotionClock(art.animation, Random(3))
        val frames = (0 until play.ticks).map { clock.step(it, "idle", PetReaction(PetReactions.PLAY, 1)).frame }
        assertTrue("Нет прыжка в реакции play", frames.any { it.dy < 0 })
        assertTrue("Моргание поверх радостных глаз", frames.none { it.blink })
    }

    @Test
    fun `реакции меняют изображение, частицы видны, рост показывает прежнюю стадию`() {
        val hex = content.petParts().colors.first().hex
        fun pet(reaction: String?, tick: Int, stage: GrowthStage = GrowthStage.TEEN, before: GrowthStage? = null) =
            composePet(
                art, "dog", stage, hex, "none", StatLevel.MEDIUM, StatLevel.MEDIUM,
                reaction = reaction, reactionTick = tick, stageBefore = before,
            )
        val still = pet(null, 0)
        listOf(PetReactions.EAT, PetReactions.PLAY, PetReactions.SAVE, PetReactions.REWARD).forEach { kind ->
            val spec = art.animation.reactions.getValue(kind)
            val changed = (0 until spec.ticks).any { !pet(kind, it).contentEquals(still) }
            assertTrue("Реакция $kind не видна", changed)
        }

        val grow = art.animation.reactions.getValue(PetReactions.GROW)
        val baby = pet(null, 0, stage = GrowthStage.BABY)
        val beforeSwap = pet(PetReactions.GROW, grow.swapStage!! - 2, before = GrowthStage.BABY)
        // До смены стадии — прежняя фигура (с искрами поверх).
        val differsFromBaby = beforeSwap.indices.count { beforeSwap[it] != baby[it] }
        val differsFromTeen = beforeSwap.indices.count { beforeSwap[it] != still[it] }
        assertTrue("До смены стадии не прежняя фигура", differsFromBaby < differsFromTeen)

        val flash = pet(PetReactions.GROW, grow.flashAt!!, before = GrowthStage.BABY)
        val light = art.colors[art.indexOf('c')]
        assertTrue("Нет вспышки", flash.count { it == light } > still.count { it == light })
    }

    @Test
    fun `наклейки видны на палатке`() {
        val pet = composePet(
            art, "cat", GrowthStage.BABY, content.petParts().colors.first().hex, "none",
            StatLevel.MEDIUM, StatLevel.MEDIUM, inScene = true,
        )
        val tent = composeScene(art, house = true, goalId = null, pet = pet)
        assertFalse(tent.contentEquals(composeScene(art, house = true, goalId = null, pet = pet, stickers = true)))
        // Без палатки наклеек нет: клеить не на что.
        val bare = composeScene(art, house = false, goalId = null, pet = pet)
        assertTrue(bare.contentEquals(composeScene(art, house = false, goalId = null, pet = pet, stickers = true)))
    }
}
