package com.easylogger.app.ui.navigation

sealed class NavRoutes(val route: String) {
    data object Main : NavRoutes("main")
    data object Detail : NavRoutes("detail/{categoryId}") {
        fun createRoute(categoryId: Long) = "detail/$categoryId"
    }
}
