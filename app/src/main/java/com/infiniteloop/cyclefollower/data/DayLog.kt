package com.infiniteloop.cyclefollower.data

import kotlinx.serialization.Serializable
import java.time.LocalDate

/**
 * How the day actually went, as observed by the partner.
 *
 * She never sees this app and logs nothing in it, so these taps are the only measurement the app
 * ever gets. Everything personalised is built from them.
 */
enum class DayMood(val label: String, val score: Int) {
    ROUGH("Rough", 0),
    NORMAL("Normal", 1),
    GOOD("Good", 2),
}

@Serializable
data class DayLog(
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
    val mood: DayMood,
    /** Names of [Symptom] entries actually noticed that day. */
    val tags: Set<String> = emptySet(),
) {
    val observedSymptoms: List<Symptom>
        get() = tags.mapNotNull { name -> Symptom.entries.firstOrNull { it.name == name } }
}
