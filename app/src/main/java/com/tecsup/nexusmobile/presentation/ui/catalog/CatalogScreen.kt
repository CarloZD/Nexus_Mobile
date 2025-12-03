package com.tecsup.nexusmobile.presentation.ui.catalog

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.tecsup.nexusmobile.R
import com.tecsup.nexusmobile.domain.model.Game
import com.tecsup.nexusmobile.presentation.viewmodel.CatalogUiState
import com.tecsup.nexusmobile.presentation.viewmodel.CatalogViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    viewModel: CatalogViewModel,
    onLogout: () -> Unit,
    onGameClick: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }
    var filterState by remember { mutableStateOf(FilterState()) }

    // Obtener categorías y plataformas únicas de los juegos
    val availableCategories = remember(uiState) {
        val currentState = uiState
        when (currentState) {
            is CatalogUiState.Success -> {
                currentState.games.mapNotNull { it.category }.distinct()
            }
            else -> emptyList()
        }
    }

    val availablePlatforms = remember(uiState) {
        val currentState = uiState
        when (currentState) {
            is CatalogUiState.Success -> {
                currentState.games.mapNotNull { it.platform }.distinct()
            }
            else -> emptyList()
        }
    }

    // Filtrar juegos según el estado del filtro
    val filteredGames = remember(uiState, filterState) {
        val currentState = uiState
        when (currentState) {
            is CatalogUiState.Success -> {
                currentState.games.filter { game ->
                    (filterState.selectedCategory == null || game.category == filterState.selectedCategory) &&
                    (filterState.selectedPlatform == null || game.platform == filterState.selectedPlatform) &&
                    (!filterState.onlyFree || game.isFree) &&
                    (!filterState.onlyFeatured || game.featured)
                }
            }
            else -> emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_nexus),
                            contentDescription = "Nexus Logo",
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Catálogo")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Filtros"
                        )
                    }
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Cerrar sesión"
                        )
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is CatalogUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is CatalogUiState.Success -> {
                if (filteredGames.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "No se encontraron juegos",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Intenta ajustar los filtros",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(onClick = { filterState = FilterState() }) {
                                Text("Limpiar filtros")
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        items(filteredGames) { game ->
                            GameCard(
                                game = game,
                                onClick = { onGameClick(game.id) }
                            )
                        }
                    }
                }
            }

            is CatalogUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = { viewModel.loadGames() }) {
                            Text("Reintentar")
                        }
                    }
                }
            }
        }
    }

    // Sheet de filtros
    GameFilterSheet(
        isVisible = showFilterSheet,
        onDismiss = { showFilterSheet = false },
        filterState = filterState,
        onFilterChange = { filterState = it },
        availableCategories = availableCategories,
        availablePlatforms = availablePlatforms
    )
}