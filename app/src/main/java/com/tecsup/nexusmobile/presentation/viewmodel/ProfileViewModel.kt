package com.tecsup.nexusmobile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.nexusmobile.data.repository.ProfileRepositoryImpl
import com.tecsup.nexusmobile.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(val user: User) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

sealed class UpdateProfileUiState {
    object Idle : UpdateProfileUiState()
    object Loading : UpdateProfileUiState()
    data class Success(val user: User) : UpdateProfileUiState()
    data class Error(val message: String) : UpdateProfileUiState()
}

class ProfileViewModel(
    private val repository: ProfileRepositoryImpl = ProfileRepositoryImpl()
) : ViewModel() {
    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState

    private val _updateState = MutableStateFlow<UpdateProfileUiState>(UpdateProfileUiState.Idle)
    val updateState: StateFlow<UpdateProfileUiState> = _updateState

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            repository.getCurrentUser()
                .onSuccess { user ->
                    if (user != null) {
                        _uiState.value = ProfileUiState.Success(user)
                    } else {
                        _uiState.value = ProfileUiState.Error("Usuario no encontrado")
                    }
                }
                .onFailure { error ->
                    _uiState.value = ProfileUiState.Error(
                        error.message ?: "Error al cargar el perfil"
                    )
                }
        }
    }

    fun updateProfile(
        userId: String,
        username: String,
        fullName: String,
        avatarUrl: String? = null
    ) {
        viewModelScope.launch {
            _updateState.value = UpdateProfileUiState.Loading
            repository.updateProfile(userId, username, fullName, avatarUrl)
                .onSuccess { updatedUser ->
                    _updateState.value = UpdateProfileUiState.Success(updatedUser)
                    _uiState.value = ProfileUiState.Success(updatedUser)
                }
                .onFailure { error ->
                    _updateState.value = UpdateProfileUiState.Error(
                        error.message ?: "Error al actualizar el perfil"
                    )
                }
        }
    }

    fun resetUpdateState() {
        _updateState.value = UpdateProfileUiState.Idle
    }
}
