package com.infiniteloop.cyclefollower.domain

enum class NoteTone { INFO, WARNING, GOLDEN }

sealed interface Block {
    data class Head(val text: String) : Block
    data class Para(val text: String) : Block
    data class Bullets(val items: List<String>) : Block
    data class Numbered(val items: List<String>) : Block
    data class Note(val text: String, val tone: NoteTone = NoteTone.INFO, val title: String? = null) : Block
}

data class Article(
    val id: String,
    val title: String,
    val subtitle: String,
    val emoji: String,
    val minutes: Int,
    val blocks: List<Block>,
)

/**
 * The reference section. Written for someone starting from zero, because most men are, and
 * because the day-to-day hints only make sense once you know what is driving them.
 */
object Library {

    private val basics = Article(
        id = "basics",
        title = "The whole cycle in five minutes",
        subtitle = "What is actually happening, month after month",
        emoji = "🔄",
        minutes = 5,
        blocks = listOf(
            Block.Para(
                "The menstrual cycle is not a countdown to a period. It is a month-long hormonal loop whose " +
                    "whole purpose is to prepare an egg and a place for it to land. Everything else -- energy, " +
                    "mood, sleep, appetite, sex drive, pain -- rides on top of that loop.",
            ),
            Block.Head("Day 1 is the first day of bleeding"),
            Block.Para(
                "Not the day before, not spotting, not the end. The first day of proper flow. Everything in " +
                    "this app counts from there, which is why logging it accurately matters more than any other " +
                    "input.",
            ),
            Block.Head("The first half: building up"),
            Block.Para(
                "While she bleeds, the pituitary is already pushing FSH, which starts ripening follicles in the " +
                    "ovaries. One takes the lead and pumps out estrogen. Estrogen rebuilds the uterine lining and, " +
                    "as a side effect, lifts mood, energy, verbal fluency and pain tolerance. This is why most " +
                    "people feel progressively better through the first two weeks.",
            ),
            Block.Head("The middle: release"),
            Block.Para(
                "When estrogen peaks, it triggers a surge of LH. Somewhere between 24 and 36 hours later the " +
                    "follicle ruptures and releases an egg. The egg survives less than a day. Sperm, in the right " +
                    "cervical mucus, survive up to five. That mismatch is what creates a fertile window of about " +
                    "six days rather than one.",
            ),
            Block.Head("The second half: waiting"),
            Block.Para(
                "The emptied follicle becomes the corpus luteum and produces progesterone, which holds the lining " +
                    "in place in case a pregnancy arrives. Progesterone is calming and sedating, raises body " +
                    "temperature slightly, and slows digestion. This is the quiet, cosy, hungry part of the month.",
            ),
            Block.Head("The end: the drop"),
            Block.Para(
                "If no pregnancy arrives, the corpus luteum breaks down after about 12 to 14 days. Progesterone " +
                    "and estrogen both fall off a cliff. That fall is what triggers the next period -- and it is " +
                    "also what causes PMS. Then day 1 comes round again.",
            ),
            Block.Note(
                title = "The single most useful fact",
                text = "The second half of the cycle is a near-fixed length: ovulation to period is almost always " +
                    "12 to 14 days, whatever her total cycle length. The first half is what stretches and shrinks. " +
                    "So a 34-day cycle does not mean she ovulates on day 17 -- it means she ovulates around day 20.",
            ),
            Block.Head("What counts as normal"),
            Block.Bullets(
                listOf(
                    "Cycle length: 21 to 35 days in adults. Only around one in eight cycles is exactly 28 days.",
                    "Bleeding: 2 to 7 days.",
                    "Variation between her own cycles of a few days is completely normal.",
                    "Teenagers and people approaching menopause vary far more than that, normally.",
                ),
            ),
        ),
    )

