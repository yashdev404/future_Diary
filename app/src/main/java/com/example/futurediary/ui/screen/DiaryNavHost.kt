package com.example.futurediary.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.futurediary.ui.viewmodel.AuthViewModel
import com.example.futurediary.ui.viewmodel.DiaryViewModel
import kotlinx.coroutines.launch
import com.example.futurediary.ui.screen.PhotosScreen
import com.example.futurediary.ui.screen.ProfileScreen

sealed class Screen(val route: String, val title: String = "") {
    object Login : Screen("login", "Login")
    object List : Screen("diary_list", "My Journal")
    object Vault : Screen("memory_vault", "Memory Vault")
    object Photos : Screen("photos", "Photos")
    object Profile : Screen("profile", "Profile")
    object Add : Screen("add_entry?entryId={entryId}", "Add Memory") {
        fun createRoute(entryId: Long? = null) = 
            if (entryId != null) "add_entry?entryId=$entryId" else "add_entry"
    }
    object Detail : Screen("diary_detail/{entryId}", "Memory Detail") {
        fun createRoute(entryId: Long) = "diary_detail/$entryId"
    }
}

@Composable
fun DiaryNavHost() {
    val navController = rememberNavController()
    val diaryViewModel: DiaryViewModel = hiltViewModel()
    val authViewModel: AuthViewModel = hiltViewModel()
    val isUserLoggedIn by authViewModel.isUserLoggedIn.collectAsStateWithLifecycle()
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Define which screens should show the drawer
    val showDrawer = currentRoute == Screen.List.route || 
                     currentRoute == Screen.Vault.route || 
                     currentRoute == Screen.Photos.route ||
                     currentRoute == Screen.Profile.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = showDrawer && isUserLoggedIn,
        drawerContent = {
            if (isUserLoggedIn) {
                ModalDrawerSheet {
                    // Drawer Header
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(
                                "Future Diary",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "Capture your journey",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Book, contentDescription = null) },
                        label = { Text("My Journal") },
                        selected = currentRoute == Screen.List.route,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Screen.List.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    inclusive = false
                                }
                                launchSingleTop = true
                            }
                        }
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        label = { Text("Memory Vault") },
                        selected = currentRoute == Screen.Vault.route,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Screen.Vault.route) {
                                launchSingleTop = true
                            }
                        }
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                        label = { Text("Photos") },
                        selected = currentRoute == Screen.Photos.route,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Screen.Photos.route) {
                                launchSingleTop = true
                            }
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = null) },
                        label = { Text("Profile & Insights") },
                        selected = currentRoute == Screen.Profile.route,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Screen.Profile.route) {
                                launchSingleTop = true
                            }
                        }
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("Settings") },
                        selected = false,
                        onClick = { scope.launch { drawerState.close() } }
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    NavigationDrawerItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
                        label = { Text("Logout") },
                        selected = false,
                        onClick = {
                            scope.launch { 
                                drawerState.close()
                                authViewModel.logout()
                                diaryViewModel.updateCurrentUser()
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        },
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = if (isUserLoggedIn) Screen.List.route else Screen.Login.route,
        ) {
            composable(Screen.Login.route) {
                LoginScreen {
                    authViewModel.onLoginSuccess()
                    diaryViewModel.updateCurrentUser()
                    navController.navigate(Screen.List.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            }

            composable(Screen.List.route) {
                DiaryListScreen(
                    viewModel = diaryViewModel,
                    onNavigateToAdd = { entryId -> 
                        navController.navigate(Screen.Add.createRoute(entryId)) 
                    },
                    onNavigateToDetail = { entryId ->
                        navController.navigate(Screen.Detail.createRoute(entryId))
                    },
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }

            composable(Screen.Vault.route) {
                VaultScreen(
                    viewModel = diaryViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onEntryClick = { entryId ->
                        navController.navigate(Screen.Detail.createRoute(entryId))
                    }
                )
            }

            composable(Screen.Photos.route) {
                PhotosScreen(
                    viewModel = diaryViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onEntryClick = { entryId ->
                        navController.navigate(Screen.Detail.createRoute(entryId))
                    }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    viewModel = diaryViewModel,
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }
            
            composable(
                route = Screen.Add.route,
                arguments = listOf(
                    androidx.navigation.navArgument("entryId") {
                        type = androidx.navigation.NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) { backStackEntry ->
                val entryId = backStackEntry.arguments?.getLong("entryId") ?: -1L
                AddEntryScreen(
                    viewModel = diaryViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    entryId = entryId
                )
            }
            
            composable(
                route = Screen.Detail.route,
                arguments = listOf(
                    androidx.navigation.navArgument("entryId") {
                        type = androidx.navigation.NavType.LongType
                    }
                )
            ) { backStackEntry ->
                val entryId = backStackEntry.arguments?.getLong("entryId") ?: -1L
                DiaryDetailScreen(
                    entryId = entryId,
                    viewModel = diaryViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { id ->
                        navController.navigate(Screen.Add.createRoute(id))
                    }
                )
            }
        }
    }
}
