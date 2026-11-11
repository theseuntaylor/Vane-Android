package com.theseuntaylor.vane.core.di

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.theseuntaylor.vane.BuildConfig
import com.theseuntaylor.vane.core.remote.RemoteDataSource
import com.theseuntaylor.vane.core.utils.hasNetwork
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class NetworkModule {

    private val cacheSize = (5 * 1024 * 1024).toLong()

    @Provides
    @Singleton
    fun provideRemoteDataSource(retrofit: Retrofit): RemoteDataSource =
        retrofit.create(RemoteDataSource::class.java)

    /**
     * Open-Meteo sends no cache headers, so OkHttp would never reuse a response.
     * Runs only for requests that reach the network: marks responses fresh for 15 minutes.
     */
    private val cacheForecastsInterceptor = Interceptor { chain ->
        chain.proceed(chain.request())
            .newBuilder()
            .header("Cache-Control", CacheControl.Builder().maxAge(15, TimeUnit.MINUTES).build().toString())
            .removeHeader("Pragma")
            .build()
    }

    /**
     * Runs for every request, before the cache: when offline, serve a cached forecast up to a day old
     * instead of trying the network. Network interceptors can't do this, because OkHttp skips them offline.
     */
    private fun offlineCacheInterceptor(context: Context) = Interceptor { chain ->
        val request = if (hasNetwork(context)) {
            chain.request()
        } else {
            chain.request()
                .newBuilder()
                .cacheControl(CacheControl.Builder().onlyIfCached().maxStale(1, TimeUnit.DAYS).build())
                .build()
        }
        chain.proceed(request)
    }

    @Provides
    @Singleton
    fun provideHttpClient(@ApplicationContext context: Context, okHttpCache: Cache): OkHttpClient {
        return builder(context).cache(okHttpCache).build()
    }

    @Provides
    @Singleton
    fun provideOkHttpCache(@ApplicationContext context: Context): Cache = Cache(context.cacheDir, cacheSize)

    private fun builder(context: Context): OkHttpClient.Builder {
        return OkHttpClient.Builder()
            .readTimeout(BuildConfig.REQUEST_TIMEOUT, TimeUnit.SECONDS)
            .connectTimeout(BuildConfig.REQUEST_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(BuildConfig.REQUEST_TIMEOUT, TimeUnit.SECONDS)
            .apply {
                // Full request and response logging is for debugging only; release builds log nothing.
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                }
            }
            .addInterceptor(offlineCacheInterceptor(context))
            .addNetworkInterceptor(cacheForecastsInterceptor)
    }

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().setLenient().create()

    @Provides
    @Singleton
    fun provideRetrofit(httpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create(gson))
            .baseUrl(BuildConfig.BASE_URL).client(httpClient).build()
    }

}