package ci.devsphere.civmarketplace.util

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ci.devsphere.civmarketplace.data.local.TokenManager
import ci.devsphere.civmarketplace.domain.repository.UserRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class FcmTokenSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val token = inputData.getString(KEY_FCM_TOKEN)?.takeIf(String::isNotBlank)
            ?: return Result.failure()
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            FcmTokenWorkerEntryPoint::class.java
        )

        if (entryPoint.tokenManager().getToken().first().isNullOrBlank()) {
            return Result.success()
        }

        val result = entryPoint.userRepository().updateFcmToken(token)
        return if (result.isSuccess) {
            Result.success()
        } else if (runAttemptCount < MAX_RETRIES) {
            Result.retry()
        } else {
            Result.failure()
        }
    }

    companion object {
        private const val KEY_FCM_TOKEN = "fcm_token"
        private const val WORK_TAG = "fcm-token-sync"
        private const val MAX_RETRIES = 8

        fun enqueue(context: Context, token: String) {
            if (token.isBlank()) return

            val request = OneTimeWorkRequestBuilder<FcmTokenSyncWorker>()
                .setInputData(Data.Builder().putString(KEY_FCM_TOKEN, token).build())
                .addTag(WORK_TAG)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                "fcm-token-sync-${token.hashCode()}",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context.applicationContext)
                .cancelAllWorkByTag("fcm-token-sync")
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface FcmTokenWorkerEntryPoint {
    fun userRepository(): UserRepository
    fun tokenManager(): TokenManager
}
