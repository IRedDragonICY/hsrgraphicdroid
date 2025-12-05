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
    private var isApplyingPreset = false  // Flag to prevent clearing preset during programmatic changes

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
                Snackbar.make(binding.root, getString(R.string.export_failed), Snackbar.LENGTH_SHORT)
                    .setAnchorView(binding.bottomActionBar)
                    .show()
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
                    .setAnchorView(binding.bottomActionBar)
                    .setAction("Open") {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "text/xml")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        runCatching { startActivity(intent) }
                    }
                    .show()
            } else {
                Snackbar.make(binding.root, getString(R.string.export_failed), Snackbar.LENGTH_SHORT)
                    .setAnchorView(binding.bottomActionBar)
                    .show()
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
                Snackbar.make(binding.root, getString(R.string.share_failed), Snackbar.LENGTH_SHORT)
                    .setAnchorView(binding.bottomActionBar)
                    .show()
            }
        }
    }

    private fun launchGame() {
        val pkg = gameManager.installedGamePackage
        if (pkg == null) {
            Snackbar.make(binding.root, getString(R.string.game_not_found), Snackbar.LENGTH_SHORT)
                .setAnchorView(binding.bottomActionBar)
                .show()
            return
        }

        val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
        if (launchIntent != null) {
            startActivity(launchIntent)
        } else {
            Snackbar.make(binding.root, getString(R.string.game_not_found), Snackbar.LENGTH_SHORT)
                .setAnchorView(binding.bottomActionBar)
                .show()
        }
    }

    private fun openGameAppInfo() {
        val pkg = gameManager.installedGamePackage
        if (pkg == null) {
            Snackbar.make(binding.root, getString(R.string.game_not_found), Snackbar.LENGTH_SHORT)
                .setAnchorView(binding.bottomActionBar)
                .show()
            return
        }

        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$pkg")
        }
        startActivity(intent)
    }
    
    private fun setupGraphicsEditor() {
        // Extended Presets - These fill sliders with values that can EXCEED game limits
        // Automatically sets GraphicsQuality to 0 (Custom) so game uses our individual settings
        binding.btnPresetLow.setOnClickListener { applyExtendedPreset(0) }     // Low
        binding.btnPresetMedium.setOnClickListener { applyExtendedPreset(1) }  // Medium
        binding.btnPresetHigh.setOnClickListener { applyExtendedPreset(2) }    // High
        binding.btnPresetUltra.setOnClickListener { applyExtendedPreset(3) }   // Ultra
        binding.btnPresetMax.setOnClickListener { applyExtendedPreset(4) }     // MAX
        
        // Setup all sliders with unified change listener
        setupSliderListeners()
        
        // Setup all switches with unified change listener  
        setupSwitchListeners()
        
        // Resolution and Fullscreen Mode are VIEW ONLY - no editing
        // The game uses device screen resolution, we just display it
        
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
    
    private fun setupSliderListeners() {
        // Map of slider to (textView, formatter)
        // Note: sliderGraphicsQuality is handled separately as it's the master quality
        val sliderConfigs: List<Triple<com.google.android.material.slider.Slider, android.widget.TextView, (Float) -> String>> = listOf(
            Triple(binding.sliderFps, binding.tvFpsValue) { v -> v.toInt().toString() },
            Triple(binding.sliderRenderScale, binding.tvRenderScaleValue) { v -> String.format("%.1fx", v) },
            Triple(binding.sliderResolution, binding.tvResolutionValue) { v -> currentSettings?.getQualityName(v.toInt()) ?: "" },
            Triple(binding.sliderShadow, binding.tvShadowValue) { v -> currentSettings?.getQualityName(v.toInt()) ?: "" },
            Triple(binding.sliderLight, binding.tvLightValue) { v -> currentSettings?.getQualityName(v.toInt()) ?: "" },
            Triple(binding.sliderCharacter, binding.tvCharacterValue) { v -> currentSettings?.getQualityName(v.toInt()) ?: "" },
            Triple(binding.sliderEnvironment, binding.tvEnvironmentValue) { v -> currentSettings?.getQualityName(v.toInt()) ?: "" },
            Triple(binding.sliderReflection, binding.tvReflectionValue) { v -> currentSettings?.getQualityName(v.toInt()) ?: "" },
            Triple(binding.sliderSfx, binding.tvSfxValue) { v -> currentSettings?.getSfxQualityName(v.toInt()) ?: "" },
            Triple(binding.sliderBloom, binding.tvBloomValue) { v -> currentSettings?.getQualityName(v.toInt()) ?: "" },
            Triple(binding.sliderAa, binding.tvAaValue) { v -> currentSettings?.getAAModeName(v.toInt()) ?: "" },
            Triple(binding.sliderSelfShadow, binding.tvSelfShadowValue) { v -> currentSettings?.getSelfShadowName(v.toInt()) ?: "" },
            Triple(binding.sliderDlss, binding.tvDlssValue) { v -> currentSettings?.getDlssName(v.toInt()) ?: "" },
            Triple(binding.sliderParticleTrail, binding.tvParticleTrailValue) { v -> currentSettings?.getParticleTrailName(v.toInt()) ?: "" }
        )
        
        // Individual settings sliders - when changed, set to Custom mode (0)
        sliderConfigs.forEach { (slider, textView, formatter) ->
            slider.addOnChangeListener { _, value, fromUser ->
                textView.text = formatter(value)
                if (fromUser) setCustomMode()
            }
        }
        
        // Master graphics quality slider - this sets the game preset level
        binding.sliderGraphicsQuality.addOnChangeListener { _, value, fromUser ->
            val quality = value.toInt()
            binding.tvGraphicsQualityValue.text = currentSettings?.getMasterQualityName(quality) ?: "Custom"
            currentSettings?.graphicsQuality = quality
            if (fromUser && quality == 0) {
                binding.toggleGroupPresets.clearChecked()
            }
        }
    }
    
    private fun setupSwitchListeners() {
        listOf(binding.switchVsync, binding.switchMetalFx, binding.switchHalfResTransparent).forEach { switch ->
            switch.setOnCheckedChangeListener { _, _ ->
                setCustomMode()
            }
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
                Snackbar.make(binding.root, getString(R.string.settings_loaded), Snackbar.LENGTH_SHORT)
                    .setAnchorView(binding.bottomActionBar)
                    .show()
            } else {
                // Use default settings if can't read from game
                currentSettings = com.ireddragonicy.hsrgraphicdroid.data.GraphicsSettings()
                updateUIWithSettings(currentSettings!!)
                Snackbar.make(binding.root, getString(R.string.error_read_settings_default), Snackbar.LENGTH_SHORT)
                    .setAnchorView(binding.bottomActionBar)
                    .show()
            }
        }
    }
    
    private fun updateUIWithSettings(settings: com.ireddragonicy.hsrgraphicdroid.data.GraphicsSettings) {
        // Overall Graphics Quality (Master quality - 0=Custom, 1-5=Game presets)
        binding.sliderGraphicsQuality.value = settings.graphicsQuality.toFloat()
        binding.tvGraphicsQualityValue.text = settings.getMasterQualityName(settings.graphicsQuality)
        
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
        binding.tvSfxValue.text = settings.getSfxQualityName(settings.sfxQuality)
        
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
        
        // Screen Resolution (View Only)
        binding.tvCurrentResolution.text = "${settings.screenWidth}×${settings.screenHeight}"
        
        // Fullscreen Mode (View Only)
        binding.tvFullscreenMode.text = settings.getFullscreenModeName()
        
        // Speed Up Open
        binding.switchSpeedUpOpen.isChecked = settings.speedUpOpen == 1
    }
    
    // Resolution and Fullscreen Mode are VIEW ONLY - removed editing functions
    
    /**
     * Apply game preset (1-5) - This sets GraphicsSettings_GraphicsQuality
     * which makes the game use its built-in default values for that quality level.
     * 1=Very Low, 2=Low, 3=Medium, 4=High, 5=Very High
     * Use the slider at the top to set this value.
     */
    private fun applyGamePreset(quality: Int) {
        currentSettings?.let { settings ->
            isApplyingPreset = true
            
            // Set the master graphics quality - game will use its defaults
            settings.graphicsQuality = quality
            
            // Update UI slider to show the selected preset
            binding.sliderGraphicsQuality.value = quality.toFloat()
            binding.tvGraphicsQualityValue.text = settings.getMasterQualityName(quality)
            
            isApplyingPreset = false
        }
    }
    
    /**
     * Apply extended preset (0-4) - These are custom presets that can EXCEED game limits
     * Automatically sets GraphicsQuality to 0 (Custom) so game uses individual settings.
     * 0=Low, 1=Medium, 2=High, 3=Ultra, 4=MAX
     */
    private fun applyExtendedPreset(level: Int) {
        currentSettings?.let { settings ->
            isApplyingPreset = true
            
            // IMPORTANT: Set to Custom (0) so game uses our individual settings instead of its presets
            settings.graphicsQuality = 0
            binding.sliderGraphicsQuality.value = 0f
            binding.tvGraphicsQualityValue.text = settings.getMasterQualityName(0)
            
            // Extended presets that can exceed game's normal limits
            when (level) {
                0 -> { // Low - Lowest settings for weak devices
                    binding.sliderFps.value = 30f
                    binding.switchVsync.isChecked = true
                    binding.sliderRenderScale.value = 0.6f
                    binding.sliderResolution.value = 0f    // Very Low
                    binding.sliderShadow.value = 0f        // Off
                    binding.sliderLight.value = 0f         // Very Low
                    binding.sliderCharacter.value = 0f     // Very Low
                    binding.sliderEnvironment.value = 0f   // Very Low
                    binding.sliderReflection.value = 0f    // Very Low
                    binding.sliderSfx.value = 1f           // Very Low (minimum is 1)
                    binding.sliderBloom.value = 0f         // Off
                    binding.sliderAa.value = 0f            // Off
                    binding.sliderSelfShadow.value = 0f    // Off
                    binding.sliderDlss.value = 0f          // Off
                    binding.sliderParticleTrail.value = 0f // Off
                    binding.switchMetalFx.isChecked = false
                    binding.switchHalfResTransparent.isChecked = false
                }
                1 -> { // Medium - Balanced quality
                    binding.sliderFps.value = 60f
                    binding.switchVsync.isChecked = true
                    binding.sliderRenderScale.value = 0.8f
                    binding.sliderResolution.value = 1f    // Low
                    binding.sliderShadow.value = 1f        // Low
                    binding.sliderLight.value = 1f         // Low
                    binding.sliderCharacter.value = 1f     // Low
                    binding.sliderEnvironment.value = 1f   // Low
                    binding.sliderReflection.value = 1f    // Low
                    binding.sliderSfx.value = 2f           // Low (1=VeryLow, 2=Low)
                    binding.sliderBloom.value = 1f         // Low
                    binding.sliderAa.value = 1f            // TAA
                    binding.sliderSelfShadow.value = 0f    // Off
                    binding.sliderDlss.value = 0f          // Off
                    binding.sliderParticleTrail.value = 1f // Low
                }
                2 -> { // High - Good quality
                    binding.sliderFps.value = 60f
                    binding.switchVsync.isChecked = true
                    binding.sliderRenderScale.value = 1.0f
                    binding.sliderResolution.value = 2f    // Medium
                    binding.sliderShadow.value = 2f        // Medium
                    binding.sliderLight.value = 2f         // Medium
                    binding.sliderCharacter.value = 2f     // Medium
                    binding.sliderEnvironment.value = 2f   // Medium
                    binding.sliderReflection.value = 2f    // Medium
                    binding.sliderSfx.value = 3f           // Medium (1=VL, 2=L, 3=M)
                    binding.sliderBloom.value = 2f         // Medium
                    binding.sliderAa.value = 1f            // TAA
                    binding.sliderSelfShadow.value = 1f    // Low
                    binding.sliderDlss.value = 1f          // Quality
                    binding.sliderParticleTrail.value = 2f // Medium
                }
                3 -> { // Ultra - High-Very High settings
                    binding.sliderFps.value = 120f
                    binding.switchVsync.isChecked = false
                    binding.sliderRenderScale.value = 1.2f
                    binding.sliderResolution.value = 3f    // High
                    binding.sliderShadow.value = 3f        // High
                    binding.sliderLight.value = 3f         // High
                    binding.sliderCharacter.value = 3f     // High
                    binding.sliderEnvironment.value = 3f   // High
                    binding.sliderReflection.value = 3f    // High
                    binding.sliderSfx.value = 4f           // High (1=VL, 2=L, 3=M, 4=H)
                    binding.sliderBloom.value = 3f         // High
                    binding.sliderAa.value = 2f            // SMAA
                    binding.sliderSelfShadow.value = 2f    // High (max)
                    binding.sliderDlss.value = 1f          // Quality
                    binding.sliderParticleTrail.value = 3f // High (max)
                }
                4 -> { // MAX - Beyond game limits! 🔥
                    binding.sliderFps.value = 120f
                    binding.switchVsync.isChecked = false
                    binding.sliderRenderScale.value = 2.0f  // BEYOND LIMIT!
                    binding.sliderResolution.value = 5f     // Ultra (max slider)
                    binding.sliderShadow.value = 5f
                    binding.sliderLight.value = 5f
                    binding.sliderCharacter.value = 5f
                    binding.sliderEnvironment.value = 5f
                    binding.sliderReflection.value = 5f
                    binding.sliderSfx.value = 5f
                    binding.sliderBloom.value = 5f
                    binding.sliderAa.value = 2f             // SMAA (max)
                    binding.sliderSelfShadow.value = 2f     // High (max)
                    binding.switchMetalFx.isChecked = true
                    binding.sliderDlss.value = 1f           // Quality (best visual, not Ultra Performance!)
                    binding.sliderParticleTrail.value = 3f  // High (max)
                }
            }
            
            // Update all text displays after changing sliders
            updateSliderDisplays()
            
            isApplyingPreset = false
            
            val presetNames = arrayOf("Low", "Medium", "High", "Ultra", "MAX")
            Snackbar.make(
                binding.root, 
                "Extended preset: ${presetNames[level]} applied (Custom mode)",
                Snackbar.LENGTH_SHORT
            )
                .setAnchorView(binding.bottomActionBar)
                .show()
        }
    }
    
    /**
     * Update all slider display texts based on current values
     */
    private fun updateSliderDisplays() {
        currentSettings?.let { settings ->
            binding.tvFpsValue.text = binding.sliderFps.value.toInt().toString()
            binding.tvRenderScaleValue.text = String.format("%.1fx", binding.sliderRenderScale.value)
            binding.tvResolutionValue.text = settings.getQualityName(binding.sliderResolution.value.toInt())
            binding.tvShadowValue.text = settings.getQualityName(binding.sliderShadow.value.toInt())
            binding.tvLightValue.text = settings.getQualityName(binding.sliderLight.value.toInt())
            binding.tvCharacterValue.text = settings.getQualityName(binding.sliderCharacter.value.toInt())
            binding.tvEnvironmentValue.text = settings.getQualityName(binding.sliderEnvironment.value.toInt())
            binding.tvReflectionValue.text = settings.getQualityName(binding.sliderReflection.value.toInt())
            binding.tvSfxValue.text = settings.getSfxQualityName(binding.sliderSfx.value.toInt())
            binding.tvBloomValue.text = settings.getQualityName(binding.sliderBloom.value.toInt())
            binding.tvAaValue.text = settings.getAAModeName(binding.sliderAa.value.toInt())
            binding.tvSelfShadowValue.text = settings.getSelfShadowName(binding.sliderSelfShadow.value.toInt())
            binding.tvDlssValue.text = settings.getDlssName(binding.sliderDlss.value.toInt())
            binding.tvParticleTrailValue.text = settings.getParticleTrailName(binding.sliderParticleTrail.value.toInt())
        }
    }
    
    /**
     * Clear preset selection when user modifies individual settings.
     * This effectively sets graphicsQuality to 0 (Custom) so user can freely modify settings.
     */
    private fun setCustomMode() {
        if (!isApplyingPreset) {
            currentSettings?.graphicsQuality = 0
            binding.sliderGraphicsQuality.value = 0f
            binding.tvGraphicsQualityValue.text = currentSettings?.getMasterQualityName(0) ?: "Custom"
            binding.toggleGroupPresets.clearChecked()
        }
    }
    
    private fun getCurrentSettingsFromUI(): com.ireddragonicy.hsrgraphicdroid.data.GraphicsSettings {
        // Resolution and Fullscreen Mode are READ ONLY - use values from currentSettings
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
            // Use current values from game, don't modify these
            screenWidth = currentSettings?.screenWidth ?: 1920,
            screenHeight = currentSettings?.screenHeight ?: 1080,
            fullscreenMode = currentSettings?.fullscreenMode ?: 1,
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
                    .setAnchorView(binding.bottomActionBar)
                    .setAction(R.string.kill_game) {
                        killGame()
                    }
                    .show()
            } else {
                Snackbar.make(binding.root, getString(R.string.apply_failed), Snackbar.LENGTH_SHORT)
                    .setAnchorView(binding.bottomActionBar)
                    .show()
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
                Snackbar.make(binding.root, getString(R.string.backup_created), Snackbar.LENGTH_SHORT)
                    .setAnchorView(binding.bottomActionBar)
                    .show()
                loadBackups()
            } else {
                Snackbar.make(binding.root, getString(R.string.backup_failed), Snackbar.LENGTH_SHORT)
                    .setAnchorView(binding.bottomActionBar)
                    .show()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        loadBackups()
        // Don't reload settings on resume to preserve user changes
        // Settings are loaded in onCreate
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
                Snackbar.make(binding.root, getString(R.string.error_read_settings), Snackbar.LENGTH_LONG)
                    .setAnchorView(binding.bottomActionBar)
                    .show()
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
                    Snackbar.make(binding.root, getString(R.string.backup_created), Snackbar.LENGTH_SHORT)
                        .setAnchorView(binding.bottomActionBar)
                        .show()
                    loadBackups()
                } else {
                    Snackbar.make(binding.root, getString(R.string.backup_failed), Snackbar.LENGTH_SHORT)
                        .setAnchorView(binding.bottomActionBar)
                        .show()
                }
            } else {
                Snackbar.make(binding.root, getString(R.string.error_read_settings), Snackbar.LENGTH_SHORT)
                    .setAnchorView(binding.bottomActionBar)
                    .show()
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
                    .setAnchorView(binding.bottomActionBar)
                    .setAction(R.string.kill_game) {
                        killGame()
                    }
                    .show()
            } else {
                Snackbar.make(binding.root, getString(R.string.restore_failed), Snackbar.LENGTH_SHORT)
                    .setAnchorView(binding.bottomActionBar)
                    .show()
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
                Snackbar.make(binding.root, getString(R.string.backup_deleted), Snackbar.LENGTH_SHORT)
                    .setAnchorView(binding.bottomActionBar)
                    .show()
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
