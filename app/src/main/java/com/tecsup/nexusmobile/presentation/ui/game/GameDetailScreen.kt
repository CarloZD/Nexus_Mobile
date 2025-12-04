package com.tecsup.nexusmobile.presentation.ui.game

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tecsup.nexusmobile.presentation.viewmodel.CartViewModel
import com.tecsup.nexusmobile.presentation.viewmodel.GameDetailUiState
import com.tecsup.nexusmobile.presentation.viewmodel.GameDetailViewModel
import com.tecsup.nexusmobile.presentation.viewmodel.ReviewViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailScreen(
    gameId: String,
    viewModel: GameDetailViewModel = viewModel(),
    cartViewModel: CartViewModel = viewModel(),
    onBackClick: () -> Unit,
    onAddToCart: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val reviewViewModel: ReviewViewModel = viewModel()
    val reviewState by reviewViewModel.uiState.collectAsState()
    val addReviewState by reviewViewModel.addReviewState.collectAsState()

    var hasNavigatedOnSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(gameId) {
        if (gameId.isNotEmpty()) {
            try {
                viewModel.loadGame(gameId)
            } catch (e: Exception) {
                // Error manejado por el ViewModel
            }
            try {
                reviewViewModel.loadReviews(gameId)
            } catch (e: Exception) {
                // Error manejado por el ViewModel
            }
        }
    }

    LaunchedEffect(addReviewState) {
        if (addReviewState is com.tecsup.nexusmobile.presentation.viewmodel.AddReviewUiState.Success && !hasNavigatedOnSuccess) {
            hasNavigatedOnSuccess = true
            snackbarHostState.showSnackbar(
                message = "¡Reseña publicada exitosamente!",
                duration = SnackbarDuration.Short
            )
            reviewViewModel.resetAddReviewState()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { data ->
                    Snackbar(
                        modifier = Modifier.padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = data.visuals.message,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            )
        },
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
                            // Solo mostrar botón de agregar al carrito si el juego NO está en la biblioteca
                            if (!state.isInLibrary) {
                                AddToCartButton(
                                    game = state.game,
                                    onAddToCart = {
                                        // Lanzar coroutine para mostrar el snackbar
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = "\"${state.game.title}\" agregado al carrito",
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                        onAddToCart(state.game.id)
                                    },
                                    cartViewModel = cartViewModel
                                )
                            } else {
                                // Mostrar mensaje si el juego ya está en la biblioteca
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = androidx.compose.material.icons.Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Ya está en tu biblioteca",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
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

                        when (val reviewStateValue = reviewState) {
                            is com.tecsup.nexusmobile.presentation.viewmodel.ReviewUiState.Loading -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                            is com.tecsup.nexusmobile.presentation.viewmodel.ReviewUiState.Error -> {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                ) {
                                    Text(
                                        text = "Error al cargar reseñas: ${reviewStateValue.message}",
                                        modifier = Modifier.padding(16.dp),
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                            else -> {
                                ReviewSection(
                                    reviews = when (val state = reviewState) {
                                        is com.tecsup.nexusmobile.presentation.viewmodel.ReviewUiState.Success -> state.reviews
                                        else -> emptyList()
                                    },
                                    gameId = gameId,
                                    isGameInLibrary = when (val state = uiState) {
                                        is GameDetailUiState.Success -> state.isInLibrary
                                        else -> false
                                    },
                                    onAddReview = { comment, rating ->
                                        reviewViewModel.addReview(gameId, comment, rating)
                                    },
                                    isLoadingReview = addReviewState is com.tecsup.nexusmobile.presentation.viewmodel.AddReviewUiState.Loading
                                )
                            }
                        }
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