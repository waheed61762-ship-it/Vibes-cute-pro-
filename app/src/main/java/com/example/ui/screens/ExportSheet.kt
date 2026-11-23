package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.engine.CompositionBuilder
import com.example.model.DeviceEncoderCapabilities
import com.example.model.ExportConfig
import com.example.model.ExportResolution
import com.example.model.ExportUiState
import com.example.model.ProjectDraft
import com.example.model.VideoCodecOption
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CarbonElevated
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CarbonTrack
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanContainer
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.LuminousMint
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportBottomSheet(
    project: ProjectDraft,
    config: ExportConfig,
    exportState: ExportUiState,
    capabilities: DeviceEncoderCapabilities,
    onUpdateConfig: ((ExportConfig) -> ExportConfig) -> Unit,
    onStartExport: () -> Unit,
    onCancelExport: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = {
            if (exportState !is ExportUiState.Exporting) {
                onDismiss()
            }
        },
        sheetState = sheetState,
        containerColor = CarbonSurface,
        contentColor = Color.White,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Export Studio", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = capabilities.hardwareSummary,
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricCyan,
                    )
                }
                if (exportState !is ExportUiState.Exporting) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close export")
                    }
                }
            }

            when (exportState) {
                is ExportUiState.Idle -> {
                    Text("Resolution", style = MaterialTheme.typography.titleSmall)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExportResolution.entries.forEach { res ->
                            val selected = config.resolution == res
                            val (w, h) = CompositionBuilder.computeExportDimensions(project, res)
                            val supported = capabilities.supports(res, config.codec)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        onUpdateConfig { curr ->
                                            val recommendedCodec = if (res == ExportResolution.UHD_4K || res == ExportResolution.UHD_8K) {
                                                VideoCodecOption.H265
                                            } else {
                                                curr.codec
                                            }
                                            curr.copy(
                                                resolution = res,
                                                bitrateMbps = res.defaultBitrateMbps,
                                                codec = recommendedCodec,
                                            )
                                        }
                                    }
                                    .testTag("export_res_${res.name.lowercase()}"),
                                color = if (selected) ElectricCyanContainer else CarbonElevated,
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) ElectricCyan else HairlineBorder,
                                ),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = res.label,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = if (selected) ElectricCyan else Color.White,
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = "${w}×${h}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary,
                                            )
                                        }
                                        Text(
                                            text = if (supported) res.badge else "Exceeds reported hardware codec limit",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (supported) TextMuted else AmberGold,
                                        )
                                    }
                                    if (!supported) {
                                        Icon(
                                            imageVector = Icons.Rounded.WarningAmber,
                                            contentDescription = "Hardware warning",
                                            tint = AmberGold,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Codec & Bitrate
                    Text("Encoder Codec", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        VideoCodecOption.entries.forEach { codec ->
                            FilterChip(
                                selected = config.codec == codec,
                                onClick = { onUpdateConfig { it.copy(codec = codec) } },
                                label = { Text("${codec.label} • ${codec.note}") },
                            )
                        }
                    }

                    val estimatedMb = (project.totalDurationMs / 1000.0) * (config.bitrateMbps / 8.0)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Bitrate: ${config.bitrateMbps} Mbps", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = String.format(Locale.US, "Est. ~%.1f MB", estimatedMb),
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan,
                            )
                        }
                        Slider(
                            value = config.bitrateMbps.toFloat(),
                            onValueChange = { mbps ->
                                onUpdateConfig { it.copy(bitrateMbps = mbps.toInt()) }
                            },
                            valueRange = 4f..120f,
                            colors = SliderDefaults.colors(
                                thumbColor = ElectricCyan,
                                activeTrackColor = ElectricCyan,
                                inactiveTrackColor = CarbonTrack,
                            ),
                        )
                    }

                    Button(
                        onClick = onStartExport,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("start_export_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = ObsidianBg,
                        ),
                    ) {
                        Text(
                            text = "Render ${config.resolution.label} MP4",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                }

                is ExportUiState.Exporting -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { exportState.progressPercent / 100f },
                                modifier = Modifier.size(96.dp),
                                color = ElectricCyan,
                                trackColor = CarbonTrack,
                                strokeWidth = 6.dp,
                            )
                            Text(
                                text = "${exportState.progressPercent}%",
                                style = MaterialTheme.typography.titleLarge,
                                color = ElectricCyan,
                            )
                        }
                        Text(
                            text = exportState.statusLabel,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        LinearProgressIndicator(
                            progress = { exportState.progressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = ElectricCyan,
                            trackColor = CarbonTrack,
                        )
                        Text(
                            text = "Elapsed: ${exportState.elapsedSeconds}s • Hardware MediaCodec",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                        )
                        OutlinedButton(
                            onClick = onCancelExport,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("cancel_export_button"),
                        ) {
                            Text("Cancel Render", color = NeonCoral)
                        }
                    }
                }

                is ExportUiState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(LuminousMint.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Export complete",
                                tint = LuminousMint,
                                modifier = Modifier.size(38.dp),
                            )
                        }
                        Text("Export Complete!", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "${exportState.resolutionLabel} • ${String.format(Locale.US, "%.1f MB", exportState.fileSizeMb)} • ${exportState.elapsedSeconds}s",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricCyan,
                        )
                        Surface(
                            color = CarbonElevated,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
                        ) {
                            Text(
                                text = "Saved to ${exportState.displayPath}",
                                style = MaterialTheme.typography.labelSmall,
                                color = LuminousMint,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val uri = Uri.parse(exportState.outputUriString)
                                    val playIntent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "video/mp4")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    runCatching { context.startActivity(playIntent) }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Play")
                            }
                            Button(
                                onClick = {
                                    val uri = Uri.parse(exportState.outputUriString)
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "video/mp4"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    runCatching {
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricCyan,
                                    contentColor = ObsidianBg,
                                ),
                            ) {
                                Icon(Icons.Rounded.Share, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Share")
                            }
                        }
                    }
                }

                is ExportUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = "Export error",
                            tint = NeonCoral,
                            modifier = Modifier.size(44.dp),
                        )
                        Text("Export Failed", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = exportState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                        Button(
                            onClick = onStartExport,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = ObsidianBg,
                            ),
                        ) {
                            Text("Retry Export")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
