package com.tecsup.nexusmobile.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.nexusmobile.data.repository.AuthRepositoryImpl
import com.tecsup.nexusmobile.domain.model.User
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val repository = AuthRepositoryImpl()

    var authState by mutableStateOf<AuthState>(AuthState.Idle)
        private set

    var isUserLoggedIn by mutableStateOf(false)
        private set

    init {
        checkUserLoggedIn()
    }

    private fun checkUserLoggedIn() {
        isUserLoggedIn = repository.isUserLoggedIn()
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            authState = AuthState.Loading

            repository.login(email, password)
                .onSuccess { user ->
                    authState = AuthState.Success(user)
                    isUserLoggedIn = true
                }
                .onFailure { error ->
                    authState = AuthState.Error(
                        error.message ?: "Error al iniciar sesión"
                    )
                }
        }
    }

    fun register(email: String, password: String, username: String, fullName: String) {
        viewModelScope.launch {
            authState = AuthState.Loading

            repository.register(email, password, username, fullName)
                .onSuccess { user ->
                    authState = AuthState.Success(user)
                    isUserLoggedIn = true
                }
                .onFailure { error ->
                    authState = AuthState.Error(
                        error.message ?: "Error al registrarse"
                    )
                }
        }
    }

    fun logout() {
        repository.logout()
        isUserLoggedIn = false
        authState = AuthState.Idle
    }

    fun resetAuthState() {
        authState = AuthState.Idle
    }
}