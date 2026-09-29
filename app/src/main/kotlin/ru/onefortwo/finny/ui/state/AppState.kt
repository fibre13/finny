package ru.onefortwo.finny.ui.state

import ru.onefortwo.finny.content.Accessories
import ru.onefortwo.finny.content.PetAppearance
import ru.onefortwo.finny.data.SavedGame
import ru.onefortwo.finny.economy.GameState
import ru.onefortwo.finny.economy.PeriodOutcome
import ru.onefortwo.finny.economy.Difficulty
import ru.onefortwo.finny.economy.WithdrawalPreview

/** Профиль игрока: игровое имя питомца и его внешность. */
data class Profile(
    val petName: String,
    val appearance: PetAppearance,
)

/**
 * Сообщение обратной связи после действия: что изменилось и почему,
 * плюс предложение следующего шага (ТЗ 2.5.9).
 */
data class FeedbackMessage(
    val text: String,
    val nextStep: String? = null,
    /** Сообщение о затруднении: оформляется иначе и сопровождается пиктограммой. */
    val isProblem: Boolean = false,
)

/**
 * Правила экранного времени.
 *
 * Предел взят из санитарных требований к непрерывной работе с планшетом
 * для начальной школы: не более 20 минут. Ограничение не обрывает действие
 * на середине и отключается взрослым, чтобы не превращаться в давление.
 */
object ScreenTime {
    /** Предел на день в минутах. */
    const val DAILY_LIMIT_MINUTES = 20

    /** За сколько минут до предела предупредить ребёнка. */
    const val WARNING_AT_MINUTES = 15
}

/** Состояние приложения целиком. */
data class AppState(
    val profile: Profile? = null,
    val game: GameState = GameState.newProfile(),
    /** Задания, за которые награда уже получена. */
    val completedTaskIds: Set<String> = emptySet(),
    /** Итог последнего завершённого периода для экрана результатов. */
    val lastOutcome: PeriodOutcome? = null,
    val message: FeedbackMessage? = null,
    /** Профиль создан для экспертной проверки в демонстрационном режиме (ТЗ 2.5.13). */
    val isDemo: Boolean = false,
    /** Сохранённое состояние прочитано с устройства. */
    val isLoaded: Boolean = false,

    /**
     * Взрослый включил демонстрационный режим, и ребёнок проходит знакомство:
     * профиль, созданный в конце знакомства, будет тестовым. Не сохраняется:
     * до создания профиля сохранять нечего.
     */
    val demoPending: Boolean = false,

    /** Класс ребёнка: задаёт сложность заданий (ТЗ 2.5.8). */
    val difficulty: Difficulty = Difficulty.SIMPLE,

    /** Календарная дата последнего завершённого игрового дня, ГГГГ-ММ-ДД. */
    val lastFinishedDate: String? = null,

    /** Дата, к которой относится счётчик экранного времени. */
    val usageDate: String? = null,

    /** Сколько минут приложение было открыто сегодня. */
    val usageMinutes: Int = 0,

    /** Ограничение экранного времени включено; выключается взрослым. */
    val timeLimitEnabled: Boolean = true,

    /** Украшения, купленные в каталоге и доступные в гардеробе. */
    val ownedAccessories: Set<String> = emptySet(),

    /**
     * Купленная обстановка: видна на фоне главного экрана. Надевать её,
     * в отличие от украшения, не нужно — купленное появляется сразу.
     */
    val ownedScenery: Set<String> = emptySet(),

    /** Цели, которые ребёнок уже накопил и получил. */
    val achievedGoalIds: Set<String> = emptySet(),

    /**
     * Сколько новых заданий оплачено монетами в игровом дне
     * [paidTasksPeriod]. Монеты платятся за первые [TaskPay.PER_DAY].
     */
    val paidTasksPeriod: Int = 0,
    val paidTasksCount: Int = 0,

    /**
     * Пришли монеты — стартовые или карманные на новый день.
     * Показывается отдельным окном, а не карточкой среди других призывов.
     */
    val arrival: FeedbackMessage? = null,

    /** Слова словарика, встреченные в игре (см. [GlossaryTerms]). */
    val knownTerms: Set<String> = emptySet(),

    /** Встреченные, но ещё не прочитанные в словарике: отмечены «Новое». */
    val newTerms: Set<String> = emptySet(),

    /** Напоминания двора, отложенные «Не сейчас» в игровом дне [remindersPeriod]. */
    val dismissedReminders: Set<String> = emptySet(),
    val remindersPeriod: Int = 0,

    /** Сезон, банки, события дня, сюрпризы, рост. */
    val extras: SeasonExtras = SeasonExtras(),

    /** Итог решения по событию — показывается в окне события. Не сохраняется. */
    val eventResult: EventResult? = null,

    /** В банках не хватает — спросить, взять ли из копилки. Не сохраняется. */
    val savingsAsk: SavingsAsk? = null,
) {
    /** Свободные монеты — не разложены по банкам. */
    val freeCoins: Int get() = Season.free(game.balance.amount, extras)

    /** День сезона — 1, 2 или 3. */
    val seasonDay: Int get() = Season.dayOf(game.period.number)

    /** События сегодняшнего дня решены. */
    val dayEventsDone: Boolean
        get() = extras.eventsDay == game.period.number && extras.answered >= extras.dayEvents.size
    /** Напоминание отложено до конца текущего игрового дня. */
    fun isReminderDismissed(reminder: Reminder): Boolean =
        remindersPeriod == game.period.number && reminder.id in dismissedReminders

    /** Профиль создан и можно вести игру. */
    val hasProfile: Boolean get() = profile != null

    /**
     * Игровой день на сегодня уже прожит: в обычном режиме второй день
     * в те же сутки не начинается. В демонстрационном режиме ограничения нет,
     * иначе обязательный сценарий нельзя было бы показать подряд (ТЗ 2.5.13).
     */
    fun isDayFinished(today: String): Boolean = !isDemo && lastFinishedDate == today

    /** Сколько минут сегодня уже потрачено. */
    fun minutesUsed(today: String): Int = if (usageDate == today) usageMinutes else 0

    /** Экранное время на сегодня исчерпано. */
    fun isTimeUp(today: String): Boolean =
        !isDemo && timeLimitEnabled && minutesUsed(today) >= ScreenTime.DAILY_LIMIT_MINUTES

    /** Пора предупредить, что время заканчивается. */
    fun isTimeRunningOut(today: String): Boolean =
        !isDemo && timeLimitEnabled && !isTimeUp(today) &&
            minutesUsed(today) >= ScreenTime.WARNING_AT_MINUTES

    /** Сколько минут осталось до предела. */
    fun minutesLeft(today: String): Int =
        (ScreenTime.DAILY_LIMIT_MINUTES - minutesUsed(today)).coerceAtLeast(0)

    /** Доступно ли украшение для гардероба: без украшения — всегда. */
    fun isAccessoryAvailable(accessoryId: String): Boolean =
        Accessories.list(accessoryId).all { it in ownedAccessories }

    /** Сколько новых заданий уже оплачено сегодня. */
    val paidTasksToday: Int get() = if (paidTasksPeriod == game.period.number) paidTasksCount else 0

    /**
     * День закрыт, питомец спит до новых календарных суток. В обычном
     * режиме карманные придут при первом входе в новые сутки.
     */
    fun isSleeping(today: String): Boolean = isDayFinished(today)

    /** Куплена ли обстановка с таким кодом. */
    fun hasScenery(sceneryId: String): Boolean = sceneryId in ownedScenery
}

