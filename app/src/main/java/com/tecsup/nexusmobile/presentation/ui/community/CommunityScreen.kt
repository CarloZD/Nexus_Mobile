package com.tecsup.nexusmobile.presentation.ui.community

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.tecsup.nexusmobile.data.repository.ImageUploadRepository
import com.tecsup.nexusmobile.domain.model.Post
import com.tecsup.nexusmobile.presentation.viewmodel.CommunityViewModel
import com.tecsup.nexusmobile.presentation.viewmodel.CommunityUiState
import com.tecsup.nexusmobile.presentation.viewmodel.CreatePostUiState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    viewModel: CommunityViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val createPostState by viewModel.createPostState.collectAsState()
    val comments by viewModel.comments.collectAsState()

    var showCreatePostDialog by remember { mutableStateOf(false) }
    var expandedPostId by remember { mutableStateOf<String?>(null) }
    var selectedFilter by remember { mutableStateOf(FilterType.ALL) }

    LaunchedEffect(createPostState) {
        if (createPostState is CreatePostUiState.Success) {
            showCreatePostDialog = false
            viewModel.resetCreatePostState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "COMUNIDAD NEXUS",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState is CommunityUiState.Success) {
                            val posts = (uiState as CommunityUiState.Success).posts
                            Text(
                                "${posts.size} publicaciones",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    var showFilterMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Filtrar",
                            tint = if (selectedFilter != FilterType.ALL)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }
                    ) {
                        FilterType.values().forEach { filter ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(filter.icon)
                                        Text(filter.label)
                                    }
                                },
                                onClick = {
                                    selectedFilter = filter
                                    showFilterMenu = false
                                },
                                leadingIcon = {
                                    if (selectedFilter == filter) {
                                        Icon(Icons.Default.Check, null)
                                    }
                                }
                            )
                        }
                    }

                    IconButton(onClick = { showCreatePostDialog = true }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Crear post",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreatePostDialog = true },
                icon = { Icon(Icons.Default.Edit, "Crear") },
                text = { Text("Nueva Publicación") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is CommunityUiState.Loading -> {
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
                        CircularProgressIndicator()
                        Text(
                            "Cargando comunidad...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is CommunityUiState.Success -> {
                val filteredPosts = when (selectedFilter) {
                    FilterType.ALL -> state.posts
                    FilterType.POPULAR -> state.posts.sortedByDescending { it.likesCount }
                    FilterType.RECENT -> state.posts.sortedByDescending { it.createdAt }
                    FilterType.DISCUSSED -> state.posts.sortedByDescending { it.commentsCount }
                }

                if (filteredPosts.isEmpty()) {
                    EmptyCommunityView(
                        modifier = Modifier.padding(padding),
                        onCreatePost = { showCreatePostDialog = true }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (selectedFilter != FilterType.ALL) {
                            item {
                                FilterChip(
                                    selected = true,
                                    onClick = { selectedFilter = FilterType.ALL },
                                    label = {
                                        Text("${selectedFilter.icon} ${selectedFilter.label}")
                                    },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Quitar filtro",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                )
                            }
                        }

                        items(filteredPosts, key = { it.id }) { post ->
                            EnhancedPostCard(
                                post = post,
                                isLiked = viewModel.isPostLiked(post),
                                comments = comments[post.id] ?: emptyList(),
                                isExpanded = expandedPostId == post.id,
                                onLikeClick = { viewModel.toggleLike(post.id) },
                                onCommentClick = {
                                    if (expandedPostId == post.id) {
                                        viewModel.stopListeningToComments(post.id)
                                        expandedPostId = null
                                    } else {
                                        expandedPostId = post.id
                                        viewModel.loadComments(post.id)
                                    }
                                },
                                onAddComment = { content ->
                                    viewModel.addComment(post.id, content)
                                }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }

            is CommunityUiState.Error -> {
                ErrorCommunityView(
                    message = state.message,
                    onRetry = { viewModel.loadPosts() },
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }

    if (showCreatePostDialog) {
        val currentCreateState = createPostState
        EnhancedCreatePostDialog(
            onDismiss = {
                showCreatePostDialog = false
                viewModel.resetCreatePostState()
            },
            onCreatePost = { title, content, imageUrls ->
                viewModel.createPost(title, content, imageUrls)
            },
            isLoading = currentCreateState is CreatePostUiState.Loading,
            errorMessage = when (currentCreateState) {
                is CreatePostUiState.Error -> currentCreateState.message
                else -> null
            }
        )
    }
}

enum class FilterType(val label: String, val icon: String) {
    ALL("Todo", "📰"),
    POPULAR("Popular", "🔥"),
    RECENT("Reciente", "⏰"),
    DISCUSSED("Más comentados", "💬")
}

@Composable
fun EmptyCommunityView(
    modifier: Modifier = Modifier,
    onCreatePost: () -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "🎮",
                style = MaterialTheme.typography.displayLarge
            )
            Text(
                text = "¡Sé el primero!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "No hay publicaciones aún.\n¡Comparte algo con la comunidad!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onCreatePost,
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Crear Publicación")
            }
        }
    }
}

@Composable
fun ErrorCommunityView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = "Error al cargar",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Button(onClick = onRetry) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("Reintentar")
            }
        }
    }
}

// CONTINUACIÓN DE CommunityScreen.kt

@Composable
fun EnhancedPostCard(
    post: Post,
    isLiked: Boolean,
    comments: List<com.tecsup.nexusmobile.domain.model.Comment>,
    isExpanded: Boolean,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onAddComment: (String) -> Unit
) {
    var commentText by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isExpanded) 8.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header con gradiente
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    )
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Avatar mejorado
                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 4.dp
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (post.userAvatarUrl != null && post.userAvatarUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = post.userAvatarUrl,
                                    contentDescription = "Avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = post.userName.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = post.userName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = formatTimeAgo(post.createdAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // Título con ícono
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // Contenido
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Imágenes del post
            if (post.imageUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                post.imageUrls.forEach { imageUrl ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp, max = 400.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "Imagen del post",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Divider()

            // Acciones mejoradas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Like button
                Button(
                    onClick = onLikeClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLiked)
                            MaterialTheme.colorScheme.errorContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isLiked)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Like",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "${post.likesCount}",
                        fontWeight = FontWeight.Bold
                    )
                }

                // Comment button
                Button(
                    onClick = onCommentClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isExpanded)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isExpanded)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Comentarios",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "${post.commentsCount}",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Sección de comentarios
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(300)) + fadeIn(),
                exit = shrinkVertically(animationSpec = tween(300)) + fadeOut()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Divider()

                    // Header de comentarios
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.ChatBubble,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Comentarios (${comments.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Lista de comentarios
                    if (comments.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            comments.forEach { comment ->
                                EnhancedCommentItem(comment = comment)
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "💬 Sé el primero en comentar",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Input de comentario
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = commentText,
                                onValueChange = { commentText = it },
                                modifier = Modifier.weight(1f),
                                placeholder = {
                                    Text(
                                        "Escribe un comentario...",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                maxLines = 3,
                                shape = RoundedCornerShape(24.dp)
                            )
                            IconButton(
                                onClick = {
                                    if (commentText.isNotBlank()) {
                                        onAddComment(commentText.trim())
                                        commentText = ""
                                    }
                                },
                                enabled = commentText.isNotBlank(),
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        if (commentText.isNotBlank())
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant,
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    Icons.Default.Send,
                                    contentDescription = "Enviar",
                                    tint = if (commentText.isNotBlank())
                                        MaterialTheme.colorScheme.onPrimary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedCommentItem(comment: com.tecsup.nexusmobile.domain.model.Comment) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar del comentario
            Surface(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (comment.userAvatarUrl != null && comment.userAvatarUrl.isNotEmpty()) {
                        AsyncImage(
                            model = comment.userAvatarUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = comment.userName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = comment.userName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatTimeAgo(comment.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = comment.content,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

private fun formatTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Ahora"
        diff < 3600_000 -> "${diff / 60_000}m"
        diff < 86400_000 -> "${diff / 3600_000}h"
        diff < 604800_000 -> "${diff / 86400_000}d"
        else -> {
            val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

// CONTINUACIÓN FINAL DE CommunityScreen.kt

@Composable
fun EnhancedCreatePostDialog(
    onDismiss: () -> Unit,
    onCreatePost: (String, String, List<String>) -> Unit,
    isLoading: Boolean,
    errorMessage: String?
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    val maxTitleLength = 100
    val maxContentLength = 1000

    // Estados para imagen
    val context = LocalContext.current
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploadingImage by remember { mutableStateOf(false) }
    var uploadedImageUrl by remember { mutableStateOf<String?>(null) }
    var imageUploadError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Launcher para seleccionar imagen
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            imageUploadError = null
            isUploadingImage = true

            coroutineScope.launch {
                try {
                    val imageRepository = ImageUploadRepository()
                    imageRepository.uploadPostImage(context, it)
                        .onSuccess { url ->
                            uploadedImageUrl = url
                            isUploadingImage = false
                        }
                        .onFailure { error ->
                            isUploadingImage = false
                            imageUploadError = error.message
                            selectedImageUri = null
                        }
                } catch (e: Exception) {
                    isUploadingImage = false
                    imageUploadError = e.message
                    selectedImageUri = null
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Create,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Column {
                Text(
                    "Nueva Publicación",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Comparte con la comunidad Nexus",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Título
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        if (it.length <= maxTitleLength) title = it
                    },
                    label = { Text("Título") },
                    placeholder = { Text("¿De qué quieres hablar?") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading && !isUploadingImage,
                    supportingText = {
                        Text(
                            "${title.length}/$maxTitleLength",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Title, null)
                    }
                )

                // Contenido
                OutlinedTextField(
                    value = content,
                    onValueChange = {
                        if (it.length <= maxContentLength) content = it
                    },
                    label = { Text("Contenido") },
                    placeholder = { Text("Escribe aquí tu publicación...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5,
                    maxLines = 10,
                    enabled = !isLoading && !isUploadingImage,
                    supportingText = {
                        Text(
                            "${content.length}/$maxContentLength",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                )

                // Sección de imagen
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Imagen (opcional)",
                                style = MaterialTheme.typography.labelMedium
                            )
                            if (selectedImageUri != null && !isUploadingImage) {
                                IconButton(
                                    onClick = {
                                        selectedImageUri = null
                                        uploadedImageUrl = null
                                        imageUploadError = null
                                    }
                                ) {
                                    Icon(Icons.Default.Close, "Eliminar imagen")
                                }
                            }
                        }

                        when {
                            isUploadingImage -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Subiendo imagen...")
                                }
                            }
                            selectedImageUri != null -> {
                                AsyncImage(
                                    model = selectedImageUri,
                                    contentDescription = "Vista previa",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            else -> {
                                Button(
                                    onClick = { imagePickerLauncher.launch("image/*") },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isLoading
                                ) {
                                    Icon(Icons.Default.Image, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Seleccionar Imagen")
                                }
                            }
                        }

                        if (imageUploadError != null) {
                            Text(
                                text = "❌ Error: $imageUploadError",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && content.isNotBlank()) {
                        val imageUrls = uploadedImageUrl?.let { listOf(it) } ?: emptyList()
                        onCreatePost(title.trim(), content.trim(), imageUrls)
                    }
                },
                enabled = title.isNotBlank() && content.isNotBlank() && !isLoading && !isUploadingImage
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.Send, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Publicar")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading && !isUploadingImage
            ) {
                Text("Cancelar")
            }
        }
    )
}