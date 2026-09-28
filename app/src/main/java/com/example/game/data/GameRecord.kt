package com.example.game.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_records")
data class GameRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val score: Int,
    val difficulty: String,
    val birdSkin: String,
    val theme: String,
    val medal: String,
    val timestamp: Long = System.currentTimeMillis()
)
