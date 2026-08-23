package com.infiniteloop.cyclefollower.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.infiniteloop.cyclefollower.data.UserProfile
import com.infiniteloop.cyclefollower.domain.PlanWindow
import com.infiniteloop.cyclefollower.domain.Planner
import com.infiniteloop.cyclefollower.domain.Suitability
import com.infiniteloop.cyclefollower.ui.components.Callout
import com.infiniteloop.cyclefollower.ui.components.CalloutTone
import com.infiniteloop.cyclefollower.ui.components.BoundedDatePickerDialog
import com.infiniteloop.cyclefollower.ui.components.SectionCard
import com.infiniteloop.cyclefollower.ui.components.shortLabel
import com.infiniteloop.cyclefollower.ui.theme.phasePalette
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
private fun colourOf(suitability: Suitability): Color = when (suitability) {
    Suitability.BEST -> if (isSystemInDarkTheme()) Color(0xFF8CD9A3) else Color(0xFF2E7D4F)
    Suitability.OK -> if (isSystemInDarkTheme()) Color(0xFFB7B4F0) else Color(0xFF4F4CB0)
    Suitability.POOR -> if (isSystemInDarkTheme()) Color(0xFFF0A97C) else Color(0xFFB35309)
    Suitability.AVOID -> if (isSystemInDarkTheme()) Color(0xFFFF8FA3) else Color(0xFFC2185B)
}

/**
 * The app otherwise only ever talks about today, which is no use at all for booking a trip or
 * keeping a bad week clear. This is the same phase timeline, read forwards.
 */
@Composable
fun PlanScreen(profile: UserProfile) {
    val today = remember { LocalDate.now() }
    val good = remember(profile, today) { Planner.goodWindows(profile, today) }
    val avoid = remember(profile, today) { Planner.avoidWindows(profile, today) }
    var checking by rememberSaveable { mutableStateOf(false) }
    var checked by rememberSaveable { mutableStateOf<Long?>(null) }
    val checkedDate = checked?.let { LocalDate.ofEpochDay(it) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                Text("Plan around it", style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    "The app knows her next few months. Use it to put things in the right week " +
                        "instead of apologising for the wrong one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (good.isEmpty() && avoid.isEmpty()) {
            item {
                Callout(
                    title = "Nothing to plan with yet",
                    text = "Add the first day of her most recent period and this fills in.",
                    tone = CalloutTone.INFO,
                )
            }
            return@LazyColumn
        }

        item {
            SectionCard(
                title = "Check a date",
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                        .clickable { checking = true }
                        .padding(horizontal = 13.dp, vertical = 12.dp),
                ) {
                    Text(
                        checkedDate?.shortLabel() ?: "Pick a date you are thinking about",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (checkedDate != null) {
                    val verdict = Planner.dayFor(profile, checkedDate)
                    Spacer(Modifier.height(12.dp))
                    if (verdict == null) {
                        Text(
                            "That is before the first period you logged, so there is nothing to say about it.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        val palette = phasePalette(verdict.phase, isSystemInDarkTheme())
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(palette.container, RoundedCornerShape(14.dp))
                                .padding(13.dp),
                        ) {
                            Text(
                                "Day ${verdict.cycleDay} — ${verdict.phase.displayName.lowercase()}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = palette.onContainer,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                verdictLine(profile, checkedDate, today),
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.onContainer,
                            )
                        }
                    }
                }
            }
        }

        if (good.isNotEmpty()) {
            item { WindowGroup("Good weeks", good, today) }
        }
        if (avoid.isNotEmpty()) {
            item { WindowGroup("Keep these clear", avoid, today) }
        }

        item {
            SectionCard(title = "What the weeks are good for") {
                com.infiniteloop.cyclefollower.ui.components.BulletList(
                    listOf(
                        "Best weeks: a trip, people staying, a night out, the conversation you have been putting off.",
                        "Fine: quiet plans at home, anything low-demand.",
                        "Keep clear: anything demanding, early starts, big social occasions, difficult subjects.",
                    ),
                    marker = "→",
                )
            }
        }
    }

    if (checking) {
        BoundedDatePickerDialog(
            initial = checkedDate ?: today,
            minDate = today,
            maxDate = today.plusDays(Planner.HORIZON_DAYS.toLong()),
            onDismiss = { checking = false },
            onPicked = { checked = it.toEpochDay() },
        )
    }
}

private fun verdictLine(profile: UserProfile, date: LocalDate, today: LocalDate): String {
    val day = Planner.dayFor(profile, date) ?: return ""
    return when (day.suitability) {
        Suitability.BEST -> "Good week. Put it in the diary."
        Suitability.OK -> "Fine for something low-key at home, less good for anything demanding."
        Suitability.POOR, Suitability.AVOID -> {
            val better = Planner.betterDateNear(profile, date, notBefore = today)
            val tail = better?.let { " ${it.shortLabel()} would land far better." } ?: ""
            "Bad week for anything that asks much of her.$tail"
        }
    }
}

@Composable
private fun WindowGroup(title: String, windows: List<PlanWindow>, today: LocalDate) {
    SectionCard(title = title) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            windows.forEach { window ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    Box(
                        Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .background(colourOf(window.suitability), RoundedCornerShape(2.dp)),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Text(
                                "${window.start.shortLabel()} – ${window.end.shortLabel()}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                window.suitability.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = colourOf(window.suitability),
                            )
                        }
                        Text(
                            "${window.days} days · ${window.phase.displayName.lowercase()} · " +
                                startsIn(window, today),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun startsIn(window: PlanWindow, today: LocalDate): String {
    val days = ChronoUnit.DAYS.between(today, window.start).toInt()
    return when {
        days <= 0 -> "on now"
        days == 1 -> "starts tomorrow"
        else -> "starts in $days days"
    }
}
