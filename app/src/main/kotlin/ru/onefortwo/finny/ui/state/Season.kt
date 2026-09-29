package ru.onefortwo.finny.ui.state

import kotlin.random.Random
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ru.onefortwo.finny.content.EventContent
import ru.onefortwo.finny.content.EventKind
import ru.onefortwo.finny.content.ItemCategory
import ru.onefortwo.finny.content.ShopItemContent
import ru.onefortwo.finny.economy.GrowthStage

/*
 * Сезон — основной цикл игры (версия 0.8.0).
 *
 * Сезон — три игровых дня; игровой день равен календарным
 * суткам, в демонстрационном режиме дни идут подряд. В начале сезона
 * приходят всегда одни и те же 50 монет; ребёнок раскладывает их по трём
 * банкам — «Нужное», «Хочу», «Копим на мечту» — целиком, остаток не остаётся.
 * План сезона можно поправить в любой момент. В каждом дне три события:
 * обязательное (голод или жажда), затратное и незатратное — два затратных
 * подряд не идут; пока событие не решено, день не закончить. Монет за задания нет, кроме «Помоги своему
 * питомцу»: каждые три решённых задания открывают сюрприз в лавке.
 * Питомец растёт не по очкам, а за мечты и задания (см. [Growth]).
 */

/** Сколько игровых дней в сезоне. */
const val SEASON_DAYS = 3

/** Сколько событий в игровом дне. */
const val EVENTS_PER_DAY = 3

/** Через сколько календарных дней незавершённый сезон закрывается. */
const val SEASON_EXPIRES_DAYS = 6

/**
 * Через сколько игровых дней после появления товара в лавке о нём может
 * попросить событие: не в тот же день и не на следующий, чтобы связь
 * «открыл за задания — понадобилось» не была прямолинейной.
 */
const val ITEM_EVENT_DELAY = 2

/** Состояние сезона, хранится одной строкой JSON в профиле. */
@Serializable
data class SeasonExtras(
    /** Сезон, к которому относятся банки и факт ниже. */
    val season: Int = 1,
    /** Календарная дата начала сезона, ГГГГ-ММ-ДД. */
    val seasonStart: String? = null,

    /** План сезона составлен: монеты разложены по банкам. */
    val planned: Boolean = false,
    /** Сколько монет осталось в банках «Надо» и «Хочу». */
    val needsJar: Int = 0,
    val wantsJar: Int = 0,

    /** План сезона — с учётом поправок — и факт. */
    val plannedNeeds: Int = 0,
    val plannedWants: Int = 0,
    val plannedSavings: Int = 0,
    val spentNeeds: Int = 0,
    val spentWants: Int = 0,
    val deposited: Int = 0,
    /** Сколько монет из банка «Хочу» ушло на нужное, когда в «Нужном» не хватило. */
    val wantsToNeeds: Int = 0,
    /** Сколько монет взято из копилки на нужное, когда в банках не хватило. */
    val savingsToNeeds: Int = 0,

    /** События дня [eventsDay]; решено первых [answered]. */
    val eventsDay: Int = 0,
    val dayEvents: List<String> = emptyList(),
    val answered: Int = 0,
    /** События, уже бывшие в этом сезоне: не повторяются. */
    val usedEvents: List<String> = emptyList(),
    /** Событие, от которого ребёнок отказался последним: у него бывает продолжение. */
    val refused: String? = null,

    /** «Хочешь скорректировать план?» уже спрашивали в этом сезоне. */
    val correctionAsked: Boolean = false,
    /** День, в который уже играли с питомцем: радость растёт раз в день. */
    val playedDay: Int = 0,

    /** Сколько заданий решено всего, верно или нет, вместе с повторами. */
    val tasksSolved: Int = 0,
    /** Сколько заданий решено сегодня — для напоминания двора. */
    val tasksDay: Int = 0,
    val tasksToday: Int = 0,
    /** Пороги сюрпризов, за которые товар уже открыт. */
    val rewarded: List<Int> = emptyList(),
    /** Открытые сюрпризами товары. */
    val unlocked: List<String> = emptyList(),
    /** Игровой день, в который товар появился в лавке. */
    val unlockedAt: Map<String, Int> = emptyMap(),
    /** Сюрприз, о котором ещё не сказали ребёнку. */
    val surprise: String? = null,
    /** Товары, открытые вместе с сюрпризом: еда, билеты, аптечка. */
    val surpriseAlso: List<String> = emptyList(),
    /** Купленные товары — в лавке и в событиях: мячик, палатка, игрушка. */
    val owned: List<String> = emptyList(),

    /** Стадия, праздник которой уже показан. */
    val growthShown: GrowthStage = GrowthStage.BABY,

    /** Сезон закончен (третий день закрыт), итоги ещё не закрыты. */
    val seasonDone: Boolean = false,
    /** Прошлый сезон закрылся сам: ребёнок не приходил шесть дней. */
    val missed: Boolean = false,
) {
    fun encode(): String = JSON.encodeToString(serializer(), this)

    companion object {
        private val JSON = Json { ignoreUnknownKeys = true }

        fun decode(text: String): SeasonExtras =
            if (text.isBlank()) SeasonExtras() else runCatching { JSON.decodeFromString(serializer(), text) }.getOrDefault(SeasonExtras())
    }
}

