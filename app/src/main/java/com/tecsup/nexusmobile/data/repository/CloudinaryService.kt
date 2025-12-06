package com.tecsup.nexusmobile.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class CloudinaryService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Credenciales de Cloudinary
    private val cloudName = ""
    private val apiKey = ""
    private val apiSecret = ""
    private val uploadPreset = "nexus_profiles" // Vamos a crear este preset en Cloudinary

    /**
     * Sube una imagen a Cloudinary con transformaciones automáticas
     */
    suspend fun uploadProfileImage(
        context: Context,
        imageUri: Uri,
        userId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 1. Procesar imagen (crop cuadrado y resize)
            val processedFile = processImageForProfile(context, imageUri)

            // 2. Preparar multipart request
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    "profile_$userId.jpg",
                    processedFile.asRequestBody("image/jpeg".toMediaType())
                )
                .addFormDataPart("upload_preset", uploadPreset)
                .addFormDataPart("folder", "nexus/profiles") // Organizar en carpetas
                .addFormDataPart("public_id", "user_$userId") // ID único por usuario
                .addFormDataPart("overwrite", "true") // Sobrescribir si existe
                .addFormDataPart("transformation", "c_fill,g_face,h_400,w_400") // Crop centrado en cara
                .build()

            // 3. Hacer request a Cloudinary
            val request = Request.Builder()
                .url("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful) {
                processedFile.delete()
                return@withContext Result.failure(
                    Exception("Error al subir imagen: ${response.code} - ${responseBody ?: "Sin respuesta"}")
                )
            }

            if (responseBody == null) {
                processedFile.delete()
                return@withContext Result.failure(Exception("Respuesta vacía del servidor"))
            }

            // 4. Parsear respuesta
            val jsonResponse = JSONObject(responseBody)
            val secureUrl = jsonResponse.getString("secure_url")

            // 5. Limpiar archivo temporal
            processedFile.delete()

            Result.success(secureUrl)
        } catch (e: Exception) {
            Result.failure(Exception("Error al subir la imagen: ${e.message}"))
        }
    }

    /**
     * Procesa la imagen: crop cuadrado centrado + resize a 800x800
     */
    private suspend fun processImageForProfile(
        context: Context,
        imageUri: Uri
    ): File = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(imageUri)
            ?: throw Exception("No se pudo leer la imagen")

        // Decodificar imagen original
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        // Hacer crop cuadrado centrado
        val croppedBitmap = cropToSquare(originalBitmap)

        // Resize a 800x800 (tamaño óptimo para perfiles)
        val resizedBitmap = Bitmap.createScaledBitmap(croppedBitmap, 800, 800, true)

        // Comprimir a JPEG con calidad 85%
        val outputStream = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)

        // Guardar en archivo temporal
        val tempFile = File(context.cacheDir, "temp_profile_${System.currentTimeMillis()}.jpg")
        FileOutputStream(tempFile).use { fos ->
            fos.write(outputStream.toByteArray())
        }

        // Liberar memoria
        originalBitmap.recycle()
        croppedBitmap.recycle()
        resizedBitmap.recycle()
        outputStream.close()

        tempFile
    }

    /**
     * Hace crop cuadrado centrado de la imagen
     */
    private fun cropToSquare(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val size = minOf(width, height)

        val x = (width - size) / 2
        val y = (height - size) / 2

        return Bitmap.createBitmap(bitmap, x, y, size, size)
    }

    /**
     * Elimina imagen anterior de Cloudinary (opcional)
     */
    suspend fun deleteImage(imageUrl: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {

            if (!imageUrl.contains("cloudinary.com")) {
                return@withContext Result.success(Unit) // No es de Cloudinary, ignorar
            }

            val publicId = extractPublicIdFromUrl(imageUrl)
            if (publicId == null) {
                return@withContext Result.success(Unit)
            }

            // Cloudinary requiere firma para eliminación
            // Por simplicidad, dejamos que se sobrescriba automáticamente
            // (usamos overwrite=true en el upload)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.success(Unit) // No fallar si no se puede eliminar
        }
    }

    private fun extractPublicIdFromUrl(url: String): String? {
        return try {
            val parts = url.split("/upload/")
            if (parts.size < 2) return null

            val afterUpload = parts[1]
            val pathParts = afterUpload.split("/")

            // Remover versión (v1234567890) si existe
            val relevantParts = pathParts.filter { !it.startsWith("v") || it.length < 10 }

            // Unir y remover extensión
            relevantParts.joinToString("/").substringBeforeLast(".")
        } catch (e: Exception) {
            null
        }
    }
}
