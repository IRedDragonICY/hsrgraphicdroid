package com.ireddragonicy.hsrgraphicdroid.utils

import android.content.Context
import android.util.Log
import com.ireddragonicy.hsrgraphicdroid.data.BackupData
import com.ireddragonicy.hsrgraphicdroid.data.GamePreferences
import com.ireddragonicy.hsrgraphicdroid.data.GraphicsSettings
import com.topjohnwu.superuser.Shell
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import com.ireddragonicy.hsrgraphicdroid.data.GamePrefsBackupData

/**
 * Manager class for interacting with Honkai: Star Rail game data.
 * Handles game detection, graphics settings read/write, and backup management.
 */
class HsrGameManager(private val context: Context) {

    private var cachedPackage: String? = null
    private var cachedPrefsPath: String? = null

    // region Public API

    val isRootAvailable: Boolean
        get() = Shell.getShell().isRoot

    val installedGamePackage: String?
        get() = cachedPackage ?: detectInstalledPackage().also { cachedPackage = it }

    val isGameInstalled: Boolean
        get() = installedGamePackage != null

    val gameVersionName: String
        get() = GamePackage.fromPackageName(installedGamePackage)?.displayName ?: "Unknown"

    val configPath: String?
        get() = findPrefsPath()

    fun readCurrentSettings(): GraphicsSettings? {
        if (!isRootAvailable) {
            Log.e(TAG, "Root not available!")
            return null
        }

        val prefsPath = findPrefsPath() ?: run {
            Log.e(TAG, "Could not find game preferences file")
            return null
        }

        return runCatching {
            val content = Shell.cmd("cat $prefsPath").exec().out.joinToString("\n")
            if (content.isEmpty()) {
                Log.e(TAG, "Preferences file is empty")
                return null
            }
            
            Log.d(TAG, "File content length: ${content.length}")

            val encoded = GRAPHICS_REGEX.find(content)?.groupValues?.get(1) ?: run {
                Log.e(TAG, "GraphicsSettings_Model key not found in content")
                Log.d(TAG, "Content preview: ${content.take(1000)}")
                return null
            }
            
            Log.d(TAG, "Found encoded string length: ${encoded.length}")

            GraphicsSettings.fromEncodedString(encoded).also {
                if (it == null) {
                    Log.e(TAG, "Failed to parse encoded string: ${encoded.take(200)}")
                } else {
                    Log.d(TAG, "Successfully parsed settings - FPS: ${it.fps}, Quality: ${it.graphicsQuality}")
                    
                    // Parse other fields from XML content
                    it.screenWidth = RESOLUTION_WIDTH_REGEX.find(content)?.groupValues?.get(1)?.toIntOrNull() ?: 1920
                    it.screenHeight = RESOLUTION_HEIGHT_REGEX.find(content)?.groupValues?.get(1)?.toIntOrNull() ?: 1080
                    it.fullscreenMode = FULLSCREEN_MODE_REGEX.find(content)?.groupValues?.get(1)?.toIntOrNull() ?: 1
                    it.graphicsQuality = GRAPHICS_QUALITY_REGEX.find(content)?.groupValues?.get(1)?.toIntOrNull() ?: 3
                }
            }
        }.onFailure { e ->
            Log.e(TAG, "Error reading settings", e)
        }.getOrNull()
    }

