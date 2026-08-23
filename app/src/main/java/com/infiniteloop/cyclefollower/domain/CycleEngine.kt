package com.infiniteloop.cyclefollower.domain

import com.infiniteloop.cyclefollower.data.Contraception
import com.infiniteloop.cyclefollower.data.Suppression
import com.infiniteloop.cyclefollower.data.UserProfile
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** One phase and the cycle days it covers (both ends inclusive, 1-based). */
data class PhaseSpan(val phase: CyclePhase, val startDay: Int, val endDay: Int) {
    val length: Int get() = endDay - startDay + 1
    operator fun contains(day: Int): Boolean = day in startDay..endDay
}

data class CycleStatus(
    val today: LocalDate,
    val anchor: LocalDate,
    val cycleDay: Int,
    val cycleLength: Int,
    val variabilityDays: Int,
    val cyclesTracked: Int,
    val timeline: List<PhaseSpan>,
    val currentSpan: PhaseSpan,
    val nextPeriodStart: LocalDate,
    /** Negative once the period is overdue. */
    val daysUntilNextPeriod: Int,
    val daysLate: Int,
    val periodLength: Int,
    val ovulationDate: LocalDate?,
    val ovulationCycleDay: Int?,
    val fertileWindow: Pair<LocalDate, LocalDate>?,
    val inFertileWindow: Boolean,
    val ovulationSuppressed: Boolean,
    val ovulationUncertain: Boolean,
    val bleedingPredictable: Boolean,
    val confidence: Confidence,
    /** True when the last logged period is so old the prediction has stopped meaning anything. */
    val dataIsStale: Boolean,
) {
    val phase: CyclePhase get() = currentSpan.phase
    val dayInPhase: Int get() = cycleDay - currentSpan.startDay + 1
    val phaseLength: Int get() = currentSpan.length
    val daysLeftInPhase: Int get() = (currentSpan.endDay - cycleDay).coerceAtLeast(0)
    val isLate: Boolean get() = daysLate > 0

    /** Days from today until the estimated ovulation date; negative once it has passed. */
    val daysUntilOvulation: Int
        get() = ovulationDate?.let { ChronoUnit.DAYS.between(today, it).toInt() } ?: Int.MIN_VALUE
    val isBleeding: Boolean get() = phase == CyclePhase.MENSTRUAL
    val progress: Float get() = (cycleDay.toFloat() / cycleLength.toFloat()).coerceIn(0f, 1f)

    /** Predicted period start, widened by how much her cycles actually vary. */
    val nextPeriodWindow: Pair<LocalDate, LocalDate>
        get() = nextPeriodStart.minusDays(variabilityDays.toLong()) to
            nextPeriodStart.plusDays(variabilityDays.toLong())

    fun dateOfCycleDay(day: Int): LocalDate = anchor.plusDays((day - 1).toLong())

    fun spanFor(day: Int): PhaseSpan =
        timeline.firstOrNull { day in it } ?: if (day < 1) timeline.first() else timeline.last()
}

object CycleEngine {

    /** Below this the logged gap is treated as a mis-entry rather than a real cycle. */
    private const val MIN_PLAUSIBLE_GAP = 15
    private const val MAX_PLAUSIBLE_GAP = 60

