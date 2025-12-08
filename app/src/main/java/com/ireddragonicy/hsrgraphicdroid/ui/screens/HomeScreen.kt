package com.ireddragonicy.hsrgraphicdroid.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VideogameAsset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ireddragonicy.hsrgraphicdroid.R
import com.ireddragonicy.hsrgraphicdroid.data.GraphicsSettings
import com.ireddragonicy.hsrgraphicdroid.ui.components.InfoCard
import com.ireddragonicy.hsrgraphicdroid.ui.components.StatusChip
import com.ireddragonicy.hsrgraphicdroid.ui.viewmodel.GameInfo
import com.ireddragonicy.hsrgraphicdroid.ui.viewmodel.MainViewModel
import com.ireddragonicy.hsrgraphicdroid.ui.viewmodel.StatusState
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    mainViewModel: MainViewModel,
    onNavigateToGraphics: () -> Unit = {},
    onNavigateToGamePrefs: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val status by mainViewModel.status.collectAsStateWithLifecycle()
    
    var currentSettings by remember { mutableStateOf<GraphicsSettings?>(null) }
    var gameInfo by remember { mutableStateOf<GameInfo?>(null) }

    LaunchedEffect(Unit) {
        mainViewModel.refreshStatus()
        currentSettings = mainViewModel.readGraphicsSettings()
        gameInfo = mainViewModel.loadGameInfo()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 4.dp,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.about_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

            Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                    StatusPill(
                        icon = Icons.Outlined.Security,
                        text = when {
                            status.isChecking -> stringResource(R.string.checking)
                            status.isRootGranted -> stringResource(R.string.root_granted)
                            else -> stringResource(R.string.root_not_granted)
                        },
                        type = if (status.isChecking) PillType.Loading else if (status.isRootGranted) PillType.Success else PillType.Warning
                )
                    StatusPill(
                        icon = Icons.Outlined.VideogameAsset,
                        text = when {
                            status.isChecking -> stringResource(R.string.checking)
                            status.isGameInstalled -> stringResource(R.string.game_found)
                            else -> stringResource(R.string.game_not_found)
                        },
                        type = if (status.isChecking) PillType.Loading else if (status.isGameInstalled) PillType.Success else PillType.Warning
                    )
                }

                Divider()

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            status.gameVersion?.let { version ->
                Text(
                    text = "Version: $version",
                                style = MaterialTheme.typography.bodyMedium
                )
            }
            gameInfo?.let { info ->
                info.apkName?.let { apk ->
                    Text(
                        text = "APK: $apk",
                        style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (info.dataBytes != null && info.cacheBytes != null) {
                    Text(
                        text = "Data: ${formatBytes(info.dataBytes)} • Cache: ${formatBytes(info.cacheBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Current Settings Preview (KernelSU-style info list)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.graphics_settings),
                    style = MaterialTheme.typography.titleMedium
                )
                currentSettings?.let { settings ->
                    InfoListRow(
                        icon = Icons.Outlined.Tune,
                        title = stringResource(R.string.overall_graphics_quality),
                        value = settings.getMasterQualityName(settings.graphicsQuality)
                    )
                    InfoListRow(
                        icon = Icons.Outlined.Speed,
                        title = stringResource(R.string.fps),
                        value = "${settings.fps}"
                    )
                    InfoListRow(
                        icon = Icons.Outlined.AspectRatio,
                        title = stringResource(R.string.render_scale),
                        value = String.format("%.1fx", settings.renderScale)
                    )
                    InfoListRow(
                        icon = Icons.Outlined.Apps,
                        title = stringResource(R.string.screen_resolution),
                        value = "${settings.screenWidth}×${settings.screenHeight}"
                    )
                    InfoListRow(
                        icon = Icons.Outlined.Sync,
                        title = stringResource(R.string.vsync),
                        value = if (settings.enableVSync) "On" else "Off"
                    )
                    InfoListRow(
                        icon = Icons.Outlined.Memory,
                        title = stringResource(R.string.dlss_quality),
                        value = settings.getDlssName(settings.dlssQuality)
                    )
                    InfoListRow(
                        icon = Icons.Outlined.Tune,
                        title = stringResource(R.string.anti_aliasing),
                        value = settings.getAAModeName(settings.aaMode)
                    )
                } ?: run {
                    Text(
                        text = stringResource(R.string.not_available),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Actions pinned near nav
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 3.dp,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.quick_actions),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            mainViewModel.currentPackage()?.let { pkg ->
                                context.packageManager.getLaunchIntentForPackage(pkg)?.let {
                                    context.startActivity(it)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = status.isGameInstalled
                    ) {
                        Icon(Icons.Outlined.PlayArrow, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.launch_game))
                    }
                    OutlinedButton(
                        onClick = { scope.launch { mainViewModel.killGame() } },
                        modifier = Modifier.weight(1f),
                        enabled = status.isGameInstalled && status.isRootGranted
                    ) {
                        Icon(Icons.Outlined.Close, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.kill_game))
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = onNavigateToGraphics,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.Tune, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.graphics_editor))
                    }
                    TextButton(
                        onClick = {
                            mainViewModel.currentPackage()?.let { pkg ->
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:$pkg")
                                }
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = status.isGameInstalled
                    ) {
                        Icon(Icons.Outlined.Info, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.app_info))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Donate card (3-column large icons)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Support / Donate",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SupportTile(
                        label = "PayPal",
                        url = "https://www.paypal.com/paypalme/IRedDragonICY",
                        icon = Icons.Outlined.CreditCard,
                        modifier = Modifier.weight(1f)
                    )
                    SupportTile(
                        label = "Ko-fi",
                        url = "https://ko-fi.com/ireddragonicy",
                        icon = Icons.Outlined.Coffee,
                        modifier = Modifier.weight(1f)
                    )
                    SupportTile(
                        label = "Saweria",
                        url = "https://saweria.co/IRedDragonICY",
                        icon = Icons.Outlined.FavoriteBorder,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val unit = 1024
    val exp = (Math.log(bytes.toDouble()) / Math.log(unit.toDouble())).toInt()
    val pre = "KMGTPE"[exp - 1]
    return String.format("%.1f %sB", bytes / Math.pow(unit.toDouble(), exp.toDouble()), pre)
}

@Composable
private fun StatusPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    type: PillType
) {
    val colors = AssistChipDefaults.assistChipColors(
        containerColor = when (type) {
            PillType.Success -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            PillType.Warning -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.28f)
            PillType.Loading -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        },
        labelColor = MaterialTheme.colorScheme.onSurface,
        leadingIconContentColor = MaterialTheme.colorScheme.onSurface
    )

    AssistChip(
        onClick = {},
        enabled = false,
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
        label = { Text(text, style = MaterialTheme.typography.labelLarge) },
        colors = colors
    )
}

private enum class PillType { Success, Warning, Loading }

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SupportTile(
    label: String,
    url: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Surface(
        modifier = modifier
            .height(110.dp)
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            },
        shape = MaterialTheme.shapes.large,
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun InfoListRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
