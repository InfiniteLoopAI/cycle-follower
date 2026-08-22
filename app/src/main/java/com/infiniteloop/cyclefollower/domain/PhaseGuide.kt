package com.infiniteloop.cyclefollower.domain

/**
 * The partner-facing description of a phase.
 *
 * Everything here describes typical patterns. Individual variation is enormous -- plenty of
 * people feel nothing like this, and a bad day in the follicular phase is still just a bad day.
 * The app is a prompt to pay attention, never an explanation to hand back to her.
 */
data class PhaseGuide(
    val phase: CyclePhase,
    val title: String,
    val tagline: String,
    val emoji: String,
    val whatsHappening: String,
    val physical: List<String>,
    val emotional: List<String>,
    val doThis: List<String>,
    val avoidThis: List<String>,
    val goodTimeFor: List<String>,
    /** What is physically going on down there this phase, and what it means for sex. */
    val intimacy: String,
    val energy: Level,
    val libido: Level,
    val socialBattery: Level,
    val patience: Level,
    /** One short line for the notification, the widget and the top of the Today screen. */
    val oneLiner: String,
)

object PhaseGuides {

    private val menstrual = PhaseGuide(
        phase = CyclePhase.MENSTRUAL,
        title = "Period",
        tagline = "Hormones at their lowest point of the month. The pain is real, the tiredness is physical.",
        emoji = "🩸",
        whatsHappening = "Estrogen and progesterone have both crashed, and that crash is exactly what tells the " +
            "uterus to shed its lining. To push the lining out, the uterus contracts -- driven by prostaglandins, " +
            "which also explain why the gut often joins in with nausea or looser stools. Blood loss drains iron, " +
            "which is a genuine physical reason for the flatness she feels. Days 1 and 2 are usually the worst; " +
            "by day 3 or 4 estrogen starts climbing again and most people feel the lift.",
        physical = listOf(
            "Cramping low in the abdomen, often spreading to the lower back and thighs",
            "Heaviest flow on days 1 to 2",
            "Real tiredness -- low hormones plus iron loss, not laziness",
            "Headaches, nausea or an unsettled stomach",
            "Feeling cold, achy, and wanting to be horizontal",
        ),
        emotional = listOf(
            "Often relief: the premenstrual tension usually lifts within a day or two of bleeding starting",
            "Inward and quiet -- low appetite for noise, crowds and plans",
            "Short patience for anything that is not essential",
            "Some people feel unexpectedly calm and clear once the period arrives",
        ),
        doThis = listOf(
            "Get her heat: hot water bottle or a stick-on heat patch beats almost everything else for cramps",
            "Painkillers early rather than once the pain has peaked -- ibuprofen-type ones work best on cramps",
            "Silently take chores off her plate. Cook, drive, deal with the admin",
            "Check supplies before she runs out. Knowing where they are kept is a low bar worth clearing",
            "Keep affection going with zero expectation attached",
            "Warm food, early night, no pressure to be good company",
        ),
        avoidThis = listOf(
            "Using the period to explain anything she says or feels",
            "Booking anything demanding or physical for days 1 and 2",
            "Commenting on what she is eating",
            "Expecting her usual energy and then acting disappointed",
        ),
        goodTimeFor = listOf(
            "Staying in, films, easy company",
            "Cooking for her and handling the household",
            "Quiet, low-demand time together",
        ),
        intimacy = "Estrogen is at its lowest point, so the vaginal walls are at their least plump and natural " +
            "lubrication is the scarcest of the month -- menstrual fluid is not lubrication and does not behave " +
            "like it. The cervix sits low and feels firm, and it is slightly open to let the flow out, which is " +
            "why deep penetration is more likely to bump it and can hurt sharply. Add cramping and the whole " +
            "area is already sore. Desire genuinely splits: some people want sex more now, partly because " +
            "orgasm can ease cramps through muscle release and endorphins, and many want nothing near them. If " +
            "it happens, shallower angles, plenty of lubricant, a towel, and no assumptions.",
        energy = Level.VERY_LOW,
        libido = Level.VARIES,
        socialBattery = Level.LOW,
        patience = Level.LOW,
        oneLiner = "Low energy, real pain. Heat, food, and taking things off her plate beat any conversation today.",
    )

