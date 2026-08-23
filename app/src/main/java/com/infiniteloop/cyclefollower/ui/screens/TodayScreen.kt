package com.infiniteloop.cyclefollower.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infiniteloop.cyclefollower.data.DayLog
import com.infiniteloop.cyclefollower.data.DayMood
import com.infiniteloop.cyclefollower.data.UserProfile
import com.infiniteloop.cyclefollower.domain.Briefings
import com.infiniteloop.cyclefollower.domain.CycleEngine
import com.infiniteloop.cyclefollower.domain.CyclePhase
import com.infiniteloop.cyclefollower.domain.CycleStatus
import com.infiniteloop.cyclefollower.domain.PhaseGuide
import com.infiniteloop.cyclefollower.domain.PhaseGuides
import com.infiniteloop.cyclefollower.ui.AppViewModel
import com.infiniteloop.cyclefollower.ui.components.BulletList
import com.infiniteloop.cyclefollower.ui.components.BoundedDatePickerDialog
import com.infiniteloop.cyclefollower.ui.components.Callout
import com.infiniteloop.cyclefollower.ui.components.CalloutTone
import com.infiniteloop.cyclefollower.ui.components.LevelMeter
import com.infiniteloop.cyclefollower.ui.components.SectionCard
import com.infiniteloop.cyclefollower.ui.theme.phasePalette
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val headerFormat = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.UK)

/**
 * Ordered actionable-first: what to do, what to avoid, then the daily tap, then the reference
 * material behind disclosures. The long endocrinology used to sit above the advice, which is the
 * wrong way round for something checked in ten seconds.
 */
