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

    /** ТЕСТ 3: значок товара — пиксельный спрайт. */
    val icon: String? = null,

    /**
     * ТЕСТ 3: сюрприз за задания — товар появляется в лавке, когда решено
     * столько заданий (верно или нет). `null` — товар доступен сразу.
     */
    @SerialName("unlock_after")
    val unlockAfter: Int? = null,
)

/** Финансовая цель для накоплений (ТЗ 2.5.7). */
@Serializable
data class GoalContent(
    val id: String,
    val title: String,
    val price: Int,
    /** Короткое название для кнопки «Забрать …»: «аквариум». */
    @SerialName("claim_title")
    val claimTitle: String = title,
    /**
     * Строка праздника, когда цель накоплена; вместо `{name}` — имя
     * питомца в именительном падеже: «{name} будет смотреть на рыбок».
     */
    val celebration: String = "",
)

/** Строка праздника с именем питомца. */
fun GoalContent.celebrationFor(petName: String): String = celebration.replace("{name}", petName)

/** Размер мечты по цене — подпись на карточке выбора цели. */
fun GoalContent.dreamSize(): String = when {
    price <= 60 -> "Маленькая мечта"
    price <= 100 -> "Средняя мечта"
    else -> "Большая мечта"
}

/** Термин справочного раздела (ТЗ 2.5.11). */
@Serializable
data class GlossaryEntry(
    val term: String,
    val explanation: String,
    /** ТЕСТ 3: пиксельный значок термина. */
    val icon: String? = null,
    /** ТЕСТ 3: термин показывается только на уровне «Посложнее». */
    @SerialName("hard_only")
    val hardOnly: Boolean = false,
    /** ТЕСТ 3: термин ведёт на экран: `plan` — план, `fact` — план и факт. */
    val link: String? = null,
)

/** ТЕСТ 3: происхождение события сезона. */
@Serializable
enum class EventKind {
    /** Питомец сам просит. */
    @SerialName("internal")
    INTERNAL,

    /** Обстоятельства: погода, поломки. */
    @SerialName("external")
    EXTERNAL,

    /** Друзья и родные. */
    @SerialName("social")
    SOCIAL,

    /** Поиграть с питомцем. */
    @SerialName("interactive")
    INTERACTIVE,
}

/**
 * ТЕСТ 3: событие сезона. Справочник хранится в `events.json`: новое
 * событие добавляется без изменения кода (ТЗ 2.5.14). Имя питомца —
 * `{name}`, только в именительном падеже.
 */
@Serializable
data class EventContent(
    val id: String,
    val kind: EventKind,
    /** Затратное: решение о деньгах, после ответа — подсказка. */
    val cost: Boolean,
    /** Обязательное: голод или жажда, бывает в каждом дне. */
    val mandatory: Boolean = false,
    /** Группа обязательного события: `hunger` или `thirst`. */
    val group: String? = null,
    val title: String,
    /** Другие формулировки обязательного события, чтобы не повторяться. */
    val titles: List<String> = emptyList(),
    /** Что происходит на картинке — для программы чтения с экрана. */
    val scene: String = "",
    val icon: String? = null,
    val weather: String? = null,
    val yes: String,
    val no: String? = null,
    val price: Int = 0,
    val category: ItemCategory? = null,
    val stat: PetStat? = null,
    @SerialName("stat_delta")
    val statDelta: Int = 0,
    @SerialName("decline_stat")
    val declineStat: PetStat? = null,
    @SerialName("decline_delta")
    val declineDelta: Int = 0,
    @SerialName("hint_yes")
    val hintYes: String? = null,
    @SerialName("hint_no")
    val hintNo: String? = null,
    @SerialName("emotion_yes")
    val emotionYes: String? = null,
    @SerialName("emotion_no")
    val emotionNo: String? = null,
    /** Подарок монетами: «бабушка подарила». */
    val coins: Int = 0,
    @SerialName("unlocks_accessory")
    val unlocksAccessory: String? = null,
    /** Бывает, только если куплена обстановка. */
    @SerialName("requires_scenery")
    val requiresScenery: String? = null,
    /** Бывает, только если обстановка не куплена. */
    @SerialName("requires_no_scenery")
    val requiresNoScenery: String? = null,
    /** Бывает сразу после отказа в этом событии. */
    val follows: String? = null,
    /** Бывает, только если эта мечта уже получена. */
    @SerialName("requires_goal")
    val requiresGoal: String? = null,
) {
    /** Формулировки события: основная и запасные. */
    val allTitles: List<String> get() = (listOf(title) + titles).distinct()
}

/** Текст с именем питомца вместо `{name}`. */
fun String.withPetName(petName: String): String = replace("{name}", petName)

/**
 * Пояснение влияния покупки с именем питомца, которое выбрал ребёнок.
 * Имя стоит в именительном падеже: придуманные имена склоняются
 * по-разному, и «У Мурзик» читалось бы как ошибка.
 */
fun ShopItemContent.effectFor(petName: String): String = effect.replace("{name}", petName)
