package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.AccountDao
import com.example.data.dao.ExcelArchiveDao
import com.example.data.dao.InventoryDao
import com.example.data.dao.JournalDao
import com.example.data.dao.OpeningBalanceDao
import com.example.data.dao.TransactionDao
import com.example.data.model.Account
import com.example.data.model.AccountCategory
import com.example.data.model.ExcelArchive
import com.example.data.model.InventoryLog
import com.example.data.model.JournalEntry
import com.example.data.model.JournalLine
import com.example.data.model.NormalBalance
import com.example.data.model.OpeningBalanceBatch
import com.example.data.model.OpeningBalanceDetail
import com.example.data.model.TransactionRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Account::class,
        TransactionRecord::class,
        JournalEntry::class,
        JournalLine::class,
        InventoryLog::class,
        ExcelArchive::class,
        OpeningBalanceBatch::class,
        OpeningBalanceDetail::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun journalDao(): JournalDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun excelArchiveDao(): ExcelArchiveDao
    abstract fun openingBalanceDao(): OpeningBalanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "diprint_accounting.db"
                )
                    .addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialAccounts(database)
                    }
                }
            }
        }

        suspend fun populateInitialAccounts(db: AppDatabase) {
            val accountDao = db.accountDao()
            if (accountDao.countAccounts() > 0) return

            val defaultAccounts = listOf(
                // ASET
                Account(code = "1001", name = "Kas", category = AccountCategory.ASSET, subcategory = "Kas & Setara Kas", normalBalance = NormalBalance.DEBIT, isSystem = true, description = "Kas fisik di tangan"),
                Account(code = "1002", name = "Bank BCA", category = AccountCategory.ASSET, subcategory = "Bank", normalBalance = NormalBalance.DEBIT, isSystem = true, description = "Rekening utama Bank BCA"),
                Account(code = "1101", name = "Piutang Usaha", category = AccountCategory.ASSET, subcategory = "Piutang Lancar", normalBalance = NormalBalance.DEBIT, isSystem = false, description = "Tagihan piutang pelanggan"),
                Account(code = "1201", name = "Persediaan", category = AccountCategory.ASSET, subcategory = "Persediaan Barang", normalBalance = NormalBalance.DEBIT, isSystem = true, description = "Nilai total persediaan barang"),

                // KEWAJIBAN
                Account(code = "2001", name = "Hutang Usaha", category = AccountCategory.LIABILITY, subcategory = "Hutang Lancar", normalBalance = NormalBalance.CREDIT, isSystem = true, description = "Kewajiban hutang usaha"),

                // EKUITAS
                Account(code = "3001", name = "Modal Pemilik", category = AccountCategory.EQUITY, subcategory = "Modal Disetor", normalBalance = NormalBalance.CREDIT, isSystem = true, description = "Setoran modal pemilik"),
                Account(code = "3002", name = "Prive Pemilik", category = AccountCategory.EQUITY, subcategory = "Penarikan Modal", normalBalance = NormalBalance.DEBIT, isSystem = true, description = "Penarikan modal pribadi (kontra-ekuitas)"),
                Account(code = "3003", name = "Charity Pemilik", category = AccountCategory.EQUITY, subcategory = "Donasi / Zakat", normalBalance = NormalBalance.DEBIT, isSystem = true, description = "Donasi/charity pemilik (kontra-ekuitas)"),
                Account(code = "3101", name = "Laba Ditahan", category = AccountCategory.EQUITY, subcategory = "Laba Akumulasi", normalBalance = NormalBalance.CREDIT, isSystem = true, description = "Akumulasi laba periode sebelumnya"),
                Account(code = "3999", name = "Opening Balance Equity", category = AccountCategory.EQUITY, subcategory = "Penyeimbang Saldo Awal", normalBalance = NormalBalance.CREDIT, isSystem = true, description = "Akun penyeimbang khusus jurnal saldo awal"),

                // PENDAPATAN
                Account(code = "4001", name = "Penjualan", category = AccountCategory.REVENUE, subcategory = "Pendapatan Usaha", normalBalance = NormalBalance.CREDIT, isSystem = true, description = "Pendapatan penjualan percetakan"),
                Account(code = "4002", name = "Pendapatan Lain-lain", category = AccountCategory.REVENUE, subcategory = "Pendapatan Non-Operasional", normalBalance = NormalBalance.CREDIT, isSystem = false, description = "Pendapatan non-operasional"),

                // HPP DAN PEMBELIAN
                Account(code = "5001", name = "Harga Pokok Penjualan", category = AccountCategory.COGS, subcategory = "Beban Pokok", normalBalance = NormalBalance.DEBIT, isSystem = true, description = "Beban pemakaian persediaan cetak langsung"),
                Account(code = "5101", name = "Pembelian Persediaan", category = AccountCategory.COGS, subcategory = "Pembelian", normalBalance = NormalBalance.DEBIT, isSystem = true, description = "Pembelian bahan persediaan cetak"),

                // BEBAN
                Account(code = "6001", name = "Ongkos Kirim", category = AccountCategory.EXPENSE, subcategory = "Beban Distribusi", normalBalance = NormalBalance.DEBIT, isSystem = false, description = "Biaya kurir dan pengiriman barang"),
                Account(code = "6002", name = "Biaya Administrasi Bank", category = AccountCategory.EXPENSE, subcategory = "Beban Administrasi", normalBalance = NormalBalance.DEBIT, isSystem = false, description = "Biaya admin transfer & bulanan bank"),
                Account(code = "6003", name = "Biaya Operasional Lainnya", category = AccountCategory.EXPENSE, subcategory = "Beban Umum", normalBalance = NormalBalance.DEBIT, isSystem = true, description = "Beban operasional harian lainnya")
            )
            accountDao.insertAccounts(defaultAccounts)
        }
    }
}
