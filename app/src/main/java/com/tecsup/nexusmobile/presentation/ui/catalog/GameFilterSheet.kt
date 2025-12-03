package com.tecsup.nexusmobile.presentation.ui.catalog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class FilterState(
    val selectedCategory: String? = null,
    val selectedPlatform: String? = null,
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val onlyFree: Boolean = false,
    val onlyFeatured: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameFilterSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    filterState: FilterState,
    onFilterChange: (FilterState) -> Unit,
    availableCategories: List<String> = emptyList(),
    availablePlatforms: List<String> = emptyList(),
    modifier: Modifier = Modifier
) {
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            modifier = modifier
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filtros",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar"
                        )
                    }
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Categorías
                    if (availableCategories.isNotEmpty()) {
                        item {
                            Column {
                                Text(
                                    text = "Categoría",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilterChip(
                                        selected = filterState.selectedCategory == null,
                                        onClick = {
                                            onFilterChange(filterState.copy(selectedCategory = null))
                                        },
                                        label = { Text("Todas") }
                                    )
                                    availableCategories.forEach { category ->
                                        FilterChip(
                                            selected = filterState.selectedCategory == category,
                                            onClick = {
                                                onFilterChange(
                                                    filterState.copy(selectedCategory = category)
                                                )
                                            },
                                            label = { Text(category) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Plataformas
                    if (availablePlatforms.isNotEmpty()) {
                        item {
                            Column {
                                Text(
                                    text = "Plataforma",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilterChip(
                                        selected = filterState.selectedPlatform == null,
                                        onClick = {
                                            onFilterChange(filterState.copy(selectedPlatform = null))
                                        },
                                        label = { Text("Todas") }
                                    )
                                    availablePlatforms.forEach { platform ->
                                        FilterChip(
                                            selected = filterState.selectedPlatform == platform,
                                            onClick = {
                                                onFilterChange(
                                                    filterState.copy(selectedPlatform = platform)
                                                )
                                            },
                                            label = { Text(platform) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Opciones adicionales
                    item {
                        Column {
                            Text(
                                text = "Opciones",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = filterState.onlyFree,
                                    onClick = {
                                        onFilterChange(
                                            filterState.copy(onlyFree = !filterState.onlyFree)
                                        )
                                    },
                                    label = { Text("Solo Gratis") }
                                )
                                FilterChip(
                                    selected = filterState.onlyFeatured,
                                    onClick = {
                                        onFilterChange(
                                            filterState.copy(onlyFeatured = !filterState.onlyFeatured)
                                        )
                                    },
                                    label = { Text("Solo Destacados") }
                                )
                            }
                        }
                    }

                    // Botones de acción
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    onFilterChange(
                                        FilterState()
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Limpiar")
                            }
                            Button(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Aplicar")
                            }
                        }
                    }
                }
            }
        }
    }
}
