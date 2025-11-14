package com.group_7.studysage.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.group_7.studysage.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    val authRepository: AuthRepository
) : ViewModel() {

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    private val _isSignedIn = mutableStateOf(authRepository.isUserSignedIn())
    val isSignedIn: State<Boolean> = _isSignedIn

    private val _userProfile = mutableStateOf<Map<String, Any>?>(null)
    val userProfile: State<Map<String, Any>?> = _userProfile

    private val _currentUser = MutableStateFlow<FirebaseUser?>(authRepository.getCurrentUser())
    val currentUser: StateFlow<FirebaseUser?> = _currentUser

    init {
        if (_isSignedIn.value) {
            loadUserProfile()
            _currentUser.value = authRepository.getCurrentUser()
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
                    loadUserProfile()
                    _currentUser.value = authRepository.getCurrentUser()
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
                    loadUserProfile()
                    _currentUser.value = authRepository.getCurrentUser()
                }
                .onFailure { exception ->
                    _errorMessage.value = exception.message ?: "Sign in failed"
                }

            _isLoading.value = false
        }
    }

    fun signOut() {
        authRepository.signOut()
        _isSignedIn.value = false
        _userProfile.value = null
        _currentUser.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            val profile = authRepository.getUserProfile()
            _userProfile.value = profile
            _isLoading.value = false
        }
    }
}