package com.tecsup.nexusmobile.data.api


import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GroqChatService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()


    private val apiKey = " " //AQUI PONER EL KEY para que funcione, si se sube con key se bloquea

    private val baseUrl = "https://api.groq.com/openai/v1/chat/completions"

    suspend fun getGameRecommendation(
        userMessage: String,
        availableGames: List<String>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = """
                Eres un asistente especializado en recomendar videojuegos de la tienda Nexus.
                Solo puedes recomendar juegos de esta lista: ${availableGames.joinToString(", ")}.
                
                Reglas:
                - Solo recomienda juegos que estén en la lista proporcionada
                - Sé breve y directo (máximo 3-4 oraciones)
                - Si preguntan por un juego que no está en la lista, sugiere alternativas disponibles
                - Enfócate en las características del juego (género, jugabilidad)
                - Sé amigable y entusiasta
            """.trimIndent()

            val requestBody = JSONObject().apply {
                put("model", "llama-3.1-8b-instant") // Modelo rápido y gratuito
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userMessage)
                    })
                })
                put("temperature", 0.7)
                put("max_tokens", 250)
            }

            val request = Request.Builder()
                .url(baseUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Error ${response.code}: ${responseBody ?: "Sin respuesta"}")
                )
            }

            if (responseBody == null) {
                return@withContext Result.failure(Exception("Respuesta vacía del servidor"))
            }

            val jsonResponse = JSONObject(responseBody)
            val choices = jsonResponse.getJSONArray("choices")
            val message = choices.getJSONObject(0)
                .getJSONObject("message")
                .getString("content")

            Result.success(message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Método alternativo con conversación mantenida
    suspend fun getChatResponse(
        messages: List<ChatMessage>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val requestBody = JSONObject().apply {
                put("model", "llama-3.1-8b-instant")
                put("messages", JSONArray().apply {
                    messages.forEach { msg ->
                        put(JSONObject().apply {
                            put("role", msg.role)
                            put("content", msg.content)
                        })
                    }
                })
                put("temperature", 0.7)
                put("max_tokens", 250)
            }

            val request = Request.Builder()
                .url(baseUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Error ${response.code}: ${responseBody ?: "Sin respuesta"}")
                )
            }

            if (responseBody == null) {
                return@withContext Result.failure(Exception("Respuesta vacía"))
            }

            val jsonResponse = JSONObject(responseBody)
            val choices = jsonResponse.getJSONArray("choices")
            val message = choices.getJSONObject(0)
                .getJSONObject("message")
                .getString("content")

            Result.success(message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class ChatMessage(
    val role: String, // "system", "user", o "assistant"
    val content: String
)