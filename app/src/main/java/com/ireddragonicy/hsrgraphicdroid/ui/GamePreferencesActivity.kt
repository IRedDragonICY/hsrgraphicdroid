package com.ireddragonicy.hsrgraphicdroid.ui

import android.os.Bundle
import android.view.View
import android.view.ViewPropertyAnimator
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.ireddragonicy.hsrgraphicdroid.R
import com.ireddragonicy.hsrgraphicdroid.data.GamePreferences
import com.ireddragonicy.hsrgraphicdroid.databinding.ActivityGamePreferencesBinding
import com.ireddragonicy.hsrgraphicdroid.utils.HsrGameManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Activity for managing game preferences (language, blacklists, etc.)
 * Optimized with proper state management and smooth loading transitions
 */
class GamePreferencesActivity : AppCompatActivity() {

    // region UI State Management
    
    private sealed class UiState {
        data object Loading : UiState()
        data class Success(val preferences: GamePreferences) : UiState()
        data class Error(val message: String) : UiState()
        data object Saving : UiState()
    }
    
    private sealed class SaveResult {
        data object Success : SaveResult()
        data class Failure(val failures: List<String>) : SaveResult()
    }
    
    // endregion

    // region Properties
    
    private lateinit var binding: ActivityGamePreferencesBinding
    private val gameManager by lazy { HsrGameManager(this) }
    
    // Adapters - lazy init untuk mencegah lag saat onCreate
    // NOTE: Blacklist is VIEW ONLY - editing will cause game to re-download data!
    private val videoBlacklistAdapter by lazy {
        BlacklistAdapter(isVideo = true, onDelete = null) // View only, no delete
    }
    
    private val audioBlacklistAdapter by lazy {
        BlacklistAdapter(isVideo = false, onDelete = null) // View only, no delete
    }
    
    private var currentPreferences: GamePreferences? = null
    private val videoBlacklist = mutableListOf<String>()
    private val audioBlacklist = mutableListOf<String>()
    
    // Language chip mappings - compile-time constants
    private companion object {
        val TEXT_LANGUAGE_CHIPS = mapOf(
            R.id.chipTextCn to 0,
            R.id.chipTextTw to 1,
            R.id.chipTextEn to 2,
            R.id.chipTextJp to 3,
            R.id.chipTextKr to 4,
            R.id.chipTextDe to 5,
            R.id.chipTextEs to 6,
            R.id.chipTextFr to 7,
            R.id.chipTextId to 8,
            R.id.chipTextRu to 9,
            R.id.chipTextPt to 10,
            R.id.chipTextTh to 11,
            R.id.chipTextVi to 12
        )
        
        val AUDIO_LANGUAGE_CHIPS = mapOf(
            R.id.chipAudioCn to 0,
            R.id.chipAudioEn to 1,
            R.id.chipAudioJp to 2,
            R.id.chipAudioKr to 3
        )
        
        private const val LOADING_DELAY_MS = 100L
    }
    
    // endregion

    // region Lifecycle
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        binding = ActivityGamePreferencesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupWindowInsets()
        setupToolbar()
        setupRecyclerViews()
        setupClickListeners()
        