    /**
     * Returns null when there is nothing to count from -- no logged period, or every logged date
     * is in the future. The UI treats that as "finish setting me up" rather than guessing.
     */
    fun status(profile: UserProfile, today: LocalDate = LocalDate.now()): CycleStatus? {
        val anchor = profile.periodStarts.filter { !it.isAfter(today) }.maxOrNull() ?: return null

        val gaps = plausibleGaps(profile)
        val cycleLength = effectiveCycleLength(profile, gaps)
        val variability = variability(gaps)
        val cycleDay = ChronoUnit.DAYS.between(anchor, today).toInt() + 1

        val periodLength = profile.periodLength.coerceIn(1, maxOf(1, minOf(10, cycleLength / 2)))
        val suppression = profile.contraception.suppression

        val ovulationCycleDay = if (suppression == Suppression.FULL) {
            null
        } else {
            (cycleLength - UserProfile.LUTEAL_PHASE_LENGTH).coerceAtLeast(6)
        }

        val pmsWindow = pmsWindow(profile, cycleLength)
        val timeline = buildTimeline(
            contraception = profile.contraception,
            cycleLength = cycleLength,
            periodLength = periodLength,
            ovulationCycleDay = ovulationCycleDay,
            pmsWindow = pmsWindow,
        )

        val nextPeriodStart = anchor.plusDays(cycleLength.toLong())
        val daysUntilNextPeriod = ChronoUnit.DAYS.between(today, nextPeriodStart).toInt()
        val daysLate = (-daysUntilNextPeriod).coerceAtLeast(0)

        val ovulationDate = ovulationCycleDay?.let { anchor.plusDays((it - 1).toLong()) }
        val fertileWindow = ovulationDate?.let {
            it.minusDays(UserProfile.FERTILE_DAYS_BEFORE_OVULATION.toLong()) to
                it.plusDays(UserProfile.FERTILE_DAYS_AFTER_OVULATION.toLong())
        }
        val inFertileWindow = fertileWindow != null &&
            !today.isBefore(fertileWindow.first) && !today.isAfter(fertileWindow.second)

        return CycleStatus(
            today = today,
            anchor = anchor,
            cycleDay = cycleDay,
            cycleLength = cycleLength,
            variabilityDays = variability,
            cyclesTracked = gaps.size,
            timeline = timeline,
            currentSpan = timeline.firstOrNull { cycleDay in it } ?: timeline.last(),
            nextPeriodStart = nextPeriodStart,
            daysUntilNextPeriod = daysUntilNextPeriod,
            daysLate = daysLate,
            periodLength = periodLength,
            ovulationDate = ovulationDate,
            ovulationCycleDay = ovulationCycleDay,
            fertileWindow = fertileWindow,
            inFertileWindow = inFertileWindow,
            ovulationSuppressed = suppression == Suppression.FULL,
            ovulationUncertain = suppression == Suppression.PARTIAL,
            bleedingPredictable = !profile.contraception.bleedingUnpredictable,
            confidence = confidence(profile, gaps, variability),
            dataIsStale = cycleDay > cycleLength * 2 && cycleDay > 60,
        )
    }

    /** Days between consecutive logged periods, with obvious mis-entries dropped. */
    fun plausibleGaps(profile: UserProfile): List<Int> {
        val starts = profile.periodStarts.distinct().sortedDescending().take(13)
        return starts.zipWithNext { newer, older ->
            ChronoUnit.DAYS.between(older, newer).toInt()
        }.filter { it in MIN_PLAUSIBLE_GAP..MAX_PLAUSIBLE_GAP }
    }

    fun effectiveCycleLength(profile: UserProfile, gaps: List<Int> = plausibleGaps(profile)): Int {
        if (!profile.useHistoryAverage || gaps.isEmpty()) {
            return profile.statedCycleLength.coerceIn(UserProfile.MIN_CYCLE_LENGTH, UserProfile.MAX_CYCLE_LENGTH)
        }
        // Only the six most recent cycles: a cycle length from two years ago is not informative.
        val recent = gaps.take(6)
        return recent.average().roundToInt()
            .coerceIn(UserProfile.MIN_CYCLE_LENGTH, UserProfile.MAX_CYCLE_LENGTH)
    }

    /** Standard deviation of recent cycle lengths, rounded up. Reported to the user as "+/- n days". */
    fun variability(gaps: List<Int>): Int {
        val recent = gaps.take(6)
        if (recent.size < 2) return 0
        val mean = recent.average()
        val variance = recent.sumOf { (it - mean) * (it - mean) } / recent.size
        return ceil(sqrt(variance)).toInt().coerceIn(0, 12)
    }

