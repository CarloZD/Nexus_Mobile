package com.tecsup.nexusmobile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tecsup.nexusmobile.data.repository.CommunityRepositoryImpl
import com.tecsup.nexusmobile.domain.model.Comment
import com.tecsup.nexusmobile.domain.model.Post
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class CommunityUiState {
    object Loading : CommunityUiState()
    data class Success(val posts: List<Post>) : CommunityUiState()
    data class Error(val message: String) : CommunityUiState()
}

sealed class CreatePostUiState {
    object Idle : CreatePostUiState()
    object Loading : CreatePostUiState()
    data class Success(val post: Post) : CreatePostUiState()
    data class Error(val message: String) : CreatePostUiState()
}

class CommunityViewModel(
    private val repository: CommunityRepositoryImpl = CommunityRepositoryImpl()
) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    private val _uiState = MutableStateFlow<CommunityUiState>(CommunityUiState.Loading)
    val uiState: StateFlow<CommunityUiState> = _uiState

    private val _createPostState = MutableStateFlow<CreatePostUiState>(CreatePostUiState.Idle)
    val createPostState: StateFlow<CreatePostUiState> = _createPostState

    private val _comments = MutableStateFlow<Map<String, List<Comment>>>(emptyMap())
    val comments: StateFlow<Map<String, List<Comment>>> = _comments

    init {
        loadPosts()
    }

    fun loadPosts() {
        viewModelScope.launch {
            _uiState.value = CommunityUiState.Loading
            repository.getAllPosts()
                .onSuccess { posts ->
                    _uiState.value = CommunityUiState.Success(posts)
                }
                .onFailure { error ->
                    _uiState.value = CommunityUiState.Error(
                        error.message ?: "Error al cargar los posts"
                    )
                }
        }
    }

    fun createPost(title: String, content: String) {
        viewModelScope.launch {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                _createPostState.value = CreatePostUiState.Error("Debes iniciar sesión")
                return@launch
            }

            _createPostState.value = CreatePostUiState.Loading
            
            // Obtener información del usuario desde Firestore
            val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
            val userName = userDoc.getString("username")?.ifEmpty { null }
                ?: userDoc.getString("fullName")?.ifEmpty { null }
                ?: currentUser.email?.split("@")?.firstOrNull()
                ?: "Usuario"
            val userAvatarUrl = userDoc.getString("avatarUrl")

            repository.createPost(
                userId = currentUser.uid,
                userName = userName,
                userAvatarUrl = userAvatarUrl,
                title = title,
                content = content
            )
                .onSuccess { post ->
                    _createPostState.value = CreatePostUiState.Success(post)
                    loadPosts() // Recargar posts
                }
                .onFailure { error ->
                    _createPostState.value = CreatePostUiState.Error(
                        error.message ?: "Error al crear el post"
                    )
                }
        }
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            val currentUser = auth.currentUser ?: return@launch
            repository.toggleLike(postId, currentUser.uid)
                .onSuccess {
                    loadPosts() // Recargar posts para actualizar likes
                }
                .onFailure {
                    // Error silencioso, no mostramos mensaje
                }
        }
    }

    fun loadComments(postId: String) {
        viewModelScope.launch {
            repository.getCommentsByPostId(postId)
                .onSuccess { commentsList ->
                    _comments.value = _comments.value.toMutableMap().apply {
                        put(postId, commentsList)
                    }
                }
                .onFailure {
                    // Error silencioso
                }
        }
    }

    fun addComment(postId: String, content: String) {
        viewModelScope.launch {
            val currentUser = auth.currentUser
            if (currentUser == null) return@launch

            // Obtener información del usuario desde Firestore
            val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
            val userName = userDoc.getString("username")?.ifEmpty { null }
                ?: userDoc.getString("fullName")?.ifEmpty { null }
                ?: currentUser.email?.split("@")?.firstOrNull()
                ?: "Usuario"
            val userAvatarUrl = userDoc.getString("avatarUrl")

            repository.addComment(
                postId = postId,
                userId = currentUser.uid,
                userName = userName,
                userAvatarUrl = userAvatarUrl,
                content = content
            )
                .onSuccess {
                    loadComments(postId) // Recargar comentarios
                    loadPosts() // Actualizar contador de comentarios
                }
                .onFailure {
                    // Error silencioso
                }
        }
    }

    fun resetCreatePostState() {
        _createPostState.value = CreatePostUiState.Idle
    }

    fun isPostLiked(post: Post): Boolean {
        val currentUser = auth.currentUser ?: return false
        return post.likedBy.contains(currentUser.uid)
    }
}
