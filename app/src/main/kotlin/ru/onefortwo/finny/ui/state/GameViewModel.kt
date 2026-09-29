package ru.onefortwo.finny.ui.state

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.onefortwo.finny.content.Accessories
import ru.onefortwo.finny.content.ContentRepository
import ru.onefortwo.finny.content.ItemCategory
import ru.onefortwo.finny.content.PetAppearance
import ru.onefortwo.finny.content.TaskAnswer
import ru.onefortwo.finny.content.TaskCheck
import ru.onefortwo.finny.content.TaskContent
import ru.onefortwo.finny.content.TaskTopic
import ru.onefortwo.finny.content.check
import ru.onefortwo.finny.content.toDomain
import ru.onefortwo.finny.data.GameRepository
import ru.onefortwo.finny.data.SavedDisplaySettings
import ru.onefortwo.finny.economy.GrowthStage
import ru.onefortwo.finny.economy.BudgetPlan
import ru.onefortwo.finny.economy.Coins
import ru.onefortwo.finny.economy.DepositResult
import ru.onefortwo.finny.economy.GameState
import ru.onefortwo.finny.economy.startNewDay
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
import ru.onefortwo.finny.content.EventContent
import ru.onefortwo.finny.content.PetStat
import ru.onefortwo.finny.content.withPetName
import ru.onefortwo.finny.economy.BudgetCategory
import ru.onefortwo.finny.economy.GrowthStage as Stage
import ru.onefortwo.finny.economy.PetStatKind
import ru.onefortwo.finny.economy.ShopItem
import ru.onefortwo.finny.economy.PeriodState
import java.time.temporal.ChronoUnit

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

/**
 * Итог ответа на задание: проверка и фактически начисленная сумма.
 *
 * Сумма передаётся отдельно от источника награды: за повтор начисляется
 * половина, и карточка результата обязана показывать именно её, а не полную
 * награду источника. Иначе баланс менялся бы не на ту сумму, что названа
 * ребёнку (ТЗ 2.5.4).
 */
data class AnsweredTask(
    val check: TaskCheck,
    val credited: Coins,
    val isRepeat: Boolean,
)

