package com.example.receipt.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.receipt.data.dao.ReceiptProductDao
import com.example.receipt.data.dao.ReceiptRecordDao
import com.example.receipt.data.model.ReceiptItemSnapshot
import com.example.receipt.data.model.ReceiptProduct
import com.example.receipt.data.model.ReceiptRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Isolated Room Database specifically for the Receipt Feature:
 * Name: receipt_feature.db
 *
 * NOTE: Strictly isolated from diprint_accounting.db.
 */
@Database(
    entities = [
        ReceiptProduct::class,
        ReceiptRecord::class,
        ReceiptItemSnapshot::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ReceiptDatabase : RoomDatabase() {

    abstract fun receiptProductDao(): ReceiptProductDao
    abstract fun receiptRecordDao(): ReceiptRecordDao

    companion object {
        @Volatile
        private var INSTANCE: ReceiptDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): ReceiptDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ReceiptDatabase::class.java,
                    "receipt_feature.db"
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
                        populateInitialProducts(database)
                    }
                }
            }
        }

        suspend fun populateInitialProducts(db: ReceiptDatabase) {
            val dao = db.receiptProductDao()
            if (dao.countProducts() > 0) return

            val defaultProducts = listOf(
                ReceiptProduct(sku = "BNR-280", name = "Banner Flexi Frontlite 280gsm", unitPrice = 25000.0),
                ReceiptProduct(sku = "BNR-380", name = "Banner Flexi Korea 380gsm High-Res", unitPrice = 45000.0),
                ReceiptProduct(sku = "X-BNR-60", name = "X-Banner 60x160cm Lengkap Rangka", unitPrice = 65000.0),
                ReceiptProduct(sku = "ROLL-UP-80", name = "Roll Up Banner 80x200cm Aluminium", unitPrice = 185000.0),
                ReceiptProduct(sku = "STK-VINYL", name = "Stiker Vinyl A3+ Glossy / Matte + Kiss Cut", unitPrice = 18000.0),
                ReceiptProduct(sku = "STK-CHRMO", name = "Stiker Chromo A3+ Cetak Warna + Potong", unitPrice = 14000.0),
                ReceiptProduct(sku = "BRS-A4-150", name = "Brosur A4 Art Paper 150gsm (Full Colour)", unitPrice = 3500.0),
                ReceiptProduct(sku = "KRT-NAMA", name = "Kartu Nama 2 Muka + Laminasi Doff (Box)", unitPrice = 45000.0),
                ReceiptProduct(sku = "ID-PVC", name = "ID Card PVC Tebal 0.76mm Instan", unitPrice = 12000.0),
                ReceiptProduct(sku = "JLD-SPIRAL", name = "Jilid Spiral Kawat + Mika Bening", unitPrice = 15000.0),
                ReceiptProduct(sku = "PLT-CETAK", name = "Plat / Master Cetak Offset GTO", unitPrice = 60000.0),
                ReceiptProduct(sku = "POSTER-A3", name = "Poster A3+ Art Carton 260gsm", unitPrice = 10000.0)
            )
            dao.insertProducts(defaultProducts)
        }
    }
}
