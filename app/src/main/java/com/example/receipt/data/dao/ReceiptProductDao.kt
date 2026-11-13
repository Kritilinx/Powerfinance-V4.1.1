package com.example.receipt.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.receipt.data.model.ReceiptProduct
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptProductDao {

    @Query("SELECT * FROM receipt_products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ReceiptProduct>>

    @Query("SELECT * FROM receipt_products WHERE sku = :sku LIMIT 1")
    suspend fun getProductBySku(sku: String): ReceiptProduct?

    @Query("SELECT * FROM receipt_products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Long): ReceiptProduct?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProduct(product: ReceiptProduct): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ReceiptProduct>)

    @Update
    suspend fun updateProduct(product: ReceiptProduct)

    @Delete
    suspend fun deleteProduct(product: ReceiptProduct)

    @Query("SELECT COUNT(*) FROM receipt_products")
    suspend fun countProducts(): Int

    @Query("SELECT * FROM receipt_products WHERE name LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchProducts(query: String): Flow<List<ReceiptProduct>>
}