    private val hormones = Article(
        id = "hormones",
        title = "What the hormones actually do",
        subtitle = "Five chemicals explain most of the month",
        emoji = "🧪",
        minutes = 5,
        blocks = listOf(
            Block.Head("Estrogen -- the one that lifts"),
            Block.Para(
                "Rises through the first half, peaks just before ovulation, makes a smaller second bump in the " +
                    "luteal phase. It boosts serotonin production and receptor sensitivity, which is why mood, " +
                    "confidence, sociability and even pain tolerance track it so closely. When estrogen falls, " +
                    "serotonin falls with it -- that is the mechanism behind both PMS mood and menstrual migraine.",
            ),
            Block.Head("Progesterone -- the one that calms and slows"),
            Block.Para(
                "Only present in the second half, after ovulation. The body converts it into allopregnanolone, " +
                    "which acts on the same brain receptors as sedatives and anti-anxiety medication. It also " +
                    "raises core temperature by about a third of a degree, relaxes smooth muscle (hence bloating " +
                    "and constipation), and nudges appetite up. Its withdrawal at the end of the cycle is a genuine " +
                    "chemical withdrawal, and some brains react to it with anxiety and irritability.",
            ),
            Block.Head("Testosterone -- yes, she has it too"),
            Block.Para(
                "Peaks around ovulation alongside estrogen. It is the main driver of the mid-cycle jump in sex " +
                    "drive, and it contributes to skin oil production -- one reason breakouts cluster in the " +
                    "second half when estrogen is no longer balancing it.",
            ),
            Block.Head("FSH and LH -- the instructions from the brain"),
            Block.Para(
                "FSH ripens the follicles. LH surges to trigger release. You never feel these directly, but " +
                    "ovulation predictor strips work by detecting the LH surge, which is why they warn about " +
                    "ovulation roughly a day before it happens.",
            ),
            Block.Head("Prostaglandins -- the ones that hurt"),
            Block.Para(
                "Not sex hormones at all. They are local chemicals released in the uterus to make it contract and " +
                    "shed. Too many, and the contractions become painful cramps -- and because they also act on " +
                    "the bowel, they explain the nausea and looser stools that come with day one. Ibuprofen and " +
                    "similar anti-inflammatories work on period pain precisely because they block prostaglandin " +
                    "production, which is why they beat paracetamol here and why taking them early works better.",
            ),
        ),
    )

    private val ovulation = Article(
        id = "ovulation",
        title = "Ovulation and the fertile window",
        subtitle = "How pregnancy timing really works",
        emoji = "🥚",
        minutes = 5,
        blocks = listOf(
            Block.Para(
                "Ovulation is a single event lasting minutes: one follicle ruptures and releases one egg. " +
                    "Everything around it -- the fertile window, the mood, the libido -- is the build-up and the " +
                    "aftermath.",
            ),
            Block.Head("Why the window is six days, not one"),
            Block.Bullets(
                listOf(
                    "The egg survives 12 to 24 hours after release.",
                    "Sperm survive up to 5 days, but only in the fertile cervical mucus that appears near ovulation.",
                    "So sex up to five days before ovulation can still result in pregnancy, and sex a day after it usually cannot.",
                ),
            ),
            Block.Note(
                title = "This app is not contraception",
                tone = NoteTone.WARNING,
                text = "Calendar prediction is the least reliable family-planning method there is. Ovulation shifts " +
                    "between cycles even for people with regular periods -- illness, stress, travel and disrupted " +
                    "sleep all move it. Fertility-awareness methods that actually work require daily temperature " +
                    "readings and mucus checks and proper training. Do not use these day numbers to avoid or " +
                    "achieve a pregnancy.",
            ),
            Block.Head("The day 14 myth"),
            Block.Para(
                "Day 14 is only right for a textbook 28-day cycle. The reliable rule runs the other way: ovulation " +
                    "happens about 14 days before the NEXT period. On a 32-day cycle that is day 18; on a 25-day " +
                    "cycle it is day 11. This app counts backwards for exactly that reason.",
            ),
            Block.Head("The three signs her body actually gives"),
            Block.Numbered(
                listOf(
                    "Cervical mucus. It shifts from dry, to sticky, to creamy, to clear and stretchy like raw egg " +
                        "white right before ovulation, then dries up sharply. This is the earliest usable sign, and " +
                        "it appears before ovulation, which is what makes it useful.",
                    "Basal body temperature. It rises about 0.3 degrees Celsius after ovulation and stays up until " +
                        "the next period. This confirms ovulation happened -- but only after the fertile window has " +
                        "already closed.",
                    "Ovulation pain. A one-sided ache low in the abdomen, lasting hours to a day. Not everyone gets " +
                        "it, but if she does, it is the most precise marker you will get without test strips.",
                ),
            ),
            Block.Head("What it means for you day to day"),
            Block.Para(
                "This is the stretch where estrogen and testosterone are both high: more confident, more social, " +
                    "more physically interested, better sleep, better skin, more energy in the gym. If there is a " +
                    "week to book the good thing, this is it.",
            ),
        ),
    )

