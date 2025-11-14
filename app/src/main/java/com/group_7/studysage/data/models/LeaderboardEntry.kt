package com.group_7.studysage.data.models

data class LeaderboardEntry(
    val playerId: String = "",
    val playerName: String = "",
    val score: Int = 0,
    val rank: Int = 0
)