    private val follicular = PhaseGuide(
        phase = CyclePhase.FOLLICULAR,
        title = "Follicular phase",
        tagline = "Estrogen climbing. For most people this is the best stretch of the month.",
        emoji = "🌱",
        whatsHappening = "The pituitary is pushing FSH, which ripens follicles in the ovary, and the growing " +
            "follicle floods the body with estrogen. Estrogen is the feel-good hormone of the cycle: it lifts " +
            "serotonin and dopamine, sharpens memory and word-finding, raises pain tolerance, improves sleep and " +
            "puts energy back in the tank. The uterine lining is quietly rebuilding at the same time.",
        physical = listOf(
            "Energy properly back, stamina noticeably better",
            "Skin clearer, sleep deeper",
            "Strength and endurance near their best -- a good stretch for training hard",
            "Appetite steady, cravings gone",
        ),
        emotional = listOf(
            "Optimistic, open, up for things",
            "More resilient to stress -- the same problem simply lands lighter than it would in the PMS week",
            "Sociable and talkative",
            "Motivated: this is when new plans and projects get started",
        ),
        doThis = listOf(
            "Put the good stuff in the diary now: trips, dates, the weekend with friends",
            "Bring up the difficult subject this week if there is one to bring up",
            "Say yes to spontaneity -- it will be met halfway",
            "Start the shared project, the training plan, the house thing",
        ),
        avoidThis = listOf(
            "Letting the window go by and then trying to cram it all into her PMS week",
            "Assuming this energy level is her permanent baseline and being disappointed later",
        ),
        goodTimeFor = listOf(
            "Big decisions and planning",
            "Hard conversations",
            "Social events, travel, anything ambitious",
            "Trying something new together",
        ),
        intimacy = "Estrogen is climbing, and it rebuilds the vaginal tissue as it goes: the walls thicken, blood flow " +
            "improves and natural lubrication returns and increases week on week. The cervix is starting to " +
            "rise and soften. Comfort improves noticeably day by day and desire usually rises with it. Not the " +
            "peak yet, but easy and unhurried -- often the nicest stretch for taking your time.",
        energy = Level.HIGH,
        libido = Level.MEDIUM,
        socialBattery = Level.HIGH,
        patience = Level.HIGH,
        oneLiner = "Energy and mood on the way up. Best week of the month to plan things, ask things, or book something.",
    )

    private val fertileWindow = PhaseGuide(
        phase = CyclePhase.FERTILE_WINDOW,
        title = "Fertile window",
        tagline = "Estrogen near its peak, testosterone rising. Sex during these days can lead to pregnancy.",
        emoji = "✨",
        whatsHappening = "Estrogen is close to its monthly maximum and testosterone is climbing with it, which " +
            "is the one stretch of the cycle where desire has a clear hormonal push behind it. Cervical mucus " +
            "turns clear and stretchy, like raw egg white -- that change is what lets sperm survive for up to " +
            "five days inside the body, and it is why these days count as fertile even though the egg has not " +
            "been released yet.",
        physical = listOf(
            "Noticeably more discharge, clear and stretchy",
            "Skin, hair and energy at their best",
            "Breasts may feel fuller",
            "Senses sharper -- smell in particular",
        ),
        emotional = listOf(
            "Confident and outgoing",
            "Flirtier, more physically expressive",
            "Higher tolerance for risk and novelty",
            "Wants to be seen and to go out",
        ),
        doThis = listOf(
            "Initiate -- this is the most receptive window of her month",
            "Make an effort with how you look and how the evening is planned. It registers more now",
            "Get out of the house together",
            "Be deliberate about contraception if a pregnancy is not the plan",
        ),
        avoidThis = listOf(
            "Treating any day of the cycle as a 'safe' day. This app is not birth control",
            "Wasting the most sociable week of her month on the sofa, unless that is what she wants",
        ),
        goodTimeFor = listOf(
            "Dates, going out, seeing people",
            "Sex -- with contraception handled deliberately either way",
            "Anything that needs her at her most confident",
        ),
        intimacy = "Estrogen is near its maximum, and this is physically the easiest stretch of her month. Blood flow " +
            "is highest so the tissue is at its plumpest and most elastic, and natural lubrication is at its " +
            "most abundant and slippery -- the clear, stretchy, egg-white mucus is part of that. The cervix has " +
            "moved high and turned soft; the standard description is that it changes from feeling like the tip " +
            "of your nose to feeling like your lips, and it opens slightly. A high cervix means more room, so " +
            "deep penetration is most comfortable now. It is also the window where pregnancy is possible, so " +
            "contraception here is a deliberate decision rather than an afterthought.",
        energy = Level.VERY_HIGH,
        libido = Level.HIGH,
        socialBattery = Level.VERY_HIGH,
        patience = Level.HIGH,
        oneLiner = "Peak energy and confidence, sex drive climbing. Also the days when pregnancy is actually possible.",
    )

