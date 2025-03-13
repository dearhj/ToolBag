package com.android.toolbag

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.android.toolbag.App.Companion.cameraManager
import com.android.toolbag.App.Companion.mContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@RequiresApi(Build.VERSION_CODES.O)
class LightControlService : Service() {
    private var loopJob: Job? = null
    private var mSensorManager: SensorManager? = null
    private var batteryManager: BatteryManager? = null

    @SuppressLint("NewApi", "ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val id = "toolbox_id"
        val mChannel = NotificationChannel(id, id, NotificationManager.IMPORTANCE_NONE)
        notificationManager.createNotificationChannel(mChannel)
        val notification = Notification.Builder(this, id)
            .setContentTitle(getString(R.string.services_tip))
            .setSmallIcon(R.drawable.ic_launcher).setColor(Color.argb(0, 0, 0, 0))
            .setCategory(NotificationCompat.CATEGORY_SERVICE).build()
        startForeground(1, notification)

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        observerGlobal(mContext!!, "flashLight_sos") {
            val status = Settings.Global.getString(contentResolver, "flashLight_sos") ?: "off"
            if (status == "on") {
                loopJob = MainScope().launch(Dispatchers.IO) {
                    try {
                        while (true) {
                            for (i in 1..3) {
                                cameraManager?.setTorchMode("0", true)
                                delay(500) // 短亮
                                cameraManager?.setTorchMode("0", false)
                                delay(500) // 间隔
                            }
                            delay(300)
                            // 三长
                            for (i in 1..3) {
                                cameraManager?.setTorchMode("0", true)
                                delay(1300) // 长亮
                                cameraManager?.setTorchMode("0", false)
                                delay(500) // 间隔
                            }
                            delay(300)
                            // 三短
                            for (i in 1..3) {
                                cameraManager?.setTorchMode("0", true)
                                delay(500) // 短亮
                                cameraManager?.setTorchMode("0", false)
                                delay(500) // 间隔
                            }
                            delay(800)
                        }
                    } catch (e: Exception) {
                        //相机打开，手电筒被迫关闭
                        Settings.Global.putString(contentResolver, "flashLight_sos", "off")
                        e.printStackTrace()
                    }
                }
            } else {
                loopJob?.cancel()
                cameraManager?.setTorchMode("0", false)
            }
        }

        MainScope().launch(Dispatchers.IO) {
            while (true) {
                try {
                    val batteryValue =
                        batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                            ?: 0
                    if (batteryValue <= 15) { //关闭闪光灯
                        batteryChange = batteryValue
                        cameraManager?.setTorchMode("0", false)
                    }
                } catch (_: Exception){
                }
                delay(30000)
            }
        }


        val filter = IntentFilter()
        //监听日期变化
        filter.addAction(Intent.ACTION_DATE_CHANGED)
        filter.addAction(Intent.ACTION_TIME_CHANGED)
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED)
        registerReceiver(mBatInfoReceiver, filter)

        // 启动十五分钟定时器，用户获取当前步数数据并保存
        val workRequest =
            PeriodicWorkRequestBuilder<StepCounterWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(mContext!!).enqueueUniquePeriodicWork(
            "StepCounterWorkerTag",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )

        mSensorManager = getSystemService("sensor") as SensorManager
        val stepCounterSensor = mSensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        mSensorManager?.registerListener(
            listener,
            stepCounterSensor,
            SensorManager.SENSOR_DELAY_UI
        )
    }

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
                updateStepsInfo(this@LightControlService)
            }
        }

        override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        }
    }

    private val mBatInfoReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onReceive(context: Context?, intent: Intent?) {
            if (Intent.ACTION_DATE_CHANGED == intent!!.action) updateStepsInfo(context!!)
            else if (Intent.ACTION_TIME_CHANGED == intent.action) updateStepsInfo(context!!)
            else if (Intent.ACTION_TIMEZONE_CHANGED == intent.action) updateStepsInfo(context!!)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mBatInfoReceiver)
        mSensorManager?.unregisterListener(listener)
    }

    override fun onBind(intent: Intent): IBinder? = null
}