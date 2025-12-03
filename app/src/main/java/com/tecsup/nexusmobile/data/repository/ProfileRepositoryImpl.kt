package com.tecsup.nexusmobile.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tecsup.nexusmobile.domain.model.User
import com.tecsup.nexusmobile.domain.repository.ProfileRepository
import kotlinx.coroutines.tasks.await

class ProfileRepositoryImpl(
    private val imageUploadRepository: ImageUploadRepository = ImageUploadRepository()
) : ProfileRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    override suspend fun getCurrentUser(): Result<User?> {
        return try {
            val firebaseUser = auth.currentUser
            if (firebaseUser == null) {
                return Result.success(null)
            }

            val doc = usersCollection.document(firebaseUser.uid).get().await()
            val user = doc.toObject(User::class.java)?.copy(id = doc.id)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProfile(
        userId: String,
        username: String,
        fullName: String,
        avatarUrl: String?
    ): Result<User> {
        return try {
            val updates = hashMapOf<String, Any>(
                "username" to username,
                "fullName" to fullName
            )
            
            if (avatarUrl != null) {
                updates["avatarUrl"] = avatarUrl
            }

            usersCollection.document(userId)
                .update(updates)
                .await()

            // Obtener el usuario actualizado
            val doc = usersCollection.document(userId).get().await()
            val updatedUser = doc.toObject(User::class.java)?.copy(id = doc.id)
                ?: throw Exception("Error al obtener usuario actualizado")

            Result.success(updatedUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadAndUpdateProfileImage(
        userId: String,
        imageUri: Uri
    ): Result<String> {
        return try {
            // Obtener el usuario actual para eliminar la imagen anterior si existe
            val currentUserDoc = usersCollection.document(userId).get().await()
            val currentUser = currentUserDoc.toObject(User::class.java)
            
            // Subir nueva imagen
            val imageUrlResult = imageUploadRepository.uploadProfileImage(imageUri)
            val newImageUrl = imageUrlResult.getOrElse {
                return Result.failure(it)
            }

            // Eliminar imagen anterior si existe
            currentUser?.avatarUrl?.let { oldImageUrl ->
                if (oldImageUrl.isNotEmpty() && oldImageUrl.startsWith("https://")) {
                    imageUploadRepository.deleteProfileImage(oldImageUrl)
                }
            }

            // Actualizar perfil con nueva URL
            usersCollection.document(userId)
                .update("avatarUrl", newImageUrl)
                .await()

            Result.success(newImageUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
