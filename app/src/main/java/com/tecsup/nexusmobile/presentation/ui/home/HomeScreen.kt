package com.tecsup.nexusmobile.presentation.ui.home

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.tecsup.nexusmobile.R
import com.tecsup.nexusmobile.domain.model.Game
import com.tecsup.nexusmobile.presentation.ui.catalog.GameCard
import com.tecsup.nexusmobile.presentation.viewmodel.CatalogViewModel
import com.tecsup.nexusmobile.presentation.viewmodel.CatalogUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: CatalogViewModel = viewModel(),
    onNavigateToCart: () -> Unit,
    onNavigateToGameDetail: (String) -> Unit,
    onNavigateToChatbot: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Filtrar juegos según la búsqueda Y categoría
    val filteredGames = remember(uiState, searchQuery, selectedCategory) {
        when (val state = uiState) {
            is CatalogUiState.Success -> {
                var games = state.games
                
                // Filtrar por búsqueda
                if (searchQuery.isNotBlank()) {
                    games = games.filter { game ->
                        game.title.contains(searchQuery, ignoreCase = true) ||
                                game.category.contains(searchQuery, ignoreCase = true) ||
                                game.developer.contains(searchQuery, ignoreCase = true)
                    }
                }
                
                // Filtrar por categoría
                selectedCategory?.let { category ->
                    // Convertir la categoría seleccionada (español) a inglés (BD)
                    val categoryInDb = categoryMapReverse[category] ?: category.uppercase()
                    Log.d("HomeScreen", "Filtrando por categoría: '$category' -> BD: '$categoryInDb'")
                    
                    games = games.filter { game ->
                        val gameCategory = game.category.uppercase().trim()
                        // Comparar directamente o a través del mapeo
                        val matches = gameCategory.equals(categoryInDb.uppercase().trim(), ignoreCase = true) ||
                                // También verificar si la categoría del juego mapeada coincide
                                (categoryMap[gameCategory] == category)
                        
                        if (matches) {
                            Log.d("HomeScreen", "✓ Juego '${game.title}' coincide - Categoría BD: '$gameCategory'")
                        }
                        matches
                    }
                    Log.d("HomeScreen", "Total juegos filtrados encontrados: ${games.size}")
                }
                
                games
            }
            else -> emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        // Campo de búsqueda
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 8.dp),
                            placeholder = { Text("Buscar juegos...") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Limpiar")
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Search
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    keyboardController?.hide()
                                }
                            )
                        )
                    } else {
                        // Logo y título normal
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
                            Text(
                                "NEXUS",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isSearchActive = !isSearchActive
                        if (!isSearchActive) {
                            searchQuery = ""
                        }
                    }) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = if (isSearchActive) "Cerrar búsqueda" else "Buscar"
                        )
                    }
                    IconButton(onClick = onNavigateToChatbot) {
                        Icon(Icons.Default.ChatBubble, contentDescription = "Chatbot")
                    }
                    IconButton(onClick = onNavigateToCart) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "Carrito")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
                // Debug: Verificar cuántos juegos hay y sus categorías
                LaunchedEffect(state.games.size, selectedCategory, searchQuery) {
                    Log.d("HomeScreen", "Total juegos cargados: ${state.games.size}")
                    Log.d("HomeScreen", "Juegos filtrados: ${filteredGames.size}")
                    Log.d("HomeScreen", "Categoría seleccionada: $selectedCategory")
                    Log.d("HomeScreen", "Búsqueda activa: $isSearchActive, Query: $searchQuery")
                    
                    // Mostrar todas las categorías únicas de los juegos
                    val uniqueCategories = state.games.map { it.category.uppercase().trim() }.distinct().sorted()
                    Log.d("HomeScreen", "Categorías únicas en juegos: $uniqueCategories")
                    
                    if (state.games.isNotEmpty()) {
                        state.games.take(5).forEach { game ->
                            Log.d("HomeScreen", "Juego: ${game.title}, Categoría BD: '${game.category}'")
                        }
                    }
                }
                
                if (isSearchActive && searchQuery.isNotEmpty()) {
                    // Vista de resultados de búsqueda
                    SearchResultsView(
                        games = filteredGames,
                        searchQuery = searchQuery,
                        onGameClick = onNavigateToGameDetail,
                        modifier = Modifier.padding(padding)
                    )
                } else {
                    // Vista normal del Home
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        // Banner principal
                        item {
                            FeaturedBanner(
                                game = state.games.firstOrNull { it.featured }
                                    ?: state.games.firstOrNull(),
                                onClick = { game ->
                                    game?.let { onNavigateToGameDetail(it.id) }
                                }
                            )
                        }

                        // Categorías con funcionalidad
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            // Mostrar todas las categorías posibles de la BD
                            CategoryChips(
                                selectedCategory = selectedCategory,
                                onCategorySelected = { category ->
                                    selectedCategory = if (selectedCategory == category) null else category
                                }
                            )
                        }

                        // Mostrar indicador de filtro activo
                        if (selectedCategory != null) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${filteredGames.size} juegos de ${selectedCategory ?: ""}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    TextButton(
                                        onClick = { selectedCategory = null }
                                    ) {
                                        Text("Limpiar filtro")
                                    }
                                }
                            }
                        }

                        // Ofertas especiales o juegos filtrados
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = if (selectedCategory != null) 
                                    "JUEGOS DE ${selectedCategory?.uppercase()}"
                                else 
                                    "OFERTAS ESPECIALES",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Si hay categoría seleccionada, mostrar juegos en vertical
                        if (selectedCategory != null) {
                            item {
                                if (filteredGames.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "No hay juegos disponibles",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "No encontramos juegos de $selectedCategory",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else {
                                    // Mostrar juegos filtrados en vertical
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        filteredGames.forEach { game ->
                                            VerticalGameCard(
                                                game = game,
                                                onClick = { onNavigateToGameDetail(game.id) }
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // Sin filtro: mostrar ofertas especiales en horizontal
                            item {
                                if (state.games.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "No hay juegos disponibles",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "No hay juegos en la base de datos",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else {
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(state.games.take(10)) { game ->
                                            GameCard(
                                                game = game,
                                                onClick = { onNavigateToGameDetail(game.id) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Noticias solo si no hay filtro activo
                        if (selectedCategory == null) {
                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = "NOTICIAS Y ACTUALIZACIONES",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            item {
                                NewsSection()
                                Spacer(modifier = Modifier.height(16.dp))
                            }
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
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Error al cargar juegos",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadGames() }) {
                            Text("Reintentar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultsView(
    games: List<Game>,
    searchQuery: String,
    onGameClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = if (games.isEmpty())
                "No se encontraron resultados para \"$searchQuery\""
            else
                "${games.size} resultado(s) para \"$searchQuery\"",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (games.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🔍",
                        style = MaterialTheme.typography.displayLarge
                    )
                    Text(
                        text = "No encontramos juegos con ese nombre",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Intenta con otro término de búsqueda",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(games) { game ->
                    SearchGameCard(
                        game = game,
                        onClick = { onGameClick(game.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun SearchGameCard(
    game: Game,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // Imagen
            AsyncImage(
                model = game.headerImage ?: game.backgroundImage,
                contentDescription = game.title,
                modifier = Modifier
                    .width(120.dp)
                    .fillMaxHeight(),
                contentScale = ContentScale.Crop
            )

            // Información
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = game.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = game.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (game.isFree) "GRATIS" else "S/${game.price}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (game.isFree)
                            Color(0xFF00FF00)
                        else
                            MaterialTheme.colorScheme.secondary
                    )

                    if (game.rating > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "★",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFFFD700)
                            )
                            Text(
                                text = String.format("%.1f", game.rating),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FeaturedBanner(
    game: Game?,
    onClick: (Game?) -> Unit
) {
    if (game == null) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(horizontal = 16.dp)
            .clickable { onClick(game) },
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Imagen de fondo
            AsyncImage(
                model = game.headerImage ?: game.backgroundImage,
                contentDescription = game.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Gradiente oscuro
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            )

            // Contenido
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = game.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xFF00FF00).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (game.isFree) "GRATIS" else "-${((game.price * 0.2).toInt())}%",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF00FF00),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    if (!game.isFree) {
                        Text(
                            text = "S/${game.price}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// Mapeo de categorías de BD (inglés) a español para la UI
private val categoryMap = mapOf(
    "ACTION" to "ACCIÓN",
    "ADVENTURE" to "AVENTURA",
    "RPG" to "RPG",
    "STRATEGY" to "ESTRATEGIA",
    "SPORTS" to "DEPORTES",
    "SIMULATION" to "SIMULACIÓN",
    "RACING" to "CARRERAS",
    "PUZZLE" to "PUZZLE",
    "HORROR" to "TERROR",
    "INDIE" to "INDIE"
)

// Mapeo inverso: español a inglés (BD)
private val categoryMapReverse = categoryMap.entries.associate { (k, v) -> v to k }

@Composable
fun CategoryChips(
    selectedCategory: String?,
    onCategorySelected: (String) -> Unit,
    availableCategories: List<String> = emptyList()
) {
    // Mostrar todas las categorías posibles de la BD en español
    val categories = categoryMap.values.toList()

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selectedCategory == category) 
                            FontWeight.Bold 
                        else 
                            FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                ),
                border = if (selectedCategory == category) {
                    FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = true,
                        borderColor = MaterialTheme.colorScheme.primary,
                        selectedBorderColor = MaterialTheme.colorScheme.primary,
                        borderWidth = 2.dp,
                        selectedBorderWidth = 2.dp
                    )
                } else {
                    FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = false
                    )
                }
            )
        }
    }
}

@Composable
fun VerticalGameCard(
    game: Game,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // Imagen del juego
            AsyncImage(
                model = game.headerImage ?: game.backgroundImage,
                contentDescription = game.title,
                modifier = Modifier
                    .width(140.dp)
                    .fillMaxHeight(),
                contentScale = ContentScale.Crop
            )

            // Información del juego
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = game.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = game.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (game.isFree) "GRATIS" else "S/${game.price}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (game.isFree)
                            Color(0xFF00FF00)
                        else
                            MaterialTheme.colorScheme.secondary
                    )

                    if (game.rating > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "★",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFFFD700)
                            )
                            Text(
                                text = String.format("%.1f", game.rating),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewsSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(60.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "📰",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Últimas noticias de la comunidad",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Descubre las últimas actualizaciones",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}