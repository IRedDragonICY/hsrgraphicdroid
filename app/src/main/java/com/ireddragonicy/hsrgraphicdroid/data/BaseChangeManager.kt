package com.ireddragonicy.hsrgraphicdroid.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Snapshot of settings at a point in time
 */
data class SettingsSnapshot<T>(
    val settings: T,
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

/**
 * Professional Base Change Manager
 * Handles undo/redo operations and change tracking generically
 */
abstract class BaseChangeManager<T> {

    // History stacks for undo/redo
    private val undoStack = mutableListOf<SettingsSnapshot<T>>()
    private val redoStack = mutableListOf<SettingsSnapshot<T>>()
    
    // Maximum history size
    private val maxHistorySize = 50

    // Current baseline (last saved/loaded settings from game)
    protected var baselineSettings: T? = null
    
    // Track modified fields
    protected val _modifiedFields = MutableStateFlow<Set<String>>(emptySet())
    val modifiedFields: StateFlow<Set<String>> = _modifiedFields.asStateFlow()
    
    // Undo/Redo availability
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()
    
    // Pending changes count
    protected val _pendingChangesCount = MutableStateFlow(0)
    val pendingChangesCount: StateFlow<Int> = _pendingChangesCount.asStateFlow()
    
    // External changes detected (from game)
    protected val _externalChanges = MutableStateFlow<List<SettingChange>>(emptyList())
    val externalChanges: StateFlow<List<SettingChange>> = _externalChanges.asStateFlow()

    abstract fun copySettings(settings: T): T
    
    /**
     * Set baseline settings (from game file or after apply)
     */
    fun setBaseline(settings: T) {
        baselineSettings = copySettings(settings)
        undoStack.clear()
        redoStack.clear()
        _modifiedFields.value = emptySet()
        _pendingChangesCount.value = 0
        updateUndoRedoState()
    }

    /**
     * Record a setting change for undo/redo
     */
    fun recordChange(fieldName: String, oldValue: Any?, newValue: Any?, currentSettings: T) {
        if (oldValue == newValue) return
        
        val snapshot = SettingsSnapshot(
            settings = copySettings(currentSettings),
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
    fun undo(): T? {
        if (undoStack.isEmpty()) return null
        
        val lastChange = undoStack.removeAt(undoStack.lastIndex)
        redoStack.add(lastChange)
        
        // Get previous state
        val previousSettings = if (undoStack.isNotEmpty()) {
            copySettings(undoStack.last().settings)
        } else {
            baselineSettings?.let { copySettings(it) }
        }
        
        previousSettings?.let { updateModifiedFields(it) }
        updateUndoRedoState()
        
        return previousSettings
    }

    /**
     * Redo last undone change
     */
    fun redo(): T? {
        if (redoStack.isEmpty()) return null
        
        val redoChange = redoStack.removeAt(redoStack.lastIndex)
        undoStack.add(redoChange)
        
        updateModifiedFields(redoChange.settings)
        updateUndoRedoState()
        
        return copySettings(redoChange.settings)
    }

    /**
     * Compare with game settings and detect external changes
     */
    abstract fun detectExternalChanges(gameSettings: T, localSettings: T): List<SettingChange>

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
    abstract fun getModifiedFieldsDetails(currentSettings: T): List<SettingChange>

    /**
     * Reset to baseline
     */
    fun resetToBaseline(): T? {
        baselineSettings?.let { baseline ->
            undoStack.clear()
            redoStack.clear()
            _modifiedFields.value = emptySet()
            _pendingChangesCount.value = 0
            updateUndoRedoState()
            return copySettings(baseline)
        }
        return null
    }

    protected abstract fun updateModifiedFields(currentSettings: T)

    private fun updateUndoRedoState() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    protected fun compareField(name: String, localValue: Any?, gameValue: Any?): SettingChange? {
        return if (localValue != gameValue) {
            SettingChange(name, localValue.toString(), gameValue.toString())
        } else null
    }
}
