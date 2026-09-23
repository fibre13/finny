package ru.onefortwo.finny.ui.state

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.onefortwo.finny.content.ContentRepository
import ru.onefortwo.finny.content.PetAppearance
import ru.onefortwo.finny.content.TaskAnswer
import ru.onefortwo.finny.content.TaskCheck
import ru.onefortwo.finny.content.TaskContent
import ru.onefortwo.finny.content.check
import ru.onefortwo.finny.content.toDomain
import ru.onefortwo.finny.data.GameRepository
import ru.onefortwo.finny.data.SavedDisplaySettings
import ru.onefortwo.finny.economy.BudgetPlan
import ru.onefortwo.finny.economy.Coins
import ru.onefortwo.finny.economy.DepositResult
import ru.onefortwo.finny.economy.GameState
import ru.onefortwo.finny.economy.IncomeSource
import ru.onefortwo.finny.economy.PeriodCompletion
import ru.onefortwo.finny.economy.PlanConfirmation
import ru.onefortwo.finny.economy.PurchaseResult
import ru.onefortwo.finny.economy.Difficulty
import ru.onefortwo.finny.economy.WithdrawalPreview
import ru.onefortwo.finny.economy.WithdrawalResult
import ru.onefortwo.finny.economy.buy
import ru.onefortwo.finny.economy.chooseGoal
import ru.onefortwo.finny.economy.claimGoal
import ru.onefortwo.finny.economy.confirmPlan
import ru.onefortwo.finny.economy.deposit
import ru.onefortwo.finny.economy.earn
import ru.onefortwo.finny.economy.finishPeriod
import ru.onefortwo.finny.economy.previewWithdrawal
import ru.onefortwo.finny.economy.withdraw
import ru.onefortwo.finny.ui.theme.DisplaySettings
import ru.onefortwo.finny.ui.theme.ThemeMode

/**
 * Связывает экраны с правилами экономики и учебным контентом.
 *
 * Экраны не содержат расчётов: любое решение пользователя передаётся
 * в модуль `core-economy`, а результат превращается в объяснение
 * средствами [Explanations] (ТЗ 3.4).
 */
/** Источник текущей календарной даты; в тестах подменяется фиксированной. */
fun interface DateProvider {
    /** Сегодняшняя дата в формате ГГГГ-ММ-ДД. */
    fun today(): String
}

