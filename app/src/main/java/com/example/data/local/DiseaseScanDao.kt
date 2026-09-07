package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DiseaseScanDao {
    @Query("SELECT * FROM disease_scans ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<DiseaseScanEntity>>

    @Query("SELECT * FROM disease_scans WHERE userId = :userId ORDER BY timestamp DESC")
    fun getScansForUser(userId: String): Flow<List<DiseaseScanEntity>>

    @Query("SELECT * FROM disease_scans WHERE id = :id LIMIT 1")
    suspend fun getScanById(id: Long): DiseaseScanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: DiseaseScanEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(scans: List<DiseaseScanEntity>)

    @Delete
    suspend fun deleteScan(scan: DiseaseScanEntity)

    @Query("DELETE FROM disease_scans WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM disease_scans")
    suspend fun getCount(): Int

    /** Recent scans for a user, used by the retry pass to re-push unsynced history. */
    @Query("SELECT * FROM disease_scans WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentScansForUser(userId: String, limit: Int): List<DiseaseScanEntity>

    /** Used when restoring from the cloud so an existing local scan is not duplicated. */
    @Query(
        """
        SELECT COUNT(*) FROM disease_scans
        WHERE userId = :userId
          AND cropName = :cropName
          AND diseaseNameEn = :diseaseNameEn
          AND confidencePercent = :confidencePercent
        """
    )
    suspend fun countMatching(
        userId: String,
        cropName: String,
        diseaseNameEn: String,
        confidencePercent: Int
    ): Int
}