    /** How many days before the period the app should start flagging the PMS window. */
    /**
     * The window implied by the severity setting alone, before any logged observation.
     *
     * Kept separate from [pmsWindow] on purpose: Personalisation needs to know what the app would
     * have assumed WITHOUT the logs, and calling pmsWindow for that would recurse straight back
     * into Personalisation.
     */
    fun configuredPmsWindow(profile: UserProfile, cycleLength: Int): Int {
        if (!profile.contraception.hasNaturalCycle) return 0
        val base = profile.pmsSeverity.windowDays
        val withPmdd = if (profile.pmdd) base + 3 else base
        // Never let the PMS window swallow more than half the cycle.
        return withPmdd.coerceIn(0, cycleLength / 2)
    }

    fun pmsWindow(profile: UserProfile, cycleLength: Int): Int {
        if (!profile.contraception.hasNaturalCycle) return 0

        // Logged days beat the setting: they are his own observations of her, and more recent.
        val personal = Personalisation.of(profile)
        val observed = personal.observedPmsStartDay
        if (personal.hasEnoughData && observed != null) {
            return (cycleLength - observed + 1).coerceIn(0, cycleLength / 2)
        }
        return configuredPmsWindow(profile, cycleLength)
    }

    fun buildTimeline(
        contraception: Contraception,
        cycleLength: Int,
        periodLength: Int,
        ovulationCycleDay: Int?,
        pmsWindow: Int,
    ): List<PhaseSpan> {
        val spans = mutableListOf<PhaseSpan>()

        if (contraception.suppression == Suppression.FULL) {
            // No natural cycle underneath: describe the pack, not the phases.
            if (!contraception.bleedingUnpredictable) {
                spans += PhaseSpan(CyclePhase.MENSTRUAL, 1, periodLength)
                val breakEnd = maxOf(periodLength, 7)
                if (breakEnd > periodLength) {
                    spans += PhaseSpan(CyclePhase.HORMONE_BREAK, periodLength + 1, breakEnd)
                }
                if (cycleLength > breakEnd) {
                    spans += PhaseSpan(CyclePhase.STEADY_STATE, breakEnd + 1, cycleLength)
                }
            } else {
                // Implant, injection, continuous pill: bleeding is incidental, hormones are flat.
                spans += PhaseSpan(CyclePhase.MENSTRUAL, 1, periodLength)
                spans += PhaseSpan(CyclePhase.STEADY_STATE, periodLength + 1, maxOf(cycleLength, periodLength + 1))
            }
            return spans.normalise(cycleLength)
        }

        spans += PhaseSpan(CyclePhase.MENSTRUAL, 1, periodLength)
        var cursor = periodLength + 1

        if (ovulationCycleDay != null) {
            val fertileStart = maxOf(cursor, ovulationCycleDay - UserProfile.FERTILE_DAYS_BEFORE_OVULATION)
            val ovulationStart = ovulationCycleDay - 1
            val ovulationEnd = ovulationCycleDay + UserProfile.FERTILE_DAYS_AFTER_OVULATION

            if (fertileStart > cursor) {
                spans += PhaseSpan(CyclePhase.FOLLICULAR, cursor, fertileStart - 1)
                cursor = fertileStart
            }
            if (ovulationStart > cursor) {
                spans += PhaseSpan(CyclePhase.FERTILE_WINDOW, cursor, ovulationStart - 1)
                cursor = ovulationStart
            }
            if (ovulationEnd >= cursor) {
                spans += PhaseSpan(CyclePhase.OVULATION, cursor, ovulationEnd)
                cursor = ovulationEnd + 1
            }
        }

        if (pmsWindow > 0) {
            val pmsStart = maxOf(cursor, cycleLength - pmsWindow + 1)
            if (pmsStart > cursor) {
                spans += PhaseSpan(CyclePhase.EARLY_LUTEAL, cursor, pmsStart - 1)
            }
            spans += PhaseSpan(CyclePhase.LATE_LUTEAL, pmsStart, maxOf(cycleLength, pmsStart))
        } else {
            spans += PhaseSpan(CyclePhase.EARLY_LUTEAL, cursor, maxOf(cycleLength, cursor))
        }

        return spans.normalise(cycleLength)
    }

