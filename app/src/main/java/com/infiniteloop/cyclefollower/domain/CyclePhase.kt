package com.infiniteloop.cyclefollower.domain

/**
 * The phases the app can report.
 *
 * The first six describe a natural (non-suppressed) cycle. The last two only appear when a
 * hormonal method is switching ovulation off -- in that situation talking about a "follicular
 * phase" or a "fertile window" would simply be wrong, so the app says something different.
 */
enum class CyclePhase(val displayName: String, val shortName: String) {
    /** Bleeding days. Day 1 of the cycle is the first day of real flow. */
    MENSTRUAL("Period", "Period"),

    /** After bleeding, before the fertile window opens. Estrogen climbing. */
    FOLLICULAR("Follicular phase", "Rising"),

    /** The five days before ovulation, when sex could lead to pregnancy. */
    FERTILE_WINDOW("Fertile window", "Fertile"),

    /** Ovulation day, plus the day either side of it. */
    OVULATION("Ovulation", "Ovulation"),

    /** After ovulation: progesterone rising, usually the calmest stretch. */
    EARLY_LUTEAL("Early luteal phase", "Calm"),

    /** The PMS window: hormones falling away before the next period. */
    LATE_LUTEAL("Late luteal (PMS window)", "PMS"),

    /** The pill/patch/ring break week, once bleeding has stopped. Hormone withdrawal. */
    HORMONE_BREAK("Hormone-free break", "Break"),

    /** Active hormonal contraception: levels held flat, no natural cycle underneath. */
    STEADY_STATE("Steady hormone phase", "Steady");

    val isNaturalCycle: Boolean
        get() = this != HORMONE_BREAK && this != STEADY_STATE

    /**
     * Short enough for the middle of the ring, where the label is constrained to the hole.
     * [displayName] is too long there for the luteal phases.
     */
    val ringLabel: String
        get() = when (this) {
            MENSTRUAL -> "Period"
            FOLLICULAR -> "Follicular"
            FERTILE_WINDOW -> "Fertile window"
            OVULATION -> "Ovulation"
            EARLY_LUTEAL -> "Early luteal"
            LATE_LUTEAL -> "PMS window"
            HORMONE_BREAK -> "Break week"
            STEADY_STATE -> "Steady"
        }
}

/** A coarse level used for the "energy / libido / patience" meters on the Today screen. */
enum class Level(val displayName: String, val score: Int) {
    VERY_LOW("Very low", 1),
    LOW("Low", 2),
    MEDIUM("Medium", 3),
    HIGH("High", 4),
    VERY_HIGH("Very high", 5),
    VARIES("Varies a lot", 3);
}

/** How much the app trusts its own prediction. Shown honestly in the UI. */
enum class Confidence(val displayName: String, val explanation: String) {
    HIGH(
        "Good",
        "Based on several logged cycles that are close to the same length. The day count should be within a day or so.",
    ),
    MEDIUM(
        "Rough",
        "Based on the average you entered, or on only one or two logged cycles. Treat the day count as an estimate.",
    ),
    LOW(
        "Loose",
        "Her cycles vary a lot from month to month, so the exact day is a guess. Use the phase description, not the day number.",
    ),
    SUPPRESSED(
        "Not a natural cycle",
        "Her contraception controls the hormone levels, so there is no natural cycle to predict. The bleed timing still follows the pack, but ovulation content does not apply.",
    ),
    UNKNOWN(
        "Not enough data",
        "Add the start date of her most recent period so the app has something to count from.",
    ),
}
