package com.ireddragonicy.hsrgraphicdroid.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Professional Settings Change Manager
 * Handles undo/redo operations and change tracking for graphics settings
 */
class SettingsChangeManager {

    // History stacks for undo/redo
    private val undoStack = mutableListOf<SettingsSnapshot>()
    private val redoStack = mutableListOf<SettingsSnapshot>()
    
    // Maximum history size
    private val maxHistorySize = 50

    // Current baseline (last saved/loaded settings from game)
    private var baselineSettings: GraphicsSettings? = null
    
    // Track modified fields
    private val _modifiedFields = MutableStateFlow<Set<String>>(emptySet())
    val modifiedFields: StateFlow<Set<String>> = _modifiedFields.asStateFlow()
    
    // Undo/Redo availability
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()
    
    // Pending changes count
    private val _pendingChangesCount = MutableStateFlow(0)
    val pendingChangesCount: StateFlow<Int> = _pendingChangesCount.asStateFlow()
    
    // External changes detected (from game)
    private val _externalChanges = MutableStateFlow<List<SettingChange>>(emptyList())
    val externalChanges: StateFlow<List<SettingChange>> = _externalChanges.asStateFlow()

    /**
     * Set baseline settings (from game file or after apply)
     */
    fun setBaseline(settings: GraphicsSettings) {
        baselineSettings = settings.copy()
        undoStack.clear()
        redoStack.clear()
        _modifiedFields.value = emptySet()
        _pendingChangesCount.value = 0
        updateUndoRedoState()
    }

    /**
     * Record a setting change for undo/redo
     */
    fun recordChange(fieldName: String, oldValue: Any?, newValue: Any?, currentSettings: GraphicsSettings) {
        if (oldValue == newValue) return
        
        val snapshot = SettingsSnapshot(
            settings = currentSettings.copy(),
            fieldName = fieldName,
            oldValue = oldValue,
            newValue = newValue,
            timestamp = System.currentTimeMillis()
        )
        
        undoStack.add(snapshot)
        redoStack.clear()
        
        // Trim history if too large
        while (undoStack.size > maxHistorySize) {
            undoStack.removeAt(0)
        }
        
        updateModifiedFields(currentSettings)
        updateUndoRedoState()
    }

    /**
     * Undo last change
     */
    fun undo(): GraphicsSettings? {
        if (undoStack.isEmpty()) return null
        
        val lastChange = undoStack.removeAt(undoStack.lastIndex)
        redoStack.add(lastChange)
        
        // Get previous state
        val previousSettings = if (undoStack.isNotEmpty()) {
            undoStack.last().settings.copy()
        } else {
            baselineSettings?.copy()
        }
        
        previousSettings?.let { updateModifiedFields(it) }
        updateUndoRedoState()
        
        return previousSettings
    }

    /**
     * Redo last undone change
     */
    fun redo(): GraphicsSettings? {
        if (redoStack.isEmpty()) return null
        
        val redoChange = redoStack.removeAt(redoStack.lastIndex)
        undoStack.add(redoChange)
        
        updateModifiedFields(redoChange.settings)
        updateUndoRedoState()
        
        return redoChange.settings.copy()
    }

