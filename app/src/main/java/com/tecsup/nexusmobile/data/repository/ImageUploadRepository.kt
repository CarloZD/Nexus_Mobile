package com.tecsup.nexusmobile.data.repository

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ImageUploadRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val cloudinaryService = CloudinaryService()

    /**
     * Sube una imagen de perfil de usuario
     */
    suspend fun uploadProfileImage(context: Context, imageUri: Uri): Result<String> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("Usuario no autenticado"))
            }

            // Verificar que la URI sea válida
            if (imageUri.toString().isEmpty()) {
                return Result.failure(Exception("URI de imagen inválida"))
            }

            // Subir a Cloudinary
            cloudinaryService.uploadProfileImage(context, imageUri, currentUser.uid)

        } catch (e: Exception) {
            Result.failure(Exception("Error al subir la imagen: ${e.message ?: "Error desconocido"}"))
        }
    }

    /**
     * Sube una imagen para un post de la comunidad
     */
    suspend fun uploadPostImage(context: Context, imageUri: Uri): Result<String> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("Usuario no autenticado"))
            }

            // Verificar que la URI sea válida
            if (imageUri.toString().isEmpty()) {
                return Result.failure(Exception("URI de imagen inválida"))
            }

            // Subir a Cloudinary con un identificador único para posts
            // Formato: post_USERID_TIMESTAMP para distinguir de fotos de perfil
            val timestamp = System.currentTimeMillis()
            val uniqueId = "post_${currentUser.uid}_$timestamp"
            cloudinaryService.uploadProfileImage(context, imageUri, uniqueId)

        } catch (e: Exception) {
            Result.failure(Exception("Error al subir la imagen: ${e.message ?: "Error desconocido"}"))
        }
    }

    suspend fun deleteProfileImage(imageUrl: String): Result<Unit> {
        return try {
            if (imageUrl.isBlank()) {
                return Result.success(Unit)
            }

            // Eliminar de Cloudinary (opcional, se sobrescribe automáticamente)
            cloudinaryService.deleteImage(imageUrl)

            Result.success(Unit)
        } catch (e: Exception) {
            // No fallar si no se puede eliminar la imagen anterior
            Result.success(Unit)
        }
    }
}