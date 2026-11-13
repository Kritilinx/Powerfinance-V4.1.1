package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class InventoryMutationType(val displayName: String) {
    PURCHASE("Pembelian Persediaan"),
    USAGE("Pemakaian Persediaan (Beban HPP)"),
    ADJUSTMENT("Penyesuaian Nilai Persediaan")
}

@Entity(
    tableName = "inventory_logs",
    indices = [
        Index(value = ["dateEpochMs"]),
        Index(value = ["journalId"])
    ]
)
data class InventoryLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateEpochMs: Long,
    val type: InventoryMutationType,
    val amount: Double,
    val endingBalance: Double,
    val journalId: Long? = null,
    val transactionId: Long? = null,
    val notes: String = ""
)
