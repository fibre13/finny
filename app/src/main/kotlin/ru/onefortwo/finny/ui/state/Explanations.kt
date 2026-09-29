package ru.onefortwo.finny.ui.state

import ru.onefortwo.finny.economy.Coins
import ru.onefortwo.finny.economy.GoalForecast
import ru.onefortwo.finny.economy.GrowthCondition
import ru.onefortwo.finny.economy.IncomeSource
import ru.onefortwo.finny.economy.PetStatKind
import ru.onefortwo.finny.economy.PeriodOutcome
import ru.onefortwo.finny.economy.PurchaseResult
import ru.onefortwo.finny.economy.StatLevel
import ru.onefortwo.finny.economy.StatValue

/**
 * Формулировки для ребёнка. Модуль экономики возвращает структурированные
 * результаты, а словесные объяснения собираются здесь, в интерфейсном слое
 * (ТЗ 3.4: разделение логики и интерфейса).
 *
 * Требования к формулировкам: короткие фразы, простые слова, объяснение
 * причины и следующий шаг; тексты не запугивают и не стыдят (ТЗ 3.5, 3.6).
 */
object Explanations {

    /** Склонение слова «монета» для числа. */
    fun coins(amount: Int): String {
        val tail = amount % 100
        val last = amount % 10
        val word = when {
            tail in 11..14 -> "монет"
            last == 1 -> "монета"
            last in 2..4 -> "монеты"
            else -> "монет"
        }
        return "$amount $word"
    }

    fun coins(value: Coins): String = coins(value.amount)

    /**
     * «Монета» в винительном падеже: «распределить 1 монету», «на 21 монету».
     * Отличается от [coins] только формой для единицы.
     */
    fun coinsAccusative(amount: Int): String {
        val tail = amount % 100
        return if (amount % 10 == 1 && tail != 11) "$amount монету" else coins(amount)
    }

    /** Склонение слова «день» для числа игровых периодов. */
    fun days(count: Int): String {
        val tail = count % 100
        val last = count % 10
        val word = when {
            tail in 11..14 -> "дней"
            last == 1 -> "день"
            last in 2..4 -> "дня"
            else -> "дней"
        }
        return "$count $word"
    }

    fun statName(kind: PetStatKind): String = when (kind) {
        PetStatKind.CARE -> "Забота"
        PetStatKind.JOY -> "Радость"
    }

    /** Текстовая метка уровня показателя: состояние читается без цвета (ТЗ 3.6). */
    fun statLabel(kind: PetStatKind, value: StatValue): String = when (kind) {
        PetStatKind.CARE -> when (value.level) {
            StatLevel.LOW -> "Голодный"
            StatLevel.MEDIUM -> "В порядке"
            StatLevel.HIGH -> "Довольный"
        }

        PetStatKind.JOY -> when (value.level) {
            StatLevel.LOW -> "Скучает"
            StatLevel.MEDIUM -> "Спокойный"
            StatLevel.HIGH -> "Весёлый"
        }
    }

    fun incomeSource(source: IncomeSource): String = when (source) {
        IncomeSource.START_BUDGET -> "Стартовые монеты"
        IncomeSource.POCKET_MONEY -> "Карманные монеты на новый день"
        IncomeSource.TASK_CORRECT -> "Награда за задание"
        IncomeSource.TASK_PARTIAL -> "Награда за старание"
        IncomeSource.RECOVERY_TASK -> "Награда за помощь питомцу"
        IncomeSource.SEASON_MONEY -> "Монеты на новый сезон"
        IncomeSource.GIFT -> "Подарок"
    }

    /**
     * Строка о награде: источник и фактически начисленная сумма.
     *
     * За повтор задания начисляется половина награды за этот ответ, и об
     * этом говорится прямо: иначе меньшая сумма при том же источнике
     * выглядит ошибкой счёта. Половина считается от награды за текущий
     * ответ, а не за первый: первый мог быть ошибочным, поэтому
     * «половина от первого раза» была бы неверна.
     */
    fun reward(source: IncomeSource, amount: Coins, repeat: Boolean = false): String =
        if (amount.amount == 0) {
            // Монеты платятся за первые задания дня.
            "Монеты за задания на сегодня уже получены. Завтра можно заработать снова"
        } else if (repeat) {
            "${incomeSource(source)} ещё раз: +${coins(amount)}, за повтор — половина награды"
        } else {
            "${incomeSource(source)}: +${coins(amount)}"
        }

    /** Сообщение о начислении: источник и сумма (ТЗ 2.5.4). */
    fun income(
        source: IncomeSource,
        amount: Coins,
        balanceAfter: Coins,
        repeat: Boolean = false,
    ): FeedbackMessage = FeedbackMessage(
        text = "${reward(source, amount, repeat)}. Теперь у тебя ${coins(balanceAfter)}.",
    )

