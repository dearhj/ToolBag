package com.android.toolbag.item

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.android.toolbag.App
import com.android.toolbag.R
import com.android.toolbag.bean.TurnOnTime
import com.android.toolbag.formatDate
import com.android.toolbag.getTodayInfoAsInt
import com.android.toolbag.updateStepsInfo
import com.android.toolbag.widget.StepArcView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter
import java.time.LocalDate

@RequiresApi(Build.VERSION_CODES.O)
class StepCounterActivity : AppCompatActivity(), SensorEventListener {
    private var stepCounterSensor: Sensor? = null
    private var mSensorManager: SensorManager? = null
    private var stepArcView: StepArcView? = null
    private var todayTarget: TextView? = null
    private var todayCount: TextView? = null
    private var barChart: BarChart? = null

    private var mainScope: CoroutineScope? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_step_counter)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { //实现底部导航栏透明
            // 设置窗口不适应系统窗口，允许内容绘制在系统栏后面
            WindowCompat.setDecorFitsSystemWindows(window, false)
            // 禁用导航栏对比度增强（防止出现半透明遮罩）
            window.isNavigationBarContrastEnforced = false
        }

        mSensorManager = getSystemService("sensor") as SensorManager

        stepArcView = findViewById(R.id.cc)
        todayTarget = findViewById(R.id.tv_set)
        todayCount = findViewById(R.id.tv_data)
        barChart = findViewById(R.id.barChart)

        stepArcView?.setTarget(App.dao?.getTurnOnTime("targetNum")?.dataValue ?: 7000)

        todayTarget!!.setOnClickListener {
            showEditTextDialog()
        }

        todayCount!!.setOnClickListener {
        }

        initBarChart(barChart!!)
        mainScope = MainScope()
    }

    override fun onResume() {
        super.onResume()
        updateActivityUiAndDb(true)
        stepCounterSensor = mSensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        mSensorManager?.registerListener(
            this,
            stepCounterSensor,
            SensorManager.SENSOR_DELAY_UI
        )
    }

    override fun onPause() {
        super.onPause()
        mSensorManager?.unregisterListener(this)
    }

    private fun updateActivityUiAndDb(isResume: Boolean = false) {
        mainScope!!.launch(Dispatchers.IO) {
            if (isResume) {
                val map = getBarChartData()
                val listData = map.keys.toList()
                val listSteps = map.values.toList()
                val steps = App.dao?.getStepCountByDate(getTodayInfoAsInt())?.todaySteps ?: 0L
                withContext(Dispatchers.Main) {
                    stepArcView!!.setCurrentCount(steps)
                    updateBarChart(barChart!!, listSteps, listData)
                }
            } else {
                updateStepsInfo(this@StepCounterActivity)
                delay(300)
                val map = getBarChartData()
                val listData = map.keys.toList()
                val listSteps = map.values.toList()
                val steps = App.dao?.getStepCountByDate(getTodayInfoAsInt())?.todaySteps ?: 0L
                withContext(Dispatchers.Main) {
                    stepArcView!!.setCurrentCount(steps)
                    updateBarChart(barChart!!, listSteps, listData)
                }
            }
        }

    }

    private fun getBarChartData(): MutableMap<String, Long> {
        val map = mutableMapOf<String, Long>()
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        val date = LocalDate.parse(getTodayInfoAsInt().toString(), formatter)
        val dataList =
            (0 until 7).map { date.minusDays(it.toLong()).format(formatter).toInt() }.reversed()
        dataList.forEach {
            val queryResult = App.dao?.getStepCountByDate(it)
            if (queryResult != null) map[queryResult.displayDate] = queryResult.todaySteps
            else map[formatDate(it.toString())] = 0
        }
        return map
    }

    private fun initBarChart(barChart: BarChart) {
        // 禁用缩放
        barChart.setScaleEnabled(false)
        barChart.setPinchZoom(false)
        // 去掉网格线
        barChart.setDrawGridBackground(false)
        barChart.axisLeft.setDrawGridLines(false)
        barChart.xAxis.setDrawGridLines(false)
        // 禁用图例和描述
        barChart.legend.isEnabled = false
        barChart.description.isEnabled = false

        // 修改X轴样式
        barChart.xAxis.textColor = ContextCompat.getColor(this, R.color.color_circle_4)
        barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM // X轴标签显示在下方
        barChart.xAxis.labelCount = 7 // 设置X轴标签数量
        barChart.xAxis.textSize = 10f // 设置X轴标签数量数值文字大小

        // 修改Y轴样式
        barChart.axisLeft.textColor = ContextCompat.getColor(this, R.color.color_circle_4)
        barChart.axisLeft.axisMinimum = 0f  // Y轴从0开始
        barChart.axisLeft.textSize = 10f  // 设置Y轴标签数量数值文字大小
        barChart.axisRight.isEnabled = false  // 禁用右侧Y轴
    }

    private fun updateBarChart(barChart: BarChart, data: List<Long>, timeData: List<String>) {
        // 创建新的数据集
        val entries = mutableListOf<BarEntry>()
        data.forEachIndexed { index, value ->
            entries.add(BarEntry(index.toFloat(), value.toFloat()))
        }

        val dataSet = BarDataSet(entries, "步数")
        dataSet.color = ContextCompat.getColor(this, R.color.sound_value2)
        dataSet.valueTextColor = ContextCompat.getColor(this, R.color.color_circle_4)
        dataSet.valueTextSize = 12f
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return value.toInt().toString() // 格式化为整型数
            }
        }

        val barData = BarData(dataSet)
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(timeData)
        barChart.data = barData
        barChart.invalidate() // 刷新图表
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            updateActivityUiAndDb()
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }

    @SuppressLint("SetTextI18n")
    private fun showEditTextDialog() {
        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.input_layout, null)
        val inputValue = dialogView.findViewById<EditText>(R.id.text)
        val regex = "^(?:[1-9]\\d{0,5}|1000000)\$".toRegex()
        var lastValidValue = ""
        inputValue.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                try {
                    s?.let {
                        var input = it.toString()
                        if (input.isEmpty() || regex.matches(input)) {
                            if (input.startsWith("0") && input.length > 1) {
                                input = input.substring(1)
                                inputValue.setText(input)
                                inputValue.setSelection(inputValue.text.length)
                            }
                            lastValidValue = input
                        } else {
                            inputValue.setText(lastValidValue)
                            inputValue.setSelection(inputValue.text.length)
                        }
                    }
                } catch (_: Exception) {
                }
            }
        })
        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.input))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.sure)) { _, _ ->
                if (inputValue.text.toString() != "")
                    updateTargetValue(inputValue.text.toString().toInt())
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ -> }
            .create()
        dialog.show()
    }

    private fun updateTargetValue(value: Int) {
        App.dao?.insertOrUpdateToTurnOnTime(TurnOnTime("targetNum", value))
        stepArcView!!.setTodayTargetCount(value)
    }
}