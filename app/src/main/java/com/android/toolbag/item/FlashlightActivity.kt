package com.android.toolbag.item

import android.content.Context
import android.hardware.camera2.CameraManager.TorchCallback
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.android.toolbag.App.Companion.cameraManager
import com.android.toolbag.R
import com.android.toolbag.batteryChangeLevel
import com.android.toolbag.customToast


class FlashlightActivity : AppCompatActivity() {
    private var torchCallback: MyTorchCallback? = null
    private var torchStatus = false
    private var flashLightButton: ImageView? = null
    private var flashLight: ImageView? = null
    private var flashSosLightButton: ImageView? = null
    private var mSoundPool: SoundPool? = null
    private var mVoiceId = 0
    private var lastChangeTime = 0L
    private var sosStatus = "off"

    private var batteryManager: BatteryManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flashlight)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.isNavigationBarContrastEnforced = false
        }
        batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager

        flashLightButton = findViewById(R.id.button_on_off)
        flashLight = findViewById(R.id.light)
        flashSosLightButton = findViewById(R.id.sos_button)
        mSoundPool = null

        sosStatus = Settings.Global.getString(contentResolver, "flashLight_sos") ?: "off"

        torchCallback = MyTorchCallback {
            if (System.currentTimeMillis() - lastChangeTime > 100) {
                lastChangeTime = System.currentTimeMillis()
                if ("off" == sosStatus) { //非SOS情况
                    torchStatus = it
                    if (it) {
                        flashLightButton?.setImageResource(R.drawable.ic_flash_on)
                        flashLight?.visibility = View.VISIBLE
                        Settings.Global.putString(contentResolver, "flashLight_normal", "on")
                    } else {
                        flashLightButton?.setImageResource(R.drawable.ic_flash_off)
                        flashLight?.visibility = View.GONE
                        Settings.Global.putString(contentResolver, "flashLight_normal", "off")
                    }
                }
            }
        }
        cameraManager?.registerTorchCallback(torchCallback!!, Handler(Looper.getMainLooper()))
        flashLightButton?.setOnClickListener {
            val batteryValue =
                batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 0
            if (batteryValue <= 15) {
                customToast(this, getString(R.string.battery_low))
                return@setOnClickListener
            }
            if (sosStatus == "off") {
                if (torchStatus) cameraManager?.setTorchMode("0", false)
                else cameraManager?.setTorchMode("0", true)
            }
            playSoundEffect()
        }
        flashSosLightButton?.setOnClickListener {
            val batteryValue =
                batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 0
            if (batteryValue <= 15) {
                customToast(this, getString(R.string.battery_low))
                return@setOnClickListener
            }
            if (sosStatus == "off") {
                flashSosLightButton?.setImageResource(R.drawable.icon_sos_on)
                flashLightButton?.setImageResource(R.drawable.ic_flash_off)
                flashLight?.visibility = View.VISIBLE
                Settings.Global.putString(contentResolver, "flashLight_sos", "on")
                sosStatus = "on"
            } else {
                flashSosLightButton?.setImageResource(R.drawable.icon_sos_off)
                flashLight?.visibility = View.GONE
                Settings.Global.putString(contentResolver, "flashLight_sos", "off")
                sosStatus = "off"
            }
            playSoundEffect()
        }

        batteryChangeLevel {
            if (it <= 15) {
                if (sosStatus != "off" || torchStatus) {
                    Settings.Global.putString(contentResolver, "flashLight_sos", "off")
                    Settings.Global.putString(contentResolver, "flashLight_normal", "off")
                    try {
                        cameraManager?.setTorchMode("0", false)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    flashLight?.visibility = View.GONE
                    flashLightButton?.setImageResource(R.drawable.ic_flash_off)
                    flashSosLightButton?.setImageResource(R.drawable.icon_sos_off)
                    sosStatus = "off"
                    torchStatus = false
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val batteryValue =
            batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 0
        if (batteryValue <= 15) {
            Settings.Global.putString(contentResolver, "flashLight_sos", "off")
            Settings.Global.putString(contentResolver, "flashLight_normal", "off")
            cameraManager?.setTorchMode("0", false)
            flashLight?.visibility = View.GONE
            flashLightButton?.setImageResource(R.drawable.ic_flash_off)
            flashSosLightButton?.setImageResource(R.drawable.icon_sos_off)
            torchStatus = false
        }
        sosStatus = Settings.Global.getString(contentResolver, "flashLight_sos") ?: "off"
        if (sosStatus == "off") {
            flashSosLightButton?.setImageResource(R.drawable.icon_sos_off)
            if ((Settings.Global.getString(contentResolver, "flashLight_normal") ?: "off") == "off")
                flashLight?.visibility = View.GONE
            else flashLight?.visibility = View.VISIBLE
        } else {
            flashSosLightButton?.setImageResource(R.drawable.icon_sos_on)
            flashLight?.visibility = View.VISIBLE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            cameraManager?.unregisterTorchCallback(torchCallback!!)
            torchCallback = null
            if (mSoundPool != null) mSoundPool?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playSoundEffect() {
        if (mSoundPool == null) {
            val builder = SoundPool.Builder()
            builder.setMaxStreams(1)
            val builder2 = AudioAttributes.Builder()
            builder2.setLegacyStreamType(3)
            builder.setAudioAttributes(builder2.build())
            val build = builder.build()
            mSoundPool = build
            mVoiceId = build.load(this, R.raw.sound, 1)
            mSoundPool?.setOnLoadCompleteListener { _, _, i2 ->
                if (i2 == 0) {
                    mSoundPool?.play(mVoiceId, 1.0f, 1.0f, 1, 0, 1.0f)
                }
            }
            return
        }
        mSoundPool?.play(this.mVoiceId, 1.0f, 1.0f, 1, 0, 1.0f)
    }


    class MyTorchCallback(private val status: (Boolean) -> Unit) : TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            status(enabled)
        }
    }
}