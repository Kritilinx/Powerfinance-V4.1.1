package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TransactionStatus(val displayName: String) {
    DRAFT("Draft"),
    POSTED("Diposting"),
    CANCELLED("Dibatalkan")
}

enum class TransactionType(val displayName: String) {
    REVENUE("Penjualan"),
    EXPENSE("Pengeluaran Beban Operasional"),
    PURCHASE_INVENTORY("Pembelian Persediaan"),
    USE_INVENTORY("Pemakaian Persediaan (HPP)"),
    ADJUST_INVENTORY("Penyesuaian Nilai Persediaan"),
    DONATION("Donasi Pemilik (Pengurang Ekuitas)"),
    PAY_DEBT("Pembayaran Hutang"),
    RECEIVE_RECEIVABLE("Penerimaan Piutang"),
    OWNER_CAPITAL("Setoran Modal Pemilik"),
    OWNER_DRAW("Prive Pemilik"),
    TRANSFER("Transfer Kas / Bank"),
    INITIAL_BALANCE("Saldo Awal Pembukuan"),
    EXCEL_MIGRATION("Migrasi Data Excel")
}

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["txNumber"], unique = true),
        Index(value = ["dateEpochMs"]),
        Index(value = ["status"])
    ]
)
data class TransactionRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val txNumber: String,
    val dateEpochMs: Long,
    val description: String,
    val type: TransactionType,
    val status: TransactionStatus = TransactionStatus.POSTED,
    val amount: Double,
    val debitAccountId: Long? = null,
    val creditAccountId: Long? = null,
    val referenceNumber: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
