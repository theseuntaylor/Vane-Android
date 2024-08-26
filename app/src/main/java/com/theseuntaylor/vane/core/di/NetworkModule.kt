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

    @Provides
    @Singleton
    fun provideInterceptor(@ApplicationContext context: Context) = Interceptor { chain ->
        val originalRequest = chain.request()

        val request = if (hasNetwork(context)) {
            originalRequest
                .newBuilder()
                .cacheControl(
                    CacheControl.Builder().maxAge(15, TimeUnit.MINUTES).build()
                )
        } else {
            originalRequest
                .newBuilder()
                .cacheControl(
                    CacheControl.Builder().maxStale(1, TimeUnit.DAYS).build()
                )
        }

        chain.proceed(request.build())
    }

    @Provides
    @Singleton
    fun provideHttpClient(interceptor: Interceptor, okHttpCache: Cache): OkHttpClient {
        return builder(interceptor).cache(okHttpCache).build()
    }

    @Provides
    @Singleton
    fun provideOkHttpCache(@ApplicationContext context: Context): Cache = Cache(context.cacheDir, cacheSize)

    private fun builder(interceptor: Interceptor): OkHttpClient.Builder {
        return OkHttpClient.Builder()
            .readTimeout(BuildConfig.REQUEST_TIMEOUT, TimeUnit.SECONDS)
            .connectTimeout(BuildConfig.REQUEST_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(BuildConfig.REQUEST_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor()
                    .setLevel(HttpLoggingInterceptor.Level.BODY)
                    .setLevel(HttpLoggingInterceptor.Level.HEADERS)
            )
            .addNetworkInterceptor(interceptor)
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