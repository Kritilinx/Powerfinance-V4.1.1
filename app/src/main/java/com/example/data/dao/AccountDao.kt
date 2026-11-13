package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Account
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY code ASC")
    fun getAllAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE isActive = 1 ORDER BY code ASC")
    fun getActiveAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: Long): Account?

    @Query("SELECT * FROM accounts WHERE code = :code LIMIT 1")
    suspend fun getAccountByCode(code: String): Account?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAccounts(accounts: List<Account>): List<Long>

    @Update
    suspend fun updateAccount(account: Account)

    @Delete
    suspend fun deleteAccount(account: Account)

    @Query("SELECT * FROM accounts WHERE code LIKE '%' || :query || '%' OR name LIKE '%' || :query || '%' ORDER BY code ASC")
    fun searchAccounts(query: String): Flow<List<Account>>

    @Query("SELECT COUNT(*) FROM journal_lines WHERE accountId = :accountId")
    suspend fun getJournalLinesCountForAccount(accountId: Long): Int

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun countAccounts(): Int

    @Query("SELECT * FROM accounts ORDER BY code ASC")
    suspend fun getAllAccountsList(): List<Account>
}
