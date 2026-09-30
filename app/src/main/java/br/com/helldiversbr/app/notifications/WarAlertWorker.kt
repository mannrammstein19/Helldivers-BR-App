package br.com.helldiversbr.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
<<<<<<< HEAD
import br.com.helldiversbr.app.data.OrderRepository
=======
import br.com.helldiversbr.app.data.HelldiversApi
>>>>>>> 4126736d414f57bf192f28a9f89522910ca923d0
import kotlinx.coroutines.CancellationException

class WarAlertWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        if (!WarAlertManager.isEnabled(applicationContext)) return Result.success()
        return try {
<<<<<<< HEAD
            val data = OrderRepository.load()
            if (data.telemetrySource != "cache" && "campanhas" !in data.staleSources) {
                WarAlertManager.processCampaigns(applicationContext, data.campaigns)
                Result.success()
            } else {
                Result.retry()
            }
=======
            val campaigns = HelldiversApi.campaigns()
            WarAlertManager.processCampaigns(applicationContext, campaigns)
            Result.success()
>>>>>>> 4126736d414f57bf192f28a9f89522910ca923d0
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
