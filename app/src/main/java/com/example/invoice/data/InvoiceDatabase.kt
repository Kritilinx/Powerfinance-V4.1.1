package com.example.invoice.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.invoice.data.dao.InvoiceDao
import com.example.invoice.data.model.InvoiceItemRecord
import com.example.invoice.data.model.InvoiceRecord

/**
 * Isolated Room Database specifically for the Invoice Feature:
 * Name: invoice_feature.db
 *
 * NOTE: Strictly isolated from diprint_accounting.db and receipt_feature.db.
 */
@Database(
    entities = [
        InvoiceRecord::class,
        InvoiceItemRecord::class
    ],
    version = 1,
    exportSchema = false
)
abstract class InvoiceDatabase : RoomDatabase() {

    abstract fun invoiceDao(): InvoiceDao

    companion object {
        @Volatile
        private var INSTANCE: InvoiceDatabase? = null

        fun getDatabase(context: Context): InvoiceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    InvoiceDatabase::class.java,
                    "invoice_feature.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
