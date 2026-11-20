package com.kunvarpreet.standbymode.core.configuration

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kunvarpreet.standbymode.core.preferences.clockDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Repository responsible for persisting and mutating [DisplayProfile]s and pane widget configurations.
 * Handles schema versioning and guarantees configuration isolation across panes.
 */
class DisplayProfileRepository(
    private val dataStore: DataStore<Preferences>
) {
    constructor(context: Context) : this(context.clockDataStore)

    companion object {
        val KEY_SCHEMA_VERSION = intPreferencesKey("profile_schema_version")
        val KEY_ACTIVE_PROFILE_JSON = stringPreferencesKey("active_display_profile_json")
    }

    val profileFlow: Flow<DisplayProfile> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val profileJson = preferences[KEY_ACTIVE_PROFILE_JSON]
            DisplayProfileSerializer.deserializeProfile(profileJson)
        }

    /**
     * Updates the active widget index for a specific pane.
     */
    suspend fun updatePaneSelectedIndex(paneId: String, newIndex: Int) {
        updateProfile { current ->
            current.updatePane(paneId) { pane ->
                pane.withSafeIndex(newIndex)
            }
        }
    }

    /**
     * Updates the clock appearance of a specific widget instance in a specific pane.
     * Guarantees that changes to this instance do NOT affect any other widget instance.
     */
    suspend fun updateWidgetClockAppearance(
        paneId: String,
        instanceId: String,
        appearance: ClockAppearanceConfig
    ) {
        updateProfile { current ->
            current.updatePane(paneId) { pane ->
                val updatedWidgets = pane.widgets.map { widget ->
                    if (widget.instanceId == instanceId) {
                        widget.copy(clockAppearance = appearance)
                    } else {
                        widget
                    }
                }
                pane.copy(widgets = updatedWidgets)
            }
        }
    }

    /**
     * Appends a new widget instance to a pane stack.
     */
    suspend fun addWidgetToPane(paneId: String, widget: WidgetInstanceConfig) {
        updateProfile { current ->
            current.updatePane(paneId) { pane ->
                pane.copy(widgets = pane.widgets + widget)
            }
        }
    }

    /**
     * Removes a widget instance by ID from a pane stack.
     * Clamps selected index safely so active position remains valid.
     */
    suspend fun removeWidgetFromPane(paneId: String, instanceId: String) {
        updateProfile { current ->
            current.updatePane(paneId) { pane ->
                val remaining = pane.widgets.filterNot { it.instanceId == instanceId }
                val newIndex = if (remaining.isEmpty()) 0 else pane.selectedIndex.coerceIn(0, remaining.lastIndex)
                pane.copy(widgets = remaining, selectedIndex = newIndex)
            }
        }
    }

    /**
     * Reorders widgets in a pane according to the provided list of instance IDs.
     */
    suspend fun reorderPaneWidgets(paneId: String, newOrderInstanceIds: List<String>) {
        updateProfile { current ->
            current.updatePane(paneId) { pane ->
                val widgetMap = pane.widgets.associateBy { it.instanceId }
                val reordered = newOrderInstanceIds.mapNotNull { widgetMap[it] }
                // Append any unmentioned widgets to avoid losing data
                val remaining = pane.widgets.filter { it.instanceId !in newOrderInstanceIds }
                val finalList = reordered + remaining
                val newIndex = if (finalList.isEmpty()) 0 else pane.selectedIndex.coerceIn(0, finalList.lastIndex)
                pane.copy(widgets = finalList, selectedIndex = newIndex)
            }
        }
    }

    /**
     * Replaces a specific widget instance with a new widget.
     */
    suspend fun replaceWidgetInPane(
        paneId: String,
        oldInstanceId: String,
        newWidget: WidgetInstanceConfig
    ) {
        updateProfile { current ->
            current.updatePane(paneId) { pane ->
                val updated = pane.widgets.map { widget ->
                    if (widget.instanceId == oldInstanceId) newWidget else widget
                }
                pane.copy(widgets = updated)
            }
        }
    }

    /**
     * Saves an entire [DisplayProfile] transactionally.
     */
    suspend fun saveProfile(profile: DisplayProfile) {
        val serialized = DisplayProfileSerializer.serializeProfile(profile)
        dataStore.edit { preferences ->
            preferences[KEY_SCHEMA_VERSION] = DisplayProfileSerializer.CURRENT_SCHEMA_VERSION
            preferences[KEY_ACTIVE_PROFILE_JSON] = serialized
        }
    }

    private suspend fun updateProfile(transform: (DisplayProfile) -> DisplayProfile) {
        dataStore.edit { preferences ->
            val currentJson = preferences[KEY_ACTIVE_PROFILE_JSON]
            val current = DisplayProfileSerializer.deserializeProfile(currentJson)
            val updated = transform(current)
            preferences[KEY_SCHEMA_VERSION] = DisplayProfileSerializer.CURRENT_SCHEMA_VERSION
            preferences[KEY_ACTIVE_PROFILE_JSON] = DisplayProfileSerializer.serializeProfile(updated)
        }
    }
}
