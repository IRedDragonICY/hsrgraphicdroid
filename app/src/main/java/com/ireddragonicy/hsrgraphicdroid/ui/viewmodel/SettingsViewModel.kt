package com.ireddragonicy.hsrgraphicdroid.ui.viewmodel

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ireddragonicy.hsrgraphicdroid.ui.AppLanguage
import com.ireddragonicy.hsrgraphicdroid.ui.AppTheme
import com.ireddragonicy.hsrgraphicdroid.utils.PreferenceManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val currentTheme: AppTheme = AppTheme.SYSTEM,
    val currentLanguage: AppLanguage = AppLanguage.SYSTEM,
    // For Compose UI
    val isDarkMode: Boolean? = null, // null = follow system
    val useDynamicColor: Boolean = true,
    val appLanguage: String = "en"
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val preferenceManager = PreferenceManager(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val themeFlow: StateFlow<AppTheme> = preferenceManager.getTheme()
        .map { key -> AppTheme.fromKey(key) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppTheme.SYSTEM)

    val languageFlow: StateFlow<AppLanguage> = preferenceManager.getLanguage()
        .map { key -> AppLanguage.fromKey(key) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppLanguage.SYSTEM)

    init {
        viewModelScope.launch {
            combine(themeFlow, languageFlow) { theme, language ->
                val isDarkMode = when (theme) {
                    AppTheme.DARK -> true
                    AppTheme.LIGHT -> false
                    AppTheme.SYSTEM -> null
                }
                SettingsUiState(
                    currentTheme = theme,
                    currentLanguage = language,
                    isDarkMode = isDarkMode,
                    useDynamicColor = true, // Could be persisted too
                    appLanguage = language.key.ifEmpty { "en" }
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun updateTheme(theme: AppTheme) {
        viewModelScope.launch {
            preferenceManager.setTheme(theme.key)
        }
        AppCompatDelegate.setDefaultNightMode(theme.mode)
    }

    fun updateLanguage(language: AppLanguage) {
        viewModelScope.launch {
            preferenceManager.setLanguage(language.key)
        }
        AppCompatDelegate.setApplicationLocales(AppLanguage.toLocaleList(language))
    }

    // Compose UI helpers
    fun setDarkMode(isDark: Boolean) {
        val theme = if (isDark) AppTheme.DARK else AppTheme.LIGHT
        updateTheme(theme)
    }

    fun setDynamicColor(enabled: Boolean) {
        _uiState.update { it.copy(useDynamicColor = enabled) }
        // Could persist this if needed
    }

    fun setAppLanguage(languageCode: String) {
        val language = AppLanguage.entries.find { it.key == languageCode } ?: AppLanguage.SYSTEM
        updateLanguage(language)
    }
}


