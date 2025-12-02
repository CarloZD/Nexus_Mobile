package com.tecsup.nexusmobile.domain.model

data class User(
    val id: String = "",
    val email: String = "",
    val username: String = "",
    val fullName: String = "",
    val avatarUrl: String? = null,
    val role: String = "USER",
    val active: Boolean = true
)