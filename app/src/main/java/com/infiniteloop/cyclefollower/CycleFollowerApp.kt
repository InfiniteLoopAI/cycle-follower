package com.infiniteloop.cyclefollower

import android.app.Application
import com.infiniteloop.cyclefollower.notify.DailyHintScheduler
import com.infiniteloop.cyclefollower.notify.HeadsUp

class CycleFollowerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DailyHintScheduler.createChannel(this)
        HeadsUp.createChannel(this)
        DailyHintScheduler.rescheduleFromProfile(this)
        HeadsUp.rescheduleFromProfile(this)
    }
}
