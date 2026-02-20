package com.ireddragonicy.hsrgraphicdroid.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ireddragonicy.hsrgraphicdroid.R
import com.ireddragonicy.hsrgraphicdroid.data.BackupData
import com.ireddragonicy.hsrgraphicdroid.data.GraphicsSettings
import com.ireddragonicy.hsrgraphicdroid.ui.components.*
import com.ireddragonicy.hsrgraphicdroid.ui.viewmodel.GraphicsViewModel
import com.ireddragonicy.hsrgraphicdroid.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

// Data class for slider configuration to reduce redundancy
private data class SliderConfig(
    val key: String,
    val labelRes: Int,
    val icon: Painter,
    val descriptionRes: Int? = null,
    val valueRange: ClosedFloatingPointRange<Float>,
    val steps: Int,
    val getValue: (GraphicsSettings) -> Float,
    val setValue: (GraphicsSettings, Float) -> GraphicsSettings,
    val getDisplayValue: (GraphicsSettings, Float) -> String
)

// Data class for switch configuration
private data class SwitchConfig(
    val key: String,
    val labelRes: Int,
    val icon: Painter,
    val descriptionRes: Int? = null,
    val getValue: (GraphicsSettings) -> Boolean,
    val setValue: (GraphicsSettings, Boolean) -> GraphicsSettings
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphicsScreen(
    mainViewModel: MainViewModel,
    graphicsViewModel: GraphicsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val status by mainViewModel.status.collectAsStateWithLifecycle()
    val uiState by graphicsViewModel.uiState.collectAsStateWithLifecycle()
    val canUndo by graphicsViewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by graphicsViewModel.canRedo.collectAsStateWithLifecycle()

    var showBackupsSheet by remember { mutableStateOf(false) }
    var showSaveBackupDialog by remember { mutableStateOf(false) }
    var showApplyDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showPendingChangesDialog by remember { mutableStateOf(false) }
    var selectedPreset by remember { mutableStateOf<Int?>(null) }

    // Snackbar host
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle messages
    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            graphicsViewModel.clearMessage()
        }
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            graphicsViewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            EditorTopAppBar(
                title = stringResource(R.string.graphics_editor),
                canUndo = canUndo,
                canRedo = canRedo,
                onUndo = { graphicsViewModel.undo() },
                onRedo = { graphicsViewModel.redo() },
                onReset = { showResetDialog = true }
            )
        },
        bottomBar = {
            EditorBottomBar(
                hasChanges = uiState.hasChanges,
                pendingChangesCount = uiState.pendingChangesCount,
                onSaveBackup = { showSaveBackupDialog = true },
                onApply = { showApplyDialog = true },
                onViewChanges = { showPendingChangesDialog = true }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // Master Quality Card
                item {
                    QualitySliderCard(
                        title = stringResource(R.string.overall_graphics_quality),
                        description = stringResource(R.string.overall_graphics_quality_desc),
                        value = uiState.currentSettings.graphicsQuality,
                        displayValue = uiState.currentSettings.getMasterQualityName(uiState.currentSettings.graphicsQuality),
                        onValueChange = { value ->
                            graphicsViewModel.updateSettings(
                                uiState.currentSettings.copy(graphicsQuality = value)
                            )
                        }
                    )
                }

                // Extended Graphics Settings Card
                item {
                    ExtendedSettingsCard(
                        settings = uiState.currentSettings,
                        modifiedFields = uiState.modifiedFields,
                        selectedPreset = selectedPreset,
                        onPresetSelected = { preset ->
                            selectedPreset = preset
                            graphicsViewModel.applyPreset(preset)
                        },
                        onSettingsChange = { newSettings ->
                            selectedPreset = null
                            graphicsViewModel.updateSettings(newSettings)
                        }
                    )
                }

                // Display Settings
                item {
                    DisplaySettingsCard(
                        settings = uiState.currentSettings,
                        modifiedFields = uiState.modifiedFields,
                        onSettingsChange = { newSettings ->
                            graphicsViewModel.updateSettings(newSettings)
                        }
                    )
                }
            }

            // Loading Overlay
            LoadingOverlay(isLoading = uiState.isLoading)
        }
    }

    // Backups Bottom Sheet
    if (showBackupsSheet) {
        GraphicsBackupsBottomSheet(
            backups = uiState.backups,
            onRestore = { backup ->
                graphicsViewModel.restoreBackup(backup)
                showBackupsSheet = false
            },
            onDelete = { backup ->
                graphicsViewModel.deleteBackup(backup)
            },
            onDismissRequest = { showBackupsSheet = false }
        )
    }

    // Save Backup Dialog
    if (showSaveBackupDialog) {
        SaveBackupDialog(
            onConfirm = { name ->
                val finalName = name.ifEmpty { "Backup ${System.currentTimeMillis()}" }
                graphicsViewModel.saveBackup(finalName)
                showSaveBackupDialog = false
            },
            onDismissRequest = { showSaveBackupDialog = false }
        )
    }

    // Apply Settings Dialog
    if (showApplyDialog) {
        AlertDialog(
            onDismissRequest = { showApplyDialog = false },
            title = { Text(stringResource(R.string.apply)) },
            text = { Text(stringResource(R.string.apply_settings_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        graphicsViewModel.applySettings()
                        showApplyDialog = false
                    }
                ) {
                    Text(stringResource(R.string.apply))
                }
            },
            dismissButton = {
                TextButton(onClick = { showApplyDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Reset Dialog
    if (showResetDialog) {
        ResetChangesDialog(
            onConfirm = {
                graphicsViewModel.resetToOriginal()
                selectedPreset = null
                showResetDialog = false
            },
            onDismissRequest = { showResetDialog = false }
        )
    }

    // Pending Changes Dialog
    if (showPendingChangesDialog) {
        PendingChangesDialog(
            changes = graphicsViewModel.getPendingChangesDetails(),
            onDismissRequest = { showPendingChangesDialog = false }
        )
    }
}

@Composable
private fun StatusCard(
    status: com.ireddragonicy.hsrgraphicdroid.ui.viewmodel.StatusState,
    onLaunchGame: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onShowBackups: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_info),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.system_status),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            StatusChip(
                text = if (status.isChecking) stringResource(R.string.checking)
                else if (status.isRootGranted) stringResource(R.string.root_granted)
                else stringResource(R.string.root_check_failed),
                isSuccess = status.isRootGranted,
                isLoading = status.isChecking
            )

            Spacer(modifier = Modifier.height(8.dp))

            StatusChip(
                text = if (status.isChecking) stringResource(R.string.checking)
                else if (status.isGameInstalled) {
                    stringResource(R.string.game_found) + (status.gameVersion?.let { " ($it)" } ?: "")
                } else stringResource(R.string.game_not_found),
                isSuccess = status.isGameInstalled,
                isLoading = status.isChecking
            )

            if (status.isGameInstalled && !status.isChecking) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onLaunchGame,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(painterResource(R.drawable.ic_play), null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.launch_game))
                    }

                    OutlinedButton(
                        onClick = onOpenAppInfo,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(painterResource(R.drawable.ic_info), null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.app_info))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onShowBackups,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(painterResource(R.drawable.ic_backup), null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.saved_backups))
                }
            }
        }
    }
}

