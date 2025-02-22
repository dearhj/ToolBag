package com.android.toolbag.item

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.android.toolbag.R
import com.android.toolbag.customToast
import com.android.toolbag.widget.CustomNumberPicker
import kotlin.math.abs
import kotlin.math.tan


class HeightMeasureActivity : AppCompatActivity(), SensorEventListener {
    private var heightMeasureResult: LinearLayout? = null
    private var heightMeasureScrollview: ScrollView? = null
    private var targetImage: ImageView? = null
    private var heightMeasureTarget: LinearLayout? = null
    private var commonLayout: LinearLayout? = null
    private var setUserHeight: RelativeLayout? = null
    private var firstNumberPicker: CustomNumberPicker? = null
    private var secondNumberPicker: CustomNumberPicker? = null
    private var lastNumberPicker: CustomNumberPicker? = null
    private var startButton: Button? = null

    private var yourHeightTextView: TextView? = null
    private var highPointAngleTextView: TextView? = null
    private var lowPointAngleTextView: TextView? = null
    private var targetDistanceTextView: TextView? = null
    private var targetHeightTextView: TextView? = null


    private var uiStatus = 0
    private var isAutoRotateEnabled = false
    private var isPORTRAIT = false

    private var vibrator: Vibrator? = null

    private var measureResult = 0 //0表示还未开始测量 1表示俯角测量成功 2表示仰角测量成功

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null
    private var lastAccelerometerValues: FloatArray? = null
    private var lastMagnetometerValues: FloatArray? = null

