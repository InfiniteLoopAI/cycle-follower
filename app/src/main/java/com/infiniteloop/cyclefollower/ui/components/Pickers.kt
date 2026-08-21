package com.infiniteloop.cyclefollower.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val displayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.UK)
private val shortFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.UK)

fun LocalDate.longLabel(): String = format(displayFormatter)
fun LocalDate.shortLabel(): String = format(shortFormatter)

fun LocalTime.label(): String = String.format(Locale.UK, "%02d:%02d", hour, minute)

/**
 * Date picker restricted to the past year and never the future -- a period cannot start
 * tomorrow, and a stray future date would silently break every prediction.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastDatePickerDialog(
    initial: LocalDate,
    onDismiss: () -> Unit,
    onPicked: (LocalDate) -> Unit,
    maxDate: LocalDate = LocalDate.now(),
    minDate: LocalDate = LocalDate.now().minusYears(2),
) {
    val maxMillis = maxDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val minMillis = minDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                utcTimeMillis in minMillis..maxMillis

            override fun isSelectableYear(year: Int): Boolean =
                year in minDate.year..maxDate.year
        },
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onPicked(Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate())
                    }
                    onDismiss()
                },
            ) { Text("Select") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onPicked: (Int, Int) -> Unit,
) {
    val state = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Daily hint time") },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(
                onClick = {
                    onPicked(state.hour, state.minute)
                    onDismiss()
                },
            ) { Text("Set") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
