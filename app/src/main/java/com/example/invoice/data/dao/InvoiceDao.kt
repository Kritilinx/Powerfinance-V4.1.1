package com.example.invoice.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.invoice.data.model.InvoiceItemRecord
import com.example.invoice.data.model.InvoiceRecord
import com.example.invoice.data.model.InvoiceWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {

    @Transaction
    @Query("SELECT * FROM invoice_records ORDER BY createdAt DESC, id DESC")
    fun getAllInvoices(): Flow<List<InvoiceWithItems>>

    @Transaction
    @Query("SELECT * FROM invoice_records WHERE id = :id LIMIT 1")
    fun getInvoiceById(id: Long): Flow<InvoiceWithItems?>

    @Query("SELECT * FROM invoice_records WHERE invoiceNumber = :number LIMIT 1")
    suspend fun getInvoiceByNumber(number: String): InvoiceRecord?

    @Query("SELECT * FROM invoice_records ORDER BY id DESC LIMIT 1")
    suspend fun getLatestInvoice(): InvoiceRecord?

    @Transaction
    @Query("""
        SELECT * FROM invoice_records 
        WHERE invoiceNumber LIKE '%' || :query || '%' 
           OR customerName LIKE '%' || :query || '%' 
           OR customerPhone LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
    """)
    fun searchInvoices(query: String): Flow<List<InvoiceWithItems>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertInvoice(invoice: InvoiceRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<InvoiceItemRecord>)

    @Delete
    suspend fun deleteInvoice(invoice: InvoiceRecord)

    @Transaction
    @Query("SELECT * FROM invoice_records ORDER BY createdAt ASC, id ASC")
    suspend fun getAllInvoicesSync(): List<InvoiceWithItems>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceReplace(invoice: InvoiceRecord): Long

    @Query("DELETE FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun deleteItemsByInvoiceId(invoiceId: Long)

    @Query("DELETE FROM invoice_records WHERE id = :invoiceId")
    suspend fun deleteInvoiceById(invoiceId: Long)

    @Query("SELECT COUNT(*) FROM invoice_records")
    suspend fun countInvoices(): Int
}
