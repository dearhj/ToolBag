package com.android.toolbag

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.hardware.camera2.CameraManager
import android.preference.PreferenceManager
import java.io.File

class App : Application() {
    companion object {
        var sp: SharedPreferences? = null
        @SuppressLint("StaticFieldLeak")
        var mContext: Context? = null
        var cameraManager: CameraManager? = null
        private const val P2PRO_PATH = "/sys/devices/platform/gftk_camplight/camplight_mode"
        val isP2Pro = File(P2PRO_PATH).exists()
    }

    override fun onCreate() {
        super.onCreate()
        sp = PreferenceManager.getDefaultSharedPreferences(this)
        mContext = this
    }
}