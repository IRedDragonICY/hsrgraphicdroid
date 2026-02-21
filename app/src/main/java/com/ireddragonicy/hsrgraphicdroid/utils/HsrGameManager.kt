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

        val map = getPrefsMap() ?: return null

        return runCatching {
            val encoded = map[GRAPHICS_KEY] as? String ?: run {
                Log.e(TAG, "GraphicsSettings_Model key not found in map")
                return null
            }
            
            GraphicsSettings.fromEncodedString(encoded).also {
                if (it == null) {
                    Log.e(TAG, "Failed to parse encoded string")
                } else {
                    it.screenWidth = map["Screenmanager%20Resolution%20Width"] as? Int ?: 1920
                    it.screenHeight = map["Screenmanager%20Resolution%20Height"] as? Int ?: 1080
                    it.fullscreenMode = map["Screenmanager%20Fullscreen%20mode"] as? Int ?: 1
                    it.graphicsQuality = map["GraphicsSettings_GraphicsQuality"] as? Int ?: 3
                }
            }
        }.onFailure { e ->
            Log.e(TAG, "Error reading settings", e)
        }.getOrNull()
    }

    fun writeSettings(settings: GraphicsSettings): Boolean {
        if (!isRootAvailable) return false
        val map = getPrefsMap() ?: return false

        return runCatching {
            map[GRAPHICS_KEY] = GraphicsSettings.toEncodedString(settings)
            map["Screenmanager%20Resolution%20Width"] = settings.screenWidth
            map["Screenmanager%20Resolution%20Height"] = settings.screenHeight
            map["Screenmanager%20Fullscreen%20mode"] = settings.fullscreenMode
            map["GraphicsSettings_GraphicsQuality"] = settings.graphicsQuality
            
            savePrefsMap(map)
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
        
        val map = getPrefsMap() ?: return null
        
        return runCatching {
            val lastUserId = map[LAST_USER_ID_KEY] as? Int
            val elfOrderHint = map["User_${lastUserId}_ElfOrderNeedShowNewHint"] == 1
            
            GamePreferences(
                lastUserId = lastUserId,
                lastServerName = map[LAST_SERVER_KEY] as? String,
                textLanguage = GamePreferences.getTextLanguageFromCode(
                    map[TEXT_LANGUAGE_KEY] as? String ?: "en"
                ),
                audioLanguage = GamePreferences.getAudioLanguageFromCode(
                    map[AUDIO_LANGUAGE_KEY] as? String ?: "jp"
                ),
                elfOrderNeedShowNewHint = elfOrderHint,
                videoBlacklist = GamePreferences.parseBlacklistFromEncoded(
                    map[VIDEO_BLACKLIST_KEY] as? String
                ),
                audioBlacklist = GamePreferences.parseBlacklistFromEncoded(
                    map[AUDIO_BLACKLIST_KEY] as? String
                ),
                isSaveBattleSpeed = map["OtherSettings_IsSaveBattleSpeed"] as? Int,
                autoBattleOpen = map["OtherSettings_AutoBattleOpen"] as? Int,
                needDownloadAllAssets = map["App_NeedDownloadAllAssets"] as? Int,
                forceUpdateVideo = map["App_ForceUpdateVideo"] as? Int,
                forceUpdateAudio = map["App_ForceUpdateAudio"] as? Int,
                showSimplifiedSkillDesc = lastUserId?.let { map["User_${it}_ShowSimplifiedSkillDesc"] as? Int },
                gridFightSeenSeasonTalentTree = lastUserId?.let { map["User_${it}_GridFightSeenSeasonTalentTree"] as? Int },
                rogueTournEnableGodMode = lastUserId?.let { map["User_${it}_RogueTournEnableGodMode"] as? Int }
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
        val map = getPrefsMap() ?: return false
        
        return runCatching {
            map[TEXT_LANGUAGE_KEY] = GamePreferences.getCodeFromTextLanguage(textLanguage)
            map[AUDIO_LANGUAGE_KEY] = GamePreferences.getCodeFromAudioLanguage(audioLanguage)
            savePrefsMap(map)
        }.getOrDefault(false)
    }

    /**
     * Write video blacklist to skip cutscenes
     */
    fun writeVideoBlacklist(blacklist: List<String>): Boolean {
        if (!isRootAvailable) return false
        val map = getPrefsMap() ?: return false
        
        return runCatching {
            map[VIDEO_BLACKLIST_KEY] = GamePreferences.encodeBlacklistToString(blacklist)
            savePrefsMap(map)
        }.getOrDefault(false)
    }

    /**
     * Write audio blacklist
     */
    fun writeAudioBlacklist(blacklist: List<String>): Boolean {
        if (!isRootAvailable) return false
        val map = getPrefsMap() ?: return false
        
        return runCatching {
            map[AUDIO_BLACKLIST_KEY] = GamePreferences.encodeBlacklistToString(blacklist)
            savePrefsMap(map)
        }.getOrDefault(false)
    }



    /**
     * Write new QoL, Asset Update, and UID specific settings
     */
    fun writeOtherPreferences(prefs: GamePreferences): Boolean {
        if (!isRootAvailable) return false
        val map = getPrefsMap() ?: return false
        
        return runCatching {
            prefs.isSaveBattleSpeed?.let { map["OtherSettings_IsSaveBattleSpeed"] = it }
            prefs.autoBattleOpen?.let { map["OtherSettings_AutoBattleOpen"] = it }
            prefs.needDownloadAllAssets?.let { map["App_NeedDownloadAllAssets"] = it }
            prefs.forceUpdateVideo?.let { map["App_ForceUpdateVideo"] = it }
            prefs.forceUpdateAudio?.let { map["App_ForceUpdateAudio"] = it }
            
            // UID specific settings
            val uid = prefs.lastUserId ?: map[LAST_USER_ID_KEY] as? Int
            
            if (uid != null) {
                prefs.showSimplifiedSkillDesc?.let { map["User_${uid}_ShowSimplifiedSkillDesc"] = it }
                prefs.gridFightSeenSeasonTalentTree?.let { map["User_${uid}_GridFightSeenSeasonTalentTree"] = it }
                prefs.rogueTournEnableGodMode?.let { map["User_${uid}_RogueTournEnableGodMode"] = it }
            }
            
            savePrefsMap(map)
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

    private fun getPrefsMap(): MutableMap<String, Any>? {
        val prefsPath = findPrefsPath() ?: return null
        val content = Shell.cmd("cat $prefsPath").exec().out.joinToString("\n")
        if (content.isEmpty()) return null
        return UnityXmlHandler.parseXml(content).toMutableMap()
    }

    private fun savePrefsMap(map: Map<String, Any>): Boolean {
        val prefsPath = findPrefsPath() ?: return false
        val newContent = UnityXmlHandler.serializeXml(map)
        
        val tempFile = File(context.cacheDir, "temp_prefs.xml").apply {
            writeText(newContent)
        }
        
        return Shell.cmd(
            "cp ${tempFile.absolutePath} $prefsPath && chmod 660 $prefsPath && chown $(stat -c '%u:%g' $(dirname $prefsPath)) $prefsPath"
        ).exec().isSuccess.also { tempFile.delete() }
    }

    private object UnityXmlHandler {
        fun parseXml(xmlString: String): Map<String, Any> {
            val map = mutableMapOf<String, Any>()
            try {
                val factory = org.xmlpull.v1.XmlPullParserFactory.newInstance()
                factory.isNamespaceAware = false
                val parser = factory.newPullParser()
                parser.setInput(java.io.StringReader(xmlString))

                var eventType = parser.eventType
                while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                    if (eventType == org.xmlpull.v1.XmlPullParser.START_TAG) {
                        val tagName = parser.name
                        if (tagName == "int") {
                            val name = parser.getAttributeValue(null, "name")
                            val value = parser.getAttributeValue(null, "value")?.toIntOrNull()
                            if (name != null && value != null) {
                                map[name] = value
                            }
                        } else if (tagName == "string") {
                            val name = parser.getAttributeValue(null, "name")
                            if (name != null) {
                                val text = parser.nextText()
                                map[name] = text
                            }
                        }
                    }
                    eventType = parser.next()
                }
            } catch (e: Exception) {
                android.util.Log.e("UnityXmlHandler", "Failed to parse XML", e)
            }
            return map
        }

        fun serializeXml(map: Map<String, Any>): String {
            val output = java.io.StringWriter()
            try {
                val serializer = android.util.Xml.newSerializer()
                serializer.setOutput(output)
                serializer.startDocument("utf-8", true)
                
                val nl = "\n"
                serializer.text(nl)
                serializer.startTag(null, "map")
                serializer.text(nl)
                
                for ((key, value) in map) {
                    serializer.text("    ")
                    when (value) {
                        is Int -> {
                            serializer.startTag(null, "int")
                            serializer.attribute(null, "name", key)
                            serializer.attribute(null, "value", value.toString())
                            serializer.endTag(null, "int")
                        }
                        is String -> {
                            serializer.startTag(null, "string")
                            serializer.attribute(null, "name", key)
                            serializer.text(value)
                            serializer.endTag(null, "string")
                        }
                    }
                    serializer.text(nl)
                }
                
                serializer.endTag(null, "map")
                serializer.endDocument()
            } catch (e: Exception) {
                android.util.Log.e("UnityXmlHandler", "Failed to serialize XML", e)
            }
            
            return output.toString().replace("standalone='true'", "standalone=\"yes\"")
        }
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
