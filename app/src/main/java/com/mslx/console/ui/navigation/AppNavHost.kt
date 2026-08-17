package com.mslx.console.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mslx.console.data.AppSettings
import com.mslx.console.ui.connect.ConnectScreen
import com.mslx.console.ui.console.ConsoleScreen
import com.mslx.console.ui.instances.InstancesScreen
import com.mslx.console.ui.settings.SettingsScreen
import com.mslx.console.ui.splash.SplashScreen
import com.mslx.console.ui.welcome.WelcomeScreen

object Routes {
    const val SPLASH = "splash"
    const val WELCOME = "welcome"
    const val CONNECT = "connect?auto={autoConnect}"
    const val INSTANCES = "instances"
    const val SETTINGS = "settings"
    const val CONSOLE = "console/{instanceId}"

    fun console(instanceId: Long): String = "console/$instanceId"
    fun connect(auto: Boolean): String = "connect?auto=$auto"
}

@Composable
fun AppNavHost(
    settings: AppSettings,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        // 前进：新页从右滑入 + 淡入；旧页左移淡出
        enterTransition = {
            slideInHorizontally(tween(360, easing = FastOutSlowInEasing)) { it } +
                fadeIn(tween(360))
        },
        exitTransition = {
            slideOutHorizontally(tween(320)) { -it / 4 } + fadeOut(tween(320))
        },
        // 返回：反向
        popEnterTransition = {
            slideInHorizontally(tween(360, easing = FastOutSlowInEasing)) { -it / 4 } +
                fadeIn(tween(360))
        },
        popExitTransition = {
            slideOutHorizontally(tween(320)) { it } + fadeOut(tween(320))
        },
    ) {

        composable(Routes.SPLASH) {
            // 用 rememberUpdatedState 保证动画结束后拿到最新的 onboarded 状态
            val latest by rememberUpdatedState(settings)
            SplashScreen(
                onFinished = {
                    val dest = if (latest.onboarded) Routes.connect(true) else Routes.WELCOME
                    navController.navigate(dest) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(Routes.WELCOME) {
            WelcomeScreen(
                onStart = {
                    navController.navigate(Routes.connect(true)) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(
            route = Routes.CONNECT,
            arguments = listOf(
                navArgument("autoConnect") {
                    type = NavType.BoolType
                    defaultValue = true
                },
            ),
        ) { entry ->
            val autoConnect = entry.arguments?.getBoolean("autoConnect") ?: true
            val canGoBack = navController.previousBackStackEntry != null
            ConnectScreen(
                onConnected = {
                    navController.navigate(Routes.INSTANCES) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onBack = if (canGoBack) ({ navController.popBackStack() }) else null,
                autoConnect = autoConnect,
            )
        }

        composable(Routes.INSTANCES) {
            InstancesScreen(
                onOpenSettings = {
                    navController.navigate(Routes.SETTINGS) { launchSingleTop = true }
                },
                onOpenInstance = { id ->
                    navController.navigate(Routes.console(id)) { launchSingleTop = true }
                },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onAddDaemon = {
                    navController.navigate(Routes.connect(false)) { launchSingleTop = true }
                },
            )
        }

        composable(
            route = Routes.CONSOLE,
            arguments = listOf(navArgument("instanceId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val instanceId = backStackEntry.arguments?.getLong("instanceId") ?: 0L
            ConsoleScreen(
                instanceId = instanceId,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
