package com.infiniteloop.cyclefollower.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infiniteloop.cyclefollower.data.DayLog
import com.infiniteloop.cyclefollower.data.DayMood
import com.infiniteloop.cyclefollower.data.UserProfile
import com.infiniteloop.cyclefollower.domain.Briefings
import com.infiniteloop.cyclefollower.domain.CycleEngine
import com.infiniteloop.cyclefollower.domain.CyclePhase
import com.infiniteloop.cyclefollower.ui.AppViewModel
import com.infiniteloop.cyclefollower.ui.components.CycleRing
import com.infiniteloop.cyclefollower.ui.theme.phasePalette
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val headerFormat = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.UK)

/**
 * One focal point, one read, two things to do.
 *
 * An earlier pass stacked labels, bordered rows, stat tiles and five collapsed sections onto this
 * screen. Everything that is not today's read or today's advice now lives one tap away, or in the
 * tab that already owns it.
 */
@Composable
fun TodayScreen(
    profile: UserProfile,
    viewModel: AppViewModel,
    onOpenRightNow: () -> Unit,
    onOpenDetail: () -> Unit,
) {
    val today = remember { LocalDate.now() }
    val status = remember(profile, today) { CycleEngine.status(profile, today) }
    val briefing = remember(profile, status) { Briefings.build(profile, status, today) }
    val dark = isSystemInDarkTheme()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 22.dp, end = 22.dp, top = 24.dp, bottom = 24.dp),
    ) {
        Text(
            today.format(headerFormat),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (status == null) {
            Spacer(Modifier.height(20.dp))
            Text("Today", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(14.dp))
            Text(briefing.summary, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(22.dp))
            Button(onClick = onOpenDetail) { Text("Add her last period date") }
            return@Column
        }

        val palette = phasePalette(status.phase, dark)
        val hardPhase = status.phase == CyclePhase.LATE_LUTEAL || status.phase == CyclePhase.MENSTRUAL

        Spacer(Modifier.height(6.dp))
        CycleRing(
            status = status,
            centerTop = "Cycle day",
            centerMain = status.cycleDay.toString(),
            centerBottom = status.phase.ringLabel,
            modifier = Modifier.fillMaxWidth().height(240.dp),
        )

        Spacer(Modifier.height(18.dp))
        Text(
            briefing.moodBanner,
            fontSize = 19.sp,
            lineHeight = 27.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        briefing.timingLine?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        briefing.warning?.let { warning ->
            Spacer(Modifier.height(16.dp))
            Text(
                warning,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(Modifier.height(24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            briefing.doNow.take(2).forEach { line ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        Modifier
                            .padding(top = 8.dp)
                            .size(6.dp)
                            .background(palette.accent, RoundedCornerShape(3.dp)),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(line, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        // Only when it is the thing that actually matters today.
        if (status.bleedingPredictable && (status.isLate || status.daysUntilNextPeriod in 0..1)) {
            Spacer(Modifier.height(18.dp))
            TextButton(
                onClick = { viewModel.logPeriodStart(today) },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            ) { Text("Her period started today") }
        }

        Spacer(Modifier.height(28.dp))
        MoodRow(profile, today, viewModel, palette.accent)

        Spacer(Modifier.height(14.dp))
        TextButton(
            onClick = { if (hardPhase) onOpenRightNow() else onOpenDetail() },
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        ) {
            Text(
                if (hardPhase) "What is going on, and what not to say"
                else "What is going on, and what this week is good for",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun MoodRow(profile: UserProfile, today: LocalDate, viewModel: AppViewModel, accent: Color) {
    val existing = profile.logFor(today)
    fun set(mood: DayMood) {
        viewModel.update { it.withDayLog(DayLog(today, mood, existing?.tags ?: emptySet())) }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Today",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(12.dp))
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MoodPill(Icons.Filled.SentimentDissatisfied, DayMood.ROUGH, existing?.mood, accent, Modifier.weight(1f)) { set(DayMood.ROUGH) }
            MoodPill(Icons.Filled.SentimentNeutral, DayMood.NORMAL, existing?.mood, accent, Modifier.weight(1f)) { set(DayMood.NORMAL) }
            MoodPill(Icons.Filled.SentimentSatisfied, DayMood.GOOD, existing?.mood, accent, Modifier.weight(1f)) { set(DayMood.GOOD) }
        }
    }
}

@Composable
private fun MoodPill(
    icon: ImageVector,
    mood: DayMood,
    selected: DayMood?,
    accent: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val on = selected == mood
    Box(
        modifier
            .height(44.dp)
            .background(
                if (on) accent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(22.dp),
            )
            .then(if (on) Modifier.border(1.5.dp, accent, RoundedCornerShape(22.dp)) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = mood.label,
            tint = if (on) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(21.dp),
        )
    }
}
