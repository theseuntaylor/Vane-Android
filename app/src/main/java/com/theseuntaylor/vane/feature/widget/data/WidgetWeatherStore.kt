package com.theseuntaylor.vane.feature.widget.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** Everything the widget renders, read from disk so it works with the app closed and offline. */
data class WidgetState(
    val current: WidgetWeatherSnapshot?,
    val favourites: List<FavouriteWeather>,
)

/**
 * Persists the widget's data on disk so the widget can render while the app process is dead
 * and the device is offline.
 */
@Singleton
class WidgetWeatherStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun read(): WidgetWeatherSnapshot? = decode(KEY_SNAPSHOT)

    fun readFavourites(): List<FavouriteWeather> = decode(KEY_FAVOURITES) ?: emptyList()

    fun readState() = WidgetState(current = read(), favourites = readFavourites())

    fun save(snapshot: WidgetWeatherSnapshot) {
        preferences.edit().putString(KEY_SNAPSHOT, json.encodeToString(snapshot)).apply()
    }

    fun saveFavourites(favourites: List<FavouriteWeather>) {
        preferences.edit().putString(KEY_FAVOURITES, json.encodeToString(favourites)).apply()
    }

    fun observe(): Flow<WidgetState> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_SNAPSHOT || key == KEY_FAVOURITES) trySend(readState())
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        send(readState())
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    private inline fun <reified T> decode(key: String): T? {
        val raw = preferences.getString(key, null) ?: return null
        return try {
            json.decodeFromString<T>(raw)
        } catch (e: SerializationException) {
            Log.e(TAG, "Discarding unreadable widget data for $key", e)
            null
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Discarding unreadable widget data for $key", e)
            null
        }
    }

    private companion object {
        const val TAG = "WidgetWeatherStore"
        const val PREFERENCES_NAME = "vane_widget"
        const val KEY_SNAPSHOT = "snapshot"
        const val KEY_FAVOURITES = "favourites"
    }
}
