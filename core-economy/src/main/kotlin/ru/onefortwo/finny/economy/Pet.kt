package ru.onefortwo.finny.economy

/**
 * Обратимый показатель состояния питомца по шкале 0–100.
 * Значение всегда ограничивается диапазоном, поэтому ошибка пользователя
 * не может увести показатель в недопустимую область (ТЗ 2.2, «безопасная ошибка»).
 */
@JvmInline
value class StatValue private constructor(val value: Int) : Comparable<StatValue> {

    operator fun plus(delta: Int): StatValue = of(value + delta)

    operator fun minus(delta: Int): StatValue = of(value - delta)

    override fun compareTo(other: StatValue): Int = value.compareTo(other.value)

    /**
     * Уровень показателя. Передаётся в интерфейс отдельно от числа, чтобы
     * состояние читалось не только по цвету (ТЗ 3.6): уровню соответствуют
     * текстовая метка и пиктограмма.
     */
    val level: StatLevel
        get() = when {
            value <= LOW_MAX -> StatLevel.LOW
            value <= MEDIUM_MAX -> StatLevel.MEDIUM
            else -> StatLevel.HIGH
        }

    companion object {
        const val MIN = 0
        const val MAX = 100

        /** Верхняя граница низкого уровня показателя. */
        const val LOW_MAX = 33

        /** Верхняя граница среднего уровня показателя. */
        const val MEDIUM_MAX = 66

        fun of(raw: Int): StatValue = StatValue(raw.coerceIn(MIN, MAX))
    }
}

/** Уровень показателя состояния; словесная формулировка задаётся в интерфейсе. */
enum class StatLevel {
    LOW,
    MEDIUM,
    HIGH,
}

/** Показатель состояния, на который влияет покупка или завершение периода. */
enum class PetStatKind {
    /** Забота: сытость и уход, зависит от обязательных расходов. */
    CARE,

    /** Радость: настроение, зависит от необязательных расходов. */
    JOY,
}

/** Показатели состояния питомца, меняющиеся после отдельных решений (ТЗ 2.5.10). */
data class PetState(
    val care: StatValue,
    val joy: StatValue,
) {
    operator fun get(kind: PetStatKind): StatValue = when (kind) {
        PetStatKind.CARE -> care
        PetStatKind.JOY -> joy
    }

    /**
     * Показатель упёрся в предел шкалы: покупка, которая его повышает,
     * ничего не изменит, и об этом стоит предупредить до списания монет.
     */
    fun isAtMax(kind: PetStatKind): Boolean = this[kind].value == StatValue.MAX

    /** Изменяет один показатель на [delta] с учётом границ шкалы. */
    fun changed(kind: PetStatKind, delta: Int): PetState = when (kind) {
        PetStatKind.CARE -> copy(care = care + delta)
        PetStatKind.JOY -> copy(joy = joy + delta)
    }

    /**
     * Естественное снижение показателей по итогам игрового периода.
     * Величина зависит от стадии: чем старше питомец, тем дороже уход.
     */
    fun afterPeriodDecay(stage: GrowthStage): PetState = PetState(
        care = care - stage.careDecay,
        joy = joy - stage.joyDecay,
    )

    companion object {
        /** Стартовые значения при создании профиля. */
        val INITIAL = PetState(care = StatValue.of(60), joy = StatValue.of(60))
    }
}

/**
 * Стадия развития питомца. Меняется по совокупности решений
 * за несколько игровых периодов и никогда не понижается (ТЗ 2.5.10).
 */
enum class GrowthStage(
    val displayName: String,
    val requiredPoints: Int,
    /**
     * Снижение заботы за период. Растёт со стадией: подросшему питомцу
     * нужно больше еды и ухода.
     *
     * Это не украшение, а условие задачи. При постоянных потребностях
     * доход быстро обгонял расходы: к десятому дню у ребёнка набиралось
     * около трёхсот монет при каталоге в восемьдесят шесть, и выбирать
     * между обязательным, желаемым и накоплениями было уже не из чего
     * (ТЗ 2.1: за один период нельзя купить всё сразу). Растущие
     * потребности съедают запас и возвращают выбор, а ребёнку понятны:
     * питомец вырос — стал есть больше.
     */
    val careDecay: Int,
    /** Снижение радости за период; растёт со стадией по той же причине. */
    val joyDecay: Int,
) {
    BABY("Малыш", 0, careDecay = 20, joyDecay = 10),
    TEEN("Подросток", 5, careDecay = 25, joyDecay = 14),
    ADULT("Взрослый", 10, careDecay = 30, joyDecay = 18);

    companion object {
        /** Стадия, соответствующая накопленному числу очков роста. */
        fun forPoints(points: Int): GrowthStage =
            entries.last { points >= it.requiredPoints }
    }
}
