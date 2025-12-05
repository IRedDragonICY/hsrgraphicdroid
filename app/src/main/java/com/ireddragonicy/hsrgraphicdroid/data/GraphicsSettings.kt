package com.ireddragonicy.hsrgraphicdroid.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.net.URLDecoder
import java.net.URLEncoder

data class GraphicsSettings(
    @Expose @SerializedName("FPS")
    var fps: Int = 60,
    
    @Expose @SerializedName("EnableVSync")
    var enableVSync: Boolean = true,
    
    @Expose @SerializedName("RenderScale")
    var renderScale: Double = 1.0,
    
    @Expose @SerializedName("ResolutionQuality")
    var resolutionQuality: Int = 3,
    
    @Expose @SerializedName("ShadowQuality")
    var shadowQuality: Int = 3,
    
    @Expose @SerializedName("LightQuality")
    var lightQuality: Int = 3,
    
    @Expose @SerializedName("CharacterQuality")
    var characterQuality: Int = 3,
    
    @Expose @SerializedName("EnvDetailQuality")
    var envDetailQuality: Int = 3,
    
    @Expose @SerializedName("ReflectionQuality")
    var reflectionQuality: Int = 3,
    
    @Expose @SerializedName("SFXQuality")
    var sfxQuality: Int = 3,
    
    @Expose @SerializedName("BloomQuality")
    var bloomQuality: Int = 3,
    
    @Expose @SerializedName("AAMode")
    var aaMode: Int = 1,
    
    @Expose @SerializedName("EnableMetalFXSU")
    var enableMetalFXSU: Boolean = false,
    
    @Expose @SerializedName("EnableHalfResTransparent")
    var enableHalfResTransparent: Boolean = false,
    
    @Expose @SerializedName("EnableSelfShadow")
    var enableSelfShadow: Int = 1,
    
    @Expose @SerializedName("DlssQuality")
    var dlssQuality: Int = 0,
    
    @Expose @SerializedName("ParticleTrailSmoothness")
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
        private val modelGson = GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()
        
        fun fromEncodedString(encoded: String): GraphicsSettings? {
            return try {
                val decoded = URLDecoder.decode(encoded, "UTF-8")
                modelGson.fromJson(decoded, GraphicsSettings::class.java)
            } catch (e: Exception) {
                null
            }
        }
        
        fun toEncodedString(settings: GraphicsSettings): String {
            // Use LinkedHashMap to preserve order exactly as the game expects
            val map = LinkedHashMap<String, Any>()
            map["FPS"] = settings.fps
            map["EnableVSync"] = settings.enableVSync
            map["RenderScale"] = settings.renderScale
            map["ResolutionQuality"] = settings.resolutionQuality
            map["ShadowQuality"] = settings.shadowQuality
            map["LightQuality"] = settings.lightQuality
            map["CharacterQuality"] = settings.characterQuality
            map["EnvDetailQuality"] = settings.envDetailQuality
            map["ReflectionQuality"] = settings.reflectionQuality
            map["SFXQuality"] = settings.sfxQuality
            map["BloomQuality"] = settings.bloomQuality
            map["AAMode"] = settings.aaMode
            map["EnableMetalFXSU"] = settings.enableMetalFXSU
            map["EnableHalfResTransparent"] = settings.enableHalfResTransparent
            map["EnableSelfShadow"] = settings.enableSelfShadow
            map["DlssQuality"] = settings.dlssQuality
            map["ParticleTrailSmoothness"] = settings.particleTrailSmoothness
            
            val json = gson.toJson(map)
            return URLEncoder.encode(json, "UTF-8")
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
