package com.tecsup.nexusmobile.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ImageUploadRepository {
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    // Comprimir imagen y convertir a Base64
    private suspend fun compressImageToBase64(context: Context, imageUri: Uri, maxSizeKB: Int = 200): String = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(imageUri)
            ?: throw Exception("No se pudo leer la imagen")
        
        // Decodificar la imagen
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        
        // Comprimir la imagen
        var quality = 90
        var compressedBitmap = originalBitmap
        var outputStream: ByteArrayOutputStream
        
        do {
            outputStream = ByteArrayOutputStream()
            compressedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val sizeKB = outputStream.size() / 1024
            
            if (sizeKB > maxSizeKB && quality > 20) {
                quality -= 10
                // Redimensionar si es necesario
                val scale = 0.8f
                val width = (compressedBitmap.width * scale).toInt()
                val height = (compressedBitmap.height * scale).toInt()
                compressedBitmap = Bitmap.createScaledBitmap(compressedBitmap, width, height, true)
            } else {
                break
            }
        } while (sizeKB > maxSizeKB && quality > 20)
        
        // Convertir a Base64
        val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        outputStream.close()
        
        // Liberar memoria
        originalBitmap.recycle()
        if (compressedBitmap != originalBitmap) {
            compressedBitmap.recycle()
        }
        
        base64
    }

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

            // Intentar primero con Firebase Storage
            val storageResult = tryUploadToStorage(context, imageUri, currentUser.uid)
            
            if (storageResult.isSuccess) {
                return storageResult
            }

            // Si falla Storage, usar Firestore como alternativa (Base64)
            uploadToFirestoreAsBase64(context, imageUri, currentUser.uid)
            
        } catch (e: Exception) {
            Result.failure(Exception("Error al subir la imagen: ${e.message ?: "Error desconocido"}"))
        }
    }
    
    private suspend fun tryUploadToStorage(context: Context, imageUri: Uri, userId: String): Result<String> {
        return try {
            // Crear referencia única para la imagen
            val fileName = "profile_images/$userId/${UUID.randomUUID()}.jpg"
            val storageRef = storage.reference.child(fileName)

            // Convertir URI content:// a archivo temporal para Firebase Storage
            val tempFile = withContext(Dispatchers.IO) {
                val inputStream = context.contentResolver.openInputStream(imageUri)
                    ?: throw Exception("No se pudo leer la imagen")
                
                val file = File(context.cacheDir, "temp_upload_${UUID.randomUUID()}.jpg")
                FileOutputStream(file).use { output ->
                    inputStream.copyTo(output)
                }
                file
            }

            try {
                val uploadTask = storageRef.putFile(Uri.fromFile(tempFile)).await()
                tempFile.delete()
                val downloadUrl = storageRef.downloadUrl.await()
                Result.success(downloadUrl.toString())
            } catch (e: com.google.firebase.storage.StorageException) {
                tempFile.delete()
                Result.failure(e) // Devolver el error para intentar alternativa
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun uploadToFirestoreAsBase64(context: Context, imageUri: Uri, userId: String): Result<String> {
        return try {
            // Comprimir y convertir a Base64 (máximo 200KB para que quepa en Firestore)
            val base64Image = compressImageToBase64(context, imageUri, maxSizeKB = 200)
            
            // Guardar en Firestore en una colección separada para imágenes
            val imageId = UUID.randomUUID().toString()
            val imageData = hashMapOf(
                "userId" to userId,
                "imageBase64" to base64Image,
                "timestamp" to System.currentTimeMillis(),
                "type" to "profile_image"
            )
            
            firestore.collection("user_images")
                .document(imageId)
                .set(imageData)
                .await()
            
            // Retornar un identificador especial que indique que está en Firestore
            // Formato: "firestore://{imageId}" para distinguirlo de URLs de Storage
            Result.success("firestore://$imageId")
        } catch (e: Exception) {
            Result.failure(Exception("Error al guardar imagen en Firestore: ${e.message ?: "Error desconocido"}"))
        }
    }
    
    suspend fun getImageFromFirestore(imageId: String): Result<String> {
        return try {
            val doc = firestore.collection("user_images")
                .document(imageId)
                .get()
                .await()
            
            val base64Image = doc.getString("imageBase64")
                ?: return Result.failure(Exception("Imagen no encontrada"))
            
            // Convertir Base64 a data URI para que Coil pueda cargarla
            Result.success("data:image/jpeg;base64,$base64Image")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProfileImage(imageUrl: String): Result<Unit> {
        return try {
            if (imageUrl.isBlank()) {
                return Result.success(Unit)
            }
            
            // Si es una imagen en Firestore
            if (imageUrl.startsWith("firestore://")) {
                val imageId = imageUrl.removePrefix("firestore://")
                try {
                    firestore.collection("user_images")
                        .document(imageId)
                        .delete()
                        .await()
                } catch (e: Exception) {
                    // No es crítico si falla
                }
                return Result.success(Unit)
            }
            
            // Si es una URL de Firebase Storage
            if (imageUrl.contains("firebasestorage.googleapis.com")) {
                val storageRef = storage.getReferenceFromUrl(imageUrl)
                try {
                    storageRef.metadata.await()
                    storageRef.delete().await()
                } catch (e: Exception) {
                    // Si no existe, no es un error crítico
                    if (e.message?.contains("does not exist") == true) {
                        return Result.success(Unit)
                    }
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            // No fallar si no se puede eliminar la imagen anterior
            Result.success(Unit)
        }
    }
}