/** Откуда берутся монеты на покупку: банки по порядку и копилка. */
data class Payment(
    val price: Int,
    val fromFree: Int = 0,
    val fromNeeds: Int = 0,
    val fromWants: Int = 0,
    val fromSavings: Int = 0,
    /** Монет не хватает даже с копилкой. */
    val impossible: Boolean = false,
) {
    val needsSavings: Boolean get() = fromSavings > 0 && !impossible
}

object Season {

    /** Пояснения к строкам «Нужное», «Хочу» и «Копим на мечту», если на нужное брали из других банков. */
    data class BorrowNotes(val needs: String?, val wants: String?, val savings: String?)

    fun borrowNotes(x: SeasonExtras): BorrowNotes {
        val fromWants = x.wantsToNeeds
        val fromSavings = x.savingsToNeeds
        if (fromWants <= 0 && fromSavings <= 0) return BorrowNotes(null, null, null)
        val over = x.spentNeeds - x.plannedNeeds
        // Перерасход покрывают банк «Хочу», копилка и свободные монеты (подарок, награда
        // за помощь питомцу): называются все источники, сумма равна перерасходу.
        val fromFree = (over - fromWants - fromSavings).coerceAtLeast(0)
        val sources = listOfNotNull(
            if (fromWants > 0) "взято из «Хочу» ${Explanations.coins(fromWants)}" else null,
            if (fromSavings > 0) "из копилки — ${Explanations.coins(fromSavings)}" else null,
            if (fromFree > 0) "из свободных монет — ${Explanations.coins(fromFree)}" else null,
        ).joinToString(", ")
        val needs = if (over > 0) "Потрачено больше плана на ${Explanations.coinsAccusative(over)}: $sources." else null
        val wants = if (fromWants > 0) "Из «Хочу» на нужное ушло ${Explanations.coins(fromWants)}" else null
        val savings = if (fromSavings > 0) "Из копилки на нужное взято ${Explanations.coins(fromSavings)}" else null
        return BorrowNotes(needs, wants, savings)
    }

    /** Номер сезона игрового дня [period]. */
    fun seasonOf(period: Int): Int = (period - 1) / SEASON_DAYS + 1

    /** День сезона: 1, 2 или 3. */
    fun dayOf(period: Int): Int = (period - 1) % SEASON_DAYS + 1

    /** Первый игровой день сезона [season]. */
    fun firstPeriodOf(season: Int): Int = (season - 1) * SEASON_DAYS + 1

    /** Свободные монеты: не разложены по банкам (подарок, снятие из копилки). */
    fun free(balance: Int, x: SeasonExtras): Int = (balance - x.needsJar - x.wantsJar).coerceAtLeast(0)

    /**
     * Откуда взять [price] монет. «Нужное» и «Хочу» — то, что запланировано
     * на сезон, — берутся суммарно: сначала свободные, потом «свой» банк
     * покупки (для нужного — «Нужное», для желаемого — «Хочу»), потом другой.
     * Копилка — в последнюю очередь и только с подтверждением ребёнка.
     */
    fun payment(price: Int, category: ItemCategory, balance: Int, saved: Int, x: SeasonExtras): Payment {
        var left = price
        val free = free(balance, x).coerceAtMost(balance)
        val fromFree = minOf(left, free).also { left -= it }
        val needsFirst = category == ItemCategory.NEEDS
        var fromNeeds = 0
        var fromWants = 0
        if (needsFirst) {
            fromNeeds = minOf(left, x.needsJar).also { left -= it }
            fromWants = minOf(left, x.wantsJar).also { left -= it }
        } else {
            fromWants = minOf(left, x.wantsJar).also { left -= it }
            fromNeeds = minOf(left, x.needsJar).also { left -= it }
        }
        val fromSavings = minOf(left, saved).also { left -= it }
        return Payment(price, fromFree, fromNeeds, fromWants, fromSavings, impossible = left > 0)
    }

