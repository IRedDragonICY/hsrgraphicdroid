package com.ireddragonicy.hsrgraphicdroid.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

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
        fun toLocaleList(language: AppLanguage): LocaleListCompat {
            return if (language == SYSTEM) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(language.tag)
            }
        }
    }
}

