package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface KhataDao {
    @Query("SELECT * FROM khata_entries WHERE userId = :userId ORDER BY timestamp DESC")
    fun getEntriesForUser(userId: String): Flow<List<KhataEntryEntity>>

    @Query("SELECT * FROM khata_entries WHERE userId = :userId AND entryType = :type ORDER BY timestamp DESC")
    fun getEntriesByUserAndType(userId: String, type: String): Flow<List<KhataEntryEntity>>

    @Query("SELECT * FROM khata_entries WHERE userId = :userId AND cropName = :cropName ORDER BY timestamp DESC")
    fun getEntriesByUserAndCrop(userId: String, cropName: String): Flow<List<KhataEntryEntity>>

    @Query("SELECT * FROM khata_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<KhataEntryEntity>>

    @Query("SELECT * FROM khata_entries WHERE entryType = :type ORDER BY timestamp DESC")
    fun getEntriesByType(type: String): Flow<List<KhataEntryEntity>>

    @Query("SELECT * FROM khata_entries WHERE cropName = :cropName ORDER BY timestamp DESC")
    fun getEntriesByCrop(cropName: String): Flow<List<KhataEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: KhataEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<KhataEntryEntity>)

    @Update
    suspend fun updateEntry(entry: KhataEntryEntity)

    @Delete
    suspend fun deleteEntry(entry: KhataEntryEntity)

    @Query("UPDATE khata_entries SET isSynced = :synced WHERE id = :id")
    suspend fun setSynced(id: Long, synced: Boolean)

    /** Rows that never reached Supabase, oldest first, for the background retry pass. */
    @Query("SELECT * FROM khata_entries WHERE isSynced = 0 ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getUnsyncedEntries(limit: Int): List<KhataEntryEntity>

    @Query("SELECT COUNT(*) FROM khata_entries WHERE isSynced = 0")
    suspend fun getUnsyncedCount(): Int

    /** Used when restoring from the cloud so an existing local row is not duplicated. */
    @Query(
        """
        SELECT COUNT(*) FROM khata_entries
        WHERE userId = :userId
          AND date = :date
          AND entryType = :entryType
          AND cropName = :cropName
          AND totalAmount = :totalAmount
        """
    )
    suspend fun countMatching(
        userId: String,
        date: String,
        entryType: String,
        cropName: String,
        totalAmount: Double
    ): Int

    @Query("DELETE FROM khata_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM khata_entries WHERE userId = :userId")
    suspend fun deleteByUserId(userId: String)

    @Query("SELECT COUNT(*) FROM khata_entries WHERE userId = :userId")
    suspend fun getCountForUser(userId: String): Int

    @Query("SELECT COUNT(*) FROM khata_entries")
    suspend fun getCount(): Int
}
