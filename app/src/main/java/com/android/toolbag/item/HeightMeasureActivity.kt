package com.android.toolbag.item

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
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
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.android.toolbag.R
import com.android.toolbag.widget.CustomNumberPicker


class HeightMeasureActivity : AppCompatActivity() {
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

    private var uiStatus = 0
    private var isAutoRotateEnabled = false
    private var isPORTRAIT = false

    private var vibrator: Vibrator? = null

    private var measureResult = 0 //0表示还未开始测量 1表示俯角测量成功 2表示仰角测量成功

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

        firstNumberPicker?.minValue = 0
        firstNumberPicker?.maxValue = 2
        firstNumberPicker?.value = 1
        secondNumberPicker?.minValue = 0
        secondNumberPicker?.maxValue = 9
        secondNumberPicker?.value = 7
        lastNumberPicker?.minValue = 0
        lastNumberPicker?.maxValue = 9
        lastNumberPicker?.value = 5

        // 获取当前值
//        val currentValue = customNumberPicker.value

        startButton?.setOnClickListener {
            if (uiStatus == 3) uiStatus = 1 //在显示结果页面点击按钮，跳到设置升高页面，重新测量
            else uiStatus++ //在其他页面点击按钮，跳到下一步
            updateUI(uiStatus)
        }
        targetImage?.setOnClickListener {
            if (measureResult == 0) {
                //俯角测量
                startProneAngleMeasurement()
                //操作成功
                measureResult++
                if (vibrator != null && vibrator!!.hasVibrator()) {
                    vibrator!!.vibrate(300L)
                }
                targetImage?.setImageResource(R.drawable.height_measure_last_step_view)
            } else if (measureResult == 1) {
                //仰角测量
                startElevationAngleMeasurement()
                //操作成功
                measureResult++
                if (vibrator != null && vibrator!!.hasVibrator()) {
                    val vibrationPattern = longArrayOf(0, 200, 200, 200) // 震动模式
                    val vibrationEffect = VibrationEffect.createWaveform(vibrationPattern, -1)
                    vibrator!!.vibrate(vibrationEffect)
                }
                uiStatus++
                updateUI(uiStatus)
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

    private fun startProneAngleMeasurement() {
        TODO("Not yet implemented")
    }

    private fun startElevationAngleMeasurement() {
        TODO("Not yet implemented")
    }

    @SuppressLint("SourceLockedOrientationActivity")
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
                commonLayout?.visibility = View.VISIBLE //显示按钮
                startButton?.text = getString(R.string.again_measure) //按钮文字修改为重新测量
                if (isAutoRotateEnabled) { //恢复原来的屏幕显示状态
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
                } else {
                    if (isPORTRAIT) requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
                heightMeasureTarget?.visibility = View.GONE
                heightMeasureResult?.visibility = View.VISIBLE
            }
        }
    }
}