package com.android.toolbag

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.hardware.camera2.CameraManager
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.android.toolbag.App.Companion.cameraManager
import com.android.toolbag.App.Companion.mContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class LightControlService : Service() {

    private var loopJob: Job? = null

    @SuppressLint("NewApi", "ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val id = "gh0st_id"
        val mChannel = NotificationChannel(id, id, NotificationManager.IMPORTANCE_NONE)
        notificationManager.createNotificationChannel(mChannel)
        val notification = Notification.Builder(this, id)
            .setContentTitle(getString(R.string.services_tip))
            .setSmallIcon(R.drawable.ic_launcher).setColor(Color.argb(0, 0, 0, 0))
            .setCategory(NotificationCompat.CATEGORY_SERVICE).build()
        startForeground(1, notification)

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
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
    }

    override fun onBind(intent: Intent): IBinder? = null
}