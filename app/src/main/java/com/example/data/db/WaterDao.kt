package com.example.data.db

import androidx.room.*
import com.example.data.model.WaterLog
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterDao {
    @Query("SELECT * FROM water_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<WaterLog>>

    @Query("SELECT * FROM water_logs WHERE timestamp >= :start AND timestamp <= :end ORDER BY timestamp DESC")
    fun getLogsBetween(start: Long, end: Long): Flow<List<WaterLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: WaterLog)

    @Delete
    suspend fun deleteLog(log: WaterLog)

    @Query("DELETE FROM water_logs WHERE id = :id")
    suspend fun deleteLogById(id: Int)

    @Query("DELETE FROM water_logs")
    suspend fun clearAllLogs()
}
