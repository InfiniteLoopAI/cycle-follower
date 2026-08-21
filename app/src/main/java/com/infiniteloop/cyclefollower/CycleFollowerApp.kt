package com.infiniteloop.cyclefollower

import android.app.Application
import com.infiniteloop.cyclefollower.notify.DailyHintScheduler

class CycleFollowerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DailyHintScheduler.createChannel(this)
        DailyHintScheduler.rescheduleFromProfile(this)
    }
}
