package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AccountCategory(val displayName: String) {
    ASSET("Aset"),
    LIABILITY("Kewajiban"),
    EQUITY("Ekuitas"),
    REVENUE("Pendapatan"),
    COGS("HPP dan Pembelian"),
    EXPENSE("Beban")
}

enum class NormalBalance(val displayName: String) {
    DEBIT("Debit"),
    CREDIT("Kredit")
}

@Entity(
    tableName = "accounts",
    indices = [Index(value = ["code"], unique = true)]
)
data class Account(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val code: String,
    val name: String,
    val category: AccountCategory,
    val subcategory: String = "",
    val normalBalance: NormalBalance,
    val isActive: Boolean = true,
    val isSystem: Boolean = false,
    val description: String = ""
)
