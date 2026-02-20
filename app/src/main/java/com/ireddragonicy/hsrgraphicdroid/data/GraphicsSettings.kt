package com.ireddragonicy.hsrgraphicdroid.data

import org.json.JSONObject
import java.net.URLDecoder
import java.net.URLEncoder

data class GraphicsSettings(
    var fps: Int = 60,
    var enableVSync: Boolean = true,
    var renderScale: Double = 1.0,
    var resolutionQuality: Int = 3,
    var shadowQuality: Int = 3,
    var lightQuality: Int = 3,
    var characterQuality: Int = 3,
    var envDetailQuality: Int = 3,
    var reflectionQuality: Int = 3,
    var sfxQuality: Int = 3,
    var bloomQuality: Int = 3,
    var aaMode: Int = 1,
    var enableMetalFXSU: Boolean = false,
    var enableHalfResTransparent: Boolean = false,
    var enableSelfShadow: Int = 1,
    var dlssQuality: Int = 0,
    var particleTrailSmoothness: Int = 0,
    var screenWidth: Int = 1920,
    var screenHeight: Int = 1080,
    var graphicsQuality: Int = 3,
    var fullscreenMode: Int = 1,
    var speedUpOpen: Int = 1
) {
    companion object {
        fun fromEncodedString(encoded: String): GraphicsSettings? {
            return try {
                val decoded = URLDecoder.decode(encoded, "UTF-8")
                val json = JSONObject(decoded)
                GraphicsSettings(
                    fps = json.optInt("FPS", 60),
                    enableVSync = json.optBoolean("EnableVSync", true),
                    renderScale = json.optDouble("RenderScale", 1.0),
                    resolutionQuality = json.optInt("ResolutionQuality", 3),
                    shadowQuality = json.optInt("ShadowQuality", 3),
                    lightQuality = json.optInt("LightQuality", 3),
                    characterQuality = json.optInt("CharacterQuality", 3),
                    envDetailQuality = json.optInt("EnvDetailQuality", 3),
                    reflectionQuality = json.optInt("ReflectionQuality", 3),
                    sfxQuality = json.optInt("SFXQuality", 3),
                    bloomQuality = json.optInt("BloomQuality", 3),
                    aaMode = json.optInt("AAMode", 1),
                    enableMetalFXSU = json.optBoolean("EnableMetalFXSU", false),
                    enableHalfResTransparent = json.optBoolean("EnableHalfResTransparent", false),
                    enableSelfShadow = json.optInt("EnableSelfShadow", 1),
                    dlssQuality = json.optInt("DlssQuality", 0),
                    particleTrailSmoothness = json.optInt("ParticleTrailSmoothness", 0)
                )
            } catch (e: Exception) {
                null
            }
        }
        
        fun toEncodedString(settings: GraphicsSettings): String {
            val json = JSONObject()
            json.put("FPS", settings.fps)
            json.put("EnableVSync", settings.enableVSync)
            json.put("RenderScale", settings.renderScale)
            json.put("ResolutionQuality", settings.resolutionQuality)
            json.put("ShadowQuality", settings.shadowQuality)
            json.put("LightQuality", settings.lightQuality)
            json.put("CharacterQuality", settings.characterQuality)
            json.put("EnvDetailQuality", settings.envDetailQuality)
            json.put("ReflectionQuality", settings.reflectionQuality)
            json.put("SFXQuality", settings.sfxQuality)
            json.put("BloomQuality", settings.bloomQuality)
            json.put("AAMode", settings.aaMode)
            json.put("EnableMetalFXSU", settings.enableMetalFXSU)
            json.put("EnableHalfResTransparent", settings.enableHalfResTransparent)
            json.put("EnableSelfShadow", settings.enableSelfShadow)
            json.put("DlssQuality", settings.dlssQuality)
            json.put("ParticleTrailSmoothness", settings.particleTrailSmoothness)
            return URLEncoder.encode(json.toString(), "UTF-8")
        }
    }
    
    /**
     * Get quality name for individual settings sliders (Resolution, Shadow, etc.)
     * 0=Very Low, 1=Low, 2=Medium, 3=High, 4=Very High, 5=Ultra
     */
    fun getQualityName(quality: Int): String {
        return when(quality) {
            0 -> "Very Low"
            1 -> "Low"
            2 -> "Medium"
            3 -> "High"
            4 -> "Very High"
            5 -> "Ultra"
            else -> "Unknown"
        }
    }
    
    /**
     * Get SFX quality name - SFX uses different scale
     * 0 = Not used (will reset to default)
     * 1=Very Low, 2=Low, 3=Medium, 4=High, 5=Very High
     */
    fun getSfxQualityName(quality: Int): String {
        return when(quality) {
            0 -> "Invalid"
            1 -> "Very Low"
            2 -> "Low"
            3 -> "Medium"
            4 -> "High"
            5 -> "Very High"
            else -> "Unknown"
        }
    }
    
    /**
     * Get the master quality name for GraphicsSettings_GraphicsQuality
     * 0 = Custom (user can modify extended settings)
     * 1-5 = Game presets (Very Low to Very High)
     */
    fun getMasterQualityName(quality: Int): String {
        return when(quality) {
            0 -> "Custom"
            1 -> "Very Low"
            2 -> "Low"
            3 -> "Medium"
            4 -> "High"
            5 -> "Very High"
            else -> "Custom"
        }
    }
    
    fun getResolutionPreset(): String {
        return when {
            screenWidth == 640 && screenHeight == 360 -> "360p"
            screenWidth == 1280 && screenHeight == 720 -> "720p"
            screenWidth == 1920 && screenHeight == 1080 -> "1080p"
            screenWidth == 2560 && screenHeight == 1440 -> "1440p"
            screenWidth == 3840 && screenHeight == 2160 -> "4K"
            screenWidth == 7680 && screenHeight == 4320 -> "8K"
            else -> "${screenWidth}x${screenHeight}"
        }
    }
    
    fun getFullscreenModeName(): String {
        return when (fullscreenMode) {
            0 -> "Fullscreen Window"
            1 -> "Exclusive Fullscreen"
            2 -> "Maximized Window"
            3 -> "Windowed"
            else -> "Default"
        }
    }
    
    fun getAAModeName(mode: Int): String {
        return when (mode) {
            0 -> "Off"
            1 -> "FXAA"
            2 -> "TAA"
            else -> "Off"
        }
    }
    
    fun getSelfShadowName(level: Int): String {
        return when (level) {
            0 -> "Off"
            1 -> "Low"
            2 -> "High"
            else -> "Off"
        }
    }
    
    fun getDlssName(quality: Int): String {
        return when (quality) {
            0 -> "Off"
            1 -> "Quality"
            2 -> "Balanced"
            3 -> "Performance"
            4 -> "Ultra Performance"
            else -> "Off"
        }
    }
    
    fun getParticleTrailName(level: Int): String {
        return when (level) {
            0 -> "Off"
            1 -> "Low"
            2 -> "Medium"
            3 -> "High"
            else -> "Off"
        }
    }
    
    fun setResolutionPreset(preset: String) {
        when (preset) {
            "360p" -> {
                screenWidth = 640
                screenHeight = 360
            }
            "720p" -> {
                screenWidth = 1280
                screenHeight = 720
            }
            "1080p" -> {
                screenWidth = 1920
                screenHeight = 1080
            }
            "1440p" -> {
                screenWidth = 2560
                screenHeight = 1440
            }
            "4K" -> {
                screenWidth = 3840
                screenHeight = 2160
            }
            "8K" -> {
                screenWidth = 7680
                screenHeight = 4320
            }
        }
    }
}

data class BackupData(
    val timestamp: Long,
    val settings: GraphicsSettings,
    val name: String
)
