package ru.onefortwo.finny.data

import ru.onefortwo.finny.economy.BudgetCategory
import ru.onefortwo.finny.economy.BudgetPlan
import ru.onefortwo.finny.economy.Coins
import ru.onefortwo.finny.economy.GameState
import ru.onefortwo.finny.economy.Goal
import ru.onefortwo.finny.economy.GrowthCondition
import ru.onefortwo.finny.economy.GrowthStage
import ru.onefortwo.finny.economy.IncomeEvent
import ru.onefortwo.finny.economy.IncomeSource
import ru.onefortwo.finny.economy.PeriodOutcome
import ru.onefortwo.finny.economy.PeriodState
import ru.onefortwo.finny.economy.PetState
import ru.onefortwo.finny.economy.PurchaseRecord
import ru.onefortwo.finny.economy.Difficulty
import ru.onefortwo.finny.economy.SavingsState
import ru.onefortwo.finny.economy.StatValue

/**
 * Сохраняемое состояние игры целиком: профиль игрока и состояние экономики.
 * Слой хранения не зависит от модуля интерфейса и модуля контента:
 * внешность питомца хранится идентификаторами частей (ТЗ 3.4).
 */
data class SavedGame(
    val petName: String,
    val speciesId: String,
    val colorId: String,
    val accessoryId: String,
    val isDemo: Boolean,
    val game: GameState,
    val completedTaskIds: Set<String>,

    /** Класс ребёнка: определяет сложность заданий (ТЗ 2.5.8). */
    val difficulty: Difficulty = Difficulty.SIMPLE,

    /** Дата последнего завершённого игрового дня, ГГГГ-ММ-ДД. */
    val lastFinishedDate: String? = null,

    /** Дата, к которой относится счётчик экранного времени. */
    val usageDate: String? = null,

    /** Сколько минут приложение было открыто в этот день. */
    val usageMinutes: Int = 0,

    /** Ограничение экранного времени включено (выключается взрослым). */
    val timeLimitEnabled: Boolean = true,

    /** Украшения, купленные в каталоге и доступные в гардеробе. */
    val ownedAccessories: Set<String> = emptySet(),
    val ownedScenery: Set<String> = emptySet(),

    /** Цели, на которые уже накоплено и которые ребёнок получил. */
    val achievedGoalIds: Set<String> = emptySet(),
)

/** Набор записей базы данных, соответствующий одному [SavedGame]. */
data class GameRecords(
    val profile: ProfileEntity,
    val purchases: List<PurchaseEntity>,
    val history: List<PeriodOutcomeEntity>,
    val completedTasks: List<CompletedTaskEntity>,
    val ownedAccessories: List<OwnedAccessoryEntity> = emptyList(),
    val ownedScenery: List<OwnedSceneryEntity> = emptyList(),
    val achievedGoals: List<AchievedGoalEntity> = emptyList(),
)

// --- Преобразование в записи базы ----------------------------------------

/** Раскладывает состояние игры на записи базы данных. */
fun SavedGame.toRecords(): GameRecords {
    val plan = game.period.plan

    val profile = ProfileEntity(
        petName = petName,
        speciesId = speciesId,
        colorId = colorId,
        accessoryId = accessoryId,
        isDemo = isDemo,
        balance = game.balance.amount,
        savedCoins = game.savings.saved.amount,
        goalId = game.savings.goal?.id,
        goalPrice = game.savings.goal?.price?.amount,
        care = game.pet.care.value,
        joy = game.pet.joy.value,
        growthPoints = game.growthPoints,
        periodNumber = game.period.number,
        planNeeds = plan?.needs?.amount,
        planWants = plan?.wants?.amount,
        planSavings = plan?.savings?.amount,
        depositedThisPeriod = game.period.depositedToSavings.amount,
        difficulty = difficulty.name,
        lastFinishedDate = lastFinishedDate,
        usageDate = usageDate,
        usageMinutes = usageMinutes,
        timeLimitEnabled = timeLimitEnabled,
    )

    val purchases = game.period.purchases.map { record ->
        PurchaseEntity(
            itemId = record.itemId,
            price = record.price.amount,
            category = record.category.name,
        )
    }

    val history = game.history.map { outcome ->
        PeriodOutcomeEntity(
            number = outcome.number,
            planNeeds = outcome.plan.needs.amount,
            planWants = outcome.plan.wants.amount,
            planSavings = outcome.plan.savings.amount,
            spentNeeds = outcome.spentNeeds.amount,
            spentWants = outcome.spentWants.amount,
            deposited = outcome.deposited.amount,
            needsCovered = GrowthCondition.NEEDS_COVERED in outcome.metConditions,
            withinPlan = GrowthCondition.WITHIN_PLAN in outcome.metConditions,
            savedSomething = GrowthCondition.SAVED_SOMETHING in outcome.metConditions,
            careBefore = outcome.petBefore.care.value,
            joyBefore = outcome.petBefore.joy.value,
            careAfter = outcome.petAfter.care.value,
            joyAfter = outcome.petAfter.joy.value,
            pointsBefore = outcome.pointsBefore,
            pointsAfter = outcome.pointsAfter,
            incomeSource = outcome.nextPeriodIncome?.source?.name,
            incomeAmount = outcome.nextPeriodIncome?.amount?.amount,
            incomeBalanceAfter = outcome.nextPeriodIncome?.balanceAfter?.amount,
        )
    }

    val tasks = completedTaskIds.map { CompletedTaskEntity(it) }
    val accessories = ownedAccessories.map { OwnedAccessoryEntity(it) }
    val scenery = ownedScenery.map { OwnedSceneryEntity(it) }
    val achieved = achievedGoalIds.map { AchievedGoalEntity(it) }

    return GameRecords(profile, purchases, history, tasks, accessories, scenery, achieved)
}

