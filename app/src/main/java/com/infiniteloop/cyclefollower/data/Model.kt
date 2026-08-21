package com.infiniteloop.cyclefollower.data

import com.infiniteloop.cyclefollower.domain.CyclePhase
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDate

object LocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("java.time.LocalDate", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): LocalDate = LocalDate.parse(decoder.decodeString())
}

/** Whether a contraceptive method stops ovulation happening at all. */
enum class Suppression { NONE, PARTIAL, FULL }

/**
 * Contraception matters more than any other single input. On a method that switches ovulation
 * off there is no fertile window, no estrogen peak and no progesterone rise -- so every piece of
 * "she is ovulating today" content would be false. The app changes what it says instead of
 * pretending the natural cycle is still running underneath.
 */
@Serializable
enum class Contraception(
    val label: String,
    val shortLabel: String,
    val suppression: Suppression,
    /** True when the bleed is a withdrawal bleed caused by the hormone break, not a real period. */
    val bleedIsWithdrawal: Boolean,
    /** True when the bleeding schedule is set by the pack rather than by her body. */
    val scheduleIsFixed: Boolean,
    /** True when bleeding is often irregular or absent, so date predictions are unreliable. */
    val bleedingUnpredictable: Boolean,
    val explanation: String,
) {
    NONE(
        label = "None, or a non-hormonal method",
        shortLabel = "No hormones",
        suppression = Suppression.NONE,
        bleedIsWithdrawal = false,
        scheduleIsFixed = false,
        bleedingUnpredictable = false,
        explanation = "Nothing hormonal in play: condoms, withdrawal, fertility awareness, sterilisation, " +
            "or nothing at all. Her cycle runs on her own hormones, so everything this app describes applies.",
    ),
    COPPER_IUD(
        label = "Copper IUD (non-hormonal coil)",
        shortLabel = "Copper IUD",
        suppression = Suppression.NONE,
        bleedIsWithdrawal = false,
        scheduleIsFixed = false,
        bleedingUnpredictable = false,
        explanation = "A copper coil releases no hormones, so she still ovulates and still has a full natural " +
            "cycle. The one difference: periods are commonly heavier, longer and crampier, especially in the " +
            "first six months to a year.",
    ),
    COMBINED_PILL(
        label = "Combined pill, patch or ring, with a break week",
        shortLabel = "Combined pill",
        suppression = Suppression.FULL,
        bleedIsWithdrawal = true,
        scheduleIsFixed = true,
        bleedingUnpredictable = false,
        explanation = "Steady estrogen and progestin hold ovulation off completely. The bleed in the break " +
            "(or placebo) week is a withdrawal bleed, not a real period -- it happens because the hormones " +
            "stop, not because an egg went unfertilised. There is no fertile window and no ovulation.",
    ),
    COMBINED_CONTINUOUS(
        label = "Combined pill, patch or ring, taken back-to-back",
        shortLabel = "Continuous pill",
        suppression = Suppression.FULL,
        bleedIsWithdrawal = true,
        scheduleIsFixed = true,
        bleedingUnpredictable = true,
        explanation = "Packs run back-to-back with no break, so usually there is no bleed at all, and no " +
            "cycle to track. Breakthrough spotting can still happen. Hormone levels stay flat, which for many " +
            "people means the monthly mood pattern flattens out too.",
    ),
    MINI_PILL(
        label = "Progestin-only pill (mini pill)",
        shortLabel = "Mini pill",
        suppression = Suppression.PARTIAL,
        bleedIsWithdrawal = false,
        scheduleIsFixed = false,
        bleedingUnpredictable = true,
        explanation = "Desogestrel mini pills stop ovulation most of the time; older progestin-only pills " +
            "often do not, so she may still ovulate some months and not others. Bleeding is famously " +
            "unpredictable: it can be regular, constant spotting, or absent. Day counts here are a loose guide.",
    ),
    HORMONAL_IUD(
        label = "Hormonal IUD (Mirena, Kyleena, Jaydess...)",
        shortLabel = "Hormonal IUD",
        suppression = Suppression.PARTIAL,
        bleedIsWithdrawal = false,
        scheduleIsFixed = false,
        bleedingUnpredictable = true,
        explanation = "It works mostly in the uterus, so a lot of people keep ovulating normally and keep " +
            "the hormonal ups and downs of a real cycle -- while bleeding becomes very light or stops " +
            "altogether. That combination is exactly why the bleed-based day count can drift out of step.",
    ),
    IMPLANT(
        label = "Implant (Nexplanon)",
        shortLabel = "Implant",
        suppression = Suppression.FULL,
        bleedIsWithdrawal = false,
        scheduleIsFixed = false,
        bleedingUnpredictable = true,
        explanation = "The implant stops ovulation. Bleeding patterns are all over the place: about a fifth " +
            "have no bleeding, others spot irregularly for months. There is no cycle to count, so this app " +
            "leans on her logged symptoms rather than on dates.",
    ),
    INJECTION(
        label = "Injection (Depo-Provera)",
        shortLabel = "Injection",
        suppression = Suppression.FULL,
        bleedIsWithdrawal = false,
        scheduleIsFixed = false,
        bleedingUnpredictable = true,
        explanation = "The shot stops ovulation for about 12 weeks at a time. Most people stop bleeding " +
            "altogether after a few injections. There is no monthly cycle underneath to predict.",
    );

    val isHormonal: Boolean get() = this != NONE && this != COPPER_IUD
    val hasNaturalCycle: Boolean get() = suppression != Suppression.FULL
}

