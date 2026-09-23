package ru.onefortwo.finny.economy

import org.junit.Assert.assertEquals
import org.junit.Test

/** Проверки показателей состояния и стадий развития питомца (ТЗ 2.5.10). */
class PetTest {

    @Test
    fun `показатель не опускается ниже нуля`() {
        val care = StatValue.of(10) - 30

        assertEquals(0, care.value)
    }

    @Test
    fun `показатель не поднимается выше ста`() {
        val joy = StatValue.of(90) + 30

        assertEquals(100, joy.value)
    }

    @Test
    fun `стадия развития определяется накопленными очками`() {
        assertEquals(GrowthStage.BABY, GrowthStage.forPoints(0))
        assertEquals(GrowthStage.BABY, GrowthStage.forPoints(4))
        assertEquals(GrowthStage.TEEN, GrowthStage.forPoints(5))
        assertEquals(GrowthStage.TEEN, GrowthStage.forPoints(9))
        assertEquals(GrowthStage.ADULT, GrowthStage.forPoints(10))
        assertEquals(GrowthStage.ADULT, GrowthStage.forPoints(15))
    }
}
