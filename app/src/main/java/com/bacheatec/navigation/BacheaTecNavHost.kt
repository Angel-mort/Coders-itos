package com.bacheatec.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.bacheatec.ui.screens.DetectorScreen
import com.bacheatec.ui.screens.HomeScreen
import com.bacheatec.ui.screens.MapScreen
import com.bacheatec.ui.screens.ReportScreen
import com.bacheatec.ui.screens.StatsScreen

@Composable
fun BacheaTecNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.HOME,
        modifier = modifier,
    ) {
        composable(NavRoutes.HOME) { HomeScreen() }
        composable(NavRoutes.MAP) { MapScreen() }
        composable(NavRoutes.REPORT) { ReportScreen() }
        composable(NavRoutes.STATS) { StatsScreen() }
        composable(NavRoutes.DETECTOR) { DetectorScreen() }
    }
}
