package com.ireddragonicy.hsrgraphicdroid.data

import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Data class representing additional game settings stored in SharedPreferences
 * (Language, Audio, User info, Blacklists, Elf Order hint)
 */
data class GamePreferences(
    // User Info
    var lastUserId: Int? = null,
    var lastServerName: String? = null,
    
    // Language Settings (integer codes as stored in game)
    var textLanguage: Int = 2,  // Default: English
    var audioLanguage: Int = 2, // Default: Japanese
    
    // Elf Order hint shown
    var elfOrderNeedShowNewHint: Boolean? = null,
    
    // Blacklists (for skipping cutscenes/audio)
    var videoBlacklist: List<String> = emptyList(),
    var audioBlacklist: List<String> = emptyList(),
    
    // Quality of Life (QoL)
    var isSaveBattleSpeed: Int? = null,
    var autoBattleOpen: Int? = null,
    
    // Updates & Downloads
    var needDownloadAllAssets: Int? = null,
    var forceUpdateVideo: Int? = null,
    var forceUpdateAudio: Int? = null,
    
    // UID Specific Settings
    var showSimplifiedSkillDesc: Int? = null,
    var gridFightSeenSeasonTalentTree: Int? = null,
    var rogueTournEnableGodMode: Int? = null
) {
    companion object {
        /**
         * Text language codes used by the game
         * Integer value -> Language name
         */
        val TEXT_LANGUAGES = mapOf(
            0 to "简体中文",       // Simplified Chinese
            1 to "繁體中文",       // Traditional Chinese
            2 to "English",
            3 to "日本語",        // Japanese
            4 to "한국어",        // Korean
            5 to "Deutsch",       // German
            6 to "Español",       // Spanish
            7 to "Français",      // French
            8 to "Indonesia",
            9 to "Русский",       // Russian
            10 to "Português",    // Portuguese
            11 to "ไทย",          // Thai
            12 to "Tiếng Việt"    // Vietnamese
        )
        
        /**
         * Audio language codes used by the game
         */
        val AUDIO_LANGUAGES = mapOf(
            0 to "中文",      // Chinese
            1 to "English",
            2 to "日本語",    // Japanese
            3 to "한국어"     // Korean
        )

        // Mapping for XML string codes
        val TEXT_LANGUAGE_CODES = mapOf(
            0 to "cn",
            1 to "cht",
            2 to "en",
            3 to "jp",
            4 to "kr",
            5 to "de",
            6 to "es",
            7 to "fr",
            8 to "id",
            9 to "ru",
            10 to "pt",
            11 to "th",
            12 to "vi"
        )

        val AUDIO_LANGUAGE_CODES = mapOf(
            0 to "cn",
            1 to "en",
            2 to "jp",
            3 to "kr"
        )
        
        fun getTextLanguageName(code: Int): String =
            TEXT_LANGUAGES[code] ?: "Unknown"
        
        fun getAudioLanguageName(code: Int): String =
            AUDIO_LANGUAGES[code] ?: "Unknown"

        fun getCodeFromTextLanguage(language: Int): String = TEXT_LANGUAGE_CODES[language] ?: "en"
        fun getCodeFromAudioLanguage(language: Int): String = AUDIO_LANGUAGE_CODES[language] ?: "jp"
        
        fun getTextLanguageFromCode(code: String): Int = 
            TEXT_LANGUAGE_CODES.entries.find { it.value == code }?.key ?: 2
            
        fun getAudioLanguageFromCode(code: String): Int = 
            AUDIO_LANGUAGE_CODES.entries.find { it.value == code }?.key ?: 2
            
        /**
         * Parse URL-encoded JSON array string to List<String>
         * Example: %5B%22file1.usm%22%2C%22file2.usm%22%5D -> ["file1.usm", "file2.usm"]
         */
        fun parseBlacklistFromEncoded(encoded: String?): List<String> {
            if (encoded.isNullOrBlank()) return emptyList()
            
            return runCatching {
                val decoded = URLDecoder.decode(encoded, "UTF-8")
                // Parse JSON array manually - format: ["item1","item2",...]
                decoded.trim('[', ']')
                    .split(",")
                    .map { it.trim().trim('"') }
                    .filter { it.isNotBlank() }
            }.getOrDefault(emptyList())
        }
        
        /**
         * Encode List<String> to URL-encoded JSON array string
         */
        fun encodeBlacklistToString(list: List<String>): String {
            if (list.isEmpty()) return URLEncoder.encode("[]", "UTF-8")
            
            val jsonArray = list.joinToString(",", prefix = "[", postfix = "]") { "\"$it\"" }
            return URLEncoder.encode(jsonArray, "UTF-8")
        }
    }
    
    /**
     * Supported server regions
     */
    enum class ServerRegion(val serverName: String, val displayName: String) {
        PROD_GF_SG("prod_gf_sg", "SEA/Global"),
        PROD_GF_US("prod_gf_us", "America"),
        PROD_GF_EU("prod_gf_eu", "Europe"),
        PROD_GF_JP("prod_gf_jp", "Japan"),
        PROD_GF_KR("prod_gf_kr", "Korea"),
        PROD_OFFICIAL_ASIA("prod_official_asia", "Asia"),
        PROD_OFFICIAL_USA("prod_official_usa", "America"),
        PROD_OFFICIAL_EURO("prod_official_euro", "Europe"),
        PROD_OFFICIAL_CHT("prod_official_cht", "TW/HK/MO"),
        PROD_OFFICIAL_CHS("prod_official_chs", "China");
        
        companion object {
            fun fromServerName(serverName: String?): ServerRegion? =
                entries.find { it.serverName == serverName }
        }
    }
}
