package ru.onefortwo.finny.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Локальная база данных приложения. Профиль, баланс, покупки, накопления,
 * выбранная цель и учебный прогресс сохраняются между запусками (ТЗ 2.5.13).
 */
@Database(
    entities = [
        ProfileEntity::class,
        PurchaseEntity::class,
        PeriodOutcomeEntity::class,
        CompletedTaskEntity::class,
        OwnedAccessoryEntity::class,
        OwnedSceneryEntity::class,
        DisplaySettingsEntity::class,
        AchievedGoalEntity::class,
    ],
    version = 7,
    exportSchema = true,
)
abstract class FinnyDatabase : RoomDatabase() {

    abstract fun gameDao(): GameDao

    companion object {
        private const val NAME = "finny.db"

        @Volatile
        private var instance: FinnyDatabase? = null

        fun get(context: Context): FinnyDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }

        private fun build(context: Context): FinnyDatabase =
            Room.databaseBuilder(context, FinnyDatabase::class.java, NAME)
                // Переходы между версиями обязательны: без них обновление
                // приложения ломает базу у тех, у кого уже есть прогресс.
                .addMigrations(*ALL_MIGRATIONS)
                .build()
    }
}
