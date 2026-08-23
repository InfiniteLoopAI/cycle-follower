package com.infiniteloop.cyclefollower.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.infiniteloop.cyclefollower.MainActivity
import com.infiniteloop.cyclefollower.R
import com.infiniteloop.cyclefollower.data.ProfileRepository
import com.infiniteloop.cyclefollower.data.UserProfile
import com.infiniteloop.cyclefollower.domain.CycleEngine
import com.infiniteloop.cyclefollower.domain.CyclePhase
import com.infiniteloop.cyclefollower.domain.PhaseGuides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * The morning hint arrives on the day it is already happening, which is too late to shop, cook or
 * move a plan. This is the evening before, and only when tomorrow actually changes something.
 */
object HeadsUp {

    const val CHANNEL_ID = "heads_up"
    private const val NOTIFICATION_ID = 1002
    private const val REQUEST_CODE = 2002

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.headsup_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.headsup_channel_description)
            setShowBadge(false)
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    fun rescheduleFromProfile(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            val profile = runCatching { ProfileRepository.get(appContext).current() }.getOrNull() ?: return@launch
            schedule(appContext, profile)
        }
    }

    fun schedule(context: Context, profile: UserProfile) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, HeadsUpReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        if (!profile.headsUpNotification || !profile.setupComplete) {
            alarmManager.cancel(pendingIntent)
            return
        }
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(profile.headsUpHour, profile.headsUpMinute)
        if (!next.isAfter(now)) next = next.plusDays(1)
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            pendingIntent,
        )
    }

    /** What tomorrow changes, or null when it changes nothing worth interrupting the evening for. */
    fun tomorrowChange(profile: UserProfile, today: LocalDate = LocalDate.now()): Pair<String, String>? {
        val todayStatus = CycleEngine.status(profile, today) ?: return null
        val tomorrowStatus = CycleEngine.status(profile, today.plusDays(1)) ?: return null
        if (tomorrowStatus.phase == todayStatus.phase) return null

        val guide = PhaseGuides.of(tomorrowStatus.phase)
        val title = when (tomorrowStatus.phase) {
            CyclePhase.LATE_LUTEAL -> "Tomorrow: her hardest week starts"
            CyclePhase.MENSTRUAL -> "Tomorrow: her period is due"
            CyclePhase.FERTILE_WINDOW -> "Tomorrow: her best week starts"
            else -> "Tomorrow: ${guide.title.lowercase()}"
        }
        val body = buildString {
            append(guide.oneLiner)
            guide.doThis.firstOrNull()?.let { append("\n\nWorth doing tonight: ").append(it) }
        }
        return title to body
    }

    fun show(context: Context, profile: UserProfile, today: LocalDate = LocalDate.now()) {
        if (!DailyHintScheduler.hasPermission(context)) return
        val (title, body) = tomorrowChange(profile, today) ?: return

        val contentIntent = PendingIntent.getActivity(
            context,
            1,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val shown = if (profile.discreetMode) {
            context.getString(R.string.app_name) to "Something changes tomorrow. Open when you can."
        } else {
            title to body
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(shown.first)
            .setContentText(shown.second.lineSequence().first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(shown.second))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }
    }
}

class HeadsUpReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val profile = runCatching { ProfileRepository.get(appContext).current() }.getOrNull()
                if (profile != null) {
                    if (profile.headsUpNotification && profile.setupComplete) {
                        HeadsUp.show(appContext, profile)
                    }
                    HeadsUp.schedule(appContext, profile)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
