package ru.onefortwo.finny.ui.state

import ru.onefortwo.finny.economy.GrowthStage

/**
 * Речь питомца по стадии. Малыш радуется эмоционально: «Ура!»,
 * «Спасибо!»; подросток говорит, как подростки: «Круто!», «Супер!»,
 * «Огонь!». Относится ко всем репликам питомца в облачке: на дворе,
 * в событиях, в заданиях, в итогах сезона.
 */
object PetVoice {
    private val TEEN = listOf(
        "Ура!" to "Круто!",
        "Спасибо!" to "Супер, спасибо!",
        "Как здорово!" to "Огонь!",
        "Как весело!" to "Вот это круто!",
        "Как вкусно!" to "Вкуснотища!",
        "Юху!" to "Огонь!",
        "Привет!" to "Хай!",
    )

    /** Реплика подростка: восклицания малыша заменены. */
    fun teen(text: String): String = TEEN.fold(text) { acc, (baby, teen) -> acc.replace(baby, teen) }

    /** Реплика питомца стадии [stage]: малыш говорит как есть, подросток и взрослый — по-подростковому. */
    fun of(stage: GrowthStage, text: String): String = if (stage == GrowthStage.BABY) text else teen(text)
}