    /** Drop empty spans and make sure the last one runs to the end of the cycle. */
    private fun List<PhaseSpan>.normalise(cycleLength: Int): List<PhaseSpan> {
        val kept = filter { it.endDay >= it.startDay }
        if (kept.isEmpty()) return listOf(PhaseSpan(CyclePhase.MENSTRUAL, 1, cycleLength))
        val last = kept.last()
        return if (last.endDay >= cycleLength) kept
        else kept.dropLast(1) + last.copy(endDay = cycleLength)
    }


    /** What a given calendar date looked like (or is predicted to look like). */
    data class DayInfo(val date: LocalDate, val cycleDay: Int, val phase: CyclePhase, val predicted: Boolean)

    /**
     * Phase for any date, past or future.
     *
     * Past cycles are drawn using their real measured length -- the gap between the two logged
     * periods that bracket them -- so history is shown as it actually happened rather than as the
     * average would have it. Future dates repeat the predicted cycle forward.
     * Returns null before the first logged period, where there is genuinely nothing to say.
     */
    /** Where a date falls in the cycle, with no phase logic and so no dependency on the timeline. */
    data class DayPosition(val cycleDay: Int, val cycleLength: Int, val predicted: Boolean)

    fun positionOf(profile: UserProfile, date: LocalDate): DayPosition? {
        val starts = profile.periodStarts.distinct().sorted()
        val anchor = starts.lastOrNull { !it.isAfter(date) } ?: return null
        val nextLogged = starts.firstOrNull { it.isAfter(anchor) }

        val predictedLength = effectiveCycleLength(profile)
        var dayNumber = ChronoUnit.DAYS.between(anchor, date).toInt() + 1
        val measuredLength = nextLogged?.let { ChronoUnit.DAYS.between(anchor, it).toInt() }

        return if (measuredLength != null && dayNumber <= measuredLength) {
            DayPosition(
                dayNumber,
                measuredLength.coerceIn(UserProfile.MIN_CYCLE_LENGTH, UserProfile.MAX_CYCLE_LENGTH),
                predicted = false,
            )
        } else {
            // Roll forward through projected cycles for dates beyond the last logged period.
            while (dayNumber > predictedLength) dayNumber -= predictedLength
            DayPosition(dayNumber, predictedLength, predicted = true)
        }
    }

    fun dayInfo(profile: UserProfile, date: LocalDate): DayInfo? {
        val position = positionOf(profile, date) ?: return null
        val dayNumber = position.cycleDay
        val length = position.cycleLength
        val predicted = position.predicted
        val periodLength = profile.periodLength.coerceIn(1, maxOf(1, minOf(10, length / 2)))

        val ovulationCycleDay = if (profile.contraception.suppression == Suppression.FULL) null
        else (length - UserProfile.LUTEAL_PHASE_LENGTH).coerceAtLeast(6)

        val timeline = buildTimeline(
            contraception = profile.contraception,
            cycleLength = length,
            periodLength = periodLength.coerceAtMost(maxOf(1, length / 2)),
            ovulationCycleDay = ovulationCycleDay,
            pmsWindow = pmsWindow(profile, length),
        )
        val span = timeline.firstOrNull { dayNumber in it } ?: timeline.last()
        return DayInfo(date, dayNumber, span.phase, predicted)
    }

    fun confidence(profile: UserProfile, gaps: List<Int>, variability: Int): Confidence = when {
        profile.contraception.suppression == Suppression.FULL -> Confidence.SUPPRESSED
        profile.contraception.suppression == Suppression.PARTIAL -> Confidence.LOW
        gaps.size >= 3 && variability <= 2 -> Confidence.HIGH
        gaps.size >= 1 && variability <= 4 -> Confidence.MEDIUM
        gaps.isEmpty() -> Confidence.MEDIUM
        else -> Confidence.LOW
    }
}
