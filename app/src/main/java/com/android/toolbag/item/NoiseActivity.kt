package com.android.toolbag.item

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.android.toolbag.R
import com.android.toolbag.createFile
import com.android.toolbag.dbCount
import com.android.toolbag.getSmoothedDb
import com.android.toolbag.widget.SoundDiscView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.log10


class NoiseActivity : AppCompatActivity() {

    private var volume: Float = 10000f
    private var soundDiscView: SoundDiscView? = null
    private var mRecorder: MyMediaRecorder? = null
    private var job: Job? = null
    private var textView1: TextView? = null
    private var textView2: TextView? = null
    private var textView3: TextView? = null
    private var textView4: TextView? = null
    private var textView5: TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_noise)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.isNavigationBarContrastEnforced = false
        }
        mRecorder = MyMediaRecorder()
        textView1 = findViewById(R.id.quiteTextView)
        textView2 = findViewById(R.id.indoorTextView)
        textView3 = findViewById(R.id.noisyTextView)
        textView4 = findViewById(R.id.harmfulTextView)
        textView5 = findViewById(R.id.extremelyTextView)
    }

    private fun startListenAudio() {
        job = MainScope().launch(Dispatchers.Main) {
            while (true) {
                volume = mRecorder!!.getMaxAmplitude() //获取声压值
                if (volume > 0 && volume < 1000000) {
                    getSmoothedDb(20 * (log10(volume.toDouble())).toFloat()) //将声压值转为分贝值
                    soundDiscView?.refresh()
                }
                updateView()
                delay(200)
            }
        }
    }

    private fun updateView() {
        if (dbCount >= 0 && dbCount < 45) {
            textView1?.setTextColor(ContextCompat.getColor(this, R.color.sound_value1))
            textView2?.setTextColor(Color.WHITE)
            textView3?.setTextColor(Color.WHITE)
            textView4?.setTextColor(Color.WHITE)
            textView5?.setTextColor(Color.WHITE)
        } else if (dbCount >= 45 && dbCount < 60) {
            textView1?.setTextColor(Color.WHITE)
            textView2?.setTextColor(ContextCompat.getColor(this, R.color.sound_value2))
            textView3?.setTextColor(Color.WHITE)
            textView4?.setTextColor(Color.WHITE)
            textView5?.setTextColor(Color.WHITE)
        } else if (dbCount >= 60 && dbCount < 80) {
            textView1?.setTextColor(Color.WHITE)
            textView2?.setTextColor(Color.WHITE)
            textView3?.setTextColor(ContextCompat.getColor(this, R.color.sound_value3))
            textView4?.setTextColor(Color.WHITE)
            textView5?.setTextColor(Color.WHITE)
        } else if (dbCount >= 80 && dbCount < 115) {
            textView1?.setTextColor(Color.WHITE)
            textView2?.setTextColor(Color.WHITE)
            textView3?.setTextColor(Color.WHITE)
            textView4?.setTextColor(ContextCompat.getColor(this, R.color.sound_value4))
            textView5?.setTextColor(Color.WHITE)
        } else if (dbCount >= 115.0) {
            textView1?.setTextColor(Color.WHITE)
            textView2?.setTextColor(Color.WHITE)
            textView3?.setTextColor(Color.WHITE)
            textView4?.setTextColor(Color.WHITE)
            textView5?.setTextColor(ContextCompat.getColor(this, R.color.sound_value5))
        }
    }


    private fun startRecord(fFile: File?) {
        try {
            mRecorder?.setMyRecAudioFile(fFile)
            if (mRecorder!!.startRecorder()) {
                startListenAudio()
            } else {
                Toast.makeText(
                    applicationContext,
                    getString(R.string.test_fail),
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        } catch (e: Exception) {
            Toast.makeText(applicationContext, getString(R.string.test_fail), Toast.LENGTH_SHORT)
                .show()
            e.printStackTrace()
            finish()
        }
    }


    override fun onResume() {
        super.onResume()
        if (mRecorder == null) finish()
        soundDiscView = findViewById<View>(R.id.soundDiscView) as SoundDiscView
        val file = createFile("temp.amr")
        if (file != null) startRecord(file)
        else {
            finish()
            Toast.makeText(applicationContext, getString(R.string.test_fail), Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun onPause() {
        super.onPause()
        mRecorder!!.delete()
        job?.cancel()
    }

    override fun onDestroy() {
        mRecorder!!.delete()
        super.onDestroy()
    }
}