@Composable
private fun ExtendedSettingsCard(
    settings: GraphicsSettings,
    modifiedFields: Set<String>,
    selectedPreset: Int?,
    onPresetSelected: (Int) -> Unit,
    onSettingsChange: (GraphicsSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_tune), null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.graphics_settings), style = MaterialTheme.typography.titleLarge)
        }

            Text(
                text = stringResource(R.string.graphics_settings_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Presets Section
            SettingsSection(
                title = stringResource(R.string.presets),
                subtitle = stringResource(R.string.presets_desc)
            ) {
                PresetButtonRow(
                    selectedPreset = selectedPreset,
                    onPresetSelected = onPresetSelected,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Performance Section
            SettingsSection(
                title = stringResource(R.string.section_performance),
                subtitle = stringResource(R.string.section_performance_desc)
            ) {
                // FPS
                GraphicsSlider(
                    label = stringResource(R.string.fps),
                    value = settings.fps.toFloat(),
                    valueRange = 30f..120f,
                    steps = 2,
                    displayValue = settings.fps.toString(),
                    onValueChange = { onSettingsChange(settings.copy(fps = it.toInt())) },
                    icon = painterResource(R.drawable.ic_speed),
                    isModified = modifiedFields.contains("fps")
                )

                Spacer(Modifier.height(8.dp))

                // VSync
                GraphicsSwitch(
                    label = stringResource(R.string.vsync),
                    checked = settings.enableVSync,
                    onCheckedChange = { onSettingsChange(settings.copy(enableVSync = it)) },
                    icon = painterResource(R.drawable.ic_sync),
                    isModified = modifiedFields.contains("vsync")
                )

                Spacer(Modifier.height(8.dp))

                // Render Scale
                GraphicsSlider(
                    label = stringResource(R.string.render_scale),
                    value = settings.renderScale.toFloat(),
                    valueRange = 0.5f..2.0f,
                    steps = 14,
                    displayValue = String.format("%.1fx", settings.renderScale),
                    onValueChange = { onSettingsChange(settings.copy(renderScale = it.toDouble())) },
                    icon = painterResource(R.drawable.ic_aspect_ratio),
                    isModified = modifiedFields.contains("renderScale")
                )
            }

            Spacer(Modifier.height(16.dp))

            // Visual Fidelity Section
            SettingsSection(
                title = stringResource(R.string.section_visual_fidelity),
                subtitle = stringResource(R.string.section_visual_fidelity_desc)
            ) {
                QualitySliderItem(
                    labelRes = R.string.resolution_quality,
                    value = settings.resolutionQuality,
                    displayValue = settings.getQualityName(settings.resolutionQuality),
                    onValueChange = { onSettingsChange(settings.copy(resolutionQuality = it)) },
                    icon = painterResource(R.drawable.ic_high_quality),
                    isModified = modifiedFields.contains("resolution")
                )

                QualitySliderItem(
                    labelRes = R.string.shadow_quality,
                    value = settings.shadowQuality,
                    displayValue = settings.getQualityName(settings.shadowQuality),
                    onValueChange = { onSettingsChange(settings.copy(shadowQuality = it)) },
                    icon = painterResource(R.drawable.ic_contrast),
                    isModified = modifiedFields.contains("shadow"),
                    descriptionRes = R.string.shadow_quality_desc
                )

                QualitySliderItem(
                    labelRes = R.string.light_quality,
                    value = settings.lightQuality,
                    displayValue = settings.getQualityName(settings.lightQuality),
                    onValueChange = { onSettingsChange(settings.copy(lightQuality = it)) },
                    icon = painterResource(R.drawable.ic_light_mode),
                    isModified = modifiedFields.contains("light"),
                    descriptionRes = R.string.light_quality_desc
                )

                QualitySliderItem(
                    labelRes = R.string.character_quality,
                    value = settings.characterQuality,
                    displayValue = settings.getQualityName(settings.characterQuality),
                    onValueChange = { onSettingsChange(settings.copy(characterQuality = it)) },
                    icon = painterResource(R.drawable.ic_person),
                    isModified = modifiedFields.contains("character"),
                    descriptionRes = R.string.character_quality_desc
                )

                QualitySliderItem(
                    labelRes = R.string.environment_quality,
                    value = settings.envDetailQuality,
                    displayValue = settings.getQualityName(settings.envDetailQuality),
                    onValueChange = { onSettingsChange(settings.copy(envDetailQuality = it)) },
                    icon = painterResource(R.drawable.ic_landscape),
                    isModified = modifiedFields.contains("environment"),
                    descriptionRes = R.string.environment_quality_desc
                )

                QualitySliderItem(
                    labelRes = R.string.reflection_quality,
                    value = settings.reflectionQuality,
                    displayValue = settings.getQualityName(settings.reflectionQuality),
                    onValueChange = { onSettingsChange(settings.copy(reflectionQuality = it)) },
                    icon = painterResource(R.drawable.ic_water_drop),
                    isModified = modifiedFields.contains("reflection"),
                    descriptionRes = R.string.reflection_quality_desc
                )
            }

            Spacer(Modifier.height(16.dp))

            // Effects Section
            SettingsSection(
                title = stringResource(R.string.section_effects),
                subtitle = stringResource(R.string.section_effects_desc)
            ) {
                // SFX Quality (range 1-5)
                GraphicsSlider(
                    label = stringResource(R.string.sfx_quality),
                    value = settings.sfxQuality.toFloat(),
                    valueRange = 1f..5f,
                    steps = 3,
                    displayValue = settings.getSfxQualityName(settings.sfxQuality),
                    onValueChange = { onSettingsChange(settings.copy(sfxQuality = it.toInt())) },
                    icon = painterResource(R.drawable.ic_auto_awesome),
                    description = stringResource(R.string.sfx_quality_desc),
                    isModified = modifiedFields.contains("sfx")
                )

                Spacer(Modifier.height(8.dp))

                QualitySliderItem(
                    labelRes = R.string.bloom_quality,
                    value = settings.bloomQuality,
                    displayValue = settings.getQualityName(settings.bloomQuality),
                    onValueChange = { onSettingsChange(settings.copy(bloomQuality = it)) },
                    icon = painterResource(R.drawable.ic_flare),
                    isModified = modifiedFields.contains("bloom"),
                    descriptionRes = R.string.bloom_quality_desc
                )

                // AA Mode (0-2) mapped: 0=Off, 1=FXAA(Game 2), 2=TAA(Game 1)
                val aaUiValue = when (settings.aaMode) {
                    1 -> 2f // TAA is on the far right
                    2 -> 1f // FXAA is in the middle
                    else -> 0f // Off is on the far left
                }

                GraphicsSlider(
                    label = stringResource(R.string.anti_aliasing),
                    value = aaUiValue,
                    valueRange = 0f..2f,
                    steps = 1,
                    displayValue = settings.getAAModeName(settings.aaMode),
                    onValueChange = {
                        val newAaMode = when (it.toInt()) {
                            1 -> 2 // UI pos 1 -> FXAA (Game 2)
                            2 -> 1 // UI pos 2 -> TAA (Game 1)
                            else -> 0 // UI pos 0 -> Off (Game 0)
                        }
                        onSettingsChange(settings.copy(aaMode = newAaMode))
                    },
                    icon = painterResource(R.drawable.ic_filter_hdr),
                    description = stringResource(R.string.anti_aliasing_desc),
                    isModified = modifiedFields.contains("aa")
                )

                Spacer(Modifier.height(8.dp))

                // Self Shadow (0-2)
                GraphicsSlider(
                    label = stringResource(R.string.self_shadow),
                    value = settings.enableSelfShadow.toFloat(),
                    valueRange = 0f..2f,
                    steps = 1,
                    displayValue = settings.getSelfShadowName(settings.enableSelfShadow),
                    onValueChange = { onSettingsChange(settings.copy(enableSelfShadow = it.toInt())) },
                    icon = painterResource(R.drawable.ic_contrast),
                    description = stringResource(R.string.self_shadow_desc),
                    isModified = modifiedFields.contains("selfShadow")
                )
            }

            Spacer(Modifier.height(16.dp))

            // Upscaling Section
            SettingsSection(
                title = stringResource(R.string.section_upscaling),
                subtitle = stringResource(R.string.section_upscaling_desc)
            ) {
                GraphicsSwitch(
                    label = stringResource(R.string.metalfx_super_resolution),
                    checked = settings.enableMetalFXSU,
                    onCheckedChange = { onSettingsChange(settings.copy(enableMetalFXSU = it)) },
                    icon = painterResource(R.drawable.ic_auto_awesome),
                    description = stringResource(R.string.metalfx_desc),
                    isModified = modifiedFields.contains("metalFx")
                )

                Spacer(Modifier.height(8.dp))

                GraphicsSwitch(
                    label = stringResource(R.string.half_res_transparent),
                    checked = settings.enableHalfResTransparent,
                    onCheckedChange = { onSettingsChange(settings.copy(enableHalfResTransparent = it)) },
                    icon = painterResource(R.drawable.ic_opacity),
                    description = stringResource(R.string.half_res_desc),
                    isModified = modifiedFields.contains("halfRes")
                )

                Spacer(Modifier.height(8.dp))

                // DLSS (0-4)
                GraphicsSlider(
                    label = stringResource(R.string.dlss_quality),
                    value = settings.dlssQuality.toFloat(),
                    valueRange = 0f..4f,
                    steps = 3,
                    displayValue = settings.getDlssName(settings.dlssQuality),
                    onValueChange = { onSettingsChange(settings.copy(dlssQuality = it.toInt())) },
                    icon = painterResource(R.drawable.ic_memory),
                    description = stringResource(R.string.dlss_desc),
                    isModified = modifiedFields.contains("dlss")
                )
            }
    }
}

@Composable
private fun DisplaySettingsCard(
    settings: GraphicsSettings,
    modifiedFields: Set<String>,
    onSettingsChange: (GraphicsSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SettingsSection(
            title = stringResource(R.string.section_display),
            subtitle = stringResource(R.string.section_display_desc)
        ) {
            // Particle Trail (0-3)
            GraphicsSlider(
                label = stringResource(R.string.particle_trail),
                value = settings.particleTrailSmoothness.toFloat(),
                valueRange = 0f..3f,
                steps = 2,
                displayValue = settings.getParticleTrailName(settings.particleTrailSmoothness),
                onValueChange = { onSettingsChange(settings.copy(particleTrailSmoothness = it.toInt())) },
                icon = painterResource(R.drawable.ic_auto_awesome),
                description = stringResource(R.string.particle_trail_desc),
                isModified = modifiedFields.contains("particleTrail")
            )

            Spacer(Modifier.height(16.dp))

            // Resolution Display (Read-only)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(painterResource(R.drawable.ic_screenshot), null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.current_resolution), style = MaterialTheme.typography.labelLarge)
                }
                Text(
                    text = "${settings.screenWidth}×${settings.screenHeight}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(12.dp))

            // Fullscreen Mode Display (Read-only)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(painterResource(R.drawable.ic_fullscreen), null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.fullscreen_mode), style = MaterialTheme.typography.labelLarge)
                }
                Text(
                    text = settings.getFullscreenModeName(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(16.dp))

            // Speed Up Open
            GraphicsSwitch(
                label = stringResource(R.string.speed_up_open),
                checked = settings.speedUpOpen == 1,
                onCheckedChange = { onSettingsChange(settings.copy(speedUpOpen = if (it) 1 else 0)) },
                icon = painterResource(R.drawable.ic_rocket_launch),
                description = stringResource(R.string.speed_up_open_desc),
                isModified = modifiedFields.contains("speedUpOpen")
            )

            Spacer(Modifier.height(8.dp))

            // PSO Shader Warmup
            GraphicsSwitch(
                label = stringResource(R.string.pso_shader_warmup),
                checked = settings.enablePsoShaderWarmup,
                onCheckedChange = { onSettingsChange(settings.copy(enablePsoShaderWarmup = it)) },
                icon = painterResource(R.drawable.ic_memory),
                description = stringResource(R.string.pso_shader_warmup_desc),
                isModified = modifiedFields.contains("psoShader")
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            SectionHeader(title = title, subtitle = subtitle)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun QualitySliderItem(
    labelRes: Int,
    value: Int,
    displayValue: String,
    onValueChange: (Int) -> Unit,
    icon: Painter,
    isModified: Boolean,
    descriptionRes: Int? = null
) {
    GraphicsSlider(
        label = stringResource(labelRes),
        value = value.toFloat(),
        valueRange = 0f..5f,
        steps = 4,
        displayValue = displayValue,
        onValueChange = { onValueChange(it.toInt()) },
        icon = icon,
        description = descriptionRes?.let { stringResource(it) },
        isModified = isModified
    )
    Spacer(Modifier.height(8.dp))
}


