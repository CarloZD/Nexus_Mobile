package com.tecsup.nexusmobile.presentation.ui.game

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tecsup.nexusmobile.domain.model.Game

@Composable
fun AddToCartButton(
    game: Game,
    onAddToCart: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onAddToCart,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = enabled && game.stock > 0 && !game.isFree,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (game.isFree) {
                    "Juego Gratis"
                } else if (game.stock <= 0) {
                    "Sin Stock"
                } else {
                    "Agregar al Carrito - S/${game.price}"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
