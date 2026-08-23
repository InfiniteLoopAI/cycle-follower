package com.infiniteloop.cyclefollower.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Alarms do not survive a reboot or an app update, so book them again. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            DailyHintScheduler.createChannel(context.applicationContext)
            HeadsUp.createChannel(context.applicationContext)
            DailyHintScheduler.rescheduleFromProfile(context.applicationContext)
            HeadsUp.rescheduleFromProfile(context.applicationContext)
        }
    }
}