    /**
     * Compare with game settings and detect external changes
     */
    fun detectExternalChanges(gameSettings: GraphicsSettings, localSettings: GraphicsSettings): List<SettingChange> {
        val changes = mutableListOf<SettingChange>()
        
        compareField("FPS", localSettings.fps, gameSettings.fps)?.let { changes.add(it) }
        compareField("VSync", localSettings.enableVSync, gameSettings.enableVSync)?.let { changes.add(it) }
        compareField("Render Scale", localSettings.renderScale, gameSettings.renderScale)?.let { changes.add(it) }
        compareField("Resolution Quality", localSettings.resolutionQuality, gameSettings.resolutionQuality)?.let { changes.add(it) }
        compareField("Shadow Quality", localSettings.shadowQuality, gameSettings.shadowQuality)?.let { changes.add(it) }
        compareField("Light Quality", localSettings.lightQuality, gameSettings.lightQuality)?.let { changes.add(it) }
        compareField("Character Quality", localSettings.characterQuality, gameSettings.characterQuality)?.let { changes.add(it) }
        compareField("Environment Quality", localSettings.envDetailQuality, gameSettings.envDetailQuality)?.let { changes.add(it) }
        compareField("Reflection Quality", localSettings.reflectionQuality, gameSettings.reflectionQuality)?.let { changes.add(it) }
        compareField("SFX Quality", localSettings.sfxQuality, gameSettings.sfxQuality)?.let { changes.add(it) }
        compareField("Bloom Quality", localSettings.bloomQuality, gameSettings.bloomQuality)?.let { changes.add(it) }
        compareField("Anti-Aliasing", localSettings.aaMode, gameSettings.aaMode)?.let { changes.add(it) }
        compareField("Self Shadow", localSettings.enableSelfShadow, gameSettings.enableSelfShadow)?.let { changes.add(it) }
        compareField("MetalFX", localSettings.enableMetalFXSU, gameSettings.enableMetalFXSU)?.let { changes.add(it) }
        compareField("Half Res Transparent", localSettings.enableHalfResTransparent, gameSettings.enableHalfResTransparent)?.let { changes.add(it) }
        compareField("DLSS Quality", localSettings.dlssQuality, gameSettings.dlssQuality)?.let { changes.add(it) }
        compareField("Particle Trail", localSettings.particleTrailSmoothness, gameSettings.particleTrailSmoothness)?.let { changes.add(it) }
        compareField("Graphics Quality", localSettings.graphicsQuality, gameSettings.graphicsQuality)?.let { changes.add(it) }
        
        _externalChanges.value = changes
        return changes
    }

    /**
     * Clear external changes notification
     */
    fun clearExternalChanges() {
        _externalChanges.value = emptyList()
    }

    /**
     * Check if a specific field is modified from baseline
     */
    fun isFieldModified(fieldName: String): Boolean {
        return _modifiedFields.value.contains(fieldName)
    }

    /**
     * Get all modified fields with their changes
     */
    fun getModifiedFieldsDetails(currentSettings: GraphicsSettings): List<SettingChange> {
        val baseline = baselineSettings ?: return emptyList()
        val changes = mutableListOf<SettingChange>()
        
        if (currentSettings.fps != baseline.fps) {
            changes.add(SettingChange("FPS", baseline.fps.toString(), currentSettings.fps.toString()))
        }
        if (currentSettings.enableVSync != baseline.enableVSync) {
            changes.add(SettingChange("VSync", baseline.enableVSync.toString(), currentSettings.enableVSync.toString()))
        }
        if (currentSettings.renderScale != baseline.renderScale) {
            changes.add(SettingChange("Render Scale", baseline.renderScale.toString(), currentSettings.renderScale.toString()))
        }
        if (currentSettings.resolutionQuality != baseline.resolutionQuality) {
            changes.add(SettingChange("Resolution Quality", baseline.resolutionQuality.toString(), currentSettings.resolutionQuality.toString()))
        }
        if (currentSettings.shadowQuality != baseline.shadowQuality) {
            changes.add(SettingChange("Shadow Quality", baseline.shadowQuality.toString(), currentSettings.shadowQuality.toString()))
        }
        if (currentSettings.lightQuality != baseline.lightQuality) {
            changes.add(SettingChange("Light Quality", baseline.lightQuality.toString(), currentSettings.lightQuality.toString()))
        }
        if (currentSettings.characterQuality != baseline.characterQuality) {
            changes.add(SettingChange("Character Quality", baseline.characterQuality.toString(), currentSettings.characterQuality.toString()))
        }
        if (currentSettings.envDetailQuality != baseline.envDetailQuality) {
            changes.add(SettingChange("Environment Quality", baseline.envDetailQuality.toString(), currentSettings.envDetailQuality.toString()))
        }
        if (currentSettings.reflectionQuality != baseline.reflectionQuality) {
            changes.add(SettingChange("Reflection Quality", baseline.reflectionQuality.toString(), currentSettings.reflectionQuality.toString()))
        }
        if (currentSettings.sfxQuality != baseline.sfxQuality) {
            changes.add(SettingChange("SFX Quality", baseline.sfxQuality.toString(), currentSettings.sfxQuality.toString()))
        }
        if (currentSettings.bloomQuality != baseline.bloomQuality) {
            changes.add(SettingChange("Bloom Quality", baseline.bloomQuality.toString(), currentSettings.bloomQuality.toString()))
        }
        if (currentSettings.aaMode != baseline.aaMode) {
            changes.add(SettingChange("Anti-Aliasing", baseline.aaMode.toString(), currentSettings.aaMode.toString()))
        }
        if (currentSettings.enableSelfShadow != baseline.enableSelfShadow) {
            changes.add(SettingChange("Self Shadow", baseline.enableSelfShadow.toString(), currentSettings.enableSelfShadow.toString()))
        }
        if (currentSettings.enableMetalFXSU != baseline.enableMetalFXSU) {
            changes.add(SettingChange("MetalFX", baseline.enableMetalFXSU.toString(), currentSettings.enableMetalFXSU.toString()))
        }
        if (currentSettings.enableHalfResTransparent != baseline.enableHalfResTransparent) {
            changes.add(SettingChange("Half Res Transparent", baseline.enableHalfResTransparent.toString(), currentSettings.enableHalfResTransparent.toString()))
        }
        if (currentSettings.dlssQuality != baseline.dlssQuality) {
            changes.add(SettingChange("DLSS Quality", baseline.dlssQuality.toString(), currentSettings.dlssQuality.toString()))
        }
        if (currentSettings.particleTrailSmoothness != baseline.particleTrailSmoothness) {
            changes.add(SettingChange("Particle Trail", baseline.particleTrailSmoothness.toString(), currentSettings.particleTrailSmoothness.toString()))
        }
        if (currentSettings.graphicsQuality != baseline.graphicsQuality) {
            changes.add(SettingChange("Graphics Quality", baseline.graphicsQuality.toString(), currentSettings.graphicsQuality.toString()))
        }
        
        return changes
    }