    private val pms = Article(
        id = "pms",
        title = "PMS, PMDD and why none of it is made up",
        subtitle = "The hardest week, explained properly",
        emoji = "⛈️",
        minutes = 6,
        blocks = listOf(
            Block.Para(
                "Premenstrual symptoms are the direct result of estrogen and progesterone collapsing at the end of " +
                    "the cycle. Around three in four people who menstruate notice some of it. It is not a " +
                    "personality trait and it is not a choice.",
            ),
            Block.Head("What is actually going wrong"),
            Block.Bullets(
                listOf(
                    "Estrogen drops, and serotonin drops with it. Lower serotonin means lower mood, more irritability and carbohydrate cravings.",
                    "Progesterone withdraws, taking allopregnanolone with it. In sensitive brains this produces a rebound of anxiety and restlessness, much like coming off a sedative.",
                    "Fluid shifts cause bloating and breast tenderness.",
                    "Body temperature and hormone withdrawal together wreck sleep, and poor sleep makes every other symptom worse.",
                ),
            ),
            Block.Head("The tell that it is premenstrual"),
            Block.Para(
                "Real PMS follows a strict pattern: symptoms appear in the second half of the cycle, and they lift " +
                    "within a day or two of bleeding starting. If low mood is constant all month, that is not PMS -- " +
                    "and treating it as PMS means missing something that deserves attention in its own right.",
            ),
            Block.Head("PMDD is a different thing"),
            Block.Para(
                "Premenstrual dysphoric disorder affects roughly 3 to 8 percent of people who menstruate. It is a " +
                    "recognised diagnosis, not severe PMS with a fancier name. The mood symptoms dominate: deep " +
                    "hopelessness, rage, panic, feeling out of control, sometimes thoughts of self-harm -- for one " +
                    "to two weeks, every single month, then gone. The current understanding is not that these " +
                    "people have abnormal hormone levels, but that their brains respond abnormally strongly to " +
                    "normal hormonal shifts.",
            ),
            Block.Note(
                title = "If this sounds like her",
                tone = NoteTone.WARNING,
                text = "PMDD is treatable, and effective treatments exist -- SSRIs (sometimes taken only in the " +
                    "second half of the cycle), certain combined pills, and CBT among them. Tracking symptoms daily " +
                    "for two or three cycles is what doctors ask for, because the timing pattern is the diagnosis. " +
                    "If she ever mentions self-harm, treat it as urgent and get real medical help, not app advice.",
            ),
            Block.Head("What genuinely helps, from your side"),
            Block.Bullets(
                listOf(
                    "Reduce the number of decisions and chores she has to face, without announcing that you are doing it.",
                    "Be boringly predictable. Changed plans and surprises cost far more this week.",
                    "Validate before you solve. Most of the time, do not solve at all.",
                    "Protect her sleep. Take the early start, the night feed, the dog.",
                    "Do not keep score of things said in the worst 48 hours.",
                ),
            ),
            Block.Head("What does not help"),
            Block.Bullets(
                listOf(
                    "Pointing out that she is premenstrual. It reframes her feelings as a malfunction.",
                    "Debating proportionality. Her threshold genuinely moved; the argument you are having is about the threshold.",
                    "Any remark about her body, her skin, her appetite or her weight.",
                    "Treating the good weeks as the real her and the bad week as a deviation. It is all her.",
                ),
            ),
        ),
    )

