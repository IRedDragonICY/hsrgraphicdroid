package com.ireddragonicy.hsrgraphicdroid.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ireddragonicy.hsrgraphicdroid.BuildConfig
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ireddragonicy.hsrgraphicdroid.R
import com.ireddragonicy.hsrgraphicdroid.databinding.FragmentSettingsBinding
import com.ireddragonicy.hsrgraphicdroid.ui.AppLanguage
import com.ireddragonicy.hsrgraphicdroid.ui.AppTheme
import com.ireddragonicy.hsrgraphicdroid.ui.adapters.SettingId
import com.ireddragonicy.hsrgraphicdroid.ui.adapters.SettingItem
import com.ireddragonicy.hsrgraphicdroid.ui.adapters.SettingType
import com.ireddragonicy.hsrgraphicdroid.ui.adapters.SettingsAdapter
import com.ireddragonicy.hsrgraphicdroid.ui.base.BaseFragment
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class SettingsFragment :
    BaseFragment<FragmentSettingsBinding>(FragmentSettingsBinding::inflate) {

    private val settingsAdapter = SettingsAdapter(
        onItemClick = { item -> handleItemClick(item) },
        onToggleChanged = { _, _ -> }
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.appBarLayout.applyTopInsetPadding()
        binding.recyclerSettings.applyBottomInsetPadding()
        setupToolbar()
        setupRecycler()
        observePreferences()
    }

    private fun setupToolbar() {
        binding.toolbar.title = getString(R.string.settings)
    }

    private fun setupRecycler() {
        binding.recyclerSettings.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSettings.adapter = settingsAdapter
    }

    private fun observePreferences() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(mainViewModel.themeFlow, mainViewModel.languageFlow) { theme, language ->
                    theme to language
                }.collect { (theme, language) ->
                    settingsAdapter.submitList(buildSettingsList(theme, language))
                }
            }
        }
    }

    private fun buildSettingsList(theme: AppTheme, language: AppLanguage): List<SettingItem> {
        return listOf(
            SettingItem(
                id = SettingId.THEME,
                title = getString(R.string.theme),
                summary = themeSummary(theme),
                iconRes = R.drawable.ic_palette,
                type = SettingType.NAVIGATION
            ),
            SettingItem(
                id = SettingId.LANGUAGE,
                title = getString(R.string.language),
                summary = languageSummary(language),
                iconRes = R.drawable.ic_language,
                type = SettingType.NAVIGATION
            ),
            SettingItem(
                id = SettingId.ABOUT,
                title = getString(R.string.about),
                summary = "${getString(R.string.app_name)} v${BuildConfig.VERSION_NAME}",
                iconRes = R.drawable.ic_info,
                type = SettingType.NAVIGATION
            )
        )
    }

    private fun handleItemClick(item: SettingItem) {
        when (item.id) {
            SettingId.THEME -> showThemeDialog()
            SettingId.LANGUAGE -> showLanguageDialog()
            SettingId.ABOUT -> showAboutDialog()
            SettingId.DYNAMIC_COLOR -> Unit
        }
    }

    private fun showThemeDialog() {
        val themes = AppTheme.entries.toTypedArray()
        val labels = themes.map {
            when (it) {
                AppTheme.SYSTEM -> getString(R.string.system_default)
                AppTheme.LIGHT -> getString(R.string.light)
                AppTheme.DARK -> getString(R.string.dark)
            }
        }.toTypedArray()

        val current = themes.indexOf(mainViewModel.themeFlow.value)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.theme)
            .setSingleChoiceItems(labels, current) { dialog, which ->
                mainViewModel.updateTheme(themes[which])
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showLanguageDialog() {
        val languages = AppLanguage.entries.toTypedArray()
        val labels = languages.map {
            when (it) {
                AppLanguage.SYSTEM -> getString(R.string.language_system)
                AppLanguage.ENGLISH -> getString(R.string.language_english)
                AppLanguage.INDONESIAN -> getString(R.string.language_indonesian)
                AppLanguage.CHINESE -> getString(R.string.language_chinese)
                AppLanguage.RUSSIAN -> getString(R.string.language_russian)
                AppLanguage.JAPANESE -> getString(R.string.language_japanese)
            }
        }.toTypedArray()
        val current = languages.indexOf(mainViewModel.languageFlow.value)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.language)
            .setSingleChoiceItems(labels, current) { dialog, which ->
                mainViewModel.updateLanguage(languages[which])
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showAboutDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.about)
            .setMessage(R.string.about_message)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun themeSummary(theme: AppTheme): String = when (theme) {
        AppTheme.SYSTEM -> getString(R.string.system_default)
        AppTheme.LIGHT -> getString(R.string.light)
        AppTheme.DARK -> getString(R.string.dark)
    }

    private fun languageSummary(language: AppLanguage): String = when (language) {
        AppLanguage.SYSTEM -> getString(R.string.language_system)
        AppLanguage.ENGLISH -> getString(R.string.language_english)
        AppLanguage.INDONESIAN -> getString(R.string.language_indonesian)
        AppLanguage.CHINESE -> getString(R.string.language_chinese)
        AppLanguage.RUSSIAN -> getString(R.string.language_russian)
        AppLanguage.JAPANESE -> getString(R.string.language_japanese)
    }
}

