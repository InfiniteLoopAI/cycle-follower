package com.infiniteloop.cyclefollower.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.infiniteloop.cyclefollower.MainActivity
import com.infiniteloop.cyclefollower.R
import com.infiniteloop.cyclefollower.data.ProfileRepository
import com.infiniteloop.cyclefollower.data.UserProfile
import com.infiniteloop.cyclefollower.domain.Briefings
import com.infiniteloop.cyclefollower.domain.CycleEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * One inexact daily alarm. Inexact on purpose: exact alarms need a special permission on
 * Android 12+ and a morning hint does not need to land on the second.
 */
object DailyHintScheduler {

    const val CHANNEL_ID = "daily_hint"
    private const val NOTIFICATION_ID = 1001
    private const val REQUEST_CODE = 2001

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            setShowBadge(false)
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    /** Reads the profile off the main thread and schedules or cancels accordingly. */
    fun rescheduleFromProfile(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            val profile = runCatching { ProfileRepository.get(appContext).current() }.getOrNull() ?: return@launch
            schedule(appContext, profile)
        }
    }

    fun schedule(context: Context, profile: UserProfile) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = alarmPendingIntent(context)

        if (!profile.dailyNotification || !profile.setupComplete) {
            alarmManager.cancel(pendingIntent)
            return
        }

        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(profile.notificationHour, profile.notificationMinute)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val triggerAt = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java)?.cancel(alarmPendingIntent(context))
    }

    private fun alarmPendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, DailyHintReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun showNotification(context: Context, profile: UserProfile, today: LocalDate = LocalDate.now()) {
        if (!hasPermission(context)) return

        val status = CycleEngine.status(profile, today)
        val briefing = Briefings.build(profile, status, today)
        if (briefing.needsSetup) return

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title: String
        val short: String
        val expanded: String

        if (profile.discreetMode) {
            title = context.getString(R.string.app_name)
            short = briefing.dayLabel
            expanded = briefing.dayLabel + "\n\nOpen the app for today's note."
        } else {
            title = "${briefing.emoji} ${briefing.dayLabel} - ${briefing.phaseTitle}"
            short = briefing.moodBanner
            expanded = buildString {
                append(briefing.moodBanner)
                briefing.timingLine?.let { append("\n\n").append(it) }
                briefing.doNow.firstOrNull()?.let { append("\n\nToday: ").append(it) }
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(short)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expanded))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }
}
