package com.tecsup.nexusmobile.domain.repository

import com.tecsup.nexusmobile.domain.model.User

interface ProfileRepository {
    suspend fun getCurrentUser(): Result<User?>
    suspend fun updateProfile(userId: String, username: String, fullName: String, avatarUrl: String?): Result<User>
}
