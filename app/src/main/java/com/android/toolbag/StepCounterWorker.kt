package com.android.toolbag

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StepCounterWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                updateStepsInfo(App.mContext!!)
                Result.success()
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure()
            }
        }
    }
}