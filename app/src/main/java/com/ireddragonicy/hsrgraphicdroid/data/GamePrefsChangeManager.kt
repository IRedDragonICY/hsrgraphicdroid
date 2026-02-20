package com.ireddragonicy.hsrgraphicdroid.data

class GamePrefsChangeManager : BaseChangeManager<GamePreferences>() {

    override fun copySettings(settings: GamePreferences): GamePreferences {
        return settings.copy()
    }

    override fun detectExternalChanges(gameSettings: GamePreferences, localSettings: GamePreferences): List<SettingChange> {
        val changes = mutableListOf<SettingChange>()
        
        compareField("Text Language", GamePreferences.getTextLanguageName(localSettings.textLanguage), GamePreferences.getTextLanguageName(gameSettings.textLanguage))?.let { changes.add(it) }
        compareField("Audio Language", GamePreferences.getAudioLanguageName(localSettings.audioLanguage), GamePreferences.getAudioLanguageName(gameSettings.audioLanguage))?.let { changes.add(it) }
        compareField("Video Blacklist", localSettings.videoBlacklist, gameSettings.videoBlacklist)?.let { changes.add(it) }
        compareField("Audio Blacklist", localSettings.audioBlacklist, gameSettings.audioBlacklist)?.let { changes.add(it) }
        compareField("Save Battle Speed", localSettings.isSaveBattleSpeed, gameSettings.isSaveBattleSpeed)?.let { changes.add(it) }
        compareField("Auto Battle Open", localSettings.autoBattleOpen, gameSettings.autoBattleOpen)?.let { changes.add(it) }
        compareField("Download All Assets", localSettings.needDownloadAllAssets, gameSettings.needDownloadAllAssets)?.let { changes.add(it) }
        compareField("Force Update Video", localSettings.forceUpdateVideo, gameSettings.forceUpdateVideo)?.let { changes.add(it) }
        compareField("Force Update Audio", localSettings.forceUpdateAudio, gameSettings.forceUpdateAudio)?.let { changes.add(it) }
        compareField("Show Simplified Skill Desc", localSettings.showSimplifiedSkillDesc, gameSettings.showSimplifiedSkillDesc)?.let { changes.add(it) }
        compareField("Season Talent Tree", localSettings.gridFightSeenSeasonTalentTree, gameSettings.gridFightSeenSeasonTalentTree)?.let { changes.add(it) }
        compareField("Rogue Tourn God Mode", localSettings.rogueTournEnableGodMode, gameSettings.rogueTournEnableGodMode)?.let { changes.add(it) }
        
        _externalChanges.value = changes
        return changes
    }

