package com.tecsup.nexusmobile.domain.repository

import com.tecsup.nexusmobile.domain.model.Comment
import com.tecsup.nexusmobile.domain.model.Post

interface CommunityRepository {
    suspend fun getAllPosts(): Result<List<Post>>
    suspend fun createPost(
        userId: String,
        userName: String,
        userAvatarUrl: String?,
        title: String,
        content: String,
        imageUrls: List<String> = emptyList()
    ): Result<Post>
    suspend fun toggleLike(postId: String, userId: String): Result<Boolean> // Retorna true si se dio like, false si se quitó
    suspend fun getCommentsByPostId(postId: String): Result<List<Comment>>
    suspend fun addComment(postId: String, userId: String, userName: String, userAvatarUrl: String?, content: String): Result<Comment>
}