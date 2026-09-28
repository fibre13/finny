package ru.onefortwo.finny.data

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Хранение состояния игры на устройстве (ТЗ 2.5.13).
 *
 * Запись выполняется одной транзакцией, поэтому прерывание работы
 * приложения не оставляет профиль в половинчатом состоянии.
 */
class GameRepository(private val database: FinnyDatabase) {

    constructor(context: Context) : this(FinnyDatabase.get(context))

    private val dao = database.gameDao()

    /** Загружает сохранённое состояние либо null, если профиля ещё нет. */
    suspend fun load(): SavedGame? {
        val profile = dao.profile() ?: return null

        return GameRecords(
            profile = profile,
            purchases = dao.purchases(),
            history = dao.history(),
            completedTasks = dao.completedTasks(),
            ownedAccessories = dao.ownedAccessories(),
            ownedScenery = dao.ownedScenery(),
            achievedGoals = dao.achievedGoals(),
        ).toSavedGame()
    }

    /** Сохраняет состояние целиком, заменяя предыдущее. */
    suspend fun save(saved: SavedGame) {
        val records = saved.toRecords()

        database.withTransaction {
            dao.clearPurchases()
            dao.clearHistory()
            dao.clearCompletedTasks()
            dao.clearOwnedAccessories()
            dao.clearOwnedScenery()
            dao.clearAchievedGoals()

            dao.upsertProfile(records.profile)
            if (records.purchases.isNotEmpty()) dao.insertPurchases(records.purchases)
            if (records.history.isNotEmpty()) dao.insertHistory(records.history)
            if (records.completedTasks.isNotEmpty()) {
                dao.insertCompletedTasks(records.completedTasks)
            }
            if (records.ownedAccessories.isNotEmpty()) {
                dao.insertOwnedAccessories(records.ownedAccessories)
            }
            if (records.ownedScenery.isNotEmpty()) {
                dao.insertOwnedScenery(records.ownedScenery)
            }
            if (records.achievedGoals.isNotEmpty()) {
                dao.insertAchievedGoals(records.achievedGoals)
            }
        }
    }

    /**
     * Настройки отображения; `null` означает, что их не выбирали —
     * тогда действуют значения по умолчанию.
     */
    fun observeDisplaySettings(): Flow<SavedDisplaySettings?> =
        dao.observeDisplaySettings().map { entity ->
            entity?.let { SavedDisplaySettings(it.themeMode, it.highContrast, it.motionEnabled) }
        }

    /** Сохраняет настройки отображения. */
    suspend fun saveDisplaySettings(settings: SavedDisplaySettings) {
        dao.upsertDisplaySettings(
            DisplaySettingsEntity(
                themeMode = settings.themeMode,
                highContrast = settings.highContrast,
                motionEnabled = settings.motionEnabled,
            ),
        )
    }

    /**
     * Полностью удаляет профиль и связанные данные.
     * Доступно взрослому без обращения к разработчику (ТЗ 3.5).
     */
    suspend fun clear() {
        database.withTransaction {
            dao.clearPurchases()
            dao.clearHistory()
            dao.clearCompletedTasks()
            dao.clearOwnedAccessories()
            dao.clearOwnedScenery()
            dao.clearAchievedGoals()
            dao.clearProfile()
        }
    }
}
