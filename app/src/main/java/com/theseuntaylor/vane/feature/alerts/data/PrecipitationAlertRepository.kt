package com.theseuntaylor.vane.feature.alerts.data

import android.content.Context
import android.util.Log
import com.theseuntaylor.vane.BuildConfig
import com.theseuntaylor.vane.core.remote.RemoteDataSource
import com.theseuntaylor.vane.feature.alerts.domain.NowcastResult
import com.theseuntaylor.vane.feature.alerts.domain.findUpcomingPrecipitation
import com.theseuntaylor.vane.feature.widget.data.WidgetWeatherStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Checks the 15-minute forecast for the last place the app found, and alerts once per rain or
 * snow event. Uses the location the home screen last saved, so no background location is needed.
 */
@Singleton
class PrecipitationAlertRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: PrecipitationAlertSettings,
    private val widgetStore: WidgetWeatherStore,
    private val remoteDataSource: RemoteDataSource,
    private val notifier: PrecipitationAlertNotifier,
) {

    // Two checks at once would both see "not alerted yet" and post the same alert twice.
    private val checking = Mutex()

    /** Throws on a network or parsing error, so the caller can retry. */
    suspend fun check(nowMillis: Long = System.currentTimeMillis()) = checking.withLock { checkLocked(nowMillis) }

    private suspend fun checkLocked(nowMillis: Long) {
        if (!settings.enabled) return log("Alerts are off")
        if (!notifier.canNotify()) return log("Notifications aren't allowed")
        val location = widgetStore.read() ?: return log("No saved location yet")

        val nowcast = remoteDataSource.getPrecipitationNowcast(
            url = nowcastUrl(),
            latitude = location.latitude.toString(),
            longitude = location.longitude.toString(),
        )
        when (val result = nowcast.findUpcomingPrecipitation(nowMillis)) {
            NowcastResult.Dry -> log("No rain or snow in the next 90 minutes")
            NowcastResult.AlreadyFalling -> log("Already raining or snowing; no alert")
            is NowcastResult.Upcoming -> {
                val upcoming = result.precipitation
                val last = settings.lastAlertedStartMillis
                // The forecast start time drifts from run to run; treat nearby starts as one event.
                if (last != null && abs(upcoming.startsAtMillis - last) < SAME_EVENT_MILLIS) {
                    return log("Already alerted about this ${upcoming.kind}")
                }
                if (notifier.notify(upcoming, location.locationName, nowMillis)) {
                    settings.lastAlertedStartMillis = upcoming.startsAtMillis
                    log("Posted ${upcoming.kind} alert for ${location.locationName}")
                }
            }
        }
    }

    private fun nowcastUrl(): String {
        // Debug builds only: the E2E check serves a canned forecast from this base URL.
        val override = if (BuildConfig.DEBUG) {
            File(context.filesDir, E2E_BASE_URL_FILE).takeIf { it.exists() }?.readText()?.trim()
        } else {
            null
        }
        return (override?.takeIf { it.isNotEmpty() } ?: BuildConfig.BASE_URL) + "v1/forecast"
    }

    private fun log(message: String) {
        Log.i(TAG, message)
    }

    private companion object {
        const val TAG = "RainAlerts"
        const val E2E_BASE_URL_FILE = "e2e-alerts-base-url"
        val SAME_EVENT_MILLIS = TimeUnit.HOURS.toMillis(2)
    }
}
