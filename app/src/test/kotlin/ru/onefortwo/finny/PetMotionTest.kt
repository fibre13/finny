package ru.onefortwo.finny

import java.io.File
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.onefortwo.finny.content.AssetSource
import ru.onefortwo.finny.content.ContentRepository
import ru.onefortwo.finny.economy.GrowthStage
import ru.onefortwo.finny.economy.StatLevel
import ru.onefortwo.finny.ui.common.PetMotionClock
import ru.onefortwo.finny.ui.common.composePet

/** Постоянные движения питомца: дыхание, моргание, прыжки. */
class PetMotionTest {

    private val content = ContentRepository(
        AssetSource { name -> File("../content/src/main/assets/$name").readText() },
    )
    private val art = content.pixelArt()
    private val animation = art.animation

    private fun run(state: String, ticks: Int = 800, seed: Int = 7) =
        PetMotionClock(animation, Random(seed)).let { clock -> (0 until ticks).map { clock.frameAt(it, state) } }

    @Test
    fun `дыхание чередует кадры, у спокойного по 4 такта, у уставшего по 6`() {
        val idle = run("idle", ticks = 16).map { it.breath }
        assertEquals(listOf(0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 1), idle)
        val tired = run("tired", ticks = 12).map { it.breath }
        assertEquals(listOf(0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1), tired)
    }

    @Test
    fun `моргание длится такт и повторяется не чаще раза в три секунды`() {
        listOf("idle", "happy", "tired").forEach { state ->
            val blinks = run(state).withIndex().filter { it.value.blink }.map { it.index }
            assertTrue("Моргания нет: $state", blinks.isNotEmpty())
            blinks.zipWithNext().forEach { (a, b) ->
                // 24 такта по 125 мс — 3 секунды.
                assertTrue("Моргания через ${b - a} тактов: $state", b - a >= 24)
            }
        }
    }

    @Test
    fun `прыгает только радостный питомец и не выше трёх клеток`() {
        val happy = run("happy").map { it.dy }
        assertTrue("Радостный не прыгает", happy.any { it != 0 })
        assertTrue("Смещение больше трёх клеток", happy.all { it in -3..0 })
        assertTrue("Спокойный прыгает", run("idle").all { it.dy == 0 })
        assertTrue("Уставший прыгает", run("tired").all { it.dy == 0 })
    }

    @Test
    fun `облака сменяют кадр раз в четыре секунды`() {
        val sky = run("idle", ticks = 64).map { it.sky }
        assertEquals(List(32) { 0 } + List(32) { 1 }, sky)
    }

    @Test
    fun `второй кадр дыхания, моргание и прыжок меняют изображение, тень остаётся на месте`() {
        val hex = content.petParts().colors.first().hex
        fun pet(frame: Int = 0, blink: Boolean = false, dy: Int = 0) = composePet(
            art, "cat", GrowthStage.TEEN, hex, "bow", StatLevel.MEDIUM, StatLevel.MEDIUM,
            frame = frame, blink = blink, dy = dy,
        )
        val still = pet()
        assertFalse(still.contentEquals(pet(frame = 1)))
        assertFalse(still.contentEquals(pet(blink = true)))
        val jump = pet(dy = -3)
        assertFalse(still.contentEquals(jump))
        // Нижние строки холста: тень под фигурой при прыжке не поднимается.
        val shadowRows = (art.baseline until art.baseline + 2).flatMap { y ->
            (0 until art.petSize).map { x -> y * art.petSize + x }
        }
        val shadow = art.shadowOnCard
        assertTrue(shadowRows.any { still[it] == shadow })
        shadowRows.forEach { i ->
            if (still[i] == shadow) assertEquals("Тень сдвинулась в клетке $i", shadow, jump[i])
        }
    }
}
