package com.ireddragonicy.hsrgraphicdroid.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ireddragonicy.hsrgraphicdroid.data.BackupData
import com.ireddragonicy.hsrgraphicdroid.data.GraphicsSettings
import com.ireddragonicy.hsrgraphicdroid.data.SettingsChangeManager
import com.ireddragonicy.hsrgraphicdroid.data.SettingChange
import com.ireddragonicy.hsrgraphicdroid.utils.HsrGameManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class GraphicsUiState(
    val isLoading: Boolean = true,
    val currentSettings: GraphicsSettings = GraphicsSettings(),
    val originalSettings: GraphicsSettings? = null,
    val backups: List<BackupData> = emptyList(),
    val hasChanges: Boolean = false,
    val pendingChangesCount: Int = 0,
    val modifiedFields: Set<String> = emptySet(),
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class GraphicsViewModel(application: Application) : AndroidViewModel(application) {

    private val gameManager = HsrGameManager(application)
    private val changeManager = SettingsChangeManager()

    private val _uiState = MutableStateFlow(GraphicsUiState())
    val uiState: StateFlow<GraphicsUiState> = _uiState.asStateFlow()

    val canUndo: StateFlow<Boolean> = changeManager.canUndo
    val canRedo: StateFlow<Boolean> = changeManager.canRedo

    init {
        loadSettings()
        loadBackups()
    }

    fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val settings = withContext(Dispatchers.IO) {
                gameManager.readCurrentSettings()
            }

            if (settings != null) {
                changeManager.setBaseline(settings)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentSettings = settings,
                        originalSettings = settings.copy(),
                        modifiedFields = emptySet(),
                        pendingChangesCount = 0
                    )
                }
            } else {
                val defaultSettings = GraphicsSettings()
                changeManager.setBaseline(defaultSettings)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentSettings = defaultSettings,
                        originalSettings = defaultSettings.copy(),
                        errorMessage = "Failed to load settings, using defaults"
                    )
                }
            }

    fun getPendingChanges(): List<SettingChange> {
        return changeManager.getModifiedFieldsDetails(_uiState.value.currentSettings)
    }
        }
    }

    fun loadBackups() {
        viewModelScope.launch {
            val backups = withContext(Dispatchers.IO) {
                gameManager.loadBackups().reversed()
            }
            _uiState.update { it.copy(backups = backups) }
        }
    }

    fun updateSettings(newSettings: GraphicsSettings) {
        val current = _uiState.value.currentSettings
        val original = _uiState.value.originalSettings
        
        // Track changes
        changeManager.recordChange("settings", current, newSettings, newSettings)
        
        // Calculate modified fields
        val modifiedFields = mutableSetOf<String>()
        original?.let { orig ->
            if (newSettings.fps != orig.fps) modifiedFields.add("fps")
            if (newSettings.enableVSync != orig.enableVSync) modifiedFields.add("vsync")
            if (newSettings.renderScale != orig.renderScale) modifiedFields.add("renderScale")
            if (newSettings.resolutionQuality != orig.resolutionQuality) modifiedFields.add("resolution")
            if (newSettings.shadowQuality != orig.shadowQuality) modifiedFields.add("shadow")
            if (newSettings.lightQuality != orig.lightQuality) modifiedFields.add("light")
            if (newSettings.characterQuality != orig.characterQuality) modifiedFields.add("character")
            if (newSettings.envDetailQuality != orig.envDetailQuality) modifiedFields.add("environment")
            if (newSettings.reflectionQuality != orig.reflectionQuality) modifiedFields.add("reflection")
            if (newSettings.sfxQuality != orig.sfxQuality) modifiedFields.add("sfx")
            if (newSettings.bloomQuality != orig.bloomQuality) modifiedFields.add("bloom")
            if (newSettings.aaMode != orig.aaMode) modifiedFields.add("aa")
            if (newSettings.enableSelfShadow != orig.enableSelfShadow) modifiedFields.add("selfShadow")
            if (newSettings.enableMetalFXSU != orig.enableMetalFXSU) modifiedFields.add("metalFx")
            if (newSettings.enableHalfResTransparent != orig.enableHalfResTransparent) modifiedFields.add("halfRes")
            if (newSettings.dlssQuality != orig.dlssQuality) modifiedFields.add("dlss")
            if (newSettings.particleTrailSmoothness != orig.particleTrailSmoothness) modifiedFields.add("particleTrail")
            if (newSettings.graphicsQuality != orig.graphicsQuality) modifiedFields.add("graphicsQuality")
            if (newSettings.speedUpOpen != orig.speedUpOpen) modifiedFields.add("speedUpOpen")
        }

        _uiState.update {
            it.copy(
                currentSettings = newSettings,
                hasChanges = modifiedFields.isNotEmpty(),
                pendingChangesCount = modifiedFields.size,
                modifiedFields = modifiedFields
            )
        }
    }

    fun applyPreset(level: Int) {
        val current = _uiState.value.currentSettings.copy()
        current.graphicsQuality = 0 // Custom mode

        when (level) {
            0 -> { // Low
                current.fps = 30
                current.enableVSync = true
                current.renderScale = 0.6
                current.resolutionQuality = 0
                current.shadowQuality = 0
                current.lightQuality = 0
                current.characterQuality = 0
                current.envDetailQuality = 0
                current.reflectionQuality = 0
                current.sfxQuality = 1
                current.bloomQuality = 0
                current.aaMode = 0
                current.enableSelfShadow = 0
                current.dlssQuality = 0
                current.particleTrailSmoothness = 0
                current.enableMetalFXSU = false
                current.enableHalfResTransparent = false
            }
            1 -> { // Medium
                current.fps = 60
                current.enableVSync = true
                current.renderScale = 0.8
                current.resolutionQuality = 1
                current.shadowQuality = 1
                current.lightQuality = 1
                current.characterQuality = 1
                current.envDetailQuality = 1
                current.reflectionQuality = 1
                current.sfxQuality = 2
                current.bloomQuality = 1
                current.aaMode = 1
                current.enableSelfShadow = 0
                current.dlssQuality = 0
                current.particleTrailSmoothness = 1
            }
            2 -> { // High
                current.fps = 60
                current.enableVSync = true
                current.renderScale = 1.0
                current.resolutionQuality = 2
                current.shadowQuality = 2
                current.lightQuality = 2
                current.characterQuality = 2
                current.envDetailQuality = 2
                current.reflectionQuality = 2
                current.sfxQuality = 3
                current.bloomQuality = 2
                current.aaMode = 1
                current.enableSelfShadow = 1
                current.dlssQuality = 1
                current.particleTrailSmoothness = 2
            }
            3 -> { // Ultra
                current.fps = 120
                current.enableVSync = false
                current.renderScale = 1.2
                current.resolutionQuality = 3
                current.shadowQuality = 3
                current.lightQuality = 3
                current.characterQuality = 3
                current.envDetailQuality = 3
                current.reflectionQuality = 3
                current.sfxQuality = 4
                current.bloomQuality = 3
                current.aaMode = 2
                current.enableSelfShadow = 2
                current.dlssQuality = 1
                current.particleTrailSmoothness = 3
            }
            4 -> { // Max
                current.fps = 120
                current.enableVSync = false
                current.renderScale = 2.0
                current.resolutionQuality = 5
                current.shadowQuality = 5
                current.lightQuality = 5
                current.characterQuality = 5
                current.envDetailQuality = 5
                current.reflectionQuality = 5
                current.sfxQuality = 5
                current.bloomQuality = 5
                current.aaMode = 2
                current.enableSelfShadow = 2
                current.enableMetalFXSU = true
                current.dlssQuality = 1
                current.particleTrailSmoothness = 3
            }
        }
        
        updateSettings(current)
    }

    fun applySettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val success = withContext(Dispatchers.IO) {
                gameManager.writeSettings(_uiState.value.currentSettings)
            }

            if (success) {
                val settings = _uiState.value.currentSettings
                changeManager.setBaseline(settings)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        originalSettings = settings.copy(),
                        hasChanges = false,
                        pendingChangesCount = 0,
                        modifiedFields = emptySet(),
                        successMessage = "Settings applied successfully"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to apply settings"
                    )
                }
            }
        }
    }

    fun resetToOriginal() {
        _uiState.value.originalSettings?.let { original ->
            changeManager.resetToBaseline()
            _uiState.update {
                it.copy(
                    currentSettings = original.copy(),
                    hasChanges = false,
                    pendingChangesCount = 0,
                    modifiedFields = emptySet()
                )
            }
        }
    }

    fun undo() {
        changeManager.undo()?.let { previousSettings ->
            _uiState.update {
                it.copy(currentSettings = previousSettings)
            }
        }
    }

    fun redo() {
        changeManager.redo()?.let { nextSettings ->
            _uiState.update {
                it.copy(currentSettings = nextSettings)
            }
        }
    }

    fun saveBackup(name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val success = withContext(Dispatchers.IO) {
                gameManager.saveBackup(name, _uiState.value.currentSettings)
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

    fun restoreBackup(backup: BackupData) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val success = withContext(Dispatchers.IO) {
                gameManager.writeSettings(backup.settings)
            }

            if (success) {
                changeManager.setBaseline(backup.settings)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentSettings = backup.settings.copy(),
                        originalSettings = backup.settings.copy(),
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

    fun deleteBackup(backup: BackupData) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                gameManager.deleteBackup(backup)
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

    suspend fun getPrefsContent(): String? = withContext(Dispatchers.IO) {
        gameManager.getPrefsContent()
    }

    fun getPendingChanges(): List<SettingChange> {
        return changeManager.getModifiedFieldsDetails(_uiState.value.currentSettings)
    }
}
