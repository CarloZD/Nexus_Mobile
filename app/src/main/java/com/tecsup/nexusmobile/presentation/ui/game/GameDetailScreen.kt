package com.tecsup.nexusmobile.presentation.ui.game

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tecsup.nexusmobile.presentation.viewmodel.GameDetailUiState
import com.tecsup.nexusmobile.presentation.viewmodel.GameDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailScreen(
    gameId: String,
    viewModel: GameDetailViewModel = viewModel(),
    onBackClick: () -> Unit,
    onAddToCart: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(gameId) {
        viewModel.loadGame(gameId)
    }

    Scaffold(
        bottomBar = {
            when (val state = uiState) {
                is GameDetailUiState.Success -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shadowElevation = 8.dp,
                        tonalElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AddToCartButton(
                                game = state.game,
                                onAddToCart = {
                                    onAddToCart(state.game.id)
                                }
                            )
                        }
                    }
                }
                else -> {}
            }
        }
    ) { padding ->
        when (val state = uiState) {
            is GameDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is GameDetailUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    GameHeader(
                        game = state.game,
                        onBackClick = onBackClick
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        GameInfo(game = state.game)

                        Divider()

                        // Sección de reseñas (por ahora vacía, se puede conectar después)
                        ReviewSection(reviews = emptyList())
                    }
                }
            }

            is GameDetailUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Error al cargar el juego",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = { viewModel.loadGame(gameId) }) {
                            Text("Reintentar")
                        }
                        OutlinedButton(onClick = onBackClick) {
                            Text("Volver")
                        }
                    }
                }
            }
        }
    }
}
