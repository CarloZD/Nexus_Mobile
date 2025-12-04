package com.tecsup.nexusmobile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.nexusmobile.data.api.ChatMessage
import com.tecsup.nexusmobile.data.api.GroqChatService
import com.tecsup.nexusmobile.data.repository.GameRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ChatBotMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

sealed class ChatbotUiState {
    object Idle : ChatbotUiState()
    object Loading : ChatbotUiState()
    data class Success(val messages: List<ChatBotMessage>) : ChatbotUiState()
    data class Error(val message: String) : ChatbotUiState()
}

class ChatbotViewModel(
    private val chatService: GroqChatService = GroqChatService(),
    private val gameRepository: GameRepositoryImpl = GameRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatbotUiState>(ChatbotUiState.Idle)
    val uiState: StateFlow<ChatbotUiState> = _uiState

    private val _messages = mutableListOf<ChatBotMessage>()
    private val _conversationHistory = mutableListOf<ChatMessage>()

    private var availableGames = listOf<String>()

    init {
        loadGames()
        initializeChat()
    }

    private fun loadGames() {
        viewModelScope.launch {
            gameRepository.getAllGames()
                .onSuccess { games ->
                    availableGames = games.map { it.title }
                    initializeChat()
                }
        }
    }

    private fun initializeChat() {
        if (availableGames.isEmpty()) return

        val welcomeMessage = ChatBotMessage(
            content = "¡Hola! 👋 Soy tu asistente de Nexus. Puedo ayudarte a encontrar el juego perfecto. ¿Qué tipo de juego te gustaría explorar hoy?",
            isFromUser = false
        )
        _messages.add(welcomeMessage)

        // Configurar el contexto del sistema
        _conversationHistory.add(
            ChatMessage(
                role = "system",
                content = """
                    Eres un asistente especializado en recomendar videojuegos de la tienda Nexus.
                    Solo puedes recomendar juegos de esta lista: ${availableGames.joinToString(", ")}.
                    
                    Reglas:
                    - Solo recomienda juegos que estén en la lista
                    - Sé breve (máximo 3-4 oraciones)
                    - Si preguntan por un juego no disponible, sugiere alternativas
                    - Enfócate en género, jugabilidad y experiencia
                    - Sé amigable y entusiasta
                    - Usa emojis ocasionalmente 🎮
                """.trimIndent()
            )
        )

        _uiState.value = ChatbotUiState.Success(_messages.toList())
    }

    fun sendMessage(userMessage: String) {
        if (userMessage.isBlank()) return

        viewModelScope.launch {
            // Agregar mensaje del usuario
            val userMsg = ChatBotMessage(
                content = userMessage,
                isFromUser = true
            )
            _messages.add(userMsg)
            _conversationHistory.add(
                ChatMessage(role = "user", content = userMessage)
            )
            _uiState.value = ChatbotUiState.Success(_messages.toList())

            // Mostrar estado de carga
            _uiState.value = ChatbotUiState.Loading

            // Obtener respuesta del chatbot
            chatService.getChatResponse(_conversationHistory)
                .onSuccess { response ->
                    val botMsg = ChatBotMessage(
                        content = response,
                        isFromUser = false
                    )
                    _messages.add(botMsg)
                    _conversationHistory.add(
                        ChatMessage(role = "assistant", content = response)
                    )
                    _uiState.value = ChatbotUiState.Success(_messages.toList())
                }
                .onFailure { error ->
                    val errorMsg = ChatBotMessage(
                        content = "Lo siento, tuve un problema. ¿Podrías intentarlo de nuevo? 🤖",
                        isFromUser = false
                    )
                    _messages.add(errorMsg)
                    _uiState.value = ChatbotUiState.Success(_messages.toList())
                }
        }
    }

    fun clearChat() {
        _messages.clear()
        _conversationHistory.clear()
        initializeChat()
    }

    fun getQuickSuggestions(): List<String> {
        return listOf(
            "Recomiéndame un juego de terror",
            "¿Qué juegos de acción tienes?",
            "Quiero jugar algo relajante",
            "Juegos multijugador disponibles"
        )
    }
}