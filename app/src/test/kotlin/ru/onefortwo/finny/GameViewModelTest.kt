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
import ru.onefortwo.finny.content.TaskContent
import ru.onefortwo.finny.content.withNumbers
import ru.onefortwo.finny.economy.GrowthStage
import ru.onefortwo.finny.economy.Difficulty
import ru.onefortwo.finny.ui.state.Explanations
import ru.onefortwo.finny.ui.state.GameViewModel
import ru.onefortwo.finny.ui.state.Season

/**
 * Сквозная проверка действий пользователя через слой состояния.
 *
 * Повторяет обязательный сценарий из Приложения А в механике сезона:
 * создание профиля, план сезона по банкам, выполнение задания, покупка
 * нужного и желаемого, попытка покупки при нехватке средств, выбор цели
 * и пополнение копилки, события и завершение дня, рост питомца.
 *
 * Выполняется на JVM: контент читается из файлов модуля `content`,
 * поэтому эмулятор не требуется.
 */
class GameViewModelTest {

    /** Выход при нехватке всех монет — тот же в лавке и в событиях. */
    private val SHORTAGE_NEXT = "Дождись начала следующего сезона — тебе начислят новые монеты. " +
        "А чтобы ожидание не было скучным — поиграй с питомцем"

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
    /** Дата по часам устройства: сценарии на несколько дней переводят её вперёд. */
    private var day = java.time.LocalDate.of(2026, 9, 28)

