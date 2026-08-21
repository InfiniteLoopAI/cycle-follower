package com.infiniteloop.cyclefollower.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.infiniteloop.cyclefollower.MainActivity
import com.infiniteloop.cyclefollower.R
import com.infiniteloop.cyclefollower.data.ProfileRepository
import com.infiniteloop.cyclefollower.data.UserProfile
import com.infiniteloop.cyclefollower.domain.Briefings
import com.infiniteloop.cyclefollower.domain.CycleEngine
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Deliberately a plain RemoteViews widget rather than a Compose/Glance one: fewer moving parts,
 * and a home-screen widget showing three lines of text does not need a compositor.
 */
class CycleWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val profile = loadProfile(context)
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildViews(context, profile))
        }
    }

    private fun loadProfile(context: Context): UserProfile = runBlocking {
        withTimeoutOrNull(2_000) {
            runCatching { ProfileRepository.get(context).current() }.getOrNull()
        } ?: UserProfile()
    }

    private fun buildViews(context: Context, profile: UserProfile): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_cycle)
        val status = CycleEngine.status(profile)
        val briefing = Briefings.build(profile, status)

        if (briefing.needsSetup) {
            views.setTextViewText(R.id.widget_day, "--")
            views.setTextViewText(R.id.widget_phase, "Cycle Follower")
            views.setTextViewText(R.id.widget_mood, "Tap to add her last period date.")
        } else if (profile.discreetMode) {
            views.setTextViewText(R.id.widget_day, status?.cycleDay?.toString() ?: "--")
            views.setTextViewText(R.id.widget_phase, "Cycle day")
            views.setTextViewText(R.id.widget_mood, "Tap for today's note.")
        } else {
            views.setTextViewText(R.id.widget_day, status?.let { "Day ${it.cycleDay}" } ?: "--")
            views.setTextViewText(R.id.widget_phase, "${briefing.emoji} ${briefing.phaseTitle}")
            views.setTextViewText(R.id.widget_mood, briefing.moodBanner)
        }

        val intent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.widget_root, intent)
        return views
    }

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
