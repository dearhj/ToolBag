package com.android.toolbag

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.hardware.camera2.CameraManager
import com.android.toolbag.bean.StepCountDao
import com.android.toolbag.bean.StepCountDatabase
import java.io.File

class App : Application() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        var mContext: Context? = null
        var cameraManager: CameraManager? = null
        private const val P2PRO_PATH = "/sys/devices/platform/gftk_camplight/camplight_mode"
        val isP2Pro = File(P2PRO_PATH).exists()

        var database: StepCountDatabase? = null
        var dao: StepCountDao? = null
    }

    override fun onCreate() {
        super.onCreate()
        mContext = this
        database = StepCountDatabase.getDatabase(this)
        dao = database!!.stepCountDao()
    }
}