/** Монеты за задания — только за первые новые задания дня. */
object TaskPay {
    const val PER_DAY = 2
}

/** Значения внешности, доступные без покупки. */
object PetAppearanceDefaults {
    /** Вариант «без украшения» доступен всегда. */
    const val NONE = "none"
}

/** Переводит сохранённое состояние в состояние приложения. */
fun SavedGame.toAppState(): AppState = AppState(
    profile = Profile(
        petName = petName,
        appearance = PetAppearance(speciesId, colorId, accessoryId),
    ),
    game = game,
    completedTaskIds = completedTaskIds,
    isDemo = isDemo,
    isLoaded = true,
    difficulty = difficulty,
    lastFinishedDate = lastFinishedDate,
    usageDate = usageDate,
    usageMinutes = usageMinutes,
    timeLimitEnabled = timeLimitEnabled,
    ownedAccessories = ownedAccessories,
    ownedScenery = ownedScenery,
    achievedGoalIds = achievedGoalIds,
    paidTasksPeriod = paidTasksPeriod,
    paidTasksCount = paidTasksCount,
    knownTerms = knownTerms,
    newTerms = newTerms,
    dismissedReminders = dismissedReminders,
    remindersPeriod = remindersPeriod,
    // Профиль из версии без сезонов: стадия, выросшая по прежним шагам роста,
    // уже отпразднована — праздник роста за мечты для неё не показывается.
    extras = if (extras.isBlank()) SeasonExtras(growthShown = game.stage) else SeasonExtras.decode(extras),
)

/** Переводит состояние приложения в сохраняемый вид; null, если профиля нет. */
fun AppState.toSavedGame(): SavedGame? {
    val profile = profile ?: return null

    return SavedGame(
        petName = profile.petName,
        speciesId = profile.appearance.speciesId,
        colorId = profile.appearance.colorId,
        accessoryId = profile.appearance.accessoryId,
        isDemo = isDemo,
        game = game,
        completedTaskIds = completedTaskIds,
        difficulty = difficulty,
        lastFinishedDate = lastFinishedDate,
        usageDate = usageDate,
        usageMinutes = usageMinutes,
        timeLimitEnabled = timeLimitEnabled,
        ownedAccessories = ownedAccessories,
        ownedScenery = ownedScenery,
        achievedGoalIds = achievedGoalIds,
        paidTasksPeriod = paidTasksPeriod,
        paidTasksCount = paidTasksCount,
        knownTerms = knownTerms,
        newTerms = newTerms,
        dismissedReminders = dismissedReminders,
        remindersPeriod = remindersPeriod,
        extras = extras.encode(),
    )
}

/**
 * Итог решения по событию. [transfer] — сколько монет можно
 * перевести в копилку вместо отказа от желаемого; 0 — не предлагать.
 */
data class EventResult(
    val eventId: String,
    val accepted: Boolean,
    val emotion: String?,
    val hint: String?,
    val transfer: Int = 0,
    /** Монет не хватило даже с копилкой. */
    val shortage: Boolean = false,
)

/** Покупка ждёт решения — взять ли недостающее из копилки. */
data class SavingsAsk(
    /** `event` — событие дня, иначе код товара лавки. */
    val eventId: String?,
    val itemId: String?,
    val payment: Payment,
    val preview: WithdrawalPreview,
)
