package br.com.helldiversbr.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import br.com.helldiversbr.app.data.HelldiversApi
import kotlinx.coroutines.CancellationException

class WarAlertWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        if (!WarAlertManager.isEnabled(applicationContext)) return Result.success()
        return try {
            val campaigns = HelldiversApi.campaigns()
            WarAlertManager.processCampaigns(applicationContext, campaigns)
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
