package com.infiniteloop.cyclefollower.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cycle_follower")

/**
 * Single source of truth for the profile. Stored as one JSON blob in DataStore, which keeps
 * migrations trivial: unknown fields are ignored and missing fields fall back to defaults.
 */
class ProfileRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val profile: Flow<UserProfile> = context.dataStore.data.map { prefs ->
        prefs[PROFILE_KEY]?.let { raw ->
            runCatching { json.decodeFromString(UserProfile.serializer(), raw) }.getOrElse { UserProfile() }
        } ?: UserProfile()
    }

    suspend fun current(): UserProfile = profile.first()

    suspend fun update(transform: (UserProfile) -> UserProfile) {
        context.dataStore.edit { prefs ->
            val existing = prefs[PROFILE_KEY]
                ?.let { runCatching { json.decodeFromString(UserProfile.serializer(), it) }.getOrNull() }
                ?: UserProfile()
            prefs[PROFILE_KEY] = json.encodeToString(UserProfile.serializer(), transform(existing).normalised())
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }

    companion object {
        private val PROFILE_KEY = stringPreferencesKey("profile_json")

        @Volatile
        private var instance: ProfileRepository? = null

        fun get(context: Context): ProfileRepository =
            instance ?: synchronized(this) {
                instance ?: ProfileRepository(context.applicationContext).also { instance = it }
            }
    }
}
