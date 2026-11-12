package com.theseuntaylor.vane.feature.alerts.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import com.theseuntaylor.vane.feature.alerts.data.PrecipitationAlertNotifier
import com.theseuntaylor.vane.feature.alerts.data.PrecipitationAlertSettings
import com.theseuntaylor.vane.feature.alerts.data.PrecipitationAlertWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class RainAlertsUiState(
    /** Alerts are on and Vane may post notifications. */
    val on: Boolean = false,
    /** The user wants alerts but notifications are blocked, so they must allow them in Settings. */
    val notificationsBlocked: Boolean = false,
)

@HiltViewModel
class RainAlertsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: PrecipitationAlertSettings,
    private val notifier: PrecipitationAlertNotifier,
) : ViewModel() {

    // Set when the user tried to turn alerts on but notifications are blocked; cleared once allowed.
    private var blockedOnTurnOn = false

    private val _uiState = MutableStateFlow(currentState())
    val uiState: StateFlow<RainAlertsUiState> = _uiState.asStateFlow()

    /** Notifications may have been allowed or blocked in Settings while Vane was in the background. */
    fun refresh() {
        _uiState.value = currentState()
    }

    /**
     * Turns alerts on if Vane may post notifications. Otherwise (the permission prompt was declined,
     * or notifications are off in Settings) alerts stay off and the switch explains why.
     */
    fun turnOn() {
        if (notifier.canNotify()) {
            settings.enabled = true
            PrecipitationAlertWorker.schedule(context, checkNow = true)
        } else {
            blockedOnTurnOn = true
        }
        refresh()
    }

    fun turnOff() {
        blockedOnTurnOn = false
        settings.enabled = false
        PrecipitationAlertWorker.cancel(context)
        refresh()
    }

    private fun currentState(): RainAlertsUiState {
        val canNotify = notifier.canNotify()
        if (canNotify) blockedOnTurnOn = false
        if (settings.enabled && canNotify) {
            // Keeps the periodic check scheduled, e.g. after notifications were allowed in Settings.
            PrecipitationAlertWorker.schedule(context, checkNow = false)
        }
        return RainAlertsUiState(
            on = settings.enabled && canNotify,
            // Also when notifications were turned off in Settings after alerts were turned on.
            notificationsBlocked = !canNotify && (blockedOnTurnOn || settings.enabled),
        )
    }
}
