package com.group_7.studysage.data.models

data class Player(
    val id: String = "",
    val name: String = "",
    val score: Int = 0,
    val isHost: Boolean = false,
    val isReady: Boolean = false,
    val isActive: Boolean = true
)
