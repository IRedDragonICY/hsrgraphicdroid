package com.ireddragonicy.hsrgraphicdroid.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.app.usage.StorageStatsManager
import android.os.Build
import android.os.UserHandle
import android.os.storage.StorageManager
import android.app.LocaleManager
import com.topjohnwu.superuser.Shell
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ireddragonicy.hsrgraphicdroid.data.BackupData
import com.ireddragonicy.hsrgraphicdroid.data.GamePreferences
import com.ireddragonicy.hsrgraphicdroid.data.GraphicsSettings
import com.ireddragonicy.hsrgraphicdroid.ui.AppLanguage
import com.ireddragonicy.hsrgraphicdroid.ui.AppTheme
import com.ireddragonicy.hsrgraphicdroid.utils.HsrGameManager
import com.ireddragonicy.hsrgraphicdroid.utils.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class StatusState(
    val isRootGranted: Boolean = false,
    val isGameInstalled: Boolean = false,
    val gameVersion: String? = null,
    val gamePackage: String? = null,
    val configPath: String? = null,
    val isChecking: Boolean = true
)

data class GameInfo(
    val versionName: String?,
    val versionCode: Long?,
    val apkName: String?,
    val apkSizeBytes: Long?,
    val dataBytes: Long?,
    val cacheBytes: Long?
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val gameManager = HsrGameManager(application)
    private val preferenceManager = PreferenceManager(application)

    private val _status = MutableStateFlow(StatusState())
    val status: StateFlow<StatusState> = _status

    val themeFlow: StateFlow<AppTheme> = preferenceManager.getTheme()
        .map { key -> AppTheme.fromKey(key) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppTheme.SYSTEM)

    val languageFlow: StateFlow<AppLanguage> = preferenceManager.getLanguage()
        .map { key -> AppLanguage.fromKey(key) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppLanguage.SYSTEM)

    init {
        viewModelScope.launch {
            refreshStatus()
        }
    }

    suspend fun refreshStatus() {
        _status.value = _status.value.copy(isChecking = true)
        val isRoot = withContext(Dispatchers.IO) { gameManager.isRootAvailable }
        val installedPackage = withContext(Dispatchers.IO) { gameManager.installedGamePackage }
        val installed = installedPackage != null
        val version = if (installed) {
            withContext(Dispatchers.IO) { gameManager.gameVersionName }
        } else {
            null
        }
        val path = if (installed) {
            withContext(Dispatchers.IO) { gameManager.configPath }
        } else {
            null
        }

        _status.value = StatusState(
            isRootGranted = isRoot,
            isGameInstalled = installed,
            gameVersion = version,
            gamePackage = installedPackage,
            configPath = path,
            isChecking = false
        )
    }

    suspend fun readGraphicsSettings(): GraphicsSettings? = withContext(Dispatchers.IO) {
        gameManager.readCurrentSettings()
    }

    suspend fun writeGraphicsSettings(settings: GraphicsSettings): Boolean = withContext(Dispatchers.IO) {
        gameManager.writeSettings(settings)
    }

    suspend fun killGame(): Boolean = withContext(Dispatchers.IO) {
        gameManager.killGame()
    }

    suspend fun getPrefsContent(): String? = withContext(Dispatchers.IO) {
        gameManager.getPrefsContent()
    }

    suspend fun saveBackup(name: String, settings: GraphicsSettings): Boolean = withContext(Dispatchers.IO) {
        gameManager.saveBackup(name, settings)
    }

    suspend fun loadBackups(): List<BackupData> = withContext(Dispatchers.IO) {
        gameManager.loadBackups()
    }

    suspend fun deleteBackup(backup: BackupData): Boolean = withContext(Dispatchers.IO) {
        gameManager.deleteBackup(backup)
    }

    suspend fun readGamePreferences(): GamePreferences? = withContext(Dispatchers.IO) {
        gameManager.readGamePreferences()
    }

    suspend fun writeLanguageSettings(textLanguage: Int, audioLanguage: Int): Boolean =
        withContext(Dispatchers.IO) { gameManager.writeLanguageSettings(textLanguage, audioLanguage) }

    suspend fun writeVideoBlacklist(blacklist: List<String>): Boolean =
        withContext(Dispatchers.IO) { gameManager.writeVideoBlacklist(blacklist) }

    suspend fun writeAudioBlacklist(blacklist: List<String>): Boolean =
        withContext(Dispatchers.IO) { gameManager.writeAudioBlacklist(blacklist) }

    fun updateTheme(theme: AppTheme) {
        viewModelScope.launch {
            preferenceManager.setTheme(theme.key)
        }
        // Compose reactivity handles dark/light mode via SettingsViewModel.uiState.isDarkMode
    }

    fun updateLanguage(language: AppLanguage) {
        viewModelScope.launch {
            preferenceManager.setLanguage(language.key)
        }
        applyLanguage(getApplication(), language)
    }

    suspend fun bootstrapAppearance() {
        val language = AppLanguage.fromKey(preferenceManager.getLanguage().first())
        // Theme is handled reactively by Compose via SettingsViewModel.uiState.isDarkMode
        applyLanguage(getApplication(), language)
    }

    private fun applyLanguage(context: Context, language: AppLanguage) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            val localeList = AppLanguage.toLocaleList(language).unwrap() as android.os.LocaleList
            localeManager.applicationLocales = localeList
        } else {
            val locale = if (language == AppLanguage.SYSTEM || language.tag.isEmpty()) {
                java.util.Locale.getDefault()
            } else {
                java.util.Locale(language.tag)
            }
            java.util.Locale.setDefault(locale)
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        }
    }

    fun currentPackage(): String? = gameManager.installedGamePackage

    suspend fun loadGameInfo(): GameInfo? = withContext(Dispatchers.IO) {
        val pkg = gameManager.installedGamePackage ?: return@withContext null
        val pm = getApplication<Application>().packageManager
        val pkgInfo = runCatching { pm.getPackageInfo(pkg, 0) }.getOrNull() ?: return@withContext null
        val appInfo = pkgInfo.applicationInfo ?: return@withContext null
        val sourceDir = appInfo.sourceDir ?: return@withContext null
        val apkFile = kotlin.runCatching { java.io.File(sourceDir) }.getOrNull()

        var dataBytes: Long? = null
        var cacheBytes: Long? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val storage = getApplication<Application>().getSystemService(StorageStatsManager::class.java)
            val user = UserHandle.getUserHandleForUid(appInfo.uid)
            val uuid = StorageManager.UUID_DEFAULT
            val stats = runCatching { storage.queryStatsForPackage(uuid, pkg, user) }.getOrNull()
            if (stats != null) {
                dataBytes = stats.dataBytes
                cacheBytes = stats.cacheBytes
            }
        }

        // Root fallback when usage stats permission not granted
        if ((dataBytes == null || cacheBytes == null) && Shell.getShell().isRoot) {
            val rootSizes = readSizesWithRoot(pkg)
            if (dataBytes == null) dataBytes = rootSizes.first
            if (cacheBytes == null) cacheBytes = rootSizes.second
        }

        GameInfo(
            versionName = pkgInfo.versionName,
            versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pkgInfo.longVersionCode else pkgInfo.versionCode.toLong(),
            apkName = pkg,
            apkSizeBytes = apkFile?.length(),
            dataBytes = dataBytes,
            cacheBytes = cacheBytes
        )
    }

    private fun readSizesWithRoot(pkg: String): Pair<Long?, Long?> {
        val internalData = runCatching {
            val res = Shell.cmd("du -sb /data/data/$pkg || du -sb /data/user/0/$pkg").exec()
            res.out.firstOrNull()?.split("\t", " ")?.firstOrNull()?.toLongOrNull()
        }.getOrNull()

        val internalCache = runCatching {
            val res = Shell.cmd("du -sb /data/data/$pkg/cache || du -sb /data/user/0/$pkg/cache").exec()
            res.out.firstOrNull()?.split("\t", " ")?.firstOrNull()?.toLongOrNull()
        }.getOrNull()

        val externalData = runCatching {
            val res = Shell.cmd("du -sb /data/media/0/Android/data/$pkg").exec()
            res.out.firstOrNull()?.split("\t", " ")?.firstOrNull()?.toLongOrNull()
        }.getOrNull()

        val dataWithoutCache = ((internalData ?: 0) - (internalCache ?: 0)).coerceAtLeast(0)
        val dataBytes = dataWithoutCache + (externalData ?: 0)
        val cacheBytes = internalCache

        return dataBytes to cacheBytes
    }
}
