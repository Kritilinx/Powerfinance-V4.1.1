package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.JournalEntry
import com.example.data.model.JournalLine
import com.example.data.model.JournalLineDetail
import com.example.data.model.JournalWithLines
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entries ORDER BY dateEpochMs DESC, id DESC")
    fun getAllJournalEntries(): Flow<List<JournalEntry>>

    @Transaction
    @Query("SELECT * FROM journal_entries ORDER BY dateEpochMs DESC, id DESC")
    fun getJournalsWithLines(): Flow<List<JournalWithLines>>

    @Query("SELECT * FROM journal_entries WHERE id = :id LIMIT 1")
    suspend fun getJournalEntryById(id: Long): JournalEntry?

    @Query("SELECT * FROM journal_entries WHERE transactionId = :transactionId LIMIT 1")
    suspend fun getJournalByTransactionId(transactionId: Long): JournalEntry?

    @Query("SELECT * FROM journal_lines WHERE journalId = :journalId ORDER BY id ASC")
    suspend fun getLinesForJournal(journalId: Long): List<JournalLine>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalEntry(entry: JournalEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalLines(lines: List<JournalLine>)

    @Update
    suspend fun updateJournalEntry(entry: JournalEntry)

    @Query("""
        SELECT 
            jl.id AS lineId,
            je.id AS journalId,
            je.entryNumber AS entryNumber,
            je.dateEpochMs AS dateEpochMs,
            je.description AS journalDescription,
            je.isPosted AS isPosted,
            je.isReversed AS isReversed,
            a.id AS accountId,
            a.code AS accountCode,
            a.name AS accountName,
            a.category AS accountCategory,
            jl.debitAmount AS debitAmount,
            jl.creditAmount AS creditAmount,
            jl.memo AS memo
        FROM journal_lines jl
        INNER JOIN journal_entries je ON jl.journalId = je.id
        INNER JOIN accounts a ON jl.accountId = a.id
        WHERE je.isPosted = 1
        ORDER BY je.dateEpochMs ASC, je.id ASC, jl.id ASC
    """)
    fun getAllPostedJournalLineDetails(): Flow<List<JournalLineDetail>>

    @Query("""
        SELECT 
            jl.id AS lineId,
            je.id AS journalId,
            je.entryNumber AS entryNumber,
            je.dateEpochMs AS dateEpochMs,
            je.description AS journalDescription,
            je.isPosted AS isPosted,
            je.isReversed AS isReversed,
            a.id AS accountId,
            a.code AS accountCode,
            a.name AS accountName,
            a.category AS accountCategory,
            jl.debitAmount AS debitAmount,
            jl.creditAmount AS creditAmount,
            jl.memo AS memo
        FROM journal_lines jl
        INNER JOIN journal_entries je ON jl.journalId = je.id
        INNER JOIN accounts a ON jl.accountId = a.id
        WHERE je.isPosted = 1 AND a.id = :accountId
        ORDER BY je.dateEpochMs ASC, je.id ASC, jl.id ASC
    """)
    fun getPostedJournalLineDetailsForAccount(accountId: Long): Flow<List<JournalLineDetail>>

    @Query("SELECT COUNT(*) FROM journal_entries")
    suspend fun countJournalEntries(): Int

    @Query("SELECT MAX(id) FROM journal_entries")
    suspend fun getMaxJournalId(): Long?

    @Query("SELECT * FROM journal_entries ORDER BY id ASC")
    suspend fun getAllJournalEntriesList(): List<JournalEntry>

    @Query("SELECT * FROM journal_lines ORDER BY id ASC")
    suspend fun getAllJournalLinesList(): List<JournalLine>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalEntries(entries: List<JournalEntry>)
}
