package com.easylogger.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.easylogger.app.MainActivity
import com.easylogger.app.ui.detail.DetailScreen
import com.easylogger.app.ui.detail.QuestionDetailScreen
import com.easylogger.app.ui.main.MainScreen

@Composable
fun AppNavHost(activity: MainActivity) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NavRoutes.Main.route
    ) {
        composable(NavRoutes.Main.route) {
            MainScreen(
                onCategoryClick = { categoryId ->
                    navController.navigate(NavRoutes.Detail.createRoute(categoryId))
                },
                onQuestionClick = { questionId ->
                    navController.navigate(NavRoutes.QuestionDetail.createRoute(questionId))
                },
                activity = activity
            )
        }
        composable(
            route = NavRoutes.Detail.route,
            arguments = listOf(
                navArgument("categoryId") { type = NavType.LongType }
            )
        ) {
            DetailScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = NavRoutes.QuestionDetail.route,
            arguments = listOf(
                navArgument("questionId") { type = NavType.LongType }
            )
        ) {
            QuestionDetailScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
