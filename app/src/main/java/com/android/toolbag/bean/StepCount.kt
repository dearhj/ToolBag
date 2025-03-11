package com.android.toolbag.bean

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "step_count")
data class StepCount(
    @PrimaryKey val date: Int, // 日期作为主键 格式20250310
    val displayDate: String, //用于UI显示的时间 格式03/10 去掉年份
    val todaySteps: Long,  //用于记录今日步数，
    val sensorSteps: Long, //用于记录此时传感器数据，
)