package com.android.toolbag.item

import android.annotation.SuppressLint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.android.toolbag.R
import com.android.toolbag.widget.SpiritView
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.sqrt

class GradientActivity : AppCompatActivity(), SensorEventListener {
    private var mAccelerometerSensor: Sensor? = null
    private var mSensorManager: SensorManager? = null
    private var mSpiritView: SpiritView? = null
    private var verTextView: TextView? = null
    private var horTextView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gradient)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.isNavigationBarContrastEnforced = false
        }
    }

    override fun onResume() {
        super.onResume()
        verTextView = findViewById(R.id.vertical_value)
        horTextView = findViewById(R.id.horizontal_value)
        mSpiritView = findViewById(R.id.show)
        initServices()
    }

    override fun onPause() {
        super.onPause()
        mSensorManager?.unregisterListener(this)
    }

    private fun initServices() {
        mSensorManager = getSystemService("sensor") as SensorManager
        mAccelerometerSensor = mSensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) //加速度传感器
        mSensorManager?.registerListener(
            this,
            mAccelerometerSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0] // X轴加速度
            val y = event.values[1] // Y轴加速度
            val z = event.values[2] // Z轴加速度

            // 计算倾斜角度
            var pitch = atan2(-x.toDouble(), sqrt((y * y + z * z).toDouble())).toFloat() // 水平方向角度
            var roll = atan2(y.toDouble(), z.toDouble()).toFloat() // 垂直方向角度

            // 将角度转换为度
            pitch = Math.toDegrees(pitch.toDouble()).toFloat() //屏幕左右翻转角度，左负右正
            roll = Math.toDegrees(roll.toDouble()).toFloat() //屏幕前后翻转角度，前负后正

            updateUI(pitch, roll)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateUI(pitch: Float, roll: Float) {
        val ver = truncateToInteger(roll)
        val hor = truncateToInteger(pitch)
        verTextView?.text = "$ver °"
        horTextView?.text = "$hor °"
        val positionX = if (pitch > 90) 90f else if (pitch < -90) -90f else pitch
        val positionY = if (roll > 90) 90f else if (roll < -90) -90f else roll
        val bubbleX: Float
        val bubbleY: Float
        val screenWidth = mSpiritView!!.screenWidth
        val screenHeight = mSpiritView!!.screenHeight
        bubbleX = if (positionX < 0f) ((screenWidth / 2) / 90) * abs(positionX) + (screenWidth / 2)
        else if (positionX > 0f) ((screenWidth / 2) / 90) * (90 - positionX)
        else screenWidth / 2
        bubbleY =
            if (positionY < 0f) ((screenHeight / 2) / 90) * abs(positionY) + (screenHeight / 2)
            else if (positionY > 0f) ((screenHeight / 2) / 90) * (90 - positionY)
            else screenHeight / 2
        mSpiritView?.updateSpiritViewUI(bubbleX, bubbleY)
    }


    private fun truncateToInteger(value: Float): Int { //保留整数功能
        return if (value >= 0) value.toInt() // 对于正数，直接转换为 Int 即可
        else ceil(value.toDouble()).toInt() // 对于负数，ceil向上取整
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        //加速度传感器无须校准
    }
}
