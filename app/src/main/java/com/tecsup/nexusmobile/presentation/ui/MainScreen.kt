package com.tecsup.nexusmobile.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.AccountCircle
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

object EditProfileRoute {
    const val route = "edit_profile"
}

object AboutRoute {
    const val route = "about"
}

object HelpSupportRoute {
    const val route = "help_support"
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
                LibraryScreen()
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
                        // Agregar al carrito y navegar
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
                        // TODO: Navegar a checkout
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