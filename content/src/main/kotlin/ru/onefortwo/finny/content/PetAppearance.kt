package ru.onefortwo.finny.content

import kotlinx.serialization.Serializable

/**
 * Составные части внешности питомца (ТЗ 2.5.2, 2.6).
 *
 * Комбинация вида и окраса даёт визуально различимые варианты; аксессуар
 * добавляет дополнительное отличие. Изображения на текущем этапе заменены
 * цветными фигурами, поэтому окрас задаётся значением цвета.
 */

@Serializable
data class PetSpecies(
    val id: String,
    val title: String,
)

@Serializable
data class PetColor(
    val id: String,
    val title: String,
    /** Цвет в формате #RRGGBB. */
    val hex: String,
)

@Serializable
data class PetAccessory(
    val id: String,
    val title: String,
)

/** Набор частей внешности, из которых собирается питомец. */
@Serializable
data class PetPartsContent(
    val species: List<PetSpecies>,
    val colors: List<PetColor>,
    val accessories: List<PetAccessory>,
) {
    /**
     * Количество визуально различимых комбинаций вида и окраса.
     * ТЗ 2.6 требует не менее девяти.
     */
    val appearanceCombinations: Int get() = species.size * colors.size
}

/** Выбранная пользователем внешность питомца. */
data class PetAppearance(
    val speciesId: String,
    val colorId: String,
    val accessoryId: String,
)
