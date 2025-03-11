package com.android.toolbag.bean

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "turn_on_time")
data class TurnOnTime(
    @PrimaryKey val turnOn: String = "TurnOn",
    val lastTurnOnTime: Int = 18931226, // 日期作为主键
)