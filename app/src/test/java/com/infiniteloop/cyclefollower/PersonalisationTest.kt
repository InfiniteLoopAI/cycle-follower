package com.infiniteloop.cyclefollower

import com.infiniteloop.cyclefollower.data.DayLog
import com.infiniteloop.cyclefollower.data.DayMood
import com.infiniteloop.cyclefollower.data.PmsSeverity
import com.infiniteloop.cyclefollower.data.UserProfile
import com.infiniteloop.cyclefollower.domain.CycleEngine
import com.infiniteloop.cyclefollower.domain.CyclePhase
import com.infiniteloop.cyclefollower.domain.Personalisation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PersonalisationTest {

    private val start1 = LocalDate.of(2026, 1, 1)
    private val start2 = LocalDate.of(2026, 1, 29)
    private val start3 = LocalDate.of(2026, 2, 26)

    /** Logs a mood on the given cycle days of each listed cycle. */
    private fun profileWith(
        roughDays: IntRange,
        cycles: List<LocalDate> = listOf(start1, start2, start3),
        severity: PmsSeverity = PmsSeverity.MODERATE,
    ): UserProfile {
        val logs = mutableListOf<DayLog>()
        cycles.dropLast(1).forEach { anchor ->
            for (day in 1..28) {
                val date = anchor.plusDays((day - 1).toLong())
                logs += DayLog(date, if (day in roughDays) DayMood.ROUGH else DayMood.GOOD)
            }
        }
        return UserProfile(
            periodStarts = cycles,
            statedCycleLength = 28,
            periodLength = 5,
            pmsSeverity = severity,
            dayLogs = logs,
            setupComplete = true,
        )
    }

    @Test
    fun `no logs means no personalisation and no override`() {
        val bare = UserProfile(periodStarts = listOf(start1), setupComplete = true)
        val p = Personalisation.of(bare)
        assertEquals(0, p.daysLogged)
        assertFalse(p.hasEnoughData)
        assertNull(p.observedPmsStartDay)
        // The configured severity still drives the window.
        assertEquals(5, CycleEngine.pmsWindow(bare, 28))
    }

    @Test
    fun `one thin cycle is not enough to override the textbook`() {
        val thin = UserProfile(
            periodStarts = listOf(start1, start2),
            statedCycleLength = 28,
            dayLogs = (20..24).map { DayLog(start1.plusDays((it - 1).toLong()), DayMood.ROUGH) },
            setupComplete = true,
        )
        val p = Personalisation.of(thin)
        assertFalse("5 logged days must not move the window", p.hasEnoughData)
        assertEquals(5, CycleEngine.pmsWindow(thin, 28))
    }

    @Test
    fun `a consistent rough stretch moves the pms window earlier`() {
        // Rough from day 23 across two fully logged cycles; the default for MODERATE is day 24.
        val profile = profileWith(roughDays = 23..28)
        val p = Personalisation.of(profile)

        assertTrue(p.hasEnoughData)
        assertEquals(23, p.observedPmsStartDay)
        // window length = 28 - 23 + 1
        assertEquals(6, CycleEngine.pmsWindow(profile, 28))

        val onDay23 = CycleEngine.status(profile, start3.plusDays(22))!!
        assertEquals(23, onDay23.cycleDay)
        assertEquals(CyclePhase.LATE_LUTEAL, onDay23.phase)
    }

    @Test
    fun `observed data can also push the window later than the setting`() {
        val profile = profileWith(roughDays = 26..28, severity = PmsSeverity.SEVERE)
        val p = Personalisation.of(profile)
        assertEquals(26, p.observedPmsStartDay)
        // SEVERE would otherwise open the window on day 21; the logs say 26.
        assertEquals(3, CycleEngine.pmsWindow(profile, 28))
    }

    @Test
    fun `a rough patch during the period is not mistaken for pms`() {
        val profile = profileWith(roughDays = 1..3)
        val p = Personalisation.of(profile)
        assertTrue(p.hasEnoughData)
        assertNull("days 1-3 are the period, not the premenstrual window", p.observedPmsStartDay)
    }

    @Test
    fun `an isolated bad day is not a pattern`() {
        val logs = mutableListOf<DayLog>()
        listOf(start1, start2).forEach { anchor ->
            for (day in 1..28) {
                val date = anchor.plusDays((day - 1).toLong())
                // One rough day at 20, surrounded by good ones.
                logs += DayLog(date, if (day == 20) DayMood.ROUGH else DayMood.GOOD)
            }
        }
        val profile = UserProfile(
            periodStarts = listOf(start1, start2, start3),
            statedCycleLength = 28, dayLogs = logs, setupComplete = true,
        )
        assertNull(Personalisation.of(profile).observedPmsStartDay)
    }

    @Test
    fun `frequent tags surface and rare ones do not`() {
        val logs = (1..10).map {
            DayLog(start1.plusDays(it.toLong()), DayMood.ROUGH, setOf("CRAMPS"))
        } + DayLog(start1.plusDays(20), DayMood.GOOD, setOf("ACNE"))
        val profile = UserProfile(
            periodStarts = listOf(start1, start2, start3),
            statedCycleLength = 28, dayLogs = logs, setupComplete = true,
        )
        val tags = Personalisation.of(profile).frequentTags.map { it.name }
        assertTrue("CRAMPS" in tags)
        assertFalse("a single sighting is not a pattern", "ACNE" in tags)
    }

    @Test
    fun `personalisation never recurses through the timeline`() {
        // pmsWindow consults Personalisation, which places logs by cycle day. If that placement
        // ever went back through dayInfo/buildTimeline this would blow the stack.
        val profile = profileWith(roughDays = 23..28)
        repeat(40) { offset ->
            assertNotNull(CycleEngine.status(profile, start3.plusDays(offset.toLong())))
            assertNotNull(CycleEngine.dayInfo(profile, start3.plusDays(offset.toLong())))
        }
    }

    @Test
    fun `the memoised result tracks the profile it was computed for`() {
        val a = profileWith(roughDays = 23..28)
        val b = profileWith(roughDays = 26..28)
        assertEquals(23, Personalisation.of(a).observedPmsStartDay)
        assertEquals(26, Personalisation.of(b).observedPmsStartDay)
        assertEquals("recomputing for the first profile must not return the cached second", 23, Personalisation.of(a).observedPmsStartDay)
    }

    @Test
    fun `suppressed contraception still has no pms window whatever is logged`() {
        val profile = profileWith(roughDays = 23..28)
            .copy(contraception = com.infiniteloop.cyclefollower.data.Contraception.IMPLANT)
        assertEquals(0, CycleEngine.pmsWindow(profile, 28))
    }
}