@Composable
fun TodayScreen(
    profile: UserProfile,
    viewModel: AppViewModel,
    onOpenLearn: () -> Unit,
    onOpenRightNow: () -> Unit,
    onOpenLog: () -> Unit,
) {
    val today = remember { LocalDate.now() }
    val status = remember(profile, today) { CycleEngine.status(profile, today) }
    val briefing = remember(profile, status) { Briefings.build(profile, status, today) }
    val dark = isSystemInDarkTheme()

    var showLogDialog by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (status == null) {
            item {
                Column {
                    Text(today.format(headerFormat), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Today", style = MaterialTheme.typography.displaySmall)
                }
            }
            item {
                SectionCard(title = "One thing missing") {
                    Text(briefing.summary, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = { showDatePicker = true }) { Text("Add her last period date") }
                }
            }
        } else {
            val guide = PhaseGuides.of(status.phase)
            val palette = phasePalette(status.phase, dark)

            item { StatusStrip(profile, status, guide, briefing.moodBanner, today) }

            briefing.warning?.let { warning ->
                item { Callout(text = warning, tone = CalloutTone.WARNING, title = "Worth knowing") }
            }

            if (status.phase == CyclePhase.LATE_LUTEAL || status.phase == CyclePhase.MENSTRUAL) {
                item {
                    Button(
                        onClick = onOpenRightNow,
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface,
                            contentColor = MaterialTheme.colorScheme.surface,
                        ),
                    ) { Text("Something is wrong right now") }
                }
            }

            item { Label("Today, three things") }
            items(guide.doThis.take(2).size) { index ->
                ActionRow(guide.doThis[index], Icons.Filled.Check, palette.accent, palette.container)
            }
            item {
                ActionRow(
                    guide.avoidThis.first(),
                    Icons.Filled.Close,
                    MaterialTheme.colorScheme.error,
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
                )
            }

            item { Label("Coming up") }
            item { ComingUp(profile, status) }

            item { Label("How was today?") }
            item { InlineMood(profile, today, viewModel, onOpenLog) }

            item { Label("If you want the detail") }
            item {
                Disclosure("What is happening in her body") {
                    Text(guide.whatsHappening, style = MaterialTheme.typography.bodyMedium)
                }
            }
            item {
                Disclosure("Sex and comfort today") {
                    Text(guide.intimacy, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Typical physiology, nothing more. It does not tell you whether she wants to, " +
                            "and a good day on the calendar is not an argument.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (briefing.likelySymptoms.isNotEmpty()) {
                item {
                    Disclosure("What she may get in this phase") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            briefing.likelySymptoms.forEach { symptom ->
                                Column {
                                    Text(symptom.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.height(2.dp))
                                    Text(symptom.help, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
            item {
                Disclosure("Where she probably is right now") {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        LevelMeter("Energy", guide.energy, palette.accent)
                        LevelMeter("Patience", guide.patience, palette.accent)
                        LevelMeter("Sex drive", guide.libido, palette.accent)
                        LevelMeter("Social battery", guide.socialBattery, palette.accent)
                    }
                }
            }
            item {
                Disclosure("Good week for") { BulletList(guide.goodTimeFor, marker = "→") }
            }

            if (status.phase == CyclePhase.FERTILE_WINDOW || status.phase == CyclePhase.OVULATION) {
                item {
                    Callout(
                        title = "These are fertile days",
                        text = "Sex on these days can result in pregnancy. Calendar predictions are not " +
                            "contraception — ovulation moves, even in regular cycles.",
                        tone = CalloutTone.WARNING,
                    )
                }
            }

            item {
                Callout(
                    title = "The one rule",
                    text = "Never hand any of this back to her. \"Is it your period?\" turns a hard day into " +
                        "a fight. Use the app to adjust what you do — not to explain what she feels.",
                    tone = CalloutTone.GOLDEN,
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { showLogDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Her period started today")
                    }
                    OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("It started on another day")
                    }
                    TextButton(onClick = onOpenLearn, modifier = Modifier.fillMaxWidth()) {
                        Text("Read the background on all of this")
                    }
                }
            }
        }
    }

    if (showLogDialog) {
        AlertDialog(
            onDismissRequest = { showLogDialog = false },
            title = { Text("Log period start") },
            text = {
                Text(
                    "Record ${today.format(headerFormat)} as day 1 of a new cycle. Only log the first day " +
                        "of real bleeding — logging spotting would shift every prediction.",
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.logPeriodStart(today); showLogDialog = false }) { Text("Log it") }
            },
            dismissButton = { TextButton(onClick = { showLogDialog = false }) { Text("Cancel") } },
        )
    }

    if (showDatePicker) {
        BoundedDatePickerDialog(
            initial = profile.lastPeriodStart ?: today,
            onDismiss = { showDatePicker = false },
            onPicked = { viewModel.logPeriodStart(it) },
        )
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text.uppercase(),
        fontSize = 11.sp,
        letterSpacing = 0.9.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp),
    )
}

@Composable
private fun StatusStrip(
    profile: UserProfile,
    status: CycleStatus,
    guide: PhaseGuide,
    mood: String,
    today: LocalDate,
) {
    val dark = isSystemInDarkTheme()
    val palette = phasePalette(status.phase, dark)
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Text(
            today.format(headerFormat),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                Modifier
                    .size(56.dp)
                    .background(palette.container, RoundedCornerShape(16.dp)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    status.cycleDay.toString(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.onContainer,
                )
                Text("DAY", fontSize = 9.sp, letterSpacing = 0.6.sp, fontWeight = FontWeight.Bold, color = palette.onContainer)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("${guide.emoji} ${guide.title}", style = MaterialTheme.typography.titleMedium)
                Text(
                    buildString {
                        if (profile.partnerName.isNotBlank()) append(profile.partnerName).append(" · ")
                        append("day ${status.cycleDay} of ${status.cycleLength}")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        CycleBar(status)
        Spacer(Modifier.height(12.dp))
        Text(mood, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CycleBar(status: CycleStatus) {
    val dark = isSystemInDarkTheme()
    Row(Modifier.fillMaxWidth().height(6.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        status.timeline.forEach { span ->
            val isNow = status.cycleDay in span
            Box(
                Modifier
                    .weight(span.length.toFloat())
                    .fillMaxHeight()
                    .background(
                        phasePalette(span.phase, dark).accent.copy(alpha = if (isNow) 1f else 0.35f),
                        RoundedCornerShape(3.dp),
                    ),
            )
        }
    }
}

@Composable
private fun ActionRow(text: String, icon: ImageVector, accent: Color, container: Color) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier.size(32.dp).background(container, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ComingUp(profile: UserProfile, status: CycleStatus) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile(
            if (profile.contraception.bleedIsWithdrawal) "Bleed" else "Period",
            if (!status.bleedingPredictable) "not on a schedule"
            else if (status.isLate) "${status.daysLate} d late"
            else if (status.daysUntilNextPeriod == 0) "today"
            else "in ${status.daysUntilNextPeriod} d",
            Modifier.weight(1f),
        )
        StatTile(
            "Ovulation",
            when {
                status.ovulationSuppressed -> "none"
                status.daysUntilOvulation > 0 -> "in ${status.daysUntilOvulation} d"
                status.daysUntilOvulation == 0 -> "today"
                else -> "passed"
            },
            Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .padding(13.dp),
    ) {
        Text(
            label.uppercase(),
            fontSize = 11.sp,
            letterSpacing = 0.8.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun InlineMood(profile: UserProfile, today: LocalDate, viewModel: AppViewModel, onOpenLog: () -> Unit) {
    val existing = profile.logFor(today)
    val dark = isSystemInDarkTheme()
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .padding(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(DayMood.ROUGH, Icons.Filled.SentimentDissatisfied, existing?.mood == DayMood.ROUGH,
                if (dark) Color(0xFFF0A97C) else Color(0xFFB35309), Modifier.weight(1f)) {
                viewModel.update { it.withDayLog(DayLog(today, DayMood.ROUGH, existing?.tags ?: emptySet())) }
            }
            Chip(DayMood.NORMAL, Icons.Filled.SentimentNeutral, existing?.mood == DayMood.NORMAL,
                MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f)) {
                viewModel.update { it.withDayLog(DayLog(today, DayMood.NORMAL, existing?.tags ?: emptySet())) }
            }
            Chip(DayMood.GOOD, Icons.Filled.SentimentSatisfied, existing?.mood == DayMood.GOOD,
                if (dark) Color(0xFF8CD9A3) else Color(0xFF2E7D4F), Modifier.weight(1f)) {
                viewModel.update { it.withDayLog(DayLog(today, DayMood.GOOD, existing?.tags ?: emptySet())) }
            }
        }
        Spacer(Modifier.height(6.dp))
        TextButton(onClick = onOpenLog, modifier = Modifier.fillMaxWidth()) {
            Text(if (existing == null) "Add detail and see what it has learned" else "Add what you noticed")
        }
    }
}

@Composable
private fun Chip(mood: DayMood, icon: ImageVector, selected: Boolean, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .background(if (selected) accent.copy(alpha = 0.16f) else Color.Transparent, RoundedCornerShape(12.dp))
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) accent else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = mood.label, tint = accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(4.dp))
        Text(mood.label, style = MaterialTheme.typography.bodyMedium, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun Disclosure(title: String, content: @Composable () -> Unit) {
    var open by rememberSaveable(title) { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .clickable { open = !open }
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Icon(
                if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (open) "Collapse" else "Expand",
            )
        }
        AnimatedVisibility(open) {
            Column(Modifier.padding(top = 10.dp)) { content() }
        }
    }
}