class GameViewModel(
    val content: ContentRepository,
    private val repository: GameRepository?,
    private val dates: DateProvider = DateProvider { LocalDate.now().toString() },
) : ViewModel() {

    /** Сегодняшняя дата по часам устройства. */
    fun today(): String = dates.today()

    private var usageTicker: Job? = null

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private val _display = MutableStateFlow(DisplaySettings())

    /**
     * Настройки отображения. Живут отдельно от состояния игры: их
     * выбирают на экране знакомства, когда профиля ещё нет, и сброс
     * профиля их не затрагивает.
     */
    val display: StateFlow<DisplaySettings> = _display.asStateFlow()

    private val petName: String get() = _state.value.profile?.petName ?: "Финни"

    init {
        // Автосохранение включается до чтения с устройства, поэтому ни одно
        // изменение состояния не может пройти мимо записи (ТЗ 2.5.13).
        viewModelScope.launch {
            var hadProfile = false

            _state.collect { current ->
                if (!current.isLoaded) return@collect

                val toSave = current.toSavedGame()
                when {
                    toSave != null -> {
                        repository?.save(toSave)
                        hadProfile = true
                    }

                    hadProfile -> {
                        repository?.clear()
                        hadProfile = false
                    }
                }
            }
        }

        viewModelScope.launch {
            repository?.observeDisplaySettings()?.collect { saved ->
                if (saved != null) {
                    _display.value = DisplaySettings(
                        themeMode = runCatching { ThemeMode.valueOf(saved.themeMode) }
                            .getOrDefault(ThemeMode.SYSTEM),
                        highContrast = saved.highContrast,
                    )
                }
            }
        }

        viewModelScope.launch {
            val saved = repository?.load()

            _state.update { current ->
                // Если пользователь успел создать профиль, пока читалось
                // сохранённое состояние, его действие не отменяется.
                when {
                    current.hasProfile -> current.copy(isLoaded = true)
                    saved != null -> saved.toAppState()
                    else -> current.copy(isLoaded = true)
                }
            }
        }
    }

    /**
     * Меняет настройки отображения. Применяются сразу и записываются
     * на устройство: выбор взрослого не должен теряться при перезапуске.
     */
    fun setDisplaySettings(settings: DisplaySettings) {
        _display.value = settings
        viewModelScope.launch {
            repository?.saveDisplaySettings(
                SavedDisplaySettings(
                    themeMode = settings.themeMode.name,
                    highContrast = settings.highContrast,
                ),
            )
        }
    }

    // --- Профиль ---------------------------------------------------------

    /** Создаёт локальный профиль: игровое имя и внешность, без личных данных. */
    fun createProfile(petName: String, appearance: PetAppearance, difficulty: Difficulty) {
        _state.value = freshProfile(
            petName = petName.trim(),
            appearance = appearance,
            isDemo = false,
            difficulty = difficulty,
        )
    }

    /** Полный сброс профиля; доступен взрослому (ТЗ 2.5.12, 3.5). */
    fun resetProfile() {
        val previous = _state.value
        _state.value = AppState(
            isLoaded = true,
            usageDate = previous.usageDate,
            usageMinutes = previous.usageMinutes,
            timeLimitEnabled = previous.timeLimitEnabled,
        )
    }

    /**
     * Включает демонстрационный режим: создаёт тестовый профиль с
     * фиксированными данными и выбранной целью, чтобы обязательный сценарий
     * проходился подряд без ожидания календарных сроков (ТЗ 2.5.13).
     */
    fun startDemo() {
        val demo = freshProfile(
            petName = DEMO_PET_NAME,
            appearance = DEMO_APPEARANCE,
            isDemo = true,
            difficulty = DEMO_DIFFICULTY,
            keepUsage = _state.value,
        )
        val goal = content.goals().firstOrNull { it.id == DEMO_GOAL_ID }

        _state.value = if (goal == null) {
            demo
        } else {
            demo.copy(game = demo.game.chooseGoal(goal.toDomain()))
        }
    }

    /** Возвращает тестовый профиль к исходному состоянию (ТЗ 2.5.13). */
    fun resetDemo() = startDemo()

    /** Начальное состояние профиля со стартовым бюджетом. */
    private fun freshProfile(
        petName: String,
        appearance: PetAppearance,
        isDemo: Boolean,
        difficulty: Difficulty,
        keepUsage: AppState? = null,
    ): AppState {
        val game = GameState.newProfile()

        return AppState(
            profile = Profile(petName = petName, appearance = appearance),
            game = game,
            completedTaskIds = emptySet(),
            lastOutcome = null,
            isDemo = isDemo,
            isLoaded = true,
            difficulty = difficulty,
            // Счётчик экранного времени относится к устройству, а не к профилю,
            // поэтому при сбросе профиля он не обнуляется: иначе ограничение
            // обходилось бы простым пересозданием питомца.
            usageDate = keepUsage?.usageDate,
            usageMinutes = keepUsage?.usageMinutes ?: 0,
            timeLimitEnabled = keepUsage?.timeLimitEnabled ?: true,
            // Украшение, выбранное при создании питомца, доступно в гардеробе
            // без покупки: иначе его нельзя было бы вернуть, сняв однажды.
            ownedAccessories = setOfNotNull(
                appearance.accessoryId.takeIf { it != PetAppearanceDefaults.NONE },
            ),
            message = Explanations.income(
                source = IncomeSource.START_BUDGET,
                amount = IncomeSource.START_BUDGET.amount,
                balanceAfter = game.balance,
            ),
        )
    }

    fun dismissMessage() {
        _state.update { it.copy(message = null) }
    }

    // --- План бюджета ----------------------------------------------------

    fun confirmPlan(needs: Int, wants: Int, savings: Int) {
        val plan = BudgetPlan(Coins(needs), Coins(wants), Coins(savings))

        when (val result = _state.value.game.confirmPlan(plan)) {
            is PlanConfirmation.Success -> _state.update {
                val moved = result.movedToSavings
                it.copy(
                    game = result.state,
                    message = FeedbackMessage(
                        text = if (moved.amount > 0) {
                            "План на день готов. ${Explanations.coins(moved)} сразу ушли в копилку."
                        } else {
                            "План на день готов."
                        },
                        nextStep = "Теперь купи то, что нужно ${petName}.",
                    ),
                )
            }

            is PlanConfirmation.ExceedsBudget -> showProblem(
                "Ты распределил больше, чем есть: лишние ${Explanations.coins(result.excess)}.",
                "Уменьши одно из направлений.",
            )

            PlanConfirmation.AlreadyConfirmed -> showProblem(
                "План на сегодня уже составлен.",
                "Изменить его можно будет завтра.",
            )

            PlanConfirmation.NoGoalSelected -> showProblem(
                "Чтобы откладывать в копилку, нужно выбрать цель.",
                "Загляни в раздел «Копилка» и выбери, на что копишь.",
            )
        }
    }

    // --- Покупки ---------------------------------------------------------

    fun buy(itemId: String) {
        val itemContent = content.shopItems().firstOrNull { it.id == itemId } ?: return

        when (val result = _state.value.game.buy(itemContent.toDomain())) {
            is PurchaseResult.Success -> _state.update {
                val unlocked = itemContent.unlocksAccessory
                val scenery = itemContent.unlocksScenery
                it.copy(
                    game = result.state,
                    ownedAccessories = if (unlocked != null) {
                        it.ownedAccessories + unlocked
                    } else {
                        it.ownedAccessories
                    },
                    // Обстановка остаётся навсегда: покупка вещи, которая
                    // видна на фоне, не отменяется сменой дня.
                    ownedScenery = if (scenery != null) {
                        it.ownedScenery + scenery
                    } else {
                        it.ownedScenery
                    },
                    message = Explanations.purchase(
                        petName = petName,
                        result = result,
                        itemTitle = itemContent.title,
                        unlockedWardrobe = unlocked != null,
                    ),
                )
            }

            is PurchaseResult.NotEnoughCoins -> _state.update {
                it.copy(message = Explanations.notEnoughCoins(result, itemContent.title))
            }
        }
    }

    // --- Накопления и цель -----------------------------------------------

    fun chooseGoal(goalId: String) {
        val goal = content.goals().firstOrNull { it.id == goalId } ?: return

        _state.update {
            it.copy(
                game = it.game.chooseGoal(goal.toDomain()),
                message = FeedbackMessage(
                    text = "Цель выбрана: ${goal.title} за ${Explanations.coins(goal.price)}.",
                    nextStep = "Откладывай понемногу каждый день — так цель станет ближе.",
                ),
            )
        }
    }

    /**
     * Получает накопленную цель: стоимость списывается из копилки, цель
     * попадает в список достигнутых, место текущей освобождается.
     */
    fun claimGoal() {
        val current = _state.value
        val title = current.game.savings.goal
            ?.let { goal -> content.goals().firstOrNull { it.id == goal.id }?.title }
            ?: return
        val (game, goal) = current.game.claimGoal() ?: return

        _state.update {
            it.copy(
                game = game,
                achievedGoalIds = it.achievedGoalIds + goal.id,
                message = FeedbackMessage(
                    text = "Ты накопил на «$title» и получил её. " +
                        "В копилке осталось ${Explanations.coins(game.savings.saved)}.",
                    nextStep = "Выбери следующую цель — копить станет на что.",
                ),
            )
        }
    }

    fun deposit(amount: Int) {
        if (amount <= 0) return

        when (val result = _state.value.game.deposit(Coins(amount))) {
            is DepositResult.Success -> _state.update {
                it.copy(
                    game = result.state,
                    message = FeedbackMessage(
                        text = "В копилке стало ${Explanations.coins(result.savedAfter)}. " +
                            Explanations.forecast(result.state.goalForecast()),
                    ),
                )
            }

            is DepositResult.NotEnoughCoins -> showProblem(
                "На балансе только ${Explanations.coins(result.balance)}, " +
                    "не хватает ${Explanations.coins(result.shortfall)}.",
                "Отложи сумму поменьше или выполни задание.",
            )

            DepositResult.NoGoalSelected -> showProblem(
                "Сначала выбери цель, на которую копишь.",
                null,
            )
        }
    }

    /** Показ последствий снятия до подтверждения (ТЗ 2.5.7). */
    fun previewWithdrawal(amount: Int): WithdrawalPreview =
        _state.value.game.previewWithdrawal(Coins(amount))

    fun withdraw(amount: Int) {
        when (val result = _state.value.game.withdraw(Coins(amount))) {
            is WithdrawalResult.Success -> _state.update {
                it.copy(
                    game = result.state,
                    message = FeedbackMessage(
                        text = "Ты забрал ${Explanations.coins(amount)} из копилки. " +
                            "Осталось ${Explanations.coins(result.savedAfter)}. " +
                            Explanations.forecast(result.state.goalForecast()),
                    ),
                )
            }

            is WithdrawalResult.NotEnoughSavings -> showProblem(
                "В копилке только ${Explanations.coins(result.saved)}.",
                null,
            )
        }
    }

    // --- Задания ---------------------------------------------------------

    /**
     * Проверяет ответ и начисляет награду.
     *
     * Задание передаётся целиком, а не идентификатором: числа в нём
     * взяты при открытии экрана, и проверять ответ нужно против того
     * условия, которое ребёнок видел, а не против записи в содержимом.
     *
     * За повторное прохождение начисляется половина награды. Совсем
     * без награды повтор выглядел странно: задание открыто, числа новые,
     * а монет нет. Полная награда за повтор превращала бы задания
     * в источник монет без обучения.
     */
    fun answerTask(task: TaskContent, answer: TaskAnswer): TaskCheck? {
        val taskId = task.id
        val check = task.check(answer)
        val repeat = taskId in _state.value.completedTaskIds
        val reward = if (repeat) check.reward.amount.half() else check.reward.amount

        val (game, event) = _state.value.game.earn(check.reward, reward)
        _state.update {
            it.copy(
                game = game,
                completedTaskIds = it.completedTaskIds + taskId,
                message = Explanations.income(
                    source = event.source,
                    amount = event.amount,
                    balanceAfter = event.balanceAfter,
                    repeat = repeat,
                ),
            )
        }

        return check
    }

    // --- Игровой период --------------------------------------------------

    fun finishPeriod() {
        when (val result = _state.value.game.finishPeriod()) {
            is PeriodCompletion.Success -> _state.update {
                it.copy(
                    game = result.state,
                    lastOutcome = result.outcome,
                    // Дата нужна, чтобы второй игровой день не начинался
                    // в те же сутки. В демонстрационном режиме не пишется.
                    lastFinishedDate = if (it.isDemo) it.lastFinishedDate else dates.today(),
                    message = Explanations.periodSummary(petName, result.outcome),
                )
            }

            PeriodCompletion.PlanNotConfirmed -> showProblem(
                "Сначала составь план на день.",
                "Открой раздел «План».",
            )

            PeriodCompletion.NoDecision -> showProblem(
                "За день ещё нет ни одного решения.",
                "Купи что-нибудь для ${petName} или отложи монеты в копилку.",
            )
        }
    }

    private fun showProblem(text: String, nextStep: String?) {
        _state.update {
            it.copy(message = FeedbackMessage(text = text, nextStep = nextStep, isProblem = true))
        }
    }

    // --- Экранное время ---------------------------------------------------

    /**
     * Приложение вышло на передний план: счётчик экранного времени идёт
     * раз в минуту. Минутной точности достаточно для предела в 20 минут,
     * при этом запись в базу происходит не чаще раза в минуту.
     */
    fun onSessionStart() {
        if (usageTicker?.isActive == true) return

        usageTicker = viewModelScope.launch {
            while (true) {
                delay(MINUTE_MILLIS)
                countMinute()
            }
        }
    }

    /** Приложение ушло с переднего плана: счётчик останавливается. */
    fun onSessionStop() {
        usageTicker?.cancel()
        usageTicker = null
    }

    private fun countMinute() {
        val today = dates.today()

        _state.update {
            if (it.usageDate == today) {
                it.copy(usageMinutes = it.usageMinutes + 1)
            } else {
                it.copy(usageDate = today, usageMinutes = 1)
            }
        }
    }

    /** Включение и отключение ограничения экранного времени взрослым. */
    fun setTimeLimitEnabled(enabled: Boolean) {
        _state.update { it.copy(timeLimitEnabled = enabled) }
    }

    /** Сброс счётчика на сегодня; доступен взрослому. */
    fun resetTodayUsage() {
        _state.update { it.copy(usageDate = dates.today(), usageMinutes = 0) }
    }

    // --- Гардероб ---------------------------------------------------------

    /**
     * Смена окраса и украшения питомца. Вид и имя не меняются: это опознание
     * питомца. Украшение доступно, только если куплено в каталоге, поэтому
     * гардероб остаётся следствием финансовых решений.
     */
    fun changeAppearance(colorId: String, accessoryId: String) {
        val current = _state.value
        val profile = current.profile ?: return

        if (!current.isAccessoryAvailable(accessoryId)) {
            showProblem(
                "Это украшение ещё не куплено.",
                "Купи его в разделе «Покупки», и оно появится в гардеробе.",
            )
            return
        }

        _state.update {
            it.copy(
                profile = profile.copy(
                    appearance = profile.appearance.copy(
                        colorId = colorId,
                        accessoryId = accessoryId,
                    ),
                ),
                message = FeedbackMessage(text = "${profile.petName} переоделся."),
            )
        }
    }

    /** Фабрика: репозиторий контента читает assets, репозиторий игры — базу. */
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val app = context.applicationContext
            @Suppress("UNCHECKED_CAST")
            return GameViewModel(
                content = ContentRepository(app),
                repository = GameRepository(app),
            ) as T
        }
    }

    private companion object {
        /** Фиксированные данные тестового профиля для экспертной проверки. */
        const val DEMO_PET_NAME = "Финни"
        const val DEMO_GOAL_ID = "scooter"
        val DEMO_DIFFICULTY = Difficulty.HARDER
        const val MINUTE_MILLIS = 60_000L
        val DEMO_APPEARANCE = PetAppearance(
            speciesId = "cat",
            colorId = "ginger",
            accessoryId = "bow",
        )
    }
}
