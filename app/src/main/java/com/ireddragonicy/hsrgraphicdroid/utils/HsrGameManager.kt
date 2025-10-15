package com.ireddragonicy.hsrgraphicdroid.utils

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ireddragonicy.hsrgraphicdroid.data.BackupData
import com.ireddragonicy.hsrgraphicdroid.data.GraphicsSettings
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.io.SuFile
import java.io.File

class HsrGameManager(private val context: Context) {
    
    companion object {
        private const val TAG = "HsrGameManager"
        
        // Supported game packages
        private val GAME_PACKAGES = listOf(
            "com.HoYoverse.hkrpgoversea",  // Global/SEA
            "com.miHoYo.hkrpg",             // CN (China)
            "com.HoYoverse.hkrpg",          // TW/HK/MO
            "com.cognosphere.hkrpg"         // Alternative Global
        )
        
        private const val GRAPHICS_KEY = "GraphicsSettings_Model"
        private const val BACKUP_FILE = "hsr_backups.json"
    }
    
    private val gson = Gson()
    private var detectedPackage: String? = null
    
    fun isRootAvailable(): Boolean {
        return Shell.getShell().isRoot
    }
    
    fun getAllInstalledPackages(): List<String> {
        val packages = context.packageManager.getInstalledPackages(0)
        return packages.map { it.packageName }
    }
    
    fun findHoyoversePackages(): List<String> {
        val allPackages = getAllInstalledPackages()
        val hoyoversePackages = allPackages.filter { 
            it.contains("hoyoverse", ignoreCase = true) || 
            it.contains("mihoyo", ignoreCase = true) ||
            it.contains("cognosphere", ignoreCase = true) ||
            it.contains("hkrpg", ignoreCase = true) ||
            it.contains("starrail", ignoreCase = true)
        }
        
        Log.d(TAG, "Found Hoyoverse/miHoYo packages: $hoyoversePackages")
        return hoyoversePackages
    }
    
    fun isGameInstalled(): Boolean {
        Log.d(TAG, "Checking if game is installed...")
        
        // First check known packages
        for (pkg in GAME_PACKAGES) {
            try {
                val packageInfo = context.packageManager.getPackageInfo(pkg, 0)
                detectedPackage = pkg
                Log.d(TAG, "✓ Game found: $pkg (version: ${packageInfo.versionName})")
                return true
            } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
                Log.d(TAG, "✗ Package not found: $pkg")
            } catch (e: Exception) {
                Log.e(TAG, "Error checking package $pkg: ${e.message}")
            }
        }
        
        // If not found, search for any Hoyoverse package
        val hoyoversePackages = findHoyoversePackages()
        if (hoyoversePackages.isNotEmpty()) {
            Log.w(TAG, "Game not found in known packages, but found related packages: $hoyoversePackages")
        } else {
            Log.w(TAG, "No Hoyoverse/miHoYo packages found at all")
        }
        
