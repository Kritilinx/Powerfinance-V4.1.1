package com.example.receipt.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.receipt.data.model.ReceiptItemSnapshot
import com.example.receipt.data.model.ReceiptRecord
import com.example.receipt.data.model.ReceiptWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptRecordDao {

    @Transaction
    @Query("SELECT * FROM receipt_records ORDER BY dateEpochMs DESC, id DESC")
    fun getAllReceiptsWithItems(): Flow<List<ReceiptWithItems>>

    @Transaction
    @Query("SELECT * FROM receipt_records ORDER BY dateEpochMs DESC, id DESC")
    suspend fun getAllReceiptsWithItemsSync(): List<ReceiptWithItems>

    @Transaction
    @Query("SELECT * FROM receipt_records WHERE id = :id LIMIT 1")
    suspend fun getReceiptWithItemsById(id: Long): ReceiptWithItems?

    @Query("SELECT * FROM receipt_records WHERE receiptNumber = :receiptNumber LIMIT 1")
    suspend fun getReceiptByNumber(receiptNumber: String): ReceiptRecord?

    @Query("SELECT * FROM receipt_records ORDER BY id DESC LIMIT 1")
    suspend fun getLatestReceipt(): ReceiptRecord?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReceipt(receipt: ReceiptRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceiptItems(items: List<ReceiptItemSnapshot>)

    @Query("DELETE FROM receipt_records WHERE id = :id")
    suspend fun deleteReceiptById(id: Long)

    @Query("SELECT COUNT(*) FROM receipt_records")
    suspend fun countReceipts(): Int
}
