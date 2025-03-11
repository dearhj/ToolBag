package com.android.toolbag

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Size
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.android.toolbag.bean.StepCount
import com.android.toolbag.bean.TurnOnTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

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

fun getOptimalSize(sizeArr: Array<Size>, width: Int, height: Int): Size? {
    var d = (width / height).toDouble()
    if (d > 2.0) {
        d = 2.0
    }
    var d2 = Double.MAX_VALUE
    var size: Size? = null
    val sizeList = sizeArr.sortedByDescending { it.width }
    for (size2 in sizeList) {
        if (abs((size2.width / size2.height) - d) <= 0.1 && size2.height >= height) {
            val abs = abs((size2.height - height).toDouble())
            if (abs <= d2) {
                size = size2
                d2 = abs
            }
        }
    }
    return size
}

@RequiresApi(Build.VERSION_CODES.O)
fun formatDate(input: String): String {
    val inputFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    val outputFormatter = DateTimeFormatter.ofPattern("MM/dd")
    val date = LocalDate.parse(input, inputFormatter)
    return date.format(outputFormatter)
}

@RequiresApi(Build.VERSION_CODES.O)
fun formatDateStr(input: String): String {
    val inputFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    val outputFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
    val date = LocalDate.parse(input, inputFormatter)
    return date.format(outputFormatter)
}

@RequiresApi(Build.VERSION_CODES.O)
fun getTodayInfoAsInt(): Int {
    val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    val currentDate = LocalDate.now().format(formatter)
    return currentDate.toInt()
}

@RequiresApi(Build.VERSION_CODES.O)
fun getTodayInfo(): String {
    val formatter = DateTimeFormatter.ofPattern("MM/dd")
    val today = LocalDate.now()
    return today.format(formatter)
}

@RequiresApi(Build.VERSION_CODES.O)
fun getYesterdayDateAsInt(): Int {
    val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    val yesterday = LocalDate.now().minusDays(1)
    val formattedDate = yesterday.format(formatter)
    return formattedDate.toInt()
}

@RequiresApi(Build.VERSION_CODES.O)
fun getYesterdayDateInfo(): String {
    val formatter = DateTimeFormatter.ofPattern("MM/dd")
    val yesterday = LocalDate.now().minusDays(1)
    return yesterday.format(formatter)
}

@RequiresApi(Build.VERSION_CODES.O)
fun getDate30DaysAgo(): Int {
    val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    val date30DaysAgo = LocalDate.now().minusDays(30)
    return date30DaysAgo.format(formatter).toInt()
}

@Synchronized
fun getCurrentSteps(context: Context, change: (steps: Long) -> Unit) {
    var hasData = false
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    val supportStepCounter =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER)
    if (stepCounterSensor == null || !supportStepCounter) {
        change(-1L)
        return
    }
    val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                if (!hasData) {
                    change(event.values[0].toLong())
                    hasData = true
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        }
    }
    sensorManager.registerListener(
        listener,
        stepCounterSensor,
        SensorManager.SENSOR_DELAY_FASTEST
    )
    MainScope().launch(Dispatchers.IO) {
        delay(300)
        sensorManager.unregisterListener(listener)
        withContext(Dispatchers.Main) { if (!hasData) change(0L) }
    }
    return
}

