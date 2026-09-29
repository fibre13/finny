package ru.onefortwo.finny

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.onefortwo.finny.content.AssetSource
import ru.onefortwo.finny.content.ContentRepository
import ru.onefortwo.finny.content.PetAppearance
import ru.onefortwo.finny.content.effectFor
import ru.onefortwo.finny.data.FinnyDatabase
import ru.onefortwo.finny.data.GameRepository
import ru.onefortwo.finny.data.SavedGame
import ru.onefortwo.finny.economy.BudgetCategory
import ru.onefortwo.finny.economy.Coins
import ru.onefortwo.finny.economy.DepositResult
import ru.onefortwo.finny.economy.Difficulty
import ru.onefortwo.finny.economy.GameState
import ru.onefortwo.finny.economy.GrowthStage
import ru.onefortwo.finny.economy.chooseGoal
import ru.onefortwo.finny.economy.deposit
import ru.onefortwo.finny.economy.previewWithdrawal
import ru.onefortwo.finny.content.toDomain
import ru.onefortwo.finny.ui.common.LocalMotionEnabled
import ru.onefortwo.finny.ui.screens.DreamScene
import ru.onefortwo.finny.ui.screens.EventDialog
import ru.onefortwo.finny.ui.screens.GrowthCelebrationScreen
import ru.onefortwo.finny.ui.screens.MissedDialog
import ru.onefortwo.finny.ui.screens.PlanScreen
import ru.onefortwo.finny.ui.screens.SavingsScreen
import ru.onefortwo.finny.ui.screens.SeasonResultScreen
import ru.onefortwo.finny.ui.screens.SurpriseDialog
import ru.onefortwo.finny.ui.screens.YardScreen
import ru.onefortwo.finny.ui.state.AppState
import ru.onefortwo.finny.ui.state.DateProvider
import ru.onefortwo.finny.ui.state.EventResult
import ru.onefortwo.finny.ui.state.GameViewModel
import ru.onefortwo.finny.ui.state.Growth
import ru.onefortwo.finny.ui.state.PetVoice
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
        // План и факт — полосами на карточке «Мой бюджет», без строк «16 из 20».
        compose.onNodeWithText("Мой бюджет").assertIsDisplayed()
        compose.onNodeWithText("16 из 20").assertDoesNotExist()
        compose.onNodeWithContentDescription(
            "${BudgetCategory.NEEDS.displayName}: план 20 монет, факт 16 монет, меньше плана",
        ).performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription(
            "${BudgetCategory.WANTS.displayName}: план 15 монет, факт 7 монет, меньше плана",
        ).performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription(
            "${BudgetCategory.SAVINGS.displayName}: план 15 монет, факт 15 монет, точно по плану",
        ).performScrollTo().assertIsDisplayed()
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
        // «Распредели …» — вверху, остаток — внизу, под тремя блоками.
        compose.onNodeWithText("Распредели 50 монет").assertIsDisplayed()
        compose.onNodeWithText("Осталось распределить: 50 монет").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Утвердить план").performScrollTo().assertIsNotEnabled()
    }

    // --- Новое в 0.9.0 ------------------------------------------------------

    private val today = "2026-09-29"

    /** Двор без движений: разделы открываются сразу, без перебежки питомца. */
    @Composable
    private fun Yard(
        state: AppState,
        onOpenPlan: () -> Unit = {},
        onOpenShop: () -> Unit = {},
        onSleepingTap: () -> Unit = {},
    ) {
        CompositionLocalProvider(LocalMotionEnabled provides false) {
            FinnyTheme {
                YardScreen(
                    state = state, parts = content.petParts(), activeTask = null, goal = goal, today = today,
                    onDismissMessage = {}, onOpenPlan = onOpenPlan, onOpenShop = onOpenShop, onOpenSavings = {},
                    onOpenGlossary = {}, onOpenTasks = {}, onOpenPet = {}, onOpenProgress = {},
                    onOpenHelp = {}, onOpenAdult = {}, onFinishPeriod = {},
                    onSleepingTap = onSleepingTap,
                )
            }
        }
    }

    /** Модель с подменённым главным потоком: как в SeasonTest. */
    private fun withMain(block: () -> Unit) {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            block()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun `когда экранное время вышло, на дворе окно «На сегодня всё!», оно закрывается кнопкой «Пока-пока!»`() {
        val s = state(SeasonExtras(planned = true)).copy(
            usageDate = today, usageMinutes = 20, timeLimitEnabled = true, isDemo = false,
        )
        compose.setContent { Yard(s) }

        compose.onNodeWithText("На сегодня всё!").assertIsDisplayed()
        compose.onNodeWithText("Пока-пока! Увидимся завтра!").assertIsDisplayed()
        compose.onNodeWithText(
            "Экранное время на сегодня закончилось. Приходи завтра — продолжим с того же места.",
        ).assertIsDisplayed()
        compose.onNodeWithText("Пока-пока!").performClick()
        compose.onNodeWithText("На сегодня всё!").assertDoesNotExist()
        compose.onNodeWithText("Пока-пока! Увидимся завтра!").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun `когда питомец спит, план на дворе открывается, а лавка — нет`() {
        val opened = mutableListOf<String>()
        val s = state(SeasonExtras(planned = true)).copy(lastFinishedDate = today, isDemo = false)
        compose.setContent {
            Yard(s, onOpenPlan = { opened += "plan" }, onOpenShop = { opened += "shop" }, onSleepingTap = { opened += "sleeping" })
        }

        compose.onNodeWithText("Финни спит. Новый день начнётся завтра").assertIsDisplayed()
        compose.onNodeWithContentDescription("План сезона").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Покупки: лавка для питомца").performScrollTo().performClick()
        assertEquals(listOf("plan", "sleeping"), opened)
    }

    @Test
    fun `окно «Сюрприз!» называет главный товар и открытые вместе с ним`() {
        val items = content.shopItems()
        val pants = items.first { it.id == "pants" }
        val sausage = items.first { it.id == "sausage" }
        var closed = false
        compose.setContent {
            FinnyTheme { SurpriseDialog(pants, 3, "Финни", onDismiss = { closed = true }, also = listOf(sausage)) }
        }

        compose.onNodeWithText("Сюрприз!").assertIsDisplayed()
        compose.onNodeWithText("Ты выполнил 3 задания!").assertIsDisplayed()
        compose.onNodeWithText("В лавке появилось: «${pants.title}» и «${sausage.title}»").assertIsDisplayed()
        compose.onNodeWithText("Теперь ты сможешь купить их для своего питомца!").assertIsDisplayed()
        compose.onNodeWithText("Ура!").performClick()
        assertTrue(closed)
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun `сцена полученной мечты — заголовок, реплика питомца и кнопка «Дальше» для каждой мечты`() {
        var goalId by mutableStateOf("scooter")
        val done = mutableListOf<String>()
        compose.setContent {
            CompositionLocalProvider(LocalMotionEnabled provides false) {
                FinnyTheme {
                    val g = content.goals().first { it.id == goalId }
                    DreamScene(state(SeasonExtras()), content.petParts(), g.id, g.title) { done += g.id }
                }
            }
        }

        val sayings = mapOf(
            "scooter" to "Юху! Как классно! Спасибо!",
            "aquarium" to "Какие красивые рыбки! Спасибо!",
            "party" to "Ура! Праздник с друзьями! Спасибо!",
        )
        for ((id, saying) in sayings) {
            goalId = id
            compose.waitForIdle()
            val title = content.goals().first { it.id == id }.title
            compose.onNodeWithText("«$title» — твой!").assertIsDisplayed()
            compose.onNodeWithText(saying).assertIsDisplayed()
            compose.onNodeWithText("Дальше").performClick()
        }
        assertEquals(listOf("scooter", "aquarium", "party"), done)
    }

    @Test
    fun `после пропущенного сезона питомец не говорит о монетах — о них отдельная строка`() {
        var season by mutableStateOf(1)
        var closed = false
        compose.setContent {
            FinnyTheme { MissedDialog(state(SeasonExtras(season = season, missed = true)), content.petParts()) { closed = true } }
        }

        compose.onNodeWithText("С возвращением!").assertIsDisplayed()
        // Реплики меняются от сезона к сезону — ни в одной нет слова «монет».
        for (n in 1..5) {
            season = n
            compose.waitForIdle()
            val speech = compose.onNodeWithText("Давай начнём новый сезон?", substring = true).fetchSemanticsNode()
                .config[SemanticsProperties.Text].joinToString("") { it.text }
            assertFalse("Сезон $n: «$speech»", speech.contains("монет", ignoreCase = true))
        }
        compose.onNodeWithText(
            "Пока тебя не было, новые сезоны не начинались и монеты не приходили. " +
                "Заходи регулярно, чтобы не пропускать сезоны.",
        ).assertIsDisplayed()
        compose.onNodeWithText("Давай!").performClick()
        assertTrue(closed)
    }

    @Test
    fun `копилка — при накопленной мечте строка о росте, при выборе второй мечты совет`() {
        val domain = goal.toDomain()
        val reached = (GameState(balance = domain.price).chooseGoal(domain).deposit(domain.price) as DepositResult.Success).state
        var game by mutableStateOf(reached)
        var afterClaim by mutableStateOf(false)
        val line = Growth.claimLine(GrowthStage.BABY, achievedBefore = 0, tasks = 0, petName = "Финни", goalTitle = goal.title)!!
        val tip = Growth.goalTip(GrowthStage.BABY, achieved = 1, petName = "Финни")!!
        compose.setContent {
            FinnyTheme {
                SavingsScreen(
                    game = game,
                    goals = content.goals(),
                    message = null,
                    onDismissMessage = {},
                    onChooseGoal = {},
                    onClaimGoal = {},
                    onPreviewWithdrawal = { game.previewWithdrawal(Coins(it)) },
                    onWithdraw = {},
                    onBack = {},
                    afterClaim = afterClaim,
                    growthLine = if (afterClaim) null else line,
                    goalTip = if (afterClaim) tip else null,
                )
            }
        }

        // Мечта накоплена — праздник и строка «сколько ещё до роста».
        compose.onNodeWithText("Ты сделал это!").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(line).performScrollTo().assertIsDisplayed()

        // Мечта получена: выбор новой цели с советом вместо обычного.
        game = GameState()
        afterClaim = true
        compose.waitForIdle()
        compose.onNodeWithText("Выбери новую цель").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Совет: это твоя вторая мечта! Купи её — и Финни вырастет.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Совет: начни с маленькой мечты — её достичь быстрее.").assertDoesNotExist()
    }

    @Test
    fun `котёнок в событии «Погладить» мурлычет`() = withMain {
        // Событие не подставляется, а встречается само: дни сезона
        // проходятся подряд в демо, пока pickDay не выберет «Погладить».
        val model = GameViewModel(content, repository = null, dates = DateProvider { today }).apply {
            startDemo()
            createProfile("Финни", PetAppearance("cat", "ginger", "none"), Difficulty.SIMPLE)
            dismissArrival()
            if (state.value.game.savings.goal == null) chooseGoal("scooter")
        }
        assertTrue(model.confirmPlan(20, 15, 15))
        var found = false
        for (step in 0 until 300) {
            val st = model.state.value
            if (!st.extras.planned && !st.extras.seasonDone) {
                val available = st.freeCoins + st.extras.needsJar + st.extras.wantsJar
                model.confirmPlan(available / 2, available - available / 2, 0)
            }
            val event = model.pendingEvent()
            if (event?.id == "pet_stroke") {
                model.answerEvent(true)
                val result = model.state.value.eventResult!!
                assertTrue(result.accepted)
                assertEquals("Мррр… Мрррр… Как приятно!", result.emotion)
                found = true
                break
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
        assertTrue("Событие «Погладить» не встретилось", found)
    }

    @Test
    fun `подросток говорит по-подростковому — и в итоге события`() = withMain {
        assertEquals("Круто! Хай!", PetVoice.of(GrowthStage.TEEN, "Ура! Привет!"))
        assertEquals("Ура! Привет!", PetVoice.of(GrowthStage.BABY, "Ура! Привет!"))

        // Профиль подростка с решаемым событием «голод» читается из базы.
        val database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), FinnyDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        try {
            val repository = GameRepository(database)
            val extras = SeasonExtras(
                seasonStart = today, planned = true, needsJar = 20, wantsJar = 15,
                plannedNeeds = 20, plannedWants = 15, eventsDay = 1, dayEvents = listOf("hunger"),
                growthShown = GrowthStage.TEEN,
            )
            runBlocking {
                repository.save(
                    SavedGame(
                        petName = "Финни", speciesId = "cat", colorId = "ginger", accessoryId = "none",
                        isDemo = false,
                        game = GameState(balance = Coins(35), growthPoints = GrowthStage.TEEN.requiredPoints).chooseGoal(goal.toDomain()),
                        completedTaskIds = emptySet(),
                        extras = extras.encode(),
                    ),
                )
            }
            val model = GameViewModel(content, repository, dates = DateProvider { today })
            assertEquals(GrowthStage.TEEN, model.state.value.game.stage)
            val event = model.pendingEvent()!!
            assertEquals("hunger", event.id)
            model.answerEvent(true)
            val emotion = model.state.value.eventResult?.emotion
            assertNotEquals(event.emotionYes, emotion)
            assertEquals("Супер, спасибо! Я наелся! Вкуснотища!", emotion)
        } finally {
            database.close()
        }
    }
}
