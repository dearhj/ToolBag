package com.android.toolbag.item

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.toolbag.App
import com.android.toolbag.R
import com.android.toolbag.adapter.HistoryStepsAdapter
import com.android.toolbag.formatDateStr

@RequiresApi(Build.VERSION_CODES.O)
class HistoryStepsActivity : AppCompatActivity() {
    private val adapter = HistoryStepsAdapter()
    private var recyclerView: RecyclerView? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history_steps)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { //实现底部导航栏透明
            // 设置窗口不适应系统窗口，允许内容绘制在系统栏后面
            WindowCompat.setDecorFitsSystemWindows(window, false)
            // 禁用导航栏对比度增强（防止出现半透明遮罩）
            window.isNavigationBarContrastEnforced = false
        }

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView?.layoutManager = LinearLayoutManager(this)
        recyclerView?.adapter = adapter
        adapter.setList(getStepsData())
    }


    private fun getStepsData(): MutableList<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        val data = App.dao?.getAllStepCounts()
        data?.forEach {
            if (!it.virtualData) { //非虚拟数据才加入列表
                val dateDisplay = formatDateStr(it.date.toString())
                list.add(Pair(dateDisplay, it.todaySteps.toString()))
            }
        }
        return list
    }
}