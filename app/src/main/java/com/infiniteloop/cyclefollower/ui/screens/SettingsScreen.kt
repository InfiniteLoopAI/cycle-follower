package com.infiniteloop.cyclefollower.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.infiniteloop.cyclefollower.backup.BackupCodec
import com.infiniteloop.cyclefollower.security.AppLock
import kotlinx.coroutines.launch
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

    var showHeadsUpTime by rememberSaveable { mutableStateOf(false) }
    var backupPassword by rememberSaveable { mutableStateOf("") }
    var askExportPassword by rememberSaveable { mutableStateOf(false) }
    var pendingImport by rememberSaveable { mutableStateOf<String?>(null) }
    var askImportPassword by rememberSaveable { mutableStateOf(false) }
    var backupMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun applyImport(text: String, password: String?) {
        when (val result = BackupCodec.decode(text, password)) {
            is BackupCodec.Result.Ok -> {
                viewModel.replaceProfile(result.profile)
                backupMessage = "Restored. Everything from that file is now on this phone."
                pendingImport = null
            }
            BackupCodec.Result.NeedsPassword -> { pendingImport = text; askImportPassword = true }
            BackupCodec.Result.WrongPassword -> { askImportPassword = true; backupMessage = "That password did not open the file." }
            BackupCodec.Result.NotABackup -> { pendingImport = null; backupMessage = "That file is not a Cycle Follower backup." }
            is BackupCodec.Result.Unreadable -> { pendingImport = null; backupMessage = result.reason }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = BackupCodec.encode(profile, backupPassword.ifBlank { null })
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) } != null
            }.getOrDefault(false)
            backupPassword = ""
            backupMessage = if (ok) "Backup written." else "Could not write to that location."
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            if (text == null) {
                backupMessage = "Could not read that file."
            } else {
                applyImport(text, null)
            }
        }
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
            SectionCard(title = "A day's notice") {
                SwitchRow(
                    label = "Evening heads-up",
                    description = "The night before a new phase starts, while there is still time to " +
                        "shop, cook or move something.",
                    checked = profile.headsUpNotification,
                    onChange = { value ->
                        viewModel.update { it.copy(headsUpNotification = value) }
                        if (value && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            !DailyHintScheduler.hasPermission(context)
                        ) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                )
                Spacer(Modifier.height(14.dp))
                OutlinedButton(
                    onClick = { showHeadsUpTime = true },
                    enabled = profile.headsUpNotification,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(String.format(Locale.UK, "Send it at %02d:%02d", profile.headsUpHour, profile.headsUpMinute))
                }
            }
        }

        item {
            SectionCard(title = "Lock") {
                val canLock = remember { AppLock.canLock(context) }
                SwitchRow(
                    label = "Require unlock to open",
                    description = if (canLock) {
                        "Uses whatever the phone already uses - fingerprint, face or PIN."
                    } else {
                        "Unavailable: this phone has no screen lock set up yet."
                    },
                    checked = profile.appLock && canLock,
                    onChange = { value -> if (canLock) viewModel.update { it.copy(appLock = value) } },
                )
                Spacer(Modifier.height(14.dp))
                SwitchRow(
                    label = "Hide in the app switcher",
                    description = "Blanks the preview thumbnail when you switch between apps.",
                    checked = profile.secureScreen,
                    onChange = { value -> viewModel.update { it.copy(secureScreen = value) } },
                )
            }
        }

        item {
            SectionCard(title = "Backup") {
                Text(
                    "Everything lives in one file on this phone. Export it somewhere safe, or this all " +
                        "goes with the handset.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { askExportPassword = true }, modifier = Modifier.weight(1f)) { Text("Export") }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        modifier = Modifier.weight(1f),
                    ) { Text("Restore") }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "${profile.periodStarts.size} periods and ${profile.dayLogs.size} logged days on this phone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                backupMessage?.let {
                    Spacer(Modifier.height(12.dp))
                    Callout(text = it, tone = CalloutTone.INFO)
                }
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

    if (showHeadsUpTime) {
        TimePickerDialog(
            initialHour = profile.headsUpHour,
            initialMinute = profile.headsUpMinute,
            onDismiss = { showHeadsUpTime = false },
            onPicked = { hour, minute -> viewModel.update { it.copy(headsUpHour = hour, headsUpMinute = minute) } },
        )
    }

    if (askExportPassword) {
        PasswordDialog(
            title = "Protect the backup?",
            body = "A password encrypts the file. Leave it blank for a plain one - readable by anything " +
                "that can open the folder you put it in.",
            confirmLabel = "Export",
            allowEmpty = true,
            onDismiss = { askExportPassword = false },
            onConfirm = { password ->
                backupPassword = password
                askExportPassword = false
                exportLauncher.launch(BackupCodec.suggestedFileName())
            },
        )
    }

    if (askImportPassword) {
        PasswordDialog(
            title = "This backup is protected",
            body = "Enter the password it was exported with.",
            confirmLabel = "Restore",
            allowEmpty = false,
            onDismiss = { askImportPassword = false; pendingImport = null },
            onConfirm = { password ->
                askImportPassword = false
                pendingImport?.let { applyImport(it, password) }
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


@Composable
private fun PasswordDialog(
    title: String,
    body: String,
    confirmLabel: String,
    allowEmpty: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(body, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }, enabled = allowEmpty || value.isNotBlank()) {
                Text(confirmLabel)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
