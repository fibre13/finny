package ru.onefortwo.finny

import ru.onefortwo.finny.content.Accessories
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlin.random.Random
import ru.onefortwo.finny.content.TaskQueue
import ru.onefortwo.finny.content.ContentRepository
import ru.onefortwo.finny.content.GlossaryEntry
import ru.onefortwo.finny.economy.Difficulty
import ru.onefortwo.finny.content.withNumbers
import ru.onefortwo.finny.content.named
import ru.onefortwo.finny.content.PetPartsContent
import ru.onefortwo.finny.economy.StatLevel
import ru.onefortwo.finny.ui.common.PetFigure
import ru.onefortwo.finny.ui.state.AppState
import ru.onefortwo.finny.ui.state.PetReaction
import ru.onefortwo.finny.ui.state.PetReactions
import androidx.compose.foundation.layout.width
import ru.onefortwo.finny.ui.common.LocalMotionEnabled
import ru.onefortwo.finny.ui.common.NavSection
import ru.onefortwo.finny.ui.screens.AdultScreen
import ru.onefortwo.finny.ui.screens.GlossaryScreen
import ru.onefortwo.finny.ui.screens.HistoryScreen
import ru.onefortwo.finny.ui.screens.YardScreen
import ru.onefortwo.finny.ui.screens.GiftScreen
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
import ru.onefortwo.finny.ui.state.GlossaryTerms
import ru.onefortwo.finny.ui.state.YardGuide
import ru.onefortwo.finny.ui.state.Growth
import ru.onefortwo.finny.economy.GrowthStage
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
    const val SEASON_RESULT = "season_result"
    const val GROWTH = "growth"
    const val DREAM = "dream"
    const val GAMES = "games"
    const val PLAY = "play"
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
    val motionEnabled by viewModel.motionEnabled.collectAsStateWithLifecycle()

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

    // Навигационных панелей нет — разделы открываются со двора.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FinnyTheme.colors.appBackground),
    ) {
        CompositionLocalProvider(LocalMotionEnabled provides motionEnabled) {
            AppNavHost(navController = navController, viewModel = viewModel)
        }
        if (state.isDemo || state.demoPending) DemoWatermark()
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
    val petName = state.profile?.petName ?: "Финни"

    NavHost(
        navController = navController,
        // Если профиль уже сохранён, приложение открывается сразу на главном
        // экране: прогресс сохраняется между запусками (ТЗ 2.5.13).
        startDestination = if (state.hasProfile) Routes.MAIN else Routes.ONBOARDING,
    ) {
        composable(Routes.ONBOARDING) {
            // Знакомство начинается с подарка.
            GiftScreen(onOpen = { navController.navigate(Routes.PET_SETUP) })
        }

        composable(Routes.HELP) {
            OnboardingScreen(
                onContinue = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
                continueText = "Понятно",
                petName = state.profile?.petName,
            )
        }

        // Шаги 2–4 знакомства: питомец, уровень заданий и имя, проверка
        // выбора. Выбор держится внутри экрана и переживает поворот.
        composable(Routes.PET_SETUP) {
            PetSetupScreen(
                parts = content.petParts(),
                nameFilter = content.nameFilter(),
                onBack = { navController.popBackStack() },
                // В демонстрационном режиме заранее отмечен набор «Посложнее»:
                // эксперт видит задания с делением; выбор можно поменять.
                initialDifficulty = if (state.demoPending) Difficulty.HARDER else null,
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
            val speech by viewModel.speech.collectAsStateWithLifecycle()
            LaunchedEffect(Unit) {
                viewModel.startNewDayIfDue()
                viewModel.greetIfPending()
                // Вернулись на двор — заход в задания закончен, счётчик «трёх заданий» заново.
                viewModel.leaveTasks()
            }
            // Итоги сезона и праздник роста открываются сами.
            LaunchedEffect(state.extras.seasonDone, state.game.stage, state.extras.growthShown) {
                when {
                    // Праздник — только когда малыш становится подростком; взрослая стадия без праздника.
                    state.game.stage > state.extras.growthShown ->
                        if (state.extras.growthShown == GrowthStage.BABY) navController.navigate(Routes.GROWTH) else viewModel.growthCelebrated()
                    state.extras.seasonDone -> navController.navigate(Routes.SEASON_RESULT)
                }
            }

            // Главная — двор питомца; разделы открываются предметами.
            YardScreen(
                state = state,
                parts = content.petParts(),
                activeTask = activeTask,
                guide = YardGuide.step(state, today),
                glossaryHint = GlossaryTerms.hint(state, glossaryEntries(content, state).map { it.term }),
                glossaryNew = GlossaryTerms.closed(state, glossaryEntries(content, state).map { it.term }).size,
                onSleepingTap = viewModel::sleepingHint,
                onArrivalShown = viewModel::dismissArrival,
                onOpenTask = activeTask?.let { task -> { navController.navigate("${Routes.TASK}/${task.id}") } },
                onDismissReminder = viewModel::dismissReminder,
                pendingEvent = viewModel.pendingEvent(state),
                resultEvent = state.eventResult?.let { r -> content.events().firstOrNull { it.id == r.eventId } },
                onAnswerEvent = viewModel::answerEvent,
                onCloseEvent = viewModel::closeEventResult,
                onConfirmSavings = viewModel::confirmSavingsAsk,
                onCancelSavings = viewModel::cancelSavingsAsk,
                surprise = state.extras.surprise?.let { id -> content.shopItems().firstOrNull { it.id == id } },
                surpriseAlso = state.extras.surpriseAlso.mapNotNull { id -> content.shopItems().firstOrNull { it.id == id } },
                onDismissSurprise = viewModel::dismissSurprise,
                onMissedShown = viewModel::missedShown,
                // «Играть» — в игры с питомцем.
                onPlay = { navController.navigate(Routes.PLAY) },
                speech = speech,
                onSpeechShown = viewModel::speechShown,
                goal = state.game.savings.goal?.let { g -> content.goals().firstOrNull { it.id == g.id } },
                today = today,
                reaction = reactions.firstOrNull(),
                onReactionPlayed = viewModel::reactionPlayed,
                onDismissMessage = viewModel::dismissMessage,
                onOpenPlan = { navController.navigate(Routes.PLAN) },
                onOpenShop = { navController.navigate(Routes.SHOP) },
                onOpenSavings = { navController.navigate(Routes.SAVINGS) },
                onOpenGlossary = { navController.navigate(Routes.GLOSSARY) },
                onOpenTasks = { navController.navigate(Routes.TASKS) },
                onOpenGames = { navController.navigate(Routes.GAMES) },
                onOpenPet = { navController.navigate(Routes.WARDROBE) },
                onOpenProgress = { navController.navigate(Routes.HISTORY) },
                onOpenHelp = { navController.navigate(Routes.HELP) },
                onOpenAdult = { navController.navigate(Routes.ADULT) },
                // Итоги — только в конце сезона; их откроет эффект выше.
                onFinishPeriod = { viewModel.finishPeriod() },
            )
        }

        composable(Routes.PLAN) {
            PlanScreen(
                game = state.game,
                extras = state.extras,
                free = state.freeCoins,
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
                items = ru.onefortwo.finny.ui.state.Season.visibleItems(content.shopItems(), state.extras, state.profile?.appearance?.speciesId),
                jars = if (state.extras.planned) state.extras.needsJar to state.extras.wantsJar else null,
                petName = state.profile?.petName ?: "Финни",
                pet = state.game.pet,
                balance = balance,
                todayPurchases = state.game.period.purchases,
                planConfirmed = state.extras.planned,
                message = state.message,
                onDismissMessage = viewModel::dismissMessage,
                onBuy = viewModel::buy,
                onBack = { navController.popBackStack() },
                onOpenTasks = { navController.navigate(Routes.TASKS) },
            )
            state.savingsAsk?.let { ask ->
                if (ask.itemId != null) {
                    ru.onefortwo.finny.ui.screens.SavingsAskDialog(
                        ask,
                        onConfirm = viewModel::confirmSavingsAsk,
                        onCancel = viewModel::cancelSavingsAsk,
                        what = content.shopItems().firstOrNull { it.id == ask.itemId }?.title?.lowercase(),
                        dream = content.goals().firstOrNull { it.id == state.game.savings.goal?.id }?.claimTitle,
                    )
                }
            }
        }

        composable(Routes.TASKS) {
            TasksScreen(
                tasks = content.tasks(state.difficulty).map { it.named(petName) },
                completedIds = state.completedTaskIds,
                onOpenTask = { id -> navController.navigate("${Routes.TASK}/$id") },
                onBack = { navController.popBackStack() },
                // После неудачного дня задание помощи — сверху.
                // «Помоги своему питомцу» — всегда сверху, пока его не решили.
                recoveryFirst = true,
                solved = state.extras.tasksSolved,
                toSurprise = viewModel.tasksToSurprise(state),
            )
            state.extras.surprise?.let { id ->
                content.shopItems().firstOrNull { it.id == id }?.let { item ->
                    ru.onefortwo.finny.ui.screens.SurpriseDialog(
                        item,
                        state.extras.tasksSolved,
                        petName,
                        viewModel::dismissSurprise,
                        also = state.extras.surpriseAlso.mapNotNull { id -> content.shopItems().firstOrNull { it.id == id } },
                    )
                }
            }
        }

        composable("${Routes.TASK}/{taskId}") { entry ->
            val taskId = entry.arguments?.getString("taskId")
            // Числа берутся один раз на открытие экрана: при повороте
            // и перерисовке условие не должно меняться под руками. Поэтому
            // сохраняется зерно случайных чисел, а не сами числа: при повороте
            // Android пересоздаёт экран, обычная память композиции теряется,
            // а введённый ответ и результат восстанавливаются — без зерна
            // они относились бы уже к другому условию.
            // Задания с циферблатом при повторе — те же значения и тот же циферблат.
            val seed = rememberSaveable(taskId) {
                if (content.task(taskId ?: "")?.topic == ru.onefortwo.finny.content.TaskTopic.TIME) (taskId ?: "").hashCode().toLong() else Random.nextLong()
            }
            val task = remember(taskId, seed) {
                taskId?.let { content.task(it)?.withNumbers(Random(seed))?.named(petName) }
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
                    onAnswer = { answer -> viewModel.answerTask(task, answer) },
                    onBack = { navController.popBackStack() },
                    nextTask = nextTask?.named(petName),
                    petFigure = { happy -> TaskPet(state, content.petParts(), happy) },
                    voice = { text -> ru.onefortwo.finny.ui.state.PetVoice.of(state.game.stage, text) },
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
                onClaimGoal = {
                    // Мечта получена — сначала сцена, как питомец ею пользуется.
                    val claimed = state.game.savings.goal?.id
                    viewModel.claimGoal()
                    if (claimed != null) navController.navigate("${Routes.DREAM}/$claimed")
                },
                onPreviewWithdrawal = viewModel::previewWithdrawal,
                onWithdraw = viewModel::withdraw,
                onBack = { navController.popBackStack() },
                profile = state.profile,
                parts = content.petParts(),
                afterClaim = state.achievedGoalIds.isNotEmpty(),
                onOpenPlan = { navController.navigate(Routes.PLAN) },
                growthLine = Growth.claimLine(
                    stage = state.game.stage,
                    achievedBefore = state.achievedGoalIds.size,
                    tasks = state.extras.tasksSolved,
                    petName = state.profile?.petName ?: "Финни",
                    goalTitle = content.goals().firstOrNull { it.id == state.game.savings.goal?.id }?.title ?: "мечту",
                ),
                goalTip = Growth.goalTip(state.game.stage, state.achievedGoalIds.size, state.profile?.petName ?: "Финни"),
            )
        }

        composable(Routes.SEASON_RESULT) {
            ru.onefortwo.finny.ui.screens.SeasonResultScreen(
                state = state,
                parts = content.petParts(),
                goal = state.game.savings.goal?.let { g -> content.goals().firstOrNull { it.id == g.id } },
                onTransfer = viewModel::transferLeftover,
                onPlay = viewModel::play,
                onSleep = {
                    viewModel.closeSeason()
                    navController.popBackStack(Routes.MAIN, inclusive = false)
                },
            )
        }

        composable(Routes.GAMES) {
            // Раздел «Игры»: «Сам» — задания, «С питомцем» — игры без награды.
            LaunchedEffect(Unit) { viewModel.leaveTasks() }
            ru.onefortwo.finny.ui.screens.GamesScreen(
                state = state,
                parts = content.petParts(),
                onBack = { navController.popBackStack() },
                onOpenTasks = { navController.navigate(Routes.TASKS) },
                onOpenPlay = { navController.navigate(Routes.PLAY) },
                sleeping = state.isSleeping(today),
            )
        }

        composable(Routes.PLAY) {
            ru.onefortwo.finny.ui.screens.PlayScreen(
                state = state,
                parts = content.petParts(),
                onBack = { navController.popBackStack() },
                onPlayed = viewModel::play,
            )
        }

        composable("${Routes.DREAM}/{goalId}") { entry ->
            val goalId = entry.arguments?.getString("goalId").orEmpty()
            ru.onefortwo.finny.ui.screens.DreamScene(
                state = state,
                parts = content.petParts(),
                goalId = goalId,
                goalTitle = content.goals().firstOrNull { it.id == goalId }?.title ?: "Мечта",
                onDone = { navController.popBackStack() },
            )
        }

        composable(Routes.GROWTH) {
            ru.onefortwo.finny.ui.screens.GrowthCelebrationScreen(
                state = state,
                parts = content.petParts(),
                onDone = {
                    viewModel.growthCelebrated()
                    navController.popBackStack()
                },
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
                state = state,
                parts = content.petParts(),
                toSurprise = viewModel.tasksToSurprise(state),
                onOpenTasks = { navController.navigate(Routes.TASKS) },
                game = state.game,
                petName = state.profile?.petName ?: "Финни",
                balance = balance,
                tasks = content.tasks(state.difficulty),
                completedIds = state.completedTaskIds,
                goalTitle = goalTitle,
                achievedGoalTitles = content.goals()
                    .filter { it.id in state.achievedGoalIds }
                    .map { it.title },
                onBack = { navController.popBackStack() },
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
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.GLOSSARY) {
            // Заход в словарик запоминается: облачко над ним на дворе
            // не показывается до следующего игрового дня.
            LaunchedEffect(Unit) { viewModel.markTermsRead() }
            val entries = glossaryEntries(content, state)
            GlossaryScreen(
                entries = entries.map { it.copy(explanation = it.explanation.replace("{name}", petName)) },
                available = GlossaryTerms.available(state, entries.map { it.term }).toSet(),
                opened = state.extras.wordsOpened.toSet(),
                onOpenTerm = viewModel::openTerm,
                // Словарик остаётся в стеке: «Назад» из раздела возвращает к нему.
                onOpenLink = { link ->
                    when {
                        link == "budget" || link == "plan" || link == "fact" -> navController.navigate(Routes.PLAN)
                        link == "shop" -> navController.navigate(Routes.SHOP)
                        link == "savings" || link == "goal" -> navController.navigate(Routes.SAVINGS)
                        link == "main" -> navController.popBackStack(Routes.MAIN, inclusive = false)
                        link.startsWith("task:") -> navController.navigate("${Routes.TASK}/${link.removePrefix("task:")}")
                    }
                },
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
                motionEnabled = LocalMotionEnabled.current,
                onSetTimeLimit = viewModel::setTimeLimitEnabled,
                onSetMotion = viewModel::setMotionEnabled,
                onResetTodayUsage = viewModel::resetTodayUsage,
                onResetProfile = {
                    viewModel.resetProfile()
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                },
                // Демонстрационный режим начинается, как обычная игра, с подарка.
                onStartDemo = {
                    viewModel.startDemo()
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                },
                onResetDemo = {
                    viewModel.resetDemo()
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
    }
}

/**
 * Водяной знак демонстрационного режима: одна строка поверх всех экранов.
 * Стоит выше середины: на дворе — над холмами, где нет предметов и
 * подсказок, и не ложится на текст облачка. Полупрозрачный и не перехватывает нажатия; для TalkBack режим назван
 * на табличке дня, поэтому знак не озвучивается.
 */
@Composable
private fun DemoWatermark() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 190.dp)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.TopCenter,
    ) {
        Text(
            text = "Демо режим",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = FinnyTheme.colors.onSurface.copy(alpha = 0.16f),
            maxLines = 1,
            softWrap = false,
        )
    }
}

/**
 * Питомец ребёнка в ответе на задание: радуется при верном ответе,
 * спокоен при ошибке — без грусти и упрёка.
 */
@Composable
private fun TaskPet(state: AppState, parts: PetPartsContent, happy: Boolean) {
    val profile = state.profile ?: return
    val species = parts.species.firstOrNull { it.id == profile.appearance.speciesId }
    val color = parts.colors.firstOrNull { it.id == profile.appearance.colorId }
    PetFigure(
        petName = profile.petName,
        speciesId = profile.appearance.speciesId,
        speciesTitle = species?.title ?: "Питомец",
        accessoryId = profile.appearance.accessoryId,
        accessoryTitle = Accessories.title(parts, profile.appearance.accessoryId),
        colorHex = color?.hex ?: "#CCCCCC",
        stage = state.game.stage,
        care = if (happy) StatLevel.HIGH else StatLevel.MEDIUM,
        joy = if (happy) StatLevel.HIGH else StatLevel.MEDIUM,
        size = 96.dp,
        caption = false,
        plain = true,
        reaction = if (happy) PetReaction(PetReactions.PLAY, id = 1L) else null,
        modifier = Modifier.width(96.dp),
    )
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

/** Слова словарика для уровня сложности профиля — по порядку словаря. */
private fun glossaryEntries(content: ContentRepository, state: AppState): List<GlossaryEntry> =
    content.glossary().filter { !it.hardOnly || state.difficulty == Difficulty.HARDER }