/*
    设备开机 应用启动，在开机广播监听处插入一条数据，
    记录开机时间，也就是传感器数据清空时间。

    判断传感器开机记录数据的时间与今天的时间差， 初始值为18931226
    如果属于同一天。
    获取今日的步数数据，如果不存在则指定为0
    将进入步数数据加上传感器数值之和存入数据库
    将传感器开机记录数据时间调整为前一天。
    获取昨天的数据库数，
    如存在，则将昨天的sensorSteps数据改为今日步数的负数。
    如不存在，则插入一条虚拟数据。 并将sensorSteps数据改为今日步数的负数。

    如果比今天早，
    获取前面一天的数据，
    如存在，则将传感器数据与前一天数据中的sensorSteps相减后更新到数据库，并更新sensorSteps为传感器具体值。
    如不存在：
    如果是因为设备关机导致没有前一天的数据，那不会走到这里，
    如果是因为应用强制结束，没有运行，首次安装，向后调系统时间导致没有前面的数据，那么记录今日数据为0，创建虚拟数据，将传感器数据作为前一日数据的sensorSteps插入数据。

    如果比今天晚，证明用户将系统时间向前调了，
    删除今天之后的所有数据（可能删除所有）
    将传感器开机记录数据时间调整为前一天。
    将当天步数数据更新为0
    获取前面一天的数据。
    如存在，则数据中的sensorSteps值修改为传感器实时数据后，更新到数据库。
    如不存在，创建虚拟数据，将传感器数据作为前一日数据的sensorSteps插入数据库
*/
@RequiresApi(Build.VERSION_CODES.O)
@Synchronized
fun updateStepsInfo(mContext: Context) {
    MainScope().launch(Dispatchers.IO) {
        val todayInt = getTodayInfoAsInt() //今天日期
        val todayStr = getTodayInfo() //今天日期字符串形式
        val turnOnTime = App.dao?.getTurnOnTime("TurnOn")?.dataValue ?: 18931226  //开机时间，传感器数据清空时间
        App.dao?.deleteOldData(getDate30DaysAgo()) //删除30天以前的数据
        if (todayInt == turnOnTime) { //今天刚开机
            val yesInt = getYesterdayDateAsInt()
            val yesStr = getYesterdayDateInfo()
            App.dao?.insertOrUpdateToTurnOnTime(TurnOnTime("TurnOn", yesInt)) //将传感器开机记录数据时间调整为前一天
            val todaySteps =
                App.dao?.getStepCountByDate(todayInt)?.todaySteps ?: 0L //从数据库中获取今天的步数信息
            val yesInfo = App.dao?.getStepCountByDate(yesInt) //从数据库中获取前一天的步数信息
            if (yesInfo != null)
                App.dao?.insertOrUpdate( //将今日步数的负值作为昨日数据的sensorSteps，更新到数据库
                    StepCount(yesInt, yesStr, yesInfo.todaySteps, -todaySteps, yesInfo.virtualData)
                )
            else  //新插入一条假数据,今天步数负值为昨日的sensorSteps值
                App.dao?.insertOrUpdate(StepCount(yesInt, yesStr, 0, -todaySteps, true))
            getCurrentSteps(mContext) {
                App.dao?.insertOrUpdate(
                    StepCount(todayInt, todayStr, it + todaySteps, it)
                )
            }
        } else if (todayInt > turnOnTime) { //之前就已经开机了
            val yesterdayInt = getYesterdayDateAsInt()
            val yesSteps = App.dao?.getStepCountByDate(yesterdayInt) //从数据库中获取前一天的步数信息
            getCurrentSteps(mContext) {
                if (yesSteps != null)
                    App.dao?.insertOrUpdate( //将传感器数据与前一天数据中的sensorSteps相减后更新到数据库，并更新sensorSteps为传感器具体值
                        StepCount(todayInt, todayStr, it - yesSteps.sensorSteps, it)
                    )
                else { //记录今日数据0，创建虚拟数据，将传感器数据作为前一日数据的sensorSteps插入数据
                    App.dao?.insertOrUpdate(StepCount(todayInt, todayStr, 0, it))
                    val yesterdayStr = getYesterdayDateInfo()
                    App.dao?.insertOrUpdate(StepCount(yesterdayInt, yesterdayStr, 0, it, true))
                }
            }
        } else { //用户将系统时间往前调了
            val yesInt = getYesterdayDateAsInt()
            App.dao?.deleteAfterDate(todayInt) //删除今天之后的所有数据
            App.dao?.insertOrUpdateToTurnOnTime(
                TurnOnTime("TurnOn", yesInt)
            ) //将传感器开机记录数据时间调整为前一天
            val yesSteps = App.dao?.getStepCountByDate(yesInt) //从数据库中获取前一天的步数信息
            getCurrentSteps(mContext) {
                App.dao?.insertOrUpdate(StepCount(todayInt, todayStr, 0, it)) //将当天数据更新为0
                val yesStr = getYesterdayDateInfo()
                if (yesSteps != null) {
                    App.dao?.insertOrUpdate( //将昨天数据中的sensorSteps修改为传感器实时数据后，更新到数据库
                        StepCount(yesInt, yesStr, yesSteps.todaySteps, it, yesSteps.virtualData)
                    )
                } else  //创建虚拟数据，将传感器数据作为前一日数据sensorSteps插入数据库
                    App.dao?.insertOrUpdate(StepCount(yesInt, yesStr, 0, it, true))
            }
        }
    }
}