        // Show loading immediately, then load data
        updateUiState(UiState.Loading)
        loadPreferences()
    }
    
    // endregion

    // region Setup Methods
    
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
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }
    
    private fun setupRecyclerViews() {
        binding.recyclerVideoBlacklist.apply {
            layoutManager = LinearLayoutManager(this@GamePreferencesActivity)
            adapter = videoBlacklistAdapter
            itemAnimator?.changeDuration = 150
        }
        
        binding.recyclerAudioBlacklist.apply {
            layoutManager = LinearLayoutManager(this@GamePreferencesActivity)
            adapter = audioBlacklistAdapter
            itemAnimator?.changeDuration = 150
        }
    }
    
    private fun setupClickListeners() {
        with(binding) {
            // NOTE: Add buttons hidden - blacklist is view only to prevent re-download bug
            // btnAddVideo.setOnClickListener { showAddDialog(isVideo = true) }
            // btnAddAudio.setOnClickListener { showAddDialog(isVideo = false) }
            btnReset.setOnClickListener { 
                updateUiState(UiState.Loading)
                loadPreferences() 
            }
            btnApply.setOnClickListener { confirmAndSave() }
        }
    }
    
    // endregion

    // region State Management
    
    private fun updateUiState(state: UiState) {
        when (state) {
            is UiState.Loading -> showLoadingState()
            is UiState.Success -> showSuccessState(state.preferences)
            is UiState.Error -> showErrorState(state.message)
            is UiState.Saving -> showSavingState()
        }
    }
    
    private fun showLoadingState() {
        with(binding) {
            progressIndicator.show()
            loadingOverlay.isVisible = true
            contentContainer.alpha = 0.5f
            setInteractionEnabled(false)
        }
    }
    
    private fun showSuccessState(prefs: GamePreferences) {
        with(binding) {
            progressIndicator.hide()
            loadingOverlay.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction { 
                    loadingOverlay.isVisible = false
                    loadingOverlay.alpha = 1f
                }
                .start()
            contentContainer.animate()
                .alpha(1f)
                .setDuration(200)
                .start()
            setInteractionEnabled(true)
        }
        populateUI(prefs)
    }
    
    private fun showErrorState(message: String) {
        with(binding) {
            progressIndicator.hide()
            loadingOverlay.isVisible = false
            contentContainer.alpha = 1f
            setInteractionEnabled(true)
        }
        showSnackbar(message, isError = true)
    }
    
    private fun showSavingState() {
        with(binding) {
            progressIndicator.show()
            setInteractionEnabled(false)
        }
    }
    
    private fun setInteractionEnabled(enabled: Boolean) {
        with(binding) {
            btnApply.isEnabled = enabled
            btnReset.isEnabled = enabled
            btnAddVideo.isEnabled = enabled
            btnAddAudio.isEnabled = enabled
            chipGroupTextLanguage.isEnabled = enabled
            chipGroupAudioLanguage.isEnabled = enabled
        }
    }
    
    // endregion

    // region Data Loading
    
    private fun loadPreferences() {
        lifecycleScope.launch {
            // Small delay to ensure smooth UI transition
            delay(LOADING_DELAY_MS)
            
            val result = withContext(Dispatchers.IO) {
                runCatching { gameManager.readGamePreferences() }
            }
            
            result.fold(
                onSuccess = { prefs ->
                    if (prefs != null) {
                        currentPreferences = prefs
                        updateUiState(UiState.Success(prefs))
                    } else {
                        updateUiState(UiState.Error(getString(R.string.preferences_load_failed)))
                    }
                },
                onFailure = { e ->
                    updateUiState(UiState.Error(e.message ?: getString(R.string.preferences_load_failed)))
                }
            )
        }
    }
    
    private fun populateUI(prefs: GamePreferences) {
        // User Info - menggunakan extension function
        binding.tvUserId.text = prefs.lastUserId?.toString() ?: getString(R.string.not_available)
        binding.tvServerName.text = prefs.lastServerName?.let { 
            GamePreferences.ServerRegion.fromServerName(it)?.displayName ?: it 
        } ?: getString(R.string.not_available)
        
        binding.chipElfOrderHint.text = getString(
            if (prefs.elfOrderNeedShowNewHint == true) R.string.shown else R.string.hidden
        )
        
        // Languages - menggunakan extension
        TEXT_LANGUAGE_CHIPS.findKeyByValue(prefs.textLanguage)?.let {
            binding.chipGroupTextLanguage.check(it)
        }
        
        AUDIO_LANGUAGE_CHIPS.findKeyByValue(prefs.audioLanguage)?.let {
            binding.chipGroupAudioLanguage.check(it)
        }
        
        // Blacklists - batch update
        videoBlacklist.apply {
            clear()
            addAll(prefs.videoBlacklist)
        }
        updateVideoBlacklistUI()
        
        audioBlacklist.apply {
            clear()
            addAll(prefs.audioBlacklist)
        }
        updateAudioBlacklistUI()
    }
    
    // endregion

    // region Blacklist Management
    
    private fun updateVideoBlacklistUI() {
        videoBlacklistAdapter.submitList(videoBlacklist.toList())
        binding.tvVideoBlacklistCount.text = getString(R.string.videos_blocked, videoBlacklist.size)
    }
    
    private fun updateAudioBlacklistUI() {
        audioBlacklistAdapter.submitList(audioBlacklist.toList())
        binding.tvAudioBlacklistCount.text = getString(R.string.audio_files_blocked, audioBlacklist.size)
    }
    
    private fun showAddDialog(isVideo: Boolean) {
        val input = EditText(this).apply {
            hint = getString(if (isVideo) R.string.video_file_hint else R.string.audio_file_hint)
            setPadding(48, 32, 48, 32)
        }
        
        MaterialAlertDialogBuilder(this)
            .setTitle(if (isVideo) R.string.add_video_file else R.string.add_audio_file)
            .setView(input)
            .setPositiveButton(R.string.add) { _, _ ->
                val fileName = input.text.toString().trim()
                if (fileName.isNotEmpty()) {
                    addToBlacklist(fileName, isVideo)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun addToBlacklist(fileName: String, isVideo: Boolean) {
        val targetList = if (isVideo) videoBlacklist else audioBlacklist
        
        if (targetList.contains(fileName)) {
            showSnackbar(R.string.blacklist_item_exists)
            return
        }
        
        targetList.add(fileName)
        if (isVideo) updateVideoBlacklistUI() else updateAudioBlacklistUI()
        showSnackbar(R.string.blacklist_item_added)
    }
    
    // endregion

    // region Save Operations
    
    private fun confirmAndSave() {
        val selectedTextLang = binding.chipGroupTextLanguage.checkedChipId
            .takeIf { it != -1 }
            ?.let { TEXT_LANGUAGE_CHIPS[it] }
            ?: currentPreferences?.textLanguage
            ?: 2
        
        val selectedAudioLang = binding.chipGroupAudioLanguage.checkedChipId
            .takeIf { it != -1 }
            ?.let { AUDIO_LANGUAGE_CHIPS[it] }
            ?: currentPreferences?.audioLanguage
            ?: 2
        
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.apply_settings)
            .setMessage(R.string.apply_settings_message)
            .setPositiveButton(R.string.apply) { _, _ ->
                performSave(selectedTextLang, selectedAudioLang)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun performSave(textLanguage: Int, audioLanguage: Int) {
        lifecycleScope.launch {
            updateUiState(UiState.Saving)
            
            val result = withContext(Dispatchers.IO) {
                // NOTE: Only save language settings
                // Blacklists are VIEW ONLY - editing will cause game to re-download data!
                val langSuccess = gameManager.writeLanguageSettings(textLanguage, audioLanguage)
                
                if (langSuccess) SaveResult.Success else SaveResult.Failure(listOf("Language"))
            }
            
            binding.progressIndicator.hide()
            setInteractionEnabled(true)
            
            when (result) {
                is SaveResult.Success -> {
                    Snackbar.make(binding.root, R.string.preferences_saved, Snackbar.LENGTH_LONG)
                        .setAnchorView(binding.bottomActionBar)
                        .setAction(R.string.kill_game) { killGame() }
                        .show()
                }
                is SaveResult.Failure -> {
                    showSnackbar(
                        getString(R.string.preferences_save_failed) + ": ${result.failures.joinToString()}",
                        isError = true
                    )
                }
            }
        }
    }
    
    private fun buildSaveResult(
        langSuccess: Boolean,
        videoSuccess: Boolean,
        audioSuccess: Boolean
    ): SaveResult {
        val failures = buildList {
            if (!langSuccess) add("Language")
            if (!videoSuccess) add("Video Blacklist")
            if (!audioSuccess) add("Audio Blacklist")
        }
        
        return if (failures.isEmpty()) SaveResult.Success else SaveResult.Failure(failures)
    }
    
    private fun killGame() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { gameManager.killGame() }
            showSnackbar(R.string.game_killed)
        }
    }
    
    // endregion

    // region Utility Extensions
    
    private fun showSnackbar(messageRes: Int, isError: Boolean = false) {
        showSnackbar(getString(messageRes), isError)
    }
    
    private fun showSnackbar(message: String, isError: Boolean = false) {
        Snackbar.make(binding.root, message, if (isError) Snackbar.LENGTH_LONG else Snackbar.LENGTH_SHORT)
            .setAnchorView(binding.bottomActionBar)
            .show()
    }
    
    private fun <K, V> Map<K, V>.findKeyByValue(value: V): K? = entries.find { it.value == value }?.key
    
    private inline fun ViewPropertyAnimator.withEndAction(crossinline action: () -> Unit): ViewPropertyAnimator {
        return this.withEndAction { action() }
    }
    
    // endregion
}
