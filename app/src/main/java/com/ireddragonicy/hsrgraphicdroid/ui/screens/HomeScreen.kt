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
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header Card
        InfoCard(
            title = stringResource(R.string.app_name),
            subtitle = stringResource(R.string.quick_actions)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusChip(
                    text = if (status.isChecking) stringResource(R.string.checking)
                    else if (status.isRootGranted) stringResource(R.string.root_granted)
                    else stringResource(R.string.root_not_granted),
                    isSuccess = status.isRootGranted,
                    isLoading = status.isChecking
                )
                StatusChip(
                    text = if (status.isChecking) stringResource(R.string.checking)
                    else if (status.isGameInstalled) stringResource(R.string.game_found)
                    else stringResource(R.string.game_not_found),
                    isSuccess = status.isGameInstalled,
                    isLoading = status.isChecking
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Game Info
            status.gameVersion?.let { version ->
                Text(
                    text = "Version: $version",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            gameInfo?.let { info ->
                info.apkName?.let { apk ->
                    Text(
                        text = "APK: $apk",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                if (info.dataBytes != null && info.cacheBytes != null) {
                    Text(
                        text = "Data: ${formatBytes(info.dataBytes)} • Cache: ${formatBytes(info.cacheBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            status.configPath?.let { path ->
                Text(
                    text = "Config Path: $path",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Actions Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.quick_actions),
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

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

        Spacer(modifier = Modifier.height(16.dp))

        // Current Settings Preview
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.graphics_settings),
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = stringResource(R.string.overall_graphics_quality),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

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

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val unit = 1024
    val exp = (Math.log(bytes.toDouble()) / Math.log(unit.toDouble())).toInt()
    val pre = "KMGTPE"[exp - 1]
    return String.format("%.1f %sB", bytes / Math.pow(unit.toDouble(), exp.toDouble()), pre)
}
