package com.tecsup.nexusmobile.presentation.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.nexusmobile.R
import com.tecsup.nexusmobile.presentation.viewmodel.AuthState
import com.tecsup.nexusmobile.presentation.viewmodel.AuthViewModel

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val authState = viewModel.authState
    val scrollState = rememberScrollState()

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                onRegisterSuccess()
                viewModel.resetAuthState()
            }
            is AuthState.Error -> {
                errorMessage = authState.message
                showError = true
            }
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Image(
            painter = painterResource(id = R.drawable.wallpapper),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Image(
                painter = painterResource(id = R.drawable.logo_nexus),
                contentDescription = "Nexus Logo",
                modifier = Modifier
                    .size(80.dp)
                    .padding(bottom = 16.dp)
            )

            Text(
                text = "CREAR CUENTA",
                fontFamily = OrbitronFont,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Únete a la comunidad Nexus",
                fontFamily = OrbitronFont,
                fontSize = 12.sp,
                color = Color.LightGray,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            NexusTextField(
                value = fullName,
                onValueChange = { fullName = it },
                placeholder = "nombre completo",
                icon = Icons.Default.Person
            )

            Spacer(modifier = Modifier.height(12.dp))

            NexusTextField(
                value = username,
                onValueChange = { username = it },
                placeholder = "nombre de usuario",
                icon = Icons.Default.AccountCircle
            )

            Spacer(modifier = Modifier.height(12.dp))

            NexusTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = "correo electrónico",
                icon = Icons.Default.Email,
                keyboardType = KeyboardType.Email
            )

            Spacer(modifier = Modifier.height(12.dp))

            NexusPasswordField(
                value = password,
                onValueChange = { password = it },
                placeholder = "contraseña",
                isVisible = passwordVisible,
                onToggleVisibility = { passwordVisible = !passwordVisible }
            )

            Spacer(modifier = Modifier.height(12.dp))

            NexusPasswordField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                placeholder = "confirmar contraseña",
                isVisible = confirmPasswordVisible,
                onToggleVisibility = { confirmPasswordVisible = !confirmPasswordVisible },
                isError = confirmPassword.isNotEmpty() && password != confirmPassword
            )

            if (confirmPassword.isNotEmpty() && password != confirmPassword) {
                Text(
                    text = "Las contraseñas no coinciden",
                    color = Color(0xFFFF5555),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable(
                        enabled = authState !is AuthState.Loading,
                        onClick = {
                            when {
                                fullName.isBlank() || username.isBlank() ||
                                        email.isBlank() || password.isBlank() -> {
                                    errorMessage = "Por favor completa todos los campos"
                                    showError = true
                                }
                                password != confirmPassword -> {
                                    errorMessage = "Las contraseñas no coinciden"
                                    showError = true
                                }
                                password.length < 6 -> {
                                    errorMessage = "La contraseña debe tener al menos 6 caracteres"
                                    showError = true
                                }
                                else -> {
                                    viewModel.register(email, password, username, fullName)
                                }
                            }
                        }
                    )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.buttonimage),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                if (authState is AuthState.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = "REGISTRARSE",
                        color = Color.White,
                        fontFamily = OrbitronFont,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(20))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { onNavigateToLogin() }
            ) {
                Text(
                    text = "¿Ya tienes cuenta? Inicia sesión",
                    color = Color.LightGray,
                    fontFamily = OrbitronFont,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }

            if (showError) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage,
                    color = Color(0xFFFF5555),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun NexusTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Color.Gray, fontSize = 12.sp) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = Color.White) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(50),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xCC000000),
            unfocusedContainerColor = Color(0x80000000),
            focusedBorderColor = Color(0xFF7C3AED),
            unfocusedBorderColor = Color.DarkGray,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Color(0xFF7C3AED)
        ),
        textStyle = LocalTextStyle.current.copy(fontFamily = OrbitronFont, fontSize = 14.sp)
    )
}

@Composable
fun NexusPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isVisible: Boolean,
    onToggleVisibility: () -> Unit,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Color.Gray, fontSize = 12.sp) },
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White) },
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Text(text = if (isVisible) "👁️" else "👁️‍🗨️", fontSize = 18.sp)
            }
        },
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(50),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xCC000000),
            unfocusedContainerColor = Color(0x80000000),
            focusedBorderColor = if (isError) Color.Red else Color(0xFF7C3AED),
            unfocusedBorderColor = if (isError) Color.Red else Color.DarkGray,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Color(0xFF7C3AED)
        ),
        textStyle = LocalTextStyle.current.copy(fontFamily = OrbitronFont, fontSize = 14.sp)
    )
}