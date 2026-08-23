package com.infiniteloop.cyclefollower.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.infiniteloop.cyclefollower.data.Contraception
import com.infiniteloop.cyclefollower.data.PmsSeverity
import com.infiniteloop.cyclefollower.data.Symptom
import com.infiniteloop.cyclefollower.data.SymptomCategory
import com.infiniteloop.cyclefollower.data.UserProfile
import com.infiniteloop.cyclefollower.domain.Library
import com.infiniteloop.cyclefollower.notify.DailyHintScheduler
import com.infiniteloop.cyclefollower.ui.AppViewModel
import com.infiniteloop.cyclefollower.ui.components.Callout
import com.infiniteloop.cyclefollower.ui.components.CalloutTone
import com.infiniteloop.cyclefollower.ui.components.ChipGroup
import com.infiniteloop.cyclefollower.ui.components.ChoiceList
import com.infiniteloop.cyclefollower.ui.components.NumberStepper
import com.infiniteloop.cyclefollower.ui.components.BoundedDatePickerDialog
import com.infiniteloop.cyclefollower.ui.components.SectionCard
import com.infiniteloop.cyclefollower.ui.components.SwitchRow
import com.infiniteloop.cyclefollower.ui.components.TimePickerDialog
import com.infiniteloop.cyclefollower.ui.components.longLabel
import java.time.LocalDate
import java.util.Locale

private const val STEP_COUNT = 8

