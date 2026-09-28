package com.example.game.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {

    @Query("SELECT * FROM game_records ORDER BY score DESC, timestamp DESC LIMIT 20")
    fun getTopRecords(): Flow<List<GameRecord>>

    @Query("SELECT * FROM game_records ORDER BY timestamp DESC LIMIT 50")
    fun getRecentRecords(): Flow<List<GameRecord>>

    @Query("SELECT MAX(score) FROM game_records WHERE difficulty = :difficulty")
    fun getHighScoreByDifficulty(difficulty: String): Flow<Int?>

    @Query("SELECT MAX(score) FROM game_records")
    fun getOverallHighScore(): Flow<Int?>

    @Query("SELECT COUNT(*) FROM game_records")
    fun getTotalGamesCount(): Flow<Int>

    @Query("SELECT SUM(score) FROM game_records")
    fun getTotalScoreSum(): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: GameRecord): Long

    @Query("DELETE FROM game_records")
    suspend fun clearAllRecords()
}
