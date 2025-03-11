package com.android.toolbag

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat.startForegroundService
import com.android.toolbag.bean.TurnOnTime


class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        //应用首次启动也会执行这里，奇怪了。
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            if (SystemClock.elapsedRealtime() < 1000 * 30) { //设备启动时间大于30s，在执行此处则不认为是设备开机完成
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    App.dao?.insertOrUpdateToTurnOnTime(TurnOnTime("TurnOn", getTodayInfoAsInt()))
            }
            startForegroundService(context, Intent(context, LightControlService::class.java))
        }
    }
}