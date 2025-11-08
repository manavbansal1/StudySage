package com.group_7.studysage.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {

    private val _showGameActionOverlay = MutableStateFlow(false)
    val showGameActionOverlay: StateFlow<Boolean> = _showGameActionOverlay

    private val _selectedGameTitle = MutableStateFlow("")
    val selectedGameTitle: StateFlow<String> = _selectedGameTitle

    fun setShowGameActionOverlay(show: Boolean) {
        _showGameActionOverlay.value = show
    }

    fun setSelectedGameTitle(title: String) {
        _selectedGameTitle.value = title
    }
}
