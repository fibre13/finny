package ru.onefortwo.finny

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.onefortwo.finny.content.AssetSource
import ru.onefortwo.finny.content.ContentRepository
import ru.onefortwo.finny.content.PetAppearance
import ru.onefortwo.finny.economy.Coins
import ru.onefortwo.finny.economy.GameState
import ru.onefortwo.finny.economy.GrowthStage
import ru.onefortwo.finny.economy.chooseGoal
import ru.onefortwo.finny.content.toDomain
import ru.onefortwo.finny.ui.screens.EventDialog
import ru.onefortwo.finny.ui.screens.GrowthCelebrationScreen
import ru.onefortwo.finny.ui.screens.PlanScreen
import ru.onefortwo.finny.ui.screens.SeasonResultScreen
import ru.onefortwo.finny.ui.state.AppState
import ru.onefortwo.finny.ui.state.EventResult
import ru.onefortwo.finny.ui.state.Profile
import ru.onefortwo.finny.ui.state.SeasonExtras
import ru.onefortwo.finny.ui.theme.FinnyTheme

/** Экраны сезона собираются и показывают ключевые сведения. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SeasonScreensTest {

    @get:Rule
    val compose = createComposeRule()

    private val content = ContentRepository(
        AssetSource { name -> File("../content/src/main/assets/$name").readText() },
    )

    private val goal = content.goals().first { it.id == "scooter" }

    private fun state(extras: SeasonExtras, balance: Int = 12, points: Int = 0) = AppState(
        profile = Profile("Финни", PetAppearance("cat", "ginger", "none")),
        game = GameState(balance = Coins(balance), growthPoints = points).chooseGoal(goal.toDomain()),
        isLoaded = true,
        extras = extras,
    )

    @Test
    fun `итоги сезона показывают план и факт и предлагают отложить остаток`() {
        val x = SeasonExtras(planned = true, needsJar = 4, wantsJar = 8, plannedNeeds = 20, plannedWants = 15, plannedSavings = 15, spentNeeds = 16, spentWants = 7, deposited = 15, seasonDone = true)
        var slept = false
        compose.setContent {
            FinnyTheme {
                SeasonResultScreen(state(x), content.petParts(), goal, onTransfer = {}, onPlay = {}, onSleep = { slept = true })
            }
        }
        compose.onNodeWithText("Итоги сезона").assertIsDisplayed()
        compose.onNodeWithText("16 из 20").assertIsDisplayed()
        compose.onNodeWithText("Да, в копилку").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Уложить спать").performScrollTo().performClick()
        compose.mainClock.advanceTimeBy(2000)
        assertTrue(slept)
    }

    @Test
    fun `праздник роста идёт в три сцены`() {
        var done = false
        compose.setContent {
            FinnyTheme {
                GrowthCelebrationScreen(state(SeasonExtras(), points = GrowthStage.TEEN.requiredPoints), content.petParts()) { done = true }
            }
        }
        compose.onNodeWithText("Ура! Ты накопил на целых две мечты!").assertIsDisplayed()
        compose.onNodeWithText("Дальше").performScrollTo().performClick()
        compose.onNodeWithText("Смотри, как я вырос!").assertIsDisplayed()
        compose.onNodeWithText("Дальше").performScrollTo().performClick()
        compose.onNodeWithText("Твой питомец вырос!").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("К новым приключениям!").performScrollTo().performClick()
        assertTrue(done)
    }

    @Test
    fun `окно события — выбор, затем эмоция и подсказка`() {
        val event = content.events().first { it.id == "hunger" }
        val x = SeasonExtras(planned = true, needsJar = 20, wantsJar = 15, eventsDay = 1, dayEvents = listOf("hunger"))
        var answered: Boolean? = null
        compose.setContent {
            FinnyTheme { EventDialog(state(x, balance = 35), content.petParts(), event, null, onAnswer = { answered = it }, onClose = {}) }
        }
        compose.onNodeWithText("Финни проголодался. Покорми его!").assertIsDisplayed()
        compose.onNodeWithText(event.yes).performClick()
        assertEquals(true, answered)
    }

    @Test
    fun `итог события показывает эмоцию и подсказку`() {
        val event = content.events().first { it.id == "hunger" }
        compose.setContent {
            FinnyTheme {
                EventDialog(
                    state(SeasonExtras(planned = true)), content.petParts(), event,
                    EventResult("hunger", true, "Спасибо! Я наелся!", "Корм — это нужное."), onAnswer = {}, onClose = {},
                )
            }
        }
        compose.onNodeWithText("Спасибо! Я наелся!").assertIsDisplayed()
        compose.onNodeWithText("Корм — это нужное.").assertIsDisplayed()
        compose.onNodeWithText("Дальше").assertIsDisplayed()
    }

    @Test
    fun `план сезона — пока монеты не разложены, утвердить нельзя`() {
        compose.setContent {
            FinnyTheme { PlanScreen(game = GameState(balance = Coins(50)), onConfirm = { _, _, _ -> }, onBack = {}, extras = SeasonExtras(), free = 50) }
        }
        compose.onNodeWithText("Осталось распределить: 50 монет").assertIsDisplayed()
        compose.onNodeWithText("Распредели 50 монет по банкам.").assertIsDisplayed()
    }
}
