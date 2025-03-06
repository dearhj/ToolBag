package com.android.toolbag

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit


class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        println("这里收到了开机广播1111")
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            // 处理开机自启动任务，获取当前步数数据并保存
            val workRequest =
                PeriodicWorkRequestBuilder<StepCounterWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(App.mContext!!).enqueueUniquePeriodicWork(
                "StepCounterWorkerTag",
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }
    }
}