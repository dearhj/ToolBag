package com.android.toolbag.item

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.android.toolbag.App.Companion.cameraManager
import com.android.toolbag.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class AlarmActivity : AppCompatActivity() {
    private var halfTop: FrameLayout? = null
    private var halfBottom: FrameLayout? = null
    private var halfTopLand: FrameLayout? = null
    private var halfBottomLand: FrameLayout? = null
    private var flashLightButton: ImageView? = null
    private var alarmBellButton: ImageView? = null
    private var alarmLampButton: ImageView? = null

    private var mediaPlayer: MediaPlayer? = null

    private var lightJob: Job? = null
    private var alarmLightJob: Job? = null

    private var flashLightFlag = false
    private var alarmLightFlag = false


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.isNavigationBarContrastEnforced = false
        }
        halfTop = findViewById(R.id.top_half)
        halfTopLand = findViewById(R.id.top_half_land)
        halfBottom = findViewById(R.id.bottom_half)
        halfBottomLand = findViewById(R.id.bottom_half_land)
        flashLightButton = findViewById(R.id.flashImage)
        alarmBellButton = findViewById(R.id.soundImage)
        alarmLampButton = findViewById(R.id.lightImage)

        if (getResources().configuration.orientation != Configuration.ORIENTATION_PORTRAIT) {
            halfTop?.visibility = View.GONE
            halfTopLand?.visibility = View.VISIBLE
            halfBottom?.visibility = View.GONE
            halfBottomLand?.visibility = View.VISIBLE
        } else {
            halfTop?.visibility = View.VISIBLE
            halfTopLand?.visibility = View.GONE
            halfBottom?.visibility = View.VISIBLE
            halfBottomLand?.visibility = View.GONE
        }

        flashLightButton?.setOnClickListener {
            if (!flashLightFlag) {
                if ((Settings.Global.getString(contentResolver, "flashLight_sos")
                        ?: "off") != "off"
                ) {
                    Settings.Global.putString(contentResolver, "flashLight_sos", "off")
                }
                if ((Settings.Global.getString(contentResolver, "flashLight_normal")
                        ?: "off") != "off"
                ) {
                    Settings.Global.putString(contentResolver, "flashLight_normal", "off")
                }
                flashLightButton?.setImageResource(R.drawable.btn_flashlight_two)
                flashLightFlag = true
                flashLightThread(true)
            } else {
                flashLightButton?.setImageResource(R.drawable.btn_flashlight_one)
                flashLightFlag = false
                flashLightThread(false)
            }
        }

        alarmBellButton?.setOnClickListener { playOrStopRing() }

        alarmLampButton?.setOnClickListener {
            if (!alarmLightFlag) {
                alarmLightFlag = true
                playOrStopAlarmLight(true)
                alarmLampButton?.setImageResource(R.drawable.btn_screenflash_two)
            } else {
                alarmLightFlag = false
                playOrStopAlarmLight(false)
                alarmLampButton?.setImageResource(R.drawable.btn_screenflash_one)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        flashLightFlag = false
        alarmLightFlag = false
        flashLightButton?.setImageResource(R.drawable.btn_flashlight_one)
        alarmLampButton?.setImageResource(R.drawable.btn_screenflash_one)
        alarmBellButton?.setImageResource(R.drawable.btn_alarmring_one)
        halfTop?.setBackgroundColor(ContextCompat.getColor(this, R.color.flash_blue))
        halfTopLand?.setBackgroundColor(ContextCompat.getColor(this, R.color.flash_blue))
        halfBottom?.setBackgroundColor(ContextCompat.getColor(this, R.color.flash_blue))
        halfBottomLand?.setBackgroundColor(ContextCompat.getColor(this, R.color.flash_blue))
    }

    override fun onStop() {
        super.onStop()
        cameraManager?.setTorchMode("0", false)
        lightJob?.cancel()

        if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
            mediaPlayer!!.stop()
            mediaPlayer!!.release()
            mediaPlayer = null
        }
        alarmLightJob?.cancel()
    }

    private fun playOrStopAlarmLight(alarmLightUp: Boolean) {
        if (alarmLightUp) {
            alarmLightJob = MainScope().launch(Dispatchers.Main) {
                while (true) {
                    for (i in 1..4) {
                        halfTop?.setBackgroundColor(Color.RED)
                        halfTopLand?.setBackgroundColor(Color.RED)
                        halfBottom?.setBackgroundColor(Color.BLUE)
                        halfBottomLand?.setBackgroundColor(Color.BLUE)
                        delay(100)
                        halfTop?.setBackgroundColor(Color.BLACK)
                        halfTopLand?.setBackgroundColor(Color.BLACK)
                        halfBottom?.setBackgroundColor(Color.BLACK)
                        halfBottomLand?.setBackgroundColor(Color.BLACK)
                        delay(100)
                    }
                    for (i in 1..4) {
                        halfTop?.setBackgroundColor(Color.RED)
                        halfTopLand?.setBackgroundColor(Color.RED)
                        halfBottom?.setBackgroundColor(Color.BLACK)
                        halfBottomLand?.setBackgroundColor(Color.BLACK)
                        delay(50)
                        halfTop?.setBackgroundColor(Color.BLACK)
                        halfTopLand?.setBackgroundColor(Color.BLACK)
                        delay(50)
                    }
                    for (i in 1..4) {
                        halfTop?.setBackgroundColor(Color.BLACK)
                        halfTopLand?.setBackgroundColor(Color.BLACK)
                        halfBottom?.setBackgroundColor(Color.BLUE)
                        halfBottomLand?.setBackgroundColor(Color.BLUE)
                        delay(50)
                        halfBottom?.setBackgroundColor(Color.BLACK)
                        halfBottomLand?.setBackgroundColor(Color.BLACK)
                        delay(50)
                    }
                    for (i in 1..4) {
                        halfTop?.setBackgroundColor(Color.RED)
                        halfTopLand?.setBackgroundColor(Color.RED)
                        halfBottom?.setBackgroundColor(Color.BLACK)
                        halfBottomLand?.setBackgroundColor(Color.BLACK)
                        delay(100)
                        halfTop?.setBackgroundColor(Color.BLACK)
                        halfTopLand?.setBackgroundColor(Color.BLACK)
                        halfBottom?.setBackgroundColor(Color.BLUE)
                        halfBottomLand?.setBackgroundColor(Color.BLUE)
                        delay(100)
                    }
                    halfTop?.setBackgroundColor(Color.RED)
                    halfTopLand?.setBackgroundColor(Color.RED)
                    halfBottom?.setBackgroundColor(Color.BLUE)
                    halfBottomLand?.setBackgroundColor(Color.BLUE)
                }
            }
        } else {
            alarmLightJob?.cancel()
            halfTop?.setBackgroundColor(ContextCompat.getColor(this, R.color.flash_blue))
            halfTopLand?.setBackgroundColor(ContextCompat.getColor(this, R.color.flash_blue))
            halfBottom?.setBackgroundColor(ContextCompat.getColor(this, R.color.flash_blue))
            halfBottomLand?.setBackgroundColor(ContextCompat.getColor(this, R.color.flash_blue))
        }
    }

    private fun playOrStopRing() {
        if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
            mediaPlayer!!.stop()
            mediaPlayer!!.release()
            mediaPlayer = null
            alarmBellButton?.setImageResource(R.drawable.btn_alarmring_one)
            return
        }
        val create = MediaPlayer.create(this, R.raw.warning_ring)
        mediaPlayer = create
        create.isLooping = true
        mediaPlayer?.start()
        alarmBellButton?.setImageResource(R.drawable.btn_alarmring_two)
    }


    private fun flashLightThread(lightUp: Boolean) {
        if (lightUp) {
            lightJob = MainScope().launch(Dispatchers.IO) {
                while (true) {
                    cameraManager?.setTorchMode("0", true)
                    delay(100)
                    cameraManager?.setTorchMode("0", false)
                    delay(100)
                }
            }
        } else {
            lightJob?.cancel()
            cameraManager?.setTorchMode("0", false)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            halfTop?.visibility = View.GONE
            halfTopLand?.visibility = View.VISIBLE
            halfBottom?.visibility = View.GONE
            halfBottomLand?.visibility = View.VISIBLE
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            halfTop?.visibility = View.VISIBLE
            halfTopLand?.visibility = View.GONE
            halfBottom?.visibility = View.VISIBLE
            halfBottomLand?.visibility = View.GONE
        }
    }
}