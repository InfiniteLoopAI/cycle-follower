package com.infiniteloop.cyclefollower

import com.infiniteloop.cyclefollower.data.Contraception
import com.infiniteloop.cyclefollower.data.PmsSeverity
import com.infiniteloop.cyclefollower.data.Symptom
import com.infiniteloop.cyclefollower.data.UserProfile
import com.infiniteloop.cyclefollower.domain.Block
import com.infiniteloop.cyclefollower.domain.Briefings
import com.infiniteloop.cyclefollower.domain.CycleEngine
import com.infiniteloop.cyclefollower.domain.CyclePhase
import com.infiniteloop.cyclefollower.domain.Library
import com.infiniteloop.cyclefollower.domain.PhaseGuides
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BriefingTest {

    private val day1 = LocalDate.of(2025, 3, 1)

    private fun profile(
        contraception: Contraception = Contraception.NONE,
        pms: PmsSeverity = PmsSeverity.MODERATE,
        symptoms: Set<String> = emptySet(),
    ) = UserProfile(
        periodStarts = listOf(day1),
        statedCycleLength = 28,
        periodLength = 5,
        contraception = contraception,
        pmsSeverity = pms,
        symptoms = symptoms,
        setupComplete = true,
    )

    @Test
    fun `an empty profile asks to be set up`() {
        val briefing = Briefings.build(UserProfile(), null, day1)
        assertTrue(briefing.needsSetup)
    }

    @Test
    fun `banner stays short enough for a notification and a widget`() {
        for (dayOffset in 0..40) {
            for (pms in PmsSeverity.entries) {
                for (contraception in Contraception.entries) {
                    val p = profile(contraception = contraception, pms = pms)
                    val today = day1.plusDays(dayOffset.toLong())
                    val status = CycleEngine.status(p, today)
                    val briefing = Briefings.build(p, status, today)
                    assertTrue(
                        "banner too long (${briefing.moodBanner.length}): ${briefing.moodBanner}",
                        briefing.moodBanner.length <= 150,
                    )
                    assertTrue(briefing.moodBanner.isNotBlank())
                    assertTrue(briefing.dayLabel.isNotBlank())
                }
            }
        }
    }

    @Test
    fun `her own symptoms are preferred over the generic list`() {
        val p = profile(symptoms = setOf(Symptom.CRAMPS.name, Symptom.HIGH_LIBIDO.name))
        val onPeriod = Briefings.build(p, CycleEngine.status(p, day1), day1)
        assertEquals(listOf(Symptom.CRAMPS), onPeriod.likelySymptoms)

        val atOvulation = day1.plusDays(13)
        val ovulating = Briefings.build(p, CycleEngine.status(p, atOvulation), atOvulation)
        assertEquals(listOf(Symptom.HIGH_LIBIDO), ovulating.likelySymptoms)
    }

    @Test
    fun `a late period gets a warning rather than a silent wrong answer`() {
        val today = day1.plusDays(36)
        val status = CycleEngine.status(profile(), today)!!
        val briefing = Briefings.build(profile(), status, today)
        assertNotNull(briefing.warning)
        assertTrue(briefing.moodBanner.contains("late"))
    }

    @Test
    fun `suppressed methods never mention ovulation timing`() {
        val p = profile(contraception = Contraception.IMPLANT)
        val today = day1.plusDays(14)
        val briefing = Briefings.build(p, CycleEngine.status(p, today), today)
        assertFalse(briefing.timingLine.orEmpty().contains("Ovulation"))
    }

    @Test
    fun `timing line reads as a sentence in every phase of the cycle`() {
        // Caught "Ovulation around in 2 days (Sun 23 Aug)" -- dateWords already supplies the
        // "in N days" wording, so any extra preposition in front of it reads as broken English.
        val p = profile()
        for (dayOffset in 0..40) {
            val today = day1.plusDays(dayOffset.toLong())
            val line = Briefings.build(p, CycleEngine.status(p, today), today).timingLine.orEmpty()
            for (bad in listOf("around in ", "around today", "around tomorrow", "around yesterday", "  ")) {
                assertFalse("bad phrasing '$bad' in: $line", line.contains(bad))
            }
        }
    }

    @Test
    fun `the banner does not repeat what is already shown around it`() {
        // The Today card stacks: "Day 26 of 28 - <phase title>", then the banner, then the timing
        // line. Repeating the phase name or the period countdown in the banner produced three
        // near-identical sentences in a row, which is what this guards against.
        for (dayOffset in 0..40) {
            for (pms in PmsSeverity.entries) {
                val p = profile(pms = pms)
                val today = day1.plusDays(dayOffset.toLong())
                val status = CycleEngine.status(p, today)
                val b = Briefings.build(p, status, today)
                if (b.needsSetup || status == null) continue

                val title = PhaseGuides.of(status.phase).title
                assertFalse(
                    "banner repeats the phase title '$title': ${b.moodBanner}",
                    b.moodBanner.contains(title, ignoreCase = true),
                )
                if (b.timingLine?.contains("expected in") == true && !status.isLate) {
                    assertFalse(
                        "banner repeats the period countdown: ${b.moodBanner}",
                        b.moodBanner.contains("due in") || b.moodBanner.contains("expected in"),
                    )
                }
            }
        }
    }

    @Test
    fun `phase titles stay short enough for a banner line and a notification title`() {
        CyclePhase.entries.forEach { phase ->
            val title = PhaseGuides.of(phase).title
            assertTrue("phase title too long to sit on one line: '$title'", title.length <= 24)
        }
    }

    @Test
    fun `every phase has a guide and a one liner`() {
        CyclePhase.entries.forEach { phase ->
            val guide = PhaseGuides.of(phase)
            assertTrue(guide.oneLiner.isNotBlank())
            assertTrue(guide.doThis.isNotEmpty())
            assertTrue(guide.avoidThis.isNotEmpty())
            assertTrue(guide.physical.isNotEmpty())
            assertTrue(guide.emotional.isNotEmpty())
            assertTrue("no intimacy copy for $phase", guide.intimacy.isNotBlank())
        }
    }

    @Test
    fun `every article is complete and reachable by id`() {
        assertTrue(Library.articles.isNotEmpty())
        val ids = Library.articles.map { it.id }
        assertEquals("duplicate article ids", ids.size, ids.distinct().size)
        Library.articles.forEach { article ->
            assertEquals(article, Library.byId(article.id))
            assertTrue("empty article ${article.id}", article.blocks.isNotEmpty())
            assertTrue(article.title.isNotBlank() && article.subtitle.isNotBlank())
            article.blocks.forEach { block ->
                when (block) {
                    is Block.Head -> assertTrue(block.text.isNotBlank())
                    is Block.Para -> assertTrue(block.text.isNotBlank())
                    is Block.Note -> assertTrue(block.text.isNotBlank())
                    is Block.Bullets -> assertTrue(block.items.isNotEmpty())
                    is Block.Numbered -> assertTrue(block.items.isNotEmpty())
                }
            }
        }
    }
}
