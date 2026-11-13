package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.OpeningBalanceBatch
import com.example.data.model.OpeningBalanceDetail
import kotlinx.coroutines.flow.Flow

@Dao
interface OpeningBalanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(batch: OpeningBalanceBatch): Long

    @Update
    suspend fun updateBatch(batch: OpeningBalanceBatch)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetails(details: List<OpeningBalanceDetail>)

    @Query("SELECT * FROM opening_balance_batches ORDER BY createdAt DESC")
    fun getAllBatches(): Flow<List<OpeningBalanceBatch>>

    @Query("SELECT * FROM opening_balance_batches WHERE id = :id LIMIT 1")
    suspend fun getBatchById(id: Long): OpeningBalanceBatch?

    @Query("SELECT * FROM opening_balance_batches WHERE isPosted = 1 ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestPostedBatch(): OpeningBalanceBatch?

    @Query("SELECT * FROM opening_balance_batches WHERE batchNumber = :batchNumber LIMIT 1")
    suspend fun getBatchByNumber(batchNumber: String): OpeningBalanceBatch?

    @Query("SELECT * FROM opening_balance_batches WHERE cutoffDateEpochMs = :cutoffMs AND isPosted = 1 LIMIT 1")
    suspend fun getPostedBatchForCutoff(cutoffMs: Long): OpeningBalanceBatch?

    @Query("SELECT * FROM opening_balance_details WHERE batchId = :batchId")
    suspend fun getDetailsForBatch(batchId: Long): List<OpeningBalanceDetail>

    @Query("SELECT * FROM opening_balance_batches ORDER BY id ASC")
    suspend fun getAllBatchesList(): List<OpeningBalanceBatch>

    @Query("SELECT * FROM opening_balance_details ORDER BY id ASC")
    suspend fun getAllDetailsList(): List<OpeningBalanceDetail>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatches(batches: List<OpeningBalanceBatch>)

    @Delete
    suspend fun deleteBatch(batch: OpeningBalanceBatch)
}
