package com.example.invoice.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.util.UUID

/**
 * Isolated Room Entity for Invoice Header records.
 * Table: invoice_records
 * Stored in isolated database: invoice_feature.db
 */
@Entity(
    tableName = "invoice_records",
    indices = [
        Index(value = ["invoiceNumber"], unique = true),
        Index(value = ["dateEpochMs"])
    ]
)
data class InvoiceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String,
    val dateEpochMs: Long = System.currentTimeMillis(),
    val dateFormatted: String,
    val customerName: String,
    val customerPhone: String,
    val customerAddress: String = "",
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val tax: Double = 0.0,
    val shippingAndHandling: Double = 0.0,
    val other: Double = 0.0,
    val totalAmount: Double = 0.0,
    val comments: String = "",
    val pdfFilePath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Isolated Room Entity for Invoice Items.
 * Table: invoice_items
 * Stores immutable snapshot of SKU, description, unit price, quantity, tax, and line total.
 * Changes to master products in receipt_products will NEVER modify historical invoices.
 */
@Entity(
    tableName = "invoice_items",
    foreignKeys = [
        ForeignKey(
            entity = InvoiceRecord::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["invoiceId"])]
)
data class InvoiceItemRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceId: Long,
    val itemNumber: Int,
    val sku: String,
    val description: String,
    val quantity: Int,
    val unitPrice: Double,
    val tax: Double = 0.0,
    val lineTotal: Double
)

/**
 * One-to-many relationship for InvoiceRecord and its immutable line items
 */
data class InvoiceWithItems(
    @Embedded val invoice: InvoiceRecord,
    @Relation(
        parentColumn = "id",
        entityColumn = "invoiceId"
    )
    val items: List<InvoiceItemRecord>
)

/**
 * In-memory model used during invoice draft editing before database persistence.
 */
data class InvoiceDraftItem(
    val id: String = UUID.randomUUID().toString(),
    val itemNumber: Int = 1,
    val sku: String = "",
    val description: String = "",
    val quantity: Int = 1,
    val unitPrice: Double = 0.0,
    val tax: Double = 0.0
) {
    val lineTotal: Double get() = (quantity * unitPrice) + tax
}
