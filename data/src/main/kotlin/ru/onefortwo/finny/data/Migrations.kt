package ru.onefortwo.finny.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Переходы между версиями базы данных.
 *
 * Каждое изменение структуры сущностей обязано сопровождаться переходом:
 * иначе при обновлении приложения у тех, у кого уже есть данные, база не
 * откроется и приложение упадёт при запуске. Прогресс при этом должен
 * сохраняться (ТЗ 2.5.13), поэтому сброс базы вместо перехода не
 * применяется.
 */

/**
 * Версия 1 → 2: класс ребёнка для выбора сложности заданий, привязка
 * игрового дня к календарному, счётчик экранного времени и список
 * купленных украшений для гардероба.
 *
 * Все добавленные столбцы имеют значения по умолчанию, поэтому уже
 * существующие профили остаются пригодными: ребёнок продолжает игру
 * с первого класса, без ограничения по дню и с нулевым счётчиком времени.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `profile` ADD COLUMN `grade` INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE `profile` ADD COLUMN `lastFinishedDate` TEXT")
        db.execSQL("ALTER TABLE `profile` ADD COLUMN `usageDate` TEXT")
        db.execSQL("ALTER TABLE `profile` ADD COLUMN `usageMinutes` INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
            "ALTER TABLE `profile` ADD COLUMN `timeLimitEnabled` INTEGER NOT NULL DEFAULT 1",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `owned_accessories` " +
                "(`accessoryId` TEXT NOT NULL, PRIMARY KEY(`accessoryId`))",
        )

        // Украшение, которое питомец уже носит, считается купленным:
        // иначе после обновления его нельзя было бы надеть повторно,
        // сняв в гардеробе.
        db.execSQL(
            "INSERT OR IGNORE INTO `owned_accessories` (`accessoryId`) " +
                "SELECT `accessoryId` FROM `profile` WHERE `accessoryId` <> 'none'",
        )
    }
}

/**
 * Версия 2 → 3: обстановка, купленная в каталоге и видимая на фоне
 * главного экрана.
 *
 * Новых данных у существующих профилей нет: таблица создаётся пустой.
 * Восстановить покупки задним числом невозможно — `current_purchases`
 * очищается при завершении дня, а `period_history` хранит только суммы
 * по направлениям. Поэтому у тех, кто купил «Домик-палатку» до
 * обновления, дом появится только после повторной покупки.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `owned_scenery` " +
                "(`sceneryId` TEXT NOT NULL, PRIMARY KEY(`sceneryId`))",
        )
    }
}

/**
 * Версия 3 → 4: настройки отображения — тема и чёрно-белый режим.
 *
 * Таблица создаётся пустой. Отсутствие записи означает значения по
 * умолчанию: тема следует системной настройке, высокий контраст выключен.
 * Так у тех, кто обновился, ничего не меняется.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `display_settings` " +
                "(`id` INTEGER NOT NULL, `themeMode` TEXT NOT NULL, " +
                "`highContrast` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
    }
}

/**
 * 4 → 5: список достигнутых целей.
 *
 * Таблица создаётся пустой. У тех, кто обновился с накопленной целью,
 * она остаётся текущей, и получить её можно на экране копилки — прежний
 * прогресс от этого не теряется.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `achieved_goals` " +
                "(`goalId` TEXT NOT NULL, PRIMARY KEY(`goalId`))",
        )
    }
}

/**
 * 5 → 6: вместо класса ребёнка хранится выбранный уровень сложности.
 *
 * Класс — сведение о ребёнке, и хранение его на устройстве само по себе
 * не делает его неперсональным. Для подбора заданий достаточно выбора
 * «попроще или посложнее», поэтому столбец `grade` не переименовывается,
 * а исчезает: таблица создаётся заново без него. Прежние профили
 * переносятся по правилу «первый и второй класс — попроще, третий
 * и четвёртый — посложнее», то есть набор заданий у ребёнка остаётся тем же.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `profile_new` (`id` INTEGER NOT NULL, `petName` " +
            "TEXT NOT NULL, `speciesId` TEXT NOT NULL, `colorId` TEXT NOT NULL, " +
            "`accessoryId` TEXT NOT NULL, `isDemo` INTEGER NOT NULL, `balance` INTEGER " +
            "NOT NULL, `savedCoins` INTEGER NOT NULL, `goalId` TEXT, `goalPrice` INTEGER, " +
            "`care` INTEGER NOT NULL, `joy` INTEGER NOT NULL, `growthPoints` INTEGER NOT " +
            "NULL, `periodNumber` INTEGER NOT NULL, `planNeeds` INTEGER, `planWants` " +
            "INTEGER, `planSavings` INTEGER, `depositedThisPeriod` INTEGER NOT NULL, " +
            "`difficulty` TEXT NOT NULL, `lastFinishedDate` TEXT, `usageDate` TEXT, " +
            "`usageMinutes` INTEGER NOT NULL, `timeLimitEnabled` INTEGER NOT NULL, " +
            "PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "INSERT INTO `profile_new` (`id`, `petName`, `speciesId`, `colorId`, " +
            "`accessoryId`, `isDemo`, `balance`, `savedCoins`, `goalId`, `goalPrice`, " +
            "`care`, `joy`, `growthPoints`, `periodNumber`, `planNeeds`, `planWants`, " +
            "`planSavings`, `depositedThisPeriod`, `difficulty`, `lastFinishedDate`, " +
            "`usageDate`, `usageMinutes`, `timeLimitEnabled`) SELECT `id`, `petName`, " +
            "`speciesId`, `colorId`, `accessoryId`, `isDemo`, `balance`, `savedCoins`, " +
            "`goalId`, `goalPrice`, `care`, `joy`, `growthPoints`, `periodNumber`, " +
            "`planNeeds`, `planWants`, `planSavings`, `depositedThisPeriod`, CASE WHEN " +
            "`grade` <= 2 THEN 'SIMPLE' ELSE 'HARDER' END, `lastFinishedDate`, " +
            "`usageDate`, `usageMinutes`, `timeLimitEnabled` FROM `profile`",
        )
        db.execSQL("DROP TABLE `profile`")
        db.execSQL("ALTER TABLE `profile_new` RENAME TO `profile`")
    }
}

/**
 * Версия 6 → 7: выключатель движений питомца в настройках отображения.
 * По умолчанию движения включены, как и до появления настройки.
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE `display_settings` ADD COLUMN `motionEnabled` INTEGER NOT NULL DEFAULT 1",
        )
    }
}

/**
 * Версия 7 → 8: счётчик новых заданий, оплаченных монетами за игровой день
 * (монеты платятся за первые два задания дня), и снятие отметки закрытого
 * дня — карманные на него уже начислены прежней версией.
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `profile` ADD COLUMN `paidTasksPeriod` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `profile` ADD COLUMN `paidTasksCount` INTEGER NOT NULL DEFAULT 0")
        // До версии 8 карманные на новый день начислялись сразу при закрытии
        // дня. С версии 8 они приходят при первом входе в новые сутки, если
        // отмечен закрытый день. Отметка снимается: иначе профиль, обновлённый
        // со старой версии, получил бы карманные за этот день второй раз.
        db.execSQL("UPDATE `profile` SET `lastFinishedDate` = NULL")
    }
}

/**
 * Версия 8 → 9: слова словарика, встреченные в игре, непрочитанные новые
 * слова и напоминания, отложенные кнопкой «Не сейчас». Пустые значения
 * означают «ещё ничего»: встреченные слова отметятся по состоянию игры при
 * первом запуске новой версии.
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `profile` ADD COLUMN `knownTerms` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `profile` ADD COLUMN `newTerms` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `profile` ADD COLUMN `dismissedReminders` TEXT NOT NULL DEFAULT ''")
    }
}

/** Версия 9 → 10: состояние сезона — банки, события дня, сюрпризы — одной строкой JSON. */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `profile` ADD COLUMN `extras` TEXT NOT NULL DEFAULT ''")
    }
}

/** Все переходы, известные приложению. Порядок не важен. */
val ALL_MIGRATIONS = arrayOf(
    MIGRATION_1_2,
    MIGRATION_2_3,
    MIGRATION_3_4,
    MIGRATION_4_5,
    MIGRATION_5_6,
    MIGRATION_6_7,
    MIGRATION_7_8,
    MIGRATION_8_9,
    MIGRATION_9_10,
)