    /**
     * События дня [period]. В сезоне девять событий, по три в день, и два
     * затратных не идут подряд ни внутри дня, ни на стыке дней:
     *
     * - день 1: затратное — эмоциональное — затратное (голод и событие с покупкой);
     * - день 2: эмоциональное — жажда — игра с питомцем;
     * - день 3: затратное — игра — затратное (голод и событие по времени года).
     *
     * Голод стоит то первым, то последним. Событие с товаром бывает, только
     * когда товар уже есть в лавке ([itemReady]); просьбы о еде — по виду
     * питомца. Выбор повторяется при тех же входных данных — одинаково в
     * демо и в тестах. Продолжение отказа («промок») вставляется сразу после
     * отказа — см. [followUp].
     */
    @Suppress("UNUSED_PARAMETER")
    fun pickDay(
        events: List<EventContent>,
        period: Int,
        used: Collection<String>,
        refused: String?,
        scenery: Set<String>,
        achievedGoals: Set<String>,
        species: String? = null,
        owned: Set<String> = emptySet(),
        items: List<ShopItemContent> = emptyList(),
        x: SeasonExtras = SeasonExtras(),
        month: Int = 9,
    ): List<String> {
        val season = seasonOf(period)
        val day = dayOf(period)
        val random = Random(season * 101 + day * 7)
        fun fits(e: EventContent): Boolean =
            e.fits(species) &&
                e.follows == null &&
                (e.requiresScenery == null || e.requiresScenery in scenery) &&
                (e.requiresNoScenery == null || e.requiresNoScenery !in scenery) &&
                (e.requiresGoal == null || e.requiresGoal in achievedGoals) &&
                (e.requiresOwned == null || e.requiresOwned in owned) &&
                (e.requiresNotOwned == null || e.requiresNotOwned !in owned) &&
                (e.item == null || items.isEmpty() || itemReady(e.item!!, items, x, species, period))

        // Из подходящих — ещё не бывших в сезоне; если таких нет, повтор лучше пустого места.
        fun pick(pool: List<EventContent>, taken: List<EventContent?> = emptyList()): EventContent? {
            val ok = pool.filter { fits(it) && it !in taken }
            return (ok.filter { it.id !in used }.ifEmpty { ok }).randomOrNull(random)
        }

        val hunger = pick(events.filter { it.mandatory && it.group == "hunger" })
        val thirst = pick(events.filter { it.mandatory && it.group == "thirst" })
        val costly = events.filter { it.cost && !it.mandatory }
        val calm = events.filter { !it.cost && it.kind != EventKind.INTERACTIVE }
        val play = events.filter { !it.cost && it.kind == EventKind.INTERACTIVE }

        return when (day) {
            1 -> {
                val spend = pick(costly.filter { it.weather == null }) ?: pick(costly)
                val middle = pick(calm) ?: pick(play)
                if (random.nextBoolean()) listOf(hunger, middle, spend) else listOf(spend, middle, hunger)
            }

            2 -> {
                val first = pick(calm) ?: pick(play)
                val last = pick(play, listOf(first)) ?: pick(calm, listOf(first))
                listOf(first, thirst, last)
            }

            else -> {
                val weather = seasonWeather(month)
                val spend = pick(costly.filter { it.weather in weather }) ?: pick(costly)
                val middle = pick(play) ?: pick(calm)
                if (random.nextBoolean()) listOf(spend, middle, hunger) else listOf(hunger, middle, spend)
            }
        }.mapNotNull { it?.id }
    }

    /** Погода времени года для события третьего дня: зимой холод, весной и осенью дождь, летом жара. */
    fun seasonWeather(month: Int): Set<String> = when (month) {
        12, 1, 2 -> setOf("cold")
        in 3..5 -> setOf("rain")
        in 6..8 -> setOf("heat")
        else -> setOf("rain", "cold")
    }

    /** Продолжение отказа от события [refusedId] — оно идёт сразу следом. */
    fun followUp(events: List<EventContent>, refusedId: String, species: String?): EventContent? =
        events.firstOrNull { it.follows == refusedId && it.fits(species) }

    /**
     * Товар [itemId] уже можно просить в событии: он есть в лавке для этого
     * питомца с начала игры или открыт за задания не позже чем за
     * [ITEM_EVENT_DELAY] игровых дня до [period].
     */
    fun itemReady(itemId: String, items: List<ShopItemContent>, x: SeasonExtras, species: String?, period: Int): Boolean {
        val item = items.firstOrNull { it.id == itemId } ?: return false
        if (!item.fits(species)) return false
        if (item.unlockAfter == null) return true
        if (itemId !in x.unlocked) return false
        return period >= (x.unlockedAt[itemId] ?: 0) + ITEM_EVENT_DELAY
    }

