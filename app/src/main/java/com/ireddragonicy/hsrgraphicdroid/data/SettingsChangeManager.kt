package com.ireddragonicy.hsrgraphicdroid.data

/**
 * Professional Settings Change Manager
 * Handles undo/redo operations and change tracking for graphics settings
 */
class SettingsChangeManager : BaseChangeManager<GraphicsSettings>() {

    private val propertyMap = mapOf(
        "fps" to "FPS",
        "enableVSync" to "VSync",
        "renderScale" to "Render Scale",
        "resolutionQuality" to "Resolution Quality",
        "shadowQuality" to "Shadow Quality",
        "lightQuality" to "Light Quality",
        "characterQuality" to "Character Quality",
        "envDetailQuality" to "Environment Quality",
        "reflectionQuality" to "Reflection Quality",
        "sfxQuality" to "SFX Quality",
        "bloomQuality" to "Bloom Quality",
        "aaMode" to "Anti-Aliasing",
        "enableSelfShadow" to "Self Shadow",
        "enableMetalFXSU" to "MetalFX",
        "enableHalfResTransparent" to "Half Res Transparent",
        "dlssQuality" to "DLSS Quality",
        "particleTrailSmoothness" to "Particle Trail",
        "enablePsoShaderWarmup" to "PSO Shader Warmup",
        "isUserSave" to "Is User Save",
        "version" to "Version",
        "graphicsQuality" to "Graphics Quality"
    )

    override fun copySettings(settings: GraphicsSettings): GraphicsSettings {
        return settings.copy()
    }

    /**
     * Compare with game settings and detect external changes
     */
    override fun detectExternalChanges(gameSettings: GraphicsSettings, localSettings: GraphicsSettings): List<SettingChange> {
        val changes = diffSettings(gameSettings, localSettings, propertyMap)
        _externalChanges.value = changes
        return changes
    }

    /**
     * Get all modified fields with their changes
     */
    override fun getModifiedFieldsDetails(currentSettings: GraphicsSettings): List<SettingChange> {
        val baseline = baselineSettings ?: return emptyList()
        return diffSettings(baseline, currentSettings, propertyMap)
    }

    override fun updateModifiedFields(currentSettings: GraphicsSettings) {
        val baseline = baselineSettings ?: return
        
        val modified = diffSettings(baseline, currentSettings, propertyMap)
            .map { change ->
                // Map the Display Name back to the property map key format expected by UI if needed,
                // or simpler: just use propertyMap keys where the display name matches
                propertyMap.entries.firstOrNull { it.value == change.fieldName }?.key ?: change.fieldName
            }.toSet()
        
        _modifiedFields.value = modified
        _pendingChangesCount.value = modified.size
    }
}
