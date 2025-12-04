package com.ireddragonicy.hsrgraphicdroid.utils

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ireddragonicy.hsrgraphicdroid.data.BackupData
import com.ireddragonicy.hsrgraphicdroid.data.GamePreferences
import com.ireddragonicy.hsrgraphicdroid.data.GraphicsSettings
import com.topjohnwu.superuser.Shell
import java.io.File

/**
 * Manager class for interacting with Honkai: Star Rail game data.
 * Handles game detection, graphics settings read/write, and backup management.
 */
class HsrGameManager(private val context: Context) {

    private val gson = Gson()
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
            val newContent = content.replace(GRAPHICS_REGEX) {
                """<string name="$GRAPHICS_KEY">$encoded</string>"""
            }

            val tempFile = File(context.cacheDir, "temp_prefs.xml").apply {
                writeText(newContent)
            }

            Shell.cmd(
                "cp ${tempFile.absolutePath} $prefsPath",
                "chmod 660 $prefsPath",
                "chown $(stat -c '%u:%g' $(dirname $prefsPath)) $prefsPath"
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
            
            GamePreferences(
                lastUserId = LAST_USER_ID_REGEX.find(content)?.groupValues?.get(1)?.toIntOrNull(),
                lastServerName = LAST_SERVER_REGEX.find(content)?.groupValues?.get(1),
                textLanguage = TEXT_LANGUAGE_REGEX.find(content)?.groupValues?.get(1)?.toIntOrNull() ?: 2,
                audioLanguage = AUDIO_LANGUAGE_REGEX.find(content)?.groupValues?.get(1)?.toIntOrNull() ?: 2,
                elfOrderNeedShowNewHint = elfOrderHint,
                videoBlacklist = GamePreferences.parseBlacklistFromEncoded(
                    VIDEO_BLACKLIST_REGEX.find(content)?.groupValues?.get(1)
                ),
                audioBlacklist = GamePreferences.parseBlacklistFromEncoded(
                    AUDIO_BLACKLIST_REGEX.find(content)?.groupValues?.get(1)
                )
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
            
            // Update or add text language
            content = if (TEXT_LANGUAGE_REGEX.containsMatchIn(content)) {
                content.replace(TEXT_LANGUAGE_REGEX) {
                    """<int name="$TEXT_LANGUAGE_KEY" value="$textLanguage" />"""
                }
            } else {
                content.replace("</map>", """    <int name="$TEXT_LANGUAGE_KEY" value="$textLanguage" />
</map>""")
            }
            
            // Update or add audio language
            content = if (AUDIO_LANGUAGE_REGEX.containsMatchIn(content)) {
                content.replace(AUDIO_LANGUAGE_REGEX) {
                    """<int name="$AUDIO_LANGUAGE_KEY" value="$audioLanguage" />"""
                }
            } else {
                content.replace("</map>", """    <int name="$AUDIO_LANGUAGE_KEY" value="$audioLanguage" />
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
            "cp ${tempFile.absolutePath} $prefsPath",
            "chmod 660 $prefsPath",
            "chown $(stat -c '%u:%g' $(dirname $prefsPath)) $prefsPath"
        ).exec().isSuccess.also { tempFile.delete() }
    }

    // endregion

    // region Backup Management

    fun saveBackup(name: String, settings: GraphicsSettings): Boolean = runCatching {
        val backups = loadBackups().toMutableList()
        backups.add(BackupData(System.currentTimeMillis(), settings, name))
        backupFile.writeText(gson.toJson(backups))
        true
    }.getOrDefault(false)

    fun loadBackups(): List<BackupData> = runCatching {
        if (!backupFile.exists()) return emptyList()
        val type = object : TypeToken<List<BackupData>>() {}.type
        gson.fromJson<List<BackupData>>(backupFile.readText(), type)
    }.getOrDefault(emptyList())

    fun deleteBackup(backup: BackupData): Boolean = runCatching {
        val backups = loadBackups().filterNot { it.timestamp == backup.timestamp }
        backupFile.writeText(gson.toJson(backups))
        true
    }.getOrDefault(false)

    // endregion

    // region Private Helpers

    private val backupFile: File
        get() = File(context.filesDir, BACKUP_FILE)

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

        // Game Preferences Keys
        private const val LAST_USER_ID_KEY = "App_LastUserID"
        private const val LAST_SERVER_KEY = "App_LastServerName"
        private const val TEXT_LANGUAGE_KEY = "LanguageSettings_LocalTextLanguage"
        private const val AUDIO_LANGUAGE_KEY = "LanguageSettings_LocalAudioLanguage"
        private const val VIDEO_BLACKLIST_KEY = "App_VideoBlacklist"
        private const val AUDIO_BLACKLIST_KEY = "App_AudioBlacklist"

        private val GRAPHICS_REGEX = 
            """<string name="$GRAPHICS_KEY">([^<]+)</string>""".toRegex()

        // Game Preferences Regex patterns
        private val LAST_USER_ID_REGEX =
            """<int name="$LAST_USER_ID_KEY" value="(\d+)" />""".toRegex()
        private val LAST_SERVER_REGEX =
            """<string name="$LAST_SERVER_KEY">([^<]*)</string>""".toRegex()
        private val TEXT_LANGUAGE_REGEX =
            """<int name="$TEXT_LANGUAGE_KEY" value="(\d+)" />""".toRegex()
        private val AUDIO_LANGUAGE_REGEX =
            """<int name="$AUDIO_LANGUAGE_KEY" value="(\d+)" />""".toRegex()
        private val VIDEO_BLACKLIST_REGEX =
            """<string name="$VIDEO_BLACKLIST_KEY">([^<]*)</string>""".toRegex()
        private val AUDIO_BLACKLIST_REGEX =
            """<string name="$AUDIO_BLACKLIST_KEY">([^<]*)</string>""".toRegex()

        private val PREFS_PATH_TEMPLATES = listOf(
            "/data_mirror/data_ce/null/0/%s/shared_prefs/%s",
            "/data/user_de/0/%s/shared_prefs/%s",
            "/data/user/0/%s/shared_prefs/%s",
            "/data/data/%s/shared_prefs/%s"
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
