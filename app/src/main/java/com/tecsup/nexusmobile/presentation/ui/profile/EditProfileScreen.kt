package com.tecsup.nexusmobile.presentation.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.tecsup.nexusmobile.data.repository.ImageUploadRepository
import com.tecsup.nexusmobile.presentation.viewmodel.ProfileUiState
import com.tecsup.nexusmobile.presentation.viewmodel.ProfileViewModel
import com.tecsup.nexusmobile.presentation.viewmodel.UpdateProfileUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: ProfileViewModel = viewModel(),
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val imageRepository = remember { ImageUploadRepository() }
    val snackbarHostState = remember { SnackbarHostState() }

    var username by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var isUpdatingProfile by remember { mutableStateOf(false) }
    var isUploadingImage by remember { mutableStateOf(false) }

    // Estado para imagen
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var uploadedImageUrl by remember { mutableStateOf<String?>(null) }
    var uploadError by remember { mutableStateOf<String?>(null) }

    // Launcher para seleccionar imagen
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            uploadError = null
            isUploadingImage = true

            coroutineScope.launch {
                try {
                    imageRepository.uploadProfileImage(context, it)
                        .onSuccess { url ->
                            uploadedImageUrl = url
                            isUploadingImage = false
                            uploadError = null

                            // Mostrar mensaje de éxito
                            snackbarHostState.showSnackbar(
                                message = "✅ Imagen subida. Ahora haz clic en 'Guardar Cambios'",
                                duration = SnackbarDuration.Long
                            )
                        }
                        .onFailure { error ->
                            isUploadingImage = false
                            uploadError = error.message
                            selectedImageUri = null

                            // Mostrar error
                            snackbarHostState.showSnackbar(
                                message = "❌ ${error.message}",
                                duration = SnackbarDuration.Long
                            )
                        }
                } catch (e: Exception) {
                    isUploadingImage = false
                    uploadError = e.message
                    selectedImageUri = null

                    snackbarHostState.showSnackbar(
                        message = "❌ Error: ${e.message}",
                        duration = SnackbarDuration.Long
                    )
                }
            }
        }
    }

    // Cargar datos del usuario cuando se carga el estado
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is ProfileUiState.Success -> {
                username = state.user.username
                fullName = state.user.fullName
                email = state.user.email
                if (uploadedImageUrl == null) {
                    uploadedImageUrl = state.user.avatarUrl
                }
            }
            else -> {}
        }
    }

    // Mostrar mensaje de éxito y cerrar solo cuando se actualiza el perfil completo
    LaunchedEffect(updateState) {
        when (updateState) {
            is UpdateProfileUiState.Success -> {
                if (isUpdatingProfile) {
                    isUpdatingProfile = false
                    snackbarHostState.showSnackbar(
                        message = "✅ Perfil actualizado exitosamente",
                        duration = SnackbarDuration.Short
                    )
                    viewModel.loadProfile()
                    kotlinx.coroutines.delay(500)
                    onBackClick()
                }
            }
            is UpdateProfileUiState.Error -> {
                if (isUpdatingProfile) {
                    isUpdatingProfile = false
                    val error = (updateState as UpdateProfileUiState.Error).message
                    snackbarHostState.showSnackbar(
                        message = "❌ $error",
                        duration = SnackbarDuration.Long
                    )
                }
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Editar Perfil",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = if (data.visuals.message.startsWith("✅"))
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer,
                    contentColor = if (data.visuals.message.startsWith("✅"))
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    ) { padding ->
        when (val state = uiState) {
            is ProfileUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is ProfileUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Avatar con botón de cambiar imagen
                    Box(
                        modifier = Modifier.size(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Avatar circular
                        Surface(
                            modifier = Modifier
                                .size(140.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 3.dp,
                                    color = if (isUploadingImage)
                                        MaterialTheme.colorScheme.primary
                                    else if (uploadError != null)
                                        MaterialTheme.colorScheme.error
                                    else if (selectedImageUri != null)
                                        MaterialTheme.colorScheme.tertiary
                                    else
                                        MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                ),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                when {
                                    isUploadingImage -> {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(40.dp)
                                            )
                                            Text(
                                                text = "Subiendo...",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                    selectedImageUri != null || uploadedImageUrl != null -> {
                                        AsyncImage(
                                            model = selectedImageUri ?: uploadedImageUrl,
                                            contentDescription = "Avatar",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    else -> {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "Avatar",
                                            modifier = Modifier.size(60.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }

                        // Botón flotante para cambiar imagen
                        if (!isUploadingImage) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(45.dp)
                                    .clickable {
                                        imagePickerLauncher.launch("image/*")
                                    },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                shadowElevation = 4.dp
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Cambiar foto",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Texto de ayuda
                    Text(
                        text = if (isUploadingImage) {
                            "⏳ Subiendo imagen a Cloudinary..."
                        } else if (uploadError != null) {
                            " Error al subir imagen"
                        } else if (selectedImageUri != null && uploadedImageUrl != null) {
                            " Imagen lista. Haz clic en 'Guardar Cambios' abajo"
                        } else {
                            "Toca el ícono de cámara para cambiar tu foto"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (uploadError != null)
                            MaterialTheme.colorScheme.error
                        else if (selectedImageUri != null && uploadedImageUrl != null)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Divider()

                    // Formulario
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Email (solo lectura)
                        OutlinedTextField(
                            value = email,
                            onValueChange = { },
                            label = { Text("Correo electrónico") },
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        // Nombre de usuario
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Nombre de usuario") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = username.isBlank(),
                            enabled = !isUploadingImage && !isUpdatingProfile
                        )

                        // Nombre completo
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Nombre completo") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = fullName.isBlank(),
                            enabled = !isUploadingImage && !isUpdatingProfile
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Info de imagen subida
                    if (uploadError != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = " Error al subir imagen",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = uploadError!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                TextButton(
                                    onClick = {
                                        uploadError = null
                                        selectedImageUri = null
                                    }
                                ) {
                                    Text("Intentar de nuevo")
                                }
                            }
                        }
                    }

                    // Botón de guardar
                    val hasChanges = username != state.user.username ||
                            fullName != state.user.fullName ||
                            uploadedImageUrl != state.user.avatarUrl

                    Button(
                        onClick = {
                            if (username.isNotBlank() && fullName.isNotBlank()) {
                                isUpdatingProfile = true
                                viewModel.updateProfile(
                                    userId = state.user.id,
                                    username = username.trim(),
                                    fullName = fullName.trim(),
                                    avatarUrl = uploadedImageUrl
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = username.isNotBlank() &&
                                fullName.isNotBlank() &&
                                !isUploadingImage &&
                                !isUpdatingProfile &&
                                hasChanges
                    ) {
                        if (isUpdatingProfile) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Text("Guardando...")
                            }
                        } else {
                            Text(
                                "Guardar Cambios",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Mensaje de ayuda
                    if (!hasChanges && !isUploadingImage) {
                        Text(
                            text = "No hay cambios para guardar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            is ProfileUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Error al cargar el perfil",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = { viewModel.loadProfile() }) {
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
