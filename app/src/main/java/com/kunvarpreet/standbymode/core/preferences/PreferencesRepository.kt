package com.kunvarpreet.standbymode.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kunvarpreet.standbymode.core.photoframe.PhotoFrameLogic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.clockDataStore: DataStore<Preferences> by preferencesDataStore(name = "standby_clock_preferences")

/**
 * Repository managing persistent clock preferences via Jetpack DataStore.
 */
class PreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val CLOCK_FACE = stringPreferencesKey("clock_face")
        val IS_24_HOUR = booleanPreferencesKey("is_24_hour")
        val SHOW_SECONDS = booleanPreferencesKey("show_seconds")
        val SHOW_DATE = booleanPreferencesKey("show_date")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val IS_NIGHT_MODE_ENABLED = booleanPreferencesKey("is_night_mode_enabled")
        val SHOW_CHARGING_NOTIFICATION = booleanPreferencesKey("show_charging_notification")
        val IS_BURN_IN_PROTECTION_ENABLED = booleanPreferencesKey("is_burn_in_protection_enabled")
        val KEEP_SCREEN_AWAKE = booleanPreferencesKey("keep_screen_awake")
        val SHOW_BATTERY_STATUS = booleanPreferencesKey("show_battery_status")
        val SHOW_MEDIA_PLAYER = booleanPreferencesKey("show_media_player")
        val SHOW_CALENDAR_AGENDA = booleanPreferencesKey("show_calendar_agenda")
        val SHOW_WEATHER_WIDGET = booleanPreferencesKey("show_weather_widget")
        val SHOW_PHOTO_FRAME = booleanPreferencesKey("show_photo_frame")
        val PHOTO_FRAME_URIS = stringPreferencesKey("photo_frame_uris")
        val AUTO_ACTIVATE_ON_CHARGE_AND_LANDSCAPE = booleanPreferencesKey("auto_activate_on_charge_and_landscape")
        val IS_ONBOARDING_COMPLETED = booleanPreferencesKey("is_onboarding_completed")
    }

    val preferencesFlow: Flow<UserClockPreferences> = context.clockDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val clockFaceName = preferences[PreferencesKeys.CLOCK_FACE]
            val is24Hour = preferences[PreferencesKeys.IS_24_HOUR] ?: false
            val showSeconds = preferences[PreferencesKeys.SHOW_SECONDS] ?: false
            val showDate = preferences[PreferencesKeys.SHOW_DATE] ?: true
            val accentColorName = preferences[PreferencesKeys.ACCENT_COLOR]
            val isNightModeEnabled = preferences[PreferencesKeys.IS_NIGHT_MODE_ENABLED] ?: false
            val showChargingNotification = preferences[PreferencesKeys.SHOW_CHARGING_NOTIFICATION] ?: false
            val isBurnInProtectionEnabled = preferences[PreferencesKeys.IS_BURN_IN_PROTECTION_ENABLED] ?: true
            val keepScreenAwake = preferences[PreferencesKeys.KEEP_SCREEN_AWAKE] ?: true
            val showBatteryStatus = preferences[PreferencesKeys.SHOW_BATTERY_STATUS] ?: false
            val showMediaPlayer = preferences[PreferencesKeys.SHOW_MEDIA_PLAYER] ?: false
            val showCalendarAgenda = preferences[PreferencesKeys.SHOW_CALENDAR_AGENDA] ?: false
            val showWeatherWidget = preferences[PreferencesKeys.SHOW_WEATHER_WIDGET] ?: false
            val showPhotoFrame = preferences[PreferencesKeys.SHOW_PHOTO_FRAME] ?: false
            val rawPhotoUris = preferences[PreferencesKeys.PHOTO_FRAME_URIS]
            val photoFrameUris = PhotoFrameLogic.deserializeUriList(rawPhotoUris)
            val autoActivateOnChargeAndLandscape = preferences[PreferencesKeys.AUTO_ACTIVATE_ON_CHARGE_AND_LANDSCAPE] ?: false
            val isOnboardingCompleted = preferences[PreferencesKeys.IS_ONBOARDING_COMPLETED] ?: false

            UserClockPreferences(
                clockFace = ClockFaceType.fromName(clockFaceName),
                is24Hour = is24Hour,
                showSeconds = showSeconds,
                showDate = showDate,
                accentColor = AccentColorOption.fromName(accentColorName),
                isNightModeEnabled = isNightModeEnabled,
                showChargingNotification = showChargingNotification,
                isBurnInProtectionEnabled = isBurnInProtectionEnabled,
                keepScreenAwake = keepScreenAwake,
                showBatteryStatus = showBatteryStatus,
                showMediaPlayer = showMediaPlayer,
                showCalendarAgenda = showCalendarAgenda,
                showWeatherWidget = showWeatherWidget,
                showPhotoFrame = showPhotoFrame,
                photoFrameUris = photoFrameUris,
                autoActivateOnChargeAndLandscape = autoActivateOnChargeAndLandscape,
                isOnboardingCompleted = isOnboardingCompleted
            )
        }

    private suspend fun safeEdit(action: suspend (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        try {
            context.clockDataStore.edit(action)
        } catch (e: IOException) {
            android.util.Log.e("PreferencesRepository", "I/O error updating clock preferences in DataStore", e)
        }
    }

    suspend fun updateClockFace(clockFace: ClockFaceType) {
        safeEdit { preferences ->
            preferences[PreferencesKeys.CLOCK_FACE] = clockFace.name
        }
    }

    suspend fun update24Hour(is24Hour: Boolean) {
        safeEdit { preferences ->
            preferences[PreferencesKeys.IS_24_HOUR] = is24Hour
        }
    }

    suspend fun updateShowSeconds(showSeconds: Boolean) {
        safeEdit { preferences ->
            preferences[PreferencesKeys.SHOW_SECONDS] = showSeconds
        }
    }

    suspend fun updateShowDate(showDate: Boolean) {
        safeEdit { preferences ->
            preferences[PreferencesKeys.SHOW_DATE] = showDate
        }
    }

    suspend fun updateAccentColor(accentColor: AccentColorOption) {
        safeEdit { preferences ->
            preferences[PreferencesKeys.ACCENT_COLOR] = accentColor.name
        }
    }

    suspend fun updateNightMode(isNightModeEnabled: Boolean) {
        safeEdit { preferences ->
            preferences[PreferencesKeys.IS_NIGHT_MODE_ENABLED] = isNightModeEnabled
        }
    }

    suspend fun updateShowChargingNotification(showChargingNotification: Boolean) {
        safeEdit { preferences ->
            preferences[PreferencesKeys.SHOW_CHARGING_NOTIFICATION] = showChargingNotification
        }
    }

    suspend fun updateBurnInProtection(isBurnInProtectionEnabled: Boolean) {
        safeEdit { preferences ->
            preferences[PreferencesKeys.IS_BURN_IN_PROTECTION_ENABLED] = isBurnInProtectionEnabled
        }
    }

    suspend fun updateKeepScreenAwake(keepScreenAwake: Boolean) {
        safeEdit { preferences ->
            preferences[PreferencesKeys.KEEP_SCREEN_AWAKE] = keepScreenAwake
        }
    }

    suspend fun updateShowBatteryStatus(showBatteryStatus: Boolean) {
        safeEdit { preferences ->
            preferences[PreferencesKeys.SHOW_BATTERY_STATUS] = showBatteryStatus
        }
    }

    suspend fun updateShowMediaPlayer(showMediaPlayer: Boolean) {
        safeEdit { preferences ->
            preferences[PreferencesKeys.SHOW_MEDIA_PLAYER] = showMediaPlayer
        }
    }

    suspend fun updateShowCalendarAgenda(showCalendarAgenda: Boolean) {
        safeEdit { preferences ->
            preferences[PreferencesKeys.SHOW_CALENDAR_AGENDA] = showCalendarAgenda
        }
    }

    suspend fun updateShowWeatherWidget(showWeatherWidget: Boolean) {
        safeEdit { preferences ->
            preferences[PreferencesKeys.SHOW_WEATHER_WIDGET] = showWeatherWidget
        }
    }

    suspend fun updateShowPhotoFrame(showPhotoFrame: Boolean) {
        safeEdit { preferences ->
            preferences[PreferencesKeys.SHOW_PHOTO_FRAME] = showPhotoFrame
        }
    }

    suspend fun updatePhotoFrameUris(uris: List<String>) {
        val sanitized = PhotoFrameLogic.sanitizeUriList(uris)
        safeEdit { preferences ->
            preferences[PreferencesKeys.PHOTO_FRAME_URIS] = PhotoFrameLogic.serializeUriList(sanitized)
        }
    }

    suspend fun updateAutoActivateOnChargeAndLandscape(enabled: Boolean) {
        safeEdit { preferences ->
            preferences[PreferencesKeys.AUTO_ACTIVATE_ON_CHARGE_AND_LANDSCAPE] = enabled
        }
    }

    suspend fun updateOnboardingCompleted(completed: Boolean) {
        safeEdit { preferences ->
            preferences[PreferencesKeys.IS_ONBOARDING_COMPLETED] = completed
        }
    }
}
