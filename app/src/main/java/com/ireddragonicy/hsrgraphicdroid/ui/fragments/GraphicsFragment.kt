package com.ireddragonicy.hsrgraphicdroid.ui.fragments

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.ireddragonicy.hsrgraphicdroid.R
import com.ireddragonicy.hsrgraphicdroid.data.BackupData
import com.ireddragonicy.hsrgraphicdroid.data.GraphicsSettings
import com.ireddragonicy.hsrgraphicdroid.data.SettingsChangeManager
import com.ireddragonicy.hsrgraphicdroid.data.SettingChange
import com.ireddragonicy.hsrgraphicdroid.databinding.FragmentGraphicsBinding
import com.ireddragonicy.hsrgraphicdroid.ui.BackupAdapter
import com.ireddragonicy.hsrgraphicdroid.ui.base.BaseFragment
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class GraphicsFragment :
    BaseFragment<FragmentGraphicsBinding>(FragmentGraphicsBinding::inflate) {

    private val backupAdapter by lazy {
        BackupAdapter(
            onRestore = { backup -> restoreBackup(backup) },
            onDelete = { backup -> confirmDelete(backup) }
        )
    }

    private var currentSettings: GraphicsSettings? = null
    private var pendingXmlContent: String? = null
    private var isApplyingPreset = false
    private var isUpdatingUI = false  // Prevent recording changes during UI updates
    
    // Professional Change Management
    private val changeManager = SettingsChangeManager()
    private var lastGameSettings: GraphicsSettings? = null  // Settings last read from game

    private val createXmlFileLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/xml")
    ) { uri ->
        uri?.let { saveXmlToUri(it) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.appBarLayout.applyTopInsetPadding()
        binding.bottomActionBar.applyBottomInsetPadding()

        setupToolbar()
        setupUndoRedo()
        setupPendingChangesBanner()
        setupButtons()
        setupGraphicsEditor()
        observeStatus()
        observeChangeManager()

        viewLifecycleOwner.lifecycleScope.launch {
            loadCurrentSettings()
            loadBackups()
        }
    }

    private fun setupToolbar() {
        binding.toolbar.title = getString(R.string.graphics_editor)
        binding.toolbar.inflateMenu(R.menu.menu_graphics_editor)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_reset -> {
                    confirmResetChanges()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupUndoRedo() {
        binding.btnUndo.setOnClickListener {
            performUndo()
        }
        
        binding.btnRedo.setOnClickListener {
            performRedo()
        }
    }

    private fun setupPendingChangesBanner() {
        binding.btnViewChanges.setOnClickListener {
            showPendingChangesDialog()
        }
        
        binding.btnDismissExternalChanges.setOnClickListener {
            changeManager.clearExternalChanges()
            binding.externalChangesBanner.isVisible = false
        }
    }

    private fun observeChangeManager() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe undo/redo state
                launch {
                    changeManager.canUndo.collect { canUndo ->
                        binding.btnUndo.isEnabled = canUndo
                        binding.btnUndo.alpha = if (canUndo) 1f else 0.38f
                    }
                }
                
                launch {
                    changeManager.canRedo.collect { canRedo ->
                        binding.btnRedo.isEnabled = canRedo
                        binding.btnRedo.alpha = if (canRedo) 1f else 0.38f
                    }
                }
                
                // Observe pending changes count
                launch {
                    changeManager.pendingChangesCount.collect { count ->
                        updatePendingChangesBanner(count)
                    }
                }
                
                // Observe modified fields for highlighting
                launch {
                    changeManager.modifiedFields.collect { modifiedFields ->
                        updateModifiedIndicators(modifiedFields)
                    }
                }
                
                // Observe external changes
                launch {
                    changeManager.externalChanges.collect { changes ->
                        if (changes.isNotEmpty()) {
                            showExternalChangesBanner(changes)
                        }
                    }
                }
            }
        }
    }

    private fun updatePendingChangesBanner(count: Int) {
        if (count > 0) {
            binding.pendingChangesBanner.isVisible = true
            binding.tvPendingChanges.text = getString(R.string.pending_changes, count)
            
            // Update apply button text
            binding.btnApply.text = getString(R.string.apply_pending_changes, count)
            
            // Animate banner appearance
            if (binding.pendingChangesBanner.alpha == 0f) {
                binding.pendingChangesBanner.alpha = 0f
                binding.pendingChangesBanner.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start()
            }
        } else {
            binding.pendingChangesBanner.isVisible = false
            binding.btnApply.text = getString(R.string.apply_settings_now)
        }
    }

    private fun updateModifiedIndicators(modifiedFields: Set<String>) {
        // Update each setting's modified badge visibility - Performance
        binding.badgeFpsModified?.isVisible = modifiedFields.contains("fps")
        binding.badgeVsyncModified?.isVisible = modifiedFields.contains("vsync")
        binding.badgeRenderScaleModified?.isVisible = modifiedFields.contains("renderScale")
        
        // Visual Fidelity badges
        binding.badgeResolutionModified?.isVisible = modifiedFields.contains("resolution")
        binding.badgeShadowModified?.isVisible = modifiedFields.contains("shadow")
        binding.badgeLightModified?.isVisible = modifiedFields.contains("light")
        binding.badgeCharacterModified?.isVisible = modifiedFields.contains("character")
        binding.badgeEnvironmentModified?.isVisible = modifiedFields.contains("environment")
        binding.badgeReflectionModified?.isVisible = modifiedFields.contains("reflection")
        
        // Effects & Post-processing badges
        binding.badgeSfxModified?.isVisible = modifiedFields.contains("sfx")
        binding.badgeBloomModified?.isVisible = modifiedFields.contains("bloom")
        binding.badgeAaModified?.isVisible = modifiedFields.contains("aa")
        binding.badgeSelfShadowModified?.isVisible = modifiedFields.contains("selfShadow")
        
        // Upscaling & Transparency badges
        binding.badgeMetalFxModified?.isVisible = modifiedFields.contains("metalFx")
        binding.badgeHalfResModified?.isVisible = modifiedFields.contains("halfRes")
        binding.badgeDlssModified?.isVisible = modifiedFields.contains("dlss")
        
        // Display badges
        binding.badgeParticleTrailModified?.isVisible = modifiedFields.contains("particleTrail")
        
        // Launch optimization badges
        binding.badgeSpeedUpOpenModified?.isVisible = modifiedFields.contains("speedUpOpen")
        
        // Update container backgrounds for modified items - Performance
        updateContainerHighlight(binding.containerFps, modifiedFields.contains("fps"))
        updateContainerHighlight(binding.containerVsync, modifiedFields.contains("vsync"))
        updateContainerHighlight(binding.containerRenderScale, modifiedFields.contains("renderScale"))
        
        // Visual Fidelity containers
        updateContainerHighlight(binding.containerResolution, modifiedFields.contains("resolution"))
        updateContainerHighlight(binding.containerShadow, modifiedFields.contains("shadow"))
        updateContainerHighlight(binding.containerLight, modifiedFields.contains("light"))
        updateContainerHighlight(binding.containerCharacter, modifiedFields.contains("character"))
        updateContainerHighlight(binding.containerEnvironment, modifiedFields.contains("environment"))
        updateContainerHighlight(binding.containerReflection, modifiedFields.contains("reflection"))
        
        // Effects & Post-processing containers
        updateContainerHighlight(binding.containerSfx, modifiedFields.contains("sfx"))
        updateContainerHighlight(binding.containerBloom, modifiedFields.contains("bloom"))
        updateContainerHighlight(binding.containerAa, modifiedFields.contains("aa"))
        updateContainerHighlight(binding.containerSelfShadow, modifiedFields.contains("selfShadow"))
        
        // Upscaling & Transparency containers
        updateContainerHighlight(binding.containerMetalFx, modifiedFields.contains("metalFx"))
        updateContainerHighlight(binding.containerHalfRes, modifiedFields.contains("halfRes"))
        updateContainerHighlight(binding.containerDlss, modifiedFields.contains("dlss"))
        
        // Display containers
        updateContainerHighlight(binding.containerParticleTrail, modifiedFields.contains("particleTrail"))
        
        // Launch optimization containers
        updateContainerHighlight(binding.containerSpeedUpOpen, modifiedFields.contains("speedUpOpen"))
    }

    private fun updateContainerHighlight(container: View?, isModified: Boolean) {
        container ?: return
        if (isModified) {
            container.setBackgroundResource(R.drawable.bg_setting_modified_fill)
        } else {
            // Use proper theme-aware color attribute for dark mode support
            val typedValue = android.util.TypedValue()
            requireContext().theme.resolveAttribute(
                com.google.android.material.R.attr.colorSurfaceContainerHighest,
                typedValue,
                true
            )
            container.setBackgroundColor(typedValue.data)
        }
    }

    private fun showExternalChangesBanner(changes: List<SettingChange>) {
        binding.externalChangesBanner.isVisible = true
        
        val changedFields = changes.take(3).joinToString(", ") { it.fieldName }
        val suffix = if (changes.size > 3) " +${changes.size - 3} more" else ""
        binding.tvExternalChanges.text = "$changedFields$suffix changed"
        
        // Animate banner with attention effect
        binding.externalChangesBanner.alpha = 0f
        binding.externalChangesBanner.animate()
            .alpha(1f)
            .setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
        
        // Haptic feedback
        binding.root.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        
        // Show dialog for user decision
        showExternalChangesDialog(changes)
    }

    private fun showExternalChangesDialog(changes: List<SettingChange>) {
        val changesText = changes.joinToString("\n") { "• ${it.getDisplayText()}" }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.changes_detected)
            .setMessage(getString(R.string.external_changes_message, changesText))
            .setPositiveButton(R.string.load_game_settings) { _, _ ->
                lastGameSettings?.let { gameSettings ->
                    isUpdatingUI = true
                    currentSettings = gameSettings.copy()
                    updateUIWithSettings(gameSettings)
                    changeManager.setBaseline(gameSettings)
                    isUpdatingUI = false
                    binding.externalChangesBanner.isVisible = false
                    
                    Snackbar.make(binding.root, R.string.settings_loaded, Snackbar.LENGTH_SHORT)
                        .setAnchorView(binding.bottomActionBar)
                        .show()
                }
            }
            .setNegativeButton(R.string.keep_current) { _, _ ->
                changeManager.clearExternalChanges()
                binding.externalChangesBanner.isVisible = false
            }
            .setCancelable(false)
            .show()
    }

    private fun showPendingChangesDialog() {
        val currentUI = getCurrentSettingsFromUI()
        val changes = changeManager.getModifiedFieldsDetails(currentUI)
        
        if (changes.isEmpty()) {
            Snackbar.make(binding.root, R.string.no_changes_to_apply, Snackbar.LENGTH_SHORT)
                .setAnchorView(binding.bottomActionBar)
                .show()
            return
        }
        
        val dialogView = layoutInflater.inflate(R.layout.dialog_pending_changes, null)
        val recycler = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerChanges)
        val adapter = PendingChangesAdapter(changes)
        recycler.adapter = adapter
        recycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.pending_changes_title, changes.size))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.apply_pending_changes, changes.size)) { _, _ ->
                applySettings()
            }
            .setNeutralButton(R.string.reset_all_changes) { _, _ ->
                confirmResetChanges()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun confirmResetChanges() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.reset_all_changes)
            .setMessage(R.string.reset_changes_message)
            .setPositiveButton(R.string.reset) { _, _ ->
                changeManager.resetToBaseline()?.let { baseline ->
                    isUpdatingUI = true
                    currentSettings = baseline.copy()
                    updateUIWithSettings(baseline)
                    isUpdatingUI = false
                    
                    Snackbar.make(binding.root, R.string.changes_discarded, Snackbar.LENGTH_SHORT)
                        .setAnchorView(binding.bottomActionBar)
                        .show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun performUndo() {
        val previousSettings = changeManager.undo()
        if (previousSettings != null) {
            isUpdatingUI = true
            currentSettings = previousSettings.copy()
            updateUIWithSettings(previousSettings)
            isUpdatingUI = false
            
            // Haptic feedback
            binding.root.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            
            Snackbar.make(binding.root, R.string.undo, Snackbar.LENGTH_SHORT)
                .setAnchorView(binding.bottomActionBar)
                .show()
        } else {
            Snackbar.make(binding.root, R.string.nothing_to_undo, Snackbar.LENGTH_SHORT)
                .setAnchorView(binding.bottomActionBar)
                .show()
        }
    }

    private fun performRedo() {
        val redoSettings = changeManager.redo()
        if (redoSettings != null) {
            isUpdatingUI = true
            currentSettings = redoSettings.copy()
            updateUIWithSettings(redoSettings)
            isUpdatingUI = false
            
            // Haptic feedback
            binding.root.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            
            Snackbar.make(binding.root, R.string.redo, Snackbar.LENGTH_SHORT)
                .setAnchorView(binding.bottomActionBar)
                .show()
        } else {
            Snackbar.make(binding.root, R.string.nothing_to_redo, Snackbar.LENGTH_SHORT)
                .setAnchorView(binding.bottomActionBar)
                .show()
        }
    }

    // Inner adapter class for pending changes
    private inner class PendingChangesAdapter(
        private val changes: List<SettingChange>
    ) : androidx.recyclerview.widget.RecyclerView.Adapter<PendingChangesAdapter.ViewHolder>() {
        
        inner class ViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
            val tvChange: TextView = view.findViewById(android.R.id.text1)
        }
        
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val change = changes[position]
            holder.tvChange.text = getString(R.string.change_from_to, change.fieldName, change.localValue, change.gameValue)
            holder.tvChange.setTextColor(requireContext().getColor(com.google.android.material.R.color.m3_sys_color_dynamic_light_tertiary))
        }
        
        override fun getItemCount() = changes.size
    }

    private fun observeStatus() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.status.collect { status ->
                    binding.chipRootStatus.text = if (status.isChecking) {
                        getString(R.string.checking)
                    } else if (status.isRootGranted) {
                        getString(R.string.root_granted)
                    } else {
                        getString(R.string.root_check_failed)
                    }
                    
                    val rootIcon = if (status.isChecking) R.drawable.ic_help
                    else if (status.isRootGranted) R.drawable.ic_check
                    else R.drawable.ic_close
                    
                    binding.chipRootStatus.setChipIconResource(rootIcon)
                    
                    binding.chipRootStatus.setOnClickListener {
                        if (!status.isRootGranted && !status.isChecking) {
                            promptRootAccess()
                        }
                    }

                    binding.chipGameStatus.text = if (status.isChecking) {
                        getString(R.string.checking)
                    } else if (status.isGameInstalled) {
                        getString(R.string.game_found) + status.gameVersion?.let { " ($it)" }
                    } else {
                        getString(R.string.game_not_found)
                    }
                    
                    val gameIcon = if (status.isChecking) R.drawable.ic_help
                    else if (status.isGameInstalled) R.drawable.ic_check
                    else R.drawable.ic_close
                    
                    binding.chipGameStatus.setChipIconResource(gameIcon)

                    binding.layoutGameActions.isVisible = status.isGameInstalled && !status.isChecking
                    binding.layoutExportShare.isVisible = status.isGameInstalled && status.isRootGranted && !status.isChecking
                }
            }
        }
    }

    private fun setupButtons() {
        binding.fabRefresh.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                mainViewModel.refreshStatus()
                loadBackups()
                loadCurrentSettings()
            }
        }

        binding.btnLaunchGame.setOnClickListener { launchGame() }
        binding.btnGameSettings.setOnClickListener { openGameAppInfo() }
        binding.btnExportXml.setOnClickListener { exportXmlFile() }
        binding.btnShareXml.setOnClickListener { shareXmlFile() }
        binding.btnShowBackups.setOnClickListener { showBackupsSheet() }
    }

    private fun setupGraphicsEditor() {
        binding.btnPresetLow.setOnClickListener { applyExtendedPreset(0) }
        binding.btnPresetMedium.setOnClickListener { applyExtendedPreset(1) }
        binding.btnPresetHigh.setOnClickListener { applyExtendedPreset(2) }
        binding.btnPresetUltra.setOnClickListener { applyExtendedPreset(3) }
        binding.btnPresetMax.setOnClickListener { applyExtendedPreset(4) }

        setupSliderListeners()
        setupSwitchListeners()

        binding.btnApply.setOnClickListener { applySettings() }
        binding.btnSaveBackup.setOnClickListener { showSaveBackupDialog() }
    }

    private fun setupSliderListeners() {
        val sliderConfigs: List<Triple<com.google.android.material.slider.Slider, android.widget.TextView, (Float) -> String>> =
            listOf(
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

        sliderConfigs.forEach { (slider, textView, formatter) ->
            var previousValue = slider.value
            
            slider.addOnSliderTouchListener(object : com.google.android.material.slider.Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: com.google.android.material.slider.Slider) {
                    previousValue = slider.value
                }
                
                override fun onStopTrackingTouch(slider: com.google.android.material.slider.Slider) {
                    if (!isUpdatingUI && !isApplyingPreset && previousValue != slider.value) {
                        val fieldName = getSliderFieldName(slider)
                        changeManager.recordChange(fieldName, previousValue, slider.value, getCurrentSettingsFromUI())
                    }
                }
            })
            
            slider.addOnChangeListener { _, value, fromUser ->
                textView.text = formatter(value)
                if (fromUser) setCustomMode()
            }
        }

        binding.sliderGraphicsQuality.addOnChangeListener { _, value, fromUser ->
            val quality = value.toInt()
            binding.tvGraphicsQualityValue.text = currentSettings?.getMasterQualityName(quality) ?: "Custom"
            currentSettings?.graphicsQuality = quality
            if (fromUser && quality == 0) {
                binding.toggleGroupPresets.clearChecked()
            }
            if (fromUser && !isUpdatingUI) {
                changeManager.recordChange("graphicsQuality", currentSettings?.graphicsQuality, quality, getCurrentSettingsFromUI())
            }
        }
    }

    private fun getSliderFieldName(slider: com.google.android.material.slider.Slider): String {
        return when (slider) {
            binding.sliderFps -> "fps"
            binding.sliderRenderScale -> "renderScale"
            binding.sliderResolution -> "resolution"
            binding.sliderShadow -> "shadow"
            binding.sliderLight -> "light"
            binding.sliderCharacter -> "character"
            binding.sliderEnvironment -> "environment"
            binding.sliderReflection -> "reflection"
            binding.sliderSfx -> "sfx"
            binding.sliderBloom -> "bloom"
            binding.sliderAa -> "aa"
            binding.sliderSelfShadow -> "selfShadow"
            binding.sliderDlss -> "dlss"
            binding.sliderParticleTrail -> "particleTrail"
            binding.sliderGraphicsQuality -> "graphicsQuality"
            else -> "unknown"
        }
    }

    private fun setupSwitchListeners() {
        binding.switchVsync.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingUI && !isApplyingPreset) {
                changeManager.recordChange("vsync", !isChecked, isChecked, getCurrentSettingsFromUI())
            }
            setCustomMode()
        }
        
        binding.switchMetalFx.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingUI && !isApplyingPreset) {
                changeManager.recordChange("metalFx", !isChecked, isChecked, getCurrentSettingsFromUI())
            }
            setCustomMode()
        }
        
        binding.switchHalfResTransparent.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingUI && !isApplyingPreset) {
                changeManager.recordChange("halfRes", !isChecked, isChecked, getCurrentSettingsFromUI())
            }
            setCustomMode()
        }
        
        binding.switchSpeedUpOpen.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingUI && !isApplyingPreset) {
                changeManager.recordChange("speedUpOpen", !isChecked, isChecked, getCurrentSettingsFromUI())
            }
            currentSettings?.speedUpOpen = if (isChecked) 1 else 0
        }
    }

    private suspend fun loadCurrentSettings() {
        binding.progressIndicator.show()

        val gameSettings = mainViewModel.readGraphicsSettings()

        binding.progressIndicator.hide()

        if (gameSettings != null) {
            // Check for external changes if we already have local settings
            if (currentSettings != null && lastGameSettings != null) {
                val externalChanges = changeManager.detectExternalChanges(gameSettings, currentSettings!!)
                if (externalChanges.isNotEmpty()) {
                    lastGameSettings = gameSettings.copy()
                    // External changes will be shown via the observeChangeManager flow
                    return
                }
            }
            
            isUpdatingUI = true
            lastGameSettings = gameSettings.copy()
            currentSettings = gameSettings.copy()
            changeManager.setBaseline(gameSettings)
            updateUIWithSettings(gameSettings)
            isUpdatingUI = false
            
            Snackbar.make(binding.root, getString(R.string.settings_loaded), Snackbar.LENGTH_SHORT)
                .setAnchorView(binding.bottomActionBar)
                .show()
        } else {
            val defaultSettings = GraphicsSettings()
            isUpdatingUI = true
            currentSettings = defaultSettings
            changeManager.setBaseline(defaultSettings)
            updateUIWithSettings(defaultSettings)
            isUpdatingUI = false
            
            Snackbar.make(binding.root, getString(R.string.error_read_settings_default), Snackbar.LENGTH_SHORT)
                .setAnchorView(binding.bottomActionBar)
                .show()
        }
    }

    private fun updateUIWithSettings(settings: GraphicsSettings) {
        val wasUpdating = isUpdatingUI
        isUpdatingUI = true
        
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

        binding.sliderAa.value = settings.aaMode.toFloat()
        binding.tvAaValue.text = settings.getAAModeName(settings.aaMode)

        binding.sliderSelfShadow.value = settings.enableSelfShadow.toFloat()
        binding.tvSelfShadowValue.text = settings.getSelfShadowName(settings.enableSelfShadow)

        binding.switchMetalFx.isChecked = settings.enableMetalFXSU
        binding.switchHalfResTransparent.isChecked = settings.enableHalfResTransparent

        binding.sliderDlss.value = settings.dlssQuality.toFloat()
        binding.tvDlssValue.text = settings.getDlssName(settings.dlssQuality)

        binding.sliderParticleTrail.value = settings.particleTrailSmoothness.toFloat()
        binding.tvParticleTrailValue.text = settings.getParticleTrailName(settings.particleTrailSmoothness)

        binding.tvCurrentResolution.text = "${settings.screenWidth}×${settings.screenHeight}"
        binding.tvFullscreenMode.text = settings.getFullscreenModeName()
        binding.switchSpeedUpOpen.isChecked = settings.speedUpOpen == 1
        
        isUpdatingUI = wasUpdating
    }

    private fun applyExtendedPreset(level: Int) {
        currentSettings?.let { settings ->
            isApplyingPreset = true
            settings.graphicsQuality = 0
            binding.sliderGraphicsQuality.value = 0f
            binding.tvGraphicsQualityValue.text = settings.getMasterQualityName(0)

            when (level) {
                0 -> {
                    binding.sliderFps.value = 30f
                    binding.switchVsync.isChecked = true
                    binding.sliderRenderScale.value = 0.6f
                    binding.sliderResolution.value = 0f
                    binding.sliderShadow.value = 0f
                    binding.sliderLight.value = 0f
                    binding.sliderCharacter.value = 0f
                    binding.sliderEnvironment.value = 0f
                    binding.sliderReflection.value = 0f
                    binding.sliderSfx.value = 1f
                    binding.sliderBloom.value = 0f
                    binding.sliderAa.value = 0f
                    binding.sliderSelfShadow.value = 0f
                    binding.sliderDlss.value = 0f
                    binding.sliderParticleTrail.value = 0f
                    binding.switchMetalFx.isChecked = false
                    binding.switchHalfResTransparent.isChecked = false
                }

                1 -> {
                    binding.sliderFps.value = 60f
                    binding.switchVsync.isChecked = true
                    binding.sliderRenderScale.value = 0.8f
                    binding.sliderResolution.value = 1f
                    binding.sliderShadow.value = 1f
                    binding.sliderLight.value = 1f
                    binding.sliderCharacter.value = 1f
                    binding.sliderEnvironment.value = 1f
                    binding.sliderReflection.value = 1f
                    binding.sliderSfx.value = 2f
                    binding.sliderBloom.value = 1f
                    binding.sliderAa.value = 1f
                    binding.sliderSelfShadow.value = 0f
                    binding.sliderDlss.value = 0f
                    binding.sliderParticleTrail.value = 1f
                }

                2 -> {
                    binding.sliderFps.value = 60f
                    binding.switchVsync.isChecked = true
                    binding.sliderRenderScale.value = 1.0f
                    binding.sliderResolution.value = 2f
                    binding.sliderShadow.value = 2f
                    binding.sliderLight.value = 2f
                    binding.sliderCharacter.value = 2f
                    binding.sliderEnvironment.value = 2f
                    binding.sliderReflection.value = 2f
                    binding.sliderSfx.value = 3f
                    binding.sliderBloom.value = 2f
                    binding.sliderAa.value = 1f
                    binding.sliderSelfShadow.value = 1f
                    binding.sliderDlss.value = 1f
                    binding.sliderParticleTrail.value = 2f
                }

                3 -> {
                    binding.sliderFps.value = 120f
                    binding.switchVsync.isChecked = false
                    binding.sliderRenderScale.value = 1.2f
                    binding.sliderResolution.value = 3f
                    binding.sliderShadow.value = 3f
                    binding.sliderLight.value = 3f
                    binding.sliderCharacter.value = 3f
                    binding.sliderEnvironment.value = 3f
                    binding.sliderReflection.value = 3f
                    binding.sliderSfx.value = 4f
                    binding.sliderBloom.value = 3f
                    binding.sliderAa.value = 2f
                    binding.sliderSelfShadow.value = 2f
                    binding.sliderDlss.value = 1f
                    binding.sliderParticleTrail.value = 3f
                }

                4 -> {
                    binding.sliderFps.value = 120f
                    binding.switchVsync.isChecked = false
                    binding.sliderRenderScale.value = 2.0f
                    binding.sliderResolution.value = 5f
                    binding.sliderShadow.value = 5f
                    binding.sliderLight.value = 5f
                    binding.sliderCharacter.value = 5f
                    binding.sliderEnvironment.value = 5f
                    binding.sliderReflection.value = 5f
                    binding.sliderSfx.value = 5f
                    binding.sliderBloom.value = 5f
                    binding.sliderAa.value = 2f
                    binding.sliderSelfShadow.value = 2f
                    binding.switchMetalFx.isChecked = true
                    binding.sliderDlss.value = 1f
                    binding.sliderParticleTrail.value = 3f
                }
            }

            updateSliderDisplays()
            
            // Record the preset as a single change for undo/redo
            val presetNames = arrayOf("Low", "Medium", "High", "Ultra", "MAX")
            changeManager.recordChange("preset", "previous", presetNames[level], getCurrentSettingsFromUI())
            
            isApplyingPreset = false

            Snackbar.make(
                binding.root,
                "Extended preset: ${presetNames[level]} applied (Custom mode)",
                Snackbar.LENGTH_SHORT
            ).setAnchorView(binding.bottomActionBar).show()
        }
    }

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

    private fun setCustomMode() {
        if (!isApplyingPreset) {
            currentSettings?.graphicsQuality = 0
            binding.sliderGraphicsQuality.value = 0f
            binding.tvGraphicsQualityValue.text = currentSettings?.getMasterQualityName(0) ?: "Custom"
            binding.toggleGroupPresets.clearChecked()
        }
    }

    private fun getCurrentSettingsFromUI(): GraphicsSettings {
        return GraphicsSettings(
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
            screenWidth = currentSettings?.screenWidth ?: 1920,
            screenHeight = currentSettings?.screenHeight ?: 1080,
            fullscreenMode = currentSettings?.fullscreenMode ?: 1,
            speedUpOpen = if (binding.switchSpeedUpOpen.isChecked) 1 else 0
        )
    }

    private fun applySettings() {
        val settings = getCurrentSettingsFromUI()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.apply)
            .setMessage(getString(R.string.apply_settings_message))
            .setPositiveButton(R.string.apply) { _, _ ->
                performApplySettings(settings)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun performApplySettings(settings: GraphicsSettings) {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.progressIndicator.show()
            val success = mainViewModel.writeGraphicsSettings(settings)
            binding.progressIndicator.hide()

            if (success) {
                currentSettings = settings.copy()
                lastGameSettings = settings.copy()
                changeManager.setBaseline(settings)  // Reset baseline after successful apply
                
                Snackbar.make(binding.root, getString(R.string.settings_applied), Snackbar.LENGTH_LONG)
                    .setAnchorView(binding.bottomActionBar)
                    .setAction(R.string.kill_game) { killGame() }
                    .show()
            } else {
                Snackbar.make(binding.root, getString(R.string.apply_failed), Snackbar.LENGTH_SHORT)
                    .setAnchorView(binding.bottomActionBar)
                    .show()
            }
        }
    }

    private fun showSaveBackupDialog() {
        val input = EditText(requireContext()).apply {
            hint = getString(R.string.backup_name_hint)
        }

        MaterialAlertDialogBuilder(requireContext())
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
        viewLifecycleOwner.lifecycleScope.launch {
            binding.progressIndicator.show()
            val success = mainViewModel.saveBackup(name, settings)
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

    private suspend fun loadBackups() {
        val backups = mainViewModel.loadBackups().reversed()
        backupAdapter.submitList(backups)
    }

    private fun restoreBackup(backup: BackupData) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.restore_backup)
            .setMessage(getString(R.string.restore_backup_message, backup.name))
            .setPositiveButton(R.string.restore) { _, _ ->
                performRestore(backup)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun performRestore(backup: BackupData) {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.progressIndicator.show()
            val success = mainViewModel.writeGraphicsSettings(backup.settings)
            binding.progressIndicator.hide()

            if (success) {
                currentSettings = backup.settings
                updateUIWithSettings(backup.settings)
                Snackbar.make(binding.root, getString(R.string.restore_success), Snackbar.LENGTH_LONG)
                    .setAnchorView(binding.bottomActionBar)
                    .setAction(R.string.kill_game) { killGame() }
                    .show()
            } else {
                Snackbar.make(binding.root, getString(R.string.restore_failed), Snackbar.LENGTH_SHORT)
                    .setAnchorView(binding.bottomActionBar)
                    .show()
            }
        }
    }

    private fun confirmDelete(backup: BackupData) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_backup)
            .setMessage(getString(R.string.delete_backup_message, backup.name))
            .setPositiveButton(R.string.delete) { _, _ ->
                deleteBackup(backup)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun deleteBackup(backup: BackupData) {
        viewLifecycleOwner.lifecycleScope.launch {
            val success = mainViewModel.deleteBackup(backup)
            if (success) {
                loadBackups()
                Snackbar.make(binding.root, getString(R.string.backup_deleted), Snackbar.LENGTH_SHORT)
                    .setAnchorView(binding.bottomActionBar)
                    .show()
            }
        }
    }

    private fun showBackupsSheet() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_backups, null)
        val recycler = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerBackupsSheet)
        val countView = dialogView.findViewById<android.widget.TextView>(R.id.tvBackupCountSheet)
        recycler.adapter = backupAdapter
        recycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            val backups = mainViewModel.loadBackups().reversed()
            backupAdapter.submitList(backups)
            countView.text = getString(R.string.backup_count, backups.size)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.saved_backups)
            .setView(dialogView)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun killGame() {
        viewLifecycleOwner.lifecycleScope.launch {
            val success = mainViewModel.killGame()
            if (success) {
                Toast.makeText(requireContext(), R.string.game_killed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun launchGame() {
        val pkg = mainViewModel.currentPackage()
        if (pkg == null) {
            Snackbar.make(binding.root, getString(R.string.game_not_found), Snackbar.LENGTH_SHORT)
                .setAnchorView(binding.bottomActionBar)
                .show()
            return
        }
        val intent = requireContext().packageManager.getLaunchIntentForPackage(pkg)
        if (intent != null) {
            startActivity(intent)
        } else {
            Snackbar.make(binding.root, getString(R.string.game_not_found), Snackbar.LENGTH_SHORT)
                .setAnchorView(binding.bottomActionBar)
                .show()
        }
    }

    private fun openGameAppInfo() {
        val pkg = mainViewModel.currentPackage()
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

    private fun promptRootAccess() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.root_required)
            .setMessage(R.string.root_required_message)
            .setPositiveButton(R.string.grant) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    with(Shell.getShell()) {
                        // no-op just request
                    }
                    mainViewModel.refreshStatus()
                }
            }
            .setNegativeButton(R.string.exit, null)
            .setCancelable(false)
            .show()
    }

    private fun exportXmlFile() {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.progressIndicator.show()
            val content = mainViewModel.getPrefsContent()
            binding.progressIndicator.hide()

            if (content != null) {
                pendingXmlContent = content
                val pkg = mainViewModel.currentPackage() ?: "hsr"
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
        viewLifecycleOwner.lifecycleScope.launch {
            val success = runCatching {
                requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(content.toByteArray())
                }
                true
            }.getOrDefault(false)

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
        viewLifecycleOwner.lifecycleScope.launch {
            binding.progressIndicator.show()
            val content = mainViewModel.getPrefsContent()
            binding.progressIndicator.hide()

            if (content != null) {
                val pkg = mainViewModel.currentPackage() ?: "hsr"
                val fileName = "${pkg}_settings.xml"
                val shareFile = java.io.File(requireContext().cacheDir, fileName)
                shareFile.writeText(content)

                val shareUri = androidx.core.content.FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
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
}