    private val pain = Article(
        id = "pain",
        title = "Period pain, and what actually helps",
        subtitle = "Beyond offering paracetamol and looking sympathetic",
        emoji = "🔥",
        minutes = 4,
        blocks = listOf(
            Block.Para(
                "Cramps are the uterus contracting hard enough to briefly squeeze its own blood supply shut. " +
                    "Prostaglandins drive it. Some people barely notice; for others it is genuinely disabling pain " +
                    "for two days a month.",
            ),
            Block.Head("What works, roughly in order"),
            Block.Numbered(
                listOf(
                    "Heat. A hot water bottle or a stick-on heat patch on the lower abdomen is remarkably effective -- " +
                        "in trials it performs comparably to painkillers, with no side effects.",
                    "Anti-inflammatory painkillers such as ibuprofen or naproxen. They block prostaglandins directly, " +
                        "so they beat paracetamol for this specific pain. They work far better started at the first " +
                        "twinge than once the pain has taken hold.",
                    "Light movement. Walking or gentle stretching helps many people, though not on the worst day.",
                    "Sleep, warmth and being left alone. Underrated and free.",
                ),
            ),
            Block.Head("Practical things you can do without being asked"),
            Block.Bullets(
                listOf(
                    "Know where the painkillers and the supplies are kept, and keep them stocked.",
                    "Fill the hot water bottle before she asks.",
                    "Take the driving, the cooking and the admin off her for two days.",
                    "Do not book anything demanding for days 1 and 2 of her cycle. Check the app first.",
                ),
            ),
            Block.Note(
                title = "When pain is not just 'bad periods'",
                tone = NoteTone.WARNING,
                text = "Pain that stops her working or sleeping, pain that painkillers do not touch, pain during sex, " +
                    "or pain that has clearly got worse over the years is worth a doctor's appointment. Endometriosis " +
                    "affects roughly one in ten and takes years to get diagnosed, largely because severe period pain " +
                    "gets normalised. If she has been told to just get on with it, that is a reason to push, not to " +
                    "accept it.",
            ),
        ),
    )

    private val contraception = Article(
        id = "contraception",
        title = "How contraception changes the picture",
        subtitle = "Why the app says something different depending on her method",
        emoji = "💊",
        minutes = 5,
        blocks = listOf(
            Block.Para(
                "This is the input most cycle apps quietly ignore, and it changes everything. On methods that stop " +
                    "ovulation there is no estrogen peak, no progesterone rise and no fertile window -- so any app " +
                    "telling you she is 'ovulating today' is simply wrong.",
            ),
            Block.Head("Combined pill, patch or ring"),
            Block.Para(
                "Steady synthetic hormones switch ovulation off entirely. The bleed during the break week is a " +
                    "withdrawal bleed caused by stopping the hormones, not a real period -- medically there is no " +
                    "need for it at all, which is why many doctors now suggest running packs back to back. Some " +
                    "people get headaches or a mood dip in the break week from the hormone drop.",
            ),
            Block.Head("Hormonal IUD"),
            Block.Para(
                "Acts mostly locally in the uterus. Many people keep ovulating and keep the full natural hormonal " +
                    "rhythm -- while bleeding becomes very light or stops completely. That combination is exactly " +
                    "why a bleed-based day count drifts out of step: the hormones are still cycling but the marker " +
                    "the app counts from has gone quiet.",
            ),
            Block.Head("Implant, injection, mini pill"),
            Block.Para(
                "The implant and the injection suppress ovulation. The mini pill depends on type: newer " +
                    "desogestrel ones usually stop ovulation, older ones often do not. All three make bleeding " +
                    "unpredictable -- irregular spotting, or no bleeding at all, both entirely normal on these " +
                    "methods. There is no reliable cycle to count.",
            ),
            Block.Head("Copper IUD"),
            Block.Para(
                "No hormones at all, so the natural cycle runs untouched and everything in this app applies. The " +
                    "one difference is that periods are commonly heavier and crampier, particularly in the first year.",
            ),
            Block.Note(
                title = "Worth noticing",
                text = "Hormonal contraception genuinely affects mood and libido for some people, and not for others. " +
                    "If her mood or her sex drive changed noticeably after starting a method, that is a real thing " +
                    "worth raising kindly -- and worth raising with a doctor, since switching method often fixes it. " +
                    "It is not something to file away as 'how she is now'.",
            ),
        ),
    )

