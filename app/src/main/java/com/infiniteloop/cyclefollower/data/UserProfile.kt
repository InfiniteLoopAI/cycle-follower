package com.infiniteloop.cyclefollower.data

import kotlinx.serialization.Serializable
import java.time.LocalDate

/**
 * Everything the app knows. It never leaves the phone -- there is no internet permission in the
 * manifest, so the app physically cannot send this anywhere.
 */
@Serializable
data class UserProfile(
    /** Optional, only used to make the wording feel less clinical. */
    val partnerName: String = "",

    /**
     * Start dates of logged periods (day 1 of real flow), newest first.
     * The newest entry is the anchor every prediction counts from; the older entries are what
     * let the app work out her real average cycle length and how much it varies.
     */
    val periodStarts: List<@Serializable(with = LocalDateSerializer::class) LocalDate> = emptyList(),

    /** Fallback average used until there are at least two logged periods. */
    val statedCycleLength: Int = 28,

    /** How many days she actually bleeds. */
    val periodLength: Int = 5,

    val contraception: Contraception = Contraception.NONE,

    val pmsSeverity: PmsSeverity = PmsSeverity.MODERATE,

    /** Diagnosed PMDD widens and deepens the premenstrual window considerably. */
    val pmdd: Boolean = false,

    /** Names of the [Symptom] entries she actually gets. */
    val symptoms: Set<String> = emptySet(),

    /** Use the logged history to compute the average rather than the stated number. */
    val useHistoryAverage: Boolean = true,

    val setupComplete: Boolean = false,

    val dailyNotification: Boolean = true,
    val notificationHour: Int = 8,
    val notificationMinute: Int = 0,

    /**
     * Keeps the notification and widget text vague ("Day 24 - check the app") so nothing
     * personal shows on a lock screen someone else might glance at.
     */
    val discreetMode: Boolean = false,
) {
    val lastPeriodStart: LocalDate? get() = periodStarts.maxOrNull()

    val selectedSymptoms: List<Symptom>
        get() = symptoms.mapNotNull { name -> Symptom.entries.firstOrNull { it.name == name } }

    fun hasSymptom(symptom: Symptom): Boolean = symptom.name in symptoms

    /** Sorted newest first, de-duplicated. */
    fun normalised(): UserProfile = copy(
        periodStarts = periodStarts.distinct().sortedDescending(),
        statedCycleLength = statedCycleLength.coerceIn(MIN_CYCLE_LENGTH, MAX_CYCLE_LENGTH),
        periodLength = periodLength.coerceIn(1, 12),
        notificationHour = notificationHour.coerceIn(0, 23),
        notificationMinute = notificationMinute.coerceIn(0, 59),
    )

    fun withPeriodStart(date: LocalDate): UserProfile =
        copy(periodStarts = (periodStarts + date).distinct().sortedDescending()).normalised()

    fun withoutPeriodStart(date: LocalDate): UserProfile =
        copy(periodStarts = periodStarts.filterNot { it == date })

    companion object {
        const val MIN_CYCLE_LENGTH = 20
        const val MAX_CYCLE_LENGTH = 45
        const val DEFAULT_CYCLE_LENGTH = 28

        /**
         * The luteal phase -- ovulation to the next period -- is the stable part of the cycle,
         * usually 12 to 14 days whatever the total length. Counting ovulation backwards from the
         * next period is far more accurate than the "day 14" rule, which is only right for a
         * textbook 28-day cycle.
         */
        const val LUTEAL_PHASE_LENGTH = 14

        /** Sperm survive up to about five days; the egg lives less than a day after release. */
        const val FERTILE_DAYS_BEFORE_OVULATION = 5
        const val FERTILE_DAYS_AFTER_OVULATION = 1
    }
}
