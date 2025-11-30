package com.group_7.studysage.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for CreateGroupDialog to handle rotation and state persistence
 */
data class AddGroupUiState(
    val groupName: String = "",
    val groupDescription: String = ""
)

class AddGroupViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(AddGroupUiState())
    val uiState: StateFlow<AddGroupUiState> = _uiState.asStateFlow()
    
    fun setGroupName(name: String) {
        _uiState.value = _uiState.value.copy(groupName = name)
    }
    
    fun setGroupDescription(description: String) {
        _uiState.value = _uiState.value.copy(groupDescription = description)
    }
    
    fun clearState() {
        _uiState.value = AddGroupUiState()
    }
}