    fun writeSettings(settings: GraphicsSettings): Boolean {
        if (!isRootAvailable) return false

        val prefsPath = findPrefsPath() ?: return false
        val pkg = installedGamePackage ?: return false

        return runCatching {
            val content = Shell.cmd("cat $prefsPath").exec().out.joinToString("\n")
            val encoded = GraphicsSettings.toEncodedString(settings)
            
            var newContent = content
            if (GRAPHICS_REGEX.containsMatchIn(content)) {
                newContent = content.replace(GRAPHICS_REGEX) {
                    """<string name="$GRAPHICS_KEY">$encoded</string>"""
                }
            } else {
                // Insert if missing
                newContent = content.replace("</map>", """    <string name="$GRAPHICS_KEY">$encoded</string>
</map>""")
            }
            newContent = updateInt("Screenmanager%20Resolution%20Width", settings.screenWidth, newContent)
            newContent = updateInt("Screenmanager%20Resolution%20Height", settings.screenHeight, newContent)
            newContent = updateInt("Screenmanager%20Fullscreen%20mode", settings.fullscreenMode, newContent)
            // GraphicsSettings_GraphicsQuality is Unity's master quality:
            // 0 = Custom (allows modifying extended settings not in game UI)
            // 1-5 = Game presets (Very Low to Very High) - game will use its default values
            // If user selected a game preset (1-5), game will override GraphicsSettings_Model values
            // If user selected Custom (0), game will use the values from GraphicsSettings_Model
            newContent = updateInt("GraphicsSettings_GraphicsQuality", settings.graphicsQuality, newContent)

            val tempFile = File(context.cacheDir, "temp_prefs.xml").apply {
                writeText(newContent)
            }

            Shell.cmd(
                "cp ${tempFile.absolutePath} $prefsPath && chmod 660 $prefsPath && chown $(stat -c '%u:%g' $(dirname $prefsPath)) $prefsPath"
            ).exec().isSuccess.also { tempFile.delete() }
        }.getOrDefault(false)
    }

    fun killGame(): Boolean {
        if (!isRootAvailable) return false
        val pkg = installedGamePackage ?: return false
        return Shell.cmd("am force-stop $pkg").exec().isSuccess
    }

    /**
     * Export the game preferences XML file to specified output file
     */
    fun exportPrefsFile(outputFile: File): Boolean {
        if (!isRootAvailable) return false
        
        val prefsPath = findPrefsPath() ?: return false
        
        return runCatching {
            val content = Shell.cmd("cat $prefsPath").exec().out.joinToString("\n")
            if (content.isEmpty()) {
                Log.e(TAG, "Preferences file is empty")
                return false
            }
            
            outputFile.writeText(content)
            Log.d(TAG, "Exported prefs to: ${outputFile.absolutePath}")
            true
        }.onFailure { e ->
            Log.e(TAG, "Error exporting preferences", e)
        }.getOrDefault(false)
    }

    /**
     * Get the raw content of game preferences file
     */
    fun getPrefsContent(): String? {
        if (!isRootAvailable) return null
        
        val prefsPath = findPrefsPath() ?: return null
        
        return runCatching {
            Shell.cmd("cat $prefsPath").exec().out.joinToString("\n").takeIf { it.isNotEmpty() }
        }.getOrNull()
    }

    // endregion

    // region Game Preferences (Language, Audio, Blacklist)

