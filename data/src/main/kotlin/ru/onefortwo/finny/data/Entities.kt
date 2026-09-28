package ru.onefortwo.finny.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Структура хранения локального профиля (ТЗ 5.4).
 *
 * Данные не покидают устройство: аккаунт, сеть и персональные данные
 * не используются. Игровое имя питомца и его внешность персональными
 * данными не являются (ТЗ 3.5).
 */

/**
 * Основная запись профиля: одна на устройство. Содержит состояние игры
 * на текущий момент и незавершённый игровой период.
 */
@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey
    val id: Long = DEFAULT_ID,

    /** Игровое имя питомца, введённое ребёнком. */
    val petName: String,

    /** Вид питомца: котёнок, щенок, крольчонок. */
    val speciesId: String,

    /** Окрас питомца. */
    val colorId: String,

    /** Аксессуар питомца. */
    val accessoryId: String,

    /** Профиль создан для демонстрационного режима (ТЗ 2.5.13). */
    val isDemo: Boolean,

    /** Доступный баланс в монетах. */
    val balance: Int,

    /** Сумма в копилке. */
    val savedCoins: Int,

    /** Выбранная цель либо null, если цель не выбрана. */
    val goalId: String?,

    /** Стоимость выбранной цели; хранится вместе с целью, чтобы не зависеть от контента. */
    val goalPrice: Int?,

    /** Показатель «Забота», 0–100. */
    val care: Int,

    /** Показатель «Радость», 0–100. */
    val joy: Int,

    /** Накопленные очки роста. */
    val growthPoints: Int,

    /** Номер текущего игрового периода, начиная с единицы. */
    val periodNumber: Int,

    /** Суммы подтверждённого плана; null, если план ещё не подтверждён. */
    val planNeeds: Int?,
    val planWants: Int?,
    val planSavings: Int?,

    /** Сколько отложено в копилку за текущий период. */
    val depositedThisPeriod: Int,

    /**
     * Выбранный уровень сложности заданий: имя `Difficulty` (ТЗ 2.5.8).
     *
     * Хранится имя, а не номер класса: приложение не спрашивает ни класс,
     * ни возраст, поэтому и в базе сведений о ребёнке не остаётся.
     */
    val difficulty: String = "SIMPLE",

    /**
     * Календарная дата последнего завершённого игрового дня в формате
     * ГГГГ-ММ-ДД. В обычном режиме второй игровой день в те же сутки не
     * начинается; демонстрационный режим ограничения не имеет (ТЗ 2.5.13).
     */
    val lastFinishedDate: String? = null,

    /** Дата, к которой относится счётчик экранного времени. */
    val usageDate: String? = null,

    /** Сколько минут приложение было открыто в этот день. */
    val usageMinutes: Int = 0,

    /** Ограничение экранного времени включено; выключается взрослым. */
    val timeLimitEnabled: Boolean = true,

    /** Сколько новых заданий оплачено в игровом дне [paidTasksPeriod]. */
    val paidTasksPeriod: Int = 0,
    val paidTasksCount: Int = 0,

    /** Слова словарика, встреченные в игре; через «|». */
    @ColumnInfo(defaultValue = "")
    val knownTerms: String = "",

    /** Встреченные, но ещё не прочитанные в словарике слова; через «|». */
    @ColumnInfo(defaultValue = "")
    val newTerms: String = "",

    /** Напоминания, отложенные кнопкой «Не сейчас»: «номер дня:код|код». */
    @ColumnInfo(defaultValue = "")
    val dismissedReminders: String = "",
) {
    companion object {
        /** Профиль в приложении один, поэтому ключ фиксирован. */
        const val DEFAULT_ID = 1L
    }
}

/** Покупка текущего, ещё не завершённого периода (ТЗ 2.5.6). */
@Entity(tableName = "current_purchases")
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemId: String,
    val price: Int,
    /** Направление расхода: NEEDS или WANTS. */
    val category: String,
)

/**
 * Итог завершённого игрового периода. Хранится для раздела прогресса
 * и сравнения плана с фактом (ТЗ 2.5.11).
 */
@Entity(tableName = "period_history")
data class PeriodOutcomeEntity(
    @PrimaryKey
    val number: Int,

    val planNeeds: Int,
    val planWants: Int,
    val planSavings: Int,

    val spentNeeds: Int,
    val spentWants: Int,
    val deposited: Int,

    /** Выполненные условия начисления очков роста. */
    val needsCovered: Boolean,
    val withinPlan: Boolean,
    val savedSomething: Boolean,

    val careBefore: Int,
    val joyBefore: Int,
    val careAfter: Int,
    val joyAfter: Int,

    val pointsBefore: Int,
    val pointsAfter: Int,

    /** Начисление в начале следующего периода; null, если его не было. */
    val incomeSource: String?,
    val incomeAmount: Int?,
    val incomeBalanceAfter: Int?,
)

/** Задание, за которое награда уже получена (ТЗ 2.5.11). */
@Entity(tableName = "completed_tasks")
data class CompletedTaskEntity(
    @PrimaryKey
    val taskId: String,
)

/**
 * Украшение, купленное в каталоге и потому доступное в гардеробе.
 * Переодеть питомца можно только в то, что куплено, поэтому гардероб
 * остаётся следствием финансовых решений.
 */
@Entity(tableName = "owned_accessories")
data class OwnedAccessoryEntity(
    @PrimaryKey
    val accessoryId: String,
)

/**
 * Обстановка, купленная в каталоге и потому видимая на фоне главного
 * экрана.
 *
 * Хранится отдельно от покупок периода: `current_purchases` очищается при
 * завершении дня, а `period_history` держит только суммы по направлениям.
 * Без этой таблицы дом исчезал бы наутро после покупки.
 */
@Entity(tableName = "owned_scenery")
data class OwnedSceneryEntity(
    @PrimaryKey
    val sceneryId: String,
)

/**
 * Цель, на которую уже накоплено и которую ребёнок получил.
 *
 * Хранится отдельно от текущей цели в профиле: после получения цель
 * освобождается, чтобы можно было выбрать следующую, а список достигнутых
 * остаётся видимым в «Моём прогрессе» (ТЗ 2.5.11).
 */
@Entity(tableName = "achieved_goals")
data class AchievedGoalEntity(
    @PrimaryKey
    val goalId: String,
)

/**
 * Настройки отображения: тема, режим высокого контраста и движения
 * питомца.
 *
 * Хранятся отдельно от профиля: сброс профиля их не затрагивает.
 * Взрослый, отключивший движения, не должен настраивать это заново
 * после сброса игры.
 */
@Entity(tableName = "display_settings")
data class DisplaySettingsEntity(
    @PrimaryKey
    val id: Long = DEFAULT_ID,

    /** Тема: SYSTEM, LIGHT или DARK. */
    val themeMode: String,

    /** Чёрно-белый режим с усиленным контуром и крупным текстом. */
    val highContrast: Boolean,

    /** Движения питомца и кнопок: дыхание, моргание, реакции, пульсация (ТЗ 3.6). */
    @ColumnInfo(defaultValue = "1")
    val motionEnabled: Boolean = true,
) {
    companion object {
        /** Запись одна на устройство, поэтому ключ фиксирован. */
        const val DEFAULT_ID = 1L
    }
}