    private var nowRoll = 0f  //传感器横滚角实时值 单位弧度
    private var nowPitch = 0f  //传感器俯仰角实时值 单位弧度
    private var proneAngle = 0f   //俯角 单位度，用于显示
    private var elevationAngle = 0f //仰角 单位度，用于显示
    private var proneAngleCalculate = 0f //俯角 单位弧度，用于计算
    private var elevationAngleCalculate = 0f //仰角 单位弧度，用于计算
    private var yourHeight = 0f //身高
    private var targetDistance = "0" //目标距离
    private var targetHeight = "0"  //目标高度

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_height_measure)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.isNavigationBarContrastEnforced = false
        }
        uiStatus = 0 //初始页面
        measureResult = 0

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        heightMeasureScrollview = findViewById(R.id.height_measure_scrollview)
        targetImage = findViewById(R.id.target_image_view)
        setUserHeight = findViewById(R.id.set_user_height)
        heightMeasureTarget = findViewById(R.id.height_measure_target)
        heightMeasureResult = findViewById(R.id.height_measure_result)
        firstNumberPicker = findViewById(R.id.first_number)
        secondNumberPicker = findViewById(R.id.second_number)
        lastNumberPicker = findViewById(R.id.last_number)
        commonLayout = findViewById(R.id.common_bottom)
        startButton = findViewById(R.id.start)

        yourHeightTextView = findViewById(R.id.height)
        highPointAngleTextView = findViewById(R.id.angle_of_elevation)
        lowPointAngleTextView = findViewById(R.id.angle_of_depression)
        targetDistanceTextView = findViewById(R.id.target_distance)
        targetHeightTextView = findViewById(R.id.target_height)

        firstNumberPicker?.minValue = 0
        firstNumberPicker?.maxValue = 2
        firstNumberPicker?.value = 1
        secondNumberPicker?.minValue = 0
        secondNumberPicker?.maxValue = 9
        secondNumberPicker?.value = 7
        lastNumberPicker?.minValue = 0
        lastNumberPicker?.maxValue = 9
        lastNumberPicker?.value = 5

        startButton?.setOnClickListener {
            if (uiStatus == 1) yourHeight =
                (firstNumberPicker!!.value * 100 + secondNumberPicker!!.value * 10 + lastNumberPicker!!.value).toFloat()
            if (uiStatus == 3) uiStatus = 1 //在显示结果页面点击按钮，跳到设置升高页面，重新测量
            else uiStatus++ //在其他页面点击按钮，跳到下一步
            updateUI(uiStatus)
        }
        targetImage?.setOnClickListener {
            if (measureResult == 0) {
                //俯角测量
                val measure = startProneAngleMeasurement()
                //操作成功
                if (measure == 0) {
                    measureResult++
                    if (vibrator != null && vibrator!!.hasVibrator()) vibrator!!.vibrate(300L)
                    targetImage?.setImageResource(R.drawable.height_measure_last_step_view)
                } else {
                    val info =
                        if (measure == 1) getString(R.string.fail_1) else getString(R.string.fail_2)
                    customToast(this, info)
                    if (vibrator != null && vibrator!!.hasVibrator()) {
                        val vibrationPattern = longArrayOf(0, 200, 200, 200) // 震动模式
                        val vibrationEffect = VibrationEffect.createWaveform(vibrationPattern, -1)
                        vibrator!!.vibrate(vibrationEffect)
                    }
                }
            } else if (measureResult == 1) {
                //仰角测量
                val measure = startElevationAngleMeasurement()
                //操作成功
                if (measure == 0) {
                    measureResult++
                    if (vibrator != null && vibrator!!.hasVibrator()) vibrator!!.vibrate(300L)
                    calculateHeight()
                    uiStatus++
                    updateUI(uiStatus)
                } else {
                    val info =
                        if (measure == 1) getString(R.string.fail_1) else getString(R.string.fail_3)
                    customToast(this, info)
                    if (vibrator != null && vibrator!!.hasVibrator()) {
                        val vibrationPattern = longArrayOf(0, 200, 200, 200) // 震动模式
                        val vibrationEffect = VibrationEffect.createWaveform(vibrationPattern, -1)
                        vibrator!!.vibrate(vibrationEffect)
                    }
                }
            }
        }

        //监听返回键。
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (uiStatus == 0) finish()
                else {
                    uiStatus--
                    updateUI(uiStatus, true)
                }
            }
        })
    }

    @SuppressLint("DefaultLocale")
    private fun calculateHeight() {
        val distanceToTarget = yourHeight * cot(abs(proneAngleCalculate))
        targetDistance = String.format("%.2f", distanceToTarget / 100.0)
        if (elevationAngleCalculate > 0) { //仰角大于0
            val targetHeightHalf = distanceToTarget * tan(elevationAngleCalculate)
            targetHeight = String.format("%.2f", (yourHeight + targetHeightHalf) / 100.0)
        } else {
            val targetHeightHalf = distanceToTarget * tan(abs(elevationAngleCalculate))
            targetHeight = String.format("%.2f", (yourHeight - targetHeightHalf) / 100.0)
        }
    }

    private fun cot(x: Float) = 1 / tan(x)

    override fun onResume() {
        super.onResume()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)  //重力传感器
        magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) //地磁传感器
        if (uiStatus == 2) startRegisterSensor()
    }

    override fun onPause() {
        super.onPause()
        vibrator?.cancel()
        stopRegisterSensor()
    }

    private fun startRegisterSensor() {
        accelerometer?.let {
            sensorManager?.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
        magnetometer?.let {
            sensorManager?.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    private fun stopRegisterSensor() {
        sensorManager?.unregisterListener(this)
    }

    //0代表俯角测量成功，1代表设备未垂直于水平面或误差过大，2代表设备拿反了
    private fun startProneAngleMeasurement(): Int {
        val currentRoll = Math.toDegrees(nowRoll.toDouble()) //弧度转角度
        val currentPitch = Math.toDegrees(nowPitch.toDouble()) //弧度转角度
        if (abs(currentRoll) > 80 && abs(currentRoll) < 100) {
            if (currentPitch > 0) return 2
            proneAngle = currentPitch.toFloat()
            proneAngleCalculate = nowPitch
            return 0
        } else return 1
    }

    //0代表仰角测量成功，1代表设备未垂直于水平面或误差过大，2代表设备拿反了， 3代表仰角过小
    private fun startElevationAngleMeasurement(): Int {
        val currentRoll = Math.toDegrees(nowRoll.toDouble()) //弧度转角度
        val currentPitch = Math.toDegrees(nowPitch.toDouble()) //弧度转角度
        if (abs(currentRoll) > 80 && abs(currentRoll) < 100) {
            if (currentPitch < proneAngle) return 3
            elevationAngle = currentPitch.toFloat()
            elevationAngleCalculate = nowPitch
            return 0
        } else return 1
    }

    @SuppressLint("SourceLockedOrientationActivity", "DefaultLocale", "SetTextI18n")
    fun updateUI(status: Int, isBack: Boolean = false) {
        when (status) {
            0 -> { //第一个原始页面
                heightMeasureScrollview?.visibility = View.VISIBLE
                setUserHeight?.visibility = View.GONE
                startButton?.text = getString(R.string.start_measure)
            }

            1 -> { //设置身高页面
                if (isBack) { //恢复原来的屏幕显示状态
                    if (isAutoRotateEnabled) {
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
                    } else {
                        if (isPORTRAIT) requestedOrientation =
                            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    }
                    stopRegisterSensor() //传感器取消注册
                }
                heightMeasureTarget?.visibility = View.GONE
                heightMeasureResult?.visibility = View.GONE
                heightMeasureScrollview?.visibility = View.GONE
                setUserHeight?.visibility = View.VISIBLE
                commonLayout?.visibility = View.VISIBLE //显示按钮
                startButton?.text = getString(R.string.next_measure)
            }

            2 -> { //开始测量页面
                measureResult = 0
                startRegisterSensor() //注册传感器
                targetImage?.setImageResource(R.drawable.height_measure_second_step_view)
                commonLayout?.visibility = View.GONE //屏蔽按钮
                isAutoRotateEnabled = Settings.System.getInt(
                    applicationContext.contentResolver,
                    Settings.System.ACCELEROMETER_ROTATION
                ) == 1 //是否开启了屏幕自动旋转
                val orientation = resources.configuration.orientation
                if (orientation == Configuration.ORIENTATION_PORTRAIT) isPORTRAIT = true //竖屏状态
                else if (orientation == Configuration.ORIENTATION_LANDSCAPE) isPORTRAIT = false
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE //强制竖屏显示
                setUserHeight?.visibility = View.GONE
                heightMeasureResult?.visibility = View.GONE
                heightMeasureTarget?.visibility = View.VISIBLE
            }

            3 -> { //显示结果页面
                stopRegisterSensor() //传感器取消注册
                commonLayout?.visibility = View.VISIBLE //显示按钮
                startButton?.text = getString(R.string.again_measure) //按钮文字修改为重新测量
                if (isAutoRotateEnabled) { //恢复原来的屏幕显示状态
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
                } else {
                    if (isPORTRAIT) requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
                heightMeasureTarget?.visibility = View.GONE
                heightMeasureResult?.visibility = View.VISIBLE
                yourHeightTextView?.text = String.format("%.2f", yourHeight / 100.0) + " m"
                targetDistanceTextView?.text = "$targetDistance m"
                targetHeightTextView?.text = "$targetHeight m"
                val highPointAngleString =
                    if (elevationAngle > 0) String.format("%.2f", elevationAngle)
                    else "-" + String.format("%.2f", abs(elevationAngle))
                highPointAngleTextView?.text = "$highPointAngleString°"
                lowPointAngleTextView?.text = String.format("%.2f", abs(proneAngle)) + "°"
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER)
            lastAccelerometerValues = event.values.clone()
        else if (event?.sensor?.type == Sensor.TYPE_MAGNETIC_FIELD)
            lastMagnetometerValues = event.values.clone()

        if (lastAccelerometerValues != null && lastMagnetometerValues != null) {
            val rotationMatrix = FloatArray(9)
            val orientationAngles = FloatArray(3)

            if (SensorManager.getRotationMatrix(
                    rotationMatrix,
                    null,
                    lastAccelerometerValues,
                    lastMagnetometerValues
                )
            ) {
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                nowPitch = orientationAngles[1] //俯仰角
                nowRoll = orientationAngles[2] //横滚角
            }
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        //无须校准
    }
}