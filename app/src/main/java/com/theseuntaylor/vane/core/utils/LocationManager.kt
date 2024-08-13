package com.theseuntaylor.vane.core.utils

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

fun hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}