/**
 * How rough the premenstrual stretch is for her. This widens or narrows the PMS window and
 * changes how strongly the app words its advice.
 */
@Serializable
enum class PmsSeverity(val label: String, val windowDays: Int, val description: String) {
    NONE("Barely notices it", 0, "Some people genuinely sail through. If that is her, the app will keep quiet about PMS."),
    MILD("Mild", 3, "A bit more tired or short-tempered for a couple of days, nothing that derails the week."),
    MODERATE("Moderate", 5, "Clear mood and body changes for several days: irritability, cravings, sore breasts, low patience."),
    SEVERE("Severe", 8, "Symptoms that genuinely disrupt her week -- deep mood drops, anxiety, exhaustion, pain."),
}

enum class SymptomCategory(val label: String) {
    PHYSICAL("Body"),
    EMOTIONAL("Mood"),
    ENERGY("Energy and drive"),
}

/**
 * Symptoms she actually gets. Ticking these turns the generic phase description into something
 * specific to her: the app only mentions what is on her list, in the phase where it typically shows up.
 */
@Serializable
enum class Symptom(
    val label: String,
    val category: SymptomCategory,
    val phases: Set<CyclePhase>,
    /** What is behind it, in plain language. */
    val why: String,
    /** Something concrete the partner can actually do. */
    val help: String,
) {
    CRAMPS(
        "Period cramps", SymptomCategory.PHYSICAL,
        setOf(CyclePhase.MENSTRUAL),
        "The uterus is a muscle and it is squeezing to shed its lining. Chemicals called prostaglandins drive those contractions, and they can briefly cut off blood flow to the muscle -- which is why it feels like a cramp.",
        "Heat is the single most useful thing: a hot water bottle or a heat patch on the lower belly or lower back. Ibuprofen-type painkillers work better than paracetamol here because they act on prostaglandins directly, and work best taken early rather than once the pain has peaked.",
    ),
    HEAVY_BLEEDING(
        "Heavy bleeding", SymptomCategory.PHYSICAL,
        setOf(CyclePhase.MENSTRUAL),
        "A thicker lining, or a copper coil, means more to shed. Losing more blood also drains iron, which is a real physical cause of tiredness -- not her imagination.",
        "Practical beats romantic: make sure supplies are stocked before she runs out, keep iron-rich food around, and do not schedule anything demanding on her heaviest day (usually day 2).",
    ),
    BACK_PAIN(
        "Lower back or thigh ache", SymptomCategory.PHYSICAL,
        setOf(CyclePhase.MENSTRUAL),
        "The nerves serving the uterus share pathways with the lower back and upper legs, so cramping is often felt as a dull ache further down.",
        "Heat on the lower back, and a slow back or hip rub if she wants to be touched. Ask first -- some days touch helps, some days it grates.",
    ),
    HEADACHE(
        "Headaches or migraines", SymptomCategory.PHYSICAL,
        setOf(CyclePhase.LATE_LUTEAL, CyclePhase.MENSTRUAL, CyclePhase.HORMONE_BREAK),
        "Menstrual migraine is triggered by estrogen falling, not by estrogen being low. That is why it clusters in the two days before bleeding and the first days of it -- and why the pill's break week can trigger it too.",
        "Dark, quiet, cool room and no pressure to talk. If she gets these every single cycle, it is worth a doctor's appointment: there are treatments aimed specifically at hormonal migraine.",
    ),
    BLOATING(
        "Bloating and water retention", SymptomCategory.PHYSICAL,
        setOf(CyclePhase.LATE_LUTEAL, CyclePhase.MENSTRUAL),
        "Progesterone slows the gut down and shifts how the body handles salt and water. A couple of pounds of water weight is normal and it goes as the period starts.",
        "Never comment on her body, her stomach or her weight this week. If she says her clothes do not fit, that is a real physical thing happening, not a fishing line for compliments.",
    ),
    BREAST_TENDERNESS(
        "Sore or swollen breasts", SymptomCategory.PHYSICAL,
        setOf(CyclePhase.LATE_LUTEAL),
        "Progesterone after ovulation makes breast tissue swell and hold fluid. It usually peaks in the last few days before the period and eases once bleeding starts.",
        "Assume they hurt before you touch them. This is the classic week where an affectionate grab is genuinely painful rather than playful.",
    ),
    ACNE(
        "Skin breakouts", SymptomCategory.PHYSICAL,
        setOf(CyclePhase.LATE_LUTEAL),
        "As estrogen drops in the second half of the cycle, testosterone's effect on the skin is less opposed, so oil production rises. Chin and jawline are the classic spots.",
        "Say nothing about her skin unless she brings it up, and if she does, do not offer solutions. She has heard them.",
    ),
    NAUSEA(
        "Nausea or digestive upset", SymptomCategory.PHYSICAL,
        setOf(CyclePhase.MENSTRUAL),
        "The same prostaglandins that cramp the uterus also act on the bowel, which is why stomach upset around day 1 to 2 is so common.",
        "Plain food, no strong smells cooking, and no jokes about it.",
    ),
    FATIGUE(
        "Heavy tiredness", SymptomCategory.ENERGY,
        setOf(CyclePhase.MENSTRUAL, CyclePhase.LATE_LUTEAL),
        "Hormones are at their lowest point, blood loss can drop iron, and sleep is often worse in the days before. It is a physical energy deficit.",
        "Take things off her plate without being asked and without announcing it. Cook, drive, deal with the kids, cancel the optional thing.",
    ),
    INSOMNIA(
        "Poor sleep", SymptomCategory.PHYSICAL,
        setOf(CyclePhase.LATE_LUTEAL),
        "Core body temperature runs a few tenths of a degree higher after ovulation and progesterone withdrawal disturbs deep sleep, so the late luteal phase is often the worst sleep of the month.",
        "Keep the room cool, take the early wake-up or the night shift if you have kids, and do not start heavy conversations at 11pm this week.",
    ),
    CRAVINGS(
        "Food cravings and bigger appetite", SymptomCategory.PHYSICAL,
        setOf(CyclePhase.LATE_LUTEAL),
        "Resting metabolism genuinely rises slightly in the luteal phase and serotonin dips, and carbohydrate raises serotonin. Her body is chasing a real chemical, not being weak-willed.",
        "Buy the chocolate. Do not comment on the chocolate. Do not mention the diet she started.",
    ),
    IRRITABILITY(
        "Short fuse and irritability", SymptomCategory.EMOTIONAL,
        setOf(CyclePhase.LATE_LUTEAL),
        "Falling estrogen pulls serotonin down with it, and the brain's calming GABA system reacts to withdrawing progesterone. The result is a genuinely lower threshold for irritation -- the same annoyance hits harder than it would two weeks earlier.",
        "Lower the number of small frictions in her day rather than trying to fix her mood. Do the dishes before she sees them. Do not debate whether something is 'worth' being annoyed about.",
    ),
    ANXIETY(
        "Anxiety or feeling on edge", SymptomCategory.EMOTIONAL,
        setOf(CyclePhase.LATE_LUTEAL),
        "Progesterone breaks down into allopregnanolone, which acts on the brain much like a mild sedative. When it withdraws before a period, some brains get a rebound of anxiety and restlessness.",
        "Steady and predictable helps more than upbeat. Say what the plan is, stick to it, and do not spring surprises this week.",
    ),
    TEARFULNESS(
        "Crying easily", SymptomCategory.EMOTIONAL,
        setOf(CyclePhase.LATE_LUTEAL, CyclePhase.MENSTRUAL),
        "Emotional regulation genuinely takes more effort when serotonin is low. Things that would normally slide off land hard, and tears come faster.",
        "Sit with her. Do not ask why, do not try to fix it, and never say 'you are crying over nothing' -- to her nervous system, right now, it is not nothing.",
    ),
    LOW_MOOD(
        "Low mood or flatness", SymptomCategory.EMOTIONAL,
        setOf(CyclePhase.LATE_LUTEAL, CyclePhase.MENSTRUAL),
        "The premenstrual serotonin dip can feel like a short, self-limiting low mood. The tell is that it lifts within a day or two of bleeding starting.",
        "Presence over pep talks. If the low mood does not lift after the period starts, or it is severe, that is worth taking seriously rather than filing under PMS.",
    ),
    BRAIN_FOG(
        "Brain fog or forgetfulness", SymptomCategory.EMOTIONAL,
        setOf(CyclePhase.LATE_LUTEAL, CyclePhase.MENSTRUAL),
        "Poor sleep plus low estrogen makes word-finding and concentration measurably harder for some people.",
        "Take the mental load: remember the appointments, handle the admin, do not add 'you forgot again' to her day.",
    ),
    OVULATION_PAIN(
        "Ovulation pain (mittelschmerz)", SymptomCategory.PHYSICAL,
        setOf(CyclePhase.OVULATION, CyclePhase.FERTILE_WINDOW),
        "A one-sided ache low in the abdomen as the follicle stretches and releases the egg. It lasts hours to a day or two, and can swap sides from cycle to cycle.",
        "Useful signal: if she gets this reliably, it pins down ovulation far better than any calendar maths. Note which day it happens.",
    ),
    OVULATION_SPOTTING(
        "Mid-cycle spotting", SymptomCategory.PHYSICAL,
        setOf(CyclePhase.OVULATION),
        "A brief dip in estrogen right around ovulation can cause light spotting for a day. Harmless, and another useful marker of the real ovulation day.",
        "Do not mistake it for a period starting -- logging it as one would throw the whole prediction out by two weeks.",
    ),
    HIGH_LIBIDO(
        "Noticeably higher sex drive", SymptomCategory.ENERGY,
        setOf(CyclePhase.FERTILE_WINDOW, CyclePhase.OVULATION),
        "Testosterone peaks alongside estrogen right before ovulation. It is the one stretch of the cycle where desire has a clear hormonal push behind it.",
        "Read it as a green light, not a guarantee. And remember this is precisely the window where sex can lead to pregnancy.",
    ),
    LOW_LIBIDO(
        "Sex drive drops off", SymptomCategory.ENERGY,
        setOf(CyclePhase.LATE_LUTEAL, CyclePhase.MENSTRUAL),
        "Progesterone dominant, estrogen and testosterone low, often combined with bloating, pain and bad sleep. Desire falls for straightforward physical reasons.",
        "Do not take it personally and do not keep score. Affection with no expectation attached lands much better this week.",
    ),
    HIGH_ENERGY(
        "Bursts of energy and confidence", SymptomCategory.ENERGY,
        setOf(CyclePhase.FOLLICULAR, CyclePhase.FERTILE_WINDOW, CyclePhase.OVULATION),
        "Rising estrogen lifts mood, verbal fluency and stamina. Many people feel most like themselves in this stretch.",
        "This is the window to plan the trip, the big night out, the hard conversation or the ambitious project.",
    ),
    ;

    companion object {
        fun forPhase(phase: CyclePhase): List<Symptom> = entries.filter { phase in it.phases }
    }
}
