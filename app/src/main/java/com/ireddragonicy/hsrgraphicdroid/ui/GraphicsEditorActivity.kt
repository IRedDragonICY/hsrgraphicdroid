package com.ireddragonicy.hsrgraphicdroid.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.ireddragonicy.hsrgraphicdroid.R
import com.ireddragonicy.hsrgraphicdroid.data.GraphicsSettings
import com.ireddragonicy.hsrgraphicdroid.databinding.ActivityGraphicsEditorBinding
import com.ireddragonicy.hsrgraphicdroid.utils.HsrGameManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GraphicsEditorActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityGraphicsEditorBinding
    private lateinit var gameManager: HsrGameManager
    private lateinit var settings: GraphicsSettings
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        binding = ActivityGraphicsEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Handle window insets for AppBarLayout
        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, insets.top, 0, 0)
            windowInsets
        }
        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        gameManager = HsrGameManager(this)
        
        val settingsJson = intent.getStringExtra("settings")
        settings = if (settingsJson != null) {
            Gson().fromJson(settingsJson, GraphicsSettings::class.java)
        } else {
            GraphicsSettings()
        }
        
        setupUI()
        setupButtons()
    }
    
    private fun setupUI() {
        // FPS Settings
        binding.sliderFps.value = settings.fps.toFloat()
        binding.tvFpsValue.text = "${settings.fps}"
        binding.sliderFps.addOnChangeListener { _, value, _ ->
            settings.fps = value.toInt()
            binding.tvFpsValue.text = "${value.toInt()}"
        }
        
        // VSync
        binding.switchVsync.isChecked = settings.enableVSync
        binding.switchVsync.setOnCheckedChangeListener { _, isChecked ->
            settings.enableVSync = isChecked
        }
        
        // Render Scale
        binding.sliderRenderScale.value = settings.renderScale.toFloat()
        binding.tvRenderScaleValue.text = String.format("%.1f", settings.renderScale)
        binding.sliderRenderScale.addOnChangeListener { _, value, _ ->
            settings.renderScale = value.toDouble()
            binding.tvRenderScaleValue.text = String.format("%.1f", value)
        }
        
        // Resolution Quality
        binding.sliderResolution.value = settings.resolutionQuality.toFloat()
        binding.tvResolutionValue.text = settings.getQualityName(settings.resolutionQuality)
        binding.sliderResolution.addOnChangeListener { _, value, _ ->
            settings.resolutionQuality = value.toInt()
            binding.tvResolutionValue.text = settings.getQualityName(value.toInt())
        }
        
        // Shadow Quality
        binding.sliderShadow.value = settings.shadowQuality.toFloat()
        binding.tvShadowValue.text = settings.getQualityName(settings.shadowQuality)
        binding.sliderShadow.addOnChangeListener { _, value, _ ->
            settings.shadowQuality = value.toInt()
            binding.tvShadowValue.text = settings.getQualityName(value.toInt())
        }
        
        // Light Quality
        binding.sliderLight.value = settings.lightQuality.toFloat()
        binding.tvLightValue.text = settings.getQualityName(settings.lightQuality)
        binding.sliderLight.addOnChangeListener { _, value, _ ->
            settings.lightQuality = value.toInt()
            binding.tvLightValue.text = settings.getQualityName(value.toInt())
        }
        
        // Character Quality
        binding.sliderCharacter.value = settings.characterQuality.toFloat()
        binding.tvCharacterValue.text = settings.getQualityName(settings.characterQuality)
        binding.sliderCharacter.addOnChangeListener { _, value, _ ->
            settings.characterQuality = value.toInt()
            binding.tvCharacterValue.text = settings.getQualityName(value.toInt())
        }
        
        // Environment Quality
        binding.sliderEnvironment.value = settings.envDetailQuality.toFloat()
        binding.tvEnvironmentValue.text = settings.getQualityName(settings.envDetailQuality)
        binding.sliderEnvironment.addOnChangeListener { _, value, _ ->
            settings.envDetailQuality = value.toInt()
            binding.tvEnvironmentValue.text = settings.getQualityName(value.toInt())
        }
        
        // Reflection Quality
        binding.sliderReflection.value = settings.reflectionQuality.toFloat()
        binding.tvReflectionValue.text = settings.getQualityName(settings.reflectionQuality)
        binding.sliderReflection.addOnChangeListener { _, value, _ ->
            settings.reflectionQuality = value.toInt()
            binding.tvReflectionValue.text = settings.getQualityName(value.toInt())
        }
        
        // SFX Quality
        binding.sliderSfx.value = settings.sfxQuality.toFloat()
        binding.tvSfxValue.text = settings.getQualityName(settings.sfxQuality)
        binding.sliderSfx.addOnChangeListener { _, value, _ ->
            settings.sfxQuality = value.toInt()
            binding.tvSfxValue.text = settings.getQualityName(value.toInt())
        }
        
        // Bloom Quality
        binding.sliderBloom.value = settings.bloomQuality.toFloat()
        binding.tvBloomValue.text = settings.getQualityName(settings.bloomQuality)
        binding.sliderBloom.addOnChangeListener { _, value, _ ->
            settings.bloomQuality = value.toInt()
            binding.tvBloomValue.text = settings.getQualityName(value.toInt())
        }
        
        // Anti-Aliasing
        binding.switchAa.isChecked = settings.aaMode == 1
        binding.switchAa.setOnCheckedChangeListener { _, isChecked ->
            settings.aaMode = if (isChecked) 1 else 0
        }
        
        // Self Shadow
        binding.sliderSelfShadow.value = settings.enableSelfShadow.toFloat()
        binding.tvSelfShadowValue.text = when(settings.enableSelfShadow) {
            0 -> getString(R.string.off)
            1 -> getString(R.string.low)
            2 -> getString(R.string.high)
            else -> getString(R.string.unknown)
        }
        binding.sliderSelfShadow.addOnChangeListener { _, value, _ ->
            settings.enableSelfShadow = value.toInt()
            binding.tvSelfShadowValue.text = when(value.toInt()) {
                0 -> getString(R.string.off)
                1 -> getString(R.string.low)
                2 -> getString(R.string.high)
                else -> getString(R.string.unknown)
            }
        }
        
        // MetalFX Super Resolution (Apple Silicon)
        binding.switchMetalFx.isChecked = settings.enableMetalFXSU
        binding.switchMetalFx.setOnCheckedChangeListener { _, isChecked ->
            settings.enableMetalFXSU = isChecked
        }
        
        // Half Resolution Transparent
        binding.switchHalfResTransparent.isChecked = settings.enableHalfResTransparent
        binding.switchHalfResTransparent.setOnCheckedChangeListener { _, isChecked ->
            settings.enableHalfResTransparent = isChecked
        }
        
        // DLSS Quality
        binding.sliderDlss.value = settings.dlssQuality.toFloat()
        binding.tvDlssValue.text = when(settings.dlssQuality) {
            0 -> getString(R.string.off)
            1 -> "Performance"
            2 -> "Balanced"
            3 -> "Quality"
            4 -> "Ultra Performance"
            else -> getString(R.string.off)
        }
        binding.sliderDlss.addOnChangeListener { _, value, _ ->
            settings.dlssQuality = value.toInt()
            binding.tvDlssValue.text = when(value.toInt()) {
                0 -> getString(R.string.off)
                1 -> "Performance"
                2 -> "Balanced"
                3 -> "Quality"
                4 -> "Ultra Performance"
                else -> getString(R.string.off)
            }
        }
    }
    
    private fun setupButtons() {
        binding.btnPresetLow.setOnClickListener { applyPreset("low") }
        binding.btnPresetMedium.setOnClickListener { applyPreset("medium") }
        binding.btnPresetHigh.setOnClickListener { applyPreset("high") }
        binding.btnPresetUltra.setOnClickListener { applyPreset("ultra") }
        
        binding.btnApply.setOnClickListener {
            confirmApply()
        }
        
        binding.btnSaveBackup.setOnClickListener {
            showBackupDialog()
        }
    }
    
    private fun applyPreset(preset: String) {
        when (preset) {
            "low" -> {
                settings.fps = 30
                settings.renderScale = 0.8
                settings.resolutionQuality = 1
                settings.shadowQuality = 1
                settings.lightQuality = 1
                settings.characterQuality = 1
                settings.envDetailQuality = 1
                settings.reflectionQuality = 1
                settings.sfxQuality = 1
                settings.bloomQuality = 1
                settings.enableSelfShadow = 0
                settings.enableMetalFXSU = false
                settings.enableHalfResTransparent = true
                settings.dlssQuality = 0
            }
            "medium" -> {
                settings.fps = 60
                settings.renderScale = 1.0
                settings.resolutionQuality = 3
                settings.shadowQuality = 2
                settings.lightQuality = 3
                settings.characterQuality = 3
                settings.envDetailQuality = 2
                settings.reflectionQuality = 2
                settings.sfxQuality = 3
                settings.bloomQuality = 3
                settings.enableSelfShadow = 1
                settings.enableMetalFXSU = false
                settings.enableHalfResTransparent = false
                settings.dlssQuality = 2
            }
            "high" -> {
                settings.fps = 60
                settings.renderScale = 1.2
                settings.resolutionQuality = 4
                settings.shadowQuality = 4
                settings.lightQuality = 4
                settings.characterQuality = 4
                settings.envDetailQuality = 4
                settings.reflectionQuality = 4
                settings.sfxQuality = 4
                settings.bloomQuality = 4
                settings.enableSelfShadow = 2
                settings.enableMetalFXSU = true
                settings.enableHalfResTransparent = false
                settings.dlssQuality = 3
            }
            "ultra" -> {
                settings.fps = 120
                settings.renderScale = 1.4
                settings.resolutionQuality = 5
                settings.shadowQuality = 4
                settings.lightQuality = 5
                settings.characterQuality = 4
                settings.envDetailQuality = 5
                settings.reflectionQuality = 5
                settings.sfxQuality = 4
                settings.bloomQuality = 4
                settings.enableSelfShadow = 2
                settings.enableMetalFXSU = true
                settings.enableHalfResTransparent = false
                settings.dlssQuality = 3
            }
        }
        setupUI()
        Toast.makeText(this, getString(R.string.preset_applied, preset.uppercase()), Toast.LENGTH_SHORT).show()
    }
    
    private fun confirmApply() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.apply_settings)
            .setMessage(R.string.apply_settings_message)
            .setPositiveButton(R.string.apply) { _, _ ->
                applySettings()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun applySettings() {
        lifecycleScope.launch {
            binding.progressIndicator.show()
            
            val success = withContext(Dispatchers.IO) {
                gameManager.writeSettings(settings)
            }
            
            binding.progressIndicator.hide()
            
            if (success) {
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
    
    private fun showBackupDialog() {
        val input = android.widget.EditText(this).apply {
            hint = getString(R.string.backup_name_hint)
        }
        
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.save_as_backup)
            .setView(input)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = input.text.toString().ifEmpty { "Custom ${System.currentTimeMillis()}" }
                saveBackup(name)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun saveBackup(name: String) {
        lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) {
                gameManager.saveBackup(name, settings)
            }
            
            if (success) {
                Toast.makeText(this@GraphicsEditorActivity, R.string.backup_saved, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun killGame() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                gameManager.killGame()
            }
            Toast.makeText(this@GraphicsEditorActivity, R.string.game_killed, Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
