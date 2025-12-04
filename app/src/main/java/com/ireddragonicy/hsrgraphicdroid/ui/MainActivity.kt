package com.ireddragonicy.hsrgraphicdroid.ui

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.ireddragonicy.hsrgraphicdroid.R
import com.ireddragonicy.hsrgraphicdroid.databinding.ActivityMainBinding
import com.ireddragonicy.hsrgraphicdroid.utils.HsrGameManager
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var gameManager: HsrGameManager
    private lateinit var backupAdapter: BackupAdapter
    private var currentSettings: com.ireddragonicy.hsrgraphicdroid.data.GraphicsSettings? = null
    private var pendingXmlContent: String? = null

    // SAF launcher for creating XML file
    private val createXmlFileLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/xml")
    ) { uri ->
        uri?.let { saveXmlToUri(it) }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Handle window insets for AppBarLayout
        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, insets.top, 0, 0)
            windowInsets
        }
        
        setSupportActionBar(binding.toolbar)
        
        gameManager = HsrGameManager(this)
        
        setupRecyclerView()
        setupButtons()
        setupGraphicsEditor()
        checkStatus()
        loadCurrentSettings()
    }
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Update UI saat language/theme berubah tanpa recreate
        recreate()
    }
    
    private fun setupRecyclerView() {
        backupAdapter = BackupAdapter(
            onRestore = { backup ->
                restoreBackup(backup)
            },
            onDelete = { backup ->
                confirmDelete(backup)
            }
        )
        
        binding.recyclerBackups.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = backupAdapter
        }
    }
    
    private fun setupButtons() {
        binding.fabRefresh.setOnClickListener {
            checkStatus()
            loadBackups()
            loadCurrentSettings()
        }

        // Game action buttons
        binding.btnLaunchGame.setOnClickListener {
            launchGame()
        }

        binding.btnGameSettings.setOnClickListener {
            openGameAppInfo()
        }

        binding.btnExportXml.setOnClickListener {
            exportXmlFile()
        }

        binding.btnShareXml.setOnClickListener {
            shareXmlFile()
        }
    }

    private fun exportXmlFile() {
        lifecycleScope.launch {
            binding.progressIndicator.show()
            
            val content = withContext(Dispatchers.IO) {
                gameManager.getPrefsContent()
            }
            
            binding.progressIndicator.hide()
            
            if (content != null) {
                pendingXmlContent = content
                val pkg = gameManager.installedGamePackage ?: "hsr"
                val fileName = "${pkg}_settings_${System.currentTimeMillis()}.xml"
                createXmlFileLauncher.launch(fileName)
            } else {
                Snackbar.make(binding.root, getString(R.string.export_failed), Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveXmlToUri(uri: Uri) {
        val content = pendingXmlContent ?: return
        
        lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) {
                runCatching {
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(content.toByteArray())
                    }
                    true
                }.getOrDefault(false)
            }
            
            pendingXmlContent = null
            
            if (success) {
                Snackbar.make(binding.root, getString(R.string.export_success), Snackbar.LENGTH_LONG)
                    .setAction("Open") {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "text/xml")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        runCatching { startActivity(intent) }
                    }
                    .show()
            } else {
                Snackbar.make(binding.root, getString(R.string.export_failed), Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun shareXmlFile() {
        lifecycleScope.launch {
            binding.progressIndicator.show()
            
            val content = withContext(Dispatchers.IO) {
                gameManager.getPrefsContent()
            }
            
            binding.progressIndicator.hide()
            
            if (content != null) {
                val pkg = gameManager.installedGamePackage ?: "hsr"
                val fileName = "${pkg}_settings.xml"
                
                // Create temp file for sharing
                val shareFile = java.io.File(cacheDir, fileName)
                shareFile.writeText(content)
                
                val shareUri = androidx.core.content.FileProvider.getUriForFile(
                    this@MainActivity,
                    "$packageName.fileprovider",
                    shareFile
                )
                
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/xml"
                    putExtra(Intent.EXTRA_STREAM, shareUri)
                    putExtra(Intent.EXTRA_SUBJECT, "HSR Graphics Settings")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_xml)))
            } else {
                Snackbar.make(binding.root, getString(R.string.share_failed), Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun launchGame() {
        val pkg = gameManager.installedGamePackage
        if (pkg == null) {
            Snackbar.make(binding.root, getString(R.string.game_not_found), Snackbar.LENGTH_SHORT).show()
            return
        }

        val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
        if (launchIntent != null) {
            startActivity(launchIntent)
        } else {
            Snackbar.make(binding.root, getString(R.string.game_not_found), Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun openGameAppInfo() {
        val pkg = gameManager.installedGamePackage
        if (pkg == null) {
            Snackbar.make(binding.root, getString(R.string.game_not_found), Snackbar.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$pkg")
        }
        startActivity(intent)
    }
    
    private fun setupGraphicsEditor() {
        // Presets
        binding.btnPresetLow.setOnClickListener { applyPreset(0) }
        binding.btnPresetMedium.setOnClickListener { applyPreset(2) }
        binding.btnPresetHigh.setOnClickListener { applyPreset(3) }
        binding.btnPresetUltra.setOnClickListener { applyPreset(5) }
        
        // Overall Graphics Quality Slider
        binding.sliderGraphicsQuality.addOnChangeListener { _, value, _ ->
            currentSettings?.let {
                binding.tvGraphicsQualityValue.text = it.getQualityName(value.toInt())
            }
        }
        
        // FPS Slider
        binding.sliderFps.addOnChangeListener { _, value, _ ->
            binding.tvFpsValue.text = value.toInt().toString()
        }
        
        // Render Scale Slider
        binding.sliderRenderScale.addOnChangeListener { _, value, _ ->
            binding.tvRenderScaleValue.text = String.format("%.1fx", value)
        }
        
        // Quality Sliders
        binding.sliderResolution.addOnChangeListener { _, value, _ ->
            currentSettings?.let {
                binding.tvResolutionValue.text = it.getQualityName(value.toInt())
            }
        }
        
        binding.sliderShadow.addOnChangeListener { _, value, _ ->
            currentSettings?.let {
                binding.tvShadowValue.text = it.getQualityName(value.toInt())
            }
        }
        
        binding.sliderLight.addOnChangeListener { _, value, _ ->
            currentSettings?.let {
                binding.tvLightValue.text = it.getQualityName(value.toInt())
            }
        }
        
        binding.sliderCharacter.addOnChangeListener { _, value, _ ->
            currentSettings?.let {
                binding.tvCharacterValue.text = it.getQualityName(value.toInt())
            }
        }
        
        binding.sliderEnvironment.addOnChangeListener { _, value, _ ->
            currentSettings?.let {
                binding.tvEnvironmentValue.text = it.getQualityName(value.toInt())
            }
        }
        
        binding.sliderReflection.addOnChangeListener { _, value, _ ->
            currentSettings?.let {
                binding.tvReflectionValue.text = it.getQualityName(value.toInt())
            }
        }
        
        binding.sliderSfx.addOnChangeListener { _, value, _ ->
            currentSettings?.let {
                binding.tvSfxValue.text = it.getQualityName(value.toInt())
            }
        }
        
        binding.sliderBloom.addOnChangeListener { _, value, _ ->
            currentSettings?.let {
                binding.tvBloomValue.text = it.getQualityName(value.toInt())
            }
        }
        
        // AA Mode Slider (Off/TAA/SMAA)
        binding.sliderAa.addOnChangeListener { _, value, _ ->
            currentSettings?.let {
                binding.tvAaValue.text = it.getAAModeName(value.toInt())
            }
        }
        
        // Self Shadow Slider
        binding.sliderSelfShadow.addOnChangeListener { _, value, _ ->
            currentSettings?.let {
                binding.tvSelfShadowValue.text = it.getSelfShadowName(value.toInt())
            }
        }
        
        // DLSS Quality Slider
        binding.sliderDlss.addOnChangeListener { _, value, _ ->
            currentSettings?.let {
                binding.tvDlssValue.text = it.getDlssName(value.toInt())
            }
        }
        
        // Particle Trail Smoothness Slider
        binding.sliderParticleTrail.addOnChangeListener { _, value, _ ->
            currentSettings?.let {
                binding.tvParticleTrailValue.text = it.getParticleTrailName(value.toInt())
            }
        }
        
        // Resolution Presets
        binding.chip360p.setOnClickListener {
            setResolutionPreset("360p")
        }
        binding.chip720p.setOnClickListener {
            setResolutionPreset("720p")
        }
        binding.chip1080p.setOnClickListener {
            setResolutionPreset("1080p")
        }
        binding.chip1440p.setOnClickListener {
            setResolutionPreset("1440p")
        }
        binding.chip4k.setOnClickListener {
            setResolutionPreset("4K")
        }
        binding.chip8k.setOnClickListener {
            setResolutionPreset("8K")
        }
        
        // Custom Resolution Input
        binding.etResolutionWidth.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                updateResolutionFromInput()
            }
        }
        binding.etResolutionHeight.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                updateResolutionFromInput()
            }
        }
        
        // Fullscreen Mode
        binding.chipFullscreenWindow.setOnClickListener {
            currentSettings?.fullscreenMode = 0
        }
        binding.chipExclusiveFullscreen.setOnClickListener {
            currentSettings?.fullscreenMode = 1
        }
        binding.chipMaximizedWindow.setOnClickListener {
            currentSettings?.fullscreenMode = 2
        }
        binding.chipWindowed.setOnClickListener {
            currentSettings?.fullscreenMode = 3
        }
        
        // Speed Up Open (default ON)
        binding.switchSpeedUpOpen.setOnCheckedChangeListener { _, isChecked ->
            currentSettings?.speedUpOpen = if (isChecked) 1 else 0
        }
        
        // Apply Button
        binding.btnApply.setOnClickListener {
            applySettings()
        }
        
        // Save Backup Button
        binding.btnSaveBackup.setOnClickListener {
            showSaveBackupDialog()
        }
    }
    
    private fun loadCurrentSettings() {
        lifecycleScope.launch {
            binding.progressIndicator.show()
            
            val settings = withContext(Dispatchers.IO) {
                gameManager.readCurrentSettings()
            }
            
            binding.progressIndicator.hide()
            
            if (settings != null) {
                currentSettings = settings
                updateUIWithSettings(settings)
            } else {
                // Use default settings if can't read from game
                currentSettings = com.ireddragonicy.hsrgraphicdroid.data.GraphicsSettings()
                updateUIWithSettings(currentSettings!!)
            }
        }
    }
    
    private fun updateUIWithSettings(settings: com.ireddragonicy.hsrgraphicdroid.data.GraphicsSettings) {
        // Overall Graphics Quality
        binding.sliderGraphicsQuality.value = settings.graphicsQuality.toFloat()
        binding.tvGraphicsQualityValue.text = settings.getQualityName(settings.graphicsQuality)
        
        binding.sliderFps.value = settings.fps.toFloat()
        binding.tvFpsValue.text = settings.fps.toString()
        
        binding.switchVsync.isChecked = settings.enableVSync
        
        binding.sliderRenderScale.value = settings.renderScale.toFloat()
        binding.tvRenderScaleValue.text = String.format("%.1fx", settings.renderScale)
        
        binding.sliderResolution.value = settings.resolutionQuality.toFloat()
        binding.tvResolutionValue.text = settings.getQualityName(settings.resolutionQuality)
        
        binding.sliderShadow.value = settings.shadowQuality.toFloat()
        binding.tvShadowValue.text = settings.getQualityName(settings.shadowQuality)
        
        binding.sliderLight.value = settings.lightQuality.toFloat()
        binding.tvLightValue.text = settings.getQualityName(settings.lightQuality)
        
        binding.sliderCharacter.value = settings.characterQuality.toFloat()
        binding.tvCharacterValue.text = settings.getQualityName(settings.characterQuality)
        
        binding.sliderEnvironment.value = settings.envDetailQuality.toFloat()
        binding.tvEnvironmentValue.text = settings.getQualityName(settings.envDetailQuality)
        
        binding.sliderReflection.value = settings.reflectionQuality.toFloat()
        binding.tvReflectionValue.text = settings.getQualityName(settings.reflectionQuality)
        
        binding.sliderSfx.value = settings.sfxQuality.toFloat()
        binding.tvSfxValue.text = settings.getQualityName(settings.sfxQuality)
        
        binding.sliderBloom.value = settings.bloomQuality.toFloat()
        binding.tvBloomValue.text = settings.getQualityName(settings.bloomQuality)
        
        // AA Mode (Off/TAA/SMAA)
        binding.sliderAa.value = settings.aaMode.toFloat()
        binding.tvAaValue.text = settings.getAAModeName(settings.aaMode)
        
        // Self Shadow
        binding.sliderSelfShadow.value = settings.enableSelfShadow.toFloat()
        binding.tvSelfShadowValue.text = settings.getSelfShadowName(settings.enableSelfShadow)
        
        // MetalFX and Half Res Transparent
        binding.switchMetalFx.isChecked = settings.enableMetalFXSU
        binding.switchHalfResTransparent.isChecked = settings.enableHalfResTransparent
        
        // DLSS Quality
        binding.sliderDlss.value = settings.dlssQuality.toFloat()
        binding.tvDlssValue.text = settings.getDlssName(settings.dlssQuality)
        
        // Particle Trail Smoothness
        binding.sliderParticleTrail.value = settings.particleTrailSmoothness.toFloat()
        binding.tvParticleTrailValue.text = settings.getParticleTrailName(settings.particleTrailSmoothness)
        
        // Screen Resolution
        binding.etResolutionWidth.setText(settings.screenWidth.toString())
        binding.etResolutionHeight.setText(settings.screenHeight.toString())
        updateResolutionDisplay()
        updateResolutionChips()
        
        // Fullscreen Mode
        updateFullscreenModeChips(settings.fullscreenMode)
        
        // Speed Up Open
        binding.switchSpeedUpOpen.isChecked = settings.speedUpOpen == 1
    }
    
    private fun updateFullscreenModeChips(mode: Int) {
        binding.chipGroupFullscreen.clearCheck()
        when (mode) {
            0 -> binding.chipFullscreenWindow.isChecked = true
            1 -> binding.chipExclusiveFullscreen.isChecked = true
            2 -> binding.chipMaximizedWindow.isChecked = true
            3 -> binding.chipWindowed.isChecked = true
            else -> binding.chipExclusiveFullscreen.isChecked = true // default
        }
    }
    
    private fun setResolutionPreset(preset: String) {
        currentSettings?.setResolutionPreset(preset)
        currentSettings?.let {
            binding.etResolutionWidth.setText(it.screenWidth.toString())
            binding.etResolutionHeight.setText(it.screenHeight.toString())
            updateResolutionDisplay()
        }
    }
    
    private fun updateResolutionFromInput() {
        val width = binding.etResolutionWidth.text.toString().toIntOrNull() ?: 1920
        val height = binding.etResolutionHeight.text.toString().toIntOrNull() ?: 1080
        
        currentSettings?.screenWidth = width
        currentSettings?.screenHeight = height
        updateResolutionDisplay()
        updateResolutionChips()
    }
    
    private fun updateResolutionDisplay() {
        currentSettings?.let {
            binding.tvCurrentResolution.text = "Current: ${it.screenWidth}×${it.screenHeight}"
        }
    }
    
    private fun updateResolutionChips() {
        currentSettings?.let { settings ->
            // Uncheck all chips first
            binding.chipGroupResolution.clearCheck()
            
            // Check the appropriate chip based on current resolution
            when {
                settings.screenWidth == 640 && settings.screenHeight == 360 -> 
                    binding.chip360p.isChecked = true
                settings.screenWidth == 1280 && settings.screenHeight == 720 -> 
                    binding.chip720p.isChecked = true
                settings.screenWidth == 1920 && settings.screenHeight == 1080 -> 
                    binding.chip1080p.isChecked = true
                settings.screenWidth == 2560 && settings.screenHeight == 1440 -> 
                    binding.chip1440p.isChecked = true
                settings.screenWidth == 3840 && settings.screenHeight == 2160 -> 
                    binding.chip4k.isChecked = true
                settings.screenWidth == 7680 && settings.screenHeight == 4320 -> 
                    binding.chip8k.isChecked = true
                // else: custom resolution, no chip checked
            }
        }
    }
    
    private fun applyPreset(quality: Int) {
        currentSettings?.let { settings ->
            // Set overall graphics quality to match preset
            settings.graphicsQuality = quality
            settings.resolutionQuality = quality
            settings.shadowQuality = quality
            settings.lightQuality = quality
            settings.characterQuality = quality
            settings.envDetailQuality = quality
            settings.reflectionQuality = quality
            settings.sfxQuality = quality
            settings.bloomQuality = quality
            
            when (quality) {
                0 -> { // Low
                    settings.fps = 30
                    settings.renderScale = 0.7
                    settings.enableVSync = false
                    settings.aaMode = 0
                    settings.enableSelfShadow = 0
                    settings.enableMetalFXSU = false
                    settings.enableHalfResTransparent = true
                    settings.dlssQuality = 3 // Performance
                }
                2 -> { // Medium
                    settings.fps = 60
                    settings.renderScale = 0.9
                    settings.enableVSync = true
                    settings.aaMode = 1
                    settings.enableSelfShadow = 1
                    settings.enableMetalFXSU = false
                    settings.enableHalfResTransparent = false
                    settings.dlssQuality = 2 // Balanced
                }
                3 -> { // High
                    settings.fps = 60
                    settings.renderScale = 1.0
                    settings.enableVSync = true
                    settings.aaMode = 1
                    settings.enableSelfShadow = 1
                    settings.enableMetalFXSU = false
                    settings.enableHalfResTransparent = false
                    settings.dlssQuality = 1 // Quality
                }
                5 -> { // Ultra - MAKSIMAL!
                    settings.fps = 120
                    settings.renderScale = 1.4
                    settings.enableVSync = false
                    settings.aaMode = 1
                    settings.enableSelfShadow = 2
                    settings.enableMetalFXSU = true
                    settings.enableHalfResTransparent = false
                    settings.dlssQuality = 1 // Quality
                }
            }
            
            updateUIWithSettings(settings)
            Snackbar.make(binding.root, "Preset applied: ${settings.getQualityName(quality)}", Snackbar.LENGTH_SHORT).show()
        }
    }
    
    private fun getCurrentSettingsFromUI(): com.ireddragonicy.hsrgraphicdroid.data.GraphicsSettings {
        return com.ireddragonicy.hsrgraphicdroid.data.GraphicsSettings(
            graphicsQuality = binding.sliderGraphicsQuality.value.toInt(),
            fps = binding.sliderFps.value.toInt(),
            enableVSync = binding.switchVsync.isChecked,
            renderScale = binding.sliderRenderScale.value.toDouble(),
            resolutionQuality = binding.sliderResolution.value.toInt(),
            shadowQuality = binding.sliderShadow.value.toInt(),
            lightQuality = binding.sliderLight.value.toInt(),
            characterQuality = binding.sliderCharacter.value.toInt(),
            envDetailQuality = binding.sliderEnvironment.value.toInt(),
            reflectionQuality = binding.sliderReflection.value.toInt(),
            sfxQuality = binding.sliderSfx.value.toInt(),
            bloomQuality = binding.sliderBloom.value.toInt(),
            aaMode = binding.sliderAa.value.toInt(),
            enableSelfShadow = binding.sliderSelfShadow.value.toInt(),
            enableMetalFXSU = binding.switchMetalFx.isChecked,
            enableHalfResTransparent = binding.switchHalfResTransparent.isChecked,
            dlssQuality = binding.sliderDlss.value.toInt(),
            particleTrailSmoothness = binding.sliderParticleTrail.value.toInt(),
            screenWidth = binding.etResolutionWidth.text.toString().toIntOrNull() ?: 1920,
            screenHeight = binding.etResolutionHeight.text.toString().toIntOrNull() ?: 1080,
            fullscreenMode = when {
                binding.chipFullscreenWindow.isChecked -> 0
                binding.chipExclusiveFullscreen.isChecked -> 1
                binding.chipMaximizedWindow.isChecked -> 2
                binding.chipWindowed.isChecked -> 3
                else -> 1
            },
            speedUpOpen = if (binding.switchSpeedUpOpen.isChecked) 1 else 0
        )
    }
    
    private fun applySettings() {
        val settings = getCurrentSettingsFromUI()
        
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.apply)
            .setMessage(getString(R.string.apply_settings_message))
            .setPositiveButton(R.string.apply) { _, _ ->
                performApplySettings(settings)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun performApplySettings(settings: com.ireddragonicy.hsrgraphicdroid.data.GraphicsSettings) {
        lifecycleScope.launch {
            binding.progressIndicator.show()
            
            val success = withContext(Dispatchers.IO) {
                gameManager.writeSettings(settings)
            }
            
            binding.progressIndicator.hide()
            
            if (success) {
                currentSettings = settings
                Snackbar.make(binding.root, getString(R.string.settings_applied), Snackbar.LENGTH_LONG)
                    .setAction(R.string.kill_game) {
                        killGame()
                    }
                    .show()
            } else {
                Snackbar.make(binding.root, getString(R.string.apply_failed), Snackbar.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showSaveBackupDialog() {
        val input = android.widget.EditText(this).apply {
            hint = getString(R.string.backup_name_hint)
        }
        
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.save_as_backup)
            .setView(input)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = input.text.toString().ifEmpty { "Backup ${System.currentTimeMillis()}" }
                saveCurrentSettingsAsBackup(name)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun saveCurrentSettingsAsBackup(name: String) {
        val settings = getCurrentSettingsFromUI()
        
        lifecycleScope.launch {
            binding.progressIndicator.show()
            
            val success = withContext(Dispatchers.IO) {
                gameManager.saveBackup(name, settings)
            }
            
            binding.progressIndicator.hide()
            
            if (success) {
                Snackbar.make(binding.root, getString(R.string.backup_created), Snackbar.LENGTH_SHORT).show()
                loadBackups()
            } else {
                Snackbar.make(binding.root, getString(R.string.backup_failed), Snackbar.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        loadBackups()
        loadCurrentSettings()
    }
    
    private fun checkStatus() {
        lifecycleScope.launch {
            val rootStatus = withContext(Dispatchers.IO) {
                gameManager.isRootAvailable
            }
            
            val gameInstalled = withContext(Dispatchers.IO) {
                gameManager.isGameInstalled
            }
            
            val gameVersion = withContext(Dispatchers.IO) {
                if (gameInstalled) gameManager.gameVersionName else null
            }
            
            binding.chipRootStatus.apply {
                text = if (rootStatus) getString(R.string.root_granted) else getString(R.string.root_not_granted)
                setChipIconResource(if (rootStatus) R.drawable.ic_check else R.drawable.ic_close)
            }
            
            binding.chipGameStatus.apply {
                text = if (gameInstalled) {
                    getString(R.string.game_found) + " ($gameVersion)"
                } else {
                    getString(R.string.game_not_found)
                }
                setChipIconResource(if (gameInstalled) R.drawable.ic_check else R.drawable.ic_close)
            }

            // Show/hide game action buttons based on game installation status
            binding.layoutGameActions.visibility = if (gameInstalled) View.VISIBLE else View.GONE
            binding.layoutExportShare.visibility = if (gameInstalled && rootStatus) View.VISIBLE else View.GONE
            
            if (!rootStatus) {
                requestRootAccess()
            }
        }
    }
    
    private fun requestRootAccess() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.root_required)
            .setMessage(R.string.root_required_message)
            .setPositiveButton(R.string.grant) { _, _ ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        Shell.getShell()
                    }
                    checkStatus()
                }
            }
            .setNegativeButton(R.string.exit) { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun readCurrentSettings() {
        lifecycleScope.launch {
            binding.progressIndicator.show()
            
            val settings = withContext(Dispatchers.IO) {
                gameManager.readCurrentSettings()
            }
            
            binding.progressIndicator.hide()
            
            if (settings != null) {
                showSettingsInfo(settings)
            } else {
                Snackbar.make(binding.root, getString(R.string.error_read_settings), Snackbar.LENGTH_LONG).show()
            }
        }
    }
    
    private fun showSettingsInfo(settings: com.ireddragonicy.hsrgraphicdroid.data.GraphicsSettings) {
        val info = """
            Graphics Quality: ${settings.getQualityName(settings.graphicsQuality)}
            FPS: ${settings.fps}
            VSync: ${if (settings.enableVSync) "ON" else "OFF"}
            Render Scale: ${settings.renderScale}x
            Resolution: ${settings.getQualityName(settings.resolutionQuality)}
            Screen Resolution: ${settings.screenWidth}×${settings.screenHeight}
            Fullscreen Mode: ${settings.getFullscreenModeName()}
            Speed Up Open: ${if (settings.speedUpOpen == 1) "ON" else "OFF"}
            Shadows: ${settings.getQualityName(settings.shadowQuality)}
            Lighting: ${settings.getQualityName(settings.lightQuality)}
            Characters: ${settings.getQualityName(settings.characterQuality)}
            Environment: ${settings.getQualityName(settings.envDetailQuality)}
            Reflections: ${settings.getQualityName(settings.reflectionQuality)}
            SFX: ${settings.getQualityName(settings.sfxQuality)}
            Bloom: ${settings.getQualityName(settings.bloomQuality)}
            Anti-Aliasing: ${if (settings.aaMode == 1) "ON" else "OFF"}
        """.trimIndent()
        
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.current_settings)
            .setMessage(info)
            .setPositiveButton(R.string.ok, null)
            .show()
    }
    
    private fun showBackupDialog() {
        val input = android.widget.EditText(this).apply {
            hint = getString(R.string.backup_name_hint)
        }
        
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.create_backup)
            .setView(input)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = input.text.toString().ifEmpty { "Backup ${System.currentTimeMillis()}" }
                createBackup(name)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun createBackup(name: String) {
        lifecycleScope.launch {
            binding.progressIndicator.show()
            
            val settings = withContext(Dispatchers.IO) {
                gameManager.readCurrentSettings()
            }
            
            if (settings != null) {
                val success = withContext(Dispatchers.IO) {
                    gameManager.saveBackup(name, settings)
                }
                
                if (success) {
                    Snackbar.make(binding.root, getString(R.string.backup_created), Snackbar.LENGTH_SHORT).show()
                    loadBackups()
                } else {
                    Snackbar.make(binding.root, getString(R.string.backup_failed), Snackbar.LENGTH_SHORT).show()
                }
            } else {
                Snackbar.make(binding.root, getString(R.string.error_read_settings), Snackbar.LENGTH_SHORT).show()
            }
            
            binding.progressIndicator.hide()
        }
    }
    
    private fun loadBackups() {
        lifecycleScope.launch {
            val backups = withContext(Dispatchers.IO) {
                gameManager.loadBackups()
            }
            
            backupAdapter.submitList(backups.reversed())
            binding.tvBackupCount.text = getString(R.string.backup_count, backups.size)
        }
    }
    
    private fun restoreBackup(backup: com.ireddragonicy.hsrgraphicdroid.data.BackupData) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.restore_backup)
            .setMessage(getString(R.string.restore_backup_message, backup.name))
            .setPositiveButton(R.string.restore) { _, _ ->
                performRestore(backup)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun performRestore(backup: com.ireddragonicy.hsrgraphicdroid.data.BackupData) {
        lifecycleScope.launch {
            binding.progressIndicator.show()
            
            val success = withContext(Dispatchers.IO) {
                gameManager.writeSettings(backup.settings)
            }
            
            binding.progressIndicator.hide()
            
            if (success) {
                // Update current settings and UI
                currentSettings = backup.settings
                updateUIWithSettings(backup.settings)
                
                Snackbar.make(binding.root, getString(R.string.restore_success), Snackbar.LENGTH_LONG)
                    .setAction(R.string.kill_game) {
                        killGame()
                    }
                    .show()
            } else {
                Snackbar.make(binding.root, getString(R.string.restore_failed), Snackbar.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun confirmDelete(backup: com.ireddragonicy.hsrgraphicdroid.data.BackupData) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_backup)
            .setMessage(getString(R.string.delete_backup_message, backup.name))
            .setPositiveButton(R.string.delete) { _, _ ->
                deleteBackup(backup)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun deleteBackup(backup: com.ireddragonicy.hsrgraphicdroid.data.BackupData) {
        lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) {
                gameManager.deleteBackup(backup)
            }
            
            if (success) {
                loadBackups()
                Snackbar.make(binding.root, getString(R.string.backup_deleted), Snackbar.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun killGame() {
        lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) {
                gameManager.killGame()
            }
            
            if (success) {
                Toast.makeText(this@MainActivity, R.string.game_killed, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_game_prefs -> {
                startActivity(Intent(this, GamePreferencesActivity::class.java))
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_about -> {
                showAboutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showAboutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.about)
            .setMessage(R.string.about_message)
            .setPositiveButton(R.string.ok, null)
            .show()
    }
}
