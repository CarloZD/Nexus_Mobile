package com.tecsup.nexusmobile.presentation.ui.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tecsup.nexusmobile.domain.model.Game

@Composable
fun GameCard(
    game: Game,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(160.dp)
            .height(240.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Imagen del juego
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                AsyncImage(
                    model = game.headerImage ?: game.backgroundImage,
                    contentDescription = game.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Badge de descuento o gratis
                if (game.isFree) {
                    Surface(
                        color = Color(0xFF00FF00),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "GRATIS",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                } else if (game.price > 0) {
                    Surface(
                        color = Color(0xFF00FF00),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "-${((game.price * 0.2).toInt())}%",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }

            // Información del juego
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = game.title,
                    style = MaterialTheme.typography.titleSmall,
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
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (game.isFree) "GRATIS" else "S/${game.price}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (game.isFree) Color(0xFF00FF00)
                        else MaterialTheme.colorScheme.secondary
                    )
                    if (game.rating > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "★",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFFD700)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = String.format("%.1f", game.rating),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
