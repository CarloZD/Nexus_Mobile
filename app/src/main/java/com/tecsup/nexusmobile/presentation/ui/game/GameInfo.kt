package com.tecsup.nexusmobile.presentation.ui.game

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tecsup.nexusmobile.domain.model.Game

@Composable
fun GameInfo(
    game: Game,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Descripción
        if (game.description.isNotEmpty()) {
            Column {
                Text(
                    text = "Descripción",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = game.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Divider()

        // Información del juego
        Column {
            Text(
                text = "Información",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            InfoRow(label = "Desarrollador", value = game.developer)
            InfoRow(label = "Editor", value = game.publisher)
            InfoRow(label = "Plataforma", value = game.platform)
            InfoRow(label = "Categoría", value = game.category)
            if (game.releaseDate.isNotEmpty()) {
                InfoRow(label = "Fecha de lanzamiento", value = game.releaseDate)
            }
            if (game.stock > 0) {
                InfoRow(label = "Stock disponible", value = "${game.stock} unidades")
            }
        }

        // Géneros
        if (game.genres.isNotEmpty()) {
            Column {
                Text(
                    text = "Géneros",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    game.genres.forEach { genre ->
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = genre,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        // Capturas de pantalla
        if (game.screenshots.isNotEmpty()) {
            Column {
                Text(
                    text = "Capturas de pantalla",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(game.screenshots) { screenshot ->
                        Card(
                            modifier = Modifier
                                .width(280.dp)
                                .height(160.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            AsyncImage(
                                model = screenshot,
                                contentDescription = "Captura de pantalla",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    if (value.isNotEmpty()) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
