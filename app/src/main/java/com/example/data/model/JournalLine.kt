package com.example.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "journal_lines",
    foreignKeys = [
        ForeignKey(
            entity = JournalEntry::class,
            parentColumns = ["id"],
            childColumns = ["journalId"],
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
        Index(value = ["journalId"]),
        Index(value = ["accountId"])
    ]
)
data class JournalLine(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val journalId: Long,
    val accountId: Long,
    val debitAmount: Double = 0.0,
    val creditAmount: Double = 0.0,
    val memo: String = ""
)

data class JournalWithLines(
    @Embedded val entry: JournalEntry,
    @Relation(
        parentColumn = "id",
        entityColumn = "journalId"
    )
    val lines: List<JournalLine>
)

data class JournalLineDetail(
    val lineId: Long,
    val journalId: Long,
    val entryNumber: String,
    val dateEpochMs: Long,
    val journalDescription: String,
    val isPosted: Boolean,
    val isReversed: Boolean,
    val accountId: Long,
    val accountCode: String,
    val accountName: String,
    val accountCategory: AccountCategory,
    val debitAmount: Double,
    val creditAmount: Double,
    val memo: String
)