// --- Восстановление из записей базы --------------------------------------

/** Собирает состояние игры из записей базы данных. */
fun GameRecords.toSavedGame(): SavedGame {
    val outcomes = history.sortedBy { it.number }.map { it.toOutcome() }

    val plan = if (profile.planNeeds != null &&
        profile.planWants != null &&
        profile.planSavings != null
    ) {
        BudgetPlan(
            needs = Coins(profile.planNeeds),
            wants = Coins(profile.planWants),
            savings = Coins(profile.planSavings),
        )
    } else {
        null
    }

    val goal = if (profile.goalId != null && profile.goalPrice != null) {
        Goal(id = profile.goalId, price = Coins(profile.goalPrice))
    } else {
        null
    }

    val game = GameState(
        balance = Coins(profile.balance),
        savings = SavingsState(
            saved = Coins(profile.savedCoins),
            goal = goal,
            // Пополнения по периодам совпадают с суммами, отложенными
            // в завершённых периодах, поэтому отдельной таблицы не нужно.
            depositsByPeriod = outcomes.map { it.deposited },
        ),
        pet = PetState(
            care = StatValue.of(profile.care),
            joy = StatValue.of(profile.joy),
        ),
        growthPoints = profile.growthPoints,
        period = PeriodState(
            number = profile.periodNumber,
            plan = plan,
            purchases = purchases.map { entity ->
                PurchaseRecord(
                    itemId = entity.itemId,
                    price = Coins(entity.price),
                    category = BudgetCategory.valueOf(entity.category),
                )
            },
            depositedToSavings = Coins(profile.depositedThisPeriod),
        ),
        history = outcomes,
    )

    return SavedGame(
        petName = profile.petName,
        speciesId = profile.speciesId,
        colorId = profile.colorId,
        accessoryId = profile.accessoryId,
        isDemo = profile.isDemo,
        game = game,
        completedTaskIds = completedTasks.map { it.taskId }.toSet(),
        difficulty = Difficulty.ofName(profile.difficulty),
        lastFinishedDate = profile.lastFinishedDate,
        usageDate = profile.usageDate,
        usageMinutes = profile.usageMinutes,
        timeLimitEnabled = profile.timeLimitEnabled,
        ownedAccessories = ownedAccessories.map { it.accessoryId }.toSet(),
        ownedScenery = ownedScenery.map { it.sceneryId }.toSet(),
        achievedGoalIds = achievedGoals.map { it.goalId }.toSet(),
    )
}

private fun PeriodOutcomeEntity.toOutcome(): PeriodOutcome {
    val conditions = buildSet {
        if (needsCovered) add(GrowthCondition.NEEDS_COVERED)
        if (withinPlan) add(GrowthCondition.WITHIN_PLAN)
        if (savedSomething) add(GrowthCondition.SAVED_SOMETHING)
    }

    val income = if (incomeSource != null && incomeAmount != null && incomeBalanceAfter != null) {
        IncomeEvent(
            source = IncomeSource.valueOf(incomeSource),
            amount = Coins(incomeAmount),
            balanceAfter = Coins(incomeBalanceAfter),
        )
    } else {
        null
    }

    return PeriodOutcome(
        number = number,
        plan = BudgetPlan(Coins(planNeeds), Coins(planWants), Coins(planSavings)),
        spentNeeds = Coins(spentNeeds),
        spentWants = Coins(spentWants),
        deposited = Coins(deposited),
        metConditions = conditions,
        petBefore = PetState(StatValue.of(careBefore), StatValue.of(joyBefore)),
        petAfter = PetState(StatValue.of(careAfter), StatValue.of(joyAfter)),
        pointsBefore = pointsBefore,
        pointsAfter = pointsAfter,
        // Стадия однозначно определяется накопленными очками.
        stageBefore = GrowthStage.forPoints(pointsBefore),
        stageAfter = GrowthStage.forPoints(pointsAfter),
        nextPeriodIncome = income,
    )
}

/**
 * Сохранённые настройки отображения.
 *
 * Отдельно от [SavedGame]: их выбирают до создания профиля, и сброс
 * профиля их не затрагивает.
 */
data class SavedDisplaySettings(
    /** Тема: SYSTEM, LIGHT или DARK. */
    val themeMode: String,
    val highContrast: Boolean,
)
