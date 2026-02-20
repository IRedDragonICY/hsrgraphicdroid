package com.ireddragonicy.hsrgraphicdroid.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ireddragonicy.hsrgraphicdroid.data.GamePreferences
import com.ireddragonicy.hsrgraphicdroid.data.GamePrefsBackupData
import com.ireddragonicy.hsrgraphicdroid.data.GamePrefsChangeManager
import com.ireddragonicy.hsrgraphicdroid.data.SettingChange
import com.ireddragonicy.hsrgraphicdroid.utils.HsrGameManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class GamePrefsUiState(
    val isLoading: Boolean = true,
    val currentPrefs: GamePreferences = GamePreferences(),
    val originalPreferences: GamePreferences? = null,
    val backups: List<GamePrefsBackupData> = emptyList(),
    val hasChanges: Boolean = false,
    val pendingChangesCount: Int = 0,
    val modifiedFields: Set<String> = emptySet(),
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class GamePrefsViewModel(application: Application) : AndroidViewModel(application) {

    private val gameManager = HsrGameManager(application)
    private val changeManager = GamePrefsChangeManager()

    private val _uiState = MutableStateFlow(GamePrefsUiState())
    val uiState: StateFlow<GamePrefsUiState> = _uiState.asStateFlow()

    val canUndo: StateFlow<Boolean> = changeManager.canUndo
    val canRedo: StateFlow<Boolean> = changeManager.canRedo

    init {
        loadPreferences()
        loadBackups()
    }

    fun loadPreferences() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val prefs = withContext(Dispatchers.IO) {
                gameManager.readGamePreferences()
            }

            if (prefs != null) {
                changeManager.setBaseline(prefs)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentPrefs = prefs,
                        originalPreferences = prefs.copy(),
                        modifiedFields = emptySet(),
                        pendingChangesCount = 0
                    )
                }
            } else {
                val defaultPrefs = GamePreferences()
                changeManager.setBaseline(defaultPrefs)
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

    fun loadBackups() {
        viewModelScope.launch {
            val backups = withContext(Dispatchers.IO) {
                gameManager.loadPrefsBackups().reversed()
            }
            _uiState.update { it.copy(backups = backups) }
        }
    }

    fun updateSettings(newPrefs: GamePreferences) {
        val current = _uiState.value.currentPrefs
        changeManager.recordChange("preferences", current, newPrefs, newPrefs)
        updateStateWithPrefs(newPrefs)
    }

    private fun updateStateWithPrefs(newPrefs: GamePreferences) {
        val original = _uiState.value.originalPreferences
        val modifiedFields = mutableSetOf<String>()
        original?.let { orig ->
            if (newPrefs.textLanguage != orig.textLanguage) modifiedFields.add("textLanguage")
            if (newPrefs.audioLanguage != orig.audioLanguage) modifiedFields.add("audioLanguage")
            if (newPrefs.videoBlacklist != orig.videoBlacklist) modifiedFields.add("videoBlacklist")
            if (newPrefs.audioBlacklist != orig.audioBlacklist) modifiedFields.add("audioBlacklist")
            if (newPrefs.isSaveBattleSpeed != orig.isSaveBattleSpeed) modifiedFields.add("isSaveBattleSpeed")
            if (newPrefs.autoBattleOpen != orig.autoBattleOpen) modifiedFields.add("autoBattleOpen")
            if (newPrefs.needDownloadAllAssets != orig.needDownloadAllAssets) modifiedFields.add("needDownloadAllAssets")
            if (newPrefs.forceUpdateVideo != orig.forceUpdateVideo) modifiedFields.add("forceUpdateVideo")
            if (newPrefs.forceUpdateAudio != orig.forceUpdateAudio) modifiedFields.add("forceUpdateAudio")
            if (newPrefs.showSimplifiedSkillDesc != orig.showSimplifiedSkillDesc) modifiedFields.add("showSimplifiedSkillDesc")
            if (newPrefs.gridFightSeenSeasonTalentTree != orig.gridFightSeenSeasonTalentTree) modifiedFields.add("gridFightSeenSeasonTalentTree")
            if (newPrefs.rogueTournEnableGodMode != orig.rogueTournEnableGodMode) modifiedFields.add("rogueTournEnableGodMode")
        }

        _uiState.update {
            it.copy(
                currentPrefs = newPrefs,
                hasChanges = modifiedFields.isNotEmpty(),
                pendingChangesCount = modifiedFields.size,
                modifiedFields = modifiedFields
            )
        }
    }

    fun updateTextLanguage(languageCode: Int) {
        val current = _uiState.value.currentPrefs
        val newPrefs = current.copy(textLanguage = languageCode)
        updateSettings(newPrefs)
    }

    fun updateAudioLanguage(languageCode: Int) {
        val current = _uiState.value.currentPrefs
        val newPrefs = current.copy(audioLanguage = languageCode)
        updateSettings(newPrefs)
    }

    fun addToVideoBlacklist(filename: String) {
        val current = _uiState.value.currentPrefs
        val newPrefs = current.copy(videoBlacklist = current.videoBlacklist + filename)
        updateSettings(newPrefs)
    }

    fun removeFromVideoBlacklist(filename: String) {
        val current = _uiState.value.currentPrefs
        val newPrefs = current.copy(videoBlacklist = current.videoBlacklist - filename)
        updateSettings(newPrefs)
    }

    fun addToAudioBlacklist(filename: String) {
        val current = _uiState.value.currentPrefs
        val newPrefs = current.copy(audioBlacklist = current.audioBlacklist + filename)
        updateSettings(newPrefs)
    }

    fun removeFromAudioBlacklist(filename: String) {
        val current = _uiState.value.currentPrefs
        val newPrefs = current.copy(audioBlacklist = current.audioBlacklist - filename)
        updateSettings(newPrefs)
    }

    fun updateBooleanPreference(key: String, isEnabled: Boolean) {
        val value = if (isEnabled) 1 else 0
        val state = _uiState.value
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
        updateSettings(newPrefs)
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
                changeManager.setBaseline(prefs)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        originalPreferences = prefs.copy(),
                        hasChanges = false,
                        pendingChangesCount = 0,
                        modifiedFields = emptySet(),
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
            changeManager.resetToBaseline()
            _uiState.update {
                it.copy(
                    currentPrefs = original.copy(),
                    hasChanges = false,
                    pendingChangesCount = 0,
                    modifiedFields = emptySet()
                )
            }
        }
    }

    fun undo() {
        changeManager.undo()?.let { previousPrefs ->
            updateStateWithPrefs(previousPrefs)
        }
    }

    fun redo() {
        changeManager.redo()?.let { nextPrefs ->
            updateStateWithPrefs(nextPrefs)
        }
    }

    fun getPendingChangesDetails(): List<SettingChange> {
        return changeManager.getModifiedFieldsDetails(_uiState.value.currentPrefs)
    }

    fun saveBackup(name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val success = withContext(Dispatchers.IO) {
                gameManager.savePrefsBackup(name, _uiState.value.currentPrefs)
            }

            if (success) {
                loadBackups()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Backup created"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to create backup"
                    )
                }
            }
        }
    }

    fun restoreBackup(backup: GamePrefsBackupData) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val prefs = backup.prefs
            val langSuccess = withContext(Dispatchers.IO) {
                gameManager.writeLanguageSettings(prefs.textLanguage, prefs.audioLanguage)
            }
            val videoSuccess = withContext(Dispatchers.IO) {
                gameManager.writeVideoBlacklist(prefs.videoBlacklist)
            }
            val audioSuccess = withContext(Dispatchers.IO) {
                gameManager.writeAudioBlacklist(prefs.audioBlacklist)
            }
            val otherSuccess = withContext(Dispatchers.IO) {
                gameManager.writeOtherPreferences(prefs)
            }

            if (langSuccess && videoSuccess && audioSuccess && otherSuccess) {
                changeManager.setBaseline(prefs)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentPrefs = prefs.copy(),
                        originalPreferences = prefs.copy(),
                        hasChanges = false,
                        pendingChangesCount = 0,
                        modifiedFields = emptySet(),
                        successMessage = "Backup restored"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to restore backup"
                    )
                }
            }
        }
    }

    fun deleteBackup(backup: GamePrefsBackupData) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                gameManager.deletePrefsBackup(backup)
            }
            if (success) {
                loadBackups()
            }
        }
    }

    fun clearMessage() {
        _uiState.update {
            it.copy(errorMessage = null, successMessage = null)
        }
    }
}
