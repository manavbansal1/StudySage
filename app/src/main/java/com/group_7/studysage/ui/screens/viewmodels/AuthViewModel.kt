package com.group_7.studysage.ui.viewmodels

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group_7.studysage.data.repository.AuthRepository
import kotlinx.coroutines.launch
import androidx.compose.runtime.State

/**
 * sign o2ut functioality not implemented yet
 */
class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    private val _isSignedIn = mutableStateOf(authRepository.isUserSignedIn())
    val isSignedIn: State<Boolean> = _isSignedIn



    init {
        if (_isSignedIn.value) {
            // need a fxn here to load the users profile and get the notres, quiz, etc
        }
    }

    fun signUp(email: String, password: String, name: String) {
        if (email.isBlank() || password.isBlank() || name.isBlank()) {
            _errorMessage.value = "All fields are required"
            return
        }

        if (password.length < 6) {
            _errorMessage.value = "Password must be at least 6 characters"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            authRepository.signUp(email, password, name)
                .onSuccess {
                    _isSignedIn.value = true
                    // need a fxn here to load the users profile and get the notres, quiz, etc
                }
                .onFailure { exception ->
                    _errorMessage.value = exception.message ?: "Sign up failed"
                }

            _isLoading.value = false
        }
    }

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _errorMessage.value = "Email and password are required"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            authRepository.signIn(email, password)
                .onSuccess {
                    _isSignedIn.value = true
                    // need a fxn here to load the users profile and get the notres, quiz, etc
                }
                .onFailure { exception ->
                    _errorMessage.value = exception.message ?: "Sign in failed"
                }

            _isLoading.value = false
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun loadUserProfile() {
        //to:do
    }
}