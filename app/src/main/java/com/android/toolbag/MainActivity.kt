package com.android.toolbag

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.SimpleAdapter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.android.toolbag.widget.LineGridView
import java.util.HashMap


class MainActivity : AppCompatActivity() {
    private var item: List<Map<String, Any>>? = null
    private var simpleAdapter: SimpleAdapter? = null
    private var stringName: Array<String>? = null
    private var lineGridView: LineGridView? = null


    @SuppressLint("Recycle")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

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
        for (i in actionImageResIds.indices) {
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