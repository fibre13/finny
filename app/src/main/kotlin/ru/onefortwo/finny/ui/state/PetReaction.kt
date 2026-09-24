package ru.onefortwo.finny.ui.state

import ru.onefortwo.finny.economy.GrowthStage

/**
 * Реакция питомца на событие игры. Проигрывается на главном экране, когда
 * ребёнок на него возвращается: покупки, копилка и задания открываются на
 * своих экранах, а питомец виден на главном.
 *
 * Реакция несёт только оформление: смысл события сообщает текст.
 *
 * @property kind `eat`, `play`, `save`, `reward` или `grow` (`animation.json`).
 * @property id различает одинаковые реакции подряд: вторая покупка корма —
 * новая реакция, а не повтор уже проигранной.
 * @property stageBefore стадия до роста: реакция `grow` показывает смену.
 */
data class PetReaction(
    val kind: String,
    val id: Long,
    val stageBefore: GrowthStage? = null,
)

object PetReactions {
    const val EAT = "eat"
    const val PLAY = "play"
    const val SAVE = "save"
    const val REWARD = "reward"
    const val GROW = "grow"

    /**
     * Сколько реакций ждёт возвращения на главный экран. Больше трёх подряд
     * по две секунды — уже ожидание, а не отклик: старые отбрасываются.
     */
    const val MAX_QUEUED = 3
}