    /**
     * Read game preferences (user info, language settings, blacklists)
     */
    fun readGamePreferences(): GamePreferences? {
        if (!isRootAvailable) return null
        
        val prefsPath = findPrefsPath() ?: return null
        
        return runCatching {
            val content = Shell.cmd("cat $prefsPath").exec().out.joinToString("\n")
            if (content.isEmpty()) return null
            
            // Parse Elf Order hint - look for User_*_ElfOrderNeedShowNewHint
            val elfHintRegex = """<int name="User_\d+_ElfOrderNeedShowNewHint" value="(\d+)" />""".toRegex()
            val elfOrderHint = elfHintRegex.find(content)?.groupValues?.get(1)?.toIntOrNull() == 1
            
            val lastUserId = LAST_USER_ID_REGEX.find(content)?.groupValues?.get(1)?.toIntOrNull()
            
            val isSaveBattleSpeed = """<int name="OtherSettings_IsSaveBattleSpeed" value="(-?\d+)"\s*/>""".toRegex().find(content)?.groupValues?.get(1)?.toIntOrNull()
            val autoBattleOpen = """<int name="OtherSettings_AutoBattleOpen" value="(-?\d+)"\s*/>""".toRegex().find(content)?.groupValues?.get(1)?.toIntOrNull()
            val needDownloadAllAssets = """<int name="App_NeedDownloadAllAssets" value="(-?\d+)"\s*/>""".toRegex().find(content)?.groupValues?.get(1)?.toIntOrNull()
            val forceUpdateVideo = """<int name="App_ForceUpdateVideo" value="(-?\d+)"\s*/>""".toRegex().find(content)?.groupValues?.get(1)?.toIntOrNull()
            val forceUpdateAudio = """<int name="App_ForceUpdateAudio" value="(-?\d+)"\s*/>""".toRegex().find(content)?.groupValues?.get(1)?.toIntOrNull()
            
            val showSimplifiedSkillDesc = lastUserId?.let { uid ->
                """<int name="User_${uid}_ShowSimplifiedSkillDesc" value="(-?\d+)"\s*/>""".toRegex().find(content)?.groupValues?.get(1)?.toIntOrNull()
            }
            val gridFightSeenSeasonTalentTree = lastUserId?.let { uid ->
                """<int name="User_${uid}_GridFightSeenSeasonTalentTree" value="(-?\d+)"\s*/>""".toRegex().find(content)?.groupValues?.get(1)?.toIntOrNull()
            }
            val rogueTournEnableGodMode = lastUserId?.let { uid ->
                """<int name="User_${uid}_RogueTournEnableGodMode" value="(-?\d+)"\s*/>""".toRegex().find(content)?.groupValues?.get(1)?.toIntOrNull()
            }
            
            GamePreferences(
                lastUserId = lastUserId,
                lastServerName = LAST_SERVER_REGEX.find(content)?.groupValues?.get(1),
                textLanguage = GamePreferences.getTextLanguageFromCode(
                    TEXT_LANGUAGE_REGEX.find(content)?.groupValues?.get(1) ?: "en"
                ),
                audioLanguage = GamePreferences.getAudioLanguageFromCode(
                    AUDIO_LANGUAGE_REGEX.find(content)?.groupValues?.get(1) ?: "jp"
                ),
                elfOrderNeedShowNewHint = elfOrderHint,
                videoBlacklist = GamePreferences.parseBlacklistFromEncoded(
                    VIDEO_BLACKLIST_REGEX.find(content)?.groupValues?.get(1)
                ),
                audioBlacklist = GamePreferences.parseBlacklistFromEncoded(
                    AUDIO_BLACKLIST_REGEX.find(content)?.groupValues?.get(1)
                ),
                isSaveBattleSpeed = isSaveBattleSpeed,
                autoBattleOpen = autoBattleOpen,
                needDownloadAllAssets = needDownloadAllAssets,
                forceUpdateVideo = forceUpdateVideo,
                forceUpdateAudio = forceUpdateAudio,
                showSimplifiedSkillDesc = showSimplifiedSkillDesc,
                gridFightSeenSeasonTalentTree = gridFightSeenSeasonTalentTree,
                rogueTournEnableGodMode = rogueTournEnableGodMode
            )
        }.onFailure { e ->
            Log.e(TAG, "Error reading game preferences", e)
        }.getOrNull()
    }

    /**
     * Write language settings to game preferences
     */
    fun writeLanguageSettings(textLanguage: Int, audioLanguage: Int): Boolean {
        if (!isRootAvailable) return false
        val prefsPath = findPrefsPath() ?: return false
        
        return runCatching {
            var content = Shell.cmd("cat $prefsPath").exec().out.joinToString("\n")
            
            val textCode = GamePreferences.getCodeFromTextLanguage(textLanguage)
            val audioCode = GamePreferences.getCodeFromAudioLanguage(audioLanguage)
            
            // Update or add text language
            content = if (TEXT_LANGUAGE_REGEX.containsMatchIn(content)) {
                content.replace(TEXT_LANGUAGE_REGEX) {
                    """<string name="$TEXT_LANGUAGE_KEY">$textCode</string>"""
                }
            } else {
                content.replace("</map>", """    <string name="$TEXT_LANGUAGE_KEY">$textCode</string>
</map>""")
            }
            
            // Update or add audio language
            content = if (AUDIO_LANGUAGE_REGEX.containsMatchIn(content)) {
                content.replace(AUDIO_LANGUAGE_REGEX) {
                    """<string name="$AUDIO_LANGUAGE_KEY">$audioCode</string>"""
                }
            } else {
                content.replace("</map>", """    <string name="$AUDIO_LANGUAGE_KEY">$audioCode</string>
</map>""")
            }
            
            writePrefsContent(content)
        }.getOrDefault(false)
    }

