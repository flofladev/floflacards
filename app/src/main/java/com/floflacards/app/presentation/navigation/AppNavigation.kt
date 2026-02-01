/*
 * Copyright (C) 2026 FloFla Dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.floflacards.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.floflacards.app.presentation.screen.MainScreen
import com.floflacards.app.presentation.screen.SettingsScreen
import com.floflacards.app.presentation.screen.AppSettingsScreen
import com.floflacards.app.presentation.screen.StatisticsScreen
import com.floflacards.app.presentation.screen.FlashcardManagementScreen
import com.floflacards.app.presentation.screen.AddEditFlashcardScreen
import com.floflacards.app.data.entity.CategoryEntity
import com.floflacards.app.data.entity.FlashcardEntity

/**
 * App navigation component that handles all navigation routes and screens.
 * Extracted from MainActivity to follow Single Responsibility Principle.
 * Maintains all existing functionality and navigation behavior.
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    onRequestOverlayPermission: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            MainScreen(
                onNavigateToSettings = { navController.navigate("categories") },
                onNavigateToStatistics = { navController.navigate("statistics") },
                onNavigateToAppSettings = { navController.navigate("app-settings") },
                onRequestOverlayPermission = onRequestOverlayPermission
            )
        }
        composable("categories") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToFlashcards = { categoryId, categoryName ->
                    navController.navigate("flashcards/$categoryId/$categoryName")
                }
            )
        }
        composable("app-settings") {
            AppSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("statistics") {
            StatisticsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            "flashcards/{categoryId}/{categoryName}",
            arguments = listOf(
                navArgument("categoryId") { type = NavType.LongType },
                navArgument("categoryName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getLong("categoryId") ?: 0L
            val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
            val category = CategoryEntity(id = categoryId, name = categoryName)
            
FlashcardManagementScreen(
                category = category,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddFlashcard = { 
                    navController.navigate("add-flashcard/$categoryId")
                }
            )
        }
        composable(
            "add-flashcard/{categoryId}",
            arguments = listOf(
                navArgument("categoryId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getLong("categoryId") ?: 0L
            
            AddEditFlashcardScreen(
                categoryId = categoryId,
                flashcardToEdit = null,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
