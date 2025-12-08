package com.ireddragonicy.hsrgraphicdroid.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.ireddragonicy.hsrgraphicdroid.ui.navigation.MainNavigation
import com.ireddragonicy.hsrgraphicdroid.ui.theme.HsrGraphicTheme
import com.ireddragonicy.hsrgraphicdroid.ui.viewmodel.GamePrefsViewModel
import com.ireddragonicy.hsrgraphicdroid.ui.viewmodel.GraphicsViewModel
import com.ireddragonicy.hsrgraphicdroid.ui.viewmodel.MainViewModel
import com.ireddragonicy.hsrgraphicdroid.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

/**
 * Single-Activity Architecture with Jetpack Compose
 * 
 * This is the only Activity in the app. All UI is built using
 * Compose with Navigation Compose for screen navigation.
 */
class MainActivity : ComponentActivity() {

    // ViewModels - using activity-level scope for shared state
    private val mainViewModel: MainViewModel by viewModels()
    private val graphicsViewModel: GraphicsViewModel by viewModels()
    private val gamePrefsViewModel: GamePrefsViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        enableEdgeToEdge()
        
        // Bootstrap appearance settings
        lifecycleScope.launch {
            mainViewModel.bootstrapAppearance()
        }

        setContent {
            // Collect theme preferences
            val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
            
            // Determine dark mode: use user preference or system default
            val isDarkTheme = settingsUiState.isDarkMode ?: isSystemInDarkTheme()
            
            HsrGraphicTheme(
                darkTheme = isDarkTheme,
                dynamicColor = settingsUiState.useDynamicColor
            ) {
                MainNavigation(
                    mainViewModel = mainViewModel,
                    graphicsViewModel = graphicsViewModel,
                    gamePrefsViewModel = gamePrefsViewModel,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }
}