    /**
     * Write video blacklist to skip cutscenes
     */
    fun writeVideoBlacklist(blacklist: List<String>): Boolean {
        if (!isRootAvailable) return false
        val prefsPath = findPrefsPath() ?: return false
        
        return runCatching {
            var content = Shell.cmd("cat $prefsPath").exec().out.joinToString("\n")
            val encoded = GamePreferences.encodeBlacklistToString(blacklist)
            
            content = if (VIDEO_BLACKLIST_REGEX.containsMatchIn(content)) {
                content.replace(VIDEO_BLACKLIST_REGEX) {
                    """<string name="$VIDEO_BLACKLIST_KEY">$encoded</string>"""
                }
            } else {
                content.replace("</map>", """    <string name="$VIDEO_BLACKLIST_KEY">$encoded</string>
</map>""")
            }
            
            writePrefsContent(content)
        }.getOrDefault(false)
    }

    /**
     * Write audio blacklist
     */
    fun writeAudioBlacklist(blacklist: List<String>): Boolean {
        if (!isRootAvailable) return false
        val prefsPath = findPrefsPath() ?: return false
        
        return runCatching {
            var content = Shell.cmd("cat $prefsPath").exec().out.joinToString("\n")
            val encoded = GamePreferences.encodeBlacklistToString(blacklist)
            
            content = if (AUDIO_BLACKLIST_REGEX.containsMatchIn(content)) {
                content.replace(AUDIO_BLACKLIST_REGEX) {
                    """<string name="$AUDIO_BLACKLIST_KEY">$encoded</string>"""
                }
            } else {
                content.replace("</map>", """    <string name="$AUDIO_BLACKLIST_KEY">$encoded</string>
</map>""")
            }
            
            writePrefsContent(content)
        }.getOrDefault(false)
    }

    private fun writePrefsContent(content: String): Boolean {
        val prefsPath = findPrefsPath() ?: return false
        
        val tempFile = File(context.cacheDir, "temp_prefs.xml").apply {
            writeText(content)
        }
        
        return Shell.cmd(
            "cp ${tempFile.absolutePath} $prefsPath && chmod 660 $prefsPath && chown $(stat -c '%u:%g' $(dirname $prefsPath)) $prefsPath"
        ).exec().isSuccess.also { tempFile.delete() }
    }

    /**
     * Write new QoL, Asset Update, and UID specific settings
     */
    fun writeOtherPreferences(prefs: GamePreferences): Boolean {
        if (!isRootAvailable) return false
        val prefsPath = findPrefsPath() ?: return false
        
        return runCatching {
            var content = Shell.cmd("cat $prefsPath").exec().out.joinToString("\n")
            
            prefs.isSaveBattleSpeed?.let { content = updateInt("OtherSettings_IsSaveBattleSpeed", it, content) }
            prefs.autoBattleOpen?.let { content = updateInt("OtherSettings_AutoBattleOpen", it, content) }
            prefs.needDownloadAllAssets?.let { content = updateInt("App_NeedDownloadAllAssets", it, content) }
            prefs.forceUpdateVideo?.let { content = updateInt("App_ForceUpdateVideo", it, content) }
            prefs.forceUpdateAudio?.let { content = updateInt("App_ForceUpdateAudio", it, content) }
            
            // UID specific settings
            val uid = prefs.lastUserId ?: LAST_USER_ID_REGEX.find(content)?.groupValues?.get(1)?.toIntOrNull()
            
            if (uid != null) {
                prefs.showSimplifiedSkillDesc?.let { content = updateInt("User_${uid}_ShowSimplifiedSkillDesc", it, content) }
                prefs.gridFightSeenSeasonTalentTree?.let { content = updateInt("User_${uid}_GridFightSeenSeasonTalentTree", it, content) }
                prefs.rogueTournEnableGodMode?.let { content = updateInt("User_${uid}_RogueTournEnableGodMode", it, content) }
            }
            
            writePrefsContent(content)
        }.getOrDefault(false)
    }

    // endregion

    // region Backup Management

    fun saveBackup(name: String, settings: GraphicsSettings): Boolean = runCatching {
        val backups = loadBackupsAsJsonArray()
        val entry = JSONObject()
        entry.put("timestamp", System.currentTimeMillis())
        entry.put("name", name)
        entry.put("settings", GraphicsSettings.toEncodedString(settings))
        backups.put(entry)
        backupFile.writeText(backups.toString())
        true
    }.getOrDefault(false)

