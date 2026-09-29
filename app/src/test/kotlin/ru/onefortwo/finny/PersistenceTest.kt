package ru.onefortwo.finny

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.onefortwo.finny.content.AssetSource
import ru.onefortwo.finny.content.ContentRepository
import ru.onefortwo.finny.content.NumberTask
import ru.onefortwo.finny.content.PetAppearance
import ru.onefortwo.finny.content.TaskAnswer
import ru.onefortwo.finny.content.withNumbers
import ru.onefortwo.finny.data.FinnyDatabase
import ru.onefortwo.finny.data.GameRepository
import ru.onefortwo.finny.economy.GrowthStage
import ru.onefortwo.finny.economy.Difficulty
import ru.onefortwo.finny.ui.state.GameViewModel
import ru.onefortwo.finny.ui.state.Season
import ru.onefortwo.finny.ui.state.SeasonExtras

/**
 * Проверка сохранения состояния между запусками (ТЗ 2.5.13,
 * Приложение А шаги 11 и 12).
 *
 * Используется настоящая база Room в оперативной памяти, поэтому
 * проверяется реальный путь записи и чтения, а не подмена.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PersistenceTest {

    private companion object {
        /** Фиксированная «сегодняшняя» дата: проверки не зависят от часов машины. */
        const val TODAY = "2026-09-15"
    }

    private lateinit var database: FinnyDatabase
    private lateinit var repository: GameRepository

    private val today = TODAY

    private val content = ContentRepository(
        AssetSource { name -> File("../content/src/main/assets/$name").readText() },
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FinnyDatabase::class.java,
        )
            .allowMainThreadQueries()
            // Запросы выполняются в вызывающем потоке, чтобы проверка
            // сохранения была определённой, без ожидания фоновых очередей.
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        repository = GameRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    /** Модель с настоящим хранилищем и фиксированной датой. */
    /** Дата по часам устройства; многодневные сценарии переводят её вперёд. */
    private var clock = TODAY

    private fun viewModel() = GameViewModel(content, repository, dates = { clock })

    /** Следующие календарные сутки. */
    private fun nextDay() {
        clock = java.time.LocalDate.parse(clock).plusDays(1).toString()
    }

    /** Верный ответ на задание с числом. */
    private fun GameViewModel.solve(id: String) {
        val task = content.task(id)!!.withNumbers(Random(1)) as NumberTask
        answerTask(task, TaskAnswer.Number(task.answer))
    }

    /** «Помоги своему питомцу» — единственное задание сезона с монетами. */
    private fun GameViewModel.helpPet() {
        answerTask(content.task("recover_help")!!, TaskAnswer.Chosen("food"))
    }

    /** Отказ во всех событиях дня, вместе с продолжениями отказов: монеты не тратятся. */
    private fun GameViewModel.declineDay() {
        repeat(6) {
            if (pendingEvent() == null) return
            answerEvent(false)
            closeEventResult(false)
        }
    }

    /** Все свободные монеты — в копилку поправкой плана. */
    private fun GameViewModel.saveAllFree() {
        val x = state.value.extras
        assertTrue(confirmPlan(needs = x.needsJar, wants = x.wantsJar, savings = state.value.freeCoins))
    }

    @Test
    fun `выключенные движения сохраняются и переживают сброс профиля`() = runBlocking {
        val first = viewModel()
        assertTrue("по умолчанию движения включены", first.motionEnabled.value)
        first.createProfile("Финни", PetAppearance("cat", "ginger", "none"), Difficulty.HARDER)
        first.setMotionEnabled(false)
        first.resetProfile()

        assertEquals(false, repository.observeDisplaySettings().first()?.motionEnabled)
        // Повторный запуск: новая модель читает настройку с устройства.
        assertFalse(viewModel().motionEnabled.value)
    }

    @Test
    fun `на чистом устройстве профиля нет`() = runBlocking {
        assertNull(repository.load())
    }

    @Test
    fun `созданный профиль сохраняется на устройстве`() = runBlocking {
        val model = viewModel()
        model.createProfile("Мурзик", PetAppearance("dog", "grey", "scarf"), Difficulty.HARDER)

        val saved = repository.load()

        assertNotNull(saved)
        assertEquals("Мурзик", saved!!.petName)
        assertEquals("dog", saved.speciesId)
        assertEquals("grey", saved.colorId)
        assertEquals("scarf", saved.accessoryId)
        assertEquals(50, saved.game.balance.amount)
    }

    @Test
    fun `после повторного запуска прогресс восстанавливается полностью`() = runBlocking {
        val first = viewModel()
        first.createProfile("Финни", PetAppearance("cat", "ginger", "bow"), Difficulty.HARDER)
        first.chooseGoal("scooter")
        assertTrue(first.confirmPlan(needs = 20, wants = 20, savings = 10))
        first.buy("food")
        first.buy("ball")
        val task = content.task("save_rate")!!.withNumbers(Random(1)) as NumberTask
        first.answerTask(task, TaskAnswer.Number(task.answer))

        val before = first.state.value

        // Повторный запуск приложения: новая модель читает ту же базу.
        val second = viewModel()
        val after = second.state.value

        assertTrue(after.isLoaded)
        assertEquals(before.profile?.petName, after.profile?.petName)
        assertEquals(before.game.balance, after.game.balance)
        assertEquals(before.game.savings.saved, after.game.savings.saved)
        assertEquals(before.game.savings.goal?.id, after.game.savings.goal?.id)
        assertEquals(before.game.pet.care.value, after.game.pet.care.value)
        assertEquals(before.game.pet.joy.value, after.game.pet.joy.value)
        assertEquals(before.completedTaskIds, after.completedTaskIds)
        assertEquals(before.extras, after.extras)
    }

    /** Самокат (60 монет) получен: 50 монет сезона и 10 за «Помоги своему питомцу» — в копилку. */
    private fun claimScooter(model: GameViewModel) {
        model.createProfile("Финни", PetAppearance("cat", "ginger", "bow"), Difficulty.HARDER)
        model.dismissArrival()
        model.chooseGoal("scooter")
        assertTrue(model.confirmPlan(needs = 0, wants = 0, savings = 50))
        model.helpPet()
        model.saveAllFree()
        assertEquals(60, model.state.value.game.savings.saved.amount)
        model.claimGoal()
    }

    @Test
    fun `полученная мечта сохраняется и освобождает место следующей`() = runBlocking {
        val first = viewModel()
        claimScooter(first)

        assertEquals(setOf("scooter"), first.state.value.achievedGoalIds)
        assertNull("Место мечты должно освободиться", first.state.value.game.savings.goal)
        assertEquals(0, first.state.value.game.savings.saved.amount)

        // Повторный запуск: список полученных мечт читается из базы.
        val second = viewModel()
        assertEquals(setOf("scooter"), second.state.value.achievedGoalIds)
        assertNull(second.state.value.game.savings.goal)

        // Следующая мечта выбирается на освободившееся место и тоже сохраняется.
        second.chooseGoal("aquarium")
        assertEquals("aquarium", viewModel().state.value.game.savings.goal?.id)
        assertEquals(setOf("scooter"), viewModel().state.value.achievedGoalIds)
    }

    @Test
    fun `встреченные слова и отложенные напоминания переживают перезапуск`() = runBlocking {
        val first = viewModel()
        first.createProfile("Финни", PetAppearance("cat", "ginger", "bow"), Difficulty.HARDER)
        first.chooseGoal("scooter")
        assertTrue(first.confirmPlan(needs = 20, wants = 20, savings = 10))
        first.markTermsRead()
        first.buy("food")
        first.dismissReminder(ru.onefortwo.finny.ui.state.Reminder.TASK)
        val before = first.state.value
        assertTrue("после прочтения новым стало только слово покупки", before.newTerms.isNotEmpty())

        val after = viewModel().state.value
        assertEquals(before.knownTerms, after.knownTerms)
        assertEquals(before.newTerms, after.newTerms)
        assertEquals(setOf("task"), after.dismissedReminders)
        assertEquals(before.game.period.number, after.remindersPeriod)
    }

    @Test
    fun `сброс профиля удаляет полученные мечты и состояние сезона`() = runBlocking {
        val first = viewModel()
        claimScooter(first)
        assertEquals(setOf("scooter"), first.state.value.achievedGoalIds)
        assertTrue(first.state.value.extras.planned)

        first.resetProfile()

        // Сброс удаляет данные сразу, а не при следующем сохранении: в базе
        // не остаётся ни профиля, ни мечт прежнего игрока (ТЗ 3.5).
        assertNull(repository.load())
        assertTrue(database.gameDao().achievedGoals().isEmpty())
        first.createProfile("Барсик", PetAppearance("dog", "grey", "none"), Difficulty.SIMPLE)

        // Новый профиль начинает первый сезон с нуля: план, банки, задания — прежние не переходят.
        val restored = viewModel().state.value
        assertTrue(restored.achievedGoalIds.isEmpty())
        // Первый игровой день уже засчитан словарику: открыта одна карточка.
        assertEquals(SeasonExtras(season = 1, seasonStart = TODAY, wordDay = 1, wordDays = 1), restored.extras)
        assertEquals(50, restored.game.balance.amount)
        assertEquals(0, restored.game.savings.saved.amount)
    }

    @Test
    fun `план сезона, банки, события дня и открытые товары переживают перезапуск`() = runBlocking {
        val first = viewModel()
        first.createProfile("Финни", PetAppearance("cat", "ginger", "bow"), Difficulty.HARDER)
        first.dismissArrival()
        first.chooseGoal("scooter")
        assertTrue(first.confirmPlan(needs = 20, wants = 15, savings = 15))
        // Корм — из банка «Нужное».
        first.buy("food")
        // Первое событие дня решено, остальные ждут решения.
        val firstEvent = first.pendingEvent()!!.id
        first.answerEvent(false)
        first.closeEventResult(false)
        // Три задания открывают сюрприз в лавке.
        repeat(3) { first.solve("save_rate") }

        val before = first.state.value
        assertEquals(10, before.extras.needsJar)
        assertEquals(1, before.extras.answered)
        assertTrue("pants" in before.extras.unlocked)
        val waiting = first.pendingEvent()?.id
        assertNotNull(waiting)

        // Повторный запуск приложения: новая модель читает ту же базу.
        val second = viewModel()
        val after = second.state.value

        assertEquals("состояние сезона восстановлено целиком", before.extras, after.extras)
        // План и банки.
        assertTrue(after.extras.planned)
        assertEquals(20, after.extras.plannedNeeds)
        assertEquals(15, after.extras.plannedWants)
        assertEquals(15, after.extras.plannedSavings)
        assertEquals(10, after.extras.needsJar)
        assertEquals(15, after.extras.wantsJar)
        assertEquals(10, after.extras.spentNeeds)
        assertEquals(25, after.game.balance.amount)
        assertEquals(15, after.game.savings.saved.amount)
        assertEquals(0, after.freeCoins)
        // События дня: те же, решённое не повторяется, ждёт то же следующее.
        assertEquals(1, after.extras.eventsDay)
        assertTrue(after.extras.dayEvents.size >= 3)
        assertEquals(before.extras.dayEvents, after.extras.dayEvents)
        assertTrue(firstEvent in after.extras.usedEvents)
        assertEquals(waiting, second.pendingEvent()?.id)
        assertFalse(after.dayEventsDone)
        // Открытые товары и купленное.
        assertEquals(3, after.extras.tasksSolved)
        assertEquals("pants", after.extras.surprise)
        assertTrue(Season.visibleItems(content.shopItems(), after.extras, "cat").any { it.id == "pants" })
        assertTrue("food" in after.extras.owned)
    }

    @Test
    fun `итоги прожитых дней сезона и стадия роста переживают перезапуск`() = runBlocking {
        val first = viewModel()
        claimScooter(first)
        // Вторая мечта: аквариум за 90 монет — из «Помоги своему питомцу» (повтор — 5 монет).
        first.chooseGoal("aquarium")
        repeat(18) { first.helpPet() }
        first.saveAllFree()
        assertEquals(90, first.state.value.game.savings.saved.amount)
        first.claimGoal()
        // Две мечты и девятнадцать заданий — питомец подрос.
        assertEquals(GrowthStage.TEEN, first.state.value.game.stage)

        // Три дня сезона в обычном режиме: каждый — в свои календарные сутки.
        repeat(3) { index ->
            if (index > 0) {
                nextDay()
                first.startNewDayIfDue()
            }
            first.declineDay()
            first.finishPeriod()
        }
        val before = first.state.value
        assertTrue(before.extras.seasonDone)

        val after = viewModel().state.value

        assertEquals(3, after.game.history.size)
        assertEquals(4, after.game.period.number)
        assertEquals(clock, after.lastFinishedDate)
        assertTrue("итоги сезона ждут показа", after.extras.seasonDone)
        assertEquals(setOf("scooter", "aquarium"), after.achievedGoalIds)
        assertEquals(GrowthStage.TEEN, after.game.stage)
        assertEquals(before.game.growthPoints, after.game.growthPoints)
        assertEquals(19, after.extras.tasksSolved)
        assertEquals(before.extras.unlocked, after.extras.unlocked)
        assertEquals(before.extras.unlockedAt, after.extras.unlockedAt)
        assertEquals(before.extras, after.extras)
    }

    @Test
    fun `сброс профиля удаляет данные с устройства`() = runBlocking {
        val model = viewModel()
        model.createProfile("Финни", PetAppearance("cat", "ginger", "bow"), Difficulty.HARDER)
        model.chooseGoal("scooter")

        model.resetProfile()

        assertNull(repository.load())
        assertFalse(viewModel().state.value.hasProfile)
    }

    /** Включает демонстрационный режим и проходит знакомство, как ребёнок. */
    private fun startDemoProfile(model: GameViewModel) {
        model.startDemo()
        model.createProfile("Пупс", PetAppearance("dog", "white", "none"), Difficulty.HARDER)
    }

    @Test
    fun `демонстрационный режим начинается со знакомства, как обычная игра`() = runBlocking {
        val model = viewModel()
        model.createProfile("Финни", PetAppearance("cat", "ginger", "bow"), Difficulty.SIMPLE)
        model.startDemo()

        // Прежний профиль снят: ребёнок проходит подарок и выбор питомца.
        val pending = model.state.value
        assertFalse(pending.hasProfile)
        assertTrue(pending.demoPending)
        assertNull(repository.load())

        model.createProfile("Пупс", PetAppearance("dog", "white", "none"), Difficulty.HARDER)

        val state = model.state.value
        assertTrue(state.isDemo)
        assertEquals("Пупс", state.profile?.petName)
        assertEquals("dog", state.profile?.appearance?.speciesId)
        assertEquals("scooter", state.game.savings.goal?.id)
        assertEquals(1, state.game.period.number)

        // Признак демонстрационного режима тоже сохраняется.
        assertTrue(repository.load()!!.isDemo)
    }

    @Test
    fun `обычное знакомство создаёт обычный профиль без цели`() {
        val model = viewModel()
        model.createProfile("Финни", PetAppearance("cat", "ginger", "bow"), Difficulty.SIMPLE)

        val state = model.state.value
        assertFalse(state.isDemo)
        assertNull(state.game.savings.goal)
    }

    @Test
    fun `сброс тестового профиля возвращает сезон к началу`() = runBlocking {
        val model = viewModel()
        startDemoProfile(model)
        assertTrue(model.confirmPlan(needs = 20, wants = 15, savings = 15))
        model.buy("food")
        model.declineDay()
        model.finishPeriod()
        repeat(3) { model.solve("save_rate") }

        assertEquals(2, model.state.value.game.period.number)
        assertTrue(model.state.value.extras.unlocked.isNotEmpty())

        model.resetDemo()
        // Сброс возвращает к подарку: знакомство проходится заново.
        assertFalse(model.state.value.hasProfile)
        assertTrue(model.state.value.demoPending)
        model.createProfile("Пупс", PetAppearance("dog", "white", "none"), Difficulty.HARDER)

        val state = model.state.value
        assertTrue(state.isDemo)
        assertEquals(1, state.game.period.number)
        assertEquals(50, state.game.balance.amount)
        assertEquals(0, state.game.savings.saved.amount)
        assertEquals(0, state.game.growthPoints)
        assertTrue(state.game.history.isEmpty())
        assertTrue(state.completedTaskIds.isEmpty())
        // Сезон — с первого дня: план не составлен, банки пусты, сюрпризы закрыты.
        val fresh = SeasonExtras(season = 1, seasonStart = TODAY, wordDay = 1, wordDays = 1)
        assertEquals(fresh, state.extras)
        // И в базе — то же начальное состояние сезона.
        assertEquals(fresh, SeasonExtras.decode(repository.load()!!.extras))
    }

    @Test
    fun `все задания своего класса доступны сразу в демонстрационном режиме`() {
        val model = viewModel()
        startDemoProfile(model)

        val difficulty = model.state.value.difficulty
        val available = content.tasks(difficulty)

        // Порядок открытия не привязан ко времени: доступны сразу все
        // задания выбранного уровня (ТЗ 2.5.8, 2.5.13).
        assertTrue(
            "Заданий уровня ${difficulty.displayName}: ${available.size}",
            available.size >= 6,
        )
        assertTrue(available.all { it.level.suits(difficulty) })
    }

    @Test
    fun `в демонстрационном режиме следующий день сезона начинается сразу, без ожидания суток и предела времени`() = runBlocking {
        val model = viewModel()
        startDemoProfile(model)
        assertTrue(model.confirmPlan(needs = 20, wants = 15, savings = 15))
        model.declineDay()
        model.finishPeriod()

        val state = model.state.value

        // Демонстрационный режим должен проходиться подряд: ни отметка
        // прожитого дня, ни лимит времени на него не действуют (ТЗ 2.5.13).
        assertFalse(state.isDayFinished(TODAY))
        assertFalse(state.isSleeping(TODAY))
        assertFalse(state.copy(usageDate = TODAY, usageMinutes = 25).isTimeUp(TODAY))
        assertEquals(2, state.game.period.number)
        assertEquals(2, state.seasonDay)
        // События второго дня выбраны сразу, в те же сутки.
        assertEquals(2, state.extras.eventsDay)
        assertEquals(3, state.extras.dayEvents.size)
        assertNotNull(model.pendingEvent())
    }

    @Test
    fun `в обычном режиме второй день сезона в те же сутки не начинается`() = runBlocking {
        val model = viewModel()
        model.createProfile("Финни", PetAppearance("cat", "ginger", "bow"), Difficulty.HARDER)
        model.dismissArrival()
        model.chooseGoal("scooter")
        assertTrue(model.confirmPlan(needs = 20, wants = 15, savings = 15))
        model.declineDay()
        model.finishPeriod()

        // Вход в те же сутки: питомец спит, события второго дня не выбираются.
        model.startNewDayIfDue()
        val state = model.state.value
        assertEquals(TODAY, state.lastFinishedDate)
        assertTrue(state.isDayFinished(TODAY))
        assertTrue(state.isSleeping(TODAY))
        assertEquals(1, state.extras.eventsDay)
        assertNull(model.pendingEvent())
        // Закрыть день ещё раз нельзя: второй день в эти сутки не прожить.
        model.finishPeriod()
        assertEquals(2, model.state.value.game.period.number)
        assertEquals(1, model.state.value.game.history.size)

        // Наступили следующие сутки — день второй, события ждут решения.
        nextDay()
        model.startNewDayIfDue()
        val next = model.state.value
        assertFalse(next.isSleeping(clock))
        assertEquals(2, next.extras.eventsDay)
        assertNotNull(model.pendingEvent())
    }

    @Test
    fun `отметка прожитого дня сезона переживает перезапуск, питомец спит до новых суток`() = runBlocking {
        val first = viewModel()
        first.createProfile("Финни", PetAppearance("cat", "ginger", "bow"), Difficulty.HARDER)
        first.dismissArrival()
        first.chooseGoal("scooter")
        assertTrue(first.confirmPlan(needs = 20, wants = 15, savings = 15))
        first.declineDay()
        first.finishPeriod()

        // Повторный запуск в те же сутки: день не начинается заново.
        val second = viewModel()
        second.startNewDayIfDue()
        val after = second.state.value
        assertEquals(TODAY, after.lastFinishedDate)
        assertTrue(after.isDayFinished(TODAY))
        assertTrue(after.isSleeping(TODAY))
        assertEquals(2, after.game.period.number)
        assertNull(second.pendingEvent())

        // Запуск в новые сутки: второй день сезона с событиями, банки прежние.
        nextDay()
        val third = viewModel()
        third.startNewDayIfDue()
        val nextDayState = third.state.value
        assertFalse(nextDayState.isSleeping(clock))
        assertEquals(2, nextDayState.seasonDay)
        assertEquals(after.extras.needsJar, nextDayState.extras.needsJar)
        assertEquals(after.extras.wantsJar, nextDayState.extras.wantsJar)
        assertNotNull(third.pendingEvent())
    }

    @Test
    fun `купленная обстановка переживает завершение дня и перезапуск`() = runBlocking {
        val model = viewModel()
        model.createProfile("Финни", PetAppearance("cat", "ginger", "bow"), Difficulty.HARDER)
        assertTrue(model.confirmPlan(needs = 10, wants = 40, savings = 0))
        model.buy("tent")

        assertTrue("Дом не появился после покупки", model.state.value.hasScenery("house"))

        // Покупки периода очищаются при завершении дня, а обстановка —
        // нет: иначе дом исчезал бы наутро после покупки.
        model.declineDay()
        model.finishPeriod()
        assertNotNull("День не закрылся", model.state.value.lastFinishedDate)

        assertTrue("Дом пропал после завершения дня", model.state.value.hasScenery("house"))
        assertTrue("Дом не восстановился из базы", viewModel().state.value.hasScenery("house"))
    }

    @Test
    fun `покупка украшения открывает его в гардеробе и сохраняется`() = runBlocking {
        val model = viewModel()
        model.createProfile("Финни", PetAppearance("cat", "ginger", "none"), Difficulty.HARDER)
        model.chooseGoal("scooter")
        assertTrue(model.confirmPlan(needs = 20, wants = 20, savings = 10))

        // До покупки украшение недоступно.
        assertFalse(model.state.value.isAccessoryAvailable("scarf"))

        model.buy("scarf")

        assertTrue(model.state.value.isAccessoryAvailable("scarf"))
        assertTrue(viewModel().state.value.isAccessoryAvailable("scarf"))
    }

    @Test
    fun `некупленное украшение надеть нельзя`() = runBlocking {
        val model = viewModel()
        model.createProfile("Финни", PetAppearance("cat", "ginger", "none"), Difficulty.HARDER)

        model.changeAppearance(colorId = "grey", accessoryId = "bow")

        val state = model.state.value
        assertEquals("none", state.profile?.appearance?.accessoryId)
        assertTrue(state.message!!.isProblem)
    }

    @Test
    fun `окрас меняется свободно и сохраняется`() = runBlocking {
        val model = viewModel()
        model.createProfile("Финни", PetAppearance("cat", "ginger", "none"), Difficulty.HARDER)

        model.changeAppearance(colorId = "white", accessoryId = "none")

        assertEquals("white", model.state.value.profile?.appearance?.colorId)
        assertEquals("white", viewModel().state.value.profile?.appearance?.colorId)
    }

    @Test
    fun `счётчик экранного времени переживает сброс профиля`() = runBlocking {
        val model = viewModel()
        model.createProfile("Финни", PetAppearance("cat", "ginger", "bow"), Difficulty.HARDER)
        model.resetTodayUsage()

        model.resetProfile()

        // Счётчик относится к устройству, иначе ограничение обходилось бы
        // пересозданием профиля.
        assertEquals(TODAY, model.state.value.usageDate)
    }

    @Test
    fun `выбранная сложность сохраняется и определяет набор заданий`() = runBlocking {
        val model = viewModel()
        model.createProfile("Финни", PetAppearance("cat", "ginger", "bow"), Difficulty.SIMPLE)

        val restored = viewModel().state.value
        assertEquals(Difficulty.SIMPLE, restored.difficulty)

        val easy = content.tasks(Difficulty.SIMPLE)
        val normal = content.tasks(Difficulty.HARDER)
        assertTrue(easy.any { it.id == "plan_split_easy" })
        assertTrue(easy.none { it.id == "save_rate" })
        assertTrue(normal.any { it.id == "save_rate" })
        assertTrue(normal.none { it.id == "plan_split_easy" })
    }
}
