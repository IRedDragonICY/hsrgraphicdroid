package com.ireddragonicy.hsrgraphicdroid.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ireddragonicy.hsrgraphicdroid.R
import com.ireddragonicy.hsrgraphicdroid.data.GraphicsSettings
import com.ireddragonicy.hsrgraphicdroid.ui.components.StatusChip
import com.ireddragonicy.hsrgraphicdroid.data.GamePreferences
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
    var gamePrefs by remember { mutableStateOf<GamePreferences?>(null) }
    var showUid by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        mainViewModel.refreshStatus()
        currentSettings = mainViewModel.readGraphicsSettings()
        gameInfo = mainViewModel.loadGameInfo()
        gamePrefs = mainViewModel.readGamePreferences()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        // App Header
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusChip(
                text = if (status.isChecking) stringResource(R.string.checking)
                else if (status.isRootGranted) stringResource(R.string.root_granted)
                else stringResource(R.string.root_not_granted),
                isSuccess = status.isRootGranted,
                isLoading = status.isChecking,
                modifier = Modifier.weight(1f)
            )
            StatusChip(
                text = if (status.isChecking) stringResource(R.string.checking)
                else if (status.isGameInstalled) stringResource(R.string.game_found)
                else stringResource(R.string.game_not_found),
                isSuccess = status.isGameInstalled,
                isLoading = status.isChecking,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Game Info Section (Modern Flat Look)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                gamePrefs?.lastUserId?.let { uid ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Account UID",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (showUid) uid.toString() else "•".repeat(uid.toString().length),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { showUid = !showUid }) {
                            Icon(
                                painter = painterResource(if (showUid) R.drawable.ic_visibility_off else R.drawable.ic_visibility),
                                contentDescription = if (showUid) "Hide UID" else "Show UID",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                }

                status.gameVersion?.let { version ->
                    InfoRow(label = "Version", value = version)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                gameInfo?.let { info ->
                    info.apkName?.let { apk ->
                        InfoRow(label = "Package", value = apk)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    if (info.dataBytes != null && info.cacheBytes != null) {
                        InfoRow(label = "Storage", value = "${formatBytes(info.dataBytes)} Data • ${formatBytes(info.cacheBytes)} Cache")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Actions Card
        Text(
            text = stringResource(R.string.quick_actions),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                        Icon(
                            painter = painterResource(R.drawable.ic_play),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.launch_game))
                    }

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                mainViewModel.killGame()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = status.isGameInstalled && status.isRootGranted
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.kill_game))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = onNavigateToGraphics,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_tune),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
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
                        Icon(
                            painter = painterResource(R.drawable.ic_info),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.app_info))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            status.configPath?.let { path ->
                                // Copy path to clipboard as a fallback
                                val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clipData = android.content.ClipData.newPlainText("Config Path", path)
                                clipboardManager.setPrimaryClip(clipData)

                                try {
                                    val builder = android.os.StrictMode.VmPolicy.Builder()
                                    android.os.StrictMode.setVmPolicy(builder.build())
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(Uri.parse("file://$path"), "text/xml")
                                        // Do NOT use FLAG_GRANT_READ_URI_PERMISSION here!
                                        // The Android UI framework will stat() the file and throw ENOENT
                                        // because our app UID doesn't have root access natively.
                                        // Let the receiving root editor handle the file:// path with its own root shell.
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Open with Root Editor"))
                                    android.widget.Toast.makeText(context, "Path copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "No app found! Path copied to clipboard.", android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = status.isGameInstalled && status.isRootGranted && status.configPath != null
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_code),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open playerprefs.xml")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Current Settings Preview
        Text(
            text = stringResource(R.string.graphics_settings),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = stringResource(R.string.overall_graphics_quality),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                currentSettings?.let { settings ->
                    Text(
                        text = "FPS ${settings.fps} • Render ${String.format("%.1f", settings.renderScale)}x • ${settings.getMasterQualityName(settings.graphicsQuality)}",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "${settings.screenWidth}×${settings.screenHeight} • ${if (settings.enableVSync) "VSync ON" else "VSync OFF"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
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
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val unit = 1024
    val exp = (Math.log(bytes.toDouble()) / Math.log(unit.toDouble())).toInt()
    val pre = "KMGTPE"[exp - 1]
    return String.format("%.1f %sB", bytes / Math.pow(unit.toDouble(), exp.toDouble()), pre)
}
