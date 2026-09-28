package com.example.game.data

import kotlinx.coroutines.flow.Flow

class GameRepository(private val dao: GameDao) {

    val topRecords: Flow<List<GameRecord>> = dao.getTopRecords()
    val recentRecords: Flow<List<GameRecord>> = dao.getRecentRecords()
    val overallHighScore: Flow<Int?> = dao.getOverallHighScore()
    val totalGamesCount: Flow<Int> = dao.getTotalGamesCount()
    val totalScoreSum: Flow<Int?> = dao.getTotalScoreSum()

    fun getHighScoreForDifficulty(difficulty: String): Flow<Int?> =
        dao.getHighScoreByDifficulty(difficulty)

    suspend fun recordGame(record: GameRecord): Long =
        dao.insertRecord(record)

    suspend fun clearHistory() =
        dao.clearAllRecords()
}
