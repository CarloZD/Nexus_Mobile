package com.tecsup.nexusmobile.presentation.ui.checkout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tecsup.nexusmobile.presentation.viewmodel.CartUiState
import com.tecsup.nexusmobile.presentation.viewmodel.CartViewModel
import com.tecsup.nexusmobile.presentation.viewmodel.CheckoutUiState
import com.tecsup.nexusmobile.presentation.viewmodel.CheckoutViewModel

enum class PaymentMethod {
    CREDIT_CARD,
    YAPE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    onNavigateBack: () -> Unit,
    onPaymentSuccess: () -> Unit,
    cartViewModel: CartViewModel = viewModel(),
    checkoutViewModel: CheckoutViewModel = viewModel()
) {
    val cartState by cartViewModel.uiState.collectAsState()
    val checkoutState by checkoutViewModel.uiState.collectAsState()
    var selectedPaymentMethod by remember { mutableStateOf<PaymentMethod?>(null) }

    LaunchedEffect(checkoutState) {
        if (checkoutState is CheckoutUiState.Success) {
            onPaymentSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Procesar Pago",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        when (val cart = cartState) {
            is CartUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Resumen del pedido
                    OrderSummaryCard(
                        itemCount = cart.cart.items.size,
                        total = cart.cart.total
                    )

                    // Selección de método de pago
                    Text(
                        text = "Método de Pago",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    PaymentMethodCard(
                        icon = Icons.Default.CreditCard,
                        title = "Tarjeta de Crédito/Débito",
                        description = "Visa, Mastercard, American Express",
                        isSelected = selectedPaymentMethod == PaymentMethod.CREDIT_CARD,
                        onClick = { selectedPaymentMethod = PaymentMethod.CREDIT_CARD }
                    )

                    PaymentMethodCard(
                        icon = Icons.Default.Phone,
                        title = "Yape",
                        description = "Pago mediante código QR",
                        isSelected = selectedPaymentMethod == PaymentMethod.YAPE,
                        onClick = { selectedPaymentMethod = PaymentMethod.YAPE }
                    )

                    when (selectedPaymentMethod) {
                        PaymentMethod.CREDIT_CARD -> {
                            CreditCardForm(
                                onPay = { cardNumber, cvv, expiryDate, cardHolder ->
                                    checkoutViewModel.processPayment(
                                        cart = cart.cart,
                                        paymentMethod = "CREDIT_CARD",
                                        paymentDetails = mapOf(
                                            "cardNumber" to cardNumber,
                                            "cvv" to cvv,
                                            "expiryDate" to expiryDate,
                                            "cardHolder" to cardHolder
                                        )
                                    )
                                },
                                isLoading = checkoutState is CheckoutUiState.Loading
                            )
                        }
                        PaymentMethod.YAPE -> {
                            YapePaymentForm(
                                amount = cart.cart.total,
                                onPay = { phoneNumber ->
                                    checkoutViewModel.processPayment(
                                        cart = cart.cart,
                                        paymentMethod = "YAPE",
                                        paymentDetails = mapOf("phoneNumber" to phoneNumber)
                                    )
                                },
                                isLoading = checkoutState is CheckoutUiState.Loading
                            )
                        }
                        null -> {
                        }
                    }

                    // Mostrar error si hay
                    if (checkoutState is CheckoutUiState.Error) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = (checkoutState as CheckoutUiState.Error).message,
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun OrderSummaryCard(
    itemCount: Int,
    total: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Resumen del Pedido",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Divider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Artículos:")
                Text("$itemCount juegos")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Total a pagar:",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "S/ ${String.format("%.2f", total)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun PaymentMethodCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(32.dp),
                tint = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
        }
    }
}

@Composable
fun CreditCardForm(
    onPay: (String, String, String, String) -> Unit,
    isLoading: Boolean
) {
    var cardNumber by remember { mutableStateOf("") }
    var cardHolder by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = cardNumber,
            onValueChange = { if (it.length <= 16) cardNumber = it },
            label = { Text("Número de Tarjeta") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("1234 5678 9012 3456") }
        )

        OutlinedTextField(
            value = cardHolder,
            onValueChange = { cardHolder = it },
            label = { Text("Nombre del Titular") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = expiryDate,
                onValueChange = { if (it.length <= 5) expiryDate = it },
                label = { Text("Fecha Exp.") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("MM/YY") }
            )

            OutlinedTextField(
                value = cvv,
                onValueChange = { if (it.length <= 3) cvv = it },
                label = { Text("CVV") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("123") }
            )
        }

        Button(
            onClick = {
                if (cardNumber.isNotBlank() && cardHolder.isNotBlank() &&
                    expiryDate.isNotBlank() && cvv.isNotBlank()) {
                    onPay(cardNumber, cvv, expiryDate, cardHolder)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading && cardNumber.isNotBlank() &&
                    cardHolder.isNotBlank() && expiryDate.isNotBlank() && cvv.isNotBlank()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    "Pagar Ahora",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun YapePaymentForm(
    amount: Double,
    onPay: (String) -> Unit,
    isLoading: Boolean
) {
    var phoneNumber by remember { mutableStateOf("") }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Simulación de código QR
        Card(
            modifier = Modifier.size(200.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "📱",
                        style = MaterialTheme.typography.displayLarge
                    )
                    Text(
                        text = "Código QR Yape",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "S/ ${String.format("%.2f", amount)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Text(
            text = "Escanea el código con tu app Yape",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Divider()

        Text(
            text = "O ingresa tu número de celular",
            style = MaterialTheme.typography.bodyMedium
        )

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { if (it.length <= 9) phoneNumber = it },
            label = { Text("Número de Celular") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("999 999 999") }
        )

        Button(
            onClick = { onPay(phoneNumber) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading && phoneNumber.length == 9
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    "Confirmar Pago",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}