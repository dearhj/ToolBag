package com.android.toolbag.item

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.android.toolbag.R
import com.android.toolbag.widget.StepArcView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

class StepCounterActivity : AppCompatActivity(), SensorEventListener {
    private var stepCounterSensor: Sensor? = null
    private var stepCounterSensor11: Sensor? = null
    private var mSensorManager: SensorManager? = null
    private var stepArcView: StepArcView? = null
    private var todayTarget: TextView? = null
    private var todayCount: TextView? = null
    private var barChart: BarChart? = null

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

        var count = 1000
        var targetCount = 5000
        todayTarget!!.setOnClickListener {
            stepArcView!!.setTodayTargetCount(targetCount)
            targetCount += 2000
        }

        todayCount!!.setOnClickListener {
            stepArcView!!.setCurrentCount(count)
            count += 1000
            val dataTime = listOf("Day 11", "Day 22", "Day 33", "Day 44", "Day 55", "Day 66", "Day 71")
            val data = listOf(10000f, 300f, 50f, 4000f, 900f, 500f, 760f)
            updateBarChart(barChart!!, data, dataTime)
        }

        initBarChart(barChart!!)
        val dataTime = listOf("Day 1", "Day 2", "Day 3", "Day 4", "Day 5", "Day 6", "Day 7")
        val data = listOf(100f, 200f, 300f, 400f, 500f, 600f, 700f)
        updateBarChart(barChart!!, data, dataTime)
    }

    override fun onResume() {
        super.onResume()
        stepCounterSensor = mSensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        stepCounterSensor11 = mSensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        mSensorManager?.registerListener(
            this,
            stepCounterSensor,
            SensorManager.SENSOR_DELAY_UI
        )
        mSensorManager?.registerListener(
            this,
            stepCounterSensor11,
            SensorManager.SENSOR_DELAY_UI
        )

        stepArcView!!.setCurrentCount(50000)

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

    private fun updateBarChart(barChart: BarChart, data: List<Float>, timeData: List<String>) {
        // 创建新的数据集
        val entries = mutableListOf<BarEntry>()
        data.forEachIndexed { index, value ->
            entries.add(BarEntry(index.toFloat(), value))
        }

        val dataSet = BarDataSet(entries, "步数")
        dataSet.color = ContextCompat.getColor(this, R.color.sound_value2)
        dataSet.valueTextColor = ContextCompat.getColor(this, R.color.color_circle_4)
        dataSet.valueTextSize = 12f

        val barData = BarData(dataSet)
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(timeData)
        barChart.data = barData
        barChart.invalidate() // 刷新图表
    }

    override fun onSensorChanged(event: SensorEvent?) {
        println("这里收到了数据跳》》》》   ${event!!.sensor.name}  ${event.sensor.id} ")
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            println("这里获取到了步数数据》》》》   ${event.values[0]}   ")
        }
        if (event?.sensor?.type == Sensor.TYPE_STEP_DETECTOR) {
            println("这里获取到了步数数据11111》》》》   ${event.values[0]}  ")
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }
}