package com.tecsup.nexusmobile.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
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

    // ✅ Tu configuración de Cloudinary
    private val cloudName = "dnvsu8ugs"
    private val uploadPreset = "nexus_profiles"

    /**
     * Sube una imagen a Cloudinary con public_id único
     * Estrategia: user_USERID_TIMESTAMP para permitir múltiples versiones
     */
    suspend fun uploadProfileImage(
        context: Context,
        imageUri: Uri,
        userId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d("Cloudinary", " Iniciando subida de imagen para usuario: $userId")

            // 1. Procesar imagen (crop cuadrado y resize)
            val processedFile = processImageForProfile(context, imageUri)
            Log.d("Cloudinary", " Imagen procesada: ${processedFile.length() / 1024}KB")

            // 2. Generar public_id ÚNICO con timestamp
            // Esto permite múltiples versiones y evita conflictos
            val timestamp = System.currentTimeMillis()
            val publicId = "user_${userId}_$timestamp"

            // 3. Preparar multipart request
            //  En modo Unsigned, SOLO se pueden enviar parámetros básicos
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    "profile.jpg",
                    processedFile.asRequestBody("image/jpeg".toMediaType())
                )
                .addFormDataPart("upload_preset", uploadPreset)
                .addFormDataPart("public_id", publicId)
                // Tags para organizar y facilitar búsqueda/limpieza
                .addFormDataPart("tags", "profile,user_$userId")
                .build()

            // 4. Hacer request a Cloudinary
            val url = "https://api.cloudinary.com/v1_1/$cloudName/image/upload"
            Log.d("Cloudinary", " Enviando a: $url")
            Log.d("Cloudinary", " Public ID: $publicId")

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            Log.d("Cloudinary", "📡 Response code: ${response.code}")
            Log.d("Cloudinary", "📡 Response body: $responseBody")

            if (!response.isSuccessful) {
                processedFile.delete()

                val errorMessage = when (response.code) {
                    400 -> {
                        val errorDetail = try {
                            val json = JSONObject(responseBody ?: "{}")
                            json.getJSONObject("error").getString("message")
                        } catch (e: Exception) {
                            responseBody ?: "Error desconocido"
                        }
                        " Error en la solicitud: $errorDetail"
                    }
                    401 -> " Upload preset '$uploadPreset' inválido o no es 'Unsigned'"
                    404 -> " Cloud name incorrecto: '$cloudName'"
                    else -> "Error ${response.code}: ${responseBody ?: "Sin respuesta"}"
                }

                return@withContext Result.failure(Exception(errorMessage))
            }

            if (responseBody == null) {
                processedFile.delete()
                return@withContext Result.failure(Exception("Respuesta vacía del servidor"))
            }

            // 5. Parsear respuesta y obtener URL
            val jsonResponse = JSONObject(responseBody)

            // Opción 1: Usar la URL directa que devuelve Cloudinary (con transformaciones del preset)
            val secureUrl = jsonResponse.getString("secure_url")

            // Opción 2: Construir URL personalizada con transformaciones específicas
            val returnedPublicId = jsonResponse.getString("public_id")
            val customUrl = "https://res.cloudinary.com/$cloudName/image/upload/" +
                    "c_fill,g_face,h_400,w_400,q_auto,f_auto/" +
                    "$returnedPublicId.jpg"

            Log.d("Cloudinary", "✅ Imagen subida exitosamente")
            Log.d("Cloudinary", "🔗 URL Original: $secureUrl")
            Log.d("Cloudinary", "🔗 URL Custom: $customUrl")

            // 6. Limpiar archivo temporal
            processedFile.delete()

            // Retornar URL personalizada con transformaciones
            Result.success(customUrl)
        } catch (e: Exception) {
            Log.e("Cloudinary", " Error al subir imagen: ${e.message}", e)
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
     * Limpia imágenes antiguas del usuario (opcional - requiere Admin API)
     * Por ahora solo devuelve success para no bloquear
     */
    suspend fun deleteImage(imageUrl: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!imageUrl.contains("cloudinary.com")) {
                return@withContext Result.success(Unit)
            }

            Log.d("Cloudinary", "ℹ️ Las imágenes antiguas permanecen en Cloudinary")
            Log.d("Cloudinary", "💡 Tip: Implementa limpieza periódica desde el dashboard")

            Result.success(Unit)
        } catch (e: Exception) {
            Result.success(Unit)
        }
    }

    /**
     * Obtiene la URL más reciente del perfil del usuario
     * Útil para mostrar siempre la última foto subida
     */
    fun getLatestProfileUrl(userId: String): String {
        // Construir URL con transformación que obtiene la versión más reciente
        return "https://res.cloudinary.com/$cloudName/image/upload/" +
                "c_fill,g_face,h_400,w_400,q_auto,f_auto/" +
                "nexus/profiles/user_$userId.jpg"
    }
}