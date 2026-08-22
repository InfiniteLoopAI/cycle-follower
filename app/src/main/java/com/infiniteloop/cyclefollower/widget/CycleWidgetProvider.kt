package com.infiniteloop.cyclefollower.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.infiniteloop.cyclefollower.MainActivity
import com.infiniteloop.cyclefollower.R
import com.infiniteloop.cyclefollower.data.ProfileRepository
import com.infiniteloop.cyclefollower.data.UserProfile
import com.infiniteloop.cyclefollower.domain.Briefing
import com.infiniteloop.cyclefollower.domain.Briefings
import com.infiniteloop.cyclefollower.domain.CycleEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Deliberately a plain RemoteViews widget rather than Compose/Glance: fewer moving parts, and
 * four lines of text on a home screen do not need a compositor.
 */
class CycleWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // The profile lives in DataStore, which is disk-backed. Reading it with runBlocking on the
        // main thread risked the widget rendering the "not set up yet" placeholder whenever the
        // read was slow -- so the work moves to a background coroutine and the broadcast is kept
        // alive until it finishes.
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val profile = runCatching { ProfileRepository.get(appContext).current() }
                    .getOrElse { UserProfile() }
                val views = buildViews(appContext, profile)
                appWidgetIds.forEach { id ->
                    runCatching { appWidgetManager.updateAppWidget(id, views) }
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun buildViews(context: Context, profile: UserProfile): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_cycle)
        val status = CycleEngine.status(profile)
        val briefing = Briefings.build(profile, status)

        when {
            briefing.needsSetup -> render(
                views,
                day = "--",
                phase = "Cycle Follower",
                timing = null,
                mood = "Tap to add the date her last period started.",
                tip = null,
            )

            profile.discreetMode -> render(
                views,
                day = status?.cycleDay?.toString() ?: "--",
                phase = "Cycle day",
                timing = null,
                mood = "Tap for today's note.",
                tip = null,
            )

            else -> render(
                views,
                day = status?.let { "Day ${it.cycleDay}" } ?: "--",
                phase = "${briefing.emoji} ${briefing.phaseTitle}",
                timing = briefing.shortTiming,
                mood = briefing.moodBanner,
                tip = briefing.doNow.firstOrNull()?.let { "Today: $it" },
            )
        }

        views.setOnClickPendingIntent(R.id.widget_root, launchIntent(context))
        return views
    }

    private fun render(
        views: RemoteViews,
        day: String,
        phase: String,
        timing: String?,
        mood: String,
        tip: String?,
    ) {
        views.setTextViewText(R.id.widget_day, day)
        views.setTextViewText(R.id.widget_phase, phase)
        views.setTextViewText(R.id.widget_mood, mood)
        if (tip.isNullOrBlank()) {
            views.setViewVisibility(R.id.widget_tip, View.GONE)
        } else {
            views.setViewVisibility(R.id.widget_tip, View.VISIBLE)
            views.setTextViewText(R.id.widget_tip, tip)
        }
        if (timing.isNullOrBlank()) {
            views.setViewVisibility(R.id.widget_timing, View.GONE)
        } else {
            views.setViewVisibility(R.id.widget_timing, View.VISIBLE)
            views.setTextViewText(R.id.widget_timing, timing)
        }
    }

    private fun launchIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        /** Call after anything changes the profile so the home screen does not go stale. */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(context, CycleWidgetProvider::class.java))
            if (ids.isEmpty()) return
            context.sendBroadcast(
                Intent(context, CycleWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                },
            )
        }
    }
}
