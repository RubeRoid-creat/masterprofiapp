package com.example.bestapp.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fullName: String,
    val email: String,
    val phone: String,
    val passwordHash: String,
    val specialization: String
)
