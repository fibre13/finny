package ru.onefortwo.finny

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.onefortwo.finny.content.AssetSource
import ru.onefortwo.finny.content.ChoiceTask
import ru.onefortwo.finny.content.ContentRepository
import ru.onefortwo.finny.content.PetAppearance
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
import ru.onefortwo.finny.ui.screens.OnboardingSetup
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
import ru.onefortwo.finny.ui.theme.DisplaySettings
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
    fun `экран знакомства спрашивает сложность и оформление до создания питомца`() {
        compose.setContent {
            FinnyTheme {
                OnboardingScreen(
                    onContinue = {},
                    setup = OnboardingSetup(
                        difficulty = null,
                        onDifficulty = {},
                        display = DisplaySettings(),
                        onDisplay = {},
                    ),
                )
            }
        }

        compose.onNodeWithText("Какие задания тебе по силам")
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("Для взрослого").performScrollTo().assertIsDisplayed()
        // Пока сложность не выбрана, дальше пройти нельзя: задания
        // начинаются сразу после создания питомца.
        compose.onNodeWithText("Выбери сложность, и кнопка станет доступной.")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `экран создания питомца показывает варианты внешности`() {
        compose.setContent {
            FinnyTheme { PetSetupScreen(parts = content.petParts(), onDone = { _, _ -> }) }
        }

        compose.onNodeWithText("Кто это").assertIsDisplayed()
        compose.onNodeWithText("Как назовём").performScrollTo().assertIsDisplayed()
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
                    onOpenTasks = {},
                    onOpenSavings = {},
                    onOpenHistory = {},
                    onOpenGlossary = {},
                    onOpenHelp = {},
                    onOpenAdult = {},
                    onOpenWardrobe = {},
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
                    onOpenTasks = {},
                    onOpenSavings = {},
                    onOpenHistory = {},
                    onOpenGlossary = {},
                    onOpenHelp = {},
                    onOpenAdult = {},
                    onOpenWardrobe = {},
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

        // Третья дорожка — «Копилка». Значение выставляется через семантику,
        // как это делает программа чтения с экрана.
        sliders()[2].performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) { it(8f) }

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
        // превысившие новый бюджет, должны урезаться: при нулевом бюджете
        // ползунков нет, и уменьшить их было бы нечем.
        val goal = content.goals().first { it.id == "scooter" }.toDomain()
        var game by mutableStateOf(GameState.newProfile().chooseGoal(goal))
        compose.setContent {
            FinnyTheme {
                PlanScreen(game = game, onConfirm = { _, _, _ -> }, onBack = {})
            }
        }

        sliders()[0].performSemanticsAction(SemanticsActions.SetProgress) { it(19f) }
        // «19 монет» выводится дважды: под «Нужное» и в строке «Распределено».
        compose.onAllNodesWithText("19 монет").assertCountEquals(2)

        game = game.copy(balance = Coins.ZERO)
        compose.waitForIdle()

        compose.onAllNodesWithText("19 монет").assertCountEquals(0)
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
                    onOpenTasks = {},
                    onOpenSavings = {},
                    onOpenHistory = {},
                    onOpenGlossary = {},
                    onOpenHelp = {},
                    onOpenAdult = {},
                    onOpenWardrobe = {},
                    onFinishPeriod = {},
                    today = "2026-09-15",
                )
            }
        }

        compose.onNodeWithText("День 1 закончен. План на день 2 составлен: можно делать покупки.")
            .assertIsDisplayed()
        compose.onNodeWithText("можно составить план", substring = true).assertDoesNotExist()
    }

    /** Ползунки экрана по порядку: у них нет текста, только диапазон значений. */
    private fun sliders() =
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))

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
                    onOpenTasks = {},
                    onOpenSavings = {},
                    onOpenHistory = {},
                    onOpenGlossary = {},
                    onOpenHelp = {},
                    onOpenAdult = {},
                    onOpenWardrobe = {},
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
                    onOpenTasks = {},
                    onOpenSavings = {},
                    onOpenHistory = {},
                    onOpenGlossary = {},
                    onOpenHelp = {},
                    onOpenAdult = {},
                    onOpenWardrobe = {},
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
