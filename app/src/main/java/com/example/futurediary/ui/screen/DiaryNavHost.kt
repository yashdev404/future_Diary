package com.example.futurediary.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.futurediary.ui.viewmodel.AuthViewModel
import com.example.futurediary.ui.viewmodel.DiaryViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object List : Screen("diary_list")
    object Add : Screen("add_entry?entryId={entryId}") {
        fun createRoute(entryId: Long? = null) = 
            if (entryId != null) "add_entry?entryId=$entryId" else "add_entry"
    }
    object Detail : Screen("diary_detail/{entryId}") {
        fun createRoute(entryId: Long) = "diary_detail/$entryId"
    }
}

@Composable
fun DiaryNavHost() {
    val navController = rememberNavController()
    val diaryViewModel: DiaryViewModel = hiltViewModel()
    val authViewModel: AuthViewModel = hiltViewModel()
    val isUserLoggedIn by authViewModel.isUserLoggedIn.collectAsState()

    NavHost(
        navController = navController,
        startDestination = if (isUserLoggedIn) Screen.List.route else Screen.Login.route,
    ) {
        composable(Screen.Login.route) {
            LoginScreen {
                authViewModel.onLoginSuccess()
                navController.navigate(Screen.List.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            }
        }
        composable(Screen.List.route) {
            DiaryListScreen(
                viewModel = diaryViewModel,
                onNavigateToAdd = { navController.navigate(Screen.Add.createRoute()) },
                onNavigateToDetail = { entryId ->
                    navController.navigate(Screen.Detail.createRoute(entryId))
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.List.route) { inclusive = true }
                    }
                }
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
