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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.onefortwo.finny.content.AssetSource
import ru.onefortwo.finny.content.ContentRepository
import ru.onefortwo.finny.content.NumberTask
import ru.onefortwo.finny.content.PetAppearance
import ru.onefortwo.finny.content.TaskAnswer
import ru.onefortwo.finny.content.withNumbers
import ru.onefortwo.finny.economy.GrowthStage
import ru.onefortwo.finny.economy.Difficulty
import ru.onefortwo.finny.ui.state.GameViewModel

/**
 * Сквозная проверка действий пользователя через слой состояния.
 *
 * Повторяет обязательный сценарий из Приложения А: создание профиля,
 * план бюджета, выполнение задания, обязательная и необязательная покупка,
 * попытка покупки при нехватке средств, выбор цели и пополнение копилки,
 * завершение дня и изменение прогресса.
 *
 * Выполняется на JVM: контент читается из файлов модуля `content`,
 * поэтому эмулятор не требуется.
 */
class GameViewModelTest {

    private val content = ContentRepository(
        AssetSource { name -> File("../content/src/main/assets/$name").readText() },
    )

    @Before
    fun setUpMainDispatcher() {
        // Загрузка сохранённого состояния идёт в viewModelScope,
        // поэтому тестам нужен подставной главный диспетчер.
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDownMainDispatcher() {
        Dispatchers.resetMain()
    }

    /** Модель без сохранения: проверяются правила, а не работа с базой. */
    private fun viewModel(): GameViewModel = GameViewModel(content, repository = null).apply {
        createProfile("Финни", PetAppearance("cat", "ginger", "bow"), Difficulty.HARDER)
    }

    @Test
    fun `создание профиля даёт стартовый бюджет и первый день`() {
        val model = viewModel()
        val state = model.state.value

        assertNotNull(state.profile)
        assertEquals("Финни", state.profile?.petName)
        assertEquals(50, state.game.balance.amount)
        assertEquals(1, state.game.period.number)
        assertEquals(GrowthStage.BABY, state.game.stage)
    }

    @Test
    fun `сообщение о стартовом начислении содержит источник и сумму`() {
        val message = viewModel().state.value.message

        assertNotNull(message)
        assertTrue(message!!.text.contains("Стартовые монеты"))
        assertTrue(message.text.contains("50"))
    }

    @Test
    fun `план без выбранной цели не принимает накопления`() {
        val model = viewModel()

        model.confirmPlan(needs = 15, wants = 10, savings = 10)

        assertFalse(model.state.value.game.period.isPlanConfirmed)
        assertTrue(model.state.value.message!!.isProblem)
        assertTrue(model.state.value.message!!.text.contains("цель"))
    }

    @Test
    fun `план сверх бюджета отклоняется с объяснением`() {
        val model = viewModel()

        model.confirmPlan(needs = 40, wants = 20, savings = 0)

        assertFalse(model.state.value.game.period.isPlanConfirmed)
        assertTrue(model.state.value.message!!.text.contains("больше, чем есть"))
    }

    @Test
    fun `подтверждённый план сразу переводит монеты в копилку`() {
        val model = viewModel()
        model.chooseGoal("scooter")

        model.confirmPlan(needs = 15, wants = 12, savings = 10)

        val game = model.state.value.game
        assertTrue(game.period.isPlanConfirmed)
        assertEquals(10, game.savings.saved.amount)
        assertEquals(40, game.balance.amount)
    }

    @Test
    fun `обязательная покупка списывает монеты и повышает заботу`() {
        val model = viewModel()
        model.chooseGoal("scooter")
        model.confirmPlan(needs = 15, wants = 12, savings = 10)

        model.buy("food")

        val game = model.state.value.game
        assertEquals(30, game.balance.amount)
        assertEquals(90, game.pet.care.value)
        assertTrue(model.state.value.message!!.text.contains("Корм"))
    }

    @Test
    fun `покупка при полном показателе не сообщает о росте на ноль`() {
        val model = viewModel()
        model.confirmPlan(needs = 30, wants = 5, savings = 5)

        // Забота упирается в предел после двух кормлений, третья
        // покупка её уже не двигает.
        model.buy("food")
        model.buy("food")
        model.buy("food")

        val text = model.state.value.message!!.text
        assertEquals(100, model.state.value.game.pet.care.value)
        assertFalse("Сообщение: $text", text.contains("выросла на 0"))
        assertTrue("Сообщение: $text", text.contains("и так полная"))
    }

    @Test
    fun `покупка при нехватке монет объясняет разницу и предлагает выход`() {
        val model = viewModel()
        model.chooseGoal("scooter")
        model.confirmPlan(needs = 15, wants = 12, savings = 10)
        model.buy("tent")
        model.buy("food")

        // На балансе 5 монет, мячик стоит 12.
        model.buy("ball")

        val message = model.state.value.message!!
        assertTrue(message.isProblem)
        assertTrue(message.text.contains("Не хватает"))
        assertNotNull(message.nextStep)
        assertEquals(5, model.state.value.game.balance.amount)
    }

    /** Задание с вводом числа и уже подставленными числами. */
    private fun numberTask(id: String): NumberTask =
        content.task(id)!!.withNumbers(Random(1)) as NumberTask

    @Test
    fun `верный ответ приносит награду, повтор — половину`() {
        val model = viewModel()
        val before = model.state.value.game.balance.amount
        // Числа задания переменные, поэтому верный ответ берётся
        // у того же условия, которое передаётся на проверку.
        val task = numberTask("save_rate")

        val first = model.answerTask(task, TaskAnswer.Number(task.answer))

        assertTrue(first.check.isCorrect)
        assertFalse(first.isRepeat)
        assertEquals(10, first.credited.amount)
        assertEquals(before + 10, model.state.value.game.balance.amount)

        // Повтор даёт половину: задание остаётся упражнением, но
        // набирать монеты повторением невыгодно.
        val repeat = model.answerTask(task, TaskAnswer.Number(task.answer))
        assertEquals(before + 15, model.state.value.game.balance.amount)
        assertTrue(model.state.value.message!!.text.contains("половина"))

        // Карточка результата показывает ту же сумму, на которую изменился
        // баланс, а не полную награду источника (ТЗ 2.5.4).
        assertTrue(repeat.isRepeat)
        assertEquals(5, repeat.credited.amount)
    }

    @Test
    fun `повтор ошибочного ответа тоже приносит половину`() {
        val model = viewModel()
        val task = numberTask("save_rate")
        model.answerTask(task, TaskAnswer.Number(task.answer + 3))
        val after = model.state.value.game.balance.amount

        model.answerTask(task, TaskAnswer.Number(task.answer + 3))

        // Награда за старание — 5 монет, половина от неё — 2.
        assertEquals(after + 2, model.state.value.game.balance.amount)
    }

    @Test
    fun `ошибочный ответ тоже даёт монеты и объяснение`() {
        val model = viewModel()
        val before = model.state.value.game.balance.amount

        val task = numberTask("save_rate")
        val answered = model.answerTask(task, TaskAnswer.Number(task.answer + 3))

        assertFalse(answered.check.isCorrect)
        assertTrue(answered.check.explanation.isNotBlank())
        assertEquals(before + 5, model.state.value.game.balance.amount)
    }

    // --- План, покупки и оповещения ---------------------------------------

    @Test
    fun `выполнение задания не отменяет подтверждённый план`() {
        val model = viewModel()
        model.chooseGoal("scooter")
        model.confirmPlan(needs = 15, wants = 12, savings = 10)
        assertTrue(model.state.value.game.period.isPlanConfirmed)

        val task = numberTask("save_rate")
        model.answerTask(task, TaskAnswer.Number(task.answer))

        val period = model.state.value.game.period
        assertTrue("План перестал быть подтверждённым", period.isPlanConfirmed)
        assertTrue("День перестал завершаться", period.canFinish)
    }

    @Test
    fun `без покупки и без копилки план подтверждён, но день не завершить`() {
        val model = viewModel()
        model.confirmPlan(needs = 20, wants = 10, savings = 0)

        val period = model.state.value.game.period
        assertTrue("План должен считаться подтверждённым", period.isPlanConfirmed)
        assertFalse("Решения за день нет, завершать нечего", period.canFinish)
    }

    @Test
    fun `план с копилкой без цели не принимается, и экран об этом узнаёт`() {
        // У нового профиля цели нет, и план с ненулевой копилкой
        // отклоняется. Результат нужен экрану плана: закрываться он должен
        // только принятым планом, иначе отклонённый план выглядит принятым,
        // а введённые суммы пропадают вместе с экраном. Тест
        // «выполнение задания не отменяет подтверждённый план» этот случай
        // не покрывает: там цель выбрана заранее.
        val model = viewModel()

        val accepted = model.confirmPlan(needs = 20, wants = 10, savings = 10)

        assertFalse("План без цели не должен приниматься", accepted)
        assertFalse(model.state.value.game.period.isPlanConfirmed)
        assertTrue(model.state.value.message!!.isProblem)

        model.chooseGoal("scooter")
        assertTrue(model.confirmPlan(needs = 20, wants = 10, savings = 10))
        assertTrue(model.state.value.game.period.isPlanConfirmed)
    }

    @Test
    fun `каждое действие сопровождается отдельным сообщением`() {
        val model = viewModel()
        val texts = mutableListOf<String>()
        fun capture(step: String) {
            val message = model.state.value.message
            assertNotNull("Шаг «$step» прошёл без сообщения", message)
            texts += message!!.text
        }

        model.chooseGoal("scooter")
        capture("выбор цели")
        model.confirmPlan(needs = 20, wants = 10, savings = 10)
        capture("подтверждение плана")
        model.buy("food")
        capture("покупка")
        model.deposit(5)
        capture("пополнение копилки")
        val task = numberTask("save_rate")
        model.answerTask(task, TaskAnswer.Number(task.answer))
        capture("задание")

        assertEquals(5, texts.size)
    }

    @Test
    fun `три покупки одного товара подряд дают три разных сообщения`() {
        // Показатель упирается в предел, поэтому третья покупка его уже
        // не двигает. Сообщения при этом обязаны различаться: одинаковый
        // текст подряд читается как одно оповещение на три списания.
        val model = viewModel()
        model.confirmPlan(needs = 30, wants = 5, savings = 0)
        val before = model.state.value.game.balance.amount

        val texts = (1..3).map {
            model.buy("food")
            model.state.value.message!!.text
        }

        assertEquals("Тексты повторяются дословно: $texts", 3, texts.toSet().size)
        assertEquals(before - 30, model.state.value.game.balance.amount)
        assertEquals(100, model.state.value.game.pet.care.value)
    }

    @Test
    fun `пополнение копилки пересчитывает срок достижения цели`() {
        val model = viewModel()
        model.chooseGoal("scooter")

        model.deposit(15)

        val state = model.state.value
        assertEquals(15, state.game.savings.saved.amount)
        assertTrue(state.message!!.text.contains("копилке"))
    }

    @Test
    fun `снятие показывает изменение суммы и срока до подтверждения`() {
        val model = viewModel()
        model.chooseGoal("scooter")
        model.deposit(30)

        val preview = model.previewWithdrawal(20)

        assertEquals(30, preview.savedBefore.amount)
        assertEquals(10, preview.savedAfter.amount)
        // Состояние до подтверждения не меняется.
        assertEquals(30, model.state.value.game.savings.saved.amount)
    }

    @Test
    fun `день нельзя закончить без плана`() {
        val model = viewModel()

        model.finishPeriod()

        assertTrue(model.state.value.message!!.text.contains("план"))
        assertEquals(1, model.state.value.game.period.number)
    }

    @Test
    fun `день нельзя закончить без единого решения`() {
        val model = viewModel()
        model.chooseGoal("scooter")
        model.confirmPlan(needs = 15, wants = 12, savings = 0)

        model.finishPeriod()

        assertTrue(model.state.value.message!!.text.contains("нет ни одного решения"))
        assertEquals(1, model.state.value.game.period.number)
    }

    @Test
    fun `день с отложенными монетами и без покупок заканчивается`() {
        val model = viewModel()
        model.chooseGoal("scooter")
        // Подтверждение плана сразу переводит долю «Копилки» в накопления,
        // поэтому решение за день уже есть, даже если ничего не куплено.
        model.confirmPlan(needs = 15, wants = 12, savings = 10)

        model.finishPeriod()

        assertEquals(2, model.state.value.game.period.number)
    }

    @Test
    fun `завершение дня начисляет шаги роста и открывает новый день`() {
        val model = viewModel()
        model.chooseGoal("scooter")
        model.confirmPlan(needs = 15, wants = 12, savings = 10)
        model.buy("food")
        model.buy("ball")

        model.finishPeriod()

        val state = model.state.value
        val outcome = state.lastOutcome!!
        assertEquals(3, outcome.earnedPoints)
        assertFalse(outcome.isSetback)
        assertEquals(2, state.game.period.number)
        assertEquals(3, state.game.growthPoints)
    }

    @Test
    fun `обязательный сценарий за пять дней доводит питомца до третьей стадии`() {
        val model = viewModel()
        model.chooseGoal("scooter")

        // План укладывается в карманные монеты периода — 25, поэтому
        // пять дней подряд проходятся без ухода баланса в минус.
        repeat(5) {
            model.confirmPlan(needs = 10, wants = 12, savings = 3)
            model.buy("food")
            model.buy("ball")
            model.finishPeriod()
        }

        val game = model.state.value.game
        assertEquals(6, game.period.number)
        assertEquals(5, game.history.size)
        assertEquals(GrowthStage.ADULT, game.stage)
        assertTrue("Баланс ушёл в минус", game.balance.amount >= 0)
        assertEquals(15, game.savings.saved.amount)
    }

    @Test
    fun `неудачный день сохраняет прогресс и предлагает путь восстановления`() {
        val model = viewModel()
        model.chooseGoal("scooter")

        // Два продуманных дня.
        repeat(2) {
            model.confirmPlan(needs = 15, wants = 12, savings = 10)
            model.buy("food")
            model.buy("ball")
            model.finishPeriod()
        }
        val pointsBefore = model.state.value.game.growthPoints

        // Третий день: только необязательная покупка сверх нулевого плана.
        model.confirmPlan(needs = 0, wants = 0, savings = 0)
        model.buy("tent")
        model.finishPeriod()

        val state = model.state.value
        assertTrue(state.lastOutcome!!.isSetback)
        assertEquals(pointsBefore, state.game.growthPoints)
        assertEquals(GrowthStage.TEEN, state.game.stage)
        assertTrue(state.message!!.isProblem)
        assertNotNull(state.message!!.nextStep)
    }

    @Test
    fun `сброс профиля очищает весь прогресс`() {
        val model = viewModel()
        model.chooseGoal("scooter")
        model.confirmPlan(needs = 15, wants = 12, savings = 10)
        model.buy("food")

        model.resetProfile()

        val state = model.state.value
        assertFalse(state.hasProfile)
        assertEquals(0, state.completedTaskIds.size)
        assertEquals(1, state.game.period.number)
        assertEquals(0, state.game.savings.saved.amount)
    }
}
