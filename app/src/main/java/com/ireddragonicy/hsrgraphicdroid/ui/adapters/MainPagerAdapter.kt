package com.ireddragonicy.hsrgraphicdroid.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.ireddragonicy.hsrgraphicdroid.ui.fragments.GamePrefsFragment
import com.ireddragonicy.hsrgraphicdroid.ui.fragments.GraphicsFragment
import com.ireddragonicy.hsrgraphicdroid.ui.fragments.HomeFragment
import com.ireddragonicy.hsrgraphicdroid.ui.fragments.SettingsFragment

/**
 * ViewPager2 adapter for main navigation with swipe support
 * Professional implementation like WhatsApp navigation
 */
class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    companion object {
        const val NUM_PAGES = 4
        
        const val PAGE_HOME = 0
        const val PAGE_GRAPHICS = 1
        const val PAGE_GAME_PREFS = 2
        const val PAGE_SETTINGS = 3
    }

    override fun getItemCount(): Int = NUM_PAGES

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            PAGE_HOME -> HomeFragment()
            PAGE_GRAPHICS -> GraphicsFragment()
            PAGE_GAME_PREFS -> GamePrefsFragment()
            PAGE_SETTINGS -> SettingsFragment()
            else -> HomeFragment()
        }
    }
}
