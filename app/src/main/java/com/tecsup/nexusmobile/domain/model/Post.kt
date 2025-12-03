package com.tecsup.nexusmobile.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class Post(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatarUrl: String? = null,
    val title: String = "",
    val content: String = "",
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    @PropertyName("createdAt")
    private val _createdAt: Any? = null, // Puede ser Timestamp o Long
    val likedBy: List<String> = emptyList()
) {
    // Propiedad computada que convierte Timestamp a Long
    val createdAt: Long
        get() = when (_createdAt) {
            is Timestamp -> _createdAt.toDate().time
            is Long -> _createdAt
            else -> System.currentTimeMillis()
        }

    // Constructor para crear nuevos posts (sin necesidad de especificar _createdAt)
    constructor(
        id: String = "",
        userId: String = "",
        userName: String = "",
        userAvatarUrl: String? = null,
        title: String = "",
        content: String = "",
        likesCount: Int = 0,
        commentsCount: Int = 0,
        createdAt: Long = System.currentTimeMillis(),
        likedBy: List<String> = emptyList()
    ) : this(
        id = id,
        userId = userId,
        userName = userName,
        userAvatarUrl = userAvatarUrl,
        title = title,
        content = content,
        likesCount = likesCount,
        commentsCount = commentsCount,
        _createdAt = createdAt,
        likedBy = likedBy
    )
}