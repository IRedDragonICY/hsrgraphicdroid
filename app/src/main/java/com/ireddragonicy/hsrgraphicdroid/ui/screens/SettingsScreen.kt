@file:OptIn(ExperimentalMaterial3Api::class)

package com.ireddragonicy.hsrgraphicdroid.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Launch
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ireddragonicy.hsrgraphicdroid.BuildConfig
import com.ireddragonicy.hsrgraphicdroid.R
import com.ireddragonicy.hsrgraphicdroid.ui.AppLanguage
import com.ireddragonicy.hsrgraphicdroid.ui.AppTheme
import com.ireddragonicy.hsrgraphicdroid.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.app_settings),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "Modern, minimal, Material You ready.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AppearanceSection(
                    currentTheme = uiState.currentTheme,
                    useDynamicColor = uiState.useDynamicColor,
                    currentLanguage = uiState.currentLanguage,
                    onThemeChange = { settingsViewModel.updateTheme(it) },
                    onDynamicColorChange = { settingsViewModel.setDynamicColor(it) },
                    onLanguageClick = { showLanguageDialog = true }
                )
            }

            item {
                AboutSection(
                    versionName = BuildConfig.VERSION_NAME,
                    onAboutClick = { showAboutDialog = true },
                    onGitHubClick = {
                        val intent =
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ireddragonicy/hsrgraphicdroid"))
                        context.startActivity(intent)
                    },
                    onReportIssueClick = {
                        val intent =
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ireddragonicy/hsrgraphicdroid/issues"))
                        context.startActivity(intent)
                    }
                )
            }

            item {
                AdvancedSection(
                    onClearDataClick = {
                        // TODO: wire clear data flow
                    },
                    onExportLogsClick = {
                        // TODO: wire log export flow
                    }
                )
            }
        }
    }

    if (showLanguageDialog) {
        LanguageDialog(
            currentLanguage = uiState.currentLanguage,
            onLanguageChange = {
                settingsViewModel.updateLanguage(it)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    if (showAboutDialog) {
        AboutDialog(
            onDismiss = { showAboutDialog = false }
        )
    }
}

@Composable
private fun AppearanceSection(
    currentTheme: AppTheme,
    useDynamicColor: Boolean,
    currentLanguage: AppLanguage,
    onThemeChange: (AppTheme) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onLanguageClick: () -> Unit
) {
    SettingsSectionCard(
        icon = Icons.Outlined.Palette,
        title = stringResource(R.string.appearance),
        subtitle = stringResource(R.string.language_settings_desc)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.theme),
                style = MaterialTheme.typography.titleMedium
            )
            val themeOptions = listOf(AppTheme.SYSTEM, AppTheme.LIGHT, AppTheme.DARK)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                themeOptions.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = option == currentTheme,
                        onClick = { onThemeChange(option) },
                        shape = SegmentedButtonDefaults.itemShape(index, themeOptions.size),
                        label = {
                            Text(
                                text = when (option) {
                                    AppTheme.SYSTEM -> stringResource(R.string.system_default)
                                    AppTheme.LIGHT -> stringResource(R.string.light_theme)
                                    AppTheme.DARK -> stringResource(R.string.dark_theme)
                                }
                            )
                        }
                    )
                }
            }

            Divider()

            SettingSwitchRow(
                icon = Icons.Outlined.ColorLens,
                title = stringResource(R.string.dynamic_color),
                subtitle = stringResource(R.string.dynamic_color_desc),
                checked = useDynamicColor,
                onCheckedChange = onDynamicColorChange
            )

            SettingClickableRow(
                icon = Icons.Outlined.Language,
                title = stringResource(R.string.app_language),
                subtitle = languageDisplayName(currentLanguage),
                trailing = {
                    Icon(
                        imageVector = Icons.Outlined.Launch,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                onClick = onLanguageClick
            )
        }
    }
}

@Composable
private fun AboutSection(
    versionName: String,
    onAboutClick: () -> Unit,
    onGitHubClick: () -> Unit,
    onReportIssueClick: () -> Unit
) {
    SettingsSectionCard(
        icon = Icons.Outlined.Info,
        title = stringResource(R.string.about),
        subtitle = stringResource(R.string.view_source_code)
    ) {
        SettingClickableRow(
            icon = Icons.Outlined.Info,
            title = stringResource(R.string.about_app),
            subtitle = "v$versionName",
            onClick = onAboutClick
        )
        SettingClickableRow(
            icon = Icons.Outlined.Launch,
            title = stringResource(R.string.github_repo),
            subtitle = stringResource(R.string.view_source_code),
            onClick = onGitHubClick
        )
        SettingClickableRow(
            icon = Icons.Outlined.Report,
            title = stringResource(R.string.report_issue),
            subtitle = stringResource(R.string.report_issue_desc),
            onClick = onReportIssueClick
        )
    }
}

@Composable
private fun AdvancedSection(
    onClearDataClick: () -> Unit,
    onExportLogsClick: () -> Unit
) {
    SettingsSectionCard(
        icon = Icons.Outlined.BugReport,
        title = stringResource(R.string.advanced),
        subtitle = stringResource(R.string.export_logs_desc)
    ) {
        SettingClickableRow(
            icon = Icons.Outlined.DeleteForever,
            title = stringResource(R.string.clear_app_data),
            subtitle = stringResource(R.string.clear_app_data_desc),
            onClick = onClearDataClick
        )
        SettingClickableRow(
            icon = Icons.Outlined.BugReport,
            title = stringResource(R.string.export_logs),
            subtitle = stringResource(R.string.export_logs_desc),
            onClick = onExportLogsClick
        )
    }
}

@Composable
private fun SettingsSectionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Divider()
            content()
        }
    }
}

@Composable
private fun SettingClickableRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        headlineContent = {
            Text(text = title)
        },
        supportingContent = {
            Text(text = subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            trailing ?: Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

@Composable
private fun SettingSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun LanguageDialog(
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    val languages = listOf(
        AppLanguage.SYSTEM,
        AppLanguage.ENGLISH,
        AppLanguage.INDONESIAN,
        AppLanguage.CHINESE,
        AppLanguage.JAPANESE,
        AppLanguage.RUSSIAN
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.app_language)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                languages.forEach { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLanguageChange(language) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = currentLanguage == language,
                            onClick = { onLanguageChange(language) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = languageDisplayName(language))
                            if (language == AppLanguage.SYSTEM) {
                                Text(
                                    text = stringResource(R.string.system_default),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
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
private fun languageDisplayName(language: AppLanguage): String {
    return when (language) {
        AppLanguage.SYSTEM -> stringResource(R.string.language_system)
        AppLanguage.ENGLISH -> stringResource(R.string.language_english)
        AppLanguage.INDONESIAN -> stringResource(R.string.language_indonesian)
        AppLanguage.CHINESE -> stringResource(R.string.language_chinese)
        AppLanguage.RUSSIAN -> stringResource(R.string.language_russian)
        AppLanguage.JAPANESE -> stringResource(R.string.language_japanese)
    }
}
