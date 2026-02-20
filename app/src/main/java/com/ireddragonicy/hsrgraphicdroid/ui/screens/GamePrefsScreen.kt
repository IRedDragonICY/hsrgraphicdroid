package com.ireddragonicy.hsrgraphicdroid.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ireddragonicy.hsrgraphicdroid.R
import com.ireddragonicy.hsrgraphicdroid.data.GamePreferences
import com.ireddragonicy.hsrgraphicdroid.ui.components.*
import com.ireddragonicy.hsrgraphicdroid.ui.viewmodel.GamePrefsViewModel
import com.ireddragonicy.hsrgraphicdroid.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamePrefsScreen(
    mainViewModel: MainViewModel,
    gamePrefsViewModel: GamePrefsViewModel,
    modifier: Modifier = Modifier
) {
    val status by mainViewModel.status.collectAsStateWithLifecycle()
    val uiState by gamePrefsViewModel.uiState.collectAsStateWithLifecycle()
    
    var showResetDialog by remember { mutableStateOf(false) }
    var showApplyDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            gamePrefsViewModel.clearMessage()
        }
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            gamePrefsViewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.game_preferences),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.reset)
                        )
                    }
                }
            )
        },
        bottomBar = {
            GamePrefsBottomBar(
                hasChanges = uiState.hasChanges,
                onReset = { showResetDialog = true },
                onApply = { showApplyDialog = true }
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
                contentPadding = PaddingValues(
                    start = 16.dp, 
                    end = 16.dp, 
                    top = 0.dp, 
                    bottom = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Language Settings Card
                item {
                    LanguageSettingsCard(
                        textLanguage = uiState.currentPrefs.textLanguage,
                        audioLanguage = uiState.currentPrefs.audioLanguage,
                        onTextLanguageChange = { gamePrefsViewModel.updateTextLanguage(it) },
                        onAudioLanguageChange = { gamePrefsViewModel.updateAudioLanguage(it) }
                    )
                }

                // Video Blacklist Card
                item {
                    BlacklistCard(
                        title = stringResource(R.string.video_blacklist),
                        description = stringResource(R.string.video_blacklist_desc),
                        icon = Icons.Default.Videocam,
                        items = uiState.currentPrefs.videoBlacklist,
                        onRemoveItem = { gamePrefsViewModel.removeFromVideoBlacklist(it) },
                        onAddItem = { gamePrefsViewModel.addToVideoBlacklist(it) }
                    )
                }

                // Audio Blacklist Card
                item {
                    BlacklistCard(
                        title = stringResource(R.string.audio_blacklist),
                        description = stringResource(R.string.audio_blacklist_desc),
                        icon = Icons.Default.MusicNote,
                        items = uiState.currentPrefs.audioBlacklist,
                        onRemoveItem = { gamePrefsViewModel.removeFromAudioBlacklist(it) },
                        onAddItem = { gamePrefsViewModel.addToAudioBlacklist(it) }
                    )
                }
            }

            LoadingOverlay(isLoading = uiState.isLoading)
        }
    }

    // Reset Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.reset_all_changes)) },
            text = { Text(stringResource(R.string.reset_prefs_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        gamePrefsViewModel.resetToOriginal()
                        showResetDialog = false
                    }
                ) {
                    Text(stringResource(R.string.reset))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Apply Dialog
    if (showApplyDialog) {
        AlertDialog(
            onDismissRequest = { showApplyDialog = false },
            title = { Text(stringResource(R.string.apply)) },
            text = { Text(stringResource(R.string.apply_prefs_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        gamePrefsViewModel.applySettings()
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LanguageSettingsCard(
    textLanguage: Int,
    audioLanguage: Int,
    onTextLanguageChange: (Int) -> Unit,
    onAudioLanguageChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.language_settings),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Text(
                text = stringResource(R.string.language_settings_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(Modifier.height(16.dp))

            // Text Language
            Text(
                text = stringResource(R.string.text_language),
                style = MaterialTheme.typography.labelLarge
            )
            FlowRow(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GamePreferences.TEXT_LANGUAGES.forEach { (code, name) ->
                    FilterChip(
                        selected = textLanguage == code,
                        onClick = { onTextLanguageChange(code) },
                        label = { Text(name) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Audio Language
            Text(
                text = stringResource(R.string.audio_language),
                style = MaterialTheme.typography.labelLarge
            )
            FlowRow(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GamePreferences.AUDIO_LANGUAGES.forEach { (code, name) ->
                    FilterChip(
                        selected = audioLanguage == code,
                        onClick = { onAudioLanguageChange(code) },
                        label = { Text(name) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlacklistCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    items: List<String>,
    onRemoveItem: (String) -> Unit,
    onAddItem: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${items.size} items",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                }
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))

                    if (items.isEmpty()) {
                        Text(
                            text = stringResource(R.string.blacklist_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                        )
                    } else {
                        items.forEach { item ->
                            BlacklistItem(
                                filename = item,
                                onRemove = { onRemoveItem(item) }
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    FilledTonalButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.add_to_blacklist))
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddBlacklistDialog(
            title = title,
            onAdd = { 
                onAddItem(it)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
private fun AddBlacklistDialog(
    title: String,
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var itemName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_to_blacklist)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.blacklist_add_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text(stringResource(R.string.item_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (itemName.isNotBlank()) onAdd(itemName.trim()) },
                enabled = itemName.isNotBlank()
            ) {
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun GamePrefsBottomBar(
    hasChanges: Boolean,
    onReset: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.reset))
            }

            Button(
                onClick = onApply,
                modifier = Modifier.weight(1f),
                colors = if (hasChanges) ButtonDefaults.buttonColors()
                else ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.apply))
            }
        }
    }
}
