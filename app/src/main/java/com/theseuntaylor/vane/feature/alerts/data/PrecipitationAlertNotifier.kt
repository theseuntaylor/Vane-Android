package com.theseuntaylor.vane.feature.alerts.data

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.theseuntaylor.vane.MainActivity
import com.theseuntaylor.vane.R
import com.theseuntaylor.vane.feature.alerts.domain.PrecipitationKind
import com.theseuntaylor.vane.feature.alerts.domain.UpcomingPrecipitation
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.roundToLong

class PrecipitationAlertNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val notificationManager = NotificationManagerCompat.from(context)

    /** False when the user turned notifications off for Vane, or hasn't allowed them (Android 13+). */
    fun canNotify(): Boolean = notificationManager.areNotificationsEnabled()

    @SuppressLint("MissingPermission") // canNotify() checks the permission.
    fun notify(precipitation: UpcomingPrecipitation, locationName: String, nowMillis: Long): Boolean {
        if (!canNotify()) return false
        createChannel()

        val time = DateFormat.getTimeFormat(context).format(Date(precipitation.startsAtMillis))
        val minutes = roundToFive(TimeUnit.MILLISECONDS.toMinutes(precipitation.startsAtMillis - nowMillis))
        val (title, icon) = when (precipitation.kind) {
            PrecipitationKind.RAIN -> R.string.alert_rain_title to R.drawable.ic_weather_rain
            PrecipitationKind.SNOW -> R.string.alert_snow_title to R.drawable.ic_weather_snow
            PrecipitationKind.THUNDERSTORM -> R.string.alert_thunderstorm_title to R.drawable.ic_weather_thunderstorm
        }
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(context.getString(title, time))
            .setContentText(
                context.resources.getQuantityString(R.plurals.alert_body, minutes.toInt(), locationName, minutes.toInt())
            )
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            // Gone once the rain or snow should have started.
            .setTimeoutAfter((precipitation.startsAtMillis - nowMillis).coerceAtLeast(0))
            .build()
        // One id: a newer alert replaces an older one instead of stacking.
        notificationManager.notify(NOTIFICATION_ID, notification)
        return true
    }

    private fun createChannel() {
        notificationManager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.alert_channel_name))
                .setDescription(context.getString(R.string.alert_channel_description))
                .build()
        )
    }

    private fun roundToFive(minutes: Long): Long = ((minutes / 5.0).roundToLong() * 5).coerceAtLeast(5)

    private companion object {
        const val CHANNEL_ID = "precipitation_alerts"
        const val NOTIFICATION_ID = 1001
    }
}
