package com.example.data.repository

import android.content.Context
import com.example.data.local.KhataDao
import com.example.data.local.KhataEntryEntity
import com.example.data.remote.SupabaseDataSyncService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class KhataRepository(
    private val khataDao: KhataDao,
    context: Context,
    private val supabaseSync: SupabaseDataSyncService = SupabaseDataSyncService(context)
) {
    val allEntries: Flow<List<KhataEntryEntity>> = khataDao.getAllEntries()

    fun getEntriesForUser(userId: String): Flow<List<KhataEntryEntity>> {
        return khataDao.getEntriesForUser(userId)
    }

    fun getEntriesByUserAndType(userId: String, type: String): Flow<List<KhataEntryEntity>> {
        return khataDao.getEntriesByUserAndType(userId, type)
    }

    fun getEntriesByUserAndCrop(userId: String, cropName: String): Flow<List<KhataEntryEntity>> {
        return khataDao.getEntriesByUserAndCrop(userId, cropName)
    }

    suspend fun insertEntry(entry: KhataEntryEntity): Long = withContext(Dispatchers.IO) {
        val id = khataDao.insertEntry(entry)
        val entryWithId = entry.copy(id = id)
        
        try {
            if (supabaseSync.syncKhataTransaction(entryWithId)) khataDao.setSynced(id, true)
        } catch (_: Exception) {
            // Keep local entry safe and marked unsynced for a future retry.
        }

        id
    }

    suspend fun insertAll(entries: List<KhataEntryEntity>) = withContext(Dispatchers.IO) {
        khataDao.insertAll(entries)
        try {
            if (supabaseSync.syncMultipleKhataEntries(entries)) {
                entries.filter { it.id > 0 }.forEach { khataDao.setSynced(it.id, true) }
            }
        } catch (_: Exception) {
            // Keep local safe and marked unsynced.
        }
    }

    suspend fun deleteEntry(id: Long) = withContext(Dispatchers.IO) {
        khataDao.deleteById(id)
    }

    suspend fun deleteUserEntries(userId: String) = withContext(Dispatchers.IO) {
        khataDao.deleteByUserId(userId)
    }

    suspend fun getUserEntryCount(userId: String): Int = withContext(Dispatchers.IO) {
        khataDao.getCountForUser(userId)
    }

    suspend fun initializeSampleDataIfEmpty() = withContext(Dispatchers.IO) {
        // Keeping Khata section strictly clean & empty for new users as requested by user.
    }
}
