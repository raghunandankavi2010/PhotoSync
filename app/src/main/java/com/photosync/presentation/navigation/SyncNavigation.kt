package com.photosync.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.photosync.presentation.screens.*
import com.photosync.presentation.viewmodel.GalleryViewModel
import com.photosync.presentation.viewmodel.SettingsViewModel
import com.photosync.presentation.viewmodel.SyncDetailViewModel
import com.photosync.presentation.viewmodel.SyncStatusViewModel

/**
 * Navigation routes for the app
 */
sealed class Screen(val route: String) {
    data object SyncStatus : Screen("sync_status")
    data object Gallery : Screen("gallery")
    data object SyncDetail : Screen("sync_detail/{mediaStoreId}") {
        fun createRoute(mediaStoreId: Long) = "sync_detail/$mediaStoreId"
    }
    data object Settings : Screen("settings")
    data object Restore : Screen("restore")
}

/**
 * Main navigation graph
 */
@Composable
fun SyncNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.SyncStatus.route,
        modifier = modifier
    ) {
        // Sync Status Screen
        composable(Screen.SyncStatus.route) {
            val viewModel: SyncStatusViewModel = hiltViewModel()
            SyncStatusScreen(
                viewModel = viewModel,
                onNavigateToGallery = {
                    navController.navigate(Screen.Gallery.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToRestore = {
                    navController.navigate(Screen.Restore.route)
                },
                onNavigateToDetail = { mediaStoreId ->
                    navController.navigate(Screen.SyncDetail.createRoute(mediaStoreId))
                }
            )
        }

        // Gallery Screen
        composable(Screen.Gallery.route) {
            val viewModel: GalleryViewModel = hiltViewModel()
            GalleryScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDetail = { mediaStoreId ->
                    navController.navigate(Screen.SyncDetail.createRoute(mediaStoreId))
                }
            )
        }

        // Sync Detail Screen
        composable(
            route = Screen.SyncDetail.route,
            arguments = listOf(
                navArgument("mediaStoreId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val mediaStoreId = backStackEntry.arguments?.getLong("mediaStoreId") ?: -1L
            val viewModel: SyncDetailViewModel = hiltViewModel()
            SyncDetailScreen(
                viewModel = viewModel,
                mediaStoreId = mediaStoreId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Settings Screen
        composable(Screen.Settings.route) {
            val viewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Restore Screen
        composable(Screen.Restore.route) {
            RestoreScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
