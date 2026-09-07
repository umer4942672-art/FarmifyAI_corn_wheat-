package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM user_profiles WHERE isActiveSession = 1 LIMIT 1")
    fun getActiveUserProfile(): Flow<UserEntity?>

    @Query("SELECT * FROM user_profiles WHERE isActiveSession = 1 LIMIT 1")
    suspend fun getActiveUserDirect(): UserEntity?

    @Query("SELECT * FROM user_profiles WHERE phoneOrEmail = :key LIMIT 1")
    suspend fun getUserByPhoneOrEmail(key: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUser(user: UserEntity)

    @Query("UPDATE user_profiles SET isActiveSession = 0")
    suspend fun clearActiveSessions()

    @Query("UPDATE user_profiles SET isActiveSession = 1 WHERE phoneOrEmail = :key")
    suspend fun setActiveSession(key: String)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("SELECT COUNT(*) FROM user_profiles")
    suspend fun getUserCount(): Int
}
