package com.android.toolbag.item

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.android.toolbag.App.Companion.isP2Pro
import com.android.toolbag.R
import com.android.toolbag.widget.CompassView
import java.util.Locale


class CompassActivity : AppCompatActivity(), SensorEventListener {
    private var mAccelerometerSensor: Sensor? = null
    private var isChinese = false
    private var mMagneticSensor: Sensor? = null
    private var rotationVectorSensor: Sensor? = null
    private var mPointer: CompassView? = null
    private var mSensorManager: SensorManager? = null
    private val mGravity = FloatArray(3)
    private val mGeomagnetic = FloatArray(3)
    private var azimuth = 0f
    private var textTips: TextView? = null
    private var compassView: LinearLayout? = null
    private var calibrationView: RelativeLayout? = null
    private var guideImageView: ImageView? = null
    private var animationDrawable: AnimationDrawable? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compass)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.isNavigationBarContrastEnforced = false
        }
    }

    override fun onResume() {
        super.onResume()
        initResources()
        initServices()
        refreshLayout(false)
    }

    override fun onPause() {
        super.onPause()
        mSensorManager?.unregisterListener(this)
        vibrator?.cancel()
    }

    private fun initResources() {
        isChinese = TextUtils.equals(Locale.getDefault().language, "zh")
        mPointer = findViewById(R.id.compass_pointer)
        textTips = findViewById(R.id.textTips)
        compassView = findViewById(R.id.view_compass)
        calibrationView = findViewById(R.id.help_layout)
        guideImageView = findViewById(R.id.guide_animation)
        animationDrawable = guideImageView?.drawable as AnimationDrawable
        mPointer?.setImageResource(if (isChinese) R.drawable.compass_cn else R.drawable.compass)
    }

    private fun initServices() {
        mSensorManager = getSystemService("sensor") as SensorManager
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (isP2Pro) {  //旋转矢量传感器
            rotationVectorSensor = mSensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            mSensorManager?.registerListener(
                this,
                rotationVectorSensor,
                SensorManager.SENSOR_DELAY_FASTEST
            )
        } else {
            mMagneticSensor = mSensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) //磁场传感器
            mAccelerometerSensor =
                mSensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) //加速度传感器
            mSensorManager?.registerListener(
                this,
                mAccelerometerSensor,
                SensorManager.SENSOR_DELAY_FASTEST
            )
            mSensorManager?.registerListener(
                this,
                mMagneticSensor,
                SensorManager.SENSOR_DELAY_FASTEST
            )
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val alpha = 0.98f //平滑因子

        synchronized(this) {
            if (!isP2Pro) {
                //指南针转动角度算法
                //判断当前是加速度感应器还是地磁感应器
                if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
                    mGravity[0] = (alpha * mGravity[0] + (1 - alpha) * event.values[0])
                    mGravity[1] = (alpha * mGravity[1] + (1 - alpha) * event.values[1])
                    mGravity[2] = (alpha * mGravity[2] + (1 - alpha) * event.values[2])
                }

                if (event?.sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    mGeomagnetic[0] = (alpha * mGeomagnetic[0] + (1 - alpha) * event.values[0])
                    mGeomagnetic[1] = (alpha * mGeomagnetic[1] + (1 - alpha) * event.values[1])
                    mGeomagnetic[2] = (alpha * mGeomagnetic[2] + (1 - alpha) * event.values[2])
                }

                val rArray = FloatArray(9)
                val iArray = FloatArray(9)
                val success =
                    SensorManager.getRotationMatrix(rArray, iArray, mGravity, mGeomagnetic)
                if (success) {
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rArray, orientation)
                    azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    azimuth = (azimuth + 360) % 360
                    adjustArrow()
                }
            } else {
                if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat() // 方位角
                    azimuth = (azimuth + 360) % 360
                    adjustArrow()
                }
            }
        }
    }


    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        if (!isP2Pro) {
            if (p0?.type == Sensor.TYPE_MAGNETIC_FIELD) {
                if (p1 == SensorManager.SENSOR_STATUS_ACCURACY_HIGH || p1 == SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM) {
                    if (calibrationView?.visibility == View.VISIBLE) {
                        Toast.makeText(
                            this,
                            getString(R.string.calibration_success),
                            Toast.LENGTH_SHORT
                        ).show()
                        //震动提示
                        if (vibrator != null && vibrator!!.hasVibrator()) {
                            vibrator!!.vibrate(500L)
                        }
                    }
                    refreshLayout(false)
                    println("指南针无须校准")
                } else {
                    refreshLayout(true)
                    println("指南针需要校准")
                }
            }
        }
    }

    private fun refreshLayout(needCalibration: Boolean) {
        if (needCalibration) {
            animationDrawable?.start()
            calibrationView?.visibility = View.VISIBLE
            compassView?.visibility = View.GONE
        } else {
            animationDrawable?.stop()
            calibrationView?.visibility = View.GONE
            compassView?.visibility = View.VISIBLE
        }
    }


    /**
     * 陀螺仪方位显示&角度转动
     */
    @SuppressLint("SetTextI18n")
    private fun adjustArrow() {
        println("当前指南针角度是  $azimuth")
        if (azimuth <= 22.5 || azimuth >= 337.5) {
            textTips?.text = getString(R.string.north) + " " + azimuth.toInt() + "°"
        } else if (22.5 < azimuth && azimuth < 67.5) {
            textTips?.text = getString(R.string.east_north) + " " + azimuth.toInt() + "°"
        } else if (azimuth in 67.5..112.5) {
            textTips?.text = getString(R.string.east) + " " + azimuth.toInt() + "°"
        } else if (112.5 < azimuth && azimuth < 157.5) {
            textTips?.text = getString(R.string.east_south) + " " + azimuth.toInt() + "°"
        } else if (azimuth in 157.5..202.5) {
            textTips?.text = getString(R.string.south) + " " + azimuth.toInt() + "°"
        } else if (202.5 < azimuth && azimuth < 247.5) {
            textTips?.text = getString(R.string.west_south) + " " + azimuth.toInt() + "°"
        } else if (azimuth in 247.5..292.5) {
            textTips?.text = getString(R.string.west) + " " + azimuth.toInt() + "°"
        } else if (292.5 < azimuth && azimuth < 337.5) {
            textTips?.text = getString(R.string.west_north) + " " + azimuth.toInt() + "°"
        }
        mPointer?.updateDirection(360 - azimuth)
    }

}