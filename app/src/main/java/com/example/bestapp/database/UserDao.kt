package com.example.bestapp.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UserDao {
    @Insert
    suspend fun insert(user: User): Long
    
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?
    
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: Long): User?
    
    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE email = :email)")
    suspend fun isEmailExists(email: String): Boolean
}







