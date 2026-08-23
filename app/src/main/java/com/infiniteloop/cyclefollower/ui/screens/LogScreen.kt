package com.infiniteloop.cyclefollower.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.infiniteloop.cyclefollower.data.DayLog
import com.infiniteloop.cyclefollower.data.DayMood
import com.infiniteloop.cyclefollower.data.Symptom
import com.infiniteloop.cyclefollower.data.UserProfile
import com.infiniteloop.cyclefollower.domain.CycleEngine
import com.infiniteloop.cyclefollower.domain.Personalisation
import com.infiniteloop.cyclefollower.ui.AppViewModel
import com.infiniteloop.cyclefollower.ui.components.SectionCard
import java.time.LocalDate

/**
 * The only measurement the app ever gets. She never opens it and logs nothing, so three taps a
 * day from him is the entire dataset behind everything personalised.
 */
@Composable
fun LogScreen(profile: UserProfile, viewModel: AppViewModel) {
    val today = remember { LocalDate.now() }
    val existing = profile.logFor(today)
    val status = remember(profile, today) { CycleEngine.status(profile, today) }
    val personal = remember(profile) { Personalisation.of(profile) }
    val dark = isSystemInDarkTheme()

    // Offer the symptoms she is known for first, then whatever fits the phase she is in.
    val suggested = remember(profile, status) {
        val hers = profile.selectedSymptoms
        val phaseOnes = status?.let { Symptom.forPhase(it.phase) }.orEmpty()
        (hers + phaseOnes).distinct().take(10)
    }

    fun setMood(mood: DayMood) {
        val tags = existing?.tags ?: emptySet()
        viewModel.update { it.withDayLog(DayLog(today, mood, tags)) }
    }

    fun toggleTag(symptom: Symptom) {
        val current = existing ?: DayLog(today, DayMood.NORMAL)
        val tags = if (symptom.name in current.tags) current.tags - symptom.name else current.tags + symptom.name
        viewModel.update { it.withDayLog(current.copy(tags = tags)) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                Text("How was today?", style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    "One tap. You are the only thing this app can measure, so this is what makes it " +
                        "about her instead of an average.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MoodButton(
                    mood = DayMood.ROUGH, icon = Icons.Filled.SentimentDissatisfied,
                    selected = existing?.mood == DayMood.ROUGH,
                    accent = if (dark) Color(0xFFF0A97C) else Color(0xFFB35309),
                    container = if (dark) Color(0xFF64330E) else Color(0xFFFDE3D1),
                    modifier = Modifier.weight(1f),
                ) { setMood(DayMood.ROUGH) }
                MoodButton(
                    mood = DayMood.NORMAL, icon = Icons.Filled.SentimentNeutral,
                    selected = existing?.mood == DayMood.NORMAL,
                    accent = MaterialTheme.colorScheme.onSurfaceVariant,
                    container = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f),
                ) { setMood(DayMood.NORMAL) }
                MoodButton(
                    mood = DayMood.GOOD, icon = Icons.Filled.SentimentSatisfied,
                    selected = existing?.mood == DayMood.GOOD,
                    accent = if (dark) Color(0xFF8CD9A3) else Color(0xFF2E7D4F),
                    container = if (dark) Color(0xFF1E4A2C) else Color(0xFFDDF2E4),
                    modifier = Modifier.weight(1f),
                ) { setMood(DayMood.GOOD) }
            }
        }

        if (existing != null) {
            item { TagPicker(suggested, existing.tags, ::toggleTag) }
        }

        item {
            SectionCard(
                title = "What it has worked out",
                containerColor = if (personal.hasEnoughData) {
                    (if (dark) Color(0xFF1E4A2C) else Color(0xFFDDF2E4))
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    personal.insights.forEach { line ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                "•",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.size(width = 18.dp, height = 20.dp),
                            )
                            Text(line, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        item {
            SectionCard(title = "Why it is worth the tap") {
                Text(
                    "With nothing coming from her, these taps are all the app has. After a couple of " +
                        "cycles it stops using the textbook PMS window and starts using hers — which is " +
                        "often a day or two out from the default, and that is the difference between " +
                        "being ready and being caught out.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun MoodButton(
    mood: DayMood,
    icon: ImageVector,
    selected: Boolean,
    accent: Color,
    container: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .background(if (selected) container else MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) accent else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(18.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(8.dp))
        Text(
            mood.label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagPicker(options: List<Symptom>, selected: Set<String>, onToggle: (Symptom) -> Unit) {
    SectionCard(title = "Anything you noticed") {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            options.forEach { symptom ->
                val on = symptom.name in selected
                Box(
                    Modifier
                        .background(
                            if (on) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(999.dp),
                        )
                        .border(
                            1.dp,
                            if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(999.dp),
                        )
                        .clickable { onToggle(symptom) }
                        .padding(horizontal = 13.dp, vertical = 7.dp),
                ) {
                    Text(
                        symptom.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (on) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
