package com.theseuntaylor.vane.feature.widget.data

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.theseuntaylor.vane.core.local.FavouriteLocationsDao
import com.theseuntaylor.vane.core.remote.RemoteDataSource
import com.theseuntaylor.vane.core.remote.model.WeatherForecastResponse
import com.theseuntaylor.vane.feature.widget.ui.VaneWeatherWidget
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetWeatherRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: WidgetWeatherStore,
    private val remoteDataSource: RemoteDataSource,
    private val favouriteLocationsDao: FavouriteLocationsDao,
) {

    /** Called when the app loads the current location's forecast in the foreground. */
    suspend fun onCurrentLocationForecast(response: WeatherForecastResponse, locationName: String) {
        val snapshot = response.toWidgetSnapshot(locationName)
        store.save(snapshot)
        VaneWeatherWidget().updateAll(context)
        // The home screen fetches today only, so late in the evening the widget runs short of
        // upcoming hours; fetch tomorrow too, just for the widget.
        if (snapshot.upcomingHours.size < UPCOMING_HOURS) refresh()
    }

    /** Called when the app loads the favourites' forecasts in the foreground. */
    suspend fun onFavouritesForecast(favourites: List<FavouriteWeather>) {
        store.saveFavourites(favourites)
        VaneWeatherWidget().updateAll(context)
    }

    /**
     * Re-fetches the last location the app saw and every saved favourite. Each part is skipped
     * when there is nothing to refresh, and a failure leaves the previous data on the widget.
     */
    suspend fun refresh() {
        store.read()?.let { previous ->
            // Two days so the upcoming hours don't run out late in the evening.
            val response = remoteDataSource.getWeatherForecast(
                latitude = previous.latitude.toString(),
                longitude = previous.longitude.toString(),
                forecastDays = 2,
            )
            store.save(response.toWidgetSnapshot(previous.locationName))
        }
        refreshFavourites()
        VaneWeatherWidget().updateAll(context)
    }

    private suspend fun refreshFavourites() {
        val locations = favouriteLocationsDao.getFavouriteLocations()
        if (locations.isEmpty()) {
            store.saveFavourites(emptyList())
            return
        }
        val latitudes = locations.joinToString(",") { it.latitude.toString() }
        val longitudes = locations.joinToString(",") { it.longitude.toString() }
        // Open-Meteo answers one coordinate with an object and several with a list, in request order.
        val responses = if (locations.size == 1) {
            listOf(remoteDataSource.getWeatherForecast(latitude = latitudes, longitude = longitudes))
        } else {
            remoteDataSource.getFavouriteLocationsWeatherForecast(
                latitude = latitudes,
                longitude = longitudes,
                forecastDays = 1,
            )
        }
        store.saveFavourites(
            locations.zip(responses) { location, response ->
                val highAndLow = response.todayHighAndLow()
                FavouriteWeather(
                    name = location.name,
                    temperature = response.current.temperature_2m,
                    weatherCode = response.current.weatherCode,
                    high = highAndLow?.first,
                    low = highAndLow?.second,
                )
            }
        )
    }
}
