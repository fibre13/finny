package ru.onefortwo.finny

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.onNodeWithContentDescription
import java.io.File
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.onefortwo.finny.economy.PurchaseRecord
import org.junit.Assert.assertEquals
import ru.onefortwo.finny.ui.state.GuideStep
import ru.onefortwo.finny.ui.state.Reminder
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.onefortwo.finny.content.AllocateTask
import ru.onefortwo.finny.content.AssetSource
import ru.onefortwo.finny.content.ChoiceTask
import ru.onefortwo.finny.content.ContentRepository
import ru.onefortwo.finny.content.NumberTask
import ru.onefortwo.finny.content.PetAppearance
import ru.onefortwo.finny.content.TaskAnswer
import ru.onefortwo.finny.content.TaskCheck
import ru.onefortwo.finny.content.TaskQueue
import ru.onefortwo.finny.content.TaskTopic
import ru.onefortwo.finny.content.toDomain
import ru.onefortwo.finny.economy.BudgetCategory
import ru.onefortwo.finny.economy.BudgetPlan
import ru.onefortwo.finny.economy.Coins
import ru.onefortwo.finny.economy.Difficulty
import ru.onefortwo.finny.economy.GameState
import ru.onefortwo.finny.economy.IncomeSource
import ru.onefortwo.finny.economy.PeriodCompletion
import ru.onefortwo.finny.economy.PlanConfirmation
import ru.onefortwo.finny.economy.chooseGoal
import ru.onefortwo.finny.economy.buy
import ru.onefortwo.finny.economy.confirmPlan
import ru.onefortwo.finny.economy.DepositResult
import ru.onefortwo.finny.economy.deposit
import ru.onefortwo.finny.economy.finishPeriod
import ru.onefortwo.finny.economy.previewWithdrawal
import ru.onefortwo.finny.economy.StatLevel
import ru.onefortwo.finny.ui.common.FeedbackCard
import ru.onefortwo.finny.ui.common.LocalMotionEnabled
import ru.onefortwo.finny.ui.common.MotionFrame
import ru.onefortwo.finny.ui.common.rememberPetMotion
import ru.onefortwo.finny.ui.screens.AdultScreen
import ru.onefortwo.finny.ui.screens.GiftScreen
import ru.onefortwo.finny.ui.screens.GlossaryScreen
import ru.onefortwo.finny.ui.state.PetReaction
import ru.onefortwo.finny.ui.state.PetReactions
import ru.onefortwo.finny.ui.screens.OnboardingScreen
import ru.onefortwo.finny.ui.screens.PeriodResultScreen
import ru.onefortwo.finny.ui.screens.PetSetupScreen
import ru.onefortwo.finny.ui.screens.PlanScreen
import ru.onefortwo.finny.ui.screens.SavingsScreen
import ru.onefortwo.finny.ui.screens.ShopScreen
import ru.onefortwo.finny.ui.screens.TaskDetailScreen
import ru.onefortwo.finny.ui.screens.TasksScreen
import ru.onefortwo.finny.ui.screens.YardScreen
import ru.onefortwo.finny.ui.state.AnsweredTask
import ru.onefortwo.finny.ui.state.AppState
import ru.onefortwo.finny.ui.state.FeedbackMessage
import ru.onefortwo.finny.ui.state.Profile
import ru.onefortwo.finny.ui.state.SeasonExtras
import ru.onefortwo.finny.ui.theme.FinnyTheme