    private fun viewModel(): GameViewModel = GameViewModel(
        content,
        repository = null,
        dates = ru.onefortwo.finny.ui.state.DateProvider { day.toString() },
    ).apply {
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
    fun `о стартовых монетах сообщает отдельное окно с суммой`() {
        val arrival = viewModel().state.value.arrival

        assertNotNull(arrival)
        assertTrue(arrival!!.text.contains("50 монет"))
    }

    @Test
    fun `план сезона с копилкой без цели отклоняется с просьбой выбрать цель`() {
        val model = viewModel()

        // Все 50 монет разложены, но цели у нового профиля нет.
        model.confirmPlan(needs = 20, wants = 15, savings = 15)

        val state = model.state.value
        assertFalse(state.extras.planned)
        assertTrue(state.message!!.isProblem)
        assertTrue(state.message!!.text.contains("цель"))
        // Введённые суммы не пропадают: об этом говорит следующий шаг.
        assertTrue(state.message!!.nextStep!!.contains("сохранятся"))
        // Отклонённый план не трогает монеты и не выбирает события дня.
        assertEquals(50, state.game.balance.amount)
        assertEquals(0, state.game.savings.saved.amount)
        assertEquals(0, state.extras.needsJar + state.extras.wantsJar)
        assertTrue(state.extras.dayEvents.isEmpty())
    }

    @Test
    fun `план сезона сверх имеющихся монет отклоняется с объяснением`() {
        val model = viewModel()
        model.chooseGoal("scooter")

        val accepted = model.confirmPlan(needs = 40, wants = 20, savings = 0)

        val state = model.state.value
        assertFalse(accepted)
        assertFalse(state.extras.planned)
        assertTrue(state.message!!.isProblem)
        assertEquals("Разложи все монеты: в плане на 10 монет больше, чем есть", state.message!!.text)
        assertEquals(50, state.game.balance.amount)
        assertEquals(0, state.extras.needsJar + state.extras.wantsJar)
    }

    @Test
    fun `подтверждённый план сообщает, сколько монет сразу ушло в копилку, и записывает план сезона`() {
        val model = viewModel()
        model.chooseGoal("scooter")

        model.confirmPlan(needs = 20, wants = 15, savings = 15)

        val state = model.state.value
        assertEquals("План на сезон готов!", state.message!!.text)
        assertTrue(state.message!!.nextStep!!.contains("15 монет сразу ушли в копилку"))
        // План сезона — основа итогов: с ним сравнивается факт.
        assertEquals(20, state.extras.plannedNeeds)
        assertEquals(15, state.extras.plannedWants)
        assertEquals(15, state.extras.plannedSavings)
        assertEquals(15, state.extras.deposited)
    }

    @Test
    fun `покупка нужного в лавке берётся из банка «Нужное» и повышает заботу`() {
        val model = viewModel()
        model.chooseGoal("scooter")
        model.confirmPlan(needs = 20, wants = 15, savings = 15)
        val careBefore = model.state.value.game.pet.care.value

        model.buy("food")

        val state = model.state.value
        assertEquals(10, state.extras.needsJar)
        assertEquals(15, state.extras.wantsJar)
        assertEquals(25, state.game.balance.amount)
        assertEquals(15, state.game.savings.saved.amount)
        assertEquals(10, state.extras.spentNeeds)
        assertEquals(minOf(100, careBefore + 30), state.game.pet.care.value)
        assertTrue(state.game.pet.care.value > careBefore)
        assertTrue(state.message!!.text.contains("Корм"))
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
    fun `нехватка в банке покрывается другим банком, копилка — только с вопросом, без монет — объяснение и выход`() {
        val model = viewModel()
        model.chooseGoal("scooter")
        model.confirmPlan(needs = 30, wants = 5, savings = 15)

        // «Нужное» и «Хочу» берутся суммарно: мячик стоит 12, в «Хочу» 5 монет —
        // остальные 7 берутся из «Нужного», покупка проходит без вопросов.
        model.buy("ball")

        var state = model.state.value
        assertFalse(state.message!!.isProblem)
        assertNull(state.savingsAsk)
        assertTrue("ball" in state.extras.owned)
        assertEquals(0, state.extras.wantsJar)
        assertEquals(23, state.extras.needsJar)
        assertEquals(23, state.game.balance.amount)
        assertEquals(12, state.extras.spentWants)
        assertEquals(15, state.game.savings.saved.amount)

        // Нужное при пустых банках не списывается молча: сначала вопрос о копилке.
        repeat(2) { model.buy("food") }
        model.buy("food")
        state = model.state.value
        val ask = state.savingsAsk
        assertNotNull("Нет вопроса о копилке", ask)
        assertEquals(3, ask!!.payment.fromNeeds)
        assertEquals(7, ask.payment.fromSavings)
        assertEquals(15, state.game.savings.saved.amount)
        // «Не брать» — копилка и банки не меняются.
        model.cancelSavingsAsk()
        state = model.state.value
        assertNull(state.savingsAsk)
        assertEquals(15, state.game.savings.saved.amount)
        assertEquals(3, state.extras.wantsJar + state.extras.needsJar)

        // Не хватает даже вместе с копилкой: названы все монеты и цена, выход — ждать нового сезона.
        val tent = content.shopItems().first { it.id == "tent" }
        model.buy("tent")
        state = model.state.value
        val message = state.message!!
        assertTrue(message.isProblem)
        assertEquals("Не хватает монет. У тебя всего 18 монет, а «${tent.title}» стоит 25 монет", message.text)
        assertEquals(SHORTAGE_NEXT, message.nextStep)
        assertNull(state.savingsAsk)
        assertEquals(3, state.game.balance.amount)
        assertEquals(15, state.game.savings.saved.amount)
    }

    /** Задание с вводом числа и уже подставленными числами. */
    private fun numberTask(id: String): NumberTask =
        content.task(id)!!.withNumbers(Random(1)) as NumberTask

    /** «Помоги своему питомцу» — единственное задание, за которое платят монеты. */
    private fun recoverHelp(): TaskContent = content.task("recover_help")!!

    /** Отказывается от всех событий дня, в том числе от вставленных продолжений. */
    private fun GameViewModel.declineDay() {
        repeat(6) {
            if (pendingEvent() == null) return
            answerEvent(false)
            closeEventResult(false)
        }
    }

    @Test
    fun `«Помоги своему питомцу» приносит 10 монет, повтор — половину`() {
        val model = viewModel()
        val before = model.state.value.game.balance.amount
        val task = recoverHelp()

        val first = model.answerTask(task, TaskAnswer.Chosen("food"))

        assertTrue(first.check.isCorrect)
        assertFalse(first.isRepeat)
        assertEquals(10, first.credited.amount)
        assertEquals(before + 10, model.state.value.game.balance.amount)

        // Повтор даёт половину: помощь питомцу остаётся путём восстановления,
        // но набирать монеты повторением невыгодно.
        val repeat = model.answerTask(task, TaskAnswer.Chosen("food"))
        assertEquals(before + 15, model.state.value.game.balance.amount)
        assertTrue(model.state.value.message!!.text.contains("половина"))

        // Карточка результата показывает ту же сумму, на которую изменился
        // баланс, а не полную награду источника (ТЗ 2.5.4).
        assertTrue(repeat.isRepeat)
        assertEquals(5, repeat.credited.amount)
    }

    @Test
    fun `ошибочный ответ в «Помоги своему питомцу» монет не приносит, повтор тоже`() {
        val model = viewModel()
        val task = recoverHelp()
        val before = model.state.value.game.balance.amount

        val first = model.answerTask(task, TaskAnswer.Chosen("toy"))
        val repeat = model.answerTask(task, TaskAnswer.Chosen("toy"))

        assertFalse(first.check.isCorrect)
        assertTrue(first.check.explanation.isNotBlank())
        assertEquals(0, first.credited.amount)
        assertTrue(repeat.isRepeat)
        assertEquals(0, repeat.credited.amount)
        assertEquals(before, model.state.value.game.balance.amount)
        // Попытки засчитываются к сюрпризу, как и любые решённые задания.
        assertEquals(2, model.state.value.extras.tasksSolved)
    }

    @Test
    fun `ошибочный ответ даёт объяснение и засчитывается, до сюрприза назван остаток`() {
        val model = viewModel()
        val before = model.state.value.game.balance.amount

        val task = numberTask("save_rate")
        val answered = model.answerTask(task, TaskAnswer.Number(task.answer + 3))

        assertFalse(answered.check.isCorrect)
        assertTrue(answered.check.explanation.isNotBlank())
        assertEquals(before, model.state.value.game.balance.amount)
        val message = model.state.value.message!!
        assertEquals("Задание засчитано. Решено заданий: 1.", message.text)
        val left = Season.tasksToNextSurprise(content.shopItems(), model.state.value.extras, "cat")!!
        assertEquals("До новых товаров в лавке — ${Explanations.tasks(left)}.", message.nextStep)
        assertEquals(1, model.state.value.extras.tasksSolved)
    }

    // --- План, покупки и оповещения ---------------------------------------

    @Test
    fun `задание не меняет план сезона, банки и события дня`() {
        val model = viewModel()
        model.chooseGoal("scooter")
        model.confirmPlan(needs = 20, wants = 15, savings = 15)
        val before = model.state.value.extras
        val pending = model.pendingEvent()

        val task = numberTask("save_rate")
        model.answerTask(task, TaskAnswer.Number(task.answer))

        val after = model.state.value.extras
        assertTrue("План перестал быть подтверждённым", after.planned)
        assertEquals(before.needsJar, after.needsJar)
        assertEquals(before.wantsJar, after.wantsJar)
        assertEquals(before.dayEvents, after.dayEvents)
        assertEquals(before.answered, after.answered)
        assertEquals(pending?.id, model.pendingEvent()?.id)
    }

    @Test
    fun `план без копилки принимается без цели, но день не закончить до решения событий`() {
        val model = viewModel()

        assertTrue(model.confirmPlan(needs = 35, wants = 15, savings = 0))

        val state = model.state.value
        assertTrue("План должен считаться подтверждённым", state.extras.planned)
        assertNull(state.game.savings.goal)
        assertFalse("События дня не решены", state.dayEventsDone)

        model.finishPeriod()

        assertEquals(1, model.state.value.game.period.number)
        val message = model.state.value.message!!
        assertTrue(message.isProblem)
        assertEquals("Сначала реши все события дня.", message.text)
    }

    @Test
    fun `план с копилкой без цели возвращает отказ, после выбора цели те же суммы принимаются`() {
        // У нового профиля цели нет, и план с ненулевой копилкой
        // отклоняется. Результат нужен экрану плана: закрываться он должен
        // только принятым планом, иначе отклонённый план выглядит принятым,
        // а введённые суммы пропадают вместе с экраном.
        val model = viewModel()

        val accepted = model.confirmPlan(needs = 20, wants = 15, savings = 15)

        assertFalse("План без цели не должен приниматься", accepted)
        assertFalse(model.state.value.extras.planned)
        assertTrue(model.state.value.message!!.isProblem)

        model.chooseGoal("scooter")
        assertTrue(model.confirmPlan(needs = 20, wants = 15, savings = 15))
        assertTrue(model.state.value.extras.planned)
        assertEquals(15, model.state.value.game.savings.saved.amount)
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
    fun `без плана сезона событий нет и день не закончить`() {
        val model = viewModel()

        assertNull(model.pendingEvent())
        assertTrue(model.state.value.extras.dayEvents.isEmpty())

        model.finishPeriod()

        assertTrue(model.state.value.message!!.isProblem)
        assertEquals(1, model.state.value.game.period.number)
        assertNull(model.state.value.lastFinishedDate)
    }

    @Test
    fun `день не закончить, пока решены не все события`() {
        val model = viewModel()
        model.chooseGoal("scooter")
        model.confirmPlan(needs = 20, wants = 15, savings = 15)
        val total = model.state.value.extras.dayEvents.size

        // Решены два события из трёх: ответ отказом, продолжений не вставляем.
        repeat(2) {
            model.answerEvent(false)
            model.closeEventResult(false)
        }
        assertTrue(model.state.value.extras.answered < model.state.value.extras.dayEvents.size)
        assertTrue(total >= 3)

        model.finishPeriod()

        assertFalse(model.state.value.dayEventsDone)
        assertEquals("Сначала реши все события дня.", model.state.value.message!!.text)
        assertEquals(1, model.state.value.game.period.number)
    }

    @Test
    fun `день заканчивается после отказов во всех событиях без покупок в лавке`() {
        val model = viewModel()
        model.chooseGoal("scooter")
        // Копилка пополнена планом, в лавке ничего не куплено.
        model.confirmPlan(needs = 20, wants = 15, savings = 15)

        model.declineDay()
        assertTrue(model.state.value.dayEventsDone)
        model.finishPeriod()

        val state = model.state.value
        assertEquals(2, state.game.period.number)
        assertEquals(day.toString(), state.lastFinishedDate)
        assertTrue(state.isSleeping(day.toString()))
        assertTrue(state.message!!.text.contains("спит"))
    }

    @Test
    fun `завершение дня не растит питомца, новый день открывается только в новые сутки`() {
        val model = viewModel()
        model.chooseGoal("scooter")
        model.confirmPlan(needs = 20, wants = 15, savings = 15)
        val pointsBefore = model.state.value.game.growthPoints
        model.declineDay()

        model.finishPeriod()

        // Рост идёт за мечты и задания, а не за прожитые дни.
        assertEquals(pointsBefore, model.state.value.game.growthPoints)
        assertEquals(GrowthStage.BABY, model.state.value.game.stage)

        // В те же сутки питомец спит, событий второго дня нет.
        model.startNewDayIfDue()
        assertNull(model.pendingEvent())
        assertTrue(model.state.value.isSleeping(day.toString()))

        // Новые сутки — второй день сезона со своими событиями.
        nextMorning(model)
        val state = model.state.value
        assertEquals(2, state.seasonDay)
        assertEquals("Новый день! День 2 из 3 в сезоне.", state.message!!.text)
        assertNotNull(model.pendingEvent())
        assertEquals(2, state.extras.eventsDay)
    }

    @Test
    fun `полученные мечты и решённые задания доводят питомца до подростка`() {
        val model = viewModel()
        model.chooseGoal("scooter")

        // Монеты на две мечты (60 + 90) — помощью питомцу: 10 за первый
        // раз и по 5 за повтор. Вместе с 50 стартовыми — 155.
        repeat(20) { model.answerTask(recoverHelp(), TaskAnswer.Chosen("food")) }
        assertEquals(155, model.state.value.game.balance.amount)
        assertTrue(model.state.value.extras.tasksSolved >= 9)
        // Заданий хватает, но без мечт питомец не растёт.
        assertEquals(GrowthStage.BABY, model.state.value.game.stage)

        model.deposit(60)
        model.claimGoal()
        assertEquals(listOf("scooter"), model.state.value.achievedGoalIds.toList())
        assertEquals(GrowthStage.BABY, model.state.value.game.stage)

        model.chooseGoal("aquarium")
        model.deposit(90)
        model.claimGoal()

        val game = model.state.value.game
        assertEquals(2, model.state.value.achievedGoalIds.size)
        assertEquals(GrowthStage.TEEN, game.stage)
        assertEquals(GrowthStage.TEEN.requiredPoints, game.growthPoints)
        assertTrue("Баланс ушёл в минус", game.balance.amount >= 0)
        assertEquals(0, game.savings.saved.amount)
    }

    @Test
    fun `нехватка монет на нужное не отнимает прогресс и предлагает дождаться нового сезона`() {
        val model = viewModel()
        model.confirmPlan(needs = 35, wants = 15, savings = 0)

        // Все 50 монет из банков потрачены на корм: нужное берётся
        // из «Нужного», затем из «Хочу».
        repeat(5) { model.buy("food") }
        assertEquals(0, model.state.value.game.balance.amount)
        assertEquals(15, model.state.value.extras.wantsToNeeds)

        model.buy("food")
        val message = model.state.value.message!!
        assertTrue(message.isProblem)
        val food = content.shopItems().first { it.id == "food" }
        assertEquals("Не хватает монет. У тебя всего 0 монет, а «${food.title}» стоит 10 монет", message.text)
        assertEquals(SHORTAGE_NEXT, message.nextStep)

        // Затратное событие дня без монет — отказ по нехватке с подсказкой.
        val event = model.pendingEvent()!!
        assertTrue(event.cost && event.price > 0)
        model.answerEvent(true)
        val result = model.state.value.eventResult!!
        assertTrue(result.shortage)
        assertFalse(result.accepted)
        assertEquals(
            "Не хватает монет. У тебя всего 0 монет, а это стоит ${Explanations.coins(event.price)}. $SHORTAGE_NEXT",
            result.hint,
        )
        model.closeEventResult(false)

        // Прогресс на месте, путь восстановления — помощь питомцу.
        assertEquals(1, model.state.value.game.period.number)
        assertTrue(model.state.value.extras.planned)
        model.answerTask(recoverHelp(), TaskAnswer.Chosen("food"))
        assertEquals(10, model.state.value.game.balance.amount)
        model.buy("food")
        assertEquals(0, model.state.value.game.balance.amount)
        assertFalse(model.state.value.message!!.isProblem)
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

    /** Новые сутки — начинается следующий игровой день. */
    private fun nextMorning(model: GameViewModel) {
        day = day.plusDays(1)
        model.startNewDayIfDue()
    }
}
