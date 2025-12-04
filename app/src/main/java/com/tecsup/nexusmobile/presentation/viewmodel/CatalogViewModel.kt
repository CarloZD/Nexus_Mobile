package com.tecsup.nexusmobile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.tecsup.nexusmobile.data.repository.GameRepositoryImpl
import com.tecsup.nexusmobile.domain.model.Game
import com.tecsup.nexusmobile.domain.model.Post
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class CatalogUiState {
    object Loading : CatalogUiState()
    data class Success(val games: List<Game>) : CatalogUiState()
    data class Error(val message: String) : CatalogUiState()
}

class CatalogViewModel : ViewModel() {
    private val repository = GameRepositoryImpl()
    private val firestore = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow<CatalogUiState>(CatalogUiState.Loading)
    val uiState: StateFlow<CatalogUiState> = _uiState

    private val _randomPost = MutableStateFlow<Post?>(null)
    val randomPost: StateFlow<Post?> = _randomPost

    private var allPosts = listOf<Post>()

    init {
        loadGames()
        loadCommunityPosts()
    }

    fun loadGames() {
        viewModelScope.launch {
            _uiState.value = CatalogUiState.Loading
            repository.getAllGames()
                .onSuccess { games ->
                    _uiState.value = CatalogUiState.Success(games)
                }
                .onFailure { error ->
                    _uiState.value = CatalogUiState.Error(
                        error.message ?: "Error desconocido"
                    )
                }
        }
    }

    private fun loadCommunityPosts() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("posts")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(20) // Obtener los últimos 20 posts
                    .get()
                    .await()

                allPosts = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Post::class.java)?.copy(id = doc.id)
                }

                // Seleccionar un post aleatorio
                if (allPosts.isNotEmpty()) {
                    _randomPost.value = allPosts.random()
                }
            } catch (e: Exception) {
                android.util.Log.e("CatalogViewModel", "Error al cargar posts: ${e.message}")
                // No hacemos nada si falla, simplemente no mostramos post
            }
        }
    }

    fun refreshRandomPost() {
        if (allPosts.isNotEmpty()) {
            _randomPost.value = allPosts.random()
        } else {
            // Si no hay posts cargados, intentar cargarlos de nuevo
            loadCommunityPosts()
        }
    }
}