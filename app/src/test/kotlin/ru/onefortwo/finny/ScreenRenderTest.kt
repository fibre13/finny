package ru.onefortwo.finny

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import java.io.File
import org.junit.Assert.assertEquals
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
import ru.onefortwo.finny.content.toDomain
import ru.onefortwo.finny.economy.BudgetPlan
import ru.onefortwo.finny.economy.Coins
import ru.onefortwo.finny.economy.Difficulty
import ru.onefortwo.finny.economy.GameState
import ru.onefortwo.finny.economy.IncomeSource
import ru.onefortwo.finny.economy.PeriodCompletion
import ru.onefortwo.finny.economy.PlanConfirmation
import ru.onefortwo.finny.economy.chooseGoal
import ru.onefortwo.finny.economy.confirmPlan
import ru.onefortwo.finny.economy.finishPeriod
import ru.onefortwo.finny.economy.previewWithdrawal
import ru.onefortwo.finny.ui.screens.GlossaryScreen
import ru.onefortwo.finny.ui.screens.MainScreen
import ru.onefortwo.finny.ui.screens.OnboardingScreen
import ru.onefortwo.finny.ui.screens.PeriodResultScreen
import ru.onefortwo.finny.ui.screens.PetSetupScreen
import ru.onefortwo.finny.ui.screens.PlanScreen
import ru.onefortwo.finny.ui.screens.SavingsScreen
import ru.onefortwo.finny.ui.screens.ShopScreen
import ru.onefortwo.finny.ui.screens.TaskDetailScreen
import ru.onefortwo.finny.ui.screens.TasksScreen
import ru.onefortwo.finny.ui.state.AnsweredTask
import ru.onefortwo.finny.ui.state.AppState
import ru.onefortwo.finny.ui.state.Profile
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

        compose.onNodeWithText("Решение 1. Купить нужное").assertIsDisplayed()
        compose.onNodeWithText("Решение 2. Купить желаемое").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Решение 3. Отложить в копилку").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `знакомство спрашивает сложность и имя до создания питомца`() {
        compose.setContent {
            FinnyTheme { PetSetupScreen(parts = content.petParts(), onDone = { _, _, _ -> }) }
        }

        // Шаг 2 — внешность, шаг 3 — сложность заданий и имя.
        compose.onNodeWithText("Выбрать").performScrollTo().performClick()

        compose.onNodeWithText("Какие задания тебе по силам")
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("Как назовём").performScrollTo().assertIsDisplayed()
        // Пока сложность и имя не выбраны, дальше пройти нельзя: задания
        // начинаются сразу после создания питомца.
        compose.onNodeWithText("Проверить выбор").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Выбери задания и придумай имя, и кнопка станет доступной.")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `экран создания питомца показывает варианты внешности`() {
        compose.setContent {
            FinnyTheme { PetSetupScreen(parts = content.petParts(), onDone = { _, _, _ -> }) }
        }

        compose.onNodeWithText("Кто это").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Какого цвета").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Украшение").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `главный экран показывает баланс, показатели и задание одновременно`() {
        val state = AppState(profile = profile, game = GameState.newProfile())

        compose.setContent {
            FinnyTheme {
                MainScreen(
                    state = state,
                    parts = content.petParts(),
                    activeTask = content.tasks().first(),
                    titleOf = { it },
                    goalTitle = null,
                    onDismissMessage = {},
                    onOpenPlan = {},
                    onOpenShop = {},
                    onOpenSavings = {},
                    onOpenGlossary = {},
                    onOpenHelp = {},
                    onOpenAdult = {},
                    onFinishPeriod = {},
                    today = "2026-09-15",
                )
            }
        }

        compose.onNodeWithText("Финни").assertIsDisplayed()
        compose.onNodeWithText("Можно потратить").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("В копилке").performScrollTo().assertIsDisplayed()
        // Стартовое значение 60 — средний уровень. Уровень назван словом:
        // пиктограмма рядом его только дублирует и текст не заменяет
        // (ТЗ 3.6: состояние читается не по цвету и не по рисунку).
        compose.onNodeWithText("Забота: В порядке").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Радость: Спокойный").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `у неактивной кнопки завершения дня всегда есть причина текстом`() {
        // Прожитый день выключает кнопку. Объяснение есть в блоке «На сегодня
        // день закончен», но он остаётся вверху экрана: рядом с самой кнопкой
        // причина обязана быть текстом, а не только приглушённым цветом
        // (ТЗ 3.6).
        val state = AppState(
            profile = profile,
            game = GameState.newProfile(),
            lastFinishedDate = "2026-09-15",
        )

        compose.setContent {
            FinnyTheme {
                MainScreen(
                    state = state,
                    parts = content.petParts(),
                    activeTask = content.tasks().first(),
                    titleOf = { it },
                    goalTitle = null,
                    onDismissMessage = {},
                    onOpenPlan = {},
                    onOpenShop = {},
                    onOpenSavings = {},
                    onOpenGlossary = {},
                    onOpenHelp = {},
                    onOpenAdult = {},
                    onFinishPeriod = {},
                    today = "2026-09-15",
                )
            }
        }

        compose.onNodeWithText("Закончить день").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText(
            "Закончить этот день получится завтра: один игровой день — в одни сутки.",
        )
            .performScrollTo()
            .assertIsDisplayed()
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
    fun `копилка без цели выключает утверждение плана и называет причину`() {
        compose.setContent {
            FinnyTheme {
                PlanScreen(game = GameState.newProfile(), onConfirm = { _, _, _ -> }, onBack = {})
            }
        }

        // Кнопка ищется по описанию для программы чтения с экрана:
        // у трёх направлений одинаковые подписи «+5».
        compose.onNodeWithContentDescription("Копилка: прибавить 5").performScrollTo().performClick()

        compose.onNodeWithText("Утвердить план").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText(
            "Чтобы отложить в копилку, сначала выбери цель. Введённые суммы сохранятся.",
        )
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `суммы плана урезаются, если бюджет уменьшился, пока план открыт`() {
        // Из копилки поверх плана можно отложить монеты с баланса. Суммы,
        // превысившие новый бюджет, должны урезаться, а не оставаться
        // больше всего, что есть у ребёнка.
        val goal = content.goals().first { it.id == "scooter" }.toDomain()
        var game by mutableStateOf(GameState.newProfile().chooseGoal(goal))
        compose.setContent {
            FinnyTheme {
                PlanScreen(game = game, onConfirm = { _, _, _ -> }, onBack = {})
            }
        }

        repeat(4) { compose.onNodeWithContentDescription("Нужное: прибавить 5").performScrollTo().performClick() }
        // «20 монет» выводится дважды: под «Нужное» и в строке «Распределено».
        compose.onAllNodesWithText("20 монет").assertCountEquals(2)

        game = game.copy(balance = Coins.ZERO)
        compose.waitForIdle()

        compose.onAllNodesWithText("20 монет").assertCountEquals(0)
        compose.onNodeWithText("Лишние монеты").assertDoesNotExist()
    }

    @Test
    fun `после утверждения плана блок прожитого дня не предлагает составить план`() {
        // Прожитый день и уже утверждённый план на следующий: предлагать
        // составить план второй раз нельзя — повторно он не составляется.
        val goal = content.goals().first { it.id == "scooter" }.toDomain()
        val first = GameState.newProfile().chooseGoal(goal)
            .confirmPlan(BudgetPlan(Coins(20), Coins(10), Coins(5))) as PlanConfirmation.Success
        val nextDay = (first.state.finishPeriod() as PeriodCompletion.Success).state
        val planned = nextDay.confirmPlan(BudgetPlan(Coins(10), Coins(5), Coins(0)))
            as PlanConfirmation.Success
        val state = AppState(profile = profile, game = planned.state, lastFinishedDate = "2026-09-15")

        compose.setContent {
            FinnyTheme {
                MainScreen(
                    state = state,
                    parts = content.petParts(),
                    activeTask = content.tasks().first(),
                    titleOf = { it },
                    goalTitle = null,
                    onDismissMessage = {},
                    onOpenPlan = {},
                    onOpenShop = {},
                    onOpenSavings = {},
                    onOpenGlossary = {},
                    onOpenHelp = {},
                    onOpenAdult = {},
                    onFinishPeriod = {},
                    today = "2026-09-15",
                )
            }
        }

        compose.onNodeWithText("День 1 закончен. План на день 2 составлен: можно делать покупки.")
            .assertIsDisplayed()
        compose.onNodeWithText("можно составить план", substring = true).assertDoesNotExist()
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
        compose.onAllNodesWithText("50 монет").assertCountEquals(2)
        // Весь бюджет: прибавлять некуда.
        compose.onNodeWithContentDescription("Нужное: прибавить 5").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Нужное: прибавить 1").assertIsNotEnabled()

        compose.onNodeWithContentDescription("Нужное: убавить на 1").performScrollTo().performClick()
        compose.onAllNodesWithText("49 монет").assertCountEquals(2)
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
            compose.onNodeWithContentDescription("Копилка: прибавить 1").performScrollTo().performClick()
        }

        compose.onNodeWithText("Все монеты распределены.").performScrollTo().assertIsDisplayed()
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
            "Награда за задание ещё раз: +5 монет, за повтор — половина награды.",
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
    fun `экран плана показывает три направления и остаток`() {
        compose.setContent {
            FinnyTheme {
                PlanScreen(game = GameState.newProfile(), onConfirm = { _, _, _ -> }, onBack = {})
            }
        }

        compose.onNodeWithText("Нужное").assertIsDisplayed()
        compose.onNodeWithText("Хочу").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Копилка").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Распределено").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `экран покупок показывает цену и влияние на питомца`() {
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

        compose.onNodeWithText("Корм").assertIsDisplayed()
        compose.onNodeWithText("Финни будет сытым").performScrollTo().assertIsDisplayed()
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
                    onDeposit = {},
                    onPreviewWithdrawal = { GameState.newProfile().previewWithdrawal(Coins(0)) },
                    onWithdraw = {},
                    onBack = {},
                )
            }
        }

        compose.onNodeWithText("Выбери цель").assertIsDisplayed()
    }

    @Test
    fun `в копилке сумма выставляется кнопками в пределах баланса и накопленного`() {
        val goal = content.goals().first { it.id == "scooter" }.toDomain()
        val deposits = mutableListOf<Int>()
        compose.setContent {
            FinnyTheme {
                SavingsScreen(
                    game = GameState.newProfile().chooseGoal(goal),
                    goals = content.goals(),
                    message = null,
                    onDismissMessage = {},
                    onChooseGoal = {},
                    onClaimGoal = {},
                    onDeposit = { deposits += it },
                    onPreviewWithdrawal = { GameState.newProfile().previewWithdrawal(Coins(0)) },
                    onWithdraw = {},
                    onBack = {},
                )
            }
        }

        // Системного поля ввода нет: сумма выставляется кнопками шага.
        compose.onNode(hasSetTextAction()).assertDoesNotExist()
        compose.onNodeWithText("Отложить в копилку").performScrollTo().assertIsNotEnabled()
        repeat(2) {
            compose.onNodeWithContentDescription("Сколько отложить: прибавить 5")
                .performScrollTo().performClick()
        }
        compose.onNodeWithText("Отложить в копилку").performScrollTo().performClick()
        assertEquals(listOf(10), deposits)

        // Копилка пуста: забирать нечего, кнопки выключены, причина названа.
        compose.onNodeWithContentDescription("Сколько забрать: прибавить 1")
            .performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("В копилке пока пусто — забирать нечего.")
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `экран заданий показывает темы`() {
        compose.setContent {
            FinnyTheme {
                TasksScreen(
                    tasks = simpleTasks,
                    completedIds = emptySet(),
                    onOpenTask = {},
                    onBack = {},
                )
            }
        }

        // Тема подписана на каждой карточке; первые три новых задания —
        // по одному на каждую обязательную тему.
        compose.onNodeWithText("Новые задания").assertIsDisplayed()
        compose.onAllNodesWithText("Планирование бюджета").assertCountEquals(2)
        compose.onAllNodesWithText("Формирование сбережений").assertCountEquals(2)
        compose.onAllNodesWithText("Платежи и покупки").assertCountEquals(2)
    }

    @Test
    fun `решённые задания уходят из новых в отдельный раздел`() {
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

        // Решённое задание выводится один раз — в «Уже решал», а не ещё и в новых.
        compose.onAllNodesWithText(queue[0].title).assertCountEquals(1)
        compose.onNodeWithText("Новые задания").assertIsDisplayed()
        compose.onNodeWithText("Уже решал").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Решить ещё раз").performScrollTo().performClick()
        compose.onNodeWithText("Если день не удался").performScrollTo().assertIsDisplayed()
        assertEquals(listOf(queue[0].id), opened)
    }

    @Test
    fun `когда новых нет, список говорит об этом прямо`() {
        val solved = TaskQueue.ordered(simpleTasks).map { it.id }.toSet()
        compose.setContent {
            FinnyTheme {
                TasksScreen(tasks = simpleTasks, completedIds = solved, onOpenTask = {}, onBack = {})
            }
        }

        compose.onNodeWithText("Новых заданий нет").assertIsDisplayed()
        compose.onNodeWithText(
            "Новые задания закончились. Любое из тех, что уже решал, можно решить ещё раз — они ниже.",
        )
            .assertIsDisplayed()
        // «Помоги Финни» в очередь не входит и не решено — утверждать,
        // что решены все задания, экран не должен.
        compose.onNodeWithText("Все задания", substring = true).assertDoesNotExist()
    }

    @Test
    fun `после ответа сразу предлагается следующее задание`() {
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
                            credited = Coins(10),
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

        compose.onNodeWithText("Следующее задание: ${next.title}").performScrollTo().performClick()
        assertTrue("Переход к следующему заданию не сработал", nextOpened)
        compose.onNodeWithText("Готово").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `главный экран открывает следующее задание сразу`() {
        val next = TaskQueue.ordered(simpleTasks).first()
        val opened = mutableListOf<String>()
        compose.setContent {
            FinnyTheme {
                MainScreen(
                    state = AppState(profile = profile, game = GameState.newProfile()),
                    parts = content.petParts(),
                    activeTask = next,
                    titleOf = { it },
                    goalTitle = null,
                    onDismissMessage = {},
                    onOpenPlan = {},
                    onOpenShop = {},
                    onOpenSavings = {},
                    onOpenGlossary = {},
                    onOpenHelp = {},
                    onOpenAdult = {},
                    onFinishPeriod = {},
                    today = "2026-09-15",
                    onOpenTask = { opened += it },
                )
            }
        }

        compose.onNodeWithText("Следующее задание").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Начать задание").performScrollTo().performClick()
        assertEquals(listOf(next.id), opened)
    }

    @Test
    fun `когда новых нет, главный экран предлагает повтор и так и говорит`() {
        val queue = TaskQueue.ordered(simpleTasks)
        val state = AppState(
            profile = profile,
            game = GameState.newProfile(),
            completedTaskIds = queue.map { it.id }.toSet(),
        )
        compose.setContent {
            FinnyTheme {
                MainScreen(
                    state = state,
                    parts = content.petParts(),
                    activeTask = queue.first(),
                    titleOf = { it },
                    goalTitle = null,
                    onDismissMessage = {},
                    onOpenPlan = {},
                    onOpenShop = {},
                    onOpenSavings = {},
                    onOpenGlossary = {},
                    onOpenHelp = {},
                    onOpenAdult = {},
                    onFinishPeriod = {},
                    today = "2026-09-15",
                )
            }
        }

        compose.onNodeWithText("Новых заданий нет").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Решить ещё раз").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Следующее задание").assertDoesNotExist()
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
}
