package com.ireddragonicy.hsrgraphicdroid.utils

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart

class PreferenceManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    companion object {
        private const val THEME_KEY = "theme"
        private const val LANGUAGE_KEY = "language"
    }

    suspend fun setTheme(theme: String) {
        prefs.edit().putString(THEME_KEY, theme).apply()
    }

    fun getTheme(): Flow<String> = observeKey(THEME_KEY, "system")

    suspend fun setLanguage(language: String) {
        prefs.edit().putString(LANGUAGE_KEY, language).apply()
    }

    fun getLanguage(): Flow<String> = observeKey(LANGUAGE_KEY, "system")

    /**
     * Observe a SharedPreferences key as a Flow.
     * Emits the current value immediately, then emits on every change.
     */
    private fun observeKey(key: String, default: String): Flow<String> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == key) {
                trySend(prefs.getString(key, default) ?: default)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.onStart {
        emit(prefs.getString(key, default) ?: default)
    }.distinctUntilChanged()
}
