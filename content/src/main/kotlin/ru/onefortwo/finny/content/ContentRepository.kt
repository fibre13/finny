package ru.onefortwo.finny.content

import android.content.Context
import ru.onefortwo.finny.economy.Difficulty

/** Источник файлов контента. Позволяет читать контент как из assets, так и из файлов в тестах. */
fun interface AssetSource {
    fun read(fileName: String): String
}

/**
 * Загрузчик учебного контента. Единственное место, где приложение читает
 * файлы контента: добавление новой позиции каталога, цели или задания
 * не требует изменений в коде экранов (ТЗ 2.5.14).
 *
 * Зависимость от Android вынесена в [AssetSource], поэтому загрузку
 * и связанную с ней логику можно проверять на JVM без эмулятора (ТЗ 3.4).
 */
class ContentRepository(private val source: AssetSource) {

    constructor(context: Context) : this(
        AssetSource { fileName ->
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        },
    )

    private val shopCache: List<ShopItemContent> by lazy {
        ContentParser.parseShop(source.read(FILE_SHOP))
    }

    private val goalsCache: List<GoalContent> by lazy {
        ContentParser.parseGoals(source.read(FILE_GOALS))
    }

    private val tasksCache: List<TaskContent> by lazy {
        ContentParser.parseTasks(source.read(FILE_TASKS))
    }

    private val petPartsCache: PetPartsContent by lazy {
        ContentParser.parsePetParts(source.read(FILE_PET_PARTS))
    }

    private val glossaryCache: List<GlossaryEntry> by lazy {
        ContentParser.parseGlossary(source.read(FILE_GLOSSARY))
    }

    /** Каталог покупок: обязательные и необязательные позиции (ТЗ 2.5.6). */
    fun shopItems(): List<ShopItemContent> = shopCache

    /** Позиции каталога заданной категории. */
    fun shopItems(category: ItemCategory): List<ShopItemContent> =
        shopCache.filter { it.category == category }

    /** Доступные финансовые цели (ТЗ 2.5.7). */
    fun goals(): List<GoalContent> = goalsCache

    /**
     * Финансовые задания. В демонстрационном режиме доступны сразу все,
     * без привязки к реальному времени (ТЗ 2.5.8).
     */
    fun tasks(): List<TaskContent> = tasksCache

    /** Задания заданной темы. */
    fun tasks(topic: TaskTopic): List<TaskContent> = tasksCache.filter { it.topic == topic }

    /**
     * Задания, подходящие ребёнку из указанного класса (ТЗ 2.5.8:
     * сложность вычислений соответствует возрасту).
     */
    fun tasks(difficulty: Difficulty): List<TaskContent> =
        tasksCache.filter { it.level.suits(difficulty) }

    /** Задание по идентификатору либо null, если такого нет. */
    fun task(id: String): TaskContent? = tasksCache.firstOrNull { it.id == id }

    /** Части внешности питомца (ТЗ 2.5.2). */
    fun petParts(): PetPartsContent = petPartsCache

    /** Справочный раздел с объяснением основных терминов (ТЗ 2.5.11). */
    fun glossary(): List<GlossaryEntry> = glossaryCache

    private companion object {
        const val FILE_SHOP = "shop.json"
        const val FILE_GOALS = "goals.json"
        const val FILE_TASKS = "tasks.json"
        const val FILE_PET_PARTS = "pet_parts.json"
        const val FILE_GLOSSARY = "glossary.json"
    }
}