        return false
    }
    
    fun getInstalledGamePackage(): String? {
        if (detectedPackage != null) return detectedPackage
        
        // Re-detect if not already detected
        for (pkg in GAME_PACKAGES) {
            try {
                context.packageManager.getPackageInfo(pkg, 0)
                detectedPackage = pkg
                return pkg
            } catch (e: Exception) {
                // Continue checking
            }
        }
        return null
    }
    
    fun getGameDataPath(): String {
        val pkg = getInstalledGamePackage() ?: GAME_PACKAGES[0]
        return "/data/data/$pkg/shared_prefs/$pkg.v2.playerprefs.xml"
    }
    
    fun getGameDataPaths(): List<String> {
        val pkg = getInstalledGamePackage() ?: GAME_PACKAGES[0]
        val fileName = "$pkg.v2.playerprefs.xml"
        
        // Try multiple possible locations for Android 11+
        return listOf(
            "/data_mirror/data_ce/null/0/$pkg/shared_prefs/$fileName",
            "/data/user_de/0/$pkg/shared_prefs/$fileName",
            "/data/user/0/$pkg/shared_prefs/$fileName",
            "/data/data/$pkg/shared_prefs/$fileName"
        )
    }
    
    fun findActualGameDataPath(): String? {
        Log.d(TAG, "Searching for game data file...")
        
        val paths = getGameDataPaths()
        for (path in paths) {
            val checkResult = Shell.cmd("ls -la $path 2>&1").exec()
            if (checkResult.isSuccess && checkResult.out.isNotEmpty() && !checkResult.out[0].contains("No such file")) {
                Log.d(TAG, "✓ Found file at: $path")
                return path
            } else {
                Log.d(TAG, "✗ Not found: $path")
            }
        }
        
        // If not found, try to list shared_prefs directory
        val pkg = getInstalledGamePackage()
        if (pkg != null) {
            Log.d(TAG, "Searching for shared_prefs directory...")
            val basePaths = listOf(
                "/data_mirror/data_ce/null/0/$pkg/shared_prefs/",
                "/data/user_de/0/$pkg/shared_prefs/",
                "/data/user/0/$pkg/shared_prefs/",
                "/data/data/$pkg/shared_prefs/"
            )
            
            for (basePath in basePaths) {
                val listResult = Shell.cmd("ls -la $basePath 2>&1").exec()
                if (listResult.isSuccess && listResult.out.isNotEmpty() && !listResult.out[0].contains("No such file")) {
                    Log.d(TAG, "Found shared_prefs at: $basePath")
                    listResult.out.forEach { Log.d(TAG, "  $it") }
                }
            }
        }
        
        return null
    }
    
    fun readCurrentSettings(): GraphicsSettings? {
        Log.d(TAG, "Attempting to read current settings...")
        
        if (!isRootAvailable()) {
            Log.e(TAG, "Root not available!")
            return null
        }
        
        // Find the actual file path
        val prefsPath = findActualGameDataPath()
        if (prefsPath == null) {
            Log.e(TAG, "Could not find game preferences file in any known location")
            return null
        }
        
        Log.d(TAG, "Using preferences path: $prefsPath")
        
        return try {
            val content = Shell.cmd("cat $prefsPath").exec().out.joinToString("\n")
            Log.d(TAG, "File content length: ${content.length} characters")
            
            if (content.isEmpty()) {
                Log.e(TAG, "File is empty!")
                return null
            }
            
            // Parse XML to find GraphicsSettings_Model value
            val regex = "<string name=\"$GRAPHICS_KEY\">(.*?)</string>".toRegex()
            val match = regex.find(content)
            
            if (match == null) {
                Log.e(TAG, "GraphicsSettings_Model key not found in XML")
                // Log first 500 chars of content to see what's there
                Log.d(TAG, "Content preview: ${content.take(500)}")
                return null
            }
            
            val encoded = match.groupValues[1]
            Log.d(TAG, "Found encoded settings (length: ${encoded.length})")
            
            val settings = GraphicsSettings.fromEncodedString(encoded)
            Log.d(TAG, "Successfully parsed settings!")
            settings
        } catch (e: Exception) {
            Log.e(TAG, "Error reading settings: ${e.message}", e)
            e.printStackTrace()
            null
        }
    }
    
    fun writeSettings(settings: GraphicsSettings): Boolean {
        if (!isRootAvailable()) return false
        
        val prefsPath = findActualGameDataPath()
        if (prefsPath == null) {
            Log.e(TAG, "Could not find game preferences file")
            return false
        }
        
        return try {
            val content = Shell.cmd("cat $prefsPath").exec().out.joinToString("\n")
            val encoded = GraphicsSettings.toEncodedString(settings)
            
            // Replace the GraphicsSettings_Model value
            val regex = "(<string name=\"$GRAPHICS_KEY\">)(.*?)(</string>)".toRegex()
            val newContent = content.replace(regex, "$1$encoded$3")
            
            // Create temp file
            val tempFile = File(context.cacheDir, "temp_prefs.xml")
            tempFile.writeText(newContent)
            
            // Copy to game data with root
            val pkg = getInstalledGamePackage() ?: return false
            val result = Shell.cmd(
                "cp ${tempFile.absolutePath} $prefsPath",
                "chmod 660 $prefsPath",
                "chown $(stat -c '%u:%g' $(dirname $prefsPath)) $prefsPath"
            ).exec()
            
            tempFile.delete()
            
            result.isSuccess
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun saveBackup(name: String, settings: GraphicsSettings): Boolean {
        return try {
            val backups = loadBackups().toMutableList()
            backups.add(BackupData(System.currentTimeMillis(), settings, name))
            
            val json = gson.toJson(backups)
            val file = File(context.filesDir, BACKUP_FILE)
            file.writeText(json)
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun loadBackups(): List<BackupData> {
        return try {
            val file = File(context.filesDir, BACKUP_FILE)
            if (!file.exists()) return emptyList()
            
            val json = file.readText()
            val type = object : TypeToken<List<BackupData>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    fun deleteBackup(backup: BackupData): Boolean {
        return try {
            val backups = loadBackups().toMutableList()
            backups.removeIf { it.timestamp == backup.timestamp }
            
            val json = gson.toJson(backups)
            val file = File(context.filesDir, BACKUP_FILE)
            file.writeText(json)
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun killGame(): Boolean {
        if (!isRootAvailable()) return false
        
        return try {
            val pkg = getInstalledGamePackage() ?: return false
            Shell.cmd("am force-stop $pkg").exec().isSuccess
        } catch (e: Exception) {
            false
        }
    }
    
    fun getGameVersionName(): String {
        val pkg = getInstalledGamePackage() ?: return "Unknown"
        return when (pkg) {
            "com.HoYoverse.hkrpgoversea" -> "Global/SEA"
            "com.miHoYo.hkrpg" -> "CN (China)"
            "com.HoYoverse.hkrpg" -> "TW/HK/MO"
            else -> "Unknown"
        }
    }
}
