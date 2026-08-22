# Cycle Follower

An Android app that turns one date into an answer to the question *"what is going on with her this
week, and what should I actually do about it?"*

It is built for the partner, not the person having the cycle. You put in when her last period
started plus a few things about her, and it tells you where she is in her cycle, what her hormones
are doing, what she is likely to be feeling, and — the part that matters — what to do and what not
to do today.

---

## Get the app

**[Download the APK from the Releases page →](https://github.com/InfiniteLoopAI/cycle-follower/releases)**

1. Open that page **on the phone** and tap the newest `.apk` file.
2. Android will ask whether to allow installs from your browser. Allow it, then tap **Install**.
3. Open Cycle Follower and answer the eight setup questions. Only one of them is required.

Builds from `main` are published as normal releases, so once this is merged
[`/releases/latest`](https://github.com/InfiniteLoopAI/cycle-follower/releases/latest) is the
permanent link. Builds from any other branch are published as **pre-releases** under a rolling
`dev-<branch>` tag — GitHub deliberately excludes pre-releases from `/releases/latest`, so use the
full Releases list above until a `main` build exists.

No Google Play, no account, no sign-in. Every build is signed with the same key, so a newer
version installs straight over an older one.

---

## What it does

**Today** — a ring showing the whole cycle with today marked on it, the phase she is in, and a
plain-language read on her likely energy, patience, sex drive and social battery. Then the useful
part: what to do today, what to avoid today, and what this week is a good week for.

**Cycle** — a colour-coded month calendar, the phase-by-phase breakdown of the current cycle, the
dates worth knowing (fertile window, ovulation, next period, when the PMS window opens), and an
honest read on how much to trust any of it.

**Learn** — the reference section. Eleven articles covering the cycle end to end, what each hormone
actually does, how ovulation and fertility really work, sex across the cycle, PMS versus PMDD,
period pain and what genuinely helps, how contraception changes everything, the mistakes men make,
how to talk about it, and when something is worth seeing a doctor about.

**Sex and comfort** — a per-phase read on what is physically going on: how much natural lubrication
there is, how plump and elastic the tissue is, and where the cervix is sitting and how soft it is
(it rises and softens around ovulation, and sits low and firm during the period and the PMS days).
That maps directly onto which days are physically easiest and which are most likely to be
uncomfortable. It says plainly that this is physiology and not permission — it never predicts
whether she wants to, and a good day on the calendar is not an argument.

**Daily notification** — one message each morning with the cycle day, a short read on her likely
mood, and one concrete thing to do. Time is configurable.

**Home screen widget** — cycle day, phase and the one-line mood read, without opening anything.

**Discreet mode** — strips the detail out of the notification and the widget so nothing personal
shows on a lock screen someone else might glance at.

---

## Why it asks for more than a date

A date alone gives you a bad answer. These are the inputs that change the output, and why:

| Input | Why it matters |
|---|---|
| **Past period start dates** | With three or four logged, the app switches from your estimate to her real average cycle length and works out how much it varies month to month. This is the single biggest accuracy upgrade available. |
| **Cycle and period length** | Ovulation is found by counting back from the next period, so total length moves everything. Assuming 28 days when she runs 33 puts the fertile window out by the better part of a week. |
| **Contraception** | The big one. On the combined pill, implant or injection she does **not** ovulate — there is no fertile window, no estrogen peak, no progesterone rise, and the bleed on the pill is a withdrawal bleed rather than a real period. The app says something different rather than inventing a cycle that is not running. On a hormonal IUD or mini pill ovulation may or may not happen, and the app says that too instead of guessing. |
| **PMS severity and PMDD** | Sets how many days ahead the premenstrual window opens (nothing at all through to ten days) and how strongly the advice is worded. |
| **Her actual symptoms** | The app then only mentions the things she really gets, in the phase where they turn up, with what is behind each one and something concrete you can do — instead of describing an average woman. |

## How the predictions work

Day 1 is the first day of proper bleeding. Everything counts from there.

The stretch from ovulation to the next period is the stable part of the cycle — almost always 12 to
14 days regardless of total length — while the first half is what stretches and shrinks. So the app
finds ovulation by counting **14 days back from the next expected period**, not by assuming day 14.
On a 32-day cycle that is day 18; on a 25-day cycle it is day 11.

The fertile window is the five days before ovulation plus the day after, because sperm survive up to
five days in fertile cervical mucus while the egg lasts under a day.

Past cycles on the calendar are drawn using their real measured length, taken from the gap between
the two logged periods either side of them. Only future dates are projections, and they are marked
as such.

The app tells you how much to trust it — good, rough, or loose — based on how many cycles are logged
and how much they vary. When the period is late, or the last logged date has gone stale, it says so
instead of quietly returning a wrong phase.

---

## Privacy

There is **no `INTERNET` permission in the manifest**, so the app is technically incapable of
sending anything anywhere. No accounts, no analytics, no adverts, no cloud. Everything lives in one
file in the app's private storage, and uninstalling deletes it.

---

## The one rule

The app exists to change what *you* do. The moment it becomes a way to explain her feelings back to
her — *"is it your period?"* — it has made things worse than knowing nothing at all. It says this on
the first setup screen and again on the Today screen, deliberately.

It is also not a medical device and not contraception. It cannot diagnose anything, it cannot tell
you how she actually feels, and the day numbers must never be used to avoid or achieve a pregnancy.
For anything medical, a doctor. For how she feels, ask her.

---

## Building it yourself

Needs JDK 17 and the Android SDK (compileSdk 35).

```bash
./gradlew testDebugUnitTest    # 28 unit tests over the prediction engine
./gradlew assembleRelease      # -> app/build/outputs/apk/release/app-release.apk
```

`.github/workflows/build-apk.yml` does the same on every push and publishes the APK to Releases.

### About the signing key

`signing/cycle-follower.jks` is committed to this repository, with the password `cyclefollower`.
That is deliberate, and it is a trade-off worth understanding:

- **Why it is there.** CI generates a fresh debug key on every run otherwise, which changes the APK
  signature every build — and Android refuses to install an update signed with a different key.
  A fixed key is what makes "download the new APK and tap install" work.
- **What it is not.** It is a self-signed key for sideloaded builds, not a Play Store upload key. It
  proves nothing about who built the APK, and anyone with this repository could sign an APK that
  Android would accept as an update to this app. That is fine for an app you install on your own
  phone from your own repository, and not fine if you ever distribute it more widely.
- **How to replace it.** Generate your own keystore and add four repository secrets —
  `KEYSTORE_BASE64` (the `.jks` base64-encoded), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
  The workflow picks them up automatically and ignores the committed one. Note that switching keys
  means uninstalling the app once before the next update will install.

---

## Layout

```
app/src/main/java/com/infiniteloop/cyclefollower/
├── data/          profile, contraception model, symptom knowledge, storage
├── domain/        CycleEngine (the maths), PhaseGuides + Library (the knowledge base), Briefings
├── ui/            Compose screens: Today, Cycle, Learn, Settings, and the setup wizard
├── notify/        daily alarm, notification, reboot handling
└── widget/        home screen widget
```

The prediction engine is pure Kotlin with no Android dependencies, which is why it can be covered by
ordinary unit tests — including one that checks every combination of cycle length, period length,
PMS severity and contraceptive method produces a phase timeline with no gaps, no overlaps and no
missing days.
