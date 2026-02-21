package com.ireddragonicy.hsrgraphicdroid.data

class GamePrefsChangeManager : BaseChangeManager<GamePreferences>() {

    private val propertyMap = mapOf(
        "textLanguage" to "Text Language",
        "audioLanguage" to "Audio Language",
        "videoBlacklist" to "Video Blacklist",
        "audioBlacklist" to "Audio Blacklist",
        "isSaveBattleSpeed" to "Save Battle Speed",
        "autoBattleOpen" to "Auto Battle Open",
        "needDownloadAllAssets" to "Download All Assets",
        "forceUpdateVideo" to "Force Update Video",
        "forceUpdateAudio" to "Force Update Audio",
        "showSimplifiedSkillDesc" to "Show Simplified Skill Desc",
        "gridFightSeenSeasonTalentTree" to "Season Talent Tree",
        "rogueTournEnableGodMode" to "Rogue Tourn God Mode"
    )

    private val valueFormatter: (String, Any?) -> String = { fieldName, value ->
        when (fieldName) {
            "textLanguage" -> GamePreferences.getTextLanguageName(value as? Int ?: 0)
            "audioLanguage" -> GamePreferences.getAudioLanguageName(value as? Int ?: 0)
            else -> value?.toString() ?: "null"
        }
    }

    override fun copySettings(settings: GamePreferences): GamePreferences {
        return settings.copy()
    }

    override fun detectExternalChanges(gameSettings: GamePreferences, localSettings: GamePreferences): List<SettingChange> {
        val changes = diffSettings(gameSettings, localSettings, propertyMap, valueFormatter)
        _externalChanges.value = changes
        return changes
    }

    override fun getModifiedFieldsDetails(currentSettings: GamePreferences): List<SettingChange> {
        val baseline = baselineSettings ?: return emptyList()
        return diffSettings(baseline, currentSettings, propertyMap, valueFormatter)
    }

    override fun updateModifiedFields(currentSettings: GamePreferences) {
        val baseline = baselineSettings ?: return
        
        val modified = diffSettings(baseline, currentSettings, propertyMap, valueFormatter)
            .map { change ->
                propertyMap.entries.firstOrNull { it.value == change.fieldName }?.key ?: change.fieldName
            }.toSet()
        
        _modifiedFields.value = modified
        _pendingChangesCount.value = modified.size
    }
}
