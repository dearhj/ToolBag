package com.android.toolbag

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.provider.Settings
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.io.IOException

fun observerGlobal(context: Context, name: String, block: (Boolean) -> Unit) {
    observer(context, Settings.Global.getUriFor(name), block)
}

private fun observer(context: Context, uri: Uri, block: (Boolean) -> Unit) {
    context.contentResolver.registerContentObserver(uri, false, oo { block(it) })
}

fun oo(block: (Boolean) -> Unit): ContentObserver {
    return object : ContentObserver(null) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            block(selfChange)
        }
    }
}


var dbCount = 40f
var value = 0f // 声音分贝值
const val ALPHA = 0.3f // 平滑因子，值越小越平滑，越接近1越灵敏

fun getSmoothedDb(newDb: Float) {
    dbCount = ALPHA * newDb + (1 - ALPHA) * dbCount
}

fun createFile(fileName: String): File? {
    try {
        val externalDir = App.mContext!!.getExternalFilesDir(null)
        val myCaptureFile = File(externalDir, fileName)
        if (myCaptureFile.exists()) {
            myCaptureFile.delete()
        }
        myCaptureFile.createNewFile()
        return myCaptureFile
    } catch (e: IOException) {
        e.printStackTrace()
        return null
    }
}

var toast: Toast? = null
@SuppressLint("InflateParams")
fun customToast(activity: Activity, str: String) {
    val inflater = activity.layoutInflater
    val layout = inflater.inflate(R.layout.custom_toast, null)
    val textView = layout.findViewById<TextView>(R.id.toast_text)
    textView.text = str
    if (toast != null) toast?.cancel()
    toast = Toast(App.mContext)
    toast?.view = layout
    toast?.duration = Toast.LENGTH_SHORT
    toast?.setGravity(Gravity.CENTER, 0, 0)
    toast?.show()
}
