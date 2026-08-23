package com.infiniteloop.cyclefollower.domain

import com.infiniteloop.cyclefollower.data.UserProfile
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** How good a day is for asking something of her, or planning something with her. */
enum class Suitability(val label: String, val rank: Int) {
    BEST("Best", 3),
    OK("Fine", 2),
    POOR("Careful", 1),
    AVOID("Avoid", 0),
}

data class PlannedDay(val date: LocalDate, val phase: CyclePhase, val cycleDay: Int, val suitability: Suitability)

/** A run of consecutive days that all score the same. */
data class PlanWindow(
    val start: LocalDate,
    val end: LocalDate,
    val suitability: Suitability,
    val phase: CyclePhase,
) {
    val days: Int get() = ChronoUnit.DAYS.between(start, end).toInt() + 1
}

/**
 * Turns the phase timeline into an answer to the only planning question that matters:
 * which weeks to put things in, and which to keep clear.
 *
 * The app otherwise only ever talks about today, which is too late to be useful for a trip,
 * a visit from your parents, or a conversation you have been putting off.
 */
object Planner {

    const val HORIZON_DAYS = 120

    fun suitabilityOf(phase: CyclePhase, dayInPhase: Int): Suitability = when (phase) {
        // Days one and two are the worst of the month; the tail of the period is merely low.
        CyclePhase.MENSTRUAL -> if (dayInPhase <= 2) Suitability.AVOID else Suitability.POOR
        CyclePhase.FOLLICULAR -> Suitability.BEST
        CyclePhase.FERTILE_WINDOW -> Suitability.BEST
        CyclePhase.OVULATION -> Suitability.BEST
        CyclePhase.EARLY_LUTEAL -> Suitability.OK
        CyclePhase.LATE_LUTEAL -> Suitability.AVOID
        CyclePhase.HORMONE_BREAK -> Suitability.POOR
        CyclePhase.STEADY_STATE -> Suitability.OK
    }

    fun dayFor(profile: UserProfile, date: LocalDate): PlannedDay? {
        val info = CycleEngine.dayInfo(profile, date) ?: return null
        val span = spanContaining(profile, info)
        val dayInPhase = info.cycleDay - (span?.startDay ?: info.cycleDay) + 1
        return PlannedDay(date, info.phase, info.cycleDay, suitabilityOf(info.phase, dayInPhase))
    }

    private fun spanContaining(profile: UserProfile, info: CycleEngine.DayInfo): PhaseSpan? {
        val status = CycleEngine.status(profile, info.date) ?: return null
        return status.timeline.firstOrNull { info.cycleDay in it }
    }

    fun horizon(profile: UserProfile, from: LocalDate = LocalDate.now(), days: Int = HORIZON_DAYS): List<PlannedDay> =
        (0 until days).mapNotNull { dayFor(profile, from.plusDays(it.toLong())) }

    /** Consecutive same-scoring days collapsed into windows. */
    fun windows(profile: UserProfile, from: LocalDate = LocalDate.now(), days: Int = HORIZON_DAYS): List<PlanWindow> {
        val horizon = horizon(profile, from, days)
        if (horizon.isEmpty()) return emptyList()
        val out = mutableListOf<PlanWindow>()
        var start = horizon.first()
        var previous = horizon.first()
        horizon.drop(1).forEach { day ->
            val contiguous = day.date == previous.date.plusDays(1)
            if (!contiguous || day.suitability != start.suitability) {
                out += PlanWindow(start.date, previous.date, start.suitability, start.phase)
                start = day
            }
            previous = day
        }
        out += PlanWindow(start.date, previous.date, start.suitability, start.phase)
        return out
    }

    /** The good stretches worth putting something in, longest and soonest first. */
    fun goodWindows(profile: UserProfile, from: LocalDate = LocalDate.now(), limit: Int = 3): List<PlanWindow> =
        windows(profile, from)
            .filter { it.suitability == Suitability.BEST && it.days >= 3 }
            .take(limit)

    /** The stretches to keep clear. */
    fun avoidWindows(profile: UserProfile, from: LocalDate = LocalDate.now(), limit: Int = 3): List<PlanWindow> =
        windows(profile, from)
            .filter { it.suitability == Suitability.AVOID }
            .take(limit)

    /**
     * A better date near one that scores badly -- the actually useful half of "not that day".
     * Searches outward so the suggestion stays as close to the original plan as possible.
     */
    fun betterDateNear(
        profile: UserProfile,
        date: LocalDate,
        searchDays: Int = 21,
        // Explicit rather than reading the clock inside: suggesting a date in the past is useless,
        // but a hidden LocalDate.now() here silently returns nothing for any date not near today.
        notBefore: LocalDate = LocalDate.now(),
    ): LocalDate? {
        if (dayFor(profile, date)?.suitability == Suitability.BEST) return null
        for (offset in 1..searchDays) {
            listOf(date.plusDays(offset.toLong()), date.minusDays(offset.toLong()))
                .filter { !it.isBefore(notBefore) }
                .forEach { candidate ->
                    if (dayFor(profile, candidate)?.suitability == Suitability.BEST) return candidate
                }
        }
        return null
    }
}
