package com.infiniteloop.cyclefollower.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.infiniteloop.cyclefollower.data.ProfileRepository
import com.infiniteloop.cyclefollower.widget.CycleWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Fires once a day: shows the hint, refreshes the widget, then books tomorrow's alarm. */
class DailyHintReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val profile = runCatching { ProfileRepository.get(appContext).current() }.getOrNull()
                if (profile != null) {
                    if (profile.dailyNotification && profile.setupComplete) {
                        DailyHintScheduler.showNotification(appContext, profile)
                    }
                    // Always book the next one, even if today's was skipped.
                    DailyHintScheduler.schedule(appContext, profile)
                }
                CycleWidgetProvider.refresh(appContext)
            } finally {
                pending.finish()
            }
        }
    }
}
