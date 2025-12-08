package com.ireddragonicy.hsrgraphicdroid.ui.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.ireddragonicy.hsrgraphicdroid.R
import com.ireddragonicy.hsrgraphicdroid.ui.components.ActionBottomBar
import com.ireddragonicy.hsrgraphicdroid.ui.screens.*
import com.ireddragonicy.hsrgraphicdroid.ui.viewmodel.GamePrefsViewModel
import com.ireddragonicy.hsrgraphicdroid.ui.viewmodel.GraphicsViewModel
import com.ireddragonicy.hsrgraphicdroid.ui.viewmodel.MainViewModel
import com.ireddragonicy.hsrgraphicdroid.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

data class ActionBarConfig(
    val hasChanges: Boolean = false,
    val pendingChangesCount: Int = 0,
    val onOpenBackups: () -> Unit = {},
    val onSaveBackup: () -> Unit = {},
    val onApply: () -> Unit = {}
)

/**
 * Navigation routes for the app
 */
sealed class Screen(
    val route: String,
    val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : Screen(
        route = "home",
        titleRes = R.string.nav_home,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    data object Graphics : Screen(
        route = "graphics",
        titleRes = R.string.nav_graphics,
        selectedIcon = Icons.Filled.Tune,
        unselectedIcon = Icons.Outlined.Tune
    )

    data object GamePrefs : Screen(
        route = "game_prefs",
        titleRes = R.string.nav_game_prefs,
        selectedIcon = Icons.Filled.SportsEsports,
        unselectedIcon = Icons.Outlined.SportsEsports
    )

    data object Settings : Screen(
        route = "settings",
        titleRes = R.string.nav_settings,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
}

// List of all navigation items
val bottomNavItems = listOf(
    Screen.Home,
    Screen.Graphics,
    Screen.GamePrefs,
    Screen.Settings
)

/**
 * Main navigation component with swipeable HorizontalPager (WhatsApp-style)
 * Optimized for smooth transitions without lag
 */
@Composable
fun MainNavigation(
    mainViewModel: MainViewModel,
    graphicsViewModel: GraphicsViewModel,
    gamePrefsViewModel: GamePrefsViewModel,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { bottomNavItems.size }
    )
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var graphicsActionBarConfig by remember { mutableStateOf(ActionBarConfig()) }
    var gamePrefsActionBarConfig by remember { mutableStateOf(ActionBarConfig()) }

    val playGame: () -> Unit = remember {
        {
            val pkg = mainViewModel.currentPackage()
            if (pkg != null) {
                context.packageManager.getLaunchIntentForPackage(pkg)?.let {
                    context.startActivity(it)
                }
            }
        }
    }

    // Pre-cache all screens to prevent lag on first navigation
    val screens = remember {
        listOf(
            @Composable { 
                HomeScreen(
                    mainViewModel = mainViewModel,
                    onNavigateToGraphics = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(
                                page = 1,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                        }
                    },
                    onNavigateToGamePrefs = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(
                                page = 2,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                        }
                    }
                )
            },
            @Composable {
                GraphicsScreen(
                    graphicsViewModel = graphicsViewModel,
                    useExternalBottomBar = true,
                    onRegisterActionBar = { config -> graphicsActionBarConfig = config },
                    onLaunchGame = playGame
                )
            },
            @Composable {
                GamePrefsScreen(
                    mainViewModel = mainViewModel,
                    gamePrefsViewModel = gamePrefsViewModel,
                    useExternalBottomBar = true,
                    onRegisterActionBar = { config -> gamePrefsActionBarConfig = config },
                    onLaunchGame = playGame
                )
            },
            @Composable {
                SettingsScreen(
                    settingsViewModel = settingsViewModel
                )
            }
        )
    }

    Scaffold(
        bottomBar = {
            Column {
                if (pagerState.currentPage == 1 || pagerState.currentPage == 2) {
                    val config = if (pagerState.currentPage == 1) graphicsActionBarConfig else gamePrefsActionBarConfig
                    ActionBottomBar {
                        IconButton(onClick = config.onOpenBackups) {
                            Icon(Icons.Outlined.Visibility, null, Modifier.size(20.dp))
                        }
                        OutlinedButton(
                            onClick = config.onSaveBackup,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.SaveAlt, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.save_as_backup))
                        }
                        Button(
                            onClick = config.onApply,
                            modifier = Modifier.weight(1f),
                            colors = if (config.hasChanges) ButtonDefaults.buttonColors()
                            else ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(Icons.Outlined.Check, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (config.hasChanges && config.pendingChangesCount > 0)
                                    stringResource(R.string.apply_pending_changes, config.pendingChangesCount)
                                else stringResource(R.string.apply_settings_now)
                            )
                        }
                    }
                }
                AppBottomNavigationBar(
                    selectedIndex = pagerState.currentPage,
                    onItemSelected = { index ->
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(
                                page = index,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                        }
                    }
                )
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
        ),
        modifier = modifier
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            // Keep ALL pages in memory - prevents recomposition lag
            beyondViewportPageCount = bottomNavItems.size - 1,
            // Optimized fling behavior for snappier response
            flingBehavior = PagerDefaults.flingBehavior(
                state = pagerState,
                pagerSnapDistance = PagerSnapDistance.atMost(1),
                snapAnimationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
            key = { bottomNavItems[it].route },
            // Disable user scrolling during animation for smoother feel
            userScrollEnabled = true
        ) { page ->
            // Use pre-cached screens
            screens[page]()
        }
    }
}

/**
 * Bottom navigation bar with Material3 styling
 * Optimized with remember for stable lambdas
 */
@Composable
fun AppBottomNavigationBar(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        bottomNavItems.forEachIndexed { index, screen ->
            val isSelected = selectedIndex == index
            
            // Stable lambda to prevent recomposition
            val onClick = remember(index) { { onItemSelected(index) } }

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                        contentDescription = null // Removed for performance, label provides accessibility
                    )
                },
                label = {
                    Text(
                        text = stringResource(screen.titleRes),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                },
                selected = isSelected,
                onClick = onClick,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
