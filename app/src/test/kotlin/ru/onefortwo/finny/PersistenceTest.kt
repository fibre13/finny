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
        first.confirmPlan(needs = 15, wants = 12, savings = 10)
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
    }

    @Test
    fun `полученная цель сохраняется и освобождает место следующей`() = runBlocking {
        val first = viewModel()
        first.createProfile("Финни", PetAppearance("cat", "ginger", "bow"), Difficulty.HARDER)
        first.chooseGoal("scooter")
        // Самокат стоит 60, стартовых монет 50: недостающее добирается
        // наградой за задание, как это и происходит в игре.
        first.confirmPlan(needs = 0, wants = 0, savings = 50)
        val task = content.task("save_rate")!!.withNumbers(Random(1)) as NumberTask
        first.answerTask(task, TaskAnswer.Number(task.answer))
        first.deposit(10)

        first.claimGoal()

        assertEquals(setOf("scooter"), first.state.value.achievedGoalIds)
        assertNull("Место цели должно освободиться", first.state.value.game.savings.goal)

        // Повторный запуск: список достигнутых целей читается из базы.
        val second = viewModel()
        assertEquals(setOf("scooter"), second.state.value.achievedGoalIds)
        assertNull(second.state.value.game.savings.goal)
    }

    @Test
    fun `сброс профиля удаляет и полученные цели`() = runBlocking {
        val first = viewModel()
        first.createProfile("Финни", PetAppearance("cat", "ginger", "bow"), Difficulty.HARDER)
        first.chooseGoal("scooter")
        first.confirmPlan(needs = 0, wants = 0, savings = 50)
        val task = content.task("save_rate")!!.withNumbers(Random(1)) as NumberTask
        first.answerTask(task, TaskAnswer.Number(task.answer))
        first.deposit(10)
        first.claimGoal()
        assertEquals(setOf("scooter"), first.state.value.achievedGoalIds)

        first.resetProfile()

        // Сброс удаляет данные сразу, а не при следующем сохранении: в базе
        // не остаётся ни профиля, ни целей прежнего игрока (ТЗ 3.5).
        assertNull(repository.load())
        assertTrue(database.gameDao().achievedGoals().isEmpty())
        first.createProfile("Барсик", PetAppearance("dog", "grey", "none"), Difficulty.SIMPLE)
        assertTrue(viewModel().state.value.achievedGoalIds.isEmpty())
    }

    @Test
    fun `покупки текущего дня и подтверждённый план переживают перезапуск`() = runBlocking {
        val first = viewModel()
        first.createProfile("Финни", PetAppearance("cat", "ginger", "bow"), Difficulty.HARDER)
        first.chooseGoal("scooter")
        first.confirmPlan(needs = 15, wants = 12, savings = 10)
        first.buy("food")

        val after = viewModel().state.value

        assertTrue(after.game.period.isPlanConfirmed)
        assertEquals(1, after.game.period.purchases.size)
        assertEquals("food", after.game.period.purchases.single().itemId)
        assertEquals(10, after.game.period.depositedToSavings.amount)
        assertTrue(after.game.period.canFinish)
    }

    @Test
    fun `история завершённых дней и стадия развития сохраняются`() = runBlocking {
        val first = viewModel()
        first.createProfile("Финни", PetAppearance("cat", "ginger", "bow"), Difficulty.HARDER)
        first.chooseGoal("scooter")

        repeat(5) {
            first.confirmPlan(needs = 10, wants = 12, savings = 3)
            first.buy("food")
            first.buy("ball")
            first.finishPeriod()
            // Карманные на новый день приходят в новые сутки.
            clock = java.time.LocalDate.parse(clock).plusDays(1).toString()
            first.startNewDayIfDue()
        }

        val after = viewModel().state.value

        assertEquals(5, after.game.history.size)
        assertEquals(6, after.game.period.number)
        assertEquals(15, after.game.growthPoints)
        assertEquals(GrowthStage.ADULT, after.game.stage)
        // Прогноз срока цели опирается на пополнения по завершённым дням.
        assertEquals(
            listOf(3, 3, 3, 3, 3),
            after.game.savings.depositsByPeriod.map { it.amount },
        )
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
    fun `сброс тестового профиля возвращает исходное состояние`() = runBlocking {
        val model = viewModel()
        startDemoProfile(model)
        model.confirmPlan(needs = 15, wants = 12, savings = 10)
        model.buy("food")
        model.buy("ball")
        model.finishPeriod()

        assertEquals(2, model.state.value.game.period.number)

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
    fun `демонстрационный режим не ограничен днём и временем`() = runBlocking {
        val model = viewModel()
        startDemoProfile(model)
        model.confirmPlan(needs = 15, wants = 12, savings = 10)
        model.buy("food")
        model.buy("ball")
        model.finishPeriod()

        val state = model.state.value

        // Демонстрационный режим должен проходиться подряд: ни отметка
        // прожитого дня, ни лимит времени на него не действуют (ТЗ 2.5.13).
        assertFalse(state.isDayFinished(TODAY))
        assertFalse(state.isTimeUp(TODAY))
        assertEquals(2, state.game.period.number)
    }

    @Test
    fun `в обычном режиме второй игровой день в те же сутки не начинается`() = runBlocking {
        val model = viewModel()
        model.createProfile("Финни", PetAppearance("cat", "ginger", "bow"), Difficulty.HARDER)
        model.chooseGoal("scooter")
        model.confirmPlan(needs = 15, wants = 12, savings = 10)
        model.buy("food")
        model.buy("ball")
        model.finishPeriod()

        val state = model.state.value

        assertEquals(TODAY, state.lastFinishedDate)
        assertTrue(state.isDayFinished(TODAY))
        // Наступили следующие сутки — игра снова доступна.
        assertFalse(state.isDayFinished("2026-09-16"))
    }

    @Test
    fun `отметка прожитого дня переживает перезапуск`() = runBlocking {
        val first = viewModel()
        first.createProfile("Финни", PetAppearance("cat", "ginger", "bow"), Difficulty.HARDER)
        first.chooseGoal("scooter")
        first.confirmPlan(needs = 15, wants = 12, savings = 10)
        first.buy("food")
        first.finishPeriod()

        val after = viewModel().state.value

        assertEquals(TODAY, after.lastFinishedDate)
        assertTrue(after.isDayFinished(TODAY))
    }

    @Test
    fun `купленная обстановка переживает завершение дня и перезапуск`() = runBlocking {
        val model = viewModel()
        model.createProfile("Финни", PetAppearance("cat", "ginger", "bow"), Difficulty.HARDER)
        model.confirmPlan(needs = 10, wants = 25, savings = 0)
        model.buy("tent")

        assertTrue("Дом не появился после покупки", model.state.value.hasScenery("house"))

        // Покупки периода очищаются при завершении дня, а обстановка —
        // нет: иначе дом исчезал бы наутро после покупки.
        model.finishPeriod()

        assertTrue("Дом пропал после завершения дня", model.state.value.hasScenery("house"))
        assertTrue("Дом не восстановился из базы", viewModel().state.value.hasScenery("house"))
    }

    @Test
    fun `покупка украшения открывает его в гардеробе и сохраняется`() = runBlocking {
        val model = viewModel()
        model.createProfile("Финни", PetAppearance("cat", "ginger", "none"), Difficulty.HARDER)
        model.chooseGoal("scooter")
        model.confirmPlan(needs = 10, wants = 10, savings = 5)

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
