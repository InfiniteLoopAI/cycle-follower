package com.infiniteloop.cyclefollower.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity

/**
 * Whatever the phone already uses -- fingerprint, face or the device PIN. The app deliberately
 * does not invent its own password: another secret to remember is a worse lock than the one the
 * owner already unlocks the phone with.
 */
object AppLock {

    private const val ALLOWED = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

    enum class Availability { READY, NONE_ENROLLED, UNSUPPORTED }

    fun availability(context: Context): Availability =
        when (BiometricManager.from(context).canAuthenticate(ALLOWED)) {
            BiometricManager.BIOMETRIC_SUCCESS -> Availability.READY
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> Availability.NONE_ENROLLED
            else -> Availability.UNSUPPORTED
        }

    fun canLock(context: Context): Boolean = availability(context) == Availability.READY

    fun prompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
    ) {
        val prompt = BiometricPrompt(
            activity,
            androidx.core.content.ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()

                override fun onAuthenticationError(code: Int, message: CharSequence) {
                    // A cancel is the user changing their mind, not a failure worth shouting about.
                    if (code == BiometricPrompt.ERROR_USER_CANCELED ||
                        code == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        code == BiometricPrompt.ERROR_CANCELED
                    ) {
                        onFailure("")
                    } else {
                        onFailure(message.toString())
                    }
                }
            },
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Cycle Follower is locked")
                .setSubtitle("Unlock to open it")
                .setAllowedAuthenticators(ALLOWED)
                .build(),
        )
    }
}
