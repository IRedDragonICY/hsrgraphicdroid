package com.ireddragonicy.hsrgraphicdroid.ui

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
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
    
    // Preset configurations
    private enum class Preset(
        val fps: Int,
        val renderScale: Double,
        val quality: Int,
        val selfShadow: Int,
        val metalFx: Boolean,
        val halfRes: Boolean,
        val dlss: Int,
        val aaMode: Int,
        val particleTrail: Int
    ) {
        LOW(30, 0.8, 1, 0, false, true, 0, 0, 0),
        MEDIUM(60, 1.0, 3, 1, false, false, 2, 1, 1),
        HIGH(60, 1.2, 4, 2, true, false, 3, 1, 2),
        ULTRA(120, 1.4, 5, 2, true, false, 3, 2, 3)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        binding = ActivityGraphicsEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupWindowInsets()
        setupToolbar()
        initializeSettings()
        setupAllControls()
        setupButtons()
    }
    
    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, insets.top, 0, 0)
            windowInsets
        }
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
    
    private fun initializeSettings() {
        gameManager = HsrGameManager(this)
        settings = intent.getStringExtra("settings")?.let {
            Gson().fromJson(it, GraphicsSettings::class.java)
        } ?: GraphicsSettings()
    }
    
    private fun setupAllControls() {
        setupFpsControl()
        setupSwitchControls()
        setupQualitySliders()
        setupSpecialSliders()
    }
    
    private fun setupFpsControl() {
        binding.sliderFps.setupSlider(
            initialValue = settings.fps.toFloat(),
            displayView = binding.tvFpsValue,
            formatter = { it.toInt().toString() }
        ) { settings.fps = it.toInt() }
    }
    
    private fun setupSwitchControls() {
        binding.switchVsync.setupSwitch(settings.enableVSync) { settings.enableVSync = it }
        binding.switchMetalFx.setupSwitch(settings.enableMetalFXSU) { settings.enableMetalFXSU = it }
        binding.switchHalfResTransparent.setupSwitch(settings.enableHalfResTransparent) { settings.enableHalfResTransparent = it }
    }
    
    private fun setupQualitySliders() {
        // Define quality slider configurations: (slider, textView, property getter, property setter)
        val qualitySliders = listOf(
            Triple(binding.sliderRenderScale, binding.tvRenderScaleValue, 
                QualityConfig(settings.renderScale.toFloat(), { String.format("%.1f", it) }) { settings.renderScale = it.toDouble() }),
            Triple(binding.sliderResolution, binding.tvResolutionValue,
                QualityConfig(settings.resolutionQuality.toFloat(), { settings.getQualityName(it.toInt()) }) { settings.resolutionQuality = it.toInt() }),
            Triple(binding.sliderShadow, binding.tvShadowValue,
                QualityConfig(settings.shadowQuality.toFloat(), { settings.getQualityName(it.toInt()) }) { settings.shadowQuality = it.toInt() }),
            Triple(binding.sliderLight, binding.tvLightValue,
                QualityConfig(settings.lightQuality.toFloat(), { settings.getQualityName(it.toInt()) }) { settings.lightQuality = it.toInt() }),
            Triple(binding.sliderCharacter, binding.tvCharacterValue,
                QualityConfig(settings.characterQuality.toFloat(), { settings.getQualityName(it.toInt()) }) { settings.characterQuality = it.toInt() }),
            Triple(binding.sliderEnvironment, binding.tvEnvironmentValue,
                QualityConfig(settings.envDetailQuality.toFloat(), { settings.getQualityName(it.toInt()) }) { settings.envDetailQuality = it.toInt() }),
            Triple(binding.sliderReflection, binding.tvReflectionValue,
                QualityConfig(settings.reflectionQuality.toFloat(), { settings.getQualityName(it.toInt()) }) { settings.reflectionQuality = it.toInt() }),
            Triple(binding.sliderSfx, binding.tvSfxValue,
                QualityConfig(settings.sfxQuality.toFloat(), { settings.getQualityName(it.toInt()) }) { settings.sfxQuality = it.toInt() }),
            Triple(binding.sliderBloom, binding.tvBloomValue,
                QualityConfig(settings.bloomQuality.toFloat(), { settings.getQualityName(it.toInt()) }) { settings.bloomQuality = it.toInt() })
        )
        
        qualitySliders.forEach { (slider, textView, config) ->
            slider.setupSlider(config.initialValue, textView, config.formatter, config.setter)
        }
    }
    
    private fun setupSpecialSliders() {
        // AA Mode (Off/TAA/SMAA)
        binding.sliderAa.setupSlider(
            initialValue = settings.aaMode.toFloat(),
            displayView = binding.tvAaValue,
            formatter = { settings.getAAModeName(it.toInt()) }
        ) { settings.aaMode = it.toInt() }
        
        // Self Shadow (Off/Low/High)
        binding.sliderSelfShadow.setupSlider(
            initialValue = settings.enableSelfShadow.toFloat(),
            displayView = binding.tvSelfShadowValue,
            formatter = { settings.getSelfShadowName(it.toInt()) }
        ) { settings.enableSelfShadow = it.toInt() }
        
        // DLSS Quality
        binding.sliderDlss.setupSlider(
            initialValue = settings.dlssQuality.toFloat(),
            displayView = binding.tvDlssValue,
            formatter = { settings.getDlssName(it.toInt()) }
        ) { settings.dlssQuality = it.toInt() }
        
        // Particle Trail Smoothness
        binding.sliderParticleTrail.setupSlider(
            initialValue = settings.particleTrailSmoothness.toFloat(),
            displayView = binding.tvParticleTrailValue,
            formatter = { settings.getParticleTrailName(it.toInt()) }
        ) { settings.particleTrailSmoothness = it.toInt() }
    }
    
    private fun setupButtons() {
        // Preset buttons
        mapOf(
            binding.btnPresetLow to Preset.LOW,
            binding.btnPresetMedium to Preset.MEDIUM,
            binding.btnPresetHigh to Preset.HIGH,
            binding.btnPresetUltra to Preset.ULTRA
        ).forEach { (button, preset) ->
            button.setOnClickListener { applyPreset(preset) }
        }
        
        binding.btnApply.setOnClickListener { confirmApply() }
        binding.btnSaveBackup.setOnClickListener { showBackupDialog() }
    }
    
    private fun applyPreset(preset: Preset) {
        settings.apply {
            fps = preset.fps
            renderScale = preset.renderScale
            resolutionQuality = preset.quality
            shadowQuality = preset.quality
            lightQuality = preset.quality
            characterQuality = preset.quality
            envDetailQuality = preset.quality
            reflectionQuality = preset.quality
            sfxQuality = preset.quality
            bloomQuality = preset.quality
            enableSelfShadow = preset.selfShadow
            enableMetalFXSU = preset.metalFx
            enableHalfResTransparent = preset.halfRes
            dlssQuality = preset.dlss
            aaMode = preset.aaMode
            particleTrailSmoothness = preset.particleTrail
        }
        
        refreshUI()
        showToast(getString(R.string.preset_applied, preset.name))
    }
    
    private fun refreshUI() {
        setupFpsControl()
        setupSwitchControls()
        setupQualitySliders()
        setupSpecialSliders()
    }
    
    private fun confirmApply() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.apply_settings)
            .setMessage(R.string.apply_settings_message)
            .setPositiveButton(R.string.apply) { _, _ -> applySettings() }
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
                    .setAction(R.string.kill_game) { killGame() }
                    .show()
            } else {
                showSnackbar(getString(R.string.apply_failed))
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
            if (success) showToast(getString(R.string.backup_saved))
        }
    }
    
    private fun killGame() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { gameManager.killGame() }
            showToast(getString(R.string.game_killed))
        }
    }
    
    // Extension functions for cleaner setup
    private fun Slider.setupSlider(
        initialValue: Float,
        displayView: TextView,
        formatter: (Float) -> String,
        onValueChanged: (Float) -> Unit
    ) {
        value = initialValue.coerceIn(valueFrom, valueTo)
        displayView.text = formatter(value)
        
        clearOnChangeListeners()
        addOnChangeListener { _, newValue, _ ->
            displayView.text = formatter(newValue)
            onValueChanged(newValue)
        }
    }
    
    private fun SwitchMaterial.setupSwitch(
        initialValue: Boolean,
        onCheckedChanged: (Boolean) -> Unit
    ) {
        isChecked = initialValue
        setOnCheckedChangeListener { _, isChecked -> onCheckedChanged(isChecked) }
    }
    
    // Helper data class for quality slider configuration
    private data class QualityConfig(
        val initialValue: Float,
        val formatter: (Float) -> String,
        val setter: (Float) -> Unit
    )
    
    // Utility functions
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        @Suppress("DEPRECATION")
        onBackPressed()
        return true
    }
}
