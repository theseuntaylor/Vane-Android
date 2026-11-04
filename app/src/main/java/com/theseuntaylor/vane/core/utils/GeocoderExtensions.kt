package com.theseuntaylor.vane.core.utils

import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.resume

private const val TAG = "Geocoder"

/**
 * Addresses matching [name], or an empty list when the geocoder fails.
 * Android 13+ has an async listener API; older versions only have the blocking call, so it runs on IO.
 */
suspend fun Geocoder.findAddresses(name: String, maxResults: Int): List<Address> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        suspendCancellableCoroutine { continuation ->
            getFromLocationName(name, maxResults, object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<Address>) = continuation.resume(addresses)

                override fun onError(errorMessage: String?) {
                    Log.e(TAG, "Couldn't find addresses for \"$name\": $errorMessage")
                    continuation.resume(emptyList())
                }
            })
        }
    } else {
        withContext(Dispatchers.IO) {
            try {
                @Suppress("DEPRECATION")
                getFromLocationName(name, maxResults).orEmpty()
            } catch (e: IOException) {
                Log.e(TAG, "Couldn't find addresses for \"$name\"", e)
                emptyList()
            }
        }
    }

/** The address nearest to the coordinates, or null when the geocoder fails or finds nothing. */
suspend fun Geocoder.findAddress(latitude: Double, longitude: Double): Address? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        suspendCancellableCoroutine { continuation ->
            getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<Address>) =
                    continuation.resume(addresses.firstOrNull())

                override fun onError(errorMessage: String?) {
                    Log.e(TAG, "Couldn't find an address for the location: $errorMessage")
                    continuation.resume(null)
                }
            })
        }
    } else {
        withContext(Dispatchers.IO) {
            try {
                @Suppress("DEPRECATION")
                getFromLocation(latitude, longitude, 1)?.firstOrNull()
            } catch (e: IOException) {
                Log.e(TAG, "Couldn't find an address for the location", e)
                null
            }
        }
    }
