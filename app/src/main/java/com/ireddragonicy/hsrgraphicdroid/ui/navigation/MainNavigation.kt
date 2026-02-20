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
import com.ireddragonicy.hsrgraphicdroid.R
import com.ireddragonicy.hsrgraphicdroid.ui.screens.*
import com.ireddragonicy.hsrgraphicdroid.ui.viewmodel.GamePrefsViewModel
import com.ireddragonicy.hsrgraphicdroid.ui.viewmodel.GraphicsViewModel
import com.ireddragonicy.hsrgraphicdroid.ui.viewmodel.MainViewModel
import com.ireddragonicy.hsrgraphicdroid.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

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
                    mainViewModel = mainViewModel,
                    graphicsViewModel = graphicsViewModel
                )
            },
            @Composable {
                GamePrefsScreen(
                    mainViewModel = mainViewModel,
                    gamePrefsViewModel = gamePrefsViewModel
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
        },
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
