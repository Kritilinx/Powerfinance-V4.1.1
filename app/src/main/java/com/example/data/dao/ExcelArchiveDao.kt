package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ExcelArchive
import kotlinx.coroutines.flow.Flow

@Dao
interface ExcelArchiveDao {
    @Query("SELECT * FROM excel_archives ORDER BY dateEpochMs DESC, id DESC")
    fun getAllArchives(): Flow<List<ExcelArchive>>

    @Query("SELECT * FROM excel_archives WHERE id = :id LIMIT 1")
    suspend fun getArchiveById(id: Long): ExcelArchive?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArchive(archive: ExcelArchive): Long

    @Query("SELECT COUNT(*) FROM excel_archives")
    suspend fun countArchives(): Int

    @Query("SELECT * FROM excel_archives ORDER BY id ASC")
    suspend fun getAllArchivesList(): List<ExcelArchive>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArchives(archives: List<ExcelArchive>)
}
