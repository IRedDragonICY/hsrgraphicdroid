package com.ireddragonicy.hsrgraphicdroid.data

/**
 * Professional Settings Change Manager
 * Handles undo/redo operations and change tracking for graphics settings
 */
class SettingsChangeManager : BaseChangeManager<GraphicsSettings>() {

    override fun copySettings(settings: GraphicsSettings): GraphicsSettings {
        return settings.copy()
    }

    /**
     * Compare with game settings and detect external changes
     */
    override fun detectExternalChanges(gameSettings: GraphicsSettings, localSettings: GraphicsSettings): List<SettingChange> {
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
        compareField("PSO Shader Warmup", localSettings.enablePsoShaderWarmup, gameSettings.enablePsoShaderWarmup)?.let { changes.add(it) }
        compareField("Is User Save", localSettings.isUserSave, gameSettings.isUserSave)?.let { changes.add(it) }
        compareField("Version", localSettings.version, gameSettings.version)?.let { changes.add(it) }
        compareField("Graphics Quality", localSettings.graphicsQuality, gameSettings.graphicsQuality)?.let { changes.add(it) }
        
        _externalChanges.value = changes
        return changes
    }

    /**
     * Get all modified fields with their changes
     */
    override fun getModifiedFieldsDetails(currentSettings: GraphicsSettings): List<SettingChange> {
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
        if (currentSettings.enablePsoShaderWarmup != baseline.enablePsoShaderWarmup) {
            changes.add(SettingChange("PSO Shader Warmup", baseline.enablePsoShaderWarmup.toString(), currentSettings.enablePsoShaderWarmup.toString()))
        }
        if (currentSettings.isUserSave != baseline.isUserSave) {
            changes.add(SettingChange("Is User Save", baseline.isUserSave.toString(), currentSettings.isUserSave.toString()))
        }
        if (currentSettings.version != baseline.version) {
            changes.add(SettingChange("Version", baseline.version.toString(), currentSettings.version.toString()))
        }
        if (currentSettings.graphicsQuality != baseline.graphicsQuality) {
            changes.add(SettingChange("Graphics Quality", baseline.graphicsQuality.toString(), currentSettings.graphicsQuality.toString()))
        }
        
        return changes
    }

    override fun updateModifiedFields(currentSettings: GraphicsSettings) {
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
        if (currentSettings.enablePsoShaderWarmup != baseline.enablePsoShaderWarmup) modified.add("psoShader")
        if (currentSettings.isUserSave != baseline.isUserSave) modified.add("isUserSave")
        if (currentSettings.version != baseline.version) modified.add("version")
        if (currentSettings.graphicsQuality != baseline.graphicsQuality) modified.add("graphicsQuality")
        
        _modifiedFields.value = modified
        _pendingChangesCount.value = modified.size
    }
}
