package com.tecsup.nexusmobile.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.tecsup.nexusmobile.domain.model.Game
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ViewModel para pruebas
class TestFirebaseViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    var games by mutableStateOf<List<Game>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var gamesCount by mutableStateOf(0)
        private set

    fun loadGames() {
        viewModelScope.launch {
            isLoading = true
            error = null
            try {
                val snapshot = db.collection("games")
                    .limit(10)
                    .get()
                    .await()

                games = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Game::class.java)?.copy(id = doc.id)
                }

                isLoading = false
            } catch (e: Exception) {
                error = e.message
                isLoading = false
            }
        }
    }

    fun countGames() {
        viewModelScope.launch {
            isLoading = true
            try {
                val snapshot = db.collection("games").get().await()
                gamesCount = snapshot.size()
                isLoading = false
            } catch (e: Exception) {
                error = e.message
                isLoading = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestFirebaseScreen(
    viewModel: TestFirebaseViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.countGames()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Test Firebase Connection") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Estado de la conexión
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (viewModel.error == null)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = if (viewModel.error == null)
                            "✅ Conectado a Firebase"
                        else
                            "❌ Error de conexión",
                        style = MaterialTheme.typography.titleMedium
                    )

                    if (viewModel.error != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = viewModel.error ?: "",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Contador de juegos
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Total de juegos en Firestore:",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "${viewModel.gamesCount}",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botones de prueba
            Button(
                onClick = { viewModel.loadGames() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cargar primeros 10 juegos")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.countGames() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Actualizar contador")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lista de juegos
            if (viewModel.isLoading) {
                CircularProgressIndicator()
            } else if (viewModel.games.isNotEmpty()) {
                Text(
                    text = "Juegos encontrados:",
                    style = MaterialTheme.typography.titleMedium
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(viewModel.games) { game ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = game.title,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "$${game.price}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "ID: ${game.id}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}