/**
 * Проверки отрисовки экранов. Выполняются на JVM через Robolectric,
 * поэтому не требуют устройства или эмулятора.
 *
 * Цель — убедиться, что экраны собираются и показывают ключевые сведения,
 * то есть в обязательном сценарии нет падений и пустых экранов (ТЗ 3.4).
 *
 * Экраны прокручиваются, поэтому часть элементов при открытии находится ниже
 * видимой области: такие элементы проверяются после прокрутки к ним.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ScreenRenderTest {

    @get:Rule
    val compose = createComposeRule()

    private val content = ContentRepository(
        AssetSource { name -> File("../content/src/main/assets/$name").readText() },
    )

    private val profile = Profile("Финни", PetAppearance("cat", "ginger", "bow"))

    @Test
    fun `экран знакомства показывает три типа решений`() {
        compose.setContent {
            FinnyTheme { OnboardingScreen(onContinue = {}) }
        }

        compose.onNodeWithText("1. Купить нужное").assertIsDisplayed()
        compose.onNodeWithText("2. Купить желаемое").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("3. Отложить в копилку").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `подарок называет цель и три решения и открывается кнопкой`() {
        var opened = false
        compose.setContent {
            CompositionLocalProvider(LocalMotionEnabled provides false) {
                FinnyTheme { GiftScreen(onOpen = { opened = true }) }
            }
        }

        // ТЗ 2.5.1: цель игры и три типа решений.
        compose.onNodeWithText("на нужное — чтобы был сыт").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("на «Хочу» — чтобы радовался").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("в копилку — на большую мечту").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Шаг 1 из 3").assertExists()
        compose.onNodeWithText("Открыть подарок").performScrollTo().performClick()
        assertTrue(opened)
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun `шаги знакомства помещаются на телефоне 360 на 800 без прокрутки`() {
        // Эталонный телефон 720 × 1600, 320 dpi: под строкой состояния
        // (24 dp) и системной панелью (48 dp) экрану остаётся 728 dp.
        compose.setContent {
            FinnyTheme {
                Box(modifier = Modifier.height(728.dp)) {
                    PetSetupScreen(parts = content.petParts(), onDone = { _, _, _ -> })
                }
            }
        }
        val limit = with(compose.density) { 728.dp.toPx() }
        fun assertFits(button: String) {
            val bottom = compose.onNodeWithText(button).fetchSemanticsNode().boundsInRoot.bottom
            assertTrue("Кнопка «$button» ниже края экрана: $bottom > $limit", bottom <= limit)
        }

        // Шаг 2 — кто будет другом.
        assertFits("Далее")
        compose.onNodeWithText("Далее").performClick()
        // Шаг 3 до выбора — худший случай: под кнопкой причина её недоступности.
        // Положение причины здесь не проверяется: Robolectric отводит строке
        // текста около 35 px, и высоты получаются больше, чем на устройстве.
        assertFits("Далее")
        compose.onNodeWithText("Придумай имя и выбери задания.").assertExists()

        compose.onNodeWithText("Попроще").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("Финни")
        compose.onNodeWithText("Далее").performClick()
        // Финал: питомец представляется в облачке по имени, отдельной строки с именем нет, кнопка «Давай играть».
        compose.onNodeWithText("Привет-привет! Я — Финни! Рад видеть тебя").assertExists()
        compose.onNodeWithText("Финни").assertDoesNotExist()
        assertFits("Давай играть")
    }

    @Test
    fun `недопустимое имя стирается, показывается ошибка, дальше не пройти`() {
        compose.setContent {
            FinnyTheme {
                PetSetupScreen(parts = content.petParts(), onDone = { _, _, _ -> }, nameFilter = content.nameFilter())
            }
        }
        compose.onNodeWithText("Далее").performScrollTo().performClick()
        compose.onNodeWithText("Попроще").performScrollTo().performClick()

        // Корень «дурак» стирается сразу, на вводе.
        compose.onNode(hasSetTextAction()).performTextInput("Дурак")
        compose.onNodeWithText("Так не принято называть питомцев. Придумай другое имя.").performScrollTo().assertIsDisplayed()
        compose.onNode(hasSetTextAction()).assert(
            androidx.compose.ui.test.SemanticsMatcher.expectValue(
                androidx.compose.ui.semantics.SemanticsProperties.EditableText,
                androidx.compose.ui.text.AnnotatedString(""),
            ),
        )
        compose.onNodeWithText("Далее").performScrollTo().assertIsNotEnabled()

        // Нормальное имя принимается, ошибка уходит.
        compose.onNode(hasSetTextAction()).performTextInput("Барсик")
        compose.onNodeWithText("Так не принято называть питомцев. Придумай другое имя.").assertDoesNotExist()
        compose.onNodeWithText("Далее").performScrollTo().assertIsEnabled()
    }

    @Test
    fun `знакомство спрашивает сложность и имя до создания питомца`() {
        compose.setContent {
            FinnyTheme { PetSetupScreen(parts = content.petParts(), onDone = { _, _, _ -> }) }
        }

        // Шаг 2 — внешность, шаг 3 — имя и сложность заданий.
        compose.onNodeWithText("Далее").performScrollTo().performClick()

        compose.onNodeWithText("Какие задания?").performScrollTo().assertIsDisplayed()
        // Поле имени названо для программы чтения с экрана своей подписью.
        compose.onNodeWithContentDescription("Имя питомца").performScrollTo().assertIsDisplayed()
        // Подсказка ТЗ 3.5: настоящее имя не нужно.
        compose.onNodeWithText("Придумай игровое имя — настоящее писать не нужно.").performScrollTo().assertIsDisplayed()
        // Пока сложность и имя не выбраны, дальше пройти нельзя.
        compose.onNodeWithText("Далее").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Придумай имя и выбери задания.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `экран создания питомца показывает варианты внешности`() {
        compose.setContent {
            FinnyTheme { PetSetupScreen(parts = content.petParts(), onDone = { _, _, _ -> }) }
        }

        compose.onNodeWithText("Кто будет твоим другом?").assertIsDisplayed()
        // Виды — картинками, названия озвучиваются.
        listOf("Котёнок", "Щенок", "Крольчонок").forEach {
            compose.onNodeWithContentDescription(it).performScrollTo().assertIsDisplayed()
        }
        // Цвета — кружками без подписей, но TalkBack их называет.
        compose.onNodeWithText("Окрас").assertDoesNotExist()
        compose.onNodeWithText("Серый").assertDoesNotExist()
        compose.onNodeWithContentDescription("Серый", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `без цели план предлагает её выбрать и объясняет, почему кнопка неактивна`() {
        // Без цели откладывать некуда: об этом сказано на карточке копилки
        // заранее, с переходом к выбору цели, а не после отказа.
        var opened = false
        compose.setContent {
            FinnyTheme {
                PlanScreen(
                    game = GameState.newProfile(),
                    onConfirm = { _, _, _ -> },
                    onBack = {},
                    onChooseGoal = { opened = true },
                )
            }
        }

        compose.onNodeWithText("Выбрать цель").performScrollTo().performClick()
        assertTrue("Переход к выбору цели не сработал", opened)
    }

    @Test
    fun `копилка без цели выключает утверждение плана сезона и называет причину`() {
        compose.setContent {
            FinnyTheme {
                PlanScreen(game = GameState.newProfile(), onConfirm = { _, _, _ -> }, onBack = {}, extras = SeasonExtras())
            }
        }

        // Кнопка ищется по описанию для программы чтения с экрана:
        // у трёх банков одинаковые подписи «+5». Все 50 монет — в копилку.
        repeat(10) { compose.onNodeWithContentDescription("Копим на мечту: прибавить 5").performScrollTo().performClick() }

        // Остатка нет, но без цели копилка не принимается: причина — вместо остатка.
        compose.onNodeWithText("Осталось распределить", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Чтобы отложить в копилку, сначала выбери цель").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Цель не выбрана").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Утвердить план").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun `суммы плана сезона урезаются, если монет стало меньше, пока план открыт`() {
        // Раскладываются свободные монеты и банки. Если их стало меньше, пока
        // план открыт (например, монеты ушли на событие), суммы банков
        // урезаются и не остаются больше всего, что есть у ребёнка.
        val goal = content.goals().first { it.id == "scooter" }.toDomain()
        val game = GameState.newProfile().chooseGoal(goal)
        var free by mutableStateOf(50)
        compose.setContent {
            FinnyTheme {
                PlanScreen(game = game, onConfirm = { _, _, _ -> }, onBack = {}, extras = SeasonExtras(), free = free)
            }
        }

        repeat(4) { compose.onNodeWithContentDescription("Нужное: прибавить 5").performScrollTo().performClick() }
        // Сумма банка выводится один раз — на его карточке; остаток — крупной строкой под тремя блоками.
        compose.onAllNodesWithText("20 монет").assertCountEquals(1)
        compose.onNodeWithText("Осталось распределить: 30 монет").performScrollTo().assertIsDisplayed()

        free = 10
        compose.waitForIdle()

        compose.onAllNodesWithText("20 монет").assertCountEquals(0)
        compose.onAllNodesWithText("10 монет").assertCountEquals(1)
        compose.onNodeWithText("Распредели 10 монет").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Всё распределено!").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `кнопки шага не выводят сумму за пределы бюджета`() {
        // Бюджет нового профиля — 50 монет.
        compose.setContent {
            FinnyTheme {
                PlanScreen(game = GameState.newProfile(), onConfirm = { _, _, _ -> }, onBack = {})
            }
        }

        // У нуля убавлять некуда: кнопки «минус» выключены.
        compose.onNodeWithContentDescription("Нужное: убавить на 1").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Нужное: убавить на 5").assertIsNotEnabled()

        repeat(10) { compose.onNodeWithContentDescription("Нужное: прибавить 5").performScrollTo().performClick() }
        compose.onAllNodesWithText("50 монет").assertCountEquals(1)
        // Весь бюджет: прибавлять некуда.
        compose.onNodeWithContentDescription("Нужное: прибавить 5").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Нужное: прибавить 1").assertIsNotEnabled()

        compose.onNodeWithContentDescription("Нужное: убавить на 1").performScrollTo().performClick()
        compose.onAllNodesWithText("49 монет").assertCountEquals(1)
    }

    @Test
    fun `задание на распределение решается кнопками шага`() {
        // Базовая сумма задания — 10 монет, без переменных чисел.
        val task = content.task("plan_split_easy") as AllocateTask
        var answer: TaskAnswer? = null
        compose.setContent {
            FinnyTheme {
                TaskDetailScreen(
                    task = task,
                    onAnswer = {
                        answer = it
                        AnsweredTask(
                            check = TaskCheck(true, "Объяснение", reward = IncomeSource.TASK_CORRECT),
                            credited = Coins(10),
                            isRepeat = false,
                        )
                    },
                    onBack = {},
                )
            }
        }

        compose.onNodeWithText("Ответить").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithContentDescription("Нужное: прибавить 5").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Хочу: прибавить 1").performScrollTo().performClick()
        repeat(4) {
            compose.onNodeWithContentDescription("Копим на мечту: прибавить 1").performScrollTo().performClick()
        }

        compose.onNodeWithText("Все монеты распределены").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Ответить").performScrollTo().performClick()
        assertEquals(TaskAnswer.Allocation(needs = 5, wants = 1, savings = 4), answer)
    }

    @Test
    fun `числовой ответ набирается экранной цифровой панелью`() {
        val task = content.task("save_gap_easy") as NumberTask
        var answer: TaskAnswer? = null
        compose.setContent {
            FinnyTheme {
                TaskDetailScreen(
                    task = task,
                    onAnswer = {
                        answer = it
                        AnsweredTask(
                            check = TaskCheck(true, "Объяснение", reward = IncomeSource.TASK_CORRECT),
                            credited = Coins(10),
                            isRepeat = false,
                        )
                    },
                    onBack = {},
                )
            }
        }

        // Системного поля ввода нет: ответ набирается кнопками.
        compose.onNode(hasSetTextAction()).assertDoesNotExist()
        compose.onNodeWithText("Ответить").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Стереть").performScrollTo().assertIsNotEnabled()

        compose.onNodeWithText("5").performScrollTo().performClick()
        compose.onNodeWithText("7").performScrollTo().performClick()
        compose.onNodeWithText("57").assertIsDisplayed()

        compose.onNodeWithText("Стереть").performScrollTo().performClick()
        // «5» теперь и на клавише, и в строке ответа.
        compose.onAllNodesWithText("5").assertCountEquals(2)
        compose.onNodeWithText("3").performScrollTo().performClick()

        compose.onNodeWithText("Ответить").performScrollTo().performClick()
        assertEquals(TaskAnswer.Number(53), answer)
    }

    @Test
    fun `ведущий ноль в ответе не копится`() {
        val task = content.task("save_gap_easy") as NumberTask
        var answer: TaskAnswer? = null
        compose.setContent {
            FinnyTheme {
                TaskDetailScreen(
                    task = task,
                    onAnswer = {
                        answer = it
                        AnsweredTask(
                            check = TaskCheck(true, "Объяснение", reward = IncomeSource.TASK_CORRECT),
                            credited = Coins(10),
                            isRepeat = false,
                        )
                    },
                    onBack = {},
                )
            }
        }

        // Без защиты строка стала бы «000», а «8» отсеклась бы пределом в три цифры.
        repeat(3) { compose.onNode(hasText("0") and hasClickAction()).performScrollTo().performClick() }
        compose.onNodeWithText("8").performScrollTo().performClick()
        // «8» видно дважды: на клавише и в строке ответа; «08» и «008» нет.
        compose.onAllNodesWithText("8").assertCountEquals(2)
        compose.onNodeWithText("08").assertDoesNotExist()
        compose.onNodeWithText("Ответить").performScrollTo().performClick()
        assertEquals(TaskAnswer.Number(8), answer)
    }

    @Test
    fun `с выбранной целью план не предлагает выбирать её снова`() {
        val goal = content.goals().first { it.id == "scooter" }.toDomain()
        compose.setContent {
            FinnyTheme {
                PlanScreen(
                    game = GameState.newProfile().chooseGoal(goal),
                    onConfirm = { _, _, _ -> },
                    onBack = {},
                )
            }
        }

        compose.onNodeWithText("Выбрать цель").assertDoesNotExist()
    }

    @Test
    fun `карточка результата показывает фактически начисленную сумму за повтор`() {
        // За повтор начисляется половина. Карточка обязана назвать ту же
        // сумму, на которую изменился баланс, а не полную награду (ТЗ 2.5.4).
        val task = content.task("save_temptation") as ChoiceTask
        compose.setContent {
            FinnyTheme {
                TaskDetailScreen(
                    task = task,
                    onAnswer = {
                        AnsweredTask(
                            check = TaskCheck(
                                isCorrect = true,
                                explanation = "Объяснение",
                                reward = IncomeSource.TASK_CORRECT,
                            ),
                            credited = Coins(5),
                            isRepeat = true,
                        )
                    },
                    onBack = {},
                )
            }
        }

        compose.onNodeWithText(task.options.first().title).performScrollTo().performClick()
        compose.onNodeWithText("Ответить").performScrollTo().performClick()

        compose.onNodeWithText(
            "Награда за задание ещё раз: +5 монет, за повтор — половина награды",
        )
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("+10 монет", substring = true).assertDoesNotExist()
    }

    @Test
    fun `итоги дня говорят, что план на новый день можно составить сразу`() {
        // Монеты следующего дня начисляются при завершении текущего, и план
        // на него можно составить сразу. Экран итогов не должен отсылать
        // за монетами и планом «на завтра».
        val goal = content.goals().first { it.id == "scooter" }.toDomain()
        val planned = GameState.newProfile().chooseGoal(goal)
            .confirmPlan(BudgetPlan(Coins(20), Coins(10), Coins(5))) as PlanConfirmation.Success
        val finished = planned.state.finishPeriod() as PeriodCompletion.Success

        compose.setContent {
            FinnyTheme {
                PeriodResultScreen(
                    petName = "Финни",
                    outcome = finished.outcome,
                    nextDayTomorrow = true,
                    onOpenRecoveryTask = {},
                    onContinue = {},
                )
            }
        }

        compose.onNodeWithText(
            "План на новый день можно составить уже сейчас, а закончить этот день получится завтра.",
        )
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `экран плана показывает три банка и остаток, утвердить можно, когда разложено всё`() {
        val goal = content.goals().first { it.id == "scooter" }.toDomain()
        val confirmed = mutableListOf<Triple<Int, Int, Int>>()
        compose.setContent {
            FinnyTheme {
                PlanScreen(
                    game = GameState.newProfile().chooseGoal(goal),
                    onConfirm = { n, w, s -> confirmed += Triple(n, w, s) },
                    onBack = {},
                    extras = SeasonExtras(),
                )
            }
        }

        compose.onNodeWithText("Нужное").assertIsDisplayed()
        compose.onNodeWithText("Хочу").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Копим на мечту").performScrollTo().assertIsDisplayed()
        repeat(4) { compose.onNodeWithContentDescription("Нужное: прибавить 5").performScrollTo().performClick() }
        repeat(3) { compose.onNodeWithContentDescription("Хочу: прибавить 5").performScrollTo().performClick() }
        // Остаток назван числом; пока он есть, план не утвердить: «на всякий случай» монеты не остаются.
        compose.onNodeWithText("Осталось распределить: 15 монет").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Утвердить план").performScrollTo().assertIsNotEnabled()

        repeat(3) { compose.onNodeWithContentDescription("Копим на мечту: прибавить 5").performScrollTo().performClick() }
        compose.onNodeWithText("Всё распределено!").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Утвердить план").performScrollTo().assertIsEnabled().performClick()
        assertEquals(listOf(Triple(20, 15, 15)), confirmed)
    }

    @Test
    fun `пустое направление плана требует подтверждения`() {
        val confirmed = mutableListOf<Triple<Int, Int, Int>>()
        compose.setContent {
            FinnyTheme {
                PlanScreen(game = GameState.newProfile(), onConfirm = { n, w, s -> confirmed += Triple(n, w, s) }, onBack = {})
            }
        }

        // Раскладываются все монеты: 50 в «Нужное», «Хочу» и копилка пустые.
        repeat(10) { compose.onNodeWithContentDescription("Нужное: прибавить 5").performScrollTo().performClick() }
        compose.onNodeWithText("Утвердить план").performScrollTo().performClick()
        compose.onNodeWithText("Ты уверен? Если не отложишь на «Хочу», ты не сможешь порадовать питомца.").assertIsDisplayed()
        compose.onNodeWithText("Вернуться к плану").performClick()
        assertTrue(confirmed.isEmpty())
        compose.onNodeWithText("Утвердить план").performScrollTo().performClick()
        compose.onNodeWithText("Да, я уверен").performClick()
        assertEquals(listOf(Triple(50, 0, 0)), confirmed)
    }

    @Test
    fun `покупка показывает цену и влияние на питомца до списания монет`() {
        compose.setContent {
            FinnyTheme {
                ShopScreen(
                    items = content.shopItems(),
                    pet = GameState.newProfile().pet,
                    balance = GameState.newProfile().balance,
                    message = null,
                    onDismissMessage = {},
                    onBuy = {},
                    onBack = {},
                )
            }
        }

        // В строке товара — название и цена на кнопке; влияние на питомца
        // показывает окно подтверждения, до списания монет (ТЗ 2.5.6).
        compose.onNodeWithText("Корм").assertIsDisplayed()
        compose.onNodeWithContentDescription("Купить Корм за 10 монет").performClick()
        compose.onNodeWithText("Цена: 10 монет.").assertIsDisplayed()
        compose.onNodeWithText("Это нужное.").assertIsDisplayed()
        compose.onNodeWithText("Финни будет сытым").assertIsDisplayed() // имя по умолчанию
    }

    @Test
    fun `экран копилки предлагает выбрать цель`() {
        compose.setContent {
            FinnyTheme {
                SavingsScreen(
                    game = GameState.newProfile(),
                    goals = content.goals(),
                    message = null,
                    onDismissMessage = {},
                    onChooseGoal = {},
                    onClaimGoal = {},
                    onPreviewWithdrawal = { GameState.newProfile().previewWithdrawal(Coins(0)) },
                    onWithdraw = {},
                    onBack = {},
                )
            }
        }

        compose.onNodeWithText("Выбери цель").assertIsDisplayed()
    }

    @Test
    fun `копилка без блоков «Отложить» и «Взять» — пополнение через план сезона`() {
        val goal = content.goals().first { it.id == "scooter" }.toDomain()
        val empty = GameState.newProfile().chooseGoal(goal)
        var game by mutableStateOf(empty)
        var planOpened = 0
        compose.setContent {
            FinnyTheme {
                SavingsScreen(
                    game = game,
                    goals = content.goals(),
                    message = null,
                    onDismissMessage = {},
                    onChooseGoal = {},
                    onClaimGoal = {},
                    onPreviewWithdrawal = { GameState.newProfile().previewWithdrawal(Coins(0)) },
                    onWithdraw = {},
                    onBack = {},
                    onOpenPlan = { planOpened++ },
                )
            }
        }

        // Пополнение — в плане сезона: блока «Отложить» на экране копилки нет,
        // ссылки «Изменить →» тоже; главная кнопка ведёт в план.
        compose.onNode(hasSetTextAction()).assertDoesNotExist()
        compose.onNodeWithText("Отложить в копилку").assertDoesNotExist()
        compose.onNodeWithText("Изменить →").assertDoesNotExist()
        // Копилка пуста: забирать нечего — кнопки «Забрать» нет, главная кнопка — «Распределить монеты».
        compose.onNodeWithText("Копилка пока пустая").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Забрать монеты на покупки").assertDoesNotExist()
        compose.onNodeWithText("Отложить ещё").assertDoesNotExist()
        compose.onNodeWithText("Распределить монеты").performScrollTo().performClick()
        assertEquals("Переход к плану не сработал", 1, planOpened)
        // Вторичная кнопка — смена цели.
        compose.onNodeWithText("Изменить цель").performScrollTo().assertIsDisplayed()

        // С накоплениями главная кнопка — «Отложить ещё», она тоже ведёт в план.
        game = (empty.deposit(Coins(15)) as DepositResult.Success).state
        compose.waitForIdle()
        compose.onNodeWithText("Копилка пока пустая").assertDoesNotExist()
        compose.onNodeWithText("Распределить монеты").assertDoesNotExist()
        compose.onNodeWithText("Отложить ещё").performScrollTo().performClick()
        assertEquals("Переход к плану не сработал", 2, planOpened)
        compose.onNodeWithText("Изменить цель").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `экран заданий показывает звёзды и короткие карточки с темой`() {
        val queue = TaskQueue.ordered(simpleTasks)
        val opened = mutableListOf<String>()
        compose.setContent {
            FinnyTheme {
                TasksScreen(
                    tasks = simpleTasks,
                    completedIds = setOf(queue[0].id),
                    onOpenTask = { opened += it },
                    onBack = {},
                )
            }
        }

        // Звёзды — решённые в этом круге из заданий уровня.
        compose.onNodeWithContentDescription("Звёзды: 1 из ${queue.size}").assertIsDisplayed()
        // Каждое задание — один раз, без «Уже решал» и длинных пояснений.
        compose.onAllNodesWithText(queue[0].title).assertCountEquals(1)
        compose.onNodeWithText("Уже решал", substring = true).assertDoesNotExist()
        compose.onAllNodesWithText("Планирование").assertCountEquals(simpleTasks.count { it.topic == TaskTopic.BUDGET_PLANNING })
        compose.onNode(hasContentDescriptionPrefix("${queue[0].title},")).performScrollTo().performClick()
        assertEquals(listOf(queue[0].id), opened)
    }

    @Test
    fun `после неудачного дня задание помощи стоит первым`() {
        compose.setContent {
            FinnyTheme {
                TasksScreen(tasks = simpleTasks, completedIds = emptySet(), onOpenTask = {}, onBack = {}, recoveryFirst = true)
            }
        }
        val help = simpleTasks.first { it.topic == TaskTopic.RECOVERY }
        val first = compose.onNode(hasContentDescriptionPrefix("${help.title},")).fetchSemanticsNode().positionInRoot.y
        val other = compose.onNode(hasContentDescriptionPrefix("${TaskQueue.ordered(simpleTasks).first().title},"))
            .fetchSemanticsNode().positionInRoot.y
        assertTrue("задание помощи не первое", first < other)
    }

    @Test
    fun `после ответа без монет сюрприз ближе и сразу предлагается следующее задание`() {
        val task = content.task("save_temptation") as ChoiceTask
        val next = content.task("cart_fit_easy")!!
        var nextOpened = false
        compose.setContent {
            FinnyTheme {
                TaskDetailScreen(
                    task = task,
                    onAnswer = {
                        AnsweredTask(
                            check = TaskCheck(
                                isCorrect = true,
                                explanation = "Объяснение",
                                reward = IncomeSource.TASK_CORRECT,
                            ),
                            // В сезоне монет за задания нет.
                            credited = Coins.ZERO,
                            isRepeat = false,
                        )
                    },
                    onBack = {},
                    nextTask = next,
                    onNextTask = { nextOpened = true },
                )
            }
        }

        compose.onNodeWithText(task.options.first().title).performScrollTo().performClick()
        compose.onNodeWithText("Ответить").performScrollTo().performClick()

        // Монет нет — задание приближает сюрприз в лавке, строки о награде монетами нет.
        compose.onNodeWithText("Засчитано — скоро в лавке появится что-то новое!").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Награда за задание", substring = true).assertDoesNotExist()
        // Одна карточка следующего задания; кнопки «Готово» нет — возврат «Назад».
        compose.onNodeWithText(next.title).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Начать →").performScrollTo().performClick()
        assertTrue("Переход к следующему заданию не сработал", nextOpened)
        compose.onNodeWithText("Готово").assertDoesNotExist()
    }

    @Test
    fun `покупки дня перечислены на экране покупок`() {
        val food = content.shopItems().first()
        compose.setContent {
            FinnyTheme {
                ShopScreen(
                    items = content.shopItems(),
                    pet = GameState.newProfile().pet,
                    balance = Coins(40),
                    todayPurchases = listOf(PurchaseRecord(itemId = food.id, price = Coins(food.price), category = BudgetCategory.NEEDS)),
                    message = null,
                    onDismissMessage = {},
                    onBuy = {},
                    onBack = {},
                )
            }
        }

        compose.onNodeWithText("куплено сегодня").assertIsDisplayed()
        compose.onNodeWithText("Сегодня потрачено").assertIsDisplayed()
    }

    @Test
    fun `в строке товара сказано, сколько монет не хватает`() {
        compose.setContent {
            FinnyTheme {
                ShopScreen(
                    items = content.shopItems(),
                    pet = GameState.newProfile().pet,
                    balance = Coins(24),
                    message = null,
                    onDismissMessage = {},
                    onBuy = {},
                    onBack = {},
                )
            }
        }

        // Домик-палатка стоит 25 монет.
        compose.onNodeWithText("Не хватает 1").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Сегодня потрачено").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun `план помещается на телефоне 360 на 800 без прокрутки`() {
        // Эталонный телефон 720 × 1600, 320 dpi: под строкой состояния
        // (24 dp) и системной панелью (48 dp) экрану остаётся 728 dp.
        compose.setContent {
            FinnyTheme {
                Box(modifier = Modifier.height(728.dp)) {
                    PlanScreen(
                        game = GameState.newProfile(),
                        onConfirm = { _, _, _ -> },
                        onBack = {},
                        balance = Coins(50),
                    )
                }
            }
        }

        // Худший случай по высоте: копилка без цели, причина в три строки.
        compose.onNodeWithContentDescription("Копим на мечту: прибавить 5").performClick()

        val bottom = compose.onNodeWithText("Утвердить план").fetchSemanticsNode().boundsInRoot.bottom
        val limit = with(compose.density) { 728.dp.toPx() }
        assertTrue("Кнопка «Утвердить план» ниже края экрана: $bottom > $limit", bottom <= limit)
    }

    @Test
    fun `взрослый выключает и включает движения питомца`() {
        var motion by mutableStateOf(true)
        compose.setContent {
            FinnyTheme {
                AdultScreen(
                    game = GameState.newProfile(),
                    tasks = content.tasks(Difficulty.HARDER),
                    completedIds = emptySet(),
                    isDemo = false,
                    timeLimitEnabled = true,
                    minutesUsedToday = 0,
                    onResetProfile = {},
                    onStartDemo = {},
                    onResetDemo = {},
                    onSetTimeLimit = {},
                    onResetTodayUsage = {},
                    onBack = {},
                    motionEnabled = motion,
                    onSetMotion = { motion = it },
                )
            }
        }

        // Барьер: пример вида «7 × 4 = ?».
        val example = compose.onNode(hasText("= ?", substring = true)).fetchSemanticsNode()
            .config[SemanticsProperties.Text].joinToString("") { it.text }
        val (a, b) = Regex("""(\d+) × (\d+)""").find(example)!!.destructured
        compose.onNode(hasSetTextAction()).performTextInput((a.toInt() * b.toInt()).toString())
        compose.onNodeWithText("Продолжить").performClick()

        compose.onNodeWithText("включены").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Выключить движения").performScrollTo().performClick()
        assertFalse(motion)
        compose.onNodeWithText("выключены").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Включить движения").performScrollTo().performClick()
        assertTrue(motion)
    }

    @Test
    fun `при выключенных движениях питомец неподвижен, а реакция сразу снимается`() {
        val animation = content.pixelArt().animation
        val frames = mutableListOf<MotionFrame>()
        var ended: Long? = null
        compose.setContent {
            CompositionLocalProvider(LocalMotionEnabled provides false) {
                frames += rememberPetMotion(
                    animation = animation,
                    care = StatLevel.HIGH,
                    joy = StatLevel.HIGH,
                    reaction = PetReaction(PetReactions.PLAY, id = 7, stageBefore = null),
                    onReactionEnd = { ended = it },
                )
            }
        }
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitForIdle()

        assertEquals(7L, ended)
        assertTrue(frames.all { it == MotionFrame() })
    }

    @Test
    fun `отклик показывает текст и следующий шаг и закрывается крестиком`() {
        var shown by mutableStateOf(true)
        compose.setContent {
            FinnyTheme {
                if (shown) {
                    FeedbackCard(
                        message = FeedbackMessage(
                            text = "Не хватает 9 монет.",
                            nextStep = "Выполни задание.",
                            isProblem = true,
                        ),
                        onDismiss = { shown = false },
                    )
                }
            }
        }

        compose.onNodeWithText("Не хватает 9 монет.").assertIsDisplayed()
        compose.onNodeWithText("→ Выполни задание.").assertIsDisplayed()
        // Затруднение отличается не только цветом: знак озвучивается словом.
        compose.onNodeWithContentDescription("Внимание").assertExists()
        compose.onNodeWithContentDescription("Закрыть сообщение").performClick()
        assertFalse(shown)
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun `двор открывает разделы предметами, питомцем и табличкой прогресса`() {
        val opened = mutableListOf<String>()
        val state = AppState(isLoaded = true, profile = profile, game = GameState.newProfile())
        compose.setContent {
            CompositionLocalProvider(LocalMotionEnabled provides false) {
                FinnyTheme {
                    YardScreen(
                        state = state,
                        parts = content.petParts(),
                        activeTask = content.tasks(Difficulty.HARDER).first(),
                        goal = null,
                        today = "2026-09-26",
                        onDismissMessage = {},
                        onOpenPlan = { opened += "plan" },
                        onOpenShop = { opened += "shop" },
                        onOpenSavings = { opened += "savings" },
                        onOpenGlossary = { opened += "glossary" },
                        onOpenTasks = { opened += "tasks" },
                        onOpenGames = { opened += "games" },
                        onOpenPet = { opened += "pet" },
                        onOpenProgress = { opened += "progress" },
                        onOpenHelp = { opened += "help" },
                        onOpenAdult = { opened += "adult" },
                        onFinishPeriod = { opened += "finish" },
                    )
                }
            }
        }

        // Без движений раздел открывается сразу, без перебежки питомца.
        compose.onNodeWithContentDescription("Покупки: лавка для питомца").performScrollTo().performClick()
        compose.onNodeWithContentDescription("План сезона").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Словарик: финансовые слова").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Игры: задания и игры с питомцем").performScrollTo().performClick()
        compose.onNode(hasContentDescriptionPrefix("Копилка:")).performScrollTo().performClick()
        compose.onNode(hasContentDescriptionPrefix("Финни")).performScrollTo().performClick()
        compose.onNode(hasContentDescriptionPrefix("Мой прогресс.")).performClick()
        compose.onNodeWithContentDescription("Как играть").performClick()
        compose.onNodeWithContentDescription("Для взрослого").performClick()
        assertEquals(
            listOf("shop", "plan", "glossary", "games", "savings", "pet", "progress", "help", "adult"),
            opened,
        )
        // План не составлен — отметка передана словами.
        compose.onNodeWithContentDescription("План сезона").assert(
            androidx.compose.ui.test.SemanticsMatcher.expectValue(
                androidx.compose.ui.semantics.SemanticsProperties.StateDescription, "не составлен",
            ),
        )
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun `подсказка сезона видна у сундучка и открывает выбор мечты`() {
        val opened = mutableListOf<String>()
        val state = AppState(isLoaded = true, profile = profile, game = GameState.newProfile())
        val guide = ru.onefortwo.finny.ui.state.YardGuide.step(state, "2026-09-26")
        compose.setContent {
            CompositionLocalProvider(LocalMotionEnabled provides false) {
                FinnyTheme {
                    YardScreen(
                        state = state,
                        parts = content.petParts(),
                        activeTask = content.tasks(Difficulty.HARDER).first(),
                        goal = null,
                        today = "2026-09-26",
                        onDismissMessage = {},
                        onOpenPlan = { opened += "plan" },
                        onOpenShop = { opened += "shop" },
                        onOpenSavings = { opened += "savings" }, onOpenGlossary = {}, onOpenTasks = {}, onOpenPet = {},
                        onOpenProgress = {}, onOpenHelp = {}, onOpenAdult = {}, onFinishPeriod = {},
                        guide = guide,
                    )
                }
            }
        }
        // В сезоне подсказки без номеров шагов; первая — мечта.
        assertEquals(GuideStep(0, ru.onefortwo.finny.ui.state.GuideTarget.SAVINGS, "Выбери мечту — на что будем копить?"), guide)
        val bubble = compose.onNode(hasContentDescriptionPrefix("Подсказка: Выбери мечту — на что будем копить?"))
        bubble.assertIsDisplayed()
        compose.onNodeWithText("Шаг", substring = true, useUnmergedTree = true).assertDoesNotExist()
        // Облачко под сундучком, питомца над ним не закрывает.
        val px = compose.density.density
        val b = bubble.fetchSemanticsNode()
        val chest = compose.onNode(hasContentDescriptionPrefix("Копилка:")).fetchSemanticsNode()
        assertTrue("облачко выше низа сундучка", b.positionInRoot.y / px >= (chest.positionInRoot.y + chest.size.height) / px - 8)
        assertTrue("облачко выходит за экран", b.positionInRoot.x >= 0 && (b.positionInRoot.x + b.size.width) / px <= 360.5f)
        bubble.performClick()
        assertEquals(listOf("savings"), opened)
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun `питомец здоровается на дворе и прощается, прежде чем уснуть`() {
        var finished = false
        // План сезона составлен, все три события дня решены: «Уложить спать» доступна.
        val goal = content.goals().first { it.id == "scooter" }.toDomain()
        val state = AppState(
            isLoaded = true, profile = profile, game = GameState(balance = Coins(25)).chooseGoal(goal),
            extras = seasonDayDone,
        )
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalMotionEnabled provides true) {
                FinnyTheme {
                    YardScreen(
                        state = state,
                        parts = content.petParts(),
                        activeTask = null,
                        goal = null,
                        today = "2026-09-26",
                        onDismissMessage = {}, onOpenPlan = {}, onOpenShop = {}, onOpenSavings = {},
                        onOpenGlossary = {}, onOpenTasks = {}, onOpenPet = {}, onOpenProgress = {},
                        onOpenHelp = {}, onOpenAdult = {},
                        onFinishPeriod = { finished = true },
                        speech = "Привет! Как дела?",
                    )
                }
            }
        }
        compose.mainClock.advanceTimeBy(100)
        compose.onNodeWithText("Привет! Как дела?").assertExists()

        compose.onNodeWithText("Сначала", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Уложить спать").performClick()
        compose.mainClock.advanceTimeBy(500)
        // Сначала окно подтверждения: прощания ещё нет, день не закрыт.
        compose.onNodeWithText("Уложить Финни спать?").assertExists()
        compose.onNodeWithText("Пока! Приходи завтра — я буду ждать!").assertDoesNotExist()
        assertFalse("день закончился без подтверждения", finished)
        compose.onNodeWithText("Да, уложить спать").performClick()
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithText("Уложить Финни спать?").assertDoesNotExist()
        compose.onNodeWithText("Пока! Приходи завтра — я буду ждать!").assertExists()
        assertFalse("день закончился раньше прощания", finished)
        compose.mainClock.advanceTimeBy(6000)
        assertTrue("день не закончился после прощания", finished)
    }

    /**
     * Раскладка двора на экране [widthDp] × [heightDp]: предметы и питомец не
     * накладываются и не выходят за экран, двор — колонка не шире 480 dp,
     * сцена во всю ширину экрана, но не выше 480 × 100 / 180 dp.
     */
    private fun checkYardLayout(widthDp: Int, heightDp: Int) {
        // На планшете в портрете луг увеличен в k раз.
        val k = ru.onefortwo.finny.ui.screens.yardScaleFor(widthDp.dp, heightDp.dp)
        val state = AppState(isLoaded = true, profile = profile, game = GameState.newProfile())
        compose.setContent {
            FinnyTheme {
                YardScreen(
                    state = state,
                    parts = content.petParts(),
                    activeTask = content.tasks(Difficulty.HARDER).first(),
                    goal = content.goals().first(),
                    today = "2026-09-26",
                    onDismissMessage = {}, onOpenPlan = {}, onOpenShop = {}, onOpenSavings = {},
                    onOpenGlossary = {}, onOpenTasks = {}, onOpenPet = {}, onOpenProgress = {},
                    onOpenHelp = {}, onOpenAdult = {}, onFinishPeriod = {},
                )
            }
        }
        val px = compose.density.density
        // Полные границы, а не обрезанные видимой областью: на низком экране
        // двор прокручивается, и часть предметов ниже края.
        fun bounds(matcher: androidx.compose.ui.test.SemanticsMatcher) =
            compose.onNode(matcher).fetchSemanticsNode().let { n ->
                androidx.compose.ui.geometry.Rect(
                    n.positionInRoot.x / px, n.positionInRoot.y / px,
                    (n.positionInRoot.x + n.size.width) / px, (n.positionInRoot.y + n.size.height) / px,
                )
            }
        val objects = mapOf(
            "лавка" to bounds(hasContentDescriptionPrefix("Покупки:")),
            "игры" to bounds(hasContentDescriptionPrefix("Игры:")),
            "доска плана" to bounds(hasContentDescriptionPrefix("План сезона")),
            "книга" to bounds(hasContentDescriptionPrefix("Словарик:")),
            "питомец" to bounds(hasContentDescriptionPrefix("Финни")),
            "сундучок" to bounds(hasContentDescriptionPrefix("Копилка:")),
        )
        val tolerance = 0.5f
        objects.forEach { (name, r) ->
            assertTrue("$name выходит за экран: $r", r.left >= -tolerance && r.right <= widthDp + tolerance)
        }
        val names = objects.keys.toList()
        for (i in names.indices) for (j in i + 1 until names.size) {
            val a = objects.getValue(names[i])
            val b = objects.getValue(names[j])
            // Сундучок растянут на ширину двора, но стоит отдельным рядом.
            if ("сундучок" in listOf(names[i], names[j])) {
                assertTrue("${names[i]} и ${names[j]} наложились по высоте", a.bottom <= b.top + tolerance || b.bottom <= a.top + tolerance)
            } else {
                assertTrue("${names[i]} и ${names[j]} наложились: $a / $b", !a.overlaps(b))
            }
        }
        val lefts = objects.filterKeys { it != "сундучок" }.values
        val span = lefts.maxOf { it.right } - lefts.minOf { it.left }
        assertTrue("двор шире ${480 * k} dp: $span; $objects", span <= 480f * k + tolerance)
        val scene = bounds(hasContentDescriptionPrefix("Двор."))
        assertEquals("сцена не во всю ширину", widthDp.toFloat(), scene.width, 1f)
        assertTrue("сцена выше ${480 * k * 100 / 180} dp: ${scene.height}", scene.height <= 480f * k * 100 / 180 + 1)
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun `экранное время на дворе — сообщением с крестиком, а не отдельной строкой`() {
        val state = AppState(
            isLoaded = true, profile = profile, game = GameState.newProfile(),
            usageDate = "2026-09-26", usageMinutes = 16,
            message = ru.onefortwo.finny.ui.state.FeedbackMessage(
                text = "Осталось 5 минут на сегодня.",
                nextStep = "Не забудь доделать все дела, если какие-то остались.",
            ),
        )
        compose.setContent {
            FinnyTheme {
                YardScreen(
                    state = state, parts = content.petParts(), activeTask = null, goal = null, today = "2026-09-26",
                    onDismissMessage = {}, onOpenPlan = {}, onOpenShop = {}, onOpenSavings = {},
                    onOpenGlossary = {}, onOpenTasks = {}, onOpenPet = {}, onOpenProgress = {},
                    onOpenHelp = {}, onOpenAdult = {}, onFinishPeriod = {},
                )
            }
        }
        compose.onNodeWithText("Осталось 5 минут на сегодня.").assertIsDisplayed()
        compose.onNodeWithContentDescription("Закрыть сообщение").assertIsDisplayed()
        // Постоянной строки с остатком минут на дворе нет.
        compose.onNodeWithText("Осталось 4 минуты на сегодня.").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun `кнопки «Уложить спать» нет, пока не выбрана мечта, не составлен план и не решены события дня`() {
        val goal = content.goals().first { it.id == "scooter" }.toDomain()
        var state by mutableStateOf(AppState(isLoaded = true, profile = profile, game = GameState.newProfile()))
        var finished = false
        compose.setContent {
            CompositionLocalProvider(LocalMotionEnabled provides false) {
                FinnyTheme {
                    YardScreen(
                        state = state, parts = content.petParts(), activeTask = null, goal = null, today = "2026-09-26",
                        onDismissMessage = {}, onOpenPlan = {}, onOpenShop = {}, onOpenSavings = {},
                        onOpenGlossary = {}, onOpenTasks = {}, onOpenPet = {}, onOpenProgress = {},
                        onOpenHelp = {}, onOpenAdult = {}, onFinishPeriod = { finished = true },
                    )
                }
            }
        }
        // Без мечты и плана: ни кнопки, ни причины текстом — что делать, говорит облачко подсказки.
        compose.onNodeWithText("Уложить спать").assertDoesNotExist()
        compose.onNodeWithText("Играть").assertDoesNotExist()
        compose.onNodeWithText("Сначала", substring = true).assertDoesNotExist()

        // Мечта выбрана, план не составлен.
        state = state.copy(game = GameState(balance = Coins(50)).chooseGoal(goal))
        compose.waitForIdle()
        compose.onNodeWithText("Уложить спать").assertDoesNotExist()
        compose.onNodeWithText("Сначала", substring = true).assertDoesNotExist()

        // План составлен, решено одно событие из трёх.
        state = state.copy(
            game = GameState(balance = Coins(35)).chooseGoal(goal),
            extras = seasonDayDone.copy(answered = 1),
        )
        compose.waitForIdle()
        compose.onNodeWithText("Уложить спать").assertDoesNotExist()
        compose.onNodeWithText("Играть").assertDoesNotExist()
        compose.onNodeWithText("Сначала", substring = true).assertDoesNotExist()
        assertFalse("день закончился до решения событий", finished)

        // События решены — есть «Играть» и «Уложить спать»; «Уложить спать» открывает подтверждение.
        state = state.copy(extras = seasonDayDone)
        compose.waitForIdle()
        compose.onNodeWithText("Играть").assertIsDisplayed()
        compose.onNodeWithText("Уложить спать").assertIsEnabled().performClick()
        compose.onNodeWithText("Уложить Финни спать?").assertIsDisplayed()
        assertFalse("день закончился без подтверждения", finished)
        compose.onNodeWithText("Да, уложить спать").performClick()
        assertTrue(finished)
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun `окно «Уложить спать» без «Да» день не закрывает`() {
        val goal = content.goals().first { it.id == "scooter" }.toDomain()
        val state = AppState(
            isLoaded = true, profile = profile, game = GameState(balance = Coins(25)).chooseGoal(goal),
            extras = seasonDayDone,
        )
        var finished = 0
        compose.setContent {
            CompositionLocalProvider(LocalMotionEnabled provides false) {
                FinnyTheme {
                    YardScreen(
                        state = state, parts = content.petParts(), activeTask = null, goal = null, today = "2026-09-26",
                        onDismissMessage = {}, onOpenPlan = {}, onOpenShop = {}, onOpenSavings = {},
                        onOpenGlossary = {}, onOpenTasks = {}, onOpenPet = {}, onOpenProgress = {},
                        onOpenHelp = {}, onOpenAdult = {}, onFinishPeriod = { finished++ },
                    )
                }
            }
        }
        compose.onNodeWithText("Уложить Финни спать?").assertDoesNotExist()
        compose.onNodeWithText("Уложить спать").performClick()
        compose.onNodeWithText("Уложить Финни спать?").assertIsDisplayed()
        compose.onNodeWithText("Да, уложить спать").assertIsDisplayed()
        // «Не сейчас» — окно закрывается, день продолжается, кнопка на месте.
        compose.onNodeWithText("Не сейчас").performClick()
        compose.onNodeWithText("Уложить Финни спать?").assertDoesNotExist()
        assertEquals("день закрылся без «Да»", 0, finished)
        compose.onNodeWithText("Уложить спать").assertIsDisplayed()
        // Повторно — и «Да»: день закрывается один раз.
        compose.onNodeWithText("Уложить спать").performClick()
        compose.onNodeWithText("Да, уложить спать").performClick()
        compose.onNodeWithText("Уложить Финни спать?").assertDoesNotExist()
        assertEquals(1, finished)
    }

    /** Первый день сезона: план составлен, все три события дня решены. */
    private val seasonDayDone = SeasonExtras(
        planned = true, needsJar = 10, wantsJar = 15, plannedNeeds = 20, plannedWants = 15, plannedSavings = 15,
        spentNeeds = 10, deposited = 15, eventsDay = 1, dayEvents = listOf("hunger", "pet_stroke", "ball_wish"), answered = 3,
    )

    @Test
    @Config(qualifiers = "w360dp-h640dp")
    fun `на низком телефоне кнопка «Уложить спать» видна без прокрутки`() {
        // Кнопка есть только после дел дня: мечта выбрана, план составлен, события решены.
        val goal = content.goals().first { it.id == "scooter" }.toDomain()
        val state = AppState(
            isLoaded = true, profile = profile, game = GameState(balance = Coins(25)).chooseGoal(goal),
            extras = seasonDayDone,
        )
        compose.setContent {
            FinnyTheme {
                YardScreen(
                    state = state, parts = content.petParts(), activeTask = null, goal = null, today = "2026-09-26",
                    onDismissMessage = {}, onOpenPlan = {}, onOpenShop = {}, onOpenSavings = {},
                    onOpenGlossary = {}, onOpenTasks = {}, onOpenPet = {}, onOpenProgress = {},
                    onOpenHelp = {}, onOpenAdult = {}, onFinishPeriod = {},
                )
            }
        }
        // Кнопка закреплена внизу, вне прокрутки двора; над ней — «Играть».
        compose.onNodeWithText("Уложить спать").assertIsDisplayed()
        compose.onNodeWithText("Играть").assertIsDisplayed()
        val bottom = compose.onNodeWithText("Уложить спать").fetchSemanticsNode().boundsInRoot.bottom
        assertTrue("кнопка ниже экрана: $bottom", bottom <= 640 * compose.density.density + 1)
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun `двор на телефоне 360 на 800`() = checkYardLayout(360, 800)

    @Test
    @Config(qualifiers = "w800dp-h360dp-land")
    fun `двор на телефоне в альбомной ориентации`() = checkYardLayout(800, 360)

    @Test
    @Config(qualifiers = "w800dp-h1280dp")
    fun `двор на планшете в портрете`() = checkYardLayout(800, 1280)

    @Test
    @Config(qualifiers = "w1280dp-h800dp-land")
    fun `двор на планшете в альбомной ориентации`() = checkYardLayout(1280, 800)

    private fun hasContentDescriptionPrefix(prefix: String) =
        androidx.compose.ui.test.SemanticsMatcher("описание начинается с «$prefix»") { node ->
            node.config.getOrElseNullable(androidx.compose.ui.semantics.SemanticsProperties.ContentDescription) { null }
                ?.any { it.startsWith(prefix) } == true
        }

    /** Набор уровня «Попроще»: семь заданий, из них одно — восстановления. */
    private val simpleTasks get() = content.tasks(Difficulty.SIMPLE)

    @Test
    fun `словарик показывает термины с объяснениями`() {
        compose.setContent {
            FinnyTheme { GlossaryScreen(entries = content.glossary(), onBack = {}) }
        }

        compose.onNodeWithText("Бюджет").assertIsDisplayed()
        compose.onNodeWithText("Копилка").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `новое слово в словарике отмечено словом «Новое»`() {
        compose.setContent {
            FinnyTheme { GlossaryScreen(entries = content.glossary(), onBack = {}, newTerms = setOf("Бюджет")) }
        }

        compose.onAllNodesWithText("НОВОЕ").assertCountEquals(1)
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun `напоминание на дворе откладывается кнопкой «Не сейчас»`() {
        val state = AppState(isLoaded = true, profile = profile, game = GameState.newProfile())
        var dismissed: Reminder? = null
        compose.setContent {
            FinnyTheme {
                YardScreen(
                    state = state, parts = content.petParts(), activeTask = null, goal = null, today = "2026-09-26",
                    onDismissMessage = {}, onOpenPlan = {}, onOpenShop = {}, onOpenSavings = {},
                    onOpenGlossary = {}, onOpenTasks = {}, onOpenPet = {}, onOpenProgress = {},
                    onOpenHelp = {}, onOpenAdult = {}, onFinishPeriod = {},
                    guide = GuideStep(0, Reminder.WORD.target, Reminder.WORD.text, Reminder.WORD),
                    onDismissReminder = { dismissed = it },
                )
            }
        }

        compose.onNodeWithText(Reminder.WORD.text, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("В словарик", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Не сейчас", useUnmergedTree = true).performClick()
        assertEquals(Reminder.WORD, dismissed)
    }
}
