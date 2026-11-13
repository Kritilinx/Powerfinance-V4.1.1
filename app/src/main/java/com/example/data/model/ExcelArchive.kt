package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "excel_archives")
data class ExcelArchive(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateEpochMs: Long,
    val fileName: String,
    val title: String,
    val notes: String = "",
    val totalRows: Int,
    val totalDebit: Double,
    val totalCredit: Double,
    val rawContent: String, // Read-only archival content that cannot be tampered with
    val isMigrated: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
