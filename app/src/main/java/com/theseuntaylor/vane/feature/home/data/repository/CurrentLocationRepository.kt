package com.theseuntaylor.vane.feature.home.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import com.theseuntaylor.vane.R
import com.theseuntaylor.vane.core.utils.findAddress
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed interface CurrentLocationResult {
    data class Found(val latitude: Double, val longitude: Double, val name: String) : CurrentLocationResult

    /** Location is turned off for the whole device. */
    data object LocationOff : CurrentLocationResult

    /** No fix within the time limit, or the location request failed. */
    data object NotFound : CurrentLocationResult
}

/** Finds where the device is and what the place is called. Callers must hold a location permission. */
class CurrentLocationRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val geocoder: Geocoder,
    private val fusedLocationProviderClient: FusedLocationProviderClient,
) {

    suspend fun currentLocation(): CurrentLocationResult {
        val locationManager = context.getSystemService(LocationManager::class.java)
        if (locationManager == null || !LocationManagerCompat.isLocationEnabled(locationManager)) {
            return CurrentLocationResult.LocationOff
        }
        val location = try {
            // The cached fix is instant; without one (e.g. right after a reboot), ask for a fresh one.
            lastLocation() ?: freshLocation()
        } catch (e: Exception) {
            Log.e(TAG, "Couldn't find the current location", e)
            null
        } ?: return CurrentLocationResult.NotFound

        return CurrentLocationResult.Found(
            latitude = location.latitude,
            longitude = location.longitude,
            name = placeName(latitude = location.latitude, longitude = location.longitude),
        )
    }

    @SuppressLint("MissingPermission")
    private suspend fun lastLocation(): Location? = fusedLocationProviderClient.lastLocation.await()

    @SuppressLint("MissingPermission")
    private suspend fun freshLocation(): Location? {
        // Without a time limit the request can wait for a fix forever, leaving the home screen loading.
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setDurationMillis(LOCATION_TIMEOUT_MILLIS)
            .build()
        val cancellation = CancellationTokenSource()
        return try {
            fusedLocationProviderClient.getCurrentLocation(request, cancellation.token).await()
        } finally {
            // Stops the request if the caller was cancelled (e.g. the user left the screen).
            cancellation.cancel()
        }
    }

    private suspend fun placeName(latitude: Double, longitude: Double): String {
        if (!Geocoder.isPresent()) {
            return context.getString(R.string.geocoder_unavailable)
        }
        val address = geocoder.findAddress(latitude = latitude, longitude = longitude)
        return address?.subAdminArea
            ?: address?.countryName
            ?: context.getString(R.string.location_name_unavailable)
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { continuation.resume(it) }
        addOnFailureListener { continuation.resumeWithException(it) }
        addOnCanceledListener { continuation.cancel() }
    }

    private companion object {
        const val TAG = "CurrentLocation"
        const val LOCATION_TIMEOUT_MILLIS = 20_000L
    }
}
