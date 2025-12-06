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