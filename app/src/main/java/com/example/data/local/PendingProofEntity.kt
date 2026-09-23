package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Evidence captured in the field while there was no signal.
 *
 * The photo, its GPS fix and its device timestamp are stored together at the
 * moment of capture, so the location and time recorded are the ones from the
 * field rather than wherever the phone happens to be when it reconnects.
 */
@Entity(tableName = "pending_proofs")
data class PendingProofEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workOrderId: String,
    val stage: String,
    val imagePath: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyM: Float?,
    val capturedAtIso: String,
    val isMockLocation: Boolean,
    val attempts: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
