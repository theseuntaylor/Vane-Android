package com.theseuntaylor.vane.core.di

import android.content.Context
import androidx.room.Room
import com.theseuntaylor.vane.core.local.FavouriteLocationsDao
import com.theseuntaylor.vane.core.local.VaneDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
class DataModule {

    @Singleton
    @Provides
    fun provideDataBase(@ApplicationContext context: Context): VaneDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            VaneDatabase::class.java,
            "vane.db"
        ).build()
    }

    @Provides
    fun provideFavouriteLocationsDao(database: VaneDatabase): FavouriteLocationsDao =
        database.favouriteLocationsDao()
}