package ru.onefortwo.finny

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlin.random.Random
import ru.onefortwo.finny.content.TaskQueue
import ru.onefortwo.finny.content.withNumbers
import ru.onefortwo.finny.ui.common.FinnyNavigationBar
import ru.onefortwo.finny.ui.common.FinnyNavigationRail
import ru.onefortwo.finny.ui.common.NavSection
import ru.onefortwo.finny.ui.screens.AdultScreen
import ru.onefortwo.finny.ui.screens.GlossaryScreen
import ru.onefortwo.finny.ui.screens.HistoryScreen
import ru.onefortwo.finny.ui.screens.MainScreen
import ru.onefortwo.finny.ui.screens.OnboardingScreen
import ru.onefortwo.finny.ui.screens.PeriodResultScreen
import ru.onefortwo.finny.ui.screens.PetSetupScreen
import ru.onefortwo.finny.ui.screens.PlanScreen
import ru.onefortwo.finny.ui.screens.SavingsScreen
import ru.onefortwo.finny.ui.screens.ShopScreen
import ru.onefortwo.finny.ui.screens.TaskDetailScreen
import ru.onefortwo.finny.ui.screens.TasksScreen
import ru.onefortwo.finny.ui.screens.WardrobeScreen
import ru.onefortwo.finny.ui.state.GameViewModel
import ru.onefortwo.finny.ui.theme.FinnyTheme

/** Маршруты навигации. */
private object Routes {
    const val ONBOARDING = "onboarding"
    const val HELP = "help"
    const val PET_SETUP = "pet_setup"
    const val MAIN = "main"
    const val PLAN = "plan"
    const val SHOP = "shop"
    const val TASKS = "tasks"
    const val TASK = "task"
    const val SAVINGS = "savings"
    const val RESULT = "result"
    const val HISTORY = "history"
    const val GLOSSARY = "glossary"
    const val ADULT = "adult"
    const val WARDROBE = "wardrobe"
}

/** Задание восстановления после неудачного дня (ТЗ 2.5.9); есть на обоих уровнях. */
private const val RECOVERY_TASK_ID = "recover_help"

/** Маршрут вкладки навигации. */
private fun NavSection.route(): String = when (this) {
    NavSection.HOME -> Routes.MAIN
    NavSection.TASKS -> Routes.TASKS
    NavSection.PET -> Routes.WARDROBE
    NavSection.PROGRESS -> Routes.HISTORY
}

/** Вкладка, к которой относится маршрут, либо `null` для вложенного экрана. */
private fun tabOf(route: String?): NavSection? = when (route) {
    Routes.MAIN -> NavSection.HOME
    Routes.TASKS -> NavSection.TASKS
    Routes.WARDROBE -> NavSection.PET
    Routes.HISTORY -> NavSection.PROGRESS
    else -> null
}

/**
 * Переход на вкладку. Стек не растёт от переключения вкладок: под любой
 * вкладкой лежит только главный экран, и системная кнопка «Назад» ведёт
 * на него, а с него — из приложения.
 */
private fun NavController.openTab(section: NavSection) {
    navigate(section.route()) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Ширина, начиная с которой навигация переходит в боковую колонку. */
private const val EXPANDED_WIDTH_DP = 840

/**
 * Навигация приложения. Последовательность экранов повторяет сквозной
 * сценарий из Приложения А: знакомство, создание питомца, главный экран,
 * план, задания, покупки, копилка, итоги дня.
 *
 * Четыре раздела — главная, задания, питомец, прогресс — открываются
 * вкладками навигации. Остальные экраны вложены и открываются поверх
 * вкладки с кнопкой возврата.
 */
@Composable
fun FinnyApp(viewModel: GameViewModel) {
    val navController = rememberNavController()
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Счётчик экранного времени идёт только пока приложение на переднем плане.
    LifecycleResumeEffect(Unit) {
        viewModel.onSessionStart()
        onPauseOrDispose { viewModel.onSessionStop() }
    }

    // Пока сохранённое состояние читается с устройства, показывается заставка.
    if (!state.isLoaded) {
        LoadingScreen()
        return
    }

    val backStack by navController.currentBackStackEntryAsState()
    val tab = tabOf(backStack?.destination?.route)
    val windowWidthDp = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp().value }
    val expanded = windowWidthDp >= EXPANDED_WIDTH_DP
    val onSelectTab: (NavSection) -> Unit = { navController.openTab(it) }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(FinnyTheme.colors.appBackground),
    ) {
        if (tab != null && state.hasProfile && expanded) {
            FinnyNavigationRail(selected = tab, onSelect = onSelectTab)
        }
        Column(modifier = Modifier.weight(1f)) {
            val bottomBar = tab != null && state.hasProfile && !expanded
            Box(
                modifier = Modifier
                    .weight(1f)
                    // Отступ от системной панели внизу уже взяла панель
                    // навигации: экраны не должны прибавлять его ещё раз.
                    .then(
                        if (bottomBar) {
                            Modifier.consumeWindowInsets(WindowInsets.navigationBars)
                        } else {
                            Modifier
                        },
                    ),
            ) {
                AppNavHost(navController = navController, viewModel = viewModel)
            }
            if (bottomBar && tab != null) {
                FinnyNavigationBar(selected = tab, onSelect = onSelectTab)
            }
        }
    }
}

