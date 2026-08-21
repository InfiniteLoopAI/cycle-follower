package com.infiniteloop.cyclefollower

import com.infiniteloop.cyclefollower.data.Contraception
import com.infiniteloop.cyclefollower.data.PmsSeverity
import com.infiniteloop.cyclefollower.data.UserProfile
import com.infiniteloop.cyclefollower.domain.Confidence
import com.infiniteloop.cyclefollower.domain.CycleEngine
import com.infiniteloop.cyclefollower.domain.CyclePhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CycleEngineTest {

    private val day1 = LocalDate.of(2025, 3, 1)

    private fun profile(
        starts: List<LocalDate> = listOf(day1),
        cycleLength: Int = 28,
        periodLength: Int = 5,
        contraception: Contraception = Contraception.NONE,
        pms: PmsSeverity = PmsSeverity.MODERATE,
        pmdd: Boolean = false,
    ) = UserProfile(
        periodStarts = starts,
        statedCycleLength = cycleLength,
        periodLength = periodLength,
        contraception = contraception,
        pmsSeverity = pms,
        pmdd = pmdd,
        setupComplete = true,
    )

    @Test
    fun `no logged period yields no status`() {
        assertNull(CycleEngine.status(UserProfile(), day1))
    }

    @Test
    fun `future only dates yield no status`() {
        val future = profile(starts = listOf(day1.plusDays(10)))
        assertNull(CycleEngine.status(future, day1))
    }

    @Test
    fun `cycle day counts from day one inclusive`() {
        val status = CycleEngine.status(profile(), day1)!!
        assertEquals(1, status.cycleDay)
        assertEquals(CyclePhase.MENSTRUAL, status.phase)

        val later = CycleEngine.status(profile(), day1.plusDays(13))!!
        assertEquals(14, later.cycleDay)
    }

    @Test
    fun `ovulation is fourteen days before the next period not day fourteen`() {
        val standard = CycleEngine.status(profile(cycleLength = 28), day1)!!
        assertEquals(14, standard.ovulationCycleDay)

        val long = CycleEngine.status(profile(cycleLength = 32), day1)!!
        assertEquals(18, long.ovulationCycleDay)

        val short = CycleEngine.status(profile(cycleLength = 24), day1)!!
        assertEquals(10, short.ovulationCycleDay)
    }

    @Test
    fun `fertile window is five days before ovulation through one day after`() {
        val status = CycleEngine.status(profile(cycleLength = 28), day1)!!
        val (start, end) = status.fertileWindow!!
        assertEquals(status.ovulationDate!!.minusDays(5), start)
        assertEquals(status.ovulationDate!!.plusDays(1), end)

        val duringWindow = CycleEngine.status(profile(cycleLength = 28), day1.plusDays(10))!!
        assertTrue(duringWindow.inFertileWindow)

        val outsideWindow = CycleEngine.status(profile(cycleLength = 28), day1.plusDays(20))!!
        assertFalse(outsideWindow.inFertileWindow)
    }

    @Test
    fun `phases land on the expected days for a textbook cycle`() {
        val p = profile(cycleLength = 28, periodLength = 5)
        fun phaseOn(day: Int) = CycleEngine.status(p, day1.plusDays((day - 1).toLong()))!!.phase

        assertEquals(CyclePhase.MENSTRUAL, phaseOn(1))
        assertEquals(CyclePhase.MENSTRUAL, phaseOn(5))
        assertEquals(CyclePhase.FOLLICULAR, phaseOn(6))
        assertEquals(CyclePhase.FOLLICULAR, phaseOn(8))
        assertEquals(CyclePhase.FERTILE_WINDOW, phaseOn(9))
        assertEquals(CyclePhase.FERTILE_WINDOW, phaseOn(12))
        assertEquals(CyclePhase.OVULATION, phaseOn(13))
        assertEquals(CyclePhase.OVULATION, phaseOn(15))
        assertEquals(CyclePhase.EARLY_LUTEAL, phaseOn(16))
        assertEquals(CyclePhase.EARLY_LUTEAL, phaseOn(23))
        assertEquals(CyclePhase.LATE_LUTEAL, phaseOn(24))
        assertEquals(CyclePhase.LATE_LUTEAL, phaseOn(28))
    }

    @Test
    fun `pms window widens with severity and pmdd`() {
        fun pmsStart(pms: PmsSeverity, pmdd: Boolean = false): Int =
            CycleEngine.status(profile(pms = pms, pmdd = pmdd), day1)!!
                .timeline.first { it.phase == CyclePhase.LATE_LUTEAL }.startDay

        assertEquals(26, pmsStart(PmsSeverity.MILD))
        assertEquals(24, pmsStart(PmsSeverity.MODERATE))
        assertEquals(21, pmsStart(PmsSeverity.SEVERE))
        assertEquals(21, pmsStart(PmsSeverity.MODERATE, pmdd = true))
    }

    @Test
    fun `no pms window when she barely notices it`() {
        val status = CycleEngine.status(profile(pms = PmsSeverity.NONE), day1)!!
        assertTrue(status.timeline.none { it.phase == CyclePhase.LATE_LUTEAL })
        val lateInCycle = CycleEngine.status(profile(pms = PmsSeverity.NONE), day1.plusDays(26))!!
        assertEquals(CyclePhase.EARLY_LUTEAL, lateInCycle.phase)
    }

    @Test
    fun `average comes from logged history once there are enough cycles`() {
        // Real gaps of 30, 30, 30 days -- the stated 28 should be ignored.
        val starts = listOf(
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 1, 31),
            LocalDate.of(2025, 3, 2),
            LocalDate.of(2025, 4, 1),
        )
        val status = CycleEngine.status(profile(starts = starts, cycleLength = 28), LocalDate.of(2025, 4, 5))!!
        assertEquals(30, status.cycleLength)
        assertEquals(3, status.cyclesTracked)
        assertEquals(0, status.variabilityDays)
        assertEquals(Confidence.HIGH, status.confidence)
    }

    @Test
    fun `stated length is used when history is switched off`() {
        val starts = listOf(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31))
        val p = profile(starts = starts, cycleLength = 26).copy(useHistoryAverage = false)
        assertEquals(26, CycleEngine.status(p, LocalDate.of(2025, 2, 5))!!.cycleLength)
    }

    @Test
    fun `wildly irregular history lowers confidence`() {
        val starts = listOf(
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 1, 23),   // 22
            LocalDate.of(2025, 3, 3),    // 39
            LocalDate.of(2025, 3, 26),   // 23
        )
        val status = CycleEngine.status(profile(starts = starts), LocalDate.of(2025, 4, 1))!!
        assertTrue(status.variabilityDays >= 5)
        assertEquals(Confidence.LOW, status.confidence)
    }

    @Test
    fun `implausible gaps are discarded rather than skewing the average`() {
        // A duplicate-ish entry three days apart must not be treated as a three day cycle.
        val starts = listOf(
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 1, 4),
            LocalDate.of(2025, 2, 1),
        )
        val gaps = CycleEngine.plausibleGaps(profile(starts = starts))
        assertEquals(listOf(28), gaps)
    }

    @Test
    fun `late period is reported and does not go negative`() {
        // A 28 day cycle covers days 1..28, so the period is due on cycle day 29.
        // Cycle day 32 is therefore three days late, not four.
        val status = CycleEngine.status(profile(cycleLength = 28), day1.plusDays(31))!!
        assertEquals(32, status.cycleDay)
        assertEquals(day1.plusDays(28), status.nextPeriodStart)
        assertEquals(3, status.daysLate)
        assertTrue(status.isLate)
        assertEquals(CyclePhase.LATE_LUTEAL, status.phase)

        val onTime = CycleEngine.status(profile(cycleLength = 28), day1.plusDays(10))!!
        assertEquals(0, onTime.daysLate)
        assertFalse(onTime.isLate)

        val dueToday = CycleEngine.status(profile(cycleLength = 28), day1.plusDays(28))!!
        assertEquals(29, dueToday.cycleDay)
        assertEquals(0, dueToday.daysLate)
        assertFalse(dueToday.isLate)
    }

    @Test
    fun `very old data is flagged as stale`() {
        val status = CycleEngine.status(profile(cycleLength = 28), day1.plusDays(200))!!
        assertTrue(status.dataIsStale)
    }

    @Test
    fun `combined pill removes ovulation and the fertile window`() {
        val status = CycleEngine.status(profile(contraception = Contraception.COMBINED_PILL), day1.plusDays(12))!!
        assertNull(status.ovulationDate)
        assertNull(status.fertileWindow)
        assertFalse(status.inFertileWindow)
        assertTrue(status.ovulationSuppressed)
        assertEquals(Confidence.SUPPRESSED, status.confidence)
        assertEquals(CyclePhase.STEADY_STATE, status.phase)
        assertTrue(status.timeline.none { it.phase.isNaturalCycle && it.phase != CyclePhase.MENSTRUAL })
    }

    @Test
    fun `combined pill break week is reported between the bleed and the active pills`() {
        val p = profile(contraception = Contraception.COMBINED_PILL, periodLength = 4)
        assertEquals(CyclePhase.MENSTRUAL, CycleEngine.status(p, day1.plusDays(1))!!.phase)
        assertEquals(CyclePhase.HORMONE_BREAK, CycleEngine.status(p, day1.plusDays(5))!!.phase)
        assertEquals(CyclePhase.STEADY_STATE, CycleEngine.status(p, day1.plusDays(10))!!.phase)
    }

    @Test
    fun `hormonal iud keeps a natural cycle but flags it as uncertain`() {
        val status = CycleEngine.status(profile(contraception = Contraception.HORMONAL_IUD), day1.plusDays(13))!!
        assertNotNull(status.ovulationDate)
        assertTrue(status.ovulationUncertain)
        assertFalse(status.ovulationSuppressed)
        assertEquals(Confidence.LOW, status.confidence)
    }

    @Test
    fun `copper iud behaves exactly like a natural cycle`() {
        val natural = CycleEngine.status(profile(contraception = Contraception.NONE), day1.plusDays(13))!!
        val copper = CycleEngine.status(profile(contraception = Contraception.COPPER_IUD), day1.plusDays(13))!!
        assertEquals(natural.phase, copper.phase)
        assertEquals(natural.ovulationDate, copper.ovulationDate)
    }

    @Test
    fun `timeline always covers every day of every plausible cycle with no gaps`() {
        for (contraception in Contraception.entries) {
            for (cycleLength in UserProfile.MIN_CYCLE_LENGTH..UserProfile.MAX_CYCLE_LENGTH) {
                for (periodLength in 1..10) {
                    for (pms in PmsSeverity.entries) {
                        val p = profile(
                            cycleLength = cycleLength,
                            periodLength = periodLength,
                            contraception = contraception,
                            pms = pms,
                        )
                        val status = CycleEngine.status(p, day1)!!
                        val spans = status.timeline

                        assertEquals("first span must start on day 1", 1, spans.first().startDay)
                        assertTrue(
                            "last span must reach the end of the cycle",
                            spans.last().endDay >= cycleLength,
                        )
                        spans.zipWithNext { a, b ->
                            assertEquals(
                                "gap or overlap between ${a.phase} and ${b.phase} " +
                                    "(len=$cycleLength period=$periodLength $contraception $pms)",
                                a.endDay + 1,
                                b.startDay,
                            )
                        }
                        spans.forEach {
                            assertTrue("empty span ${it.phase}", it.endDay >= it.startDay)
                        }
                        // Every day in the cycle resolves to exactly one phase.
                        for (day in 1..cycleLength) {
                            assertEquals(1, spans.count { day in it })
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `day info uses the real measured length for past cycles`() {
        // Logged gap of 35 days: the day before the next logged period must still be inside that cycle.
        val starts = listOf(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 5))
        val p = profile(starts = starts, cycleLength = 28)

        val lastDayOfOldCycle = CycleEngine.dayInfo(p, LocalDate.of(2025, 2, 4))!!
        assertEquals(35, lastDayOfOldCycle.cycleDay)
        assertFalse(lastDayOfOldCycle.predicted)

        val firstDayOfNewCycle = CycleEngine.dayInfo(p, LocalDate.of(2025, 2, 5))!!
        assertEquals(1, firstDayOfNewCycle.cycleDay)
        assertEquals(CyclePhase.MENSTRUAL, firstDayOfNewCycle.phase)
    }

    @Test
    fun `day info projects forward past the last logged period`() {
        val p = profile(starts = listOf(day1), cycleLength = 28)
        val nextCycleDayOne = CycleEngine.dayInfo(p, day1.plusDays(28))!!
        assertEquals(1, nextCycleDayOne.cycleDay)
        assertTrue(nextCycleDayOne.predicted)
        assertEquals(CyclePhase.MENSTRUAL, nextCycleDayOne.phase)

        val twoCyclesOn = CycleEngine.dayInfo(p, day1.plusDays(56))!!
        assertEquals(1, twoCyclesOn.cycleDay)
    }

    @Test
    fun `day info returns nothing before the first logged period`() {
        assertNull(CycleEngine.dayInfo(profile(), day1.minusDays(1)))
    }
}
