package com.theseuntaylor.vane

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class VaneApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        Log.e("Application", "Vane Application Created! :)")
    }

}