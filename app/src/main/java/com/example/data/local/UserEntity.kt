package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserEntity(
    @PrimaryKey
    val phoneOrEmail: String,
    val fullName: String,
    val phone: String,
    val email: String,
    val passwordHash: String,
    val supabaseUserId: String = "",
    /** Absolute path to the farmer's own profile photo in app storage. Empty = use the drawn avatar. */
    val profilePhotoPath: String = "",
    val farmName: String,
    val district: String,
    val province: String,
    val farmLocation: String,
    val totalAcres: Double,
    val primaryCropsString: String, // Comma separated list of crops
    val isActiveSession: Boolean = true,
    val weatherNotifications: Boolean = true,
    val mandiNotifications: Boolean = true,
    val diseaseAlerts: Boolean = true,
    val khataReminders: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
