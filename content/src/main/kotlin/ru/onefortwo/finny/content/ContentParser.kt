package ru.onefortwo.finny.content

import kotlinx.serialization.json.Json

/**
 * Разбор учебного контента. Выделен отдельно от чтения assets, чтобы
 * формат контента проверялся модульными тестами без эмулятора (ТЗ 3.4).
 */
object ContentParser {

    private val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "type"
    }

    fun parseShop(text: String): List<ShopItemContent> = json.decodeFromString(text)

    fun parseGoals(text: String): List<GoalContent> = json.decodeFromString(text)

    fun parseTasks(text: String): List<TaskContent> = json.decodeFromString(text)

    fun parsePetParts(text: String): PetPartsContent = json.decodeFromString(text)

    fun parseGlossary(text: String): List<GlossaryEntry> = json.decodeFromString(text)

    fun parseEvents(text: String): List<EventContent> = json.decodeFromString(text)

    fun parseForbiddenWords(text: String): ForbiddenWords = json.decodeFromString(text)
}
