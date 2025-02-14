package com.android.toolbag

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.hardware.camera2.CameraManager
import android.preference.PreferenceManager

class App : Application() {
    companion object {
        var sp: SharedPreferences? = null
        @SuppressLint("StaticFieldLeak")
        var mContext: Context? = null
        var cameraManager: CameraManager? = null
    }

    override fun onCreate() {
        super.onCreate()
        sp = PreferenceManager.getDefaultSharedPreferences(this)
        mContext = this
    }
}