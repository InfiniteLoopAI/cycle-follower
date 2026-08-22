package com.infiniteloop.cyclefollower.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.infiniteloop.cyclefollower.BuildConfig
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
import com.infiniteloop.cyclefollower.ui.components.SectionCard
import com.infiniteloop.cyclefollower.ui.components.SwitchRow
import com.infiniteloop.cyclefollower.ui.components.TimePickerDialog
import java.util.Locale

@Composable
fun SettingsScreen(profile: UserProfile, viewModel: AppViewModel) {
    val context = LocalContext.current
    var nameDraft by rememberSaveable { mutableStateOf(profile.partnerName) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    var permissionMissing by rememberSaveable { mutableStateOf(false) }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionMissing = !granted
        viewModel.update { it.copy(dailyNotification = granted) }
    }

    LaunchedEffect(profile.dailyNotification) {
        permissionMissing = profile.dailyNotification && !DailyHintScheduler.hasPermission(context)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Text("Settings", style = MaterialTheme.typography.displaySmall) }

        item {
            SectionCard(title = "Her name") {
                OutlinedTextField(
                    value = nameDraft,
                    onValueChange = {
                        nameDraft = it.take(24)
                        viewModel.update { profileValue -> profileValue.copy(partnerName = nameDraft.trim()) }
                    },
                    label = { Text("Optional") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item {
            SectionCard(title = "Cycle numbers") {
                NumberStepper(
                    label = "Cycle length",
                    value = profile.statedCycleLength,
                    range = UserProfile.MIN_CYCLE_LENGTH..UserProfile.MAX_CYCLE_LENGTH,
                    suffix = "days",
                    helper = "Only used until there are two or more logged periods to average.",
                    onChange = { value -> viewModel.update { it.copy(statedCycleLength = value) } },
                )
                Spacer(Modifier.height(18.dp))
                NumberStepper(
                    label = "Bleeding lasts",
                    value = profile.periodLength,
                    range = 1..10,
                    suffix = "days",
                    helper = null,
                    onChange = { value -> viewModel.update { it.copy(periodLength = value) } },
                )
                Spacer(Modifier.height(14.dp))
                SwitchRow(
                    label = "Use logged dates for the average",
                    description = "On: the app works the cycle length out from real logged periods. " +
                        "Off: it always uses the number above.",
                    checked = profile.useHistoryAverage,
                    onChange = { value -> viewModel.update { it.copy(useHistoryAverage = value) } },
                )
            }
        }

        item {
            SectionCard(title = "Contraception") {
                ChoiceList(
                    options = Contraception.entries,
                    selected = profile.contraception,
                    labelOf = { it.label },
                    descriptionOf = { it.explanation },
                    onSelect = { value -> viewModel.update { it.copy(contraception = value) } },
                )
            }
        }

        item {
            SectionCard(title = "The week before her period") {
                ChoiceList(
                    options = PmsSeverity.entries,
                    selected = profile.pmsSeverity,
                    labelOf = { it.label },
                    descriptionOf = { it.description },
                    onSelect = { value -> viewModel.update { it.copy(pmsSeverity = value) } },
                )
                Spacer(Modifier.height(14.dp))
                SwitchRow(
                    label = "Diagnosed PMDD",
                    description = "Widens the window the app watches and changes how it words things.",
                    checked = profile.pmdd,
                    onChange = { value -> viewModel.update { it.copy(pmdd = value) } },
                )
            }
        }

        SymptomCategory.entries.forEach { category ->
            item {
                val options = Symptom.entries.filter { it.category == category }
                SectionCard(title = "Symptoms - ${category.label.lowercase(Locale.UK)}") {
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

        item {
            SectionCard(title = "Daily hint") {
                SwitchRow(
                    label = "Morning notification",
                    description = "One message a day with her cycle day, likely mood and one thing to do.",
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
                if (permissionMissing) {
                    Spacer(Modifier.height(12.dp))
                    Callout(
                        title = "Notifications are blocked",
                        tone = CalloutTone.WARNING,
                        text = "Android is not letting the app post notifications. Turn them on in the system " +
                            "settings for Cycle Follower, or tap the switch again to ask for permission.",
                    )
                }
                Spacer(Modifier.height(16.dp))
                SwitchRow(
                    label = "Discreet mode",
                    description = "Hides the details from the notification and the widget - just a day number.",
                    checked = profile.discreetMode,
                    onChange = { value -> viewModel.update { it.copy(discreetMode = value) } },
                )
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        if (DailyHintScheduler.hasPermission(context)) {
                            DailyHintScheduler.showNotification(context, profile)
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            // Otherwise the button would silently do nothing.
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            permissionMissing = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Send me today's hint now") }
            }
        }

        item {
            SectionCard(title = "Privacy") {
                Text(
                    "Everything stays on this phone. The app has no internet permission at all, so it is " +
                        "technically incapable of uploading anything, and there are no accounts, no analytics and " +
                        "no adverts. Uninstalling deletes the lot.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        item {
            SectionCard(title = "About") {
                Text(
                    "Cycle Follower ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Check github.com/InfiniteLoopAI/cycle-follower/releases for a newer build.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Text(Library.DISCLAIMER, style = MaterialTheme.typography.bodyMedium)
            }
        }

        item {
            Button(
                onClick = { showResetDialog = true },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Erase everything and start again") }
        }
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

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Erase everything?") },
            text = {
                Text(
                    "Every logged date and every setting is deleted from this phone and the setup starts over. " +
                        "There is no copy anywhere, so this cannot be undone.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetEverything()
                        showResetDialog = false
                    },
                ) { Text("Erase it") }
            },
            dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("Keep it") } },
        )
    }
}
