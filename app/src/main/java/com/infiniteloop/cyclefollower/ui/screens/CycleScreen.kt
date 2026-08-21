package com.infiniteloop.cyclefollower.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.infiniteloop.cyclefollower.data.UserProfile
import com.infiniteloop.cyclefollower.domain.Briefings
import com.infiniteloop.cyclefollower.domain.CycleEngine
import com.infiniteloop.cyclefollower.domain.CyclePhase
import com.infiniteloop.cyclefollower.ui.AppViewModel
import com.infiniteloop.cyclefollower.ui.components.Callout
import com.infiniteloop.cyclefollower.ui.components.CalloutTone
import com.infiniteloop.cyclefollower.ui.components.PastDatePickerDialog
import com.infiniteloop.cyclefollower.ui.components.PhaseLegendRow
import com.infiniteloop.cyclefollower.ui.components.SectionCard
import com.infiniteloop.cyclefollower.ui.components.shortLabel
import com.infiniteloop.cyclefollower.ui.theme.phasePalette
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.UK)

@Composable
fun CycleScreen(profile: UserProfile, viewModel: AppViewModel) {
    val today = remember { LocalDate.now() }
    val status = remember(profile, today) { CycleEngine.status(profile, today) }
    val dark = isSystemInDarkTheme()

    var monthOffset by rememberSaveable { mutableStateOf(0) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val shownMonth = remember(monthOffset) { YearMonth.from(today).plusMonths(monthOffset.toLong()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Her cycle", style = MaterialTheme.typography.displaySmall)
        }

        if (status == null) {
            item {
                Callout(
                    title = "Nothing to show yet",
                    text = "Add the first day of her most recent period and the calendar fills in.",
                    tone = CalloutTone.INFO,
                )
            }
            item {
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Add a period start date")
                }
            }
        } else {
            item {
                SectionCard {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { monthOffset-- }) {
                            Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous month")
                        }
                        Text(
                            shownMonth.atDay(1).format(monthFormat),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        IconButton(onClick = { monthOffset++ }) {
                            Icon(Icons.Filled.ChevronRight, contentDescription = "Next month")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    MonthGrid(profile = profile, month = shownMonth, today = today, dark = dark)
                    Spacer(Modifier.height(12.dp))
                    CalendarKey(dark)
                }
            }

            item {
                SectionCard(title = "This cycle, phase by phase") {
                    PhaseLegendRow(status)
                }
            }

            item {
                SectionCard(title = "Dates worth knowing") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        status.fertileWindow?.let { (start, end) ->
                            KeyDate(
                                "Fertile window",
                                "${start.shortLabel()} to ${end.shortLabel()}",
                                if (status.inFertileWindow) "Now" else null,
                            )
                        }
                        status.ovulationDate?.let {
                            KeyDate(
                                "Ovulation, roughly",
                                it.shortLabel(),
                                if (it.isBefore(today)) "Passed" else null,
                            )
                        }
                        if (status.bleedingPredictable) {
                            KeyDate(
                                if (profile.contraception.bleedIsWithdrawal) "Next withdrawal bleed" else "Next period",
                                status.nextPeriodStart.shortLabel(),
                                if (status.variabilityDays > 0) {
                                    "give or take ${status.variabilityDays} d"
                                } else {
                                    null
                                },
                            )
                        }
                        status.timeline.firstOrNull { it.phase == CyclePhase.LATE_LUTEAL }?.let { span ->
                            KeyDate(
                                "PMS window opens",
                                status.dateOfCycleDay(span.startDay).shortLabel(),
                                if (status.cycleDay >= span.startDay) "Open now" else null,
                            )
                        }
                    }
                }
            }

            item {
                SectionCard(title = "How solid these predictions are") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatRow("Cycles logged", status.cyclesTracked.toString())
                        StatRow("Average cycle length", "${status.cycleLength} days")
                        StatRow(
                            "How much it varies",
                            if (status.cyclesTracked < 2) "not enough data yet"
                            else "+/- ${status.variabilityDays} days",
                        )
                        StatRow("Accuracy", status.confidence.displayName)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        status.confidence.explanation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (status.cyclesTracked < 3 && profile.contraception.hasNaturalCycle) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Log a few more past period start dates and the app switches from your estimate to " +
                                "her real average.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        item {
            SectionCard(title = "Logged periods") {
                if (profile.periodStarts.isEmpty()) {
                    Text("Nothing logged yet.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val sorted = profile.periodStarts.sortedDescending()
                        sorted.forEachIndexed { index, date ->
                            val gap = sorted.getOrNull(index + 1)?.let {
                                java.time.temporal.ChronoUnit.DAYS.between(it, date).toInt()
                            }
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(date.shortLabel(), style = MaterialTheme.typography.bodyLarge)
                                    if (gap != null) {
                                        Text(
                                            "$gap day cycle",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                IconButton(onClick = { viewModel.removePeriodStart(date) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Remove $date")
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Add a period start date")
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Log only the first day of proper bleeding. Adding three or four past cycles is the single " +
                        "biggest accuracy upgrade you can give the app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            SectionCard(title = "How the maths works") {
                Text(
                    "Day 1 is the first day of bleeding. The stretch from ovulation to the next period is the " +
                        "stable part of the cycle -- almost always 12 to 14 days regardless of total length -- so " +
                        "the app finds ovulation by counting 14 days back from the next expected period, not by " +
                        "assuming day 14. The fertile window is the five days before ovulation plus the day after, " +
                        "because sperm can survive that long while the egg lasts under a day.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }

    if (showDatePicker) {
        PastDatePickerDialog(
            initial = profile.lastPeriodStart ?: today,
            onDismiss = { showDatePicker = false },
            onPicked = { viewModel.logPeriodStart(it) },
        )
    }
}

@Composable
private fun MonthGrid(profile: UserProfile, month: YearMonth, today: LocalDate, dark: Boolean) {
    val firstOfMonth = month.atDay(1)
    // Monday-first grid.
    val leadingBlanks = (firstOfMonth.dayOfWeek.value + 6) % 7
    val totalCells = leadingBlanks + month.lengthOfMonth()
    val rows = (totalCells + 6) / 7

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        repeat(rows) { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(7) { column ->
                    val cellIndex = row * 7 + column
                    val dayOfMonth = cellIndex - leadingBlanks + 1
                    if (dayOfMonth < 1 || dayOfMonth > month.lengthOfMonth()) {
                        Box(Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val date = month.atDay(dayOfMonth)
                        DayCell(profile, date, today, dark, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    profile: UserProfile,
    date: LocalDate,
    today: LocalDate,
    dark: Boolean,
    modifier: Modifier,
) {
    val info = remember(profile, date) { CycleEngine.dayInfo(profile, date) }
    val palette = info?.let { phasePalette(it.phase, dark) }
    val isToday = date == today

    Box(
        modifier
            .aspectRatio(1f)
            .background(
                palette?.container ?: Color.Transparent,
                RoundedCornerShape(10.dp),
            )
            .then(
                if (isToday) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(10.dp))
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = palette?.onContainer ?: MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (info != null && !info.predicted) {
                Box(
                    Modifier
                        .size(4.dp)
                        .background(palette!!.accent, RoundedCornerShape(50)),
                )
            }
        }
    }
}

@Composable
private fun CalendarKey(dark: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(
                CyclePhase.MENSTRUAL,
                CyclePhase.FOLLICULAR,
                CyclePhase.FERTILE_WINDOW,
            ).forEach { KeyChip(it, dark, Modifier.weight(1f)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(
                CyclePhase.OVULATION,
                CyclePhase.EARLY_LUTEAL,
                CyclePhase.LATE_LUTEAL,
            ).forEach { KeyChip(it, dark, Modifier.weight(1f)) }
        }
        Text(
            "A dot means the cycle was measured from real logged dates. No dot means it is projected forward.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun KeyChip(phase: CyclePhase, dark: Boolean, modifier: Modifier) {
    val palette = phasePalette(phase, dark)
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .background(palette.accent, RoundedCornerShape(50)),
        )
        Spacer(Modifier.size(6.dp))
        Text(
            phase.shortName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun KeyDate(label: String, value: String, badge: String?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        }
        if (badge != null) {
            Text(
                badge,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
