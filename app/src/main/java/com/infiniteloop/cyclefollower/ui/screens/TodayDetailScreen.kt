package com.infiniteloop.cyclefollower.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.infiniteloop.cyclefollower.data.UserProfile
import com.infiniteloop.cyclefollower.domain.Briefings
import com.infiniteloop.cyclefollower.domain.CycleEngine
import com.infiniteloop.cyclefollower.domain.CyclePhase
import com.infiniteloop.cyclefollower.domain.PhaseGuides
import com.infiniteloop.cyclefollower.ui.AppViewModel
import com.infiniteloop.cyclefollower.ui.components.BoundedDatePickerDialog
import com.infiniteloop.cyclefollower.ui.components.BulletList
import com.infiniteloop.cyclefollower.ui.components.Callout
import com.infiniteloop.cyclefollower.ui.components.CalloutTone
import com.infiniteloop.cyclefollower.ui.components.LevelMeter
import com.infiniteloop.cyclefollower.ui.components.SectionCard
import com.infiniteloop.cyclefollower.ui.theme.phasePalette
import androidx.compose.foundation.isSystemInDarkTheme
import java.time.LocalDate

/**
 * Everything Today used to stack into five collapsed rows. One tap away instead of five boxes
 * on the screen you look at every morning.
 */
@Composable
fun TodayDetailScreen(profile: UserProfile, viewModel: AppViewModel, onBack: () -> Unit) {
    val today = remember { LocalDate.now() }
    val status = remember(profile, today) { CycleEngine.status(profile, today) }
    val briefing = remember(profile, status) { Briefings.build(profile, status, today) }
    val guide = remember(status) { status?.let { PhaseGuides.of(it.phase) } }
    val dark = isSystemInDarkTheme()
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showLogDialog by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            TextButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Back")
            }
        }

        if (status == null || guide == null) {
            item {
                Callout(
                    text = "Add the date her last period started and this fills in.",
                    tone = CalloutTone.INFO,
                )
            }
            return@LazyColumn
        }

        val palette = phasePalette(status.phase, dark)

        item {
            Column {
                Text(guide.title, style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Day ${status.cycleDay} of ${status.cycleLength}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        briefing.warning?.let { warning ->
            item { Callout(text = warning, tone = CalloutTone.WARNING, title = "Worth knowing") }
        }

        item {
            SectionCard(title = "Avoid today") {
                BulletList(guide.avoidThis, marker = "✕", markerColor = MaterialTheme.colorScheme.error)
            }
        }

        item {
            SectionCard(title = "What is happening in her body") {
                Text(guide.whatsHappening, style = MaterialTheme.typography.bodyMedium)
            }
        }

        item {
            SectionCard(title = "Sex and comfort today") {
                Text(guide.intimacy, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Typical physiology, nothing more. It does not tell you whether she wants to, and a " +
                        "good day on the calendar is not an argument.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (briefing.likelySymptoms.isNotEmpty()) {
            item {
                SectionCard(title = "What she may get in this phase") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        briefing.likelySymptoms.forEach { symptom ->
                            Column {
                                Text(
                                    symptom.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    symptom.help,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
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
            }
        }

        item {
            SectionCard(title = "Good week for") { BulletList(guide.goodTimeFor, marker = "→") }
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
                text = "Never hand any of this back to her. \"Is it your period?\" turns a hard day into a " +
                    "fight. Use the app to adjust what you do — not to explain what she feels.",
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
            }
        }
    }

    if (showLogDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showLogDialog = false },
            title = { Text("Log period start") },
            text = {
                Text(
                    "Record today as day 1 of a new cycle. Only log the first day of real bleeding — " +
                        "logging spotting would shift every prediction.",
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