    /** Формулировка события: у обязательных — своя в каждом сезоне и дне. */
    fun titleOf(event: EventContent, period: Int): String {
        val titles = event.allTitles
        return titles[(period - 1).mod(titles.size)]
    }

    /** Товары лавки, которые видит ребёнок: для его питомца, обычные и открытые сюрпризами. */
    fun visibleItems(items: List<ShopItemContent>, x: SeasonExtras, species: String? = null): List<ShopItemContent> =
        items.filter { it.fits(species) && (it.unlockAfter == null || it.id in x.unlocked) }

    /**
     * Сюрприз, который пора открыть: товары наименьшего достигнутого порога,
     * которых ещё нет в лавке. Первым идёт главный сюрприз — одежда, за ним
     * то, что открывается вместе с ним: еда по виду питомца, билеты, аптечка.
     */
    fun pendingSurprise(items: List<ShopItemContent>, x: SeasonExtras, species: String? = null): List<ShopItemContent> {
        val due = items.filter { val n = it.unlockAfter; n != null && n <= x.tasksSolved && it.id !in x.unlocked && it.fits(species) }
        val first = due.minOfOrNull { it.unlockAfter!! } ?: return emptyList()
        return due.filter { it.unlockAfter == first }
    }

    /** Сколько заданий до следующего сюрприза; `null` — сюрпризы кончились. */
    fun tasksToNextSurprise(items: List<ShopItemContent>, x: SeasonExtras, species: String? = null): Int? =
        items.filter { it.fits(species) }.mapNotNull { it.unlockAfter }.filter { it > x.tasksSolved }.minOrNull()?.let { it - x.tasksSolved }
}

/**
 * Рост питомца: подростком — за две полученные мечты и девять решённых
 * заданий (три сюрприза), взрослым — за все три мечты и восемнадцать
 * заданий. Мечты — понятная ребёнку история («накопил на самокат и
 * аквариум — питомец вырос»), задания — вклад в рост без монет.
 */
object Growth {
    const val TEEN_DREAMS = 2
    const val TEEN_TASKS = 9
    const val ADULT_DREAMS = 3
    const val ADULT_TASKS = 18

    fun stageFor(dreams: Int, tasks: Int): GrowthStage = when {
        dreams >= ADULT_DREAMS && tasks >= ADULT_TASKS -> GrowthStage.ADULT
        dreams >= TEEN_DREAMS && tasks >= TEEN_TASKS -> GrowthStage.TEEN
        else -> GrowthStage.BABY
    }

    /**
     * Строка при получении мечты: сколько мечт ещё до роста. [achievedBefore] —
     * сколько мечт было до этой; `null`, если питомец уже взрослый.
     */
    fun claimLine(stage: GrowthStage, achievedBefore: Int, tasks: Int, petName: String, goalTitle: String): String? {
        val (needDreams, needTasks) = nextNeeds(stage) ?: return null
        val dreamsLeft = needDreams - (achievedBefore + 1)
        val tasksLeft = needTasks - tasks
        return when {
            dreamsLeft > 0 -> "Ты накопил на «$goalTitle»! Осталось ещё ${dreamsWord(dreamsLeft)}, чтобы $petName стал взрослее!"
            tasksLeft > 0 -> "Ты накопил на «$goalTitle»! Реши ещё ${Explanations.tasks(tasksLeft)} — и $petName вырастет!"
            else -> "Ты накопил на «$goalTitle»! Сейчас $petName вырастет!"
        }
    }

    /** Совет при выборе цели: эта мечта — та, после которой питомец вырастет. */
    fun goalTip(stage: GrowthStage, achieved: Int, petName: String): String? {
        val (needDreams, _) = nextNeeds(stage) ?: return null
        if (achieved != needDreams - 1 || achieved == 0) return null
        val nth = if (needDreams == 2) "вторая" else "третья"
        return "Совет: это твоя $nth мечта! Купи её — и $petName вырастет."
    }

    private fun dreamsWord(n: Int): String = when {
        n % 10 == 1 && n % 100 != 11 -> "$n мечта"
        n % 10 in 2..4 && n % 100 !in 12..14 -> "$n мечты"
        else -> "$n мечт"
    }

    /** Что нужно для следующей стадии: мечты и задания; `null` — выше расти некуда. */
    fun nextNeeds(stage: GrowthStage): Pair<Int, Int>? = when (stage) {
        GrowthStage.BABY -> TEEN_DREAMS to TEEN_TASKS
        GrowthStage.TEEN -> ADULT_DREAMS to ADULT_TASKS
        GrowthStage.ADULT -> null
    }
}
