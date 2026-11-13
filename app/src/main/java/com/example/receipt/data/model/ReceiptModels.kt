package com.example.receipt.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * Product Master Table specifically for Receipt Generator (Separate Database: receipt_feature.db)
 */
@Entity(
    tableName = "receipt_products",
    indices = [Index(value = ["sku"], unique = true)]
)
data class ReceiptProduct(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sku: String,
    val name: String,
    val unitPrice: Double,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Saved Receipt Header Record
 */
@Entity(
    tableName = "receipt_records",
    indices = [
        Index(value = ["receiptNumber"], unique = true),
        Index(value = ["dateEpochMs"])
    ]
)
data class ReceiptRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val receiptNumber: String,
    val dateEpochMs: Long = System.currentTimeMillis(),
    val formattedDateTime: String,
    val storeName: String = "Diprint Indonesia",
    val storeAddress: String = "Jl. Percetakan Grafika No. 12, Jakarta",
    val storePhone: String = "0812-3456-7890",
    val paymentMethod: String = "Lunas", // Lunas / Belum Dibayar
    val totalAmount: Double,
    val totalItemTypes: Int,
    val totalQuantity: Int,
    val footerMessage: String = "Terima kasih atas kunjungan Anda!",
    val printedAtFormatted: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Item Snapshot on a Receipt.
 * Stores immutable copy of SKU, Name, and Unit Price so that subsequent edits
 * to the master product table will NEVER affect historical receipts.
 */
@Entity(
    tableName = "receipt_items",
    foreignKeys = [
        ForeignKey(
            entity = ReceiptRecord::class,
            parentColumns = ["id"],
            childColumns = ["receiptId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["receiptId"])]
)
data class ReceiptItemSnapshot(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val receiptId: Long,
    val sku: String,
    val productName: String,
    val unitPrice: Double,
    val quantity: Int,
    val subtotal: Double
)

/**
 * 1-to-many relationship for Receipt Record and its Item Snapshots
 */
data class ReceiptWithItems(
    @Embedded val receipt: ReceiptRecord,
    @Relation(
        parentColumn = "id",
        entityColumn = "receiptId"
    )
    val items: List<ReceiptItemSnapshot>
)

/**
 * Transient item model used when authoring a receipt
 */
data class ReceiptCartItem(
    val productId: Long? = null,
    val sku: String,
    val productName: String,
    val originalPrice: Double,
    val customPrice: Double,
    val quantity: Int
) {
    val subtotal: Double get() = customPrice * quantity
}

/**
 * Store settings for receipt header and numbering
 */
data class ReceiptStoreSettings(
    val storeName: String = "Diprint Indonesia",
    val address: String = "Jl. Percetakan Grafika No. 12, Jakarta",
    val phone: String = "0812-3456-7890",
    val receiptPrefix: String = "STRUK-",
    val startNumber: Int = 1,
    val footerNote1: String = "Terima kasih atas kunjungan Anda!",
    val footerNote2: String = "Barang yang sudah dicetak tidak dapat ditukar/dikembalikan."
)
