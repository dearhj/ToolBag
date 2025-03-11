package com.android.toolbag.bean

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface StepCountDao {
    //新增数据，如果存在则更新
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(stepCount: StepCount)

    //查询某一天的数据
    @Query("SELECT * FROM step_count WHERE date = :date")
    fun getStepCountByDate(date: Int): StepCount?

    // 删除指定日期之后的所有数据
    @Query("DELETE FROM step_count WHERE date > :cutoffDate")
    fun deleteAfterDate(cutoffDate: Int)

    //删除指定日期之前的所有数据
    @Query("DELETE FROM step_count WHERE date < :cutoffDate")
    fun deleteOldData(cutoffDate: Int)

    // 查询开机时间数据
    @Query("SELECT * FROM turn_on_time WHERE name = :name")
    fun getTurnOnTime(name: String): TurnOnTime?

    // 插入数据，如果数据已存在则更新
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateToTurnOnTime(turnOnTime: TurnOnTime)
}