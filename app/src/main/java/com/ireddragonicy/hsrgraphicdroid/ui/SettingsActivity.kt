package com.ireddragonicy.hsrgraphicdroid.ui

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityOptionsCompat
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.ireddragonicy.hsrgraphicdroid.R
import com.ireddragonicy.hsrgraphicdroid.databinding.ActivitySettingsBinding
import com.ireddragonicy.hsrgraphicdroid.utils.PreferenceManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefManager: PreferenceManager
    
    private var selectedTheme = "system"
    private var selectedLanguage = "system"
    private var isLanguageChanging = false
    private var isThemeChanging = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Fade in animation saat activity dibuka
        if (savedInstanceState == null) {
            binding.root.alpha = 0f
            binding.root.animate()
                .alpha(1f)
                .setDuration(150)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }
        
        // Handle window insets for AppBarLayout
        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, insets.top, 0, 0)
            windowInsets
        }
        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        prefManager = PreferenceManager(this)
        
        setupSettings()
    }
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        
        if (isLanguageChanging) {
            // Recreate dengan smooth transition menggunakan modern API
            finish()
            startActivity(intent)
            
            // Modern transition API
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                overrideActivityTransition(
                    OVERRIDE_TRANSITION_OPEN,
                    R.anim.fade_in_smooth,
                    R.anim.fade_out_smooth
                )
            } else {
                @Suppress("DEPRECATION")
                overridePendingTransition(R.anim.fade_in_smooth, R.anim.fade_out_smooth)
            }
            
            isLanguageChanging = false
        } else if (isThemeChanging) {
            // Recreate dengan smooth fade untuk theme change
            finish()
            startActivity(intent)
            
            // Modern transition API
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                overrideActivityTransition(
                    OVERRIDE_TRANSITION_OPEN,
                    R.anim.fade_in_smooth,
                    R.anim.fade_out_smooth
                )
            } else {
                @Suppress("DEPRECATION")
                overridePendingTransition(R.anim.fade_in_smooth, R.anim.fade_out_smooth)
            }
            
            isThemeChanging = false
        }
    }
    
    private fun setupSettings() {
        // Load current theme
        lifecycleScope.launch {
            prefManager.getTheme().collect { theme ->
                selectedTheme = theme
                updateThemeSelection(theme)
            }
        }
        
        // Load current language
        lifecycleScope.launch {
            prefManager.getLanguage().collect { language ->
                selectedLanguage = language
                updateLanguageSelection(language)
            }
        }
        
        // Theme card click listeners dengan ripple animation
        setupThemeClickListeners()
        
        // Language card click listeners dengan smooth transition
        setupLanguageClickListeners()
    }
    
    private fun setupThemeClickListeners() {
        binding.cardThemeSystem.setOnClickListener {
            animateCardSelection(it) {
                selectTheme("system")
            }
        }
        
        binding.cardThemeLight.setOnClickListener {
            animateCardSelection(it) {
                selectTheme("light")
            }
        }
        
        binding.cardThemeDark.setOnClickListener {
            animateCardSelection(it) {
                selectTheme("dark")
            }
        }
    }
    
    private fun setupLanguageClickListeners() {
        binding.cardLangSystem.setOnClickListener {
            animateCardSelection(it) {
                selectLanguage("system")
            }
        }
        
        binding.cardLangEnglish.setOnClickListener {
            animateCardSelection(it) {
                selectLanguage("en")
            }
        }
        
        binding.cardLangIndonesian.setOnClickListener {
            animateCardSelection(it) {
                selectLanguage("id")
            }
        }
        
        binding.cardLangChinese.setOnClickListener {
            animateCardSelection(it) {
                selectLanguage("zh")
            }
        }
        
        binding.cardLangJapanese.setOnClickListener {
            animateCardSelection(it) {
                selectLanguage("ja")
            }
        }
    }
    
    private fun animateCardSelection(view: View, action: () -> Unit) {
        // Subtle scale animation untuk feedback - dipercepat
        view.animate()
            .scaleX(0.98f)
            .scaleY(0.98f)
            .setDuration(50)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(50)
                    .withEndAction {
                        action()
                    }
                    .start()
            }
            .start()
    }
    
    private fun selectTheme(theme: String) {
        if (selectedTheme == theme) return // Skip jika sudah sama
        
        selectedTheme = theme
        updateThemeSelection(theme)
        
        lifecycleScope.launch {
            prefManager.setTheme(theme)
            
            // Set flag untuk onConfigurationChanged
            isThemeChanging = true
            
            // Smooth fade out animation untuk theme change
            binding.root.animate()
                .alpha(0f)
                .setDuration(100)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    // Apply theme change
                    applyTheme(theme)
                    
                    // Modern transition API untuk smooth recreate
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        overrideActivityTransition(
                            OVERRIDE_TRANSITION_CLOSE,
                            R.anim.fade_in_smooth,
                            R.anim.fade_out_smooth
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        overridePendingTransition(R.anim.fade_in_smooth, R.anim.fade_out_smooth)
                    }
                }
                .start()
        }
    }
    
    private fun selectLanguage(language: String) {
        if (selectedLanguage == language) return // Skip jika sudah sama
        
        selectedLanguage = language
        updateLanguageSelection(language)
        
        lifecycleScope.launch {
            prefManager.setLanguage(language)
            
            // Set flag untuk onConfigurationChanged
            isLanguageChanging = true
            
            // Smooth fade out animation - dipercepat
            binding.root.animate()
                .alpha(0f)
                .setDuration(100)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    // Apply language change
                    applyLanguage(language)
                    
                    // Modern transition API untuk smooth recreate
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        overrideActivityTransition(
                            OVERRIDE_TRANSITION_CLOSE,
                            R.anim.fade_in_smooth,
                            R.anim.fade_out_smooth
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        overridePendingTransition(R.anim.fade_in_smooth, R.anim.fade_out_smooth)
                    }
                }
                .start()
        }
    }
    
    private fun updateThemeSelection(theme: String) {
        // Reset all
        resetCardSelection(binding.cardThemeSystem, binding.checkThemeSystem)
        resetCardSelection(binding.cardThemeLight, binding.checkThemeLight)
        resetCardSelection(binding.cardThemeDark, binding.checkThemeDark)
        
        // Select current
        when (theme) {
            "light" -> setCardSelected(binding.cardThemeLight, binding.checkThemeLight)
            "dark" -> setCardSelected(binding.cardThemeDark, binding.checkThemeDark)
            else -> setCardSelected(binding.cardThemeSystem, binding.checkThemeSystem)
        }
    }
    
    private fun updateLanguageSelection(language: String) {
        // Reset all
        resetCardSelection(binding.cardLangSystem, binding.checkLangSystem)
        resetCardSelection(binding.cardLangEnglish, binding.checkLangEnglish)
        resetCardSelection(binding.cardLangIndonesian, binding.checkLangIndonesian)
        resetCardSelection(binding.cardLangChinese, binding.checkLangChinese)
        resetCardSelection(binding.cardLangJapanese, binding.checkLangJapanese)
        
        // Select current
        when (language) {
            "en" -> setCardSelected(binding.cardLangEnglish, binding.checkLangEnglish)
            "id" -> setCardSelected(binding.cardLangIndonesian, binding.checkLangIndonesian)
            "zh" -> setCardSelected(binding.cardLangChinese, binding.checkLangChinese)
            "ja" -> setCardSelected(binding.cardLangJapanese, binding.checkLangJapanese)
            else -> setCardSelected(binding.cardLangSystem, binding.checkLangSystem)
        }
    }
    
    private fun setCardSelected(card: MaterialCardView, check: View) {
        card.strokeWidth = dpToPx(3)
        card.strokeColor = getColorFromAttr(com.google.android.material.R.attr.colorPrimary)
        check.visibility = View.VISIBLE
    }
    
    private fun resetCardSelection(card: MaterialCardView, check: View) {
        card.strokeWidth = dpToPx(2)
        card.strokeColor = getColorFromAttr(com.google.android.material.R.attr.colorOutline)
        check.visibility = View.GONE
    }
    
    private fun getColorFromAttr(attr: Int): Int {
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }
    
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
    
    private fun applyTheme(theme: String) {
        when (theme) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
    
    private fun applyLanguage(language: String) {
        val locale = when (language) {
            "en" -> Locale("en")
            "id" -> Locale("id")
            "zh" -> Locale("zh")
            "ja" -> Locale("ja")
            else -> {
                // System default - clear app locale
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                return
            }
        }
        
        val localeList = LocaleListCompat.create(locale)
        AppCompatDelegate.setApplicationLocales(localeList)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
