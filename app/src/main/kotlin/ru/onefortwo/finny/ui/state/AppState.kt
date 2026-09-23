package ru.onefortwo.finny.ui.state

import ru.onefortwo.finny.content.PetAppearance
import ru.onefortwo.finny.data.SavedGame
import ru.onefortwo.finny.economy.GameState
import ru.onefortwo.finny.economy.PeriodOutcome
import ru.onefortwo.finny.economy.Difficulty

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
) {
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
        accessoryId == PetAppearanceDefaults.NONE || accessoryId in ownedAccessories

    /** Куплена ли обстановка с таким кодом. */
    fun hasScenery(sceneryId: String): Boolean = sceneryId in ownedScenery
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
    )
}
