package com.tecsup.nexusmobile.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.tecsup.nexusmobile.domain.model.Comment
import com.tecsup.nexusmobile.domain.model.Post
import com.tecsup.nexusmobile.domain.repository.CommunityRepository
import kotlinx.coroutines.tasks.await

class CommunityRepositoryImpl : CommunityRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val postsCollection = db.collection("posts")
    private val commentsCollection = db.collection("comments")

    override suspend fun getAllPosts(): Result<List<Post>> {
        return try {
            val snapshot = postsCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val posts = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Post::class.java)?.copy(id = doc.id)
            }
            Result.success(posts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createPost(
        userId: String,
        userName: String,
        userAvatarUrl: String?,
        title: String,
        content: String,
        imageUrls: List<String>
    ): Result<Post> {
        return try {
            val post = Post(
                userId = userId,
                userName = userName,
                userAvatarUrl = userAvatarUrl,
                title = title,
                content = content,
                imageUrls = imageUrls,
                createdAt = System.currentTimeMillis()
            )

            val docRef = postsCollection.add(post).await()
            val createdPost = post.copy(id = docRef.id)
            Result.success(createdPost)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleLike(postId: String, userId: String): Result<Boolean> {
        return try {
            val postDoc = postsCollection.document(postId).get().await()
            val post = postDoc.toObject(Post::class.java) ?: throw Exception("Post no encontrado")

            val likedBy = post.likedBy.toMutableList()
            val isLiked = likedBy.contains(userId)

            if (isLiked) {
                likedBy.remove(userId)
            } else {
                likedBy.add(userId)
            }

            postsCollection.document(postId).update(
                mapOf(
                    "likedBy" to likedBy,
                    "likesCount" to likedBy.size
                )
            ).await()

            Result.success(!isLiked)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCommentsByPostId(postId: String): Result<List<Comment>> {
        return try {
            android.util.Log.d("CommunityRepo", "Obteniendo comentarios para post: $postId")

            val snapshot = commentsCollection
                .whereEqualTo("postId", postId)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .await()

            android.util.Log.d("CommunityRepo", "Documentos encontrados: ${snapshot.documents.size}")

            val comments = snapshot.documents.mapNotNull { doc ->
                try {
                    android.util.Log.d("CommunityRepo", "Procesando doc: ${doc.id}, data: ${doc.data}")
                    doc.toObject(Comment::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    android.util.Log.e("CommunityRepo", "Error al parsear comentario ${doc.id}: ${e.message}")
                    null
                }
            }

            android.util.Log.d("CommunityRepo", "Comentarios parseados exitosamente: ${comments.size}")
            Result.success(comments)
        } catch (e: Exception) {
            android.util.Log.e("CommunityRepo", "Error al obtener comentarios: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun addComment(
        postId: String,
        userId: String,
        userName: String,
        userAvatarUrl: String?,
        content: String
    ): Result<Comment> {
        return try {
            android.util.Log.d("CommunityRepo", "Agregando comentario - Post: $postId, Usuario: $userName")

            // Crear el comentario con estructura clara
            val commentData = hashMapOf(
                "postId" to postId,
                "userId" to userId,
                "userName" to userName,
                "userAvatarUrl" to (userAvatarUrl ?: ""),
                "content" to content,
                "createdAt" to System.currentTimeMillis()
            )

            android.util.Log.d("CommunityRepo", "Datos del comentario: $commentData")

            // Agregar a Firestore
            val docRef = commentsCollection.add(commentData).await()

            android.util.Log.d("CommunityRepo", "Comentario agregado con ID: ${docRef.id}")

            // Crear el objeto Comment para retornar
            val createdComment = Comment(
                id = docRef.id,
                postId = postId,
                userId = userId,
                userName = userName,
                userAvatarUrl = userAvatarUrl,
                content = content,
                createdAt = System.currentTimeMillis()
            )

            // Actualizar el contador de comentarios en el post
            try {
                val postDoc = postsCollection.document(postId).get().await()
                val currentCount = postDoc.getLong("commentsCount")?.toInt() ?: 0
                postsCollection.document(postId).update("commentsCount", currentCount + 1).await()
                android.util.Log.d("CommunityRepo", "Contador actualizado: ${currentCount + 1}")
            } catch (e: Exception) {
                android.util.Log.e("CommunityRepo", "Error al actualizar contador: ${e.message}")
                // No fallar si el contador no se actualiza
            }

            Result.success(createdComment)
        } catch (e: Exception) {
            android.util.Log.e("CommunityRepo", "Error al agregar comentario: ${e.message}", e)
            Result.failure(e)
        }
    }
}