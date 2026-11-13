package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.InventoryLog
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_logs ORDER BY dateEpochMs DESC, id DESC")
    fun getAllInventoryLogs(): Flow<List<InventoryLog>>

    @Query("SELECT * FROM inventory_logs ORDER BY dateEpochMs DESC, id DESC LIMIT 1")
    suspend fun getLatestLog(): InventoryLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: InventoryLog): Long

    @Query("SELECT COUNT(*) FROM inventory_logs")
    suspend fun countLogs(): Int

    @Query("SELECT * FROM inventory_logs ORDER BY id ASC")
    suspend fun getAllInventoryLogsList(): List<InventoryLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<InventoryLog>)
}