@Composable
fun SetupScreen(profile: UserProfile, viewModel: AppViewModel) {
    var step by rememberSaveable { mutableStateOf(0) }
    var nameDraft by rememberSaveable { mutableStateOf(profile.partnerName) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    val today = remember { LocalDate.now() }
    val context = LocalContext.current

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.update { it.copy(dailyNotification = granted) }
    }

    val canAdvance = step != 2 || profile.lastPeriodStart != null

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        LinearProgressIndicator(
            progress = { (step + 1f) / STEP_COUNT },
            modifier = Modifier.fillMaxWidth(),
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    "Step ${step + 1} of $STEP_COUNT",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            when (step) {
                0 -> welcomeStep(this)
                1 -> item {
                    StepBody(
                        title = "What should I call her?",
                        blurb = "Optional. It only changes the wording so the app reads less like a lab report.",
                    ) {
                        OutlinedTextField(
                            value = nameDraft,
                            onValueChange = { nameDraft = it.take(24) },
                            label = { Text("Her name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                2 -> item {
                    StepBody(
                        title = "When did her last period start?",
                        blurb = "The first day of proper bleeding, not spotting the day before. This is the one " +
                            "thing the app genuinely cannot work without.",
                    ) {
                        Button(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(profile.lastPeriodStart?.longLabel() ?: "Pick the date")
                        }
                        if (profile.periodStarts.size > 1) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "${profile.periodStarts.size} dates logged. You can add older ones now or later - " +
                                    "each one makes the predictions sharper.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Add an earlier period start too")
                        }
                    }
                }
                3 -> item {
                    StepBody(
                        title = "How long are her cycles?",
                        blurb = "Best guess is fine. Once you have logged a few periods the app works it out from " +
                            "the real dates and stops using this number.",
                    ) {
                        NumberStepper(
                            label = "Cycle length",
                            value = profile.statedCycleLength,
                            range = UserProfile.MIN_CYCLE_LENGTH..UserProfile.MAX_CYCLE_LENGTH,
                            suffix = "days",
                            helper = "First day of one period to the first day of the next. 28 is the average, " +
                                "but anything from 21 to 35 is completely normal.",
                            onChange = { value -> viewModel.update { it.copy(statedCycleLength = value) } },
                        )
                        Spacer(Modifier.height(20.dp))
                        NumberStepper(
                            label = "Bleeding lasts",
                            value = profile.periodLength,
                            range = 1..10,
                            suffix = "days",
                            helper = "Two to seven days is the normal range.",
                            onChange = { value -> viewModel.update { it.copy(periodLength = value) } },
                        )
                    }
                }
                4 -> item {
                    StepBody(
                        title = "Is she using contraception?",
                        blurb = "This matters more than anything else here. On methods that stop ovulation there " +
                            "is no fertile window and no hormone peak, so the app has to say something different.",
                    ) {
                        ChoiceList(
                            options = Contraception.entries,
                            selected = profile.contraception,
                            labelOf = { it.label },
                            descriptionOf = { it.explanation },
                            onSelect = { value -> viewModel.update { it.copy(contraception = value) } },
                        )
                    }
                }
                5 -> item {
                    StepBody(
                        title = "How rough is the week before her period?",
                        blurb = "This sets how many days ahead the app starts flagging the PMS window, and how " +
                            "strongly it words the advice.",
                    ) {
                        ChoiceList(
                            options = PmsSeverity.entries,
                            selected = profile.pmsSeverity,
                            labelOf = { it.label },
                            descriptionOf = { it.description },
                            onSelect = { value -> viewModel.update { it.copy(pmsSeverity = value) } },
                        )
                        Spacer(Modifier.height(16.dp))
                        SwitchRow(
                            label = "She has been diagnosed with PMDD",
                            description = "A recognised condition, not just severe PMS. It widens the window the " +
                                "app watches and changes the tone of what it tells you.",
                            checked = profile.pmdd,
                            onChange = { value -> viewModel.update { it.copy(pmdd = value) } },
                        )
                    }
                }
                6 -> symptomStep(this, profile, viewModel)
                7 -> item {
                    StepBody(
                        title = "Want a daily hint?",
                        blurb = "One notification each morning with where she is and what today needs from you.",
                    ) {
                        SwitchRow(
                            label = "Daily morning notification",
                            description = "Shows the cycle day, her likely mood and one thing to do about it.",
                            checked = profile.dailyNotification,
                            onChange = { value ->
                                viewModel.update { it.copy(dailyNotification = value) }
                                if (value && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    !DailyHintScheduler.hasPermission(context)
                                ) {
                                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                        )
                        Spacer(Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = { showTimePicker = true },
                            enabled = profile.dailyNotification,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                String.format(
                                    Locale.UK,
                                    "Send it at %02d:%02d",
                                    profile.notificationHour,
                                    profile.notificationMinute,
                                ),
                            )
                        }
                        Spacer(Modifier.height(18.dp))
                        SwitchRow(
                            label = "Discreet mode",
                            description = "Keeps the notification and the home screen widget vague, so nothing " +
                                "personal shows on a lock screen someone else might glance at.",
                            checked = profile.discreetMode,
                            onChange = { value -> viewModel.update { it.copy(discreetMode = value) } },
                        )
                        Spacer(Modifier.height(18.dp))
                        Callout(
                            title = "Add the widget too",
                            text = "Long-press the home screen, pick Widgets, find Cycle Follower and drop it " +
                                "somewhere you will actually see it.",
                        )
                    }
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (step > 0) {
                OutlinedButton(
                    onClick = {
                        if (step == 1) viewModel.update { it.copy(partnerName = nameDraft.trim()) }
                        step--
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Back") }
            }
            Button(
                onClick = {
                    if (step == 1) viewModel.update { it.copy(partnerName = nameDraft.trim()) }
                    if (step == STEP_COUNT - 1) viewModel.completeSetup() else step++
                },
                enabled = canAdvance,
                modifier = Modifier.weight(1f),
            ) {
                Text(if (step == STEP_COUNT - 1) "Finish" else "Next")
            }
        }

        if (step in 3 until STEP_COUNT - 1) {
            TextButton(
                onClick = { viewModel.completeSetup() },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            ) { Text("Skip the rest, I will fill it in later") }
        } else {
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showDatePicker) {
        BoundedDatePickerDialog(
            initial = profile.lastPeriodStart ?: today,
            onDismiss = { showDatePicker = false },
            onPicked = { viewModel.logPeriodStart(it) },
        )
    }

    if (showTimePicker) {
        TimePickerDialog(
            initialHour = profile.notificationHour,
            initialMinute = profile.notificationMinute,
            onDismiss = { showTimePicker = false },
            onPicked = { hour, minute ->
                viewModel.update { it.copy(notificationHour = hour, notificationMinute = minute) }
            },
        )
    }
}

private fun welcomeStep(scope: androidx.compose.foundation.lazy.LazyListScope) {
    scope.item {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Cycle Follower", style = MaterialTheme.typography.displaySmall)
            Text(
                "Most of what happens across her month is driven by two hormones rising and falling on a " +
                    "schedule. Knowing where she is in that schedule tells you when to plan the big thing, when " +
                    "to take things off her plate, and when to just make dinner and say nothing.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Callout(
                title = "The one rule, before anything else",
                tone = CalloutTone.GOLDEN,
                text = "This app exists to change what YOU do. The second it becomes a way to explain her feelings " +
                    "back to her -- \"is it your period?\" -- it has made things worse than knowing nothing. " +
                    "Use it to be more considerate, never to win an argument.",
            )
            Callout(text = Library.DISCLAIMER, tone = CalloutTone.INFO)
            Text(
                "Eight quick questions. Only one of them is required.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun symptomStep(
    scope: androidx.compose.foundation.lazy.LazyListScope,
    profile: UserProfile,
    viewModel: AppViewModel,
) {
    scope.item {
        StepBody(
            title = "What does she actually get?",
            blurb = "Tick what applies. The app will only mention these, in the phase where they usually turn up, " +
                "instead of describing an average woman.",
        ) {}
    }
    SymptomCategory.entries.forEach { category ->
        scope.item {
            val options = Symptom.entries.filter { it.category == category }
            SectionCard(title = category.label) {
                ChipGroup(
                    options = options.map { it.label },
                    selected = profile.selectedSymptoms.map { it.label }.toSet(),
                    onToggle = { label ->
                        val symptom = options.first { it.label == label }
                        viewModel.update { current ->
                            val updated = if (symptom.name in current.symptoms) {
                                current.symptoms - symptom.name
                            } else {
                                current.symptoms + symptom.name
                            }
                            current.copy(symptoms = updated)
                        }
                    },
                )
            }
        }
    }
    scope.item {
        Text(
            "If you do not know, leave it blank and ask her. That conversation is worth more than the app is.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StepBody(title: String, blurb: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(
            blurb,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        content()
    }
}
