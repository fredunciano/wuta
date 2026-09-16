package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.WaterLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterLogDao {
    @Query("SELECT * FROM water_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<WaterLogEntity>>

    @Query("SELECT * FROM water_logs WHERE dateEpochDay = :epochDay ORDER BY timestamp DESC")
    fun getLogsForDay(epochDay: Long): Flow<List<WaterLogEntity>>

    @Query("SELECT * FROM water_logs WHERE timestamp >= :startMillis AND timestamp <= :endMillis ORDER BY timestamp ASC")
    fun getLogsBetween(startMillis: Long, endMillis: Long): Flow<List<WaterLogEntity>>

    @Query("SELECT * FROM water_logs WHERE timestamp >= :startMillis AND timestamp <= :endMillis ORDER BY timestamp ASC")
    suspend fun getLogsBetweenSync(startMillis: Long, endMillis: Long): List<WaterLogEntity>

    @Query("SELECT * FROM water_logs WHERE isSynced = 0")
    suspend fun getUnsyncedLogs(): List<WaterLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: WaterLogEntity): Long

    @Update
    suspend fun updateLog(log: WaterLogEntity)

    @Query("DELETE FROM water_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long)

    @Query("UPDATE water_logs SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markLogsSynced(ids: List<Long>)

    @Query("SELECT SUM(amountMl) FROM water_logs WHERE dateEpochDay = :epochDay")
    fun getTotalMlForDay(epochDay: Long): Flow<Int?>
}
