package ru.onefortwo.finny

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.onefortwo.finny.ui.state.PetSpeech

/** Приветствие питомца по времени и давности. */
class PetSpeechTest {

    @Test
    fun `в тот же день — приветствие по времени суток`() {
        assertEquals("Доброе утро! Как спалось?", PetSpeech.greeting("2026-09-27", "2026-09-27", 8))
        assertEquals("Привет! Как дела?", PetSpeech.greeting("2026-09-27", "2026-09-27", 14))
        assertEquals("Добрый вечер! Я соскучился!", PetSpeech.greeting("2026-09-27", "2026-09-27", 20))
        assertEquals("Привет! Я так рад тебя видеть!", PetSpeech.greeting(null, "2026-09-27", 2))
    }

    @Test
    fun `после перерыва — приветствие по давности, только позитивное`() {
        assertEquals("Привет! Я тебя ждал!", PetSpeech.greeting("2026-09-26", "2026-09-27", 14))
        assertEquals("Привет! Мы давно не виделись. Я скучал!", PetSpeech.greeting("2026-09-24", "2026-09-27", 14))
        assertEquals("Ой, привет! Я так ждал тебя! Давай поиграем?", PetSpeech.greeting("2026-09-20", "2026-09-27", 14))
        assertEquals("Привет! Я тебя заждался! Хочется поиграть!", PetSpeech.greeting("2026-09-14", "2026-09-27", 14))
        assertEquals("Привет! Я так рад! Давай скорее начнём!", PetSpeech.greeting("2026-09-01", "2026-09-27", 14))
    }
}
