package com.group_7.studysage.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.group_7.studysage.data.api.GameApiService
import com.group_7.studysage.data.websocket.GameWebSocketManager

class GameViewModelFactory(
    private val gameApiService: GameApiService,
    private val webSocketManager: GameWebSocketManager,
    private val authViewModel: AuthViewModel
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GameViewModel(gameApiService, webSocketManager, authViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class GameLobbyViewModelFactory(
    private val gameApiService: GameApiService,
    private val authViewModel: AuthViewModel
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameLobbyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GameLobbyViewModel(gameApiService, authViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class GamePlayViewModelFactory(
    private val webSocketManager: GameWebSocketManager,
    private val authViewModel: AuthViewModel
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GamePlayViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GamePlayViewModel(webSocketManager, authViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
