package com.theseuntaylor.vane.feature.home.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import java.util.Locale

fun hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

@SuppressLint("MissingPermission")
fun getCurrentLocation(context: Context, callback: (Double, Double, Long, String) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    fusedLocationClient.lastLocation
        .addOnSuccessListener { location ->
            if (location != null) {
                val lat = location.latitude
                val long = location.longitude
                val time = location.time

                val locationFromCoordinates =
                    getLocationFromCoordinates(context, long = long, lat = lat)

                callback(
                    lat, long, time, locationFromCoordinates
                )
            }
        }
        .addOnFailureListener { exception ->
            exception.printStackTrace()
        }
}

fun getLocationFromCoordinates(context: Context, long: Double, lat: Double): String {

    if (!Geocoder.isPresent()) {
        return "Service not available for your device."
    }
    return Geocoder(context, Locale.getDefault()).getFromLocation(
        lat, long,
        1
    )?.first()?.subAdminArea ?: "Error getting your current Location :("
}

fun searchLocation(context: Context, searchedAddress: String): MutableList<Address>? {

    if (!Geocoder.isPresent()) return emptyList<Address>().toMutableList()
    return Geocoder(context).getFromLocationName(
        searchedAddress,
        5
    )

}