    /**
     * Reset to baseline
     */
    fun resetToBaseline(): GraphicsSettings? {
        baselineSettings?.let { baseline ->
            undoStack.clear()
            redoStack.clear()
            _modifiedFields.value = emptySet()
            _pendingChangesCount.value = 0
            updateUndoRedoState()
            return baseline.copy()
        }
        return null
    }

    private fun updateModifiedFields(currentSettings: GraphicsSettings) {
        val baseline = baselineSettings ?: return
        val modified = mutableSetOf<String>()
        
        if (currentSettings.fps != baseline.fps) modified.add("fps")
        if (currentSettings.enableVSync != baseline.enableVSync) modified.add("vsync")
        if (currentSettings.renderScale != baseline.renderScale) modified.add("renderScale")
        if (currentSettings.resolutionQuality != baseline.resolutionQuality) modified.add("resolution")
        if (currentSettings.shadowQuality != baseline.shadowQuality) modified.add("shadow")
        if (currentSettings.lightQuality != baseline.lightQuality) modified.add("light")
        if (currentSettings.characterQuality != baseline.characterQuality) modified.add("character")
        if (currentSettings.envDetailQuality != baseline.envDetailQuality) modified.add("environment")
        if (currentSettings.reflectionQuality != baseline.reflectionQuality) modified.add("reflection")
        if (currentSettings.sfxQuality != baseline.sfxQuality) modified.add("sfx")
        if (currentSettings.bloomQuality != baseline.bloomQuality) modified.add("bloom")
        if (currentSettings.aaMode != baseline.aaMode) modified.add("aa")
        if (currentSettings.enableSelfShadow != baseline.enableSelfShadow) modified.add("selfShadow")
        if (currentSettings.enableMetalFXSU != baseline.enableMetalFXSU) modified.add("metalFx")
        if (currentSettings.enableHalfResTransparent != baseline.enableHalfResTransparent) modified.add("halfRes")
        if (currentSettings.dlssQuality != baseline.dlssQuality) modified.add("dlss")
        if (currentSettings.particleTrailSmoothness != baseline.particleTrailSmoothness) modified.add("particleTrail")
        if (currentSettings.graphicsQuality != baseline.graphicsQuality) modified.add("graphicsQuality")
        
        _modifiedFields.value = modified
        _pendingChangesCount.value = modified.size
    }

    private fun updateUndoRedoState() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    private fun compareField(name: String, localValue: Any?, gameValue: Any?): SettingChange? {
        return if (localValue != gameValue) {
            SettingChange(name, localValue.toString(), gameValue.toString())
        } else null
    }
}

/**
 * Snapshot of settings at a point in time
 */
data class SettingsSnapshot(
    val settings: GraphicsSettings,
    val fieldName: String,
    val oldValue: Any?,
    val newValue: Any?,
    val timestamp: Long
)

/**
 * Represents a change in a setting
 */
data class SettingChange(
    val fieldName: String,
    val localValue: String,
    val gameValue: String
) {
    fun getDisplayText(): String {
        return "$fieldName: $localValue → $gameValue"
    }
}