    private val ovulation = PhaseGuide(
        phase = CyclePhase.OVULATION,
        title = "Ovulation",
        tagline = "The egg is released. Peak desire for many -- and a day of one-sided ache for some.",
        emoji = "🌟",
        whatsHappening = "A surge of LH triggers the follicle to rupture and release the egg, roughly a day to " +
            "a day and a half after the surge begins. The egg itself survives only 12 to 24 hours. Immediately " +
            "afterwards estrogen dips briefly before progesterone starts rising, and body temperature ticks up " +
            "by about a third of a degree and stays up until the next period -- which is how temperature " +
            "tracking confirms ovulation happened, always after the fact.",
        physical = listOf(
            "One-sided ache low in the abdomen for some, lasting hours to a day (mittelschmerz)",
            "Occasional light spotting for a day",
            "Body temperature rises slightly and stays raised",
            "Discharge at its most abundant, then drying up over the next couple of days",
        ),
        emotional = listOf(
            "Confidence at its highest point of the month",
            "Very sociable, quick, verbally sharp",
            "A short mood dip is possible right after, as estrogen drops before progesterone takes over",
        ),
        doThis = listOf(
            "If she gets ovulation pain, note which day -- it pins down her real ovulation better than any calendar",
            "Take the initiative socially and physically",
            "Do not read the short post-ovulation dip as something you did",
        ),
        avoidThis = listOf(
            "Believing ovulation is always day 14. It moves between cycles, even for regular people",
            "Mistaking mid-cycle spotting for a period starting -- logging that would throw every prediction out by two weeks",
        ),
        goodTimeFor = listOf(
            "Sex, if a pregnancy is wanted -- and careful contraception if it is not",
            "Anything social or high-energy",
        ),
        intimacy = "The peak of everything above: the cervix is at its highest, softest and most open, lubrication is " +
            "at its maximum, and testosterone has desire at the highest point of the cycle. Two caveats. If she " +
            "gets one-sided ovulation pain, deep penetration on that side can be uncomfortable for a day or so. " +
            "And light spotting around now is harmless, not a period starting.",
        energy = Level.HIGH,
        libido = Level.VERY_HIGH,
        socialBattery = Level.HIGH,
        patience = Level.HIGH,
        oneLiner = "Ovulation day. Highest fertility and usually the highest sex drive of her month.",
    )

    private val earlyLuteal = PhaseGuide(
        phase = CyclePhase.EARLY_LUTEAL,
        title = "Early luteal phase",
        tagline = "Progesterone takes over. Calmer, cosier, sleepier -- the quiet half of the month.",
        emoji = "🌙",
        whatsHappening = "The emptied follicle turns into the corpus luteum and starts producing progesterone. " +
            "Progesterone is the body's own sedative: it breaks down into allopregnanolone, which acts on the " +
            "same brain receptors as calming medication. It also raises body temperature, slows the gut, and " +
            "nudges appetite up because resting metabolism genuinely rises a little in this half of the cycle.",
        physical = listOf(
            "Warmer than usual, and often sleepier",
            "Hungrier, with slightly slower digestion and some bloating",
            "Workouts feel heavier, endurance drops a little",
            "Breasts may start to feel full towards the end of this stretch",
        ),
        emotional = listOf(
            "Content and settled -- often the steadiest mood of the month",
            "More inward: home beats going out",
            "Focused on tidying, sorting, nesting",
            "Less appetite for big social occasions",
        ),
        doThis = listOf(
            "Lean into home: cook together, sofa, series, early nights",
            "Keep routines predictable",
            "Get the practical things done now, before the PMS window opens",
        ),
        avoidThis = listOf(
            "Filling the diary with big social commitments",
            "Reading her wanting a quiet night in as her losing interest",
        ),
        goodTimeFor = listOf(
            "Home projects and admin",
            "Quiet, undemanding time together",
            "Getting ahead of anything that will be hard next week",
        ),
        intimacy = "Progesterone takes over and reverses the changes: the cervix drops back down, firms up and closes, " +
            "and the mucus turns thick, sticky and scant. Natural lubrication drops off noticeably from the " +
            "ovulation peak even though the tissue is still reasonably plump. Comfort is fine but drier, so " +
            "lubricant genuinely earns its place here -- reaching for it is about physiology, not a verdict on " +
            "how much she wants you. Desire settles into a steady middle.",
        energy = Level.MEDIUM,
        libido = Level.MEDIUM,
        socialBattery = Level.MEDIUM,
        patience = Level.HIGH,
        oneLiner = "Calm, cosy and a bit sleepy. Home time lands better than big plans right now.",
    )

