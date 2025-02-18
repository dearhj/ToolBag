package com.android.toolbag

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.provider.Settings
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
