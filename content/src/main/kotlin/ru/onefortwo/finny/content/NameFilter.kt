package ru.onefortwo.finny.content

import kotlinx.serialization.Serializable

/** Список недопустимых слов из `forbidden_words.json`. */
@Serializable
data class ForbiddenWords(
    val roots: List<String> = emptyList(),
    val words: List<String> = emptyList(),
    val allowed: List<String> = emptyList(),
)

/**
 * Проверка игрового имени питомца: только цензура и этика — мат,
 * обзывательства, слова про какашки и оскорбления. Выдуманные, странные
 * и иностранные имена, числа и символы не ограничиваются.
 *
 * Имя нормализуется: нижний регистр, только буквы, «ё» → «е», повторы
 * букв схлопываются («дурааак» → «дурак»). Отдельно проверяется вариант,
 * где латинские буквы-двойники заменены кириллицей («пoпa» с латинскими
 * «o» и «a»).
 *
 * Корни ([ForbiddenWords.roots]) запрещены в любом месте имени — их можно
 * проверять на каждом нажатии. Короткие слова ([ForbiddenWords.words])
 * запрещены только как имя целиком: они встречаются в начале нормальных
 * имён («Попу» → «Попугай», «Лох» → «Лохматик»), поэтому проверяются,
 * когда ребёнок закончил ввод.
 */
class NameFilter(list: ForbiddenWords) {

    private val roots = list.roots.map(::normalize).filter { it.isNotEmpty() }.toSet()
    private val words = list.words.map(::normalize).filter { it.isNotEmpty() }.toSet()
    private val allowed = list.allowed.map(::normalize).filter { it.isNotEmpty() }

    /** В имени есть запрещённый корень: поле можно стирать сразу. */
    fun hasForbiddenRoot(name: String): Boolean = variants(name).any { v ->
        val rest = allowed.fold(v) { s, ok -> s.replace(ok, " ") }
        roots.any { it in rest }
    }

    /** Имя целиком — запрещённое короткое слово. */
    fun isForbiddenWord(name: String): Boolean = variants(name).any { it in words }

    /** Имя допустимо. */
    fun isAllowed(name: String): Boolean = !hasForbiddenRoot(name) && !isForbiddenWord(name)

    private fun variants(name: String): Set<String> =
        setOf(normalize(name), normalize(lookalikes(name)))

    companion object {
        /** Сообщение ребёнку, когда имя не подходит. */
        const val MESSAGE = "Так не принято называть питомцев. Придумай другое имя."

        /** Проверка без списка: пропускает любое имя. */
        val NONE = NameFilter(ForbiddenWords())

        private val LOOKALIKES = mapOf(
            'a' to 'а', 'b' to 'в', 'c' to 'с', 'e' to 'е', 'h' to 'н', 'k' to 'к', 'm' to 'м',
            'o' to 'о', 'p' to 'р', 't' to 'т', 'x' to 'х', 'y' to 'у', '0' to 'о',
        )

        private fun lookalikes(name: String): String =
            name.lowercase().map { LOOKALIKES[it] ?: it }.joinToString("")

        internal fun normalize(text: String): String {
            val letters = text.lowercase().replace('ё', 'е').filter { it.isLetter() }
            val collapsed = StringBuilder()
            for (ch in letters) {
                if (collapsed.isEmpty() || collapsed.last() != ch) collapsed.append(ch)
            }
            return collapsed.toString()
        }
    }
}
