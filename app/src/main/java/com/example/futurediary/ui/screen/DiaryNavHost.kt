package com.example.futurediary.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
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
    object Settings : Screen("settings", "Settings")
    object Promises : Screen("promises", "My Promises")
    object Add : Screen("add_entry?entryId={entryId}&sharedLink={sharedLink}", "Add Memory") {
        fun createRoute(entryId: Long? = null, sharedLink: String? = null) = 
            buildString {
                append("add_entry")
                val params = mutableListOf<String>()
                if (entryId != null) params.add("entryId=$entryId")
                if (sharedLink != null) params.add("sharedLink=$sharedLink")
                if (params.isNotEmpty()) {
                    append("?")
                    append(params.joinToString("&"))
                }
            }
    }
    object Detail : Screen("diary_detail/{entryId}", "Memory Detail") {
        fun createRoute(entryId: Long) = "diary_detail/$entryId"
    }
    object AddPromise : Screen("add_promise", "Make a Promise")
}

@Composable
fun DiaryNavHost(
    sharedLink: String? = null,
    onSharedLinkConsumed: () -> Unit = {}
) {
    val navController = rememberNavController()
    val diaryViewModel: DiaryViewModel = hiltViewModel()
    val authViewModel: AuthViewModel = hiltViewModel()
    val isUserLoggedIn by authViewModel.isUserLoggedIn.collectAsStateWithLifecycle()
    val isAuthLoading by authViewModel.isLoading.collectAsStateWithLifecycle()
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Handle shared link
    androidx.compose.runtime.LaunchedEffect(sharedLink, isUserLoggedIn) {
        if (sharedLink != null && isUserLoggedIn) {
            navController.navigate(Screen.Add.createRoute(sharedLink = java.net.URLEncoder.encode(sharedLink, "UTF-8")))
            onSharedLinkConsumed()
        }
    }

    // Define which screens should show the drawer
    val showDrawer = currentRoute == Screen.List.route || 
                     currentRoute == Screen.Vault.route || 
                     currentRoute == Screen.Photos.route ||
                     currentRoute == Screen.Profile.route ||
                     currentRoute == Screen.Settings.route ||
                     currentRoute == Screen.Promises.route

    if (isAuthLoading && !isUserLoggedIn) {
        // Show loading screen during initial silent sign-in
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

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
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Verified, contentDescription = null) },
                        label = { Text("My Promises") },
                        selected = currentRoute == Screen.Promises.route,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Screen.Promises.route) {
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
                        selected = currentRoute == Screen.Settings.route,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Screen.Settings.route) {
                                launchSingleTop = true
                            }
                        }
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
                                    popUpTo(navController.graph.id) { inclusive = true }
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
                LoginScreen(
                    onLoginSuccess = {
                        authViewModel.onLoginSuccess()
                        diaryViewModel.updateCurrentUser()
                        navController.navigate(Screen.List.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onGuestLogin = {
                        authViewModel.signInAnonymously {
                            diaryViewModel.updateCurrentUser()
                            navController.navigate(Screen.List.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        }
                    }
                )
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
                    onNavigateToAddPromise = {
                        navController.navigate(Screen.AddPromise.route)
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
                    authViewModel = authViewModel,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = diaryViewModel,
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }

            composable(Screen.Promises.route) {
                PromisesScreen(
                    viewModel = diaryViewModel,
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }
            
            composable(Screen.AddPromise.route) {
                AddPromiseScreen(
                    viewModel = diaryViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            composable(
                route = Screen.Add.route,
                arguments = listOf(
                    androidx.navigation.navArgument("entryId") {
                        type = androidx.navigation.NavType.LongType
                        defaultValue = -1L
                    },
                    androidx.navigation.navArgument("sharedLink") {
                        type = androidx.navigation.NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val entryId = backStackEntry.arguments?.getLong("entryId") ?: -1L
                val receivedLink = backStackEntry.arguments?.getString("sharedLink")
                AddEntryScreen(
                    viewModel = diaryViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    entryId = entryId,
                    sharedLink = receivedLink
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