    fun loadBackups(): List<BackupData> = runCatching {
        if (!backupFile.exists()) return emptyList()
        val arr = JSONArray(backupFile.readText())
        val result = mutableListOf<BackupData>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val timestamp = obj.optLong("timestamp", 0L)
            val bName = obj.optString("name", "")
            val settingsEncoded = obj.optString("settings", "")
            val settings = GraphicsSettings.fromEncodedString(settingsEncoded) ?: continue
            result.add(BackupData(timestamp, settings, bName))
        }
        result
    }.getOrDefault(emptyList())

    fun deleteBackup(backup: BackupData): Boolean = runCatching {
        val arr = loadBackupsAsJsonArray()
        val filtered = JSONArray()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            if (obj.optLong("timestamp", 0L) != backup.timestamp) {
                filtered.put(obj)
            }
        }
        backupFile.writeText(filtered.toString())
        true
    }.getOrDefault(false)

    private fun loadBackupsAsJsonArray(): JSONArray {
        if (!backupFile.exists()) return JSONArray()
        return runCatching { JSONArray(backupFile.readText()) }.getOrDefault(JSONArray())
    }

    fun savePrefsBackup(name: String, prefs: GamePreferences): Boolean = runCatching {
        val backups = loadPrefsBackupsAsJsonArray()
        val entry = JSONObject()
        entry.put("timestamp", System.currentTimeMillis())
        entry.put("name", name)
        entry.put("prefs", GamePreferences.toEncodedString(prefs))
        backups.put(entry)
        prefsBackupFile.writeText(backups.toString())
        true
    }.getOrDefault(false)

    fun loadPrefsBackups(): List<GamePrefsBackupData> = runCatching {
        if (!prefsBackupFile.exists()) return emptyList()
        val arr = JSONArray(prefsBackupFile.readText())
        val result = mutableListOf<GamePrefsBackupData>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val timestamp = obj.optLong("timestamp", 0L)
            val bName = obj.optString("name", "")
            val prefsEncoded = obj.optString("prefs", "")
            val prefs = GamePreferences.fromEncodedString(prefsEncoded) ?: continue
            result.add(GamePrefsBackupData(timestamp, prefs, bName))
        }
        result
    }.getOrDefault(emptyList())

    fun deletePrefsBackup(backup: GamePrefsBackupData): Boolean = runCatching {
        val arr = loadPrefsBackupsAsJsonArray()
        val filtered = JSONArray()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            if (obj.optLong("timestamp", 0L) != backup.timestamp) {
                filtered.put(obj)
            }
        }
        prefsBackupFile.writeText(filtered.toString())
        true
    }.getOrDefault(false)

    private fun loadPrefsBackupsAsJsonArray(): JSONArray {
        if (!prefsBackupFile.exists()) return JSONArray()
        return runCatching { JSONArray(prefsBackupFile.readText()) }.getOrDefault(JSONArray())
    }

    // endregion

    // region Private Helpers

    private val backupFile: File
        get() = File(context.filesDir, BACKUP_FILE)

    private val prefsBackupFile: File
        get() = File(context.filesDir, PREFS_BACKUP_FILE)

    private fun detectInstalledPackage(): String? {
        for (pkg in GamePackage.entries) {
            runCatching {
                context.packageManager.getPackageInfo(pkg.packageName, 0)
                Log.d(TAG, "Game found: ${pkg.packageName}")
                return pkg.packageName
            }
        }
        Log.w(TAG, "No HSR package found")
        return null
    }

    private fun findPrefsPath(): String? {
        cachedPrefsPath?.let { return it }

        val pkg = installedGamePackage ?: return null
        val fileName = "$pkg.v2.playerprefs.xml"

        val paths = PREFS_PATH_TEMPLATES.map { it.format(pkg, fileName) }

        for (path in paths) {
            val result = Shell.cmd("[ -f '$path' ] && echo exists").exec()
            if (result.out.firstOrNull() == "exists") {
                Log.d(TAG, "Found prefs at: $path")
                cachedPrefsPath = path
                return path
            }
        }

        Log.e(TAG, "Preferences file not found in any location")
        return null
    }

    // endregion

    companion object {
        private const val TAG = "HsrGameManager"
        private const val GRAPHICS_KEY = "GraphicsSettings_Model"
        private const val BACKUP_FILE = "hsr_backups.json"
        private const val PREFS_BACKUP_FILE = "hsr_prefs_backups.json"

        // Game Preferences Keys
        private const val LAST_USER_ID_KEY = "App_LastUserID"
        private const val LAST_SERVER_KEY = "App_LastServerName"
        private const val TEXT_LANGUAGE_KEY = "LanguageSettings_LocalTextLanguage"
        private const val AUDIO_LANGUAGE_KEY = "LanguageSettings_LocalAudioLanguage"
        private const val VIDEO_BLACKLIST_KEY = "App_VideoBlacklist"
        private const val AUDIO_BLACKLIST_KEY = "App_AudioBlacklist"

        private val GRAPHICS_REGEX = 
            """<string name="$GRAPHICS_KEY">([^<]*)</string>""".toRegex()

        // Game Preferences Regex patterns
        private val LAST_USER_ID_REGEX =
            """<int name="$LAST_USER_ID_KEY" value="(\d+)" />""".toRegex()
        private val LAST_SERVER_REGEX =
            """<string name="$LAST_SERVER_KEY">([^<]*)</string>""".toRegex()
        private val TEXT_LANGUAGE_REGEX =
            """<string name="$TEXT_LANGUAGE_KEY">([^<]+)</string>""".toRegex()
        private val AUDIO_LANGUAGE_REGEX =
            """<string name="$AUDIO_LANGUAGE_KEY">([^<]+)</string>""".toRegex()
        private val VIDEO_BLACKLIST_REGEX =
            """<string name="$VIDEO_BLACKLIST_KEY">([^<]*)</string>""".toRegex()
        private val AUDIO_BLACKLIST_REGEX =
            """<string name="$AUDIO_BLACKLIST_KEY">([^<]*)</string>""".toRegex()
            
        private val RESOLUTION_WIDTH_REGEX = 
            """<int name="Screenmanager%20Resolution%20Width" value="(\d+)" />""".toRegex()
        private val RESOLUTION_HEIGHT_REGEX = 
            """<int name="Screenmanager%20Resolution%20Height" value="(\d+)" />""".toRegex()
        private val FULLSCREEN_MODE_REGEX = 
            """<int name="Screenmanager%20Fullscreen%20mode" value="(-?\d+)" />""".toRegex()
        private val GRAPHICS_QUALITY_REGEX = 
            """<int name="GraphicsSettings_GraphicsQuality" value="(\d+)" />""".toRegex()

        // Helper to update int values in XML safely
        fun updateInt(key: String, value: Int, xml: String): String {
            val escapedKey = Regex.escape(key)
            val regex = """<int name="$escapedKey" value="(-?\d+)"\s*/>""".toRegex()
            return if (regex.containsMatchIn(xml)) {
                xml.replace(regex, """<int name="$key" value="$value" />""")
            } else {
                xml.replace("</map>", """    <int name="$key" value="$value" />
</map>""")
            }
        }

        private val PREFS_PATH_TEMPLATES = listOf(
            "/data_mirror/data_ce/null/0/%s/shared_prefs/%s", // Modern Android bypasses mount namespace isolation
            "/data/user/0/%s/shared_prefs/%s",
            "/data/data/%s/shared_prefs/%s",
            "/data/user_de/0/%s/shared_prefs/%s"
        )
    }

    /**
     * Enum representing supported HSR game packages
     */
    private enum class GamePackage(val packageName: String, val displayName: String) {
        GLOBAL("com.HoYoverse.hkrpgoversea", "Global/SEA"),
        CHINA("com.miHoYo.hkrpg", "CN (China)"),
        TW_HK_MO("com.HoYoverse.hkrpg", "TW/HK/MO"),
        COGNOSPHERE("com.cognosphere.hkrpg", "Alternative Global");

        companion object {
            fun fromPackageName(name: String?): GamePackage? =
                entries.find { it.packageName == name }
        }
    }
}