    private val mistakes = Article(
        id = "mistakes",
        title = "The mistakes men make",
        subtitle = "Including the one this app makes it easy to make",
        emoji = "🚫",
        minutes = 4,
        blocks = listOf(
            Block.Note(
                title = "Read this one first",
                tone = NoteTone.GOLDEN,
                text = "The point of knowing where she is in her cycle is to change what YOU do. The moment it " +
                    "becomes a way to explain, dismiss or predict what she feels, it has made things worse than " +
                    "knowing nothing at all.",
            ),
            Block.Numbered(
                listOf(
                    "Asking 'is it your period?' during an argument. It tells her that her point is a symptom. " +
                        "Nothing in this app is worth saying out loud in that moment.",
                    "Treating the cycle as the cause of everything. Plenty of her bad days are caused by you, by " +
                        "work, or by the world -- exactly like yours.",
                    "Assuming every woman follows the textbook. A large share of people notice no cyclical mood " +
                        "change at all. If she is one of them, believe her over this app.",
                    "Announcing that you are 'tracking' her. Being handled feels very different from being " +
                        "considered. Tell her the app exists; do not narrate its outputs at her.",
                    "Waiting for the bad week and then being helpful. The good weeks are when the relationship gets " +
                        "built; the hard week is when it gets tested.",
                    "Treating period sex, or its absence, as a rule. Ask. Desire during a period varies enormously.",
                    "Commenting on bloating, skin or appetite. Never a good idea; catastrophically bad in the " +
                        "premenstrual week.",
                    "Assuming heavy pain is normal and she should cope. Severe pain is a medical question, not a " +
                        "character test.",
                    "Using the fertile window as birth control. It is not, and it never has been.",
                    "Getting the basics wrong for years -- not knowing which products she uses, where they are, or " +
                        "what her cycle length is. Fixing that costs one conversation.",
                    "Expecting credit. Doing the dishes in her PMS week is not a favour you cash in later.",
                ),
            ),
        ),
    )

    private val talking = Article(
        id = "talking",
        title = "How to talk about it",
        subtitle = "Without sounding like you swallowed a textbook",
        emoji = "💬",
        minutes = 3,
        blocks = listOf(
            Block.Head("Tell her the app exists"),
            Block.Para(
                "Secretly tracking her cycle reads badly however good the intention was. Say it plainly: you " +
                    "wanted to stop being caught out, and you would rather understand than guess. Most people find " +
                    "that flattering. Some find it intrusive -- and if she does, that answer stands.",
            ),
            Block.Head("Ask, then use what she tells you"),
            Block.Bullets(
                listOf(
                    "'What actually helps when the cramps are bad?'",
                    "'Is there a week where you would rather I did not book things?'",
                    "'When you are like this, do you want company or space?'",
                    "'Do you want me to do something, or do you just want me to listen?'",
                ),
            ),
            Block.Head("Phrases that work in the hard week"),
            Block.Bullets(
                listOf(
                    "'That sounds genuinely annoying.'",
                    "'I have got dinner. Go and lie down.'",
                    "'You do not have to be good company tonight.'",
                    "'Do you want me to cancel Saturday?'",
                ),
            ),
            Block.Head("Phrases that do damage"),
            Block.Bullets(
                listOf(
                    "'Is it that time of the month?'",
                    "'You are overreacting.'",
                    "'You were fine about this last week.'",
                    "'Calm down.'",
                ),
            ),
            Block.Note(
                text = "If she keeps saying nothing is wrong and clearly something is, the useful move is not " +
                    "interrogation. It is staying nearby, being easy to be around, and asking again tomorrow.",
            ),
        ),
    )