/** Граф экранов. */
@Composable
private fun AppNavHost(
    navController: NavHostController,
    viewModel: GameViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val content = viewModel.content
    val today = viewModel.today()
    val balance = state.game.balance

    NavHost(
        navController = navController,
        // Если профиль уже сохранён, приложение открывается сразу на главном
        // экране: прогресс сохраняется между запусками (ТЗ 2.5.13).
        startDestination = if (state.hasProfile) Routes.MAIN else Routes.ONBOARDING,
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onContinue = { navController.navigate(Routes.PET_SETUP) },
                step = 1,
            )
        }

        composable(Routes.HELP) {
            OnboardingScreen(
                onContinue = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
                continueText = "Понятно",
            )
        }

        // Шаги 2–4 знакомства: питомец, уровень заданий и имя, проверка
        // выбора. Выбор держится внутри экрана и переживает поворот.
        composable(Routes.PET_SETUP) {
            PetSetupScreen(
                parts = content.petParts(),
                onBack = { navController.popBackStack() },
                onDone = { name, appearance, difficulty ->
                    viewModel.createProfile(
                        petName = name,
                        appearance = appearance,
                        difficulty = difficulty,
                    )
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.MAIN) {
            val goalTitle = state.game.savings.goal?.let { goal ->
                content.goals().firstOrNull { it.id == goal.id }?.title
            }
            val availableTasks = content.tasks(state.difficulty)
            // Пока есть новые, предлагается следующее по очереди. Когда новых
            // не осталось, предлагается решить одно из пройденных ещё раз —
            // каждый день другое. Порядок задаёт TaskQueue.
            val activeTask = TaskQueue.next(availableTasks, state.completedTaskIds)
                ?: TaskQueue.repeatSuggestion(availableTasks, state.game.period.number)

            val reactions by viewModel.reactions.collectAsStateWithLifecycle()

            MainScreen(
                state = state,
                parts = content.petParts(),
                activeTask = activeTask,
                reaction = reactions.firstOrNull(),
                onReactionPlayed = viewModel::reactionPlayed,
                onOpenPet = { navController.openTab(NavSection.PET) },
                goalTitle = goalTitle,
                onDismissMessage = viewModel::dismissMessage,
                onOpenPlan = { navController.navigate(Routes.PLAN) },
                onOpenShop = { navController.navigate(Routes.SHOP) },
                onOpenSavings = { navController.navigate(Routes.SAVINGS) },
                onOpenGlossary = { navController.navigate(Routes.GLOSSARY) },
                onOpenHelp = { navController.navigate(Routes.HELP) },
                onOpenAdult = { navController.navigate(Routes.ADULT) },
                today = today,
                onFinishPeriod = {
                    viewModel.finishPeriod()
                    navController.navigate(Routes.RESULT)
                },
                onOpenTask = { id -> navController.navigate("${Routes.TASK}/$id") },
            )
        }

        composable(Routes.PLAN) {
            PlanScreen(
                game = state.game,
                goalTitle = state.game.savings.goal?.let { goal ->
                    content.goals().firstOrNull { it.id == goal.id }?.title
                },
                balance = balance,
                message = state.message,
                onDismissMessage = viewModel::dismissMessage,
                onBack = { navController.popBackStack() },
                // Экран закрывается только принятым планом. Отклонённый план
                // остаётся на экране вместе с причиной: иначе он выглядел бы
                // принятым, а введённые суммы пропадали бы с экраном.
                onConfirm = { needs, wants, savings ->
                    if (viewModel.confirmPlan(needs, wants, savings)) {
                        navController.popBackStack()
                    }
                },
                // Копилка открывается поверх плана: запись плана остаётся
                // в стеке, и введённые суммы при возврате сохраняются.
                onChooseGoal = { navController.navigate(Routes.SAVINGS) },
            )
        }

        composable(Routes.SHOP) {
            ShopScreen(
                items = content.shopItems(),
                pet = state.game.pet,
                balance = balance,
                todayPurchases = state.game.period.purchases,
                message = state.message,
                onDismissMessage = viewModel::dismissMessage,
                onBuy = viewModel::buy,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.TASKS) {
            TasksScreen(
                tasks = content.tasks(state.difficulty),
                completedIds = state.completedTaskIds,
                balance = balance,
                onOpenTask = { id -> navController.navigate("${Routes.TASK}/$id") },
            )
        }

        composable("${Routes.TASK}/{taskId}") { entry ->
            val taskId = entry.arguments?.getString("taskId")
            // Числа берутся один раз на открытие экрана: при повороте
            // и перерисовке условие не должно меняться под руками. Поэтому
            // сохраняется зерно случайных чисел, а не сами числа: при повороте
            // Android пересоздаёт экран, обычная память композиции теряется,
            // а введённый ответ и результат восстанавливаются — без зерна
            // они относились бы уже к другому условию.
            val seed = rememberSaveable(taskId) { Random.nextLong() }
            val task = remember(taskId, seed) {
                taskId?.let { content.task(it)?.withNumbers(Random(seed)) }
            }

            if (task == null) {
                // Навигация выполняется как эффект, а не во время композиции.
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                // Следующее новое задание предлагается сразу после ответа.
                // Когда экранное время на сегодня вышло, не предлагается:
                // приложение не подталкивает продолжать сверх предела
                // (ТЗ 8.1: этичность мотивации, отсутствие давления;
                // основание предела — docs/01, раздел «Экранное время»).
                val nextTask = TaskQueue.nextAfterAnswer(
                    tasks = content.tasks(state.difficulty),
                    solved = state.completedTaskIds,
                    answeredId = task.id,
                    allowed = !state.isTimeUp(today),
                )
                TaskDetailScreen(
                    task = task,
                    balance = balance,
                    onAnswer = { answer -> viewModel.answerTask(task, answer) },
                    onBack = { navController.popBackStack() },
                    nextTask = nextTask,
                    // Текущее задание заменяется следующим, а не остаётся под
                    // ним в стеке: «Назад» из следующего ведёт туда, откуда
                    // пришли, а не к уже решённому заданию.
                    onNextTask = {
                        nextTask?.let { next ->
                            navController.navigate("${Routes.TASK}/${next.id}") {
                                popUpTo("${Routes.TASK}/{taskId}") { inclusive = true }
                            }
                        }
                    },
                )
            }
        }

        composable(Routes.SAVINGS) {
            SavingsScreen(
                game = state.game,
                balance = balance,
                goals = content.goals(),
                message = state.message,
                onDismissMessage = viewModel::dismissMessage,
                onChooseGoal = viewModel::chooseGoal,
                onClaimGoal = viewModel::claimGoal,
                onDeposit = viewModel::deposit,
                onPreviewWithdrawal = viewModel::previewWithdrawal,
                onWithdraw = viewModel::withdraw,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.RESULT) {
            val outcome = state.lastOutcome
            if (outcome == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                PeriodResultScreen(
                    petName = state.profile?.petName ?: "Финни",
                    outcome = outcome,
                    // В обычном режиме второй игровой день в те же сутки
                    // не заканчивается; в демонстрационном ограничения нет.
                    nextDayTomorrow = !state.isDemo,
                    // Кнопка восстановления открывает само задание, а не
                    // общий список: путь восстановления должен быть понятным
                    // и коротким (ТЗ 2.5.9).
                    onOpenRecoveryTask = {
                        navController.navigate("${Routes.TASK}/$RECOVERY_TASK_ID") {
                            popUpTo(Routes.MAIN)
                        }
                    },
                    onContinue = {
                        navController.popBackStack(Routes.MAIN, inclusive = false)
                    },
                )
            }
        }

        composable(Routes.HISTORY) {
            val goalTitle = state.game.savings.goal?.let { goal ->
                content.goals().firstOrNull { it.id == goal.id }?.title
            }

            HistoryScreen(
                game = state.game,
                petName = state.profile?.petName ?: "Финни",
                balance = balance,
                tasks = content.tasks(state.difficulty),
                completedIds = state.completedTaskIds,
                goalTitle = goalTitle,
                achievedGoalTitles = content.goals()
                    .filter { it.id in state.achievedGoalIds }
                    .map { it.title },
            )
        }

        composable(Routes.WARDROBE) {
            WardrobeScreen(
                state = state,
                parts = content.petParts(),
                // Образ применён — питомец в новом виде показывается на главной.
                onApply = { colorId, accessoryId ->
                    viewModel.changeAppearance(colorId, accessoryId)
                    navController.openTab(NavSection.HOME)
                },
                onOpenShop = {
                    navController.navigate(Routes.SHOP) { popUpTo(Routes.MAIN) }
                },
            )
        }

        composable(Routes.GLOSSARY) {
            GlossaryScreen(
                entries = content.glossary(),
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.ADULT) {
            AdultScreen(
                game = state.game,
                tasks = content.tasks(state.difficulty),
                completedIds = state.completedTaskIds,
                isDemo = state.isDemo,
                timeLimitEnabled = state.timeLimitEnabled,
                minutesUsedToday = state.minutesUsed(today),
                onSetTimeLimit = viewModel::setTimeLimitEnabled,
                onResetTodayUsage = viewModel::resetTodayUsage,
                onResetProfile = {
                    viewModel.resetProfile()
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                },
                onStartDemo = {
                    viewModel.startDemo()
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                },
                onResetDemo = {
                    viewModel.resetDemo()
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
    }
}

/** Заставка на время чтения сохранённого состояния. */
@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FinnyTheme.colors.appBackground),
        contentAlignment = Alignment.Center,
    ) {
        Text("Питомец Финни", style = MaterialTheme.typography.headlineMedium)
    }
}
