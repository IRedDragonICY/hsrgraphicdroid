package com.ireddragonicy.hsrgraphicdroid.ui

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.ireddragonicy.hsrgraphicdroid.databinding.ActivitySettingsBinding
import com.ireddragonicy.hsrgraphicdroid.utils.PreferenceManager
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefManager: PreferenceManager

    // Enums for better type safety and cleaner code
    enum class AppTheme(val mode: Int, val key: String) {
        SYSTEM(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, "system"),
        LIGHT(AppCompatDelegate.MODE_NIGHT_NO, "light"),
        DARK(AppCompatDelegate.MODE_NIGHT_YES, "dark");

        companion object {
            fun fromKey(key: String) = entries.find { it.key == key } ?: SYSTEM
        }
    }

    enum class AppLanguage(val tag: String, val key: String) {
        SYSTEM("", "system"),
        ENGLISH("en", "en"),
        INDONESIAN("id", "id"),
        CHINESE("zh", "zh"),
        RUSSIAN("ru", "ru"),
        JAPANESE("ja", "ja");

        companion object {
            fun fromKey(key: String) = entries.find { it.key == key } ?: SYSTEM
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupWindowInsets()
        setupToolbar()
        
        prefManager = PreferenceManager(this)
        
        setupClickListeners()
        observePreferences()
    }
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Manually recreate activity to apply changes instantly without black flash
        val intent = intent
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        finish()
        overridePendingTransition(0, 0)
        startActivity(intent)
        overridePendingTransition(0, 0)
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

    private fun setupClickListeners() {
        // Theme
        binding.cardThemeSystem.setOnClickListener { setTheme(AppTheme.SYSTEM) }
        binding.cardThemeLight.setOnClickListener { setTheme(AppTheme.LIGHT) }
        binding.cardThemeDark.setOnClickListener { setTheme(AppTheme.DARK) }

        // Language
        binding.cardLangSystem.setOnClickListener { setLanguage(AppLanguage.SYSTEM) }
        binding.cardLangEnglish.setOnClickListener { setLanguage(AppLanguage.ENGLISH) }
        binding.cardLangIndonesian.setOnClickListener { setLanguage(AppLanguage.INDONESIAN) }
        binding.cardLangChinese.setOnClickListener { setLanguage(AppLanguage.CHINESE) }
        binding.cardLangRussian.setOnClickListener { setLanguage(AppLanguage.RUSSIAN) }
        binding.cardLangJapanese.setOnClickListener { setLanguage(AppLanguage.JAPANESE) }
    }

    private fun observePreferences() {
        lifecycleScope.launch {
            prefManager.getTheme().collect { key ->
                updateThemeUI(AppTheme.fromKey(key))
            }
        }

        lifecycleScope.launch {
            prefManager.getLanguage().collect { key ->
                updateLanguageUI(AppLanguage.fromKey(key))
            }
        }
    }

    private fun setTheme(theme: AppTheme) {
        // Save preference safely ensuring it completes even if activity is destroyed
        lifecycleScope.launch {
            withContext(NonCancellable) {
                prefManager.setTheme(theme.key)
            }
        }
        // Apply immediately - this will trigger recreation
        AppCompatDelegate.setDefaultNightMode(theme.mode)
    }

    private fun setLanguage(language: AppLanguage) {
        // Save preference safely
        lifecycleScope.launch {
            withContext(NonCancellable) {
                prefManager.setLanguage(language.key)
            }
        }
        
        // Apply immediately
        val localeList = if (language == AppLanguage.SYSTEM) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(language.tag)
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    private fun updateThemeUI(selectedTheme: AppTheme) {
        val cards = mapOf(
            AppTheme.SYSTEM to (binding.cardThemeSystem to binding.checkThemeSystem),
            AppTheme.LIGHT to (binding.cardThemeLight to binding.checkThemeLight),
            AppTheme.DARK to (binding.cardThemeDark to binding.checkThemeDark)
        )

        cards.forEach { (theme, views) ->
            val (card, check) = views
            val isSelected = theme == selectedTheme
            updateCardState(card, check, isSelected)
        }
    }

    private fun updateLanguageUI(selectedLanguage: AppLanguage) {
        val cards = mapOf(
            AppLanguage.SYSTEM to (binding.cardLangSystem to binding.checkLangSystem),
            AppLanguage.ENGLISH to (binding.cardLangEnglish to binding.checkLangEnglish),
            AppLanguage.INDONESIAN to (binding.cardLangIndonesian to binding.checkLangIndonesian),
            AppLanguage.CHINESE to (binding.cardLangChinese to binding.checkLangChinese),
            AppLanguage.RUSSIAN to (binding.cardLangRussian to binding.checkLangRussian),
            AppLanguage.JAPANESE to (binding.cardLangJapanese to binding.checkLangJapanese)
        )

        cards.forEach { (lang, views) ->
            val (card, check) = views
            val isSelected = lang == selectedLanguage
            updateCardState(card, check, isSelected)
        }
    }

    private fun updateCardState(card: MaterialCardView, check: View, isSelected: Boolean) {
        card.strokeWidth = dpToPx(if (isSelected) 3 else 2)
        card.strokeColor = getColorFromAttr(
            if (isSelected) com.google.android.material.R.attr.colorPrimary 
            else com.google.android.material.R.attr.colorOutline
        )
        check.visibility = if (isSelected) View.VISIBLE else View.GONE
    }

    private fun getColorFromAttr(attr: Int): Int {
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
