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

    // Listener en tiempo real para posts
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
                    // No necesitamos recargar manualmente, el listener se encarga
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
                    // El listener actualizará automáticamente
                }
                .onFailure {
                    // Error silencioso
                }
        }
    }

    // Listener en tiempo real para comentarios de un post específico
    fun loadComments(postId: String) {
        // Si ya existe un listener para este post, no crear otro
        if (commentListeners.containsKey(postId)) {
            android.util.Log.d("CommunityVM", "Listener ya existe para post $postId")
            return
        }

        android.util.Log.d("CommunityVM", "Creando listener para post $postId")

        val listener = firestore.collection("comments")
            .whereEqualTo("postId", postId)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("CommunityVM", "Error listener comentarios: ${error.message}")
                    android.util.Log.e("CommunityVM", "Error completo: ", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    android.util.Log.d("CommunityVM", "Snapshot recibido - Total documentos: ${snapshot.documents.size}")

                    val commentsList = snapshot.documents.mapNotNull { doc ->
                        try {
                            android.util.Log.d("CommunityVM", "Procesando doc ${doc.id}: ${doc.data}")

                            // Obtener los datos manualmente para debug
                            val postIdField = doc.getString("postId")
                            val userIdField = doc.getString("userId")
                            val userNameField = doc.getString("userName")
                            val contentField = doc.getString("content")

                            android.util.Log.d("CommunityVM", "Campos - postId: $postIdField, userId: $userIdField, userName: $userNameField, content: $contentField")

                            doc.toObject(Comment::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            android.util.Log.e("CommunityVM", "Error al parsear comentario ${doc.id}: ${e.message}")
                            android.util.Log.e("CommunityVM", "Stack trace: ", e)
                            null
                        }
                    }

                    android.util.Log.d("CommunityVM", "Comentarios parseados exitosamente: ${commentsList.size}")
                    commentsList.forEach { comment ->
                        android.util.Log.d("CommunityVM", "Comentario: ${comment.userName} - ${comment.content}")
                    }

                    // Actualizar el mapa de comentarios de forma inmutable
                    val currentComments = _comments.value.toMutableMap()
                    currentComments[postId] = commentsList
                    _comments.value = currentComments

                    android.util.Log.d("CommunityVM", "Estado actualizado - Total comentarios en mapa: ${_comments.value[postId]?.size}")
                } else {
                    android.util.Log.d("CommunityVM", "Snapshot es null para post $postId")
                    val currentComments = _comments.value.toMutableMap()
                    currentComments[postId] = emptyList()
                    _comments.value = currentComments
                }
            }

        commentListeners[postId] = listener
        android.util.Log.d("CommunityVM", "Listener registrado para post $postId")
    }

    // Detener listener de comentarios cuando se cierra el post
    fun stopListeningToComments(postId: String) {
        commentListeners[postId]?.remove()
        commentListeners.remove(postId)

        // Limpiar los comentarios del estado
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
                // Obtener información del usuario desde Firestore
                val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
                val userName = userDoc.getString("username")?.ifEmpty { null }
                    ?: userDoc.getString("fullName")?.ifEmpty { null }
                    ?: currentUser.email?.split("@")?.firstOrNull()
                    ?: "Usuario"
                val userAvatarUrl = userDoc.getString("avatarUrl")

                android.util.Log.d("CommunityVM", "Agregando comentario - Post: $postId, Usuario: $userName")

                repository.addComment(
                    postId = postId,
                    userId = currentUser.uid,
                    userName = userName,
                    userAvatarUrl = userAvatarUrl,
                    content = content
                )
                    .onSuccess {
                        android.util.Log.d("CommunityVM", "Comentario agregado exitosamente")
                        // El listener actualizará automáticamente los comentarios
                        // No necesitamos recargar manualmente
                    }
                    .onFailure { error ->
                        android.util.Log.e("CommunityVM", "Error al agregar comentario: ${error.message}")
                    }
            } catch (e: Exception) {
                android.util.Log.e("CommunityVM", "Excepción al agregar comentario: ${e.message}")
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
        // Limpiar todos los listeners
        postsListener?.remove()
        commentListeners.values.forEach { it.remove() }
        commentListeners.clear()
    }
}