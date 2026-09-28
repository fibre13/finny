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

/**
 * ТЕСТ: набор украшений. Питомец носит несколько сразу — бантик, шарфик,
 * шапочку; набор хранится в том же поле кодов через запятую («bow,hat»),
 * поэтому менять базу не нужно. «none» — без украшений.
 */
object Accessories {
    const val NONE = "none"

    /** Коды надетых украшений по порядку каталога. */
    fun list(key: String): List<String> =
        key.split(',').map { it.trim() }.filter { it.isNotEmpty() && it != NONE }.distinct()

    /** Ключ набора для хранения. */
    fun key(ids: Collection<String>): String =
        ids.filter { it.isNotEmpty() && it != NONE }.distinct().joinToString(",").ifEmpty { NONE }

    /** Нажали на украшение: надето — снять, нет — надеть; «без украшения» снимает всё. */
    fun toggle(key: String, id: String, order: List<String> = emptyList()): String {
        if (id == NONE) return NONE
        val now = list(key)
        val next = if (id in now) now - id else now + id
        return key(if (order.isEmpty()) next else next.sortedBy { order.indexOf(it) })
    }

    /** Надето ли украшение [id]; «без украшения» — когда не надето ничего. */
    fun isOn(key: String, id: String): Boolean = if (id == NONE) list(key).isEmpty() else id in list(key)

    /** Названия для подписи и TalkBack: «бантик и шапочка»; без украшений — «без украшения». */
    fun title(parts: PetPartsContent, key: String): String =
        list(key).mapNotNull { id -> parts.accessories.firstOrNull { it.id == id }?.title?.lowercase() }
            .let { if (it.isEmpty()) "без украшения" else it.joinToString(" и ") }
}
