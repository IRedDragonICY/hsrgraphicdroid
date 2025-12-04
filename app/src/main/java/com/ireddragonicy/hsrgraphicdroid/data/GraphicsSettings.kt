package com.ireddragonicy.hsrgraphicdroid.data

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.net.URLDecoder
import java.net.URLEncoder

data class GraphicsSettings(
    @SerializedName("FPS")
    var fps: Int = 60,
    
    @SerializedName("EnableVSync")
    var enableVSync: Boolean = true,
    
    @SerializedName("RenderScale")
    var renderScale: Double = 1.0,
    
    @SerializedName("ResolutionQuality")
    var resolutionQuality: Int = 3,
    
    @SerializedName("ShadowQuality")
    var shadowQuality: Int = 3,
    
    @SerializedName("LightQuality")
    var lightQuality: Int = 3,
    
    @SerializedName("CharacterQuality")
    var characterQuality: Int = 3,
    
    @SerializedName("EnvDetailQuality")
    var envDetailQuality: Int = 3,
    
    @SerializedName("ReflectionQuality")
    var reflectionQuality: Int = 3,
    
    @SerializedName("SFXQuality")
    var sfxQuality: Int = 3,
    
    @SerializedName("BloomQuality")
    var bloomQuality: Int = 3,
    
    @SerializedName("AAMode")
    var aaMode: Int = 1,
    
    @SerializedName("EnableMetalFXSU")
    var enableMetalFXSU: Boolean = false,
    
    @SerializedName("EnableHalfResTransparent")
    var enableHalfResTransparent: Boolean = false,
    
    @SerializedName("EnableSelfShadow")
    var enableSelfShadow: Int = 1,
    
    @SerializedName("DlssQuality")
    var dlssQuality: Int = 0,
    
    @SerializedName("ParticleTrailSmoothness")
    var particleTrailSmoothness: Int = 0,
    
    @SerializedName("Screenmanager Resolution Width")
    var screenWidth: Int = 1920,
    
    @SerializedName("Screenmanager Resolution Height")
    var screenHeight: Int = 1080,
    
    @SerializedName("GraphicsSettings_GraphicsQuality")
    var graphicsQuality: Int = 3,
    
    @SerializedName("Screenmanager Fullscreen mode")
    var fullscreenMode: Int = 1,
    
    @SerializedName("User_800754040_SpeedUpOpen")
    var speedUpOpen: Int = 1
) {
    companion object {
        private val gson = Gson()
        
        fun fromEncodedString(encoded: String): GraphicsSettings? {
            return try {
                val decoded = URLDecoder.decode(encoded, "UTF-8")
                gson.fromJson(decoded, GraphicsSettings::class.java)
            } catch (e: Exception) {
                null
            }
        }
        
        fun toEncodedString(settings: GraphicsSettings): String {
            val json = gson.toJson(settings)
            return URLEncoder.encode(json, "UTF-8")
        }
    }
    
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
            1 -> "TAA"
            2 -> "SMAA"
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
