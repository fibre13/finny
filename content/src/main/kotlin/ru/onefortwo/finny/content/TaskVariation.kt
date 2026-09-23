package ru.onefortwo.finny.content

import kotlin.random.Random
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Переменные числа в задании.
 *
 * Задание с постоянными числами решается один раз, а дальше вспоминается
 * по ответу, а не считается. Здесь числа берутся заново при каждом
 * открытии, поэтому задание остаётся упражнением, а не загадкой
 * с известной отгадкой.
 *
 * Правильность при этом нигде не хранится готовым числом. Она либо
 * считается правилами задания (распределение суммы), либо выводится
 * тем же выражением, что и условие ([answer]), либо держится на связи
 * между значениями: цена «не помещается в план» не потому, что равна
 * девяти, а потому, что построена как «план плюс несколько монет».
 *
 * Числовые поля самого задания остаются числами: подстановка описана
 * здесь, в одном месте, и содержимое задания читается без шаблонов.
 */
@Serializable
data class TaskVariation(
    /** Случайные значения: имя — пара «от» и «до» включительно. */
    val values: Map<String, List<Int>> = emptyMap(),

    /**
     * Значения, посчитанные из уже взятых: имя — выражение.
     * Вычисляются в порядке записи.
     */
    val derived: Map<String, String> = emptyMap(),

    /** Выражение верного ответа для задания с вводом числа. */
    val answer: String? = null,

    /** Выражение суммы для задания на распределение. */
    val amount: String? = null,

    /** Выражения нижних границ по направлениям, в порядке правил задания. */
    @SerialName("rule_mins")
    val ruleMins: List<String> = emptyList(),

    /** Выражения цен вариантов: идентификатор варианта — выражение. */
    val prices: Map<String, String> = emptyMap(),
)

/**
 * Разбор выражения: имена и целые числа, действия `+ - * /` слева
 * направо, без скобок и приоритета.
 *
 * Приоритет сознательно не вводится: выражения здесь короткие — «план
 * плюс шесть», «монет в день умножить на дни», — и правило «слева
 * направо» проверяется глазами без разбора порядка действий.
 */
internal fun evaluate(expression: String, values: Map<String, Int>): Int {
    val tokens = expression.replace(" ", "")
        .split(Regex("(?<=[-+*/])|(?=[-+*/])"))
        .filter { it.isNotEmpty() }

    require(tokens.isNotEmpty()) { "Пустое выражение" }

    fun value(token: String): Int = token.toIntOrNull()
        ?: values[token]
        ?: error("В выражении «$expression» нет значения «$token»")

    var result = value(tokens.first())
    var index = 1
    while (index + 1 < tokens.size) {
        val operand = value(tokens[index + 1])
        result = when (val operation = tokens[index]) {
            "+" -> result + operand
            "-" -> result - operand
            "*" -> result * operand
            "/" -> result / operand
            else -> error("В выражении «$expression» неизвестное действие «$operation»")
        }
        index += 2
    }
    return result
}

/** Берёт числа задания: случайные значения и посчитанные из них. */
internal fun TaskVariation.draw(random: Random): Map<String, Int> {
    val drawn = LinkedHashMap<String, Int>()
    values.forEach { (name, range) ->
        require(range.size == 2) { "Диапазон «$name» задаётся парой чисел, дано: $range" }
        drawn[name] = random.nextInt(range[0], range[1] + 1)
    }
    derived.forEach { (name, expression) -> drawn[name] = evaluate(expression, drawn) }
    return drawn
}

/**
 * Формы существительных, которые встречаются в заданиях рядом с числом.
 *
 * Без них подстановка ломает согласование: «4 монеты», но «5 монет».
 * Ключ — форма для пяти и больше, она же пишется в тексте задания.
 */
private val NOUN_FORMS = mapOf(
    "монет" to Triple("монета", "монеты", "монет"),
    "дней" to Triple("день", "дня", "дней"),
)

/** Число со словом в нужной форме: 1 монета, 2 монеты, 5 монет. */
private fun withNoun(number: Int, noun: String): String {
    val forms = NOUN_FORMS[noun] ?: error("Нет форм слова «$noun»")
    val tail = number % 100
    val last = number % 10
    val word = when {
        tail in 11..14 -> forms.third
        last == 1 -> forms.first
        last in 2..4 -> forms.second
        else -> forms.third
    }
    return "$number $word"
}

/**
 * Место подстановки: `{имя}` либо `{имя:слово}` со склонением.
 *
 * Закрывающая скобка экранирована намеренно. Разбор регулярных
 * выражений на JVM принимает её и без экранирования, а разбор в Android
 * — нет: там неэкранированная `}` даёт `PatternSyntaxException` при
 * загрузке класса, то есть падение при открытии задания. Проверено на
 * устройстве: на JVM тесты при этом проходят.
 */
private val PLACEHOLDER = Regex("""\{(\w+)(?::([а-яё]+))?\}""")

/**
 * Подставляет значения в текст.
 *
 * `{имя}` даёт само число, `{имя:монет}` — число со словом в нужной
 * форме.
 */
internal fun String.fill(values: Map<String, Int>): String =
    PLACEHOLDER.replace(this) { match ->
        val name = match.groupValues[1]
        val number = values[name] ?: error("В тексте есть «$name», а значения нет")
        val noun = match.groupValues[2]
        if (noun.isEmpty()) number.toString() else withNoun(number, noun)
    }

/**
 * Задание с новыми числами.
 *
 * Возвращает то же задание, если переменных чисел у него нет: такие
 * задания — выбор решения без вычислений, варьировать в них нечего.
 */
fun TaskContent.withNumbers(random: Random): TaskContent {
    val variation = vary ?: return this
    val values = variation.draw(random)

    fun String.text() = fill(values)

    return when (this) {
        is AllocateTask -> copy(
            prompt = prompt.text(),
            explanationCorrect = explanationCorrect.text(),
            explanationWrong = explanationWrong.text(),
            amount = variation.amount?.let { evaluate(it, values) } ?: amount,
            rules = rules.mapIndexed { index, rule ->
                val expression = variation.ruleMins.getOrNull(index)
                if (expression == null) rule else rule.copy(min = evaluate(expression, values))
            },
        )

        is PickTask -> copy(
            prompt = prompt.text(),
            explanationCorrect = explanationCorrect.text(),
            explanationWrong = explanationWrong.text(),
            options = options.map { option ->
                val expression = variation.prices[option.id]
                if (expression == null) option else option.copy(price = evaluate(expression, values))
            },
        )

        is NumberTask -> copy(
            prompt = prompt.text(),
            explanationCorrect = explanationCorrect.text(),
            explanationWrong = explanationWrong.text(),
            answer = variation.answer?.let { evaluate(it, values) } ?: answer,
        )

        is ChoiceTask -> copy(
            prompt = prompt.text(),
            explanationCorrect = explanationCorrect.text(),
            explanationWrong = explanationWrong.text(),
        )
    }
}
