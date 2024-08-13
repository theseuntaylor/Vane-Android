package com.theseuntaylor.vane.core.di

import android.content.Context
import android.location.Geocoder
import com.google.android.gms.location.LocationServices
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.Locale
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class LocationModule {

    @Provides
    @Singleton
    fun provideGeocoder(@ApplicationContext context: Context) =
        Geocoder(context, Locale.getDefault())

    @Provides
    @Singleton
    fun provideFusedLocationClient(@ApplicationContext context: Context) =
        LocationServices.getFusedLocationProviderClient(context)

}