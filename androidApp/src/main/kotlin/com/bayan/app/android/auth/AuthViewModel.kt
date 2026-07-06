package com.bayan.app.android.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bayan.app.domain.model.AuthState
import com.bayan.app.domain.repository.AuthRepository
import com.bayan.app.domain.repository.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    val authState: StateFlow<AuthState> = repository.authState

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.restoreSession()
        }
    }

    fun signIn(email: String, password: String) {
        if (_isSubmitting.value) return
        _errorMessage.value = null
        _isSubmitting.value = true
        viewModelScope.launch {
            when (val result = repository.signIn(email.trim(), password)) {
                is AuthResult.Success -> Unit
                is AuthResult.Error -> _errorMessage.value = result.message
            }
            _isSubmitting.value = false
        }
    }

    fun signUp(email: String, password: String) {
        if (_isSubmitting.value) return
        _errorMessage.value = null
        _isSubmitting.value = true
        viewModelScope.launch {
            when (val result = repository.signUp(email.trim(), password)) {
                is AuthResult.Success -> Unit
                is AuthResult.Error -> _errorMessage.value = result.message
            }
            _isSubmitting.value = false
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
