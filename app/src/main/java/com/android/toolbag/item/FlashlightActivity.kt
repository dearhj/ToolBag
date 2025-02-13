package com.android.toolbag.item

import android.content.Context
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraManager.TorchCallback
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.android.toolbag.R


class FlashlightActivity : AppCompatActivity() {
    private var cameraManager: CameraManager? = null
    private var torchCallback: MyTorchCallback? = null
    private var torchStatus = false
    private var flashLightButton: ImageView? = null
    private var flashLight: ImageView? = null
    private var mSoundPool: SoundPool? = null
    private var mVoiceId = 0
    private var lastChangeTime = 0L


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flashlight)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.isNavigationBarContrastEnforced = false
        }
        flashLightButton = findViewById(R.id.button_on_off)
        flashLight = findViewById(R.id.light)
        mSoundPool = null

        torchCallback = MyTorchCallback {
            if (System.currentTimeMillis() - lastChangeTime > 100) {
                lastChangeTime = System.currentTimeMillis()
                torchStatus = it
                if (it) {
                    flashLightButton?.setImageResource(R.drawable.ic_flash_on)
                    flashLight?.visibility = View.VISIBLE
                } else {
                    flashLightButton?.setImageResource(R.drawable.ic_flash_off)
                    flashLight?.visibility = View.GONE
                }
            }
        }
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraManager?.registerTorchCallback(torchCallback!!, Handler(Looper.getMainLooper()))
        flashLightButton?.setOnClickListener {
            if (torchStatus) cameraManager?.setTorchMode("0", false)
            else cameraManager?.setTorchMode("0", true)
            playSoundEffect()
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