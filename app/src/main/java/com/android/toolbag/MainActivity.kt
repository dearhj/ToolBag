package com.android.toolbag

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.widget.SimpleAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.WindowCompat
import com.android.toolbag.widget.LineGridView


class MainActivity : AppCompatActivity() {
    private var item: List<Map<String, Any>>? = null
    private var simpleAdapter: SimpleAdapter? = null
    private var stringName: Array<String>? = null
    private var lineGridView: LineGridView? = null
    private var toolbar: Toolbar? = null


    @SuppressLint("Recycle")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { //实现底部导航栏透明
            // 设置窗口不适应系统窗口，允许内容绘制在系统栏后面
            WindowCompat.setDecorFitsSystemWindows(window, false)
            // 禁用导航栏对比度增强（防止出现半透明遮罩）
            window.isNavigationBarContrastEnforced = false
        }

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        lineGridView = findViewById(R.id.tools_app)


        stringName = getResources().getStringArray(R.array.action_name)
        val obtainTypedArray = getResources().obtainTypedArray(R.array.actions_images)
        val length = obtainTypedArray.length()
        val actionImageResIds = IntArray(length)
        for (i in 0 until length) {
            actionImageResIds[i] = obtainTypedArray.getResourceId(i, 0) // 获取drawable资源的ID
        }
        obtainTypedArray.recycle()
        item = ArrayList()
        actionImageResIds.indices.forEach { i ->
            val hashMap = HashMap<String, Any>()
            hashMap["img"] = Integer.valueOf(actionImageResIds[i])
            hashMap["text"] = stringName!![i]
            (item as ArrayList<Map<String, Any>>).add(hashMap)
        }

        val iArr = intArrayOf(R.id.img, R.id.text)
        simpleAdapter = SimpleAdapter(this, item, R.layout.gridview_item, arrayOf("img", "text"), iArr)
        lineGridView?.adapter = simpleAdapter
    }
}