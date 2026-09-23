package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingProofDao {
    @Insert
    suspend fun insert(proof: PendingProofEntity): Long

    @Query("SELECT * FROM pending_proofs ORDER BY createdAt ASC")
    suspend fun getAll(): List<PendingProofEntity>

    @Query("SELECT COUNT(*) FROM pending_proofs WHERE workOrderId = :workOrderId")
    fun countForOrder(workOrderId: String): Flow<Int>

    @Query("DELETE FROM pending_proofs WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE pending_proofs SET attempts = attempts + 1 WHERE id = :id")
    suspend fun markAttempt(id: Long)
}