    private val redFlags = Article(
        id = "red-flags",
        title = "When it is worth seeing a doctor",
        subtitle = "The things that get normalised and should not be",
        emoji = "🩺",
        minutes = 3,
        blocks = listOf(
            Block.Para(
                "You are not her doctor and this app is not a diagnostic tool. But a lot of genuinely treatable " +
                    "problems go unmentioned for years because they get filed under 'periods are just like that'. " +
                    "These are worth raising.",
            ),
            Block.Head("Bleeding"),
            Block.Bullets(
                listOf(
                    "Soaking through a pad or tampon every hour for several hours in a row.",
                    "Clots bigger than a ten pence coin, or flooding through clothes and bedding.",
                    "Bleeding lasting more than seven days.",
                    "Bleeding between periods, or after sex.",
                    "Periods stopping for three months or more, with pregnancy ruled out.",
                ),
            ),
            Block.Head("Pain"),
            Block.Bullets(
                listOf(
                    "Pain that stops her working, studying or sleeping.",
                    "Pain that anti-inflammatory painkillers do not touch.",
                    "Pain during or after sex.",
                    "Pain that has clearly worsened over the years.",
                ),
            ),
            Block.Head("Cycle pattern"),
            Block.Bullets(
                listOf(
                    "Consistently shorter than 21 days or longer than 35.",
                    "A sudden, sustained change from her own normal.",
                    "Severe mood symptoms every cycle that lift once bleeding starts -- possible PMDD, and treatable.",
                ),
            ),
            Block.Note(
                title = "Practical help",
                text = "Heavy periods cause iron deficiency, and iron deficiency causes exhaustion -- a blood test " +
                    "settles it. Endometriosis takes years to diagnose on average, mostly because severe pain gets " +
                    "dismissed. Going with her, or just keeping a symptom record she can hand over, does more than " +
                    "sympathy does.",
                tone = NoteTone.WARNING,
            ),
        ),
    )

    private val tracking = Article(
        id = "tracking",
        title = "Tracking it properly",
        subtitle = "How to make the predictions actually accurate",
        emoji = "📈",
        minutes = 3,
        blocks = listOf(
            Block.Head("The one thing that matters most"),
            Block.Para(
                "Log the first day of proper bleeding, every time. One date per cycle. Three or four logged " +
                    "cycles and the app stops using your estimate and starts using her real average -- along with " +
                    "how much it varies, which is just as useful.",
            ),
            Block.Head("What not to log as day 1"),
            Block.Bullets(
                listOf(
                    "Spotting the day before. That is not day 1.",
                    "Mid-cycle spotting around ovulation. Logging that shifts every prediction by two weeks.",
                    "A withdrawal bleed on a continuous method, which is not on a cycle at all.",
                ),
            ),
            Block.Head("If you want real accuracy"),
            Block.Para(
                "Calendar maths can only ever be an estimate. If she is interested, the things that actually pin " +
                    "ovulation down are cervical mucus changes, a daily basal temperature reading before getting " +
                    "out of bed, and LH test strips. Ovulation pain, if she gets it reliably, is nearly as good and " +
                    "costs nothing.",
            ),
            Block.Head("When predictions will be unreliable, whatever you do"),
            Block.Bullets(
                listOf(
                    "The first year or two after periods start, and the years approaching menopause.",
                    "After childbirth and while breastfeeding.",
                    "On methods that suppress ovulation or make bleeding irregular.",
                    "During illness, heavy training, big weight change, shift work or serious stress -- all of which move ovulation.",
                    "With PCOS, thyroid problems, or any condition that makes cycles irregular.",
                ),
            ),
        ),
    )

