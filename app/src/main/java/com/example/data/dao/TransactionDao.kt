package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY dateEpochMs DESC, id DESC")
    fun getAllTransactions(): Flow<List<TransactionRecord>>

    @Query("SELECT * FROM transactions WHERE status = :status ORDER BY dateEpochMs DESC, id DESC")
    fun getTransactionsByStatus(status: TransactionStatus): Flow<List<TransactionRecord>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionRecord): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionRecord)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionRecord)

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun countTransactions(): Int

    @Query("SELECT MAX(id) FROM transactions")
    suspend fun getMaxTransactionId(): Long?

    @Query("SELECT * FROM transactions ORDER BY id ASC")
    suspend fun getAllTransactionsList(): List<TransactionRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionRecord>)
}
