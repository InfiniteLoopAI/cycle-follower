package com.infiniteloop.cyclefollower

import com.infiniteloop.cyclefollower.data.UserProfile
import com.infiniteloop.cyclefollower.domain.CyclePhase
import com.infiniteloop.cyclefollower.domain.Planner
import com.infiniteloop.cyclefollower.domain.Suitability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PlannerTest {

    private val day1 = LocalDate.of(2026, 3, 1)
    private val profile = UserProfile(
        periodStarts = listOf(day1),
        statedCycleLength = 28,
        periodLength = 5,
        setupComplete = true,
    )

    @Test
    fun `nothing to plan without a logged period`() {
        assertTrue(Planner.windows(UserProfile(), day1).isEmpty())
        assertNull(Planner.dayFor(UserProfile(), day1))
    }

    @Test
    fun `the first two days of the period are the worst of the month`() {
        assertEquals(Suitability.AVOID, Planner.dayFor(profile, day1)!!.suitability)
        assertEquals(Suitability.AVOID, Planner.dayFor(profile, day1.plusDays(1))!!.suitability)
        // By day four the period is easing off.
        assertEquals(Suitability.POOR, Planner.dayFor(profile, day1.plusDays(3))!!.suitability)
    }

    @Test
    fun `the follicular and fertile stretch scores best and the pms window scores worst`() {
        assertEquals(Suitability.BEST, Planner.dayFor(profile, day1.plusDays(7))!!.suitability)
        assertEquals(Suitability.BEST, Planner.dayFor(profile, day1.plusDays(13))!!.suitability)
        assertEquals(Suitability.OK, Planner.dayFor(profile, day1.plusDays(18))!!.suitability)
        assertEquals(Suitability.AVOID, Planner.dayFor(profile, day1.plusDays(25))!!.suitability)
    }

    @Test
    fun `windows tile the horizon with no gaps and no overlaps`() {
        val windows = Planner.windows(profile, day1, days = 90)
        assertTrue(windows.isNotEmpty())
        assertEquals(day1, windows.first().start)
        windows.zipWithNext { a, b ->
            assertEquals("gap or overlap between windows", a.end.plusDays(1), b.start)
            assertTrue("adjacent windows must differ", a.suitability != b.suitability)
        }
        assertEquals(89L, java.time.temporal.ChronoUnit.DAYS.between(day1, windows.last().end))
        assertEquals(90, windows.sumOf { it.days })
    }

    @Test
    fun `good windows are genuinely good and long enough to be worth booking`() {
        val good = Planner.goodWindows(profile, day1)
        assertTrue(good.isNotEmpty())
        good.forEach {
            assertEquals(Suitability.BEST, it.suitability)
            assertTrue("a two-day window is not worth surfacing", it.days >= 3)
        }
    }

    @Test
    fun `avoid windows land on the pms week`() {
        val avoid = Planner.avoidWindows(profile, day1)
        assertTrue(avoid.isNotEmpty())
        assertTrue(avoid.any { it.phase == CyclePhase.LATE_LUTEAL })
    }

    @Test
    fun `a bad date gets a nearby better one and a good date gets none`() {
        val badDay = day1.plusDays(25)
        val better = Planner.betterDateNear(profile, badDay, notBefore = day1)
        assertNotNull(better)
        assertEquals(Suitability.BEST, Planner.dayFor(profile, better!!)!!.suitability)

        val goodDay = day1.plusDays(7)
        assertNull("a date that already scores best needs no alternative",
            Planner.betterDateNear(profile, goodDay, notBefore = day1))
    }

    @Test
    fun `a suppressed method has no bad weeks to plan around`() {
        val onImplant = profile.copy(contraception = com.infiniteloop.cyclefollower.data.Contraception.IMPLANT)
        val windows = Planner.windows(onImplant, day1, days = 60)
        assertTrue("nothing on a flat-hormone method should score AVOID after the bleed",
            windows.none { it.suitability == Suitability.AVOID && it.phase == CyclePhase.LATE_LUTEAL })
    }

    @Test
    fun `every phase maps to a suitability`() {
        CyclePhase.entries.forEach { phase ->
            assertNotNull(Planner.suitabilityOf(phase, 1))
        }
    }
}
