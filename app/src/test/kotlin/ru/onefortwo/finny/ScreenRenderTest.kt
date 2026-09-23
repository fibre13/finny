package ru.onefortwo.finny

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import java.io.File
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
import ru.onefortwo.finny.economy.previewWithdrawal
import ru.onefortwo.finny.ui.screens.GlossaryScreen
import ru.onefortwo.finny.ui.screens.MainScreen
import ru.onefortwo.finny.ui.screens.OnboardingScreen
import ru.onefortwo.finny.ui.screens.OnboardingSetup
import ru.onefortwo.finny.ui.screens.PetSetupScreen
import ru.onefortwo.finny.ui.screens.PlanScreen
import ru.onefortwo.finny.ui.screens.SavingsScreen
import ru.onefortwo.finny.ui.screens.ShopScreen
import ru.onefortwo.finny.ui.screens.TasksScreen
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
        // Прожитый день выключает кнопку. Объяснение есть в блоке «День
        // прожит», но он остаётся вверху экрана: рядом с самой кнопкой
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
        compose.onNodeWithText("Сегодня день уже закончен. Новый план будет завтра.")
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
                    tasks = content.tasks(),
                    completedIds = emptySet(),
                    onOpenTask = {},
                    onBack = {},
                )
            }
        }

        compose.onNodeWithText("Планирование бюджета").assertIsDisplayed()
        compose.onNodeWithText("Формирование сбережений").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `словарик показывает термины с объяснениями`() {
        compose.setContent {
            FinnyTheme { GlossaryScreen(entries = content.glossary(), onBack = {}) }
        }

        compose.onNodeWithText("Бюджет").assertIsDisplayed()
        compose.onNodeWithText("Копилка").performScrollTo().assertIsDisplayed()
    }
}
