package ru.onefortwo.finny.ui.state

import kotlin.random.Random
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ru.onefortwo.finny.content.EventContent
import ru.onefortwo.finny.content.ItemCategory
import ru.onefortwo.finny.content.ShopItemContent
import ru.onefortwo.finny.economy.GrowthStage

/*
 * ТЕСТ 3 (ветка test/app-3): сезон по концепту «ФИНАЛ».
 *
 * Сезон — три игровых дня; игровой день по-прежнему равен календарным
 * суткам, в демонстрационном режиме дни идут подряд. В начале сезона
 * приходят всегда одни и те же 50 монет; ребёнок раскладывает их по трём
 * банкам — «Надо», «Хочу», «Копим на мечту» — целиком, остаток не остаётся.
 * План сезона можно поправить в любой момент. В каждом дне три события:
 * обязательное (голод или жажда), затратное и незатратное; пока событие
 * не решено, день не закончить. Монет за задания нет, кроме «Помоги своему
 * питомцу»: каждые три решённых задания открывают сюрприз в лавке.
 * Питомец растёт не по очкам, а за мечты и задания (см. [Growth]).
 */

/** Сколько игровых дней в сезоне. */
const val SEASON_DAYS = 3

/** Сколько событий в игровом дне. */
const val EVENTS_PER_DAY = 3

