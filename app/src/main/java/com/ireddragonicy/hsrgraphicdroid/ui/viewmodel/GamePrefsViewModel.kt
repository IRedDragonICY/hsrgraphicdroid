package com.ireddragonicy.hsrgraphicdroid.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ireddragonicy.hsrgraphicdroid.data.GamePreferences
import com.ireddragonicy.hsrgraphicdroid.utils.HsrGameManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class GamePrefsUiState(
    val isLoading: Boolean = true,
    val currentPrefs: GamePreferences = GamePreferences(),
    val originalPreferences: GamePreferences? = null,
    val hasChanges: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class GamePrefsViewModel(application: Application) : AndroidViewModel(application) {

    private val gameManager = HsrGameManager(application)

    private val _uiState = MutableStateFlow(GamePrefsUiState())
    val uiState: StateFlow<GamePrefsUiState> = _uiState.asStateFlow()

    init {
        loadPreferences()
    }

    fun loadPreferences() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val prefs = withContext(Dispatchers.IO) {
                gameManager.readGamePreferences()
            }

            if (prefs != null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentPrefs = prefs,
                        originalPreferences = prefs.copy()
                    )
                }
            } else {
                val defaultPrefs = GamePreferences()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentPrefs = defaultPrefs,
                        originalPreferences = defaultPrefs.copy(),
                        errorMessage = "Failed to load preferences"
                    )
                }
            }
        }
    }

    fun updateTextLanguage(languageCode: Int) {
        _uiState.update {
            val newPrefs = it.currentPrefs.copy(textLanguage = languageCode)
            it.copy(
                currentPrefs = newPrefs,
                hasChanges = hasChanges(newPrefs, it.originalPreferences)
            )
        }
    }

    fun updateAudioLanguage(languageCode: Int) {
        _uiState.update {
            val newPrefs = it.currentPrefs.copy(audioLanguage = languageCode)
            it.copy(
                currentPrefs = newPrefs,
                hasChanges = hasChanges(newPrefs, it.originalPreferences)
            )
        }
    }

    fun addToVideoBlacklist(filename: String) {
        _uiState.update {
            val newBlacklist = it.currentPrefs.videoBlacklist + filename
            val newPrefs = it.currentPrefs.copy(videoBlacklist = newBlacklist)
            it.copy(
                currentPrefs = newPrefs,
                hasChanges = hasChanges(newPrefs, it.originalPreferences)
            )
        }
    }

    fun removeFromVideoBlacklist(filename: String) {
        _uiState.update {
            val newBlacklist = it.currentPrefs.videoBlacklist - filename
            val newPrefs = it.currentPrefs.copy(videoBlacklist = newBlacklist)
            it.copy(
                currentPrefs = newPrefs,
                hasChanges = hasChanges(newPrefs, it.originalPreferences)
            )
        }
    }

    fun addToAudioBlacklist(filename: String) {
        _uiState.update {
            val newBlacklist = it.currentPrefs.audioBlacklist + filename
            val newPrefs = it.currentPrefs.copy(audioBlacklist = newBlacklist)
            it.copy(
                currentPrefs = newPrefs,
                hasChanges = hasChanges(newPrefs, it.originalPreferences)
            )
        }
    }

    fun removeFromAudioBlacklist(filename: String) {
        _uiState.update {
            val newBlacklist = it.currentPrefs.audioBlacklist - filename
            val newPrefs = it.currentPrefs.copy(audioBlacklist = newBlacklist)
            it.copy(
                currentPrefs = newPrefs,
                hasChanges = hasChanges(newPrefs, it.originalPreferences)
            )
        }
    }

    fun applySettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val prefs = _uiState.value.currentPrefs
            
            // Apply language settings
            val langSuccess = withContext(Dispatchers.IO) {
                gameManager.writeLanguageSettings(prefs.textLanguage, prefs.audioLanguage)
            }

            // Apply video blacklist
            val videoSuccess = withContext(Dispatchers.IO) {
                gameManager.writeVideoBlacklist(prefs.videoBlacklist)
            }

            // Apply audio blacklist
            val audioSuccess = withContext(Dispatchers.IO) {
                gameManager.writeAudioBlacklist(prefs.audioBlacklist)
            }
            
            // Apply other settings
            val otherSuccess = withContext(Dispatchers.IO) {
                gameManager.writeOtherPreferences(prefs)
            }

            if (langSuccess && videoSuccess && audioSuccess && otherSuccess) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        originalPreferences = prefs.copy(),
                        hasChanges = false,
                        successMessage = "Preferences saved successfully"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to save some preferences"
                    )
                }
            }
        }
    }

    fun resetToOriginal() {
        _uiState.value.originalPreferences?.let { original ->
            _uiState.update {
                it.copy(
                    currentPrefs = original.copy(),
                    hasChanges = false
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.update {
            it.copy(errorMessage = null, successMessage = null)
        }
    }

    fun updateBooleanPreference(key: String, isEnabled: Boolean) {
        val value = if (isEnabled) 1 else 0
        _uiState.update { state ->
            val newPrefs = when(key) {
                "isSaveBattleSpeed" -> state.currentPrefs.copy(isSaveBattleSpeed = value)
                "autoBattleOpen" -> state.currentPrefs.copy(autoBattleOpen = value)
                "needDownloadAllAssets" -> state.currentPrefs.copy(needDownloadAllAssets = value)
                "forceUpdateVideo" -> state.currentPrefs.copy(forceUpdateVideo = value)
                "forceUpdateAudio" -> state.currentPrefs.copy(forceUpdateAudio = value)
                "showSimplifiedSkillDesc" -> state.currentPrefs.copy(showSimplifiedSkillDesc = value)
                "gridFightSeenSeasonTalentTree" -> state.currentPrefs.copy(gridFightSeenSeasonTalentTree = value)
                "rogueTournEnableGodMode" -> state.currentPrefs.copy(rogueTournEnableGodMode = value)
                else -> state.currentPrefs
            }
            state.copy(
                currentPrefs = newPrefs,
                hasChanges = hasChanges(newPrefs, state.originalPreferences)
            )
        }
    }

    private fun hasChanges(current: GamePreferences, original: GamePreferences?): Boolean {
        if (original == null) return false
        return current.textLanguage != original.textLanguage ||
                current.audioLanguage != original.audioLanguage ||
                current.videoBlacklist != original.videoBlacklist ||
                current.audioBlacklist != original.audioBlacklist ||
                current.isSaveBattleSpeed != original.isSaveBattleSpeed ||
                current.autoBattleOpen != original.autoBattleOpen ||
                current.needDownloadAllAssets != original.needDownloadAllAssets ||
                current.forceUpdateVideo != original.forceUpdateVideo ||
                current.forceUpdateAudio != original.forceUpdateAudio ||
                current.showSimplifiedSkillDesc != original.showSimplifiedSkillDesc ||
                current.gridFightSeenSeasonTalentTree != original.gridFightSeenSeasonTalentTree ||
                current.rogueTournEnableGodMode != original.rogueTournEnableGodMode
    }
}
