package com.tecsup.nexusmobile.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tecsup.nexusmobile.presentation.ui.cart.CartScreen
import com.tecsup.nexusmobile.presentation.ui.checkout.CheckoutScreen
import com.tecsup.nexusmobile.presentation.ui.community.CommunityScreen
import com.tecsup.nexusmobile.presentation.ui.game.GameDetailScreen
import com.tecsup.nexusmobile.presentation.ui.home.HomeScreen
import com.tecsup.nexusmobile.presentation.ui.library.LibraryScreen
import com.tecsup.nexusmobile.presentation.ui.profile.AboutScreen
import com.tecsup.nexusmobile.presentation.ui.profile.EditProfileScreen
import com.tecsup.nexusmobile.presentation.ui.profile.HelpSupportScreen
import com.tecsup.nexusmobile.presentation.ui.profile.ProfileScreen

// Rutas de navegación
sealed class BottomNavScreen(val route: String, val title: String, val icon: ImageVector) {
    object Home : BottomNavScreen("home", "TIENDA", Icons.Default.Home)
    object Library : BottomNavScreen("library", "BIBLIOTECA", Icons.Default.List)
    object Community : BottomNavScreen("community", "COMUNIDAD", Icons.Default.Menu)
    object Profile : BottomNavScreen("profile", "PERFIL", Icons.Default.Person)
}

object GameDetailRoute {
    const val route = "game_detail/{gameId}"
    fun createRoute(gameId: String) = "game_detail/$gameId"
}

object CartRoute {
    const val route = "cart"
}

object CheckoutRoute {
    const val route = "checkout"
}

object EditProfileRoute {
    const val route = "edit_profile"
}

object AboutRoute {
    const val route = "about"
}

object HelpSupportRoute {
    const val route = "help_support"
}

object PaymentSuccessRoute {
    const val route = "payment_success"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val items = listOf(
        BottomNavScreen.Home,
        BottomNavScreen.Library,
        BottomNavScreen.Community,
        BottomNavScreen.Profile
    )

    Scaffold(
        bottomBar = {
            // Mostrar bottom bar en las pantallas principales y en GameDetailScreen
            val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
            val isMainScreen = items.any { it.route == currentRoute }
            val isGameDetail = currentRoute?.startsWith("game_detail") == true
            val shouldShowBottomBar = isMainScreen || isGameDetail

            if (shouldShowBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            selected = currentDestination?.hierarchy?.any {
                                it.route == screen.route
                            } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = BottomNavScreen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(BottomNavScreen.Home.route) {
                HomeScreen(
                    onNavigateToCart = {
                        navController.navigate(CartRoute.route)
                    },
                    onNavigateToGameDetail = { gameId ->
                        navController.navigate(GameDetailRoute.createRoute(gameId))
                    }
                )
            }

            composable(BottomNavScreen.Library.route) {
                LibraryScreen(
                    onGameClick = { gameId ->
                        navController.navigate(GameDetailRoute.createRoute(gameId))
                    }
                )
            }

            composable(BottomNavScreen.Community.route) {
                CommunityScreen()
            }

            composable(BottomNavScreen.Profile.route) {
                ProfileScreen(
                    onLogout = onLogout,
                    onNavigateToEditProfile = {
                        navController.navigate(EditProfileRoute.route)
                    },
                    onNavigateToAbout = {
                        navController.navigate(AboutRoute.route)
                    },
                    onNavigateToHelpSupport = {
                        navController.navigate(HelpSupportRoute.route)
                    }
                )
            }

            composable(
                route = GameDetailRoute.route,
                arguments = listOf(
                    navArgument("gameId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val gameId = backStackEntry.arguments?.getString("gameId") ?: ""
                GameDetailScreen(
                    gameId = gameId,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onAddToCart = { gameId ->
                        navController.navigate(CartRoute.route)
                    }
                )
            }

            composable(CartRoute.route) {
                CartScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onProceedToCheckout = {
                        navController.navigate(CheckoutRoute.route)
                    }
                )
            }

            composable(CheckoutRoute.route) {
                CheckoutScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onPaymentSuccess = {
                        navController.navigate(PaymentSuccessRoute.route) {
                            popUpTo(BottomNavScreen.Home.route) {
                                inclusive = false
                            }
                        }
                    }
                )
            }

            composable(PaymentSuccessRoute.route) {
                PaymentSuccessScreen(
                    onNavigateToLibrary = {
                        navController.navigate(BottomNavScreen.Library.route) {
                            popUpTo(BottomNavScreen.Home.route) {
                                inclusive = false
                            }
                        }
                    },
                    onNavigateToHome = {
                        navController.navigate(BottomNavScreen.Home.route) {
                            popUpTo(BottomNavScreen.Home.route) {
                                inclusive = true
                            }
                        }
                    }
                )
            }

            composable(EditProfileRoute.route) {
                EditProfileScreen(
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }

            composable(AboutRoute.route) {
                AboutScreen(
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }

            composable(HelpSupportRoute.route) {
                HelpSupportScreen(
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

@Composable
fun PaymentSuccessScreen(
    onNavigateToLibrary: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "✅",
            style = MaterialTheme.typography.displayLarge
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "¡Pago Exitoso!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Tus juegos han sido agregados a tu biblioteca",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onNavigateToLibrary,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                "Ir a Mi Biblioteca",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = onNavigateToHome,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Seguir Explorando")
        }
    }
}