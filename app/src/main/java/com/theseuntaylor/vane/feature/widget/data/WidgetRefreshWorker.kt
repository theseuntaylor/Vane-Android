package com.theseuntaylor.vane.feature.widget.data

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class WidgetRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: WidgetWeatherRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        repository.refresh()
        Result.success()
    } catch (e: Exception) {
        // The widget keeps showing the previous snapshot; try again on the next window.
        Log.e(TAG, "Widget refresh failed", e)
        Result.retry()
    }

    companion object {
        private const val TAG = "WidgetRefreshWorker"
        private const val UNIQUE_WORK_NAME = "vane_widget_refresh"
        private const val REFRESH_INTERVAL_MINUTES = 30L

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(
                REFRESH_INTERVAL_MINUTES, TimeUnit.MINUTES
            ).setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        }
    }
}
