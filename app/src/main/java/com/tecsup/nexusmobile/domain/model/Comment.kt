package com.tecsup.nexusmobile.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class Comment(
    val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatarUrl: String? = null,
    val content: String = "",
    @PropertyName("createdAt")
    private val _createdAt: Any? = null // Puede ser Timestamp o Long
) {
    // Propiedad computada que convierte Timestamp a Long
    val createdAt: Long
        get() = when (_createdAt) {
            is Timestamp -> _createdAt.toDate().time
            is Long -> _createdAt
            is Number -> _createdAt.toLong()
            else -> System.currentTimeMillis()
        }

    // Constructor secundario para crear nuevos comentarios fácilmente
    constructor(
        id: String = "",
        postId: String = "",
        userId: String = "",
        userName: String = "",
        userAvatarUrl: String? = null,
        content: String = "",
        createdAt: Long = System.currentTimeMillis()
    ) : this(
        id = id,
        postId = postId,
        userId = userId,
        userName = userName,
        userAvatarUrl = userAvatarUrl,
        content = content,
        _createdAt = createdAt
    )

    // Metodo para convertir a Map para Firestore
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "postId" to postId,
            "userId" to userId,
            "userName" to userName,
            "userAvatarUrl" to userAvatarUrl,
            "content" to content,
            "createdAt" to System.currentTimeMillis()
        )
    }
}