package com.theseuntaylor.vane.feature.widget.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.theseuntaylor.vane.MainActivity
import com.theseuntaylor.vane.core.navigation.DetailedForecast
import com.theseuntaylor.vane.feature.widget.data.FavouriteWeather

/** Intents from the widget into the app, and reading them back in [MainActivity]. */
object WidgetLinks {
    private const val EXTRA_NAME = "com.theseuntaylor.vane.widget.NAME"
    private const val EXTRA_LATITUDE = "com.theseuntaylor.vane.widget.LATITUDE"
    private const val EXTRA_LONGITUDE = "com.theseuntaylor.vane.widget.LONGITUDE"

    /**
     * Opens [favourite]'s details, or null when the saved row has no coordinates (data from
     * before they were saved). It starts a fresh task, so the details open on top of the
     * Favourites tab whatever the app was showing.
     */
    fun openFavourite(context: Context, favourite: FavouriteWeather): Intent? {
        val latitude = favourite.latitude ?: return null
        val longitude = favourite.longitude ?: return null
        return Intent(context, MainActivity::class.java)
            // A distinct URI per place, so Android doesn't treat the rows' intents as the same one.
            .setData(Uri.parse("vane://favourite/$latitude,$longitude"))
            .putExtra(EXTRA_NAME, favourite.name)
            .putExtra(EXTRA_LATITUDE, latitude)
            .putExtra(EXTRA_LONGITUDE, longitude)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }

    /** The details screen an [openFavourite] intent asks for, or null for any other intent. */
    fun detailedForecast(intent: Intent?): DetailedForecast? {
        intent ?: return null
        val name = intent.getStringExtra(EXTRA_NAME) ?: return null
        if (!intent.hasExtra(EXTRA_LATITUDE) || !intent.hasExtra(EXTRA_LONGITUDE)) return null
        return DetailedForecast(
            location = name,
            latitude = intent.getDoubleExtra(EXTRA_LATITUDE, 0.0).toString(),
            longitude = intent.getDoubleExtra(EXTRA_LONGITUDE, 0.0).toString(),
        )
    }
}
