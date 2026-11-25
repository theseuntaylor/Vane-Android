package com.theseuntaylor.vane.feature.alerts.data

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
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

@HiltWorker
class PrecipitationAlertWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: PrecipitationAlertRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        repository.check()
        Result.success()
    } catch (e: CancellationException) {
        throw e // WorkManager stopped the job; it reschedules it itself
    } catch (e: Exception) {
        // No alert this time; a periodic run tries again in its next window.
        Log.e(TAG, "Rain and snow check failed", e)
        if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
    }

    companion object {
        private const val TAG = "RainAlerts"
        private const val PERIODIC_WORK = "vane_precipitation_alerts"
        private const val INTERVAL_MINUTES = 30L
        private const val MAX_RETRIES = 3

        private val networkConstraint = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        /**
         * Checks every 30 minutes. A newly enqueued periodic job runs its first check straight
         * away, so [checkNow] replaces any existing job (one left over would otherwise wait for its
         * next window); without it, an existing job is kept as it is.
         */
        fun schedule(context: Context, checkNow: Boolean) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK,
                if (checkNow) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE else ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<PrecipitationAlertWorker>(INTERVAL_MINUTES, TimeUnit.MINUTES)
                    .setConstraints(networkConstraint)
                    .build(),
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK)
        }
    }
}