/** Через сколько календарных дней незавершённый сезон закрывается. */
const val SEASON_EXPIRES_DAYS = 6

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
    /** Сюрприз, о котором ещё не сказали ребёнку. */
    val surprise: String? = null,

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

    /** Пояснения к строкам «Нужное» и «Хочу», если «Хочу» доплачивал за нужное. */
    fun borrowNotes(x: SeasonExtras): Pair<String?, String?> {
        val borrowed = x.wantsToNeeds
        if (borrowed <= 0) return null to null
        val over = x.spentNeeds - x.plannedNeeds
        val needs = if (over > 0) "Потрачено больше плана на ${Explanations.coinsAccusative(over)} — " +
            "из банка «Хочу» взято ${Explanations.coins(borrowed)}." else null
        val unspent = (x.plannedWants - x.spentWants).coerceAtLeast(0)
        val wants = "Не потрачено на желаемое: ${Explanations.coins(unspent)}, из них " +
            "${Explanations.coins(borrowed)} ушли на нужное."
        return needs to wants
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
     * Откуда взять [price] монет. Нужное: свободные → «Надо» → «Хочу» →
     * копилка (только с подтверждением). Желаемое: свободные → «Хочу»;
     * из «Надо» и копилки на желаемое не берётся.
     */
    fun payment(price: Int, category: ItemCategory, balance: Int, saved: Int, x: SeasonExtras): Payment {
        var left = price
        val free = free(balance, x).coerceAtMost(balance)
        val fromFree = minOf(left, free).also { left -= it }
        return when (category) {
            ItemCategory.NEEDS -> {
                val fromNeeds = minOf(left, x.needsJar).also { left -= it }
                val fromWants = minOf(left, x.wantsJar).also { left -= it }
                val fromSavings = minOf(left, saved).also { left -= it }
                Payment(price, fromFree, fromNeeds, fromWants, fromSavings, impossible = left > 0)
            }

            ItemCategory.WANTS -> {
                val fromWants = minOf(left, x.wantsJar).also { left -= it }
                Payment(price, fromFree, 0, fromWants, 0, impossible = left > 0)
            }
        }
    }

    /**
     * Три события дня [period]: обязательное, затратное, незатратное.
     * Порядок меняется по дням: обязательное то первое, то последнее,
     * затратные подряд внутри дня не идут. Выбор повторяется при
     * тех же входных данных — одинаково в демо и в тестах.
     */
    fun pickDay(
        events: List<EventContent>,
        period: Int,
        used: Collection<String>,
        refused: String?,
        scenery: Set<String>,
        achievedGoals: Set<String>,
    ): List<String> {
        val season = seasonOf(period)
        val day = dayOf(period)
        val random = Random(season * 101 + day * 7)
        fun allowed(e: EventContent): Boolean =
            e.id !in used &&
                (e.requiresScenery == null || e.requiresScenery in scenery) &&
                (e.requiresNoScenery == null || e.requiresNoScenery !in scenery) &&
                (e.requiresGoal == null || e.requiresGoal in achievedGoals)

        // Обязательное: голод в первый и третий день, жажда во второй.
        val group = if (day == 2) "thirst" else "hunger"
        val mandatory = events.filter { it.mandatory && it.group == group }
            .let { list -> list.filter { it.id !in used }.ifEmpty { list } }
            .randomOrNull(random)

        // Затратное: сначала продолжение отказа («промок» после «зонта»).
        val followUp = events.firstOrNull { it.follows != null && it.follows == refused && it.id !in used }
        val costly = followUp ?: events
            .filter { it.cost && !it.mandatory && it.follows == null && allowed(it) }
            .randomOrNull(random)

        val calm = events.filter { !it.cost && allowed(it) }.randomOrNull(random)

        val ordered = if (random.nextBoolean()) listOf(mandatory, calm, costly) else listOf(costly, calm, mandatory)
        return ordered.mapNotNull { it?.id }
    }

    /** Формулировка события: у обязательных — своя в каждом сезоне и дне. */
    fun titleOf(event: EventContent, period: Int): String {
        val titles = event.allTitles
        return titles[(period - 1).mod(titles.size)]
    }

    /** Товары лавки, которые видит ребёнок: обычные и открытые сюрпризами. */
    fun visibleItems(items: List<ShopItemContent>, x: SeasonExtras): List<ShopItemContent> =
        items.filter { it.unlockAfter == null || it.id in x.unlocked }

    /** Сюрприз, который пора открыть: наименьший достигнутый и не выданный порог. */
    fun pendingSurprise(items: List<ShopItemContent>, x: SeasonExtras): ShopItemContent? =
        items.filter { val n = it.unlockAfter; n != null && n <= x.tasksSolved && n !in x.rewarded }
            .minByOrNull { it.unlockAfter!! }

    /** Сколько заданий до следующего сюрприза; `null` — сюрпризы кончились. */
    fun tasksToNextSurprise(items: List<ShopItemContent>, x: SeasonExtras): Int? =
        items.mapNotNull { it.unlockAfter }.filter { it > x.tasksSolved }.minOrNull()?.let { it - x.tasksSolved }
}

/**
 * Рост питомца в тесте 3: подростком — за две полученные мечты и девять
 * решённых заданий (три сюрприза), взрослым — за четыре мечты и восемнадцать
 * заданий. Мечты — понятная ребёнку история («накопил на самокат и аквариум —
 * питомец вырос»), задания — вклад в рост без монет.
 */
object Growth {
    const val TEEN_DREAMS = 2
    const val TEEN_TASKS = 9
    const val ADULT_DREAMS = 4
    const val ADULT_TASKS = 18

    fun stageFor(dreams: Int, tasks: Int): GrowthStage = when {
        dreams >= ADULT_DREAMS && tasks >= ADULT_TASKS -> GrowthStage.ADULT
        dreams >= TEEN_DREAMS && tasks >= TEEN_TASKS -> GrowthStage.TEEN
        else -> GrowthStage.BABY
    }

    /** Что нужно для следующей стадии: мечты и задания; `null` — выше расти некуда. */
    fun nextNeeds(stage: GrowthStage): Pair<Int, Int>? = when (stage) {
        GrowthStage.BABY -> TEEN_DREAMS to TEEN_TASKS
        GrowthStage.TEEN -> ADULT_DREAMS to ADULT_TASKS
        GrowthStage.ADULT -> null
    }
}
