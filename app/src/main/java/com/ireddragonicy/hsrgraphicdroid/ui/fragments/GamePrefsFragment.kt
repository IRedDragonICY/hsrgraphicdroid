package com.ireddragonicy.hsrgraphicdroid.ui.fragments

import android.os.Bundle
import android.view.View
import android.view.ViewPropertyAnimator
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.ireddragonicy.hsrgraphicdroid.R
import com.ireddragonicy.hsrgraphicdroid.data.GamePreferences
import com.ireddragonicy.hsrgraphicdroid.databinding.FragmentGamePrefsBinding
import com.ireddragonicy.hsrgraphicdroid.ui.BlacklistAdapter
import com.ireddragonicy.hsrgraphicdroid.ui.base.BaseFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GamePrefsFragment :
    BaseFragment<FragmentGamePrefsBinding>(FragmentGamePrefsBinding::inflate) {

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

    private val videoBlacklistAdapter by lazy {
        BlacklistAdapter(isVideo = true, onDelete = null)
    }
    private val audioBlacklistAdapter by lazy {
        BlacklistAdapter(isVideo = false, onDelete = null)
    }

    private var currentPreferences: GamePreferences? = null
    private val videoBlacklist = mutableListOf<String>()
    private val audioBlacklist = mutableListOf<String>()

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.appBarLayout.applyTopInsetPadding()
        binding.bottomActionBar.applyBottomInsetPadding()
        setupToolbar()
        setupRecyclerViews()
        setupClickListeners()

        updateUiState(UiState.Loading)
        loadPreferences()
    }

    private fun setupToolbar() {
        binding.toolbar.title = getString(R.string.game_preferences)
        // No navigation back button needed - this is now a tab in ViewPager
    }

    private fun setupRecyclerViews() {
        binding.recyclerVideoBlacklist.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = videoBlacklistAdapter
            itemAnimator?.changeDuration = 150
        }
        binding.recyclerAudioBlacklist.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = audioBlacklistAdapter
            itemAnimator?.changeDuration = 150
        }
    }

    private fun setupClickListeners() {
        binding.btnReset.setOnClickListener {
            updateUiState(UiState.Loading)
            loadPreferences()
        }
        binding.btnApply.setOnClickListener { confirmAndSave() }
    }

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

    private fun loadPreferences() {
        viewLifecycleOwner.lifecycleScope.launch {
            delay(LOADING_DELAY_MS)
            val result = withContext(Dispatchers.IO) {
                runCatching { mainViewModel.readGamePreferences() }
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
        binding.tvUserId.text = prefs.lastUserId?.toString() ?: getString(R.string.not_available)
        binding.tvServerName.text = prefs.lastServerName?.let {
            GamePreferences.ServerRegion.fromServerName(it)?.displayName ?: it
        } ?: getString(R.string.not_available)

        binding.chipElfOrderHint.text = getString(
            if (prefs.elfOrderNeedShowNewHint == true) R.string.shown else R.string.hidden
        )

        TEXT_LANGUAGE_CHIPS.findKeyByValue(prefs.textLanguage)?.let {
            binding.chipGroupTextLanguage.check(it)
        }
        AUDIO_LANGUAGE_CHIPS.findKeyByValue(prefs.audioLanguage)?.let {
            binding.chipGroupAudioLanguage.check(it)
        }

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

    private fun updateVideoBlacklistUI() {
        videoBlacklistAdapter.submitList(videoBlacklist.toList())
        binding.tvVideoBlacklistCount.text = getString(R.string.videos_blocked, videoBlacklist.size)
    }

    private fun updateAudioBlacklistUI() {
        audioBlacklistAdapter.submitList(audioBlacklist.toList())
        binding.tvAudioBlacklistCount.text = getString(R.string.audio_files_blocked, audioBlacklist.size)
    }

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

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.apply_settings)
            .setMessage(R.string.apply_settings_message)
            .setPositiveButton(R.string.apply) { _, _ ->
                performSave(selectedTextLang, selectedAudioLang)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun performSave(textLanguage: Int, audioLanguage: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            updateUiState(UiState.Saving)

            val result = withContext(Dispatchers.IO) {
                val langSuccess = mainViewModel.writeLanguageSettings(textLanguage, audioLanguage)
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

    private fun killGame() {
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) { mainViewModel.killGame() }
            showSnackbar(R.string.game_killed)
        }
    }

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
}

