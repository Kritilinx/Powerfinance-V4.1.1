package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "journal_entries",
    foreignKeys = [
        ForeignKey(
            entity = TransactionRecord::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["entryNumber"], unique = true),
        Index(value = ["transactionId"]),
        Index(value = ["dateEpochMs"])
    ]
)
data class JournalEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionId: Long? = null,
    val entryNumber: String,
    val dateEpochMs: Long,
    val description: String,
    val isPosted: Boolean = true,
    val isReversed: Boolean = false,
    val reversalOfEntryId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
