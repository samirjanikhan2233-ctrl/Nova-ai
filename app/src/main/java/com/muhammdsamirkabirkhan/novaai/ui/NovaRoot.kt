package com.muhammdsamirkabirkhan.novaai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.muhammdsamirkabirkhan.novaai.ads.AdManager
import com.muhammdsamirkabirkhan.novaai.ads.BannerAd
import com.muhammdsamirkabirkhan.novaai.findActivity

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("chat", "Chat", Icons.Filled.ChatBubble),
    Tab("history", "History", Icons.Filled.History),
    Tab("tools", "Tools", Icons.Filled.Build),
    Tab("settings", "Settings", Icons.Filled.Settings),
)

@Composable
fun NovaRoot(vm: MainViewModel = viewModel(), chat: ChatViewModel = viewModel()) {
    val user by vm.user.collectAsStateWithLifecycle()
    LaunchedEffect(user?.uid) { chat.newChat() }   // never leak one account's chat into another
    if (user == null) AuthScreen(vm) else MainScaffold(vm, chat)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MainScaffold(vm: MainViewModel, chat: ChatViewModel) {
    val nav = rememberNavController()
    val activity = LocalContext.current.findActivity()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val premium = profile?.premium == true
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    val onTab = route in tabs.map { it.route }
    val imeOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val snack = remember { SnackbarHostState() }
    val error by vm.error.collectAsStateWithLifecycle()
    val info by vm.info.collectAsStateWithLifecycle()

    // Ads are initialised only for signed-in FREE users, after consent (UMP).
    LaunchedEffect(profile != null, premium) { if (profile != null && !premium) AdManager.init(activity) }
    LaunchedEffect(error) { error?.let { snack.showSnackbar(it); vm.error.value = null } }
    LaunchedEffect(info) { info?.let { snack.showSnackbar(it); vm.info.value = null } }

    val goPremium = { nav.navigate("premium") { launchSingleTop = true } }
    val goTab: (String) -> Unit = { r ->
        nav.navigate(r) { popUpTo("chat") { saveState = true }; launchSingleTop = true; restoreState = true }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top),
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            Column {
                // Banner only on browsing screens - never beside the chat input or a purchase screen.
                if (!premium && profile != null && onTab && route != "chat" && !imeOpen) BannerAd()
                if (onTab && !imeOpen) NavigationBar {
                    tabs.forEach { t ->
                        NavigationBarItem(
                            selected = route == t.route,
                            onClick = { goTab(t.route) },
                            icon = { Icon(t.icon, contentDescription = t.label) },
                            label = { Text(t.label) },
                        )
                    }
                }
            }
        },
    ) { pad ->
        NavHost(nav, startDestination = "chat", modifier = Modifier.padding(pad)) {
            composable("chat") { ChatScreen(chat, vm, onUpgrade = { goPremium() }) }
            composable("history") { HistoryScreen(vm, chat, onOpen = { goTab("chat") }) }
            composable("tools") { ToolsScreen(vm, onOpen = { nav.navigate("tool/$it") }) }
            composable("settings") { SettingsScreen(vm, onPremium = { goPremium() }) }
            composable("premium") { PremiumScreen(vm, onBack = { nav.popBackStack() }) }
            composable("tool/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { e ->
                ToolScreen(e.arguments?.getString("id").orEmpty(), vm,
                    onBack = { nav.popBackStack() }, onUpgrade = { goPremium() })
            }
        }
    }
}
