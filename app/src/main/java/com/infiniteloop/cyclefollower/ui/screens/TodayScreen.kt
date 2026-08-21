package com.infiniteloop.cyclefollower.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.infiniteloop.cyclefollower.data.Symptom
import com.infiniteloop.cyclefollower.data.UserProfile
import com.infiniteloop.cyclefollower.domain.Briefings
import com.infiniteloop.cyclefollower.domain.CycleEngine
import com.infiniteloop.cyclefollower.domain.CyclePhase
import com.infiniteloop.cyclefollower.domain.PhaseGuides
import com.infiniteloop.cyclefollower.ui.AppViewModel
import com.infiniteloop.cyclefollower.ui.components.BulletList
import com.infiniteloop.cyclefollower.ui.components.Callout
import com.infiniteloop.cyclefollower.ui.components.CalloutTone
import com.infiniteloop.cyclefollower.ui.components.CycleRing
import com.infiniteloop.cyclefollower.ui.components.LevelMeter
import com.infiniteloop.cyclefollower.ui.components.PastDatePickerDialog
import com.infiniteloop.cyclefollower.ui.components.Pill
import com.infiniteloop.cyclefollower.ui.components.SectionCard
import com.infiniteloop.cyclefollower.ui.theme.phasePalette
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val headerFormat = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.UK)

@Composable
fun TodayScreen(
    profile: UserProfile,
    viewModel: AppViewModel,
    onOpenLearn: () -> Unit,
) {
    val today = remember { LocalDate.now() }
    val status = remember(profile, today) { CycleEngine.status(profile, today) }
    val briefing = remember(profile, status) { Briefings.build(profile, status, today) }
    val dark = isSystemInDarkTheme()

    var showLogDialog by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                Text(
                    today.format(headerFormat),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    if (profile.partnerName.isBlank()) "Today" else "${profile.partnerName} today",
                    style = MaterialTheme.typography.displaySmall,
                )
            }
        }

        if (status == null) {
            item {
                SectionCard(title = "One thing missing") {
                    Text(briefing.summary, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = { showDatePicker = true }) {
                        Text("Add her last period date")
                    }
                }
            }
        } else {
            val palette = phasePalette(status.phase, dark)
            val guide = PhaseGuides.of(status.phase)

            item {
                CycleRing(
                    status = status,
                    centerTop = if (status.isLate) "Cycle day" else "Cycle day",
                    centerMain = status.cycleDay.toString(),
                    centerBottom = "${guide.emoji} ${guide.title}",
                    modifier = Modifier.fillMaxWidth().height(230.dp),
                )
            }

            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(palette.container, RoundedCornerShape(20.dp))
                        .padding(16.dp),
                ) {
                    Text(
                        briefing.dayLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.onContainer,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        briefing.moodBanner,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.onContainer,
                    )
                    briefing.timingLine?.let {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.onContainer,
                        )
                    }
                }
            }

            briefing.warning?.let { warning ->
                item { Callout(text = warning, tone = CalloutTone.WARNING, title = "Worth knowing") }
            }

            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Pill(
                        text = "Accuracy: ${status.confidence.displayName}",
                        background = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (status.cyclesTracked > 0) {
                        Pill(
                            text = "${status.cycleLength} day avg  +/- ${status.variabilityDays}",
                            background = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                SectionCard(title = "What is happening in her body") {
                    Text(guide.whatsHappening, style = MaterialTheme.typography.bodyMedium)
                }
            }

            item {
                SectionCard(title = "Where she probably is right now") {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        LevelMeter("Energy", guide.energy, palette.accent)
                        LevelMeter("Patience", guide.patience, palette.accent)
                        LevelMeter("Sex drive", guide.libido, palette.accent)
                        LevelMeter("Social battery", guide.socialBattery, palette.accent)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Typical for this phase, not a reading of her actual mood. She is still a person having a day.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (briefing.likelySymptoms.isNotEmpty()) {
                item {
                    val heading = if (profile.selectedSymptoms.any { status.phase in it.phases }) {
                        "What she gets in this phase"
                    } else {
                        "Common in this phase"
                    }
                    SectionCard(title = heading) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            briefing.likelySymptoms.forEach { symptom ->
                                SymptomRow(symptom)
                            }
                        }
                    }
                }
            }

            item {
                SectionCard(
                    title = "Do this today",
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                ) {
                    BulletList(guide.doThis, marker = "✓")
                }
            }

            item {
                SectionCard(
                    title = "Avoid today",
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                ) {
                    BulletList(guide.avoidThis, marker = "✕", markerColor = MaterialTheme.colorScheme.error)
                }
            }

            if (guide.goodTimeFor.isNotEmpty()) {
                item {
                    SectionCard(title = "Good week for") {
                        BulletList(guide.goodTimeFor, marker = "→")
                    }
                }
            }

            if (status.phase == CyclePhase.FERTILE_WINDOW || status.phase == CyclePhase.OVULATION) {
                item {
                    Callout(
                        title = "These are fertile days",
                        text = "Sex on these days can result in pregnancy. Calendar predictions are not " +
                            "contraception -- ovulation moves around, even in regular cycles. If a pregnancy " +
                            "is not the plan, use an actual method.",
                        tone = CalloutTone.WARNING,
                    )
                }
            }

            item {
                Callout(
                    title = "The one rule",
                    text = "Never hand any of this back to her. \"Is it your period?\" turns a hard day into a " +
                        "fight, because it tells her that her feelings are a symptom instead of a response. " +
                        "Use the app to adjust what you do -- not to explain what she feels.",
                    tone = CalloutTone.GOLDEN,
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { showLogDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Her period started today") }
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("It started on another day") }
                    TextButton(
                        onClick = onOpenLearn,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Read the background on all of this") }
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
                        "of real bleeding -- logging spotting would shift every prediction.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.logPeriodStart(today)
                        showLogDialog = false
                    },
                ) { Text("Log it") }
            },
            dismissButton = { TextButton(onClick = { showLogDialog = false }) { Text("Cancel") } },
        )
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
private fun SymptomRow(symptom: Symptom) {
    var expanded by rememberSaveable(symptom.name) { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                symptom.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
            )
        }
        AnimatedVisibility(expanded) {
            Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(symptom.why, style = MaterialTheme.typography.bodyMedium)
                Text(
                    symptom.help,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
