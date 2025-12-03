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
        content: String
    ): Result<Post> {
        return try {
            val post = Post(
                userId = userId,
                userName = userName,
                userAvatarUrl = userAvatarUrl,
                title = title,
                content = content,
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
            val snapshot = commentsCollection
                .whereEqualTo("postId", postId)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .await()

            val comments = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Comment::class.java)?.copy(id = doc.id)
            }
            Result.success(comments)
        } catch (e: Exception) {
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
            val comment = Comment(
                postId = postId,
                userId = userId,
                userName = userName,
                userAvatarUrl = userAvatarUrl,
                content = content,
                createdAt = System.currentTimeMillis()
            )

            val docRef = commentsCollection.add(comment).await()
            val createdComment = comment.copy(id = docRef.id)
            
            // Actualizar el contador de comentarios en el post
            val postDoc = postsCollection.document(postId).get().await()
            val currentCount = postDoc.getLong("commentsCount")?.toInt() ?: 0
            postsCollection.document(postId).update("commentsCount", currentCount + 1).await()
            
            Result.success(createdComment)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
