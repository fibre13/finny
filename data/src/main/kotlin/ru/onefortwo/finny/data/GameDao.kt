package ru.onefortwo.finny.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Доступ к сохранённому состоянию игры. */
@Dao
interface GameDao {

    @Query("SELECT * FROM profile WHERE id = :id LIMIT 1")
    fun observeProfile(id: Long = ProfileEntity.DEFAULT_ID): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE id = :id LIMIT 1")
    suspend fun profile(id: Long = ProfileEntity.DEFAULT_ID): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: ProfileEntity)

    @Query("SELECT * FROM current_purchases ORDER BY id")
    suspend fun purchases(): List<PurchaseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchases(purchases: List<PurchaseEntity>)

    @Query("DELETE FROM current_purchases")
    suspend fun clearPurchases()

    @Query("SELECT * FROM period_history ORDER BY number")
    suspend fun history(): List<PeriodOutcomeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(outcomes: List<PeriodOutcomeEntity>)

    @Query("DELETE FROM period_history")
    suspend fun clearHistory()

    @Query("SELECT * FROM completed_tasks")
    suspend fun completedTasks(): List<CompletedTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletedTasks(tasks: List<CompletedTaskEntity>)

    @Query("DELETE FROM completed_tasks")
    suspend fun clearCompletedTasks()

    @Query("SELECT * FROM owned_accessories")
    suspend fun ownedAccessories(): List<OwnedAccessoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOwnedAccessories(accessories: List<OwnedAccessoryEntity>)

    @Query("DELETE FROM owned_accessories")
    suspend fun clearOwnedAccessories()

    @Query("SELECT * FROM owned_scenery")
    suspend fun ownedScenery(): List<OwnedSceneryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOwnedScenery(scenery: List<OwnedSceneryEntity>)

    @Query("DELETE FROM owned_scenery")
    suspend fun clearOwnedScenery()

    @Query("SELECT * FROM achieved_goals")
    suspend fun achievedGoals(): List<AchievedGoalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievedGoals(goals: List<AchievedGoalEntity>)

    @Query("DELETE FROM achieved_goals")
    suspend fun clearAchievedGoals()

    /** Полное удаление профиля; доступно взрослому без обращения к разработчику (ТЗ 3.5). */
    @Query("DELETE FROM profile")
    suspend fun clearProfile()

    @Query("SELECT * FROM display_settings WHERE id = :id")
    fun observeDisplaySettings(
        id: Long = DisplaySettingsEntity.DEFAULT_ID,
    ): Flow<DisplaySettingsEntity?>

    @Upsert
    suspend fun upsertDisplaySettings(settings: DisplaySettingsEntity)
}
