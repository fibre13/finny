package ru.onefortwo.finny.ui.state

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/*
 * ТЕСТ (ветка test/kopilka-a): реплики питомца в облачке. Только позитив:
 * без давления на жалость и без упрёков за отсутствие (ТЗ 3.5).
 */
object PetSpeech {

    /** Приветствие на последнем шаге знакомства. */
    const val HELLO = "Привет!"

    /** Прощание, когда ребёнок заканчивает день. */
    const val FAREWELL = "Пока! Приходи завтра — я буду ждать!"

    /**
     * Приветствие при входе. [lastVisit] — дата прошлого занятия
     * (ГГГГ-ММ-ДД) или `null`, если его не было; [hour] — час по часам
     * устройства. Если ребёнок не заходил день и больше — реплика по
     * давности, иначе — по времени суток.
     */
    fun greeting(lastVisit: String?, today: String, hour: Int): String {
        val days = daysBetween(lastVisit, today)
        return when {
            days == null || days <= 0 -> byTime(hour)
            days == 1L -> "Привет! Я тебя ждал!"
            days <= 3 -> "Привет! Мы давно не виделись. Я скучал!"
            days <= 7 -> "Ой, привет! Я так ждал тебя! Давай поиграем?"
            days <= 14 -> "Привет! Я тебя заждался! Хочется поиграть!"
            else -> "Привет! Я так рад! Давай скорее начнём!"
        }
    }

    private fun byTime(hour: Int): String = when (hour) {
        in 6..11 -> "Доброе утро! Как спалось?"
        in 12..17 -> "Привет! Как дела?"
        in 18..22 -> "Добрый вечер! Я соскучился!"
        else -> "Привет! Я так рад тебя видеть!"
    }

    private fun daysBetween(from: String?, to: String): Long? = try {
        from?.let { ChronoUnit.DAYS.between(LocalDate.parse(it), LocalDate.parse(to)) }
    } catch (_: Exception) {
        null
    }
}
