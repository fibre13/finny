package ru.onefortwo.finny.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Модели учебного контента. Соответствуют структуре JSON-файлов в assets.
 * Суммы хранятся целыми числами монет; перевод в доменный тип
 * выполняется на границе модуля.
 */

/** Тип расхода: обязательный или необязательный (ТЗ 2.5.6). */
@Serializable
enum class ItemCategory {
    @SerialName("needs")
    NEEDS,

    @SerialName("wants")
    WANTS,
}

/** Показатель состояния питомца, на который влияет покупка. */
@Serializable
enum class PetStat {
    @SerialName("care")
    CARE,

    @SerialName("joy")
    JOY,
}

/** Позиция каталога покупок. */
@Serializable
data class ShopItemContent(
    val id: String,
    val title: String,
    val category: ItemCategory,
    val price: Int,
    val stat: PetStat,
    @SerialName("stat_delta")
    val statDelta: Int,
    /**
     * Короткое пояснение влияния, показывается до подтверждения покупки.
     * Вместо `{name}` подставляется имя питомца — см. [effectFor].
     */
    val effect: String,

    /**
     * Украшение, которое покупка открывает для гардероба питомца.
     * Переодеть питомца можно только в то, что куплено, поэтому гардероб
     * опирается на финансовые решения, а не выдаётся просто так.
     */
    @SerialName("unlocks_accessory")
    val unlocksAccessory: String? = null,

    /**
     * Обстановка, которая появляется на фоне главного экрана после покупки.
     *
     * Отличается от украшения тем, что надевать её не нужно: купленное
     * видно сразу и навсегда. Как и украшение, остаётся следствием
     * финансового решения, а не выдаётся просто так.
     */
    @SerialName("unlocks_scenery")
    val unlocksScenery: String? = null,
)

/** Финансовая цель для накоплений (ТЗ 2.5.7). */
@Serializable
data class GoalContent(
    val id: String,
    val title: String,
    val price: Int,
)

/** Термин справочного раздела (ТЗ 2.5.11). */
@Serializable
data class GlossaryEntry(
    val term: String,
    val explanation: String,
)

/**
 * Пояснение влияния покупки с именем питомца, которое выбрал ребёнок.
 * Имя стоит в именительном падеже: придуманные имена склоняются
 * по-разному, и «У Мурзик» читалось бы как ошибка.
 */
fun ShopItemContent.effectFor(petName: String): String = effect.replace("{name}", petName)
