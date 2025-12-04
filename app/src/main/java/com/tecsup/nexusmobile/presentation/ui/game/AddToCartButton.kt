package com.tecsup.nexusmobile.presentation.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tecsup.nexusmobile.domain.model.Game
import com.tecsup.nexusmobile.presentation.viewmodel.CartViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AddToCartButton(
    game: Game,
    onAddToCart: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    cartViewModel: CartViewModel = viewModel()
) {
    var isAdding by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }

    // Animación de éxito
    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            delay(2000)
            showSuccess = false
        }
    }

    Button(
        onClick = {
            if (!showSuccess && !isAdding) {
                isAdding = true
                cartViewModel.addToCart(
                    gameId = game.id,
                    gameTitle = game.title,
                    gameImage = game.headerImage,
                    price = game.price
                )
                // Simular pequeño delay para el feedback visual
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                    delay(500)
                    isAdding = false
                    showSuccess = true
                    onAddToCart()
                }
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = enabled && game.stock > 0 && !game.isFree && !isAdding && !showSuccess,
        colors = ButtonDefaults.buttonColors(
            containerColor = when {
                showSuccess -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.primary
            }
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            when {
                isAdding -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Agregando...")
                }
                showSuccess -> {
                    AnimatedVisibility(
                        visible = showSuccess,
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "¡Agregado!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                game.isFree -> {
                    Text("Juego Gratis")
                }
                game.stock <= 0 -> {
                    Text("Sin Stock")
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Agregar al Carrito - S/${game.price}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}