    /** Объяснение успешной покупки: что изменилось и почему. */
    fun purchase(
        petName: String,
        result: PurchaseResult.Success,
        itemTitle: String,
        unlockedWardrobe: Boolean = false,
    ): FeedbackMessage {
        val delta = result.statAfter.value - result.statBefore.value
        val stat = statName(result.stat).lowercase()
        // Показатель упирается в предел, и тогда покупка его не двигает.
        // «Выросла на 0» в этом случае читается как ошибка счёта, поэтому
        // о пределе говорится прямо.
        // Имя в именительном падеже: «У Мурзик» читалось бы как ошибка.
        val change = if (delta > 0) {
            "$petName: $stat выросла на $delta."
        } else {
            "$petName: $stat и так полная, выше некуда."
        }
        val base = "Купили: $itemTitle за ${coins(result.record.price)}. $change"

        return when {
            result.exceedsPlan -> FeedbackMessage(
                text = "$base Но эта покупка вышла за план.",
                nextStep = "Ничего страшного: учти это в плане на завтра.",
            )

            unlockedWardrobe -> FeedbackMessage(
                text = base,
                nextStep = "Теперь это украшение можно надеть в гардеробе.",
            )

            else -> FeedbackMessage(text = base)
        }
    }

    /** Сообщение о том, что экранное время на сегодня закончилось. */
    fun timeIsUp(): FeedbackMessage = FeedbackMessage(
        text = "На сегодня хватит: 20 минут прошли. Глазам нужен отдых.",
        nextStep = "Приходи завтра, прогресс сохранён.",
    )

    /** Предупреждение, что время подходит к концу. */
    fun timeRunningOut(minutesLeft: Int): FeedbackMessage = FeedbackMessage(
        text = "Осталось ${minutes(minutesLeft)} на сегодня.",
        nextStep = "Успей закончить то, что начал.",
    )

    /** Склонение слова «минута» для числа. */
    fun minutes(count: Int): String {
        val tail = count % 100
        val last = count % 10
        val word = when {
            tail in 11..14 -> "минут"
            last == 1 -> "минута"
            last in 2..4 -> "минуты"
            else -> "минут"
        }
        return "$count $word"
    }

    /** ТЕСТ 3: склонение слова «задание» для числа. */
    fun tasks(count: Int): String {
        val tail = count % 100
        val last = count % 10
        val word = when {
            tail in 11..14 -> "заданий"
            last == 1 -> "задание"
            last in 2..4 -> "задания"
            else -> "заданий"
        }
        return "$count $word"
    }

    /** Объяснение нехватки монет и варианты действий (ТЗ 2.5.6). */
    fun notEnoughCoins(result: PurchaseResult.NotEnoughCoins, itemTitle: String): FeedbackMessage =
        FeedbackMessage(
            text = "«$itemTitle» стоит ${coins(result.price)}, а у тебя ${coins(result.balance)}. " +
                "Не хватает ${coins(result.shortfall)}.",
            nextStep = "Выполни задание, выбери что-то дешевле или отложи покупку на завтра.",
            isProblem = true,
        )

    /** Срок достижения цели простыми словами (ТЗ 2.5.7). */
    fun forecast(forecast: GoalForecast): String = when (forecast) {
        GoalForecast.NoGoal -> "Цель пока не выбрана."
        GoalForecast.Reached -> "Ты накопил всю сумму!"
        GoalForecast.NotEnoughData -> "Пополни копилку, и я посчитаю срок."
        is GoalForecast.Periods -> "При таком темпе цель будет твоей через ${days(forecast.periods)}."
    }

    fun condition(condition: GrowthCondition): String = when (condition) {
        GrowthCondition.NEEDS_COVERED -> "Купил нужное"
        GrowthCondition.WITHIN_PLAN -> "Уложился в план"
        GrowthCondition.SAVED_SOMETHING -> "Пополнил копилку"
    }

    /** Почему условие не выполнено: объяснение вместо упрёка. */
    fun conditionMissed(condition: GrowthCondition): String = when (condition) {
        GrowthCondition.NEEDS_COVERED -> "Нужное не купили"
        GrowthCondition.WITHIN_PLAN -> "Потратили больше плана"
        GrowthCondition.SAVED_SOMETHING -> "В копилку ничего не отложили"
    }

    /** Итог периода: причина изменения состояния питомца (ТЗ 2.5.10). */
    fun periodSummary(petName: String, outcome: PeriodOutcome): FeedbackMessage {
        val points = outcome.earnedPoints

        if (outcome.isSetback) {
            val reason = when {
                outcome.earnedPoints == 0 -> "за день не набралось ни одного шага"
                else -> "$petName проголодался"
            }
            return FeedbackMessage(
                text = "День закончился: $reason. Прогресс сохранён, ничего не потеряно.",
                nextStep = "Выполни задание «Помоги своему питомцу» или сначала купи нужное в новом плане.",
                isProblem = true,
            )
        }

        val base = if (points == 3) {
            "Отличный день: все три шага сделаны."
        } else {
            "День засчитан: сделано $points из 3 шагов."
        }

        val stage = if (outcome.stageAdvanced) {
            " $petName подрос и теперь ${outcome.stageAfter.displayName.lowercase()}!"
        } else {
            ""
        }

        return FeedbackMessage(text = base + stage)
    }
}
