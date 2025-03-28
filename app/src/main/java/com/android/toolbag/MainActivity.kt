package com.android.toolbag

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.AdapterView
import android.widget.SimpleAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.WindowCompat
import com.android.toolbag.item.AlarmActivity
import com.android.toolbag.item.CompassActivity
import com.android.toolbag.item.FlashlightActivity
import com.android.toolbag.item.GradientActivity
import com.android.toolbag.item.GravityVertical
import com.android.toolbag.item.HeightMeasureActivity
import com.android.toolbag.item.MagnifierActivity
import com.android.toolbag.item.NoiseActivity
import com.android.toolbag.item.PaintingActivity
import com.android.toolbag.item.ProtractorActivity
import com.android.toolbag.item.StepCounterActivity
import com.android.toolbag.widget.LineGridView


class MainActivity : AppCompatActivity() {
    private var item: List<Map<String, Any>>? = null
    private var simpleAdapter: SimpleAdapter? = null
    private var itemName: Array<String>? = null
    private var lineGridView: LineGridView? = null
    private var toolbar: Toolbar? = null


    @SuppressLint("Recycle", "NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { //实现底部导航栏透明
            // 设置窗口不适应系统窗口，允许内容绘制在系统栏后面
            WindowCompat.setDecorFitsSystemWindows(window, false)
            // 禁用导航栏对比度增强（防止出现半透明遮罩）
            window.isNavigationBarContrastEnforced = false
        }

        startForegroundService(Intent(this, LightControlService::class.java))

        val supportStepCounter =
            packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        lineGridView = findViewById(R.id.tools_app)

        itemName = getResources().getStringArray(R.array.action_name)
        val obtainTypedArray = getResources().obtainTypedArray(R.array.actions_images)
        val length = obtainTypedArray.length()
        val actionImageResIds = IntArray(length)
        for (i in 0 until length) {
            actionImageResIds[i] = obtainTypedArray.getResourceId(i, 0) // 获取drawable资源的ID
        }
        obtainTypedArray.recycle()
        item = ArrayList()
        actionImageResIds.indices.forEach { i ->
            val itemName = itemName!![i]
            val hashMap = hashMapOf<String, Any>(
                "img" to actionImageResIds[i],
                "text" to itemName
            )
            when {
                itemName != getString(R.string.step) -> (item as ArrayList<Map<String, Any>>).add(
                    hashMap
                )

                supportStepCounter -> (item as ArrayList<Map<String, Any>>).add(hashMap)
            }
        }

        val iArr = intArrayOf(R.id.img, R.id.text)
        simpleAdapter =
            SimpleAdapter(this, item, R.layout.gridview_item, arrayOf("img", "text"), iArr)
        lineGridView?.adapter = simpleAdapter
        lineGridView?.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            startItemActivity(itemName!![position])
        }
    }

    private fun startItemActivity(item: String) {
        when (item) {
            getString(R.string.torch) -> {
                val intent = Intent(this, FlashlightActivity::class.java)
                startActivity(intent)
            }

            getString(R.string.protractor) -> {
                val intent = Intent(this, ProtractorActivity::class.java)
                startActivity(intent)
            }

            getString(R.string.plumb) -> {
                val intent = Intent(this, GravityVertical::class.java)
                startActivity(intent)
            }

            getString(R.string.noise) -> {
                val intent = Intent(this, NoiseActivity::class.java)
                startActivity(intent)
            }

            getString(R.string.magnifier) -> {
                val intent = Intent(this, MagnifierActivity::class.java)
                startActivity(intent)
            }

            getString(R.string.height_measure) -> {
                val intent = Intent(this, HeightMeasureActivity::class.java)
                startActivity(intent)
            }

            getString(R.string.handingpaint) -> {
                val intent = Intent(this, PaintingActivity::class.java)
                startActivity(intent)
            }

            getString(R.string.gradienter) -> {
                val intent = Intent(this, GradientActivity::class.java)
                startActivity(intent)
            }

            getString(R.string.compass) -> {
                val intent = Intent(this, CompassActivity::class.java)
                startActivity(intent)
            }

            getString(R.string.alarm) -> {
                val intent = Intent(this, AlarmActivity::class.java)
                startActivity(intent)
            }

            getString(R.string.step) -> {
                val intent = Intent(this, StepCounterActivity::class.java)
                startActivity(intent)
            }
        }
    }
}