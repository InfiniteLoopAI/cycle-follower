package com.infiniteloop.cyclefollower.domain

import com.infiniteloop.cyclefollower.data.DayMood
import com.infiniteloop.cyclefollower.data.Symptom
import com.infiniteloop.cyclefollower.data.UserProfile
import kotlin.math.roundToInt

/**
 * What the logged days say about her, as opposed to what the textbook says.
 *
 * She logs nothing, so this is built entirely from the partner's daily taps. Each log is placed on
 * the cycle day it fell on, then averaged across cycles -- so "her hard days start on day 23" is a
 * statement about her, not about an average woman.
 */
data class Personalisation(
    val daysLogged: Int,
    val cyclesCovered: Int,
    /** Cycle day her rough stretch reliably begins, when the logs support one. */
    val observedPmsStartDay: Int?,
    /** The single roughest cycle day on record. */
    val roughestDay: Int?,
    /** Symptoms logged often enough to be worth mentioning, most frequent first. */
    val frequentTags: List<Symptom>,
    val insights: List<String>,
) {
    val hasEnoughData: Boolean get() = cyclesCovered >= MIN_CYCLES && daysLogged >= MIN_DAYS

    companion object {
        /** Below this the pattern is noise, and overriding the textbook would be worse than not. */
        const val MIN_CYCLES = 2
        const val MIN_DAYS = 24

        /** Mean mood at or below this counts as a rough day (0 rough, 1 normal, 2 good). */
        const val ROUGH_THRESHOLD = 0.75

        @Volatile private var cacheKey: UserProfile? = null
        @Volatile private var cacheValue: Personalisation? = null

        fun of(profile: UserProfile): Personalisation {
            cacheValue?.let { if (cacheKey == profile) return it }
            return compute(profile).also { cacheKey = profile; cacheValue = it }
        }

        private fun compute(profile: UserProfile): Personalisation {
            val placed = profile.dayLogs.mapNotNull { log ->
                CycleEngine.positionOf(profile, log.date)?.let { it.cycleDay to log }
            }
            if (placed.isEmpty()) {
                return Personalisation(0, 0, null, null, emptyList(), emptyList())
            }

            val cycleLength = CycleEngine.effectiveCycleLength(profile)
            val byDay: Map<Int, List<DayMood>> = placed
                .groupBy({ it.first }, { it.second.mood })
            val meanByDay = byDay.mapValues { (_, moods) -> moods.sumOf { it.score }.toDouble() / moods.size }

            // A "cycle covered" is one with enough logged days to say anything about it.
            val cyclesCovered = profile.periodStarts.count { start ->
                profile.dayLogs.count { it.date >= start && it.date < start.plusDays(cycleLength.toLong()) } >= 8
            }

            val enough = cyclesCovered >= MIN_CYCLES && placed.size >= MIN_DAYS

            // Look only in the back half: a rough patch on day 4 is the period, not PMS.
            val lateWindow = (cycleLength / 2 + 1)..cycleLength
            val observedPmsStartDay = if (!enough) null else {
                lateWindow.firstOrNull { day ->
                    val here = meanByDay[day] ?: return@firstOrNull false
                    val next = meanByDay[day + 1]
                    // A single rough day is a bad day; a rough day followed by another is a pattern.
                    here <= ROUGH_THRESHOLD && (next == null || next <= ROUGH_THRESHOLD)
                }
            }

            val roughestDay = meanByDay.entries
                .filter { byDay.getValue(it.key).size >= 2 }
                .minByOrNull { it.value }
                ?.takeIf { it.value <= ROUGH_THRESHOLD }
                ?.key

            val tagCounts = profile.dayLogs
                .flatMap { it.observedSymptoms }
                .groupingBy { it }
                .eachCount()
            val frequentTags = tagCounts.entries
                .filter { it.value >= 3 }
                .sortedByDescending { it.value }
                .map { it.key }

            val insights = buildList {
                if (!enough) {
                    val need = (MIN_DAYS - placed.size).coerceAtLeast(0)
                    add(
                        if (need > 0) "$need more logged days and the app can start describing her rather than an average."
                        else "Log across one more cycle and the app can start describing her rather than an average.",
                    )
                } else {
                    val textbook = cycleLength - CycleEngine.configuredPmsWindow(profile, cycleLength) + 1
                    if (observedPmsStartDay != null && observedPmsStartDay != textbook) {
                        val dir = if (observedPmsStartDay < textbook) "earlier" else "later"
                        add(
                            "Her hard days start on day $observedPmsStartDay, not day $textbook - " +
                                "${kotlin.math.abs(observedPmsStartDay - textbook)} day(s) $dir than the default. " +
                                "The app has moved the window.",
                        )
                    } else if (observedPmsStartDay != null) {
                        add("Her rough stretch starts on day $observedPmsStartDay, which matches what the app already assumed.")
                    }
                    roughestDay?.let { add("Day $it is reliably her worst. Keep it clear if you can.") }
                    frequentTags.take(3).takeIf { it.isNotEmpty() }?.let { tags ->
                        add("Most often noticed: " + tags.joinToString(", ") { it.label.lowercase() } + ".")
                    }
                }
                add("${placed.size} days logged across ${plural(cyclesCovered, "cycle")}.")
            }

            return Personalisation(
                daysLogged = placed.size,
                cyclesCovered = cyclesCovered,
                observedPmsStartDay = observedPmsStartDay,
                roughestDay = roughestDay,
                frequentTags = frequentTags,
                insights = insights,
            )
        }

        private fun plural(n: Int, unit: String) = if (n == 1) "1 $unit" else "$n ${unit}s"

        /** Mean mood per cycle day, for the little chart on the log screen. */
        fun moodByCycleDay(profile: UserProfile): Map<Int, Double> {
            val placed = profile.dayLogs.mapNotNull { log ->
                CycleEngine.positionOf(profile, log.date)?.let { it.cycleDay to log.mood.score }
            }
            return placed.groupBy({ it.first }, { it.second })
                .mapValues { (_, xs) -> xs.average() }
        }

        fun roundedMean(values: Collection<Double>): Int =
            if (values.isEmpty()) 0 else values.average().roundToInt()
    }
}
