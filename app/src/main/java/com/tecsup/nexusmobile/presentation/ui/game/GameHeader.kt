package com.tecsup.nexusmobile.presentation.ui.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tecsup.nexusmobile.domain.model.Game

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameHeader(
    game: Game,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        // Imagen de fondo
        AsyncImage(
            model = game.backgroundImage ?: game.headerImage,
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
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
        )

        // Botón de retroceso
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = Color.White,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        // Información del juego
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = game.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            if (game.shortDescription.isNotEmpty()) {
                Text(
                    text = game.shortDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 2
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (game.rating > 0) {
                    Surface(
                        color = Color(0xFFFFD700).copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "★",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFFFD700)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = String.format("%.1f", game.rating),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (game.isFree) {
                    Surface(
                        color = Color(0xFF00FF00).copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "GRATIS",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00FF00)
                        )
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "S/${game.price}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