class GameViewModel(
    val content: ContentRepository,
    private val repository: GameRepository?,
    private val dates: DateProvider = DateProvider { LocalDate.now().toString() },
    private val hours: () -> Int = { LocalTime.now().hour },
    private val clock: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {

    /** Сегодняшняя дата по часам устройства. */
    fun today(): String = dates.today()

    private var usageTicker: Job? = null

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private val petName: String get() = _state.value.profile?.petName ?: "Финни"

    /**
     * Реакции питомца, ждущие возвращения на главный экран. Хранятся
     * отдельно от [state]: это оформление, а не игровое состояние, и в
     * сохранение не попадают.
     */
    private val _reactions = MutableStateFlow<List<PetReaction>>(emptyList())
    val reactions: StateFlow<List<PetReaction>> = _reactions.asStateFlow()
    private var reactionCount = 0L

    private fun react(kind: String, stageBefore: GrowthStage? = null) {
        _reactions.update { queued ->
            (queued + PetReaction(kind, ++reactionCount, stageBefore)).takeLast(PetReactions.MAX_QUEUED)
        }
    }

    /**
     * Реплика питомца в облачке на дворе. Приветствие готовится
     * при каждом возвращении в приложение и показывается один раз.
     */
    private val _speech = MutableStateFlow<String?>(null)
    val speech: StateFlow<String?> = _speech.asStateFlow()
    private var greetPending = true

    /** Двор открыт: если ребёнок только что вошёл, питомец здоровается. */
    fun greetIfPending() {
        val current = _state.value
        if (!greetPending || !current.hasProfile) return
        greetPending = false
        _speech.value = PetSpeech.greeting(current.usageDate, dates.today(), hours())
    }

    /** Реплика показана — облачко убирается. */
    fun speechShown() {
        _speech.value = null
    }

    /** Реакция проиграна или пропущена — убирается из очереди. */
    fun reactionPlayed(id: Long) {
        _reactions.update { queued -> queued.filterNot { it.id == id } }
    }

    /**
     * Настройки отображения устройства. Хранятся отдельно от профиля и
     * сбросом профиля не затрагиваются.
     */
    private var display = SavedDisplaySettings(themeMode = "SYSTEM", highContrast = false)
    private val _motionEnabled = MutableStateFlow(true)

    /** Движения питомца и кнопок включены; выключаются взрослым (ТЗ 3.6). */
    val motionEnabled: StateFlow<Boolean> = _motionEnabled.asStateFlow()

    /** Включение и отключение движений в разделе для взрослого. */
    fun setMotionEnabled(enabled: Boolean) {
        _motionEnabled.value = enabled
        display = display.copy(motionEnabled = enabled)
        val toSave = display
        viewModelScope.launch { repository?.saveDisplaySettings(toSave) }
    }

    init {
        viewModelScope.launch {
            repository?.observeDisplaySettings()?.first()?.let { saved ->
                display = saved
                _motionEnabled.value = saved.motionEnabled
            }
        }

        // Автосохранение включается до чтения с устройства, поэтому ни одно
        // изменение состояния не может пройти мимо записи (ТЗ 2.5.13).
        viewModelScope.launch {
            var hadProfile = false

            _state.collect { current ->
                if (!current.isLoaded) return@collect

                // Встреченные слова словарика отмечаются по состоянию игры,
                // что бы ни изменилось. Отметка — новое состояние: оно
                // придёт сюда же следующим и будет записано.
                val noted = GlossaryTerms.note(current)
                if (noted !== current) {
                    _state.update { GlossaryTerms.note(it) }
                    return@collect
                }

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

    // --- Профиль ---------------------------------------------------------

    /** Создаёт локальный профиль: игровое имя и внешность, без личных данных. */
    fun createProfile(petName: String, appearance: PetAppearance, difficulty: Difficulty) {
        _reactions.value = emptyList()
        val previous = _state.value
        val demo = previous.demoPending
        val profile = freshProfile(
            petName = petName.trim(),
            appearance = appearance,
            isDemo = demo,
            difficulty = difficulty,
            keepUsage = previous,
        )
        // В демонстрационном режиме цель выбрана заранее: сценарий проверки
        // проходится подряд, без лишних шагов (ТЗ 2.5.13).
        val goal = if (demo) content.goals().firstOrNull { it.id == DEMO_GOAL_ID } else null
        _state.value = if (goal == null) profile else profile.copy(game = profile.game.chooseGoal(goal.toDomain()))
    }

    /** Полный сброс профиля; доступен взрослому (ТЗ 2.5.12, 3.5). */
    fun resetProfile() {
        _reactions.value = emptyList()
        val previous = _state.value
        _state.value = AppState(
            isLoaded = true,
            usageDate = previous.usageDate,
            usageMinutes = previous.usageMinutes,
            timeLimitEnabled = previous.timeLimitEnabled,
        )
    }

    /**
     * Включает демонстрационный режим. Игра начинается так же, как обычная:
     * подарок, выбор питомца и имени. Профиль, созданный в конце знакомства,
     * тестовый: этапы идут подряд без ожидания календарных сроков, цель
     * выбрана заранее (ТЗ 2.5.13).
     */
    fun startDemo() {
        _reactions.value = emptyList()
        val previous = _state.value
        _state.value = AppState(
            isLoaded = true,
            demoPending = true,
            usageDate = previous.usageDate,
            usageMinutes = previous.usageMinutes,
            timeLimitEnabled = previous.timeLimitEnabled,
        )
    }

    /** Возвращает тестовый профиль к исходному состоянию — к подарку (ТЗ 2.5.13). */
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
            ownedAccessories = Accessories.list(appearance.accessoryId).toSet(),
            extras = SeasonExtras(season = 1, seasonStart = dates.today()),
            arrival = FeedbackMessage(
                text = "Тебе дали ${Explanations.coins(IncomeSource.START_BUDGET.amount)}.",
                nextStep = "Это монеты на весь сезон — на три дня. Разложи их по банкам: «Нужное», «Хочу» и «Копим на мечту».",
            ),
        )
    }

    fun dismissMessage() {
        _state.update { it.copy(message = null) }
    }

    /** Окно «Пришли монеты» прочитано. */
    fun dismissArrival() {
        _state.update { it.copy(arrival = null) }
    }

    /** Словарик открыт: новые слова прочитаны, отметка «Новое» снимается. */
    fun markTermsRead() {
        _state.update { if (it.newTerms.isEmpty()) it else it.copy(newTerms = emptySet()) }
    }

    /** «Не сейчас»: напоминание не показывается до конца игрового дня. */
    fun dismissReminder(reminder: Reminder) {
        if (reminder == Reminder.PLAN_FIX) {
            _state.update { it.copy(extras = it.extras.copy(correctionAsked = true)) }
            return
        }
        _state.update {
            val day = it.game.period.number
            val already = if (it.remindersPeriod == day) it.dismissedReminders else emptySet()
            it.copy(dismissedReminders = already + reminder.id, remindersPeriod = day)
        }
    }

    /**
     * Новые календарные сутки — начинается новый игровой день.
     * Если прошлый день закрыт в обычном режиме, карманные на новый день
     * были отложены; они приходят сейчас, при первом входе в новые сутки.
     * Не в полночь: работа в фоне не нужна, а пропуск дня ничем не грозит.
     */
    fun startNewDayIfDue() {
        val current = _state.value
        if (!current.hasProfile) return
        expireSeasonIfDue()
        val now = _state.value
        val finished = now.lastFinishedDate
        if (finished != null && !now.isDemo && finished < dates.today() && !now.extras.seasonDone) {
            _state.value = beginDay(now.copy(lastFinishedDate = null))
        }
        ensureDayEvents()
    }

    /**
     * Начало игрового дня. Если начинается новый сезон — приходят
     * 50 монет, банки высыпаются в свободные монеты и план составляется
     * заново. Итоги прошлого сезона к этому времени уже показаны.
     */
    private fun beginDay(state: AppState): AppState {
        val period = state.game.period.number
        val season = Season.seasonOf(period)
        if (season == state.extras.season) {
            return state.copy(
                arrival = if (state.isDemo) null else FeedbackMessage(
                    text = "Новый день! День ${Season.dayOf(period)} из $SEASON_DAYS в сезоне.",
                    nextStep = "${state.profile?.petName ?: "Питомец"} проснулся. Посмотрим, что сегодня случится.",
                ),
            )
        }
        val (game, event) = state.game.earn(IncomeSource.SEASON_MONEY)
        val x = state.extras
        return state.copy(
            game = game,
            extras = SeasonExtras(
                season = season,
                seasonStart = dates.today(),
                tasksSolved = x.tasksSolved,
                tasksDay = x.tasksDay,
                tasksToday = x.tasksToday,
                rewarded = x.rewarded,
                unlocked = x.unlocked,
                surprise = x.surprise,
                growthShown = x.growthShown,
                playedDay = x.playedDay,
                missed = x.missed,
            ),
            arrival = FeedbackMessage(
                text = "Новый сезон! Пришли ${Explanations.coins(event.amount)}.",
                nextStep = "Теперь у тебя ${Explanations.coins(event.balanceAfter)}. " +
                    "Разложи их по банкам в «Плане» — это монеты на три дня.",
            ),
        )
    }

    /**
     * Сезон, который не закончили за шесть календарных дней,
     * закрывается сам. Новый сезон начинается с монетами: без них ребёнку
     * нечем кормить питомца, а ошибка должна оставаться безопасной (ТЗ 2.2).
     */
    private fun expireSeasonIfDue() {
        val current = _state.value
        val start = current.extras.seasonStart ?: return
        if (current.isDemo || current.extras.seasonDone) return
        val days = runCatching {
            ChronoUnit.DAYS.between(LocalDate.parse(start), LocalDate.parse(dates.today()))
        }.getOrDefault(0L)
        if (days < SEASON_EXPIRES_DAYS) return
        val next = Season.firstPeriodOf(current.extras.season + 1)
        val moved = current.copy(
            game = current.game.copy(period = PeriodState(number = next)),
            lastFinishedDate = null,
            extras = current.extras.copy(missed = true),
        )
        _state.value = beginDay(moved)
    }

    /** События на сегодня выбираются один раз, в начале дня, после плана. */
    fun ensureDayEvents() {
        _state.update { current ->
            val x = current.extras
            val period = current.game.period.number
            if (!current.hasProfile || !x.planned || x.eventsDay == period ||
                current.isDayFinished(dates.today()) || x.seasonDone
            ) {
                current
            } else {
                val picked = Season.pickDay(
                    events = content.events(),
                    period = period,
                    used = x.usedEvents,
                    refused = x.refused,
                    scenery = current.ownedScenery,
                    achievedGoals = current.achievedGoalIds,
                )
                current.copy(extras = x.copy(eventsDay = period, dayEvents = picked, answered = 0))
            }
        }
    }

    /** Событие, которое ждёт решения, или `null`. */
    fun pendingEvent(state: AppState = _state.value): EventContent? {
        val x = state.extras
        if (!x.planned || x.eventsDay != state.game.period.number || state.eventResult != null) return null
        val id = x.dayEvents.getOrNull(x.answered) ?: return null
        return content.events().firstOrNull { it.id == id }
    }

    /** Питомец спит — разделы дня откроются завтра. */
    fun sleepingHint() {
        _state.update {
            it.copy(
                message = FeedbackMessage(
                    text = "$petName спит. Новый день начнётся завтра — тогда придут и монеты.",
                    nextStep = "Пока можно заглянуть в словарик или в гардероб.",
                ),
            )
        }
    }

    // --- План бюджета ----------------------------------------------------

    /**
     * Подтверждает план. Возвращает `true`, если план принят.
     *
     * Результат нужен экрану плана: закрываться он должен только при успехе.
     * Иначе отклонённый план выглядел бы принятым, а введённые суммы
     * пропадали бы вместе с экраном и их пришлось бы набирать заново.
     */
    fun confirmPlan(needs: Int, wants: Int, savings: Int): Boolean {
        val current = _state.value
        val x = current.extras
        // Раскладываются все свободные монеты и то, что лежит в банках.
        val available = current.freeCoins + x.needsJar + x.wantsJar
        if (needs < 0 || wants < 0 || savings < 0 || needs + wants + savings != available) {
            showProblem(
                "Разложи все монеты по банкам: осталось ${Explanations.coins(available - needs - wants - savings)}.",
                null,
            )
            return false
        }
        if (savings > 0 && current.game.savings.goal == null) {
            showProblem(
                "Чтобы откладывать в копилку, нужно выбрать цель.",
                "Нажми «Выбрать цель» — введённые суммы сохранятся.",
            )
            return false
        }
        var game = current.game
        if (savings > 0) {
            val result = game.deposit(Coins(savings)) as? DepositResult.Success ?: return false
            game = result.state
            react(PetReactions.SAVE)
        }
        val first = !x.planned
        _state.update {
            it.copy(
                game = game,
                extras = x.copy(
                    planned = true,
                    needsJar = needs,
                    wantsJar = wants,
                    plannedNeeds = x.spentNeeds + needs,
                    plannedWants = x.spentWants + wants,
                    plannedSavings = x.plannedSavings + savings,
                    deposited = x.deposited + savings,
                    correctionAsked = x.correctionAsked || !first,
                ),
                message = FeedbackMessage(
                    text = if (first) "План на сезон готов!" else "План поправлен.",
                    nextStep = if (savings > 0) "${Explanations.coins(savings)} сразу ушли в копилку." else null,
                ),
            )
        }
        ensureDayEvents()
        return true
    }

    // --- Покупки ---------------------------------------------------------

    fun buy(itemId: String) {
        val item = content.shopItems().firstOrNull { it.id == itemId } ?: return
        val current = _state.value
        val payment = Season.payment(
            item.price,
            item.category,
            current.game.balance.amount,
            current.game.savings.saved.amount,
            current.extras,
        )
        when {
            payment.impossible -> if (item.category == ItemCategory.WANTS) {
                showProblem(
                    "В банке «Хочу» не хватает монет на «${item.title}».",
                    "Желаемое покупают из банка «Хочу». Можно поправить план.",
                )
            } else {
                showProblem(
                    "Не хватает монет на «${item.title}» даже вместе с копилкой.",
                    "Давай в следующем сезоне спланируем лучше?",
                )
            }

            payment.needsSavings -> _state.update {
                it.copy(savingsAsk = SavingsAsk(null, itemId, payment, it.game.previewWithdrawal(Coins(payment.fromSavings))))
            }

            else -> completeShopPurchase(itemId, payment)
        }
    }

    private fun completeShopPurchase(itemId: String, payment: Payment) {
        val itemContent = content.shopItems().firstOrNull { it.id == itemId } ?: return
        val withdrawn = withdrawFor(payment) ?: return
        when (val result = withdrawn.game.buy(itemContent.toDomain())) {
            is PurchaseResult.Success -> {
                react(if (itemContent.category == ItemCategory.NEEDS) PetReactions.EAT else PetReactions.PLAY)
                _state.update {
                    val unlocked = itemContent.unlocksAccessory
                    val scenery = itemContent.unlocksScenery
                    it.copy(
                        game = result.state,
                        extras = spend(withdrawn.extras, payment, itemContent.category),
                        savingsAsk = null,
                        ownedAccessories = if (unlocked != null) it.ownedAccessories + unlocked else it.ownedAccessories,
                        ownedScenery = if (scenery != null) it.ownedScenery + scenery else it.ownedScenery,
                        message = Explanations.purchase(
                            petName = petName,
                            result = result,
                            itemTitle = itemContent.title,
                            unlockedWardrobe = unlocked != null,
                        ),
                    )
                }
            }

            is PurchaseResult.NotEnoughCoins -> _state.update {
                it.copy(savingsAsk = null, message = Explanations.notEnoughCoins(result, itemContent.title))
            }
        }
    }

    /** Снимает из копилки недостающее по [payment]; `null` — снять не удалось. */
    private fun withdrawFor(payment: Payment): AppState? {
        val current = _state.value
        if (payment.fromSavings <= 0) return current
        val result = current.game.withdraw(Coins(payment.fromSavings)) as? WithdrawalResult.Success ?: return null
        val x = current.extras
        return current.copy(game = result.state, extras = x.copy(deposited = (x.deposited - payment.fromSavings).coerceAtLeast(0)))
    }

    /** Банки и факт сезона после траты по [payment]. */
    private fun spend(x: SeasonExtras, payment: Payment, category: ItemCategory): SeasonExtras = x.copy(
        needsJar = (x.needsJar - payment.fromNeeds).coerceAtLeast(0),
        wantsJar = (x.wantsJar - payment.fromWants).coerceAtLeast(0),
        spentNeeds = x.spentNeeds + if (category == ItemCategory.NEEDS) payment.price else 0,
        spentWants = x.spentWants + if (category == ItemCategory.WANTS) payment.price else 0,
        wantsToNeeds = x.wantsToNeeds + if (category == ItemCategory.NEEDS) payment.fromWants else 0,
    )

    /** «Взять из копилки» подтверждено: покупка или событие завершаются. */
    fun confirmSavingsAsk() {
        val ask = _state.value.savingsAsk ?: return
        when {
            ask.itemId != null -> completeShopPurchase(ask.itemId, ask.payment)
            ask.eventId != null -> acceptCostEvent(ask.eventId, ask.payment)
        }
    }

    /** «Не брать» — окно закрывается, решение по событию ещё впереди. */
    fun cancelSavingsAsk() {
        _state.update { it.copy(savingsAsk = null) }
    }

    // --- События сезона ----------------------------------------

    /** Ответ на событие дня: [accept] — первая кнопка («Купить», «Поиграть»). */
    fun answerEvent(accept: Boolean) {
        val current = _state.value
        val event = pendingEvent(current) ?: return
        if (event.cost && accept) {
            val category = event.category ?: ItemCategory.NEEDS
            val payment = Season.payment(
                event.price,
                category,
                current.game.balance.amount,
                current.game.savings.saved.amount,
                current.extras,
            )
            when {
                payment.impossible -> decline(event, shortage = true)
                payment.needsSavings -> _state.update {
                    it.copy(savingsAsk = SavingsAsk(event.id, null, payment, it.game.previewWithdrawal(Coins(payment.fromSavings))))
                }
                else -> acceptCostEvent(event.id, payment)
            }
            return
        }
        if (accept) {
            var game = current.game
            if (event.coins > 0) game = game.earn(IncomeSource.GIFT, Coins(event.coins)).first
            val stat = event.stat
            if (stat != null && event.statDelta > 0) game = game.copy(pet = game.pet.changed(stat.toKind(), event.statDelta))
            react(PetReactions.PLAY)
            finishEvent(event, accepted = true, game = game, extras = current.extras)
        } else {
            decline(event, shortage = false)
        }
    }

    private fun acceptCostEvent(eventId: String, payment: Payment) {
        val event = content.events().firstOrNull { it.id == eventId } ?: return
        val withdrawn = withdrawFor(payment) ?: return
        val category = event.category ?: ItemCategory.NEEDS
        val needs = category == ItemCategory.NEEDS
        val stat = (event.stat ?: if (needs) PetStat.CARE else PetStat.JOY).toKind()
        val item = ShopItem(
            id = "event:${event.id}",
            price = Coins(event.price),
            category = if (needs) BudgetCategory.NEEDS else BudgetCategory.WANTS,
            stat = stat,
            statDelta = event.statDelta.coerceAtLeast(1),
        )
        val result = withdrawn.game.buy(item) as? PurchaseResult.Success
        if (result == null) {
            decline(event, shortage = true)
            return
        }
        react(if (needs) PetReactions.EAT else PetReactions.PLAY)
        event.unlocksAccessory?.let { acc -> _state.update { it.copy(ownedAccessories = it.ownedAccessories + acc) } }
        finishEvent(event, accepted = true, game = result.state, extras = spend(withdrawn.extras, payment, category))
    }

    private fun decline(event: EventContent, shortage: Boolean) {
        val current = _state.value
        var game = current.game
        val stat = event.declineStat
        if (stat != null && event.declineDelta != 0) game = game.copy(pet = game.pet.changed(stat.toKind(), event.declineDelta))
        finishEvent(event, accepted = false, game = game, extras = current.extras, shortage = shortage)
    }

    private fun finishEvent(
        event: EventContent,
        accepted: Boolean,
        game: GameState,
        extras: SeasonExtras,
        shortage: Boolean = false,
    ) {
        val teen = game.stage != Stage.BABY
        val raw = if (accepted) event.emotionYes else event.emotionNo ?: event.emotionYes
        val emotion = raw?.withPetName(petName)?.let { if (teen) PetVoice.teen(it) else it }
        val hint = when {
            // Желаемое берётся только из банка «Хочу»; нужное — со всех банков и копилки.
            shortage && event.category == ItemCategory.WANTS ->
                "В банке «Хочу» не хватает монет. Желаемое можно отложить — или поправить план."
            shortage -> "Монет не хватило даже вместе с копилкой. Ты не успел накопить. " +
                "Давай в следующем сезоне спланируем лучше?"
            !event.cost -> null
            accepted -> event.hintYes
            else -> event.hintNo
        }?.withPetName(petName)
        // Отказ от желаемого — можно не просто не тратить, а отложить эти монеты.
        val transfer = if (!accepted && !shortage && event.cost && event.category == ItemCategory.WANTS &&
            game.savings.goal != null && extras.wantsJar >= event.price && event.price > 0
        ) {
            event.price
        } else {
            0
        }
        _state.update {
            it.copy(
                game = game,
                savingsAsk = null,
                extras = extras.copy(
                    answered = extras.answered + 1,
                    usedEvents = extras.usedEvents + event.id,
                    refused = if (accepted) null else event.id,
                ),
                eventResult = EventResult(event.id, accepted, emotion, hint, transfer, shortage),
            )
        }
    }

    /** Окно события закрыто; [transfer] — перевести предложенные монеты в копилку. */
    fun closeEventResult(transfer: Boolean) {
        val current = _state.value
        val result = current.eventResult ?: return
        if (transfer && result.transfer > 0) {
            val deposit = current.game.deposit(Coins(result.transfer)) as? DepositResult.Success
            if (deposit != null) {
                react(PetReactions.SAVE)
                val x = current.extras
                _state.update {
                    it.copy(
                        game = deposit.state,
                        eventResult = null,
                        extras = x.copy(
                            wantsJar = x.wantsJar - result.transfer,
                            deposited = x.deposited + result.transfer,
                            plannedWants = x.plannedWants - result.transfer,
                            plannedSavings = x.plannedSavings + result.transfer,
                        ),
                        message = FeedbackMessage(text = "${Explanations.coins(result.transfer)} ушли в копилку. Мечта стала ближе!"),
                    )
                }
                return
            }
        }
        _state.update { it.copy(eventResult = null) }
    }

    /** Поиграть с питомцем после дел дня — радость растёт раз в день. */
    fun play() {
        val current = _state.value
        val day = current.game.period.number
        if (current.extras.playedDay == day) return
        react(PetReactions.PLAY)
        _state.update {
            it.copy(
                game = it.game.copy(pet = it.game.pet.changed(PetStatKind.JOY, PLAY_JOY)),
                extras = it.extras.copy(playedDay = day),
            )
        }
    }

    /** Остаток в банках в конце сезона — в копилку. */
    fun transferLeftover() {
        val current = _state.value
        val amount = current.extras.needsJar + current.extras.wantsJar + current.freeCoins
        if (amount <= 0 || current.game.savings.goal == null) return
        val deposit = current.game.deposit(Coins(amount)) as? DepositResult.Success ?: return
        react(PetReactions.SAVE)
        _state.update {
            it.copy(
                game = deposit.state,
                extras = it.extras.copy(needsJar = 0, wantsJar = 0, deposited = it.extras.deposited + amount),
            )
        }
    }

    /** Итоги сезона закрыты — новый сезон начинается сейчас (демо) или завтра. */
    fun closeSeason() {
        val current = _state.value
        val closed = current.copy(extras = current.extras.copy(seasonDone = false))
        val finished = current.lastFinishedDate
        _state.value = when {
            current.isDemo -> beginDay(closed)
            // Итоги закрыли уже на следующий день — новый сезон начинается сразу.
            finished != null && finished < dates.today() -> beginDay(closed.copy(lastFinishedDate = null))
            else -> closed
        }
        ensureDayEvents()
    }

    /** О сюрпризе сказали. */
    fun dismissSurprise() {
        _state.update { it.copy(extras = it.extras.copy(surprise = null)) }
    }

    /** Праздник роста показан. */
    fun growthCelebrated() {
        _state.update { it.copy(extras = it.extras.copy(growthShown = it.game.stage)) }
    }

    /** Приветствие после пропущенного сезона показано. */
    fun missedShown() {
        _state.update { it.copy(extras = it.extras.copy(missed = false)) }
    }

    /** Стадия по мечтам и заданиям; очки роста ядра выставляются под неё. */
    private fun AppState.withGrowth(): AppState {
        val stage = Growth.stageFor(achievedGoalIds.size, extras.tasksSolved)
        val best = maxOf(stage, game.stage)
        return if (game.growthPoints == best.requiredPoints) this else copy(game = game.copy(growthPoints = best.requiredPoints))
    }

    // --- Накопления и цель -----------------------------------------------

    fun chooseGoal(goalId: String) {
        val goal = content.goals().firstOrNull { it.id == goalId } ?: return

        _state.update {
            it.copy(
                game = it.game.chooseGoal(goal.toDomain()),
                message = FeedbackMessage(
                    text = "Цель выбрана: ${goal.title} за ${Explanations.coins(goal.price)}.",
                    nextStep = "Откладывай в копилку, когда составляешь план, — так мечта станет ближе.",
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
        val (game, goal) = current.game.claimGoal() ?: return

        // Отклика внизу нет: праздник уже был на экране копилки, а остаток
        // называет плашка «В копилке N монет — это старт для новой мечты».
        // Питомец радуется награде, когда ребёнок вернётся на главную.
        react(PetReactions.REWARD)
        _state.update {
            it.copy(
                game = game,
                achievedGoalIds = it.achievedGoalIds + goal.id,
                message = null,
            ).withGrowth()
        }
    }

    fun deposit(amount: Int) {
        if (amount <= 0) return

        when (val result = _state.value.game.deposit(Coins(amount))) {
            is DepositResult.Success -> {
                react(PetReactions.SAVE)
                _state.update {
                    it.copy(
                        game = result.state,
                        message = FeedbackMessage(
                            text = "В копилке стало ${Explanations.coins(result.savedAfter)}. " +
                                Explanations.forecast(result.state.goalForecast()),
                        ),
                    )
                }
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
                    extras = it.extras.copy(deposited = (it.extras.deposited - amount).coerceAtLeast(0)),
                    message = FeedbackMessage(
                        text = "Ты забрал ${Explanations.coins(amount)} из копилки — разложи их в «Плане». " +
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
     *
     * Задание попадает в пройденные при любом ответе, в том числе
     * ошибочном: за ошибку тоже начисляется награда за старание, и без
     * отметки каждый повторный ошибочный ответ приносил бы её полностью.
     * Отметка переводит повторы на половинную награду и делает повторение
     * невыгодным; число повторов она не ограничивает. Поэтому интерфейс
     * называет такие задания «уже решал», а не «выполнено»: отметка
     * означает попытку, а не верный ответ.
     */
    fun answerTask(task: TaskContent, answer: TaskAnswer): AnsweredTask {
        val taskId = task.id
        val check = task.check(answer)
        val current = _state.value
        // Круг заданий. Когда решены все задания уровня, следующий
        // ответ начинает новый круг: звёзды на списке — заново.
        val levelIds = content.tasks(current.difficulty)
            .filter { it.topic != TaskTopic.RECOVERY }
            .map { it.id }
            .toSet()
        val roundDone = levelIds.isNotEmpty() && current.completedTaskIds.containsAll(levelIds)
        val solvedBefore = if (roundDone && taskId in levelIds) current.completedTaskIds - levelIds else current.completedTaskIds
        val repeat = taskId in solvedBefore
        // Монеты — за первые TaskPay.PER_DAY заданий дня, считая и
        // повторы; «Помоги своему питомцу» — путь восстановления, оплачивается всегда.
        // Монеты — только за «Помоги своему питомцу»; остальные
        // задания открывают сюрпризы в лавке.
        val counted = check.reward != IncomeSource.RECOVERY_TASK
        val overLimit = counted
        val reward = when {
            overLimit -> Coins.ZERO
            repeat -> check.reward.amount.half()
            else -> check.reward.amount
        }

        val (game, event) = current.game.earn(check.reward, reward)
        if (event.amount.amount > 0) react(PetReactions.REWARD)
        val paid = counted && !overLimit
        _state.update {
            val day = it.game.period.number
            val x = it.extras
            val counted3 = x.copy(
                tasksSolved = x.tasksSolved + 1,
                tasksDay = day,
                tasksToday = if (x.tasksDay == day) x.tasksToday + 1 else 1,
            )
            val surprise = Season.pendingSurprise(content.shopItems(), counted3)
            val withSurprise = if (surprise == null) {
                counted3
            } else {
                counted3.copy(
                    rewarded = counted3.rewarded + surprise.unlockAfter!!,
                    unlocked = counted3.unlocked + surprise.id,
                    surprise = surprise.id,
                )
            }
            val next = Season.tasksToNextSurprise(content.shopItems(), withSurprise)
            it.copy(
                game = game,
                completedTaskIds = solvedBefore + taskId,
                paidTasksPeriod = if (paid) it.game.period.number else it.paidTasksPeriod,
                paidTasksCount = if (paid) it.paidTasksToday + 1 else it.paidTasksCount,
                extras = withSurprise,
                message = if (event.amount.amount > 0) {
                    Explanations.income(
                        source = event.source,
                        amount = event.amount,
                        balanceAfter = event.balanceAfter,
                        repeat = repeat,
                    )
                } else {
                    FeedbackMessage(
                        text = "Задание засчитано. Решено заданий: ${withSurprise.tasksSolved}.",
                        nextStep = next?.let { n -> "До сюрприза в лавке — ${Explanations.tasks(n)}." },
                    )
                },
            ).withGrowth()
        }

        return AnsweredTask(check = check, credited = event.amount, isRepeat = repeat)
    }

    // --- Игровой период --------------------------------------------------

    fun finishPeriod() {
        val current = _state.value
        if (!current.dayEventsDone) {
            showProblem("Сначала реши все события дня.", "Питомец ждёт тебя на дворе.")
            return
        }
        val lastDay = current.seasonDay == SEASON_DAYS
        when (val result = current.game.finishPeriod(payIncome = false, force = true)) {
            is PeriodCompletion.Success -> {
                _state.update {
                    val finished = it.copy(
                        game = result.state,
                        lastOutcome = result.outcome,
                        lastFinishedDate = if (it.isDemo) it.lastFinishedDate else dates.today(),
                        extras = it.extras.copy(seasonDone = lastDay),
                        message = when {
                            lastDay -> null
                            // В демонстрационном режиме следующий день начинается сразу.
                            it.isDemo -> FeedbackMessage(
                                text = "$petName выспался. Начался день ${Season.dayOf(result.state.period.number)} из $SEASON_DAYS.",
                            )
                            else -> FeedbackMessage(text = "$petName спит. Пока-пока! Приходи завтра.")
                        },
                    ).withGrowth()
                    // В демонстрационном режиме следующий день начинается сразу;
                    // после третьего — когда закрыты итоги сезона.
                    if (it.isDemo && !lastDay) beginDay(finished) else finished
                }
                ensureDayEvents()
            }

            else -> showProblem("День пока не закончить.", null)
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
        // Поворот экрана — не новый вход: приветствие только после
        // паузы дольше минуты или при первом открытии.
        if (stoppedAt == 0L || clock() - stoppedAt > GREET_AFTER_MS) greetPending = true
        startNewDayIfDue()
        if (usageTicker?.isActive == true) return

        usageTicker = viewModelScope.launch {
            while (true) {
                delay(MINUTE_MILLIS)
                countMinute()
            }
        }
    }

    /** Приложение ушло с переднего плана: счётчик останавливается. */
    private var stoppedAt = 0L

    fun onSessionStop() {
        stoppedAt = clock()
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

    private fun PetStat.toKind(): PetStatKind = if (this == PetStat.CARE) PetStatKind.CARE else PetStatKind.JOY

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
        /** Цель тестового профиля: выбрана заранее для экспертной проверки. */
        const val DEMO_GOAL_ID = "scooter"
        const val MINUTE_MILLIS = 60_000L
        const val GREET_AFTER_MS = 60_000L

        /** Радость от игры с питомцем. */
        const val PLAY_JOY = 10
    }
}