    private val intimacy = Article(
        id = "intimacy",
        title = "Sex across the cycle",
        subtitle = "Lubrication, the cervix, and which days are physically easiest",
        emoji = "💗",
        minutes = 5,
        blocks = listOf(
            Block.Para(
                "The vagina is not the same environment all month. Estrogen changes how much blood reaches " +
                    "the tissue, how thick and elastic the walls are, how much natural lubrication there is, and " +
                    "where the cervix sits. Those are physical facts with practical consequences, and almost no " +
                    "one gets taught them.",
            ),
            Block.Head("What estrogen does"),
            Block.Para(
                "Rising estrogen increases blood flow to the whole area, thickens the vaginal walls and makes " +
                    "them more elastic, and drives the fluid that provides natural lubrication. So the tissue is " +
                    "at its plumpest and most forgiving when estrogen peaks, just before ovulation, and at its " +
                    "thinnest and driest when estrogen bottoms out during the period. The premenstrual days are " +
                    "the other low point, because both hormones are falling away at once.",
            ),
            Block.Head("The cervix moves, and it changes texture"),
            Block.Para(
                "This is the part that surprises most people. Across the cycle the cervix physically travels up " +
                    "and down and changes how it feels. Away from ovulation it sits low, feels firm and stays " +
                    "closed. As ovulation approaches it rises, softens and opens slightly. The comparison used in " +
                    "fertility awareness is that it goes from feeling like the tip of your nose to feeling like " +
                    "your lips.",
            ),
            Block.Bullets(
                listOf(
                    "High, soft, open, and wet: around ovulation. Most room, most lubrication, most comfortable for deep penetration.",
                    "Low, firm, closed, and dry: during the period and the premenstrual days. Least room, so deep penetration is the most likely to bump the cervix and hurt.",
                ),
            ),
            Block.Head("So which days are physically easiest"),
            Block.Numbered(
                listOf(
                    "The fertile window and ovulation -- roughly the five days before ovulation plus the day " +
                        "after. Maximum lubrication, plumpest and most elastic tissue, cervix high and out of the " +
                        "way, and the one stretch where desire has a hormonal push behind it. Also the days " +
                        "pregnancy is possible, so decide about contraception on purpose.",
                    "The follicular phase, after bleeding stops. Improving steadily, comfortable, unhurried.",
                    "The early luteal phase. Fine, but noticeably drier as the mucus turns thick and scant. " +
                        "Lubricant is genuinely useful here.",
                    "The premenstrual days and days one to two of the period. The hardest: driest, most tender, " +
                        "cervix low, and desire usually at its lowest.",
                ),
            ),
            Block.Note(
                title = "Lubricant is not an insult",
                text = "Natural lubrication tracks estrogen, not how attractive you are or how much she wants " +
                    "you. On most days of the month there is simply less of it, and on hormonal contraception " +
                    "there can be less of it all the time. Treating lubricant as a normal part of sex rather than " +
                    "a remedy for a problem removes a conversation neither of you enjoys.",
            ),
            Block.Note(
                title = "This is physiology, not permission",
                tone = NoteTone.GOLDEN,
                text = "None of the above predicts whether she wants to have sex, and a favourable day on a " +
                    "calendar is not an argument to make. Plenty of people want sex least when the app says the " +
                    "body is most ready, and most during their period when it says the opposite. Desire is hers " +
                    "to state and it outranks every number in this app. If you ever find yourself citing the app " +
                    "to talk her into something, close it.",
            ),
            Block.Head("When pain means something"),
            Block.Para(
                "Occasional discomfort with a low cervix or on a dry day is ordinary. Sex that hurts regularly " +
                    "is not, and it is not something to push through or work around quietly. Persistent pain has " +
                    "real, treatable causes -- endometriosis, fibroids, infections, vaginismus, or low estrogen " +
                    "from a contraceptive method, breastfeeding or perimenopause. Deep pain that follows the cycle, " +
                    "or pain that has been getting worse, is worth a doctor rather than a change of position.",
            ),
            Block.Head("If she is on hormonal contraception"),
            Block.Para(
                "Steady hormones mean the cervix and the mucus stay roughly static all month, so none of the " +
                    "cyclical cues above apply and there is nothing to time. Some methods also lower lubrication " +
                    "and libido persistently as a side effect. That is a known effect of the method, not a fact " +
                    "about her, and switching method often resolves it.",
            ),
        ),
    )

    val articles: List<Article> = listOf(
        basics, hormones, ovulation, intimacy, pms, pain, contraception, mistakes, talking, redFlags,
        tracking,
    )

    fun byId(id: String): Article? = articles.firstOrNull { it.id == id }

    const val DISCLAIMER =
        "Cycle Follower is an educational tool, not a medical device and not contraception. Everything it " +
            "describes is a typical pattern -- individual variation is enormous, and plenty of people do not " +
            "follow these patterns at all. It cannot diagnose anything, it cannot tell you how she actually " +
            "feels, and it must never be used to avoid or achieve a pregnancy. For anything medical, ask a doctor. " +
            "For how she feels, ask her."
}
