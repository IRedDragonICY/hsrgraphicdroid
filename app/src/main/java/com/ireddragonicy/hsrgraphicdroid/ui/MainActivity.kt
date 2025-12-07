package com.ireddragonicy.hsrgraphicdroid.ui

import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.ireddragonicy.hsrgraphicdroid.R
import com.ireddragonicy.hsrgraphicdroid.databinding.ActivityMainBinding
import com.ireddragonicy.hsrgraphicdroid.ui.adapters.MainPagerAdapter
import com.ireddragonicy.hsrgraphicdroid.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private val mainViewModel: MainViewModel by viewModels()
    private lateinit var pagerAdapter: MainPagerAdapter
    private var lastSelectedPosition = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupInsets()
        setupViewPagerWithBottomNav()
        
        lifecycleScope.launch {
            mainViewModel.bootstrapAppearance()
        }
    }

    private fun setupViewPagerWithBottomNav() {
        // Initialize ViewPager adapter
        pagerAdapter = MainPagerAdapter(this)
        
        binding.viewPager.apply {
            adapter = pagerAdapter
            offscreenPageLimit = 2 // Keep adjacent pages in memory for smooth swiping
            
            // Professional WhatsApp-like page transformer with depth effect
            setPageTransformer(DepthPageTransformer())
            
            // Disable overscroll effect for cleaner look
            (getChildAt(0) as? androidx.recyclerview.widget.RecyclerView)?.apply {
                overScrollMode = androidx.recyclerview.widget.RecyclerView.OVER_SCROLL_NEVER
                // Smoother scrolling
                isNestedScrollingEnabled = true
            }
        }
        
        // Sync ViewPager with BottomNavigation
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                
                // Haptic feedback when changing pages (like WhatsApp)
                if (position != lastSelectedPosition) {
                    performHapticFeedback()
                    lastSelectedPosition = position
                }
                
                // Update bottom nav selection when page changes via swipe
                val menuItem = when (position) {
                    MainPagerAdapter.PAGE_HOME -> R.id.homeFragment
                    MainPagerAdapter.PAGE_GRAPHICS -> R.id.graphicsFragment
                    MainPagerAdapter.PAGE_GAME_PREFS -> R.id.gamePrefsFragment
                    MainPagerAdapter.PAGE_SETTINGS -> R.id.settingsFragment
                    else -> R.id.homeFragment
                }
                if (binding.bottomNav.selectedItemId != menuItem) {
                    binding.bottomNav.selectedItemId = menuItem
                }
            }
        })
        
        // Handle bottom nav item selection
        binding.bottomNav.setOnItemSelectedListener { menuItem ->
            val position = when (menuItem.itemId) {
                R.id.homeFragment -> MainPagerAdapter.PAGE_HOME
                R.id.graphicsFragment -> MainPagerAdapter.PAGE_GRAPHICS
                R.id.gamePrefsFragment -> MainPagerAdapter.PAGE_GAME_PREFS
                R.id.settingsFragment -> MainPagerAdapter.PAGE_SETTINGS
                else -> MainPagerAdapter.PAGE_HOME
            }
            // Use smooth scroll for professional feel
            binding.viewPager.setCurrentItem(position, true)
            true
        }
        
        // Prevent reselection from doing anything (cleaner UX)
        binding.bottomNav.setOnItemReselectedListener { 
            // Scroll to top or refresh could be implemented here
        }
    }
    
    /**
     * Provides subtle haptic feedback for page changes
     * Similar to WhatsApp's tab switching experience
     * Uses View haptic feedback which doesn't require VIBRATE permission
     */
    private fun performHapticFeedback() {
        try {
            // Use View's built-in haptic feedback - doesn't require VIBRATE permission
            binding.viewPager.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
        } catch (e: Exception) {
            // Silently ignore if haptic feedback fails
        }
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNav) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(bottom = systemBars.bottom)
            insets
        }
    }
    
    /**
     * Professional WhatsApp-like page transformer with depth effect
     * Creates an immersive sliding experience with parallax and depth
     */
    private inner class DepthPageTransformer : ViewPager2.PageTransformer {
        private val minScale = 0.85f

        override fun transformPage(page: android.view.View, position: Float) {
            page.apply {
                val pageWidth = width
                when {
                    position < -1 -> { // Page is way off-screen to the left
                        alpha = 0f
                    }
                    position <= 0 -> { // Page is sliding out to the left or at center
                        alpha = 1f
                        translationX = 0f
                        translationZ = 0f
                        scaleX = 1f
                        scaleY = 1f
                    }
                    position <= 1 -> { // Page is sliding in from the right
                        // Fade out as it slides behind
                        alpha = 1f - position
                        
                        // Counteract the default slide transition
                        translationX = pageWidth * -position
                        
                        // Move it behind the left page
                        translationZ = -1f
                        
                        // Scale down the page
                        val scaleFactor = minScale + (1 - minScale) * (1 - abs(position))
                        scaleX = scaleFactor
                        scaleY = scaleFactor
                    }
                    else -> { // Page is way off-screen to the right
                        alpha = 0f
                    }
                }
            }
        }
    }
}
