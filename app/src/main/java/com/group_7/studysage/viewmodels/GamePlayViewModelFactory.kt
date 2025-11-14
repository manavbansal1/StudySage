package com.group_7.studysage.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.group_7.studysage.data.websocket.GameWebSocketManager

class GamePlayViewModelFactory(
    private val gameWebSocketManager: GameWebSocketManager,
    private val authViewModel: AuthViewModel
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GamePlayViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GamePlayViewModel(gameWebSocketManager, authViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