    private val lateLuteal = PhaseGuide(
        phase = CyclePhase.LATE_LUTEAL,
        title = "PMS window",
        tagline = "The last stretch of the luteal phase, with both hormones falling away fast. This is the " +
            "part of the month that asks the most of you.",
        emoji = "⛈️",
        whatsHappening = "With no pregnancy, the corpus luteum breaks down and both progesterone and estrogen " +
            "drop sharply. Two things follow. Serotonin falls along with estrogen, which lowers mood and shortens " +
            "her fuse. And allopregnanolone -- the calming by-product of progesterone -- withdraws, which in " +
            "sensitive brains produces a genuine rebound of anxiety and irritability. This is a chemical " +
            "withdrawal, not a character flaw. Around three in four people notice some of it, and for most it " +
            "lifts within a day or two of bleeding starting.",
        physical = listOf(
            "Sore, swollen breasts",
            "Bloating and a couple of pounds of water weight",
            "Carbohydrate and sugar cravings driven by the serotonin dip",
            "Broken sleep, and headaches or migraines for some",
            "Skin breaking out along the chin and jaw",
            "Everything feels like more effort",
        ),
        emotional = listOf(
            "A far shorter fuse -- the same annoyance genuinely hits harder than it would two weeks earlier",
            "Anxiety, restlessness, a sense of dread with nothing attached to it",
            "Tears arriving faster and over smaller things",
            "Feeling unattractive, and more sensitive to any hint of rejection or criticism",
            "Brain fog: forgetting things, losing words",
        ),
        doThis = listOf(
            "Remove friction before she meets it: dishes done, laundry on, kids handled, no admin left for her",
            "Be predictable. Say what the plan is and then stick to it",
            "Validate first, solve later -- or never. 'That sounds genuinely annoying' does more than any fix",
            "Physical affection with no expectation attached",
            "Feed her, protect her sleep, and take the early morning",
            "Absorb the odd snap without collecting it for later",
        ),
        avoidThis = listOf(
            "'Is it your period?' -- the single fastest way to turn a bad evening into a bad week",
            "Arguing that her reaction is out of proportion",
            "Any comment about her body, her skin, her weight or what she is eating",
            "Surprises, last-minute changes and unplanned guests",
            "Starting a big relationship conversation. It will not go the way it would have ten days ago",
        ),
        goodTimeFor = listOf(
            "Low-demand evenings, takeaway, an early night",
            "Doing rather than discussing",
        ),
        intimacy = "Estrogen and progesterone are both falling away, so lubrication is at its lowest of the month and " +
            "the tissue is less plump. Everything is also more sensitive rather than more receptive: sore " +
            "breasts, bloating and the first cramps. The cervix is low and firm, so deep penetration is the " +
            "most likely to be uncomfortable now. Desire is usually at its lowest of the cycle. If it happens, " +
            "slower and shallower with plenty of lubricant -- and if it does not, that is physiology, not " +
            "rejection. Do not keep score.",
        energy = Level.LOW,
        libido = Level.LOW,
        socialBattery = Level.VERY_LOW,
        patience = Level.VERY_LOW,
        oneLiner = "PMS window. Patience is genuinely lower today -- reduce friction, do not add any, and let small snaps go.",
    )

