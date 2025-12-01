package com.group_7.studysage.viewmodels

import android.util.Log
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

    private val _currentUser = MutableStateFlow<FirebaseUser?>(authRepository.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser

    // Flag to track when profile data has been updated
    private val _profileUpdated = MutableStateFlow(0)
    val profileUpdated: StateFlow<Int> = _profileUpdated

    // Sign In form fields - preserved across rotation
    private val _signInEmail = MutableStateFlow("")
    val signInEmail: StateFlow<String> = _signInEmail

    private val _signInPassword = MutableStateFlow("")
    val signInPassword: StateFlow<String> = _signInPassword

    private val _isSignInPasswordVisible = MutableStateFlow(false)
    val isSignInPasswordVisible: StateFlow<Boolean> = _isSignInPasswordVisible

    // Sign Up form fields - preserved across rotation
    private val _signUpName = MutableStateFlow("")
    val signUpName: StateFlow<String> = _signUpName

    private val _signUpEmail = MutableStateFlow("")
    val signUpEmail: StateFlow<String> = _signUpEmail

    private val _signUpPassword = MutableStateFlow("")
    val signUpPassword: StateFlow<String> = _signUpPassword

    private val _signUpConfirmPassword = MutableStateFlow("")
    val signUpConfirmPassword: StateFlow<String> = _signUpConfirmPassword

    private val _isSignUpPasswordVisible = MutableStateFlow(false)
    val isSignUpPasswordVisible: StateFlow<Boolean> = _isSignUpPasswordVisible

    private val _isSignUpConfirmPasswordVisible = MutableStateFlow(false)
    val isSignUpConfirmPasswordVisible: StateFlow<Boolean> = _isSignUpConfirmPasswordVisible

    init {
        if (_isSignedIn.value) {
            loadUserProfile()
            _currentUser.value = authRepository.currentUser
        }
    }

    // Form field update functions
    fun setSignInEmail(email: String) {
        _signInEmail.value = email
    }

    fun setSignInPassword(password: String) {
        _signInPassword.value = password
    }

    fun toggleSignInPasswordVisibility() {
        _isSignInPasswordVisible.value = !_isSignInPasswordVisible.value
    }

    fun setSignUpName(name: String) {
        _signUpName.value = name
    }

    fun setSignUpEmail(email: String) {
        _signUpEmail.value = email
    }

    fun setSignUpPassword(password: String) {
        _signUpPassword.value = password
    }

    fun setSignUpConfirmPassword(password: String) {
        _signUpConfirmPassword.value = password
    }

    fun toggleSignUpPasswordVisibility() {
        _isSignUpPasswordVisible.value = !_isSignUpPasswordVisible.value
    }

    fun toggleSignUpConfirmPasswordVisibility() {
        _isSignUpConfirmPasswordVisible.value = !_isSignUpConfirmPasswordVisible.value
    }

    fun clearSignInForm() {
        _signInEmail.value = ""
        _signInPassword.value = ""
        _isSignInPasswordVisible.value = false
    }

    fun clearSignUpForm() {
        _signUpName.value = ""
        _signUpEmail.value = ""
        _signUpPassword.value = ""
        _signUpConfirmPassword.value = ""
        _isSignUpPasswordVisible.value = false
        _isSignUpConfirmPasswordVisible.value = false
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
                    _currentUser.value = authRepository.currentUser
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
                    _currentUser.value = authRepository.currentUser
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

    /**
     * Call this method when profile data (like profile picture) is updated
     * This will trigger a refresh on HomeScreen
     */
    fun notifyProfileUpdated() {
        _profileUpdated.value += 1
    }
}