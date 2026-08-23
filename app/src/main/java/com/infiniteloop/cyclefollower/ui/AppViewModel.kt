package com.infiniteloop.cyclefollower.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.infiniteloop.cyclefollower.data.ProfileRepository
import com.infiniteloop.cyclefollower.data.UserProfile
import com.infiniteloop.cyclefollower.notify.DailyHintScheduler
import com.infiniteloop.cyclefollower.notify.HeadsUp
import com.infiniteloop.cyclefollower.widget.CycleWidgetProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProfileRepository.get(application)

    /** null while the first read is still in flight. */
    val profile: StateFlow<UserProfile?> =
        repository.profile.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun update(transform: (UserProfile) -> UserProfile) {
        viewModelScope.launch {
            repository.update(transform)
            afterChange()
        }
    }

    fun logPeriodStart(date: LocalDate) = update { it.withPeriodStart(date) }

    fun removePeriodStart(date: LocalDate) = update { it.withoutPeriodStart(date) }

    fun completeSetup() = update { it.copy(setupComplete = true) }

    /** Wholesale replacement, used by restore. */
    fun replaceProfile(profile: UserProfile) = update { profile }

    fun resetEverything() {
        viewModelScope.launch {
            repository.clear()
            DailyHintScheduler.cancel(getApplication())
            CycleWidgetProvider.refresh(getApplication())
        }
    }

    private fun afterChange() {
        val context = getApplication<Application>()
        DailyHintScheduler.rescheduleFromProfile(context)
        HeadsUp.rescheduleFromProfile(context)
        CycleWidgetProvider.refresh(context)
    }
}
