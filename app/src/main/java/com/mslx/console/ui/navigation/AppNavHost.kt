package com.mslx.console.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.mslx.console.ui.create.CreateInstanceScreen
import com.mslx.console.ui.instances.InstancesScreen
import com.mslx.console.ui.settings.InstanceSettingsScreen
import com.mslx.console.ui.settings.FileManagerScreen
import com.mslx.console.ui.settings.PluginsModsScreen
import com.mslx.console.ui.settings.ServerPropertiesScreen
import com.mslx.console.ui.settings.SettingsScreen
import com.mslx.console.ui.splash.SplashScreen
import com.mslx.console.ui.user.UserCenterScreen
import com.mslx.console.ui.welcome.WelcomeScreen

object Routes {
    const val SPLASH = "splash"
    const val WELCOME = "welcome"
    const val CONNECT = "connect?auto={autoConnect}&daemonId={daemonId}"
    const val INSTANCES = "instances"
    const val SETTINGS = "settings"
    const val NEW_INSTANCE = "newInstance"
    const val CONSOLE = "console/{instanceId}"
    const val INSTANCE_SETTINGS = "instanceSettings/{instanceId}"
    const val FILE_MANAGER = "fileManager/{instanceId}"
    const val PLUGINS_MODS = "pluginsMods/{instanceId}"
    const val SERVER_PROPS = "serverProps/{instanceId}"
    const val USER_CENTER = "userCenter"

    fun console(instanceId: Long): String = "console/$instanceId"
    fun connect(auto: Boolean, daemonId: String? = null): String =
        "connect?auto=$auto&daemonId=${daemonId.orEmpty()}"
    fun instanceSettings(instanceId: Long): String = "instanceSettings/$instanceId"
    fun fileManager(instanceId: Long): String = "fileManager/$instanceId"
    fun pluginsMods(instanceId: Long): String = "pluginsMods/$instanceId"
    fun serverProps(instanceId: Long): String = "serverProps/$instanceId"
}

@Composable
fun AppNavHost(
    settings: AppSettings,
    navController: NavHostController = rememberNavController(),
) {
    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(Routes.INSTANCES) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        enterTransition = { fadeIn(tween(180)) },
        exitTransition = { fadeOut(tween(120)) },
        popEnterTransition = { fadeIn(tween(180)) },
        popExitTransition = { fadeOut(tween(120)) },
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
                navArgument("daemonId") {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) { entry ->
            val autoConnect = entry.arguments?.getBoolean("autoConnect") ?: true
            val daemonId = entry.arguments?.getString("daemonId")?.takeIf { it.isNotBlank() }
            ConnectScreen(
                onConnected = {
                    navController.navigate(Routes.INSTANCES) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onBack = { navController.popBackStack() },
                // 自动连接失败：回到手动连接页，鉴权失败时不得进入实例主页
                onAutoConnectFailed = {
                    navController.navigate(Routes.connect(false, daemonId)) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                autoConnect = autoConnect,
                editingDaemonId = daemonId,
            )
        }

        composable(Routes.INSTANCES) {
            InstancesScreen(
                onOpenSettings = {
                    navigateTopLevel(Routes.SETTINGS)
                },
                onOpenNewInstance = {
                    navigateTopLevel(Routes.NEW_INSTANCE)
                },
                onOpenInstance = { id ->
                    navController.navigate(Routes.console(id)) { launchSingleTop = true }
                },
            )
        }

        composable(Routes.NEW_INSTANCE) {
            CreateInstanceScreen(
                onOpenInstances = { navigateTopLevel(Routes.INSTANCES) },
                onOpenSettings = { navigateTopLevel(Routes.SETTINGS) },
                onOpenConsole = { id ->
                    navController.navigate(Routes.console(id)) { launchSingleTop = true }
                },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onOpenInstances = {
                    navigateTopLevel(Routes.INSTANCES)
                },
                onOpenNewInstance = {
                    navigateTopLevel(Routes.NEW_INSTANCE)
                },
                onAddDaemon = {
                    navController.navigate(Routes.connect(false)) { launchSingleTop = true }
                },
                onEditDaemon = { daemonId ->
                    navController.navigate(Routes.connect(false, daemonId)) { launchSingleTop = true }
                },
                onOpenUserCenter = {
                    navController.navigate(Routes.USER_CENTER) { launchSingleTop = true }
                },
            )
        }

        composable(Routes.USER_CENTER) {
            UserCenterScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.CONSOLE,
            arguments = listOf(navArgument("instanceId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val instanceId = backStackEntry.arguments?.getLong("instanceId") ?: 0L
            ConsoleScreen(
                instanceId = instanceId,
                onBack = { navController.popBackStack() },
                onOpenSettings = {
                    navController.navigate(Routes.instanceSettings(instanceId)) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(
            route = Routes.INSTANCE_SETTINGS,
            arguments = listOf(navArgument("instanceId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val instanceId = backStackEntry.arguments?.getLong("instanceId") ?: 0L
            InstanceSettingsScreen(
                instanceId = instanceId,
                onBack = { navController.popBackStack() },
                onOpenPluginsMods = {
                    navController.navigate(Routes.pluginsMods(instanceId)) { launchSingleTop = true }
                },
                onOpenServerProps = {
                    navController.navigate(Routes.serverProps(instanceId)) { launchSingleTop = true }
                },
                onOpenFileManager = {
                    navController.navigate(Routes.fileManager(instanceId)) { launchSingleTop = true }
                },
            )
        }

        composable(
            route = Routes.FILE_MANAGER,
            arguments = listOf(navArgument("instanceId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val instanceId = backStackEntry.arguments?.getLong("instanceId") ?: 0L
            FileManagerScreen(
                instanceId = instanceId,
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.PLUGINS_MODS,
            arguments = listOf(navArgument("instanceId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val instanceId = backStackEntry.arguments?.getLong("instanceId") ?: 0L
            PluginsModsScreen(
                instanceId = instanceId,
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.SERVER_PROPS,
            arguments = listOf(navArgument("instanceId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val instanceId = backStackEntry.arguments?.getLong("instanceId") ?: 0L
            ServerPropertiesScreen(
                instanceId = instanceId,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
