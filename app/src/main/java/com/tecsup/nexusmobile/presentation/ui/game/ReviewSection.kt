package com.tecsup.nexusmobile.presentation.ui.game

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tecsup.nexusmobile.domain.model.Review

@Composable
fun ReviewSection(
    reviews: List<Review> = emptyList(),
    gameId: String = "",
    isGameInLibrary: Boolean = false,
    modifier: Modifier = Modifier,
    onAddReview: (String, Int) -> Unit = { _, _ -> },
    isLoadingReview: Boolean = false
) {
    var showAddReviewDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Reseñas",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (reviews.isNotEmpty()) {
                    Text(
                        text = "${reviews.size} reseñas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Botón para agregar reseña solo si el juego está en la biblioteca
                if (isGameInLibrary) {
                    IconButton(
                        onClick = { showAddReviewDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Agregar reseña",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        if (reviews.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No hay reseñas aún",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sé el primero en dejar una reseña",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // CAMBIO IMPORTANTE: Usar Column en lugar de LazyColumn
            // porque ya estamos dentro de un scroll (GameDetailScreen)
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                reviews.forEach { review ->
                    ReviewCard(review = review)
                }
            }
        }
    }

    // Diálogo para agregar reseña
    if (showAddReviewDialog) {
        AddReviewDialog(
            onDismiss = { showAddReviewDialog = false },
            onAddReview = { comment, rating ->
                onAddReview(comment, rating)
                showAddReviewDialog = false
            },
            isLoading = isLoadingReview
        )
    }
}

@Composable
private fun AddReviewDialog(
    onDismiss: () -> Unit,
    onAddReview: (String, Int) -> Unit,
    isLoading: Boolean
) {
    var commentText by remember { mutableStateOf("") }
    var selectedRating by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar Reseña") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Selector de rating
                Text(
                    text = "Calificación:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(5) { index ->
                        IconButton(
                            onClick = { selectedRating = index + 1 }
                        ) {
                            Text(
                                text = if (index < selectedRating) "★" else "☆",
                                style = MaterialTheme.typography.titleLarge,
                                color = if (index < selectedRating)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                        }
                    }
                }

                // Campo de comentario
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    label = { Text("Tu comentario") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4,
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (commentText.isNotBlank() && selectedRating > 0) {
                        onAddReview(commentText, selectedRating)
                    }
                },
                enabled = commentText.isNotBlank() && selectedRating > 0 && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Publicar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun ReviewCard(
    review: Review,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Avatar del usuario
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = review.userName.take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Column {
                        Text(
                            text = review.userName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (review.rating > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(5) { index ->
                                    Text(
                                        text = if (index < review.rating) "★" else "☆",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (index < review.rating)
                                            MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                    )
                                }
                            }
                        }
                    }
                }
                if (review.date.isNotEmpty()) {
                    Text(
                        text = review.date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (review.comment.isNotEmpty()) {
                Text(
                    text = review.comment,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}