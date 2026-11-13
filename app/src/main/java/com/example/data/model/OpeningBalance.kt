package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "opening_balance_batches",
    indices = [
        Index(value = ["batchNumber"], unique = true),
        Index(value = ["cutoffDateEpochMs"])
    ]
)
data class OpeningBalanceBatch(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val batchNumber: String,
    val cutoffDateEpochMs: Long,
    val startDateEpochMs: Long,
    val description: String,
    val totalDebit: Double,
    val totalCredit: Double,
    val isPosted: Boolean = false,
    val journalEntryId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "opening_balance_details",
    foreignKeys = [
        ForeignKey(
            entity = OpeningBalanceBatch::class,
            parentColumns = ["id"],
            childColumns = ["batchId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["batchId"]),
        Index(value = ["accountId"])
    ]
)
data class OpeningBalanceDetail(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val batchId: Long,
    val accountId: Long,
    val debitAmount: Double = 0.0,
    val creditAmount: Double = 0.0,
    val notes: String = ""
)
