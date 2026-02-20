package com.ireddragonicy.hsrgraphicdroid.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ireddragonicy.hsrgraphicdroid.R
import com.ireddragonicy.hsrgraphicdroid.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_settings),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = paddingValues.calculateTopPadding(),
                    bottom = paddingValues.calculateBottomPadding() + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // Appearance Card
            item {
                AppearanceCard(
                    isDarkMode = uiState.isDarkMode,
                    useDynamicColor = uiState.useDynamicColor,
                    currentLanguage = uiState.appLanguage,
                    onThemeClick = { showThemeDialog = true },
                    onDynamicColorChange = { settingsViewModel.setDynamicColor(it) },
                    onLanguageClick = { showLanguageDialog = true }
                )
            }

            // About Card
            item {
                AboutCard(
                    versionName = "1.0.0",
                    onAboutClick = { showAboutDialog = true },
                    onGitHubClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ireddragonicy/hsrgraphicdroid"))
                        context.startActivity(intent)
                    },
                    onReportIssueClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ireddragonicy/hsrgraphicdroid/issues"))
                        context.startActivity(intent)
                    }
                )
            }

            // Advanced Card
            item {
                AdvancedCard(
                    onClearDataClick = {
                        // Show confirm dialog
                    },
                    onExportLogsClick = {
                        // Export logs
                    }
                )
            }
        }
    }

    // Theme Dialog
    if (showThemeDialog) {
        ThemeDialog(
            isDarkMode = uiState.isDarkMode,
            onDarkModeChange = { settingsViewModel.setDarkMode(it) },
            onDismiss = { showThemeDialog = false }
        )
    }

    // Language Dialog
    if (showLanguageDialog) {
        LanguageDialog(
            currentLanguage = uiState.appLanguage,
            onLanguageChange = { settingsViewModel.setAppLanguage(it) },
            onDismiss = { showLanguageDialog = false }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AboutDialog(
            onDismiss = { showAboutDialog = false }
        )
    }
}

@Composable
private fun AppearanceCard(
    isDarkMode: Boolean?,
    useDynamicColor: Boolean,
    currentLanguage: String,
    onThemeClick: () -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onLanguageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val effectiveDarkMode = isDarkMode ?: false
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.appearance),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp)
        )

            SettingsRow(
                icon = if (isDarkMode == null) Icons.Default.BrightnessAuto else if (effectiveDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                title = stringResource(R.string.theme),
                subtitle = if (isDarkMode == null) stringResource(R.string.language_system) else stringResource(if (effectiveDarkMode) R.string.dark_theme else R.string.light_theme),
                onClick = onThemeClick
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.dynamic_color)) },
                supportingContent = { Text(stringResource(R.string.dynamic_color_desc)) },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.ColorLens,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingContent = {
                    Switch(
                        checked = useDynamicColor,
                        onCheckedChange = onDynamicColorChange
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            SettingsRow(
                icon = Icons.Default.Language,
                title = stringResource(R.string.app_language),
                subtitle = getLanguageDisplayName(currentLanguage),
                onClick = onLanguageClick
            )
    }
}

@Composable
private fun AboutCard(
    versionName: String,
    onAboutClick: () -> Unit,
    onGitHubClick: () -> Unit,
    onReportIssueClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.about),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp)
        )

            SettingsRow(
                icon = Icons.Default.Info,
                title = stringResource(R.string.about_app),
                subtitle = "v$versionName",
                onClick = onAboutClick
            )

            SettingsRow(
                icon = Icons.Default.Code,
                title = stringResource(R.string.github_repo),
                subtitle = stringResource(R.string.view_source_code),
                onClick = onGitHubClick
            )

            SettingsRow(
                icon = Icons.Default.BugReport,
                title = stringResource(R.string.report_issue),
                subtitle = stringResource(R.string.report_issue_desc),
                onClick = onReportIssueClick
            )
    }
}

@Composable
private fun AdvancedCard(
    onClearDataClick: () -> Unit,
    onExportLogsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.advanced),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp)
        )

            SettingsRow(
                icon = Icons.Default.DeleteForever,
                title = stringResource(R.string.clear_app_data),
                subtitle = stringResource(R.string.clear_app_data_desc),
                onClick = onClearDataClick
            )

            SettingsRow(
                icon = Icons.Default.FileDownload,
                title = stringResource(R.string.export_logs),
                subtitle = stringResource(R.string.export_logs_desc),
                onClick = onExportLogsClick
            )
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun ThemeDialog(
    isDarkMode: Boolean?,
    onDarkModeChange: (Boolean?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.theme)) },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isDarkMode == null,
                        onClick = { onDarkModeChange(null) }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.language_system))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isDarkMode == false,
                        onClick = { onDarkModeChange(false) }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.light_theme))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isDarkMode == true,
                        onClick = { onDarkModeChange(true) }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.dark_theme))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        }
    )
}

@Composable
private fun LanguageDialog(
    currentLanguage: String,
    onLanguageChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val languages = listOf(
        "system" to stringResource(R.string.language_system),
        "en" to "English",
        "zh" to "中文",
        "ja" to "日本語",
        "ru" to "Русский",
        "id" to "Bahasa Indonesia"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.app_language)) },
        text = {
            Column {
                languages.forEach { (code, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentLanguage == code,
                            onClick = { onLanguageChange(code) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(name)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        }
    )
}

@Composable
private fun AboutDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.about_app)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.about_description),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.license_info),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        }
    )
}

@Composable
private fun getLanguageDisplayName(code: String): String {
    return when (code) {
        "system" -> stringResource(R.string.language_system)
        "en" -> "English"
        "zh" -> "中文"
        "ja" -> "日本語"
        "ru" -> "Русский"
        "id" -> "Bahasa Indonesia"
        else -> code
    }
}