    private val hormoneBreak = PhaseGuide(
        phase = CyclePhase.HORMONE_BREAK,
        title = "Hormone-free break",
        tagline = "The break week. Hormones are switched off for a few days, and some people feel it.",
        emoji = "⏸️",
        whatsHappening = "During the pill-free, patch-free or ring-free days there is no hormone coming in at " +
            "all. The bleed that happens is a withdrawal bleed -- caused by the hormones stopping, not by a real " +
            "cycle ending. No egg was released and none will be. The abrupt drop is why headaches, migraines and " +
            "a few flat days cluster in this week for some people, and why some doctors suggest skipping the " +
            "break entirely.",
        physical = listOf(
            "A bleed that is usually lighter and more predictable than a natural period",
            "Headaches or migraines triggered by the hormone drop",
            "Cramping, though typically milder than a natural period",
        ),
        emotional = listOf(
            "A few flat or irritable days for some, driven by withdrawal rather than by a cycle",
            "Many people notice nothing at all in this week",
        ),
        doThis = listOf(
            "Treat it like a period week: lower demands, more help",
            "If she gets a migraine every single break week, it is worth raising with her doctor -- there are options",
        ),
        avoidThis = listOf(
            "Reading ovulation or fertility into this week. Neither is happening",
        ),
        goodTimeFor = listOf("Quiet nights", "Practical help rather than plans"),
        intimacy = "During the hormone-free days there is no estrogen coming in, so lubrication and plumpness dip much " +
            "as they would in a natural period week, alongside the withdrawal bleed. There are no fertile " +
            "changes to read either: on the pill the cervix and the mucus stay largely static all month, so " +
            "cervix position tells you nothing useful about timing.",
        energy = Level.LOW,
        libido = Level.LOW,
        socialBattery = Level.MEDIUM,
        patience = Level.MEDIUM,
        oneLiner = "Pill break week. It is a withdrawal bleed, not a real period -- but the headache and the flat mood are real.",
    )

    private val steadyState = PhaseGuide(
        phase = CyclePhase.STEADY_STATE,
        title = "Steady hormone phase",
        tagline = "Hormone levels held flat. There is no natural cycle running underneath.",
        emoji = "⚖️",
        whatsHappening = "While her method is active, hormone levels stay roughly constant. No follicle ripens, " +
            "no estrogen peak arrives, no progesterone surge follows, and no egg is released. For a lot of people " +
            "that means the monthly emotional pattern flattens right out -- which is exactly why some choose it. " +
            "The important consequence for you: her moods this week are about her life, not about a cycle. Take " +
            "them at face value.",
        physical = listOf(
            "No predictable monthly pattern to energy or pain",
            "Bleeding may be light, irregular, or absent altogether -- all normal on these methods",
        ),
        emotional = listOf(
            "Usually steadier month to month than a natural cycle",
            "A persistently low mood or a flattened sex drive can be a side effect of the method itself -- " +
                "worth raising with a doctor rather than filing under 'just how she is'",
        ),
        doThis = listOf(
            "Take what she says at face value instead of looking for a hormonal explanation",
            "If her mood or libido changed noticeably after starting the method, say so kindly -- she may not have connected it",
        ),
        avoidThis = listOf(
            "Attributing anything to a cycle that is not currently running",
            "Assuming no bleeding means something is wrong. On these methods it usually does not",
        ),
        goodTimeFor = listOf("Anything -- there is no phase-based best or worst week right now"),
        intimacy = "With hormone levels held flat there is no monthly swing in cervix position, mucus or lubrication " +
            "-- the physical cues that fertility awareness relies on are switched off. Worth knowing: some " +
            "hormonal methods reduce natural lubrication and libido as a genuine side effect, particularly some " +
            "combined pills and the injection. Lubricant handles the first part. If it is a marked change from " +
            "how things were before the method, that is worth raising with a doctor rather than quietly " +
            "accepting.",
        energy = Level.MEDIUM,
        libido = Level.MEDIUM,
        socialBattery = Level.MEDIUM,
        patience = Level.MEDIUM,
        oneLiner = "No natural cycle running today. Whatever the mood is, it is about life rather than hormones.",
    )

    private val all = listOf(
        menstrual, follicular, fertileWindow, ovulation,
        earlyLuteal, lateLuteal, hormoneBreak, steadyState,
    )

    fun of(phase: CyclePhase): PhaseGuide = all.first { it.phase == phase }

    /** In cycle order, for the "whole cycle at a glance" screen. */
    fun naturalCycleOrder(): List<PhaseGuide> = listOf(
        menstrual, follicular, fertileWindow, ovulation, earlyLuteal, lateLuteal,
    )

    fun all(): List<PhaseGuide> = all
}