    override fun getModifiedFieldsDetails(currentSettings: GamePreferences): List<SettingChange> {
        val baseline = baselineSettings ?: return emptyList()
        val changes = mutableListOf<SettingChange>()
        
        if (currentSettings.textLanguage != baseline.textLanguage) {
            changes.add(SettingChange("Text Language", GamePreferences.getTextLanguageName(baseline.textLanguage), GamePreferences.getTextLanguageName(currentSettings.textLanguage)))
        }
        if (currentSettings.audioLanguage != baseline.audioLanguage) {
            changes.add(SettingChange("Audio Language", GamePreferences.getAudioLanguageName(baseline.audioLanguage), GamePreferences.getAudioLanguageName(currentSettings.audioLanguage)))
        }
        if (currentSettings.videoBlacklist != baseline.videoBlacklist) {
            changes.add(SettingChange("Video Blacklist", baseline.videoBlacklist.toString(), currentSettings.videoBlacklist.toString()))
        }
        if (currentSettings.audioBlacklist != baseline.audioBlacklist) {
            changes.add(SettingChange("Audio Blacklist", baseline.audioBlacklist.toString(), currentSettings.audioBlacklist.toString()))
        }
        if (currentSettings.isSaveBattleSpeed != baseline.isSaveBattleSpeed) {
            changes.add(SettingChange("Save Battle Speed", baseline.isSaveBattleSpeed.toString(), currentSettings.isSaveBattleSpeed.toString()))
        }
        if (currentSettings.autoBattleOpen != baseline.autoBattleOpen) {
            changes.add(SettingChange("Auto Battle Open", baseline.autoBattleOpen.toString(), currentSettings.autoBattleOpen.toString()))
        }
        if (currentSettings.needDownloadAllAssets != baseline.needDownloadAllAssets) {
            changes.add(SettingChange("Download All Assets", baseline.needDownloadAllAssets.toString(), currentSettings.needDownloadAllAssets.toString()))
        }
        if (currentSettings.forceUpdateVideo != baseline.forceUpdateVideo) {
            changes.add(SettingChange("Force Update Video", baseline.forceUpdateVideo.toString(), currentSettings.forceUpdateVideo.toString()))
        }
        if (currentSettings.forceUpdateAudio != baseline.forceUpdateAudio) {
            changes.add(SettingChange("Force Update Audio", baseline.forceUpdateAudio.toString(), currentSettings.forceUpdateAudio.toString()))
        }
        if (currentSettings.showSimplifiedSkillDesc != baseline.showSimplifiedSkillDesc) {
            changes.add(SettingChange("Show Simplified Skill Desc", baseline.showSimplifiedSkillDesc.toString(), currentSettings.showSimplifiedSkillDesc.toString()))
        }
        if (currentSettings.gridFightSeenSeasonTalentTree != baseline.gridFightSeenSeasonTalentTree) {
            changes.add(SettingChange("Season Talent Tree", baseline.gridFightSeenSeasonTalentTree.toString(), currentSettings.gridFightSeenSeasonTalentTree.toString()))
        }
        if (currentSettings.rogueTournEnableGodMode != baseline.rogueTournEnableGodMode) {
            changes.add(SettingChange("Rogue Tourn God Mode", baseline.rogueTournEnableGodMode.toString(), currentSettings.rogueTournEnableGodMode.toString()))
        }
        
        return changes
    }

    override fun updateModifiedFields(currentSettings: GamePreferences) {
        val baseline = baselineSettings ?: return
        val modified = mutableSetOf<String>()
        
        if (currentSettings.textLanguage != baseline.textLanguage) modified.add("textLanguage")
        if (currentSettings.audioLanguage != baseline.audioLanguage) modified.add("audioLanguage")
        if (currentSettings.videoBlacklist != baseline.videoBlacklist) modified.add("videoBlacklist")
        if (currentSettings.audioBlacklist != baseline.audioBlacklist) modified.add("audioBlacklist")
        if (currentSettings.isSaveBattleSpeed != baseline.isSaveBattleSpeed) modified.add("isSaveBattleSpeed")
        if (currentSettings.autoBattleOpen != baseline.autoBattleOpen) modified.add("autoBattleOpen")
        if (currentSettings.needDownloadAllAssets != baseline.needDownloadAllAssets) modified.add("needDownloadAllAssets")
        if (currentSettings.forceUpdateVideo != baseline.forceUpdateVideo) modified.add("forceUpdateVideo")
        if (currentSettings.forceUpdateAudio != baseline.forceUpdateAudio) modified.add("forceUpdateAudio")
        if (currentSettings.showSimplifiedSkillDesc != baseline.showSimplifiedSkillDesc) modified.add("showSimplifiedSkillDesc")
        if (currentSettings.gridFightSeenSeasonTalentTree != baseline.gridFightSeenSeasonTalentTree) modified.add("gridFightSeenSeasonTalentTree")
        if (currentSettings.rogueTournEnableGodMode != baseline.rogueTournEnableGodMode) modified.add("rogueTournEnableGodMode")
        
        _modifiedFields.value = modified
        _pendingChangesCount.value = modified.size
    }
}
