package com.infiniteloop.cyclefollower.domain

import com.infiniteloop.cyclefollower.data.PmsSeverity
import com.infiniteloop.cyclefollower.data.Symptom
import com.infiniteloop.cyclefollower.data.UserProfile
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Everything shown for "today", assembled once so the Today screen, the notification and the
 * widget can never disagree with each other.
 */
data class Briefing(
    val dayLabel: String,
    val phaseTitle: String,
    val emoji: String,
    /** Short enough for a notification banner or a widget line. */
    val moodBanner: String,
    val summary: String,
    val timingLine: String?,
    /** Very short countdown for the widget, where there is room for about four words. */
    val shortTiming: String?,
    val likelySymptoms: List<Symptom>,
    val doNow: List<String>,
    val avoidNow: List<String>,
    val needsSetup: Boolean = false,
    val warning: String? = null,
)

object Briefings {

    private val dayMonth: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.UK)

    fun build(profile: UserProfile, status: CycleStatus?, today: LocalDate = LocalDate.now()): Briefing {
        if (status == null) {
            return Briefing(
                dayLabel = "Not set up yet",
                phaseTitle = "Add her last period",
                emoji = "📅",
                moodBanner = "Add the start date of her most recent period to begin.",
                summary = "The app needs one date to work from: the first day of her most recent period. " +
                    "Everything else is optional and can be refined later.",
                timingLine = null,
                likelySymptoms = emptyList(),
                doNow = emptyList(),
                avoidNow = emptyList(),
                shortTiming = null,
                needsSetup = true,
            )
        }

        val guide = PhaseGuides.of(status.phase)
        val her = profile.selectedSymptoms.filter { status.phase in it.phases }
        val symptoms = her.ifEmpty { Symptom.forPhase(status.phase).take(3) }

        return Briefing(
            dayLabel = dayLabel(status),
            phaseTitle = guide.title,
            emoji = guide.emoji,
            moodBanner = moodBanner(profile, status),
            summary = summary(profile, status, guide),
            timingLine = timingLine(profile, status, today),
            shortTiming = shortTiming(profile, status),
            likelySymptoms = symptoms,
            doNow = guide.doThis.take(3),
            avoidNow = guide.avoidThis.take(2),
            warning = warning(profile, status),
        )
    }

    /**
     * Four words at most. The widget is glanced at, not read, and the countdown to the next
     * period is the single most useful thing to put in front of someone.
     */
    fun shortTiming(profile: UserProfile, status: CycleStatus): String? {
        if (status.dataIsStale) return "Log her latest period"
        if (!status.bleedingPredictable) return null
        val bleed = if (profile.contraception.bleedIsWithdrawal) "Bleed" else "Period"
        return when {
            status.isLate -> "$bleed ${plural(status.daysLate, "day")} late"
            status.phase == CyclePhase.MENSTRUAL -> "Day ${status.dayInPhase} of bleeding"
            status.daysUntilNextPeriod == 0 -> "$bleed due today"
            status.ovulationDate != null && status.daysUntilOvulation in 0..3 ->
                if (status.daysUntilOvulation == 0) "Ovulating today"
                else "Ovulation in ${plural(status.daysUntilOvulation, "day")}"
            else -> "$bleed in ${plural(status.daysUntilNextPeriod, "day")}"
        }
    }

    fun dayLabel(status: CycleStatus): String = when {
        status.dataIsStale -> "Day ${status.cycleDay} - out of date"
        status.isLate -> "Day ${status.cycleDay} - ${plural(status.daysLate, "day")} late"
        else -> "Day ${status.cycleDay} of ${status.cycleLength}"
    }

    /** Kept under about 110 characters so it survives a notification banner and a widget row. */
    fun moodBanner(profile: UserProfile, status: CycleStatus): String {
        if (status.dataIsStale) return "The last logged period is very old - log her latest one to get this working again."
        if (status.isLate && status.bleedingPredictable) {
            return "Period is ${plural(status.daysLate, "day")} late. Late cycles are common; it is not automatically a sign of anything."
        }
        return when (status.phase) {
            CyclePhase.MENSTRUAL ->
                if (status.dayInPhase <= 2) "Heaviest, most painful days. Low energy and low tolerance for plans."
                else "Bleeding, but past the worst. Energy starting to come back."
            CyclePhase.FOLLICULAR -> "Energy and mood climbing. Open, sociable, up for things."
            CyclePhase.FERTILE_WINDOW -> "Confident and outgoing, sex drive climbing. Fertile days."
            CyclePhase.OVULATION -> "Peak confidence and desire. Most fertile day of her month."
            CyclePhase.EARLY_LUTEAL -> "Calm, cosy, a little sleepy. Home beats going out."
            CyclePhase.LATE_LUTEAL -> pmsBanner(profile, status)
            CyclePhase.HORMONE_BREAK -> "Break week. Headaches and a few flat days are common."
            CyclePhase.STEADY_STATE -> "No natural cycle running. Take today's mood at face value."
        }
    }

    /**
     * Deliberately does not name the phase or count down to the period: the phase title sits
     * directly above this line and [timingLine] sits directly below it, so repeating either
     * produced three near-identical sentences stacked on top of each other.
     */
    private fun pmsBanner(profile: UserProfile, status: CycleStatus): String = when {
        profile.pmdd -> "PMDD days. This can be genuinely severe - lower every demand you can."
        profile.pmsSeverity == PmsSeverity.SEVERE -> "Deep in it. Patience at its lowest point of the month."
        profile.pmsSeverity == PmsSeverity.NONE -> "Premenstrual, though she usually sails through these days."
        profile.pmsSeverity == PmsSeverity.MILD -> "Mildly premenstrual. Slightly shorter fuse than usual."
        else -> "Shorter fuse and lower energy. Reduce friction today, and do not add any."
    }

    private fun summary(profile: UserProfile, status: CycleStatus, guide: PhaseGuide): String {
        val name = profile.partnerName.trim().ifEmpty { "She" }
        val subject = if (name == "She") "She" else name
        val base = guide.oneLiner
        val phasePosition = when {
            status.phaseLength <= 1 -> ""
            status.dayInPhase == 1 -> " This is the first day of it."
            status.daysLeftInPhase == 0 -> " Today is the last day of it."
            else -> " Day ${status.dayInPhase} of about ${status.phaseLength} in this phase."
        }
        return "$subject is in the ${guide.title.lowercase(Locale.UK)}. $base$phasePosition"
    }

    private fun timingLine(profile: UserProfile, status: CycleStatus, today: LocalDate): String? {
        if (status.dataIsStale) return null
        if (!status.bleedingPredictable) {
            return "On ${profile.contraception.shortLabel.lowercase(Locale.UK)}, bleeding is not on a predictable schedule."
        }
        val bleedWord = if (profile.contraception.bleedIsWithdrawal) "Withdrawal bleed" else "Period"
        return when {
            status.isLate -> "$bleedWord was expected ${dateWords(status.nextPeriodStart, today)}."
            status.daysUntilNextPeriod == 0 -> "$bleedWord expected today."
            status.ovulationDate != null && status.ovulationDate.isAfter(today) ->
                // dateWords() already supplies "in 2 days (Sun 23 Aug)", so no "around" prefix here.
                "Ovulation ${dateWords(status.ovulationDate, today)}. " +
                    "$bleedWord expected ${dateWords(status.nextPeriodStart, today)}."
            else -> "$bleedWord expected ${dateWords(status.nextPeriodStart, today)}."
        }
    }

    private fun warning(profile: UserProfile, status: CycleStatus): String? = when {
        status.dataIsStale ->
            "The last period logged was ${status.cycleDay} days ago, so these predictions have stopped meaning anything. " +
                "Log her most recent period to fix it."
        status.isLate && status.daysLate >= 7 && profile.contraception.hasNaturalCycle ->
            "The period is over a week late. Stress, illness, travel, big changes in weight or training can all delay " +
                "a cycle - and so can pregnancy. A test is the only thing that answers that question, and it is her call."
        status.ovulationUncertain ->
            "On ${profile.contraception.shortLabel.lowercase(Locale.UK)} she may or may not ovulate in any given cycle, " +
                "so treat the ovulation and fertile-window days as a rough guess rather than a fact."
        else -> null
    }

    fun dateWords(date: LocalDate, today: LocalDate): String {
        val days = java.time.temporal.ChronoUnit.DAYS.between(today, date).toInt()
        val stamp = "${date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.UK)} ${date.format(dayMonth)}"
        return when {
            days == 0 -> "today ($stamp)"
            days == 1 -> "tomorrow ($stamp)"
            days == -1 -> "yesterday ($stamp)"
            days > 1 -> "in ${plural(days, "day")} ($stamp)"
            else -> "${plural(-days, "day")} ago ($stamp)"
        }
    }

    fun plural(count: Int, unit: String): String = if (count == 1) "1 $unit" else "$count ${unit}s"
}
