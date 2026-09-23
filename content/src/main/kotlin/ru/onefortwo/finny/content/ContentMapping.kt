package ru.onefortwo.finny.content

import ru.onefortwo.finny.economy.BudgetCategory
import ru.onefortwo.finny.economy.Coins
import ru.onefortwo.finny.economy.Goal
import ru.onefortwo.finny.economy.PetStatKind
import ru.onefortwo.finny.economy.ShopItem

/**
 * Перевод учебного контента в доменные типы модуля экономики.
 * Это единственное место связи между форматом хранения контента и правилами,
 * поэтому формат JSON можно менять, не затрагивая логику (ТЗ 3.4).
 */

fun ItemCategory.toDomain(): BudgetCategory = when (this) {
    ItemCategory.NEEDS -> BudgetCategory.NEEDS
    ItemCategory.WANTS -> BudgetCategory.WANTS
}

fun PetStat.toDomain(): PetStatKind = when (this) {
    PetStat.CARE -> PetStatKind.CARE
    PetStat.JOY -> PetStatKind.JOY
}

fun ShopItemContent.toDomain(): ShopItem = ShopItem(
    id = id,
    price = Coins(price),
    category = category.toDomain(),
    stat = stat.toDomain(),
    statDelta = statDelta,
)

fun GoalContent.toDomain(): Goal = Goal(id = id, price = Coins(price))
