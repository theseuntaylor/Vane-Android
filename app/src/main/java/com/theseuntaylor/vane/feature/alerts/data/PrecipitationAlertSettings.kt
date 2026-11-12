package com.theseuntaylor.vane.feature.alerts.data

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Whether rain and snow alerts are on, and which event the user was last alerted about. */
@Singleton
class PrecipitationAlertSettings @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = preferences.getBoolean(KEY_ENABLED, false)
        set(value) = preferences.edit { putBoolean(KEY_ENABLED, value) }

    /** When the last alerted rain or snow was due to start, or null if there has been none. */
    var lastAlertedStartMillis: Long?
        get() = preferences.getLong(KEY_LAST_ALERTED, -1L).takeIf { it >= 0 }
        set(value) = preferences.edit {
            if (value == null) remove(KEY_LAST_ALERTED) else putLong(KEY_LAST_ALERTED, value)
        }

    private companion object {
        const val PREFERENCES_NAME = "vane_alerts"
        const val KEY_ENABLED = "enabled"
        const val KEY_LAST_ALERTED = "last_alerted_start"
    }
}
