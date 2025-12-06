package com.tecsup.nexusmobile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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

    private val commentListeners = mutableMapOf<String, ListenerRegistration>()
    private var postsListener: ListenerRegistration? = null

    init {
        setupRealtimePostsListener()
    }

    private fun setupRealtimePostsListener() {
        _uiState.value = CommunityUiState.Loading

        postsListener = firestore.collection("posts")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _uiState.value = CommunityUiState.Error(
                        error.message ?: "Error al cargar los posts"
                    )
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val posts = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Post::class.java)?.copy(id = doc.id)
                    }
                    _uiState.value = CommunityUiState.Success(posts)
                } else {
                    _uiState.value = CommunityUiState.Success(emptyList())
                }
            }
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

    fun createPost(title: String, content: String, imageUrls: List<String> = emptyList()) {
        viewModelScope.launch {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                _createPostState.value = CreatePostUiState.Error("Debes iniciar sesión")
                return@launch
            }

            _createPostState.value = CreatePostUiState.Loading

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
                content = content,
                imageUrls = imageUrls
            )
                .onSuccess { post ->
                    _createPostState.value = CreatePostUiState.Success(post)
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
        }
    }

    fun loadComments(postId: String) {
        if (commentListeners.containsKey(postId)) {
            android.util.Log.d("CommunityVM", "Listener ya existe para: $postId")
            return
        }

        android.util.Log.d("CommunityVM", "⭐ Creando listener de comentarios para post: $postId")

        val listener = firestore.collection("comments")
            .whereEqualTo("postId", postId)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("CommunityVM", "ERROR: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    android.util.Log.d("CommunityVM", "📦 Documentos recibidos: ${snapshot.documents.size}")

                    val commentsList = snapshot.documents.mapNotNull { doc ->
                        try {
                            val comment = doc.toObject(Comment::class.java)?.copy(id = doc.id)
                            android.util.Log.d("CommunityVM", "✅ Parseado: ${comment?.userName} - ${comment?.content}")
                            comment
                        } catch (e: Exception) {
                            android.util.Log.e("CommunityVM", "❌ Error parseando: ${e.message}")
                            null
                        }
                    }

                    android.util.Log.d("CommunityVM", "✅ Total comentarios parseados: ${commentsList.size}")

                    _comments.value = _comments.value.toMutableMap().apply {
                        this[postId] = commentsList
                    }

                    android.util.Log.d("CommunityVM", "📊 Comentarios en estado: ${_comments.value[postId]?.size ?: 0}")
                }
            }

        commentListeners[postId] = listener
    }

    fun stopListeningToComments(postId: String) {
        android.util.Log.d("CommunityVM", "🛑 Deteniendo listener: $postId")
        commentListeners[postId]?.remove()
        commentListeners.remove(postId)

        _comments.value = _comments.value.toMutableMap().apply {
            remove(postId)
        }
    }

    fun addComment(postId: String, content: String) {
        viewModelScope.launch {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                android.util.Log.e("CommunityVM", "Usuario no autenticado")
                return@launch
            }

            try {
                val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
                val userName = userDoc.getString("username")?.ifEmpty { null }
                    ?: userDoc.getString("fullName")?.ifEmpty { null }
                    ?: currentUser.email?.split("@")?.firstOrNull()
                    ?: "Usuario"
                val userAvatarUrl = userDoc.getString("avatarUrl")

                android.util.Log.d("CommunityVM", "Agregando comentario de: $userName")

                repository.addComment(
                    postId = postId,
                    userId = currentUser.uid,
                    userName = userName,
                    userAvatarUrl = userAvatarUrl,
                    content = content
                )
                    .onSuccess {
                        android.util.Log.d("CommunityVM", "✅ Comentario agregado")
                    }
                    .onFailure { error ->
                        android.util.Log.e("CommunityVM", "❌ Error: ${error.message}")
                    }
            } catch (e: Exception) {
                android.util.Log.e("CommunityVM", "❌ Excepción: ${e.message}")
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

    override fun onCleared() {
        super.onCleared()
        postsListener?.remove()
        commentListeners.values.forEach { it.remove() }
        commentListeners.clear()
    }
}