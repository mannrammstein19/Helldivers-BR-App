package br.com.helldiversbr.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import br.com.helldiversbr.app.data.OrderRepository
import kotlinx.coroutines.CancellationException

class WarAlertWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        if (!WarAlertManager.isEnabled(applicationContext)) return Result.success()
        return try {
            val data = OrderRepository.load()
            if (data.telemetrySource != "cache" && "campanhas" !in data.staleSources) {
                WarAlertManager.processCampaigns(applicationContext, data.campaigns)
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
