package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.CompositionPlayer
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import com.example.R
import com.example.engine.CompositionBuilder
import com.example.model.AspectRatioPreset
import com.example.model.ClipTextOverlay
import com.example.model.ClipTransition
import com.example.model.ColorAdjustments
import com.example.model.DeviceEncoderCapabilities
import com.example.model.EditorToolTab
import com.example.model.ExportConfig
import com.example.model.ExportUiState
import com.example.model.ProjectDraft
import com.example.model.SpecialEffect
import com.example.model.VideoFilter
import com.example.ui.components.EditorToolDock
import com.example.ui.components.TimelineTrack
import com.example.ui.theme.CarbonElevated
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextSecondary

@OptIn(UnstableApi::class)
@Composable
fun EditorScreen(
    project: ProjectDraft,
    player: CompositionPlayer?,
    selectedClipIndex: Int,
    activeTab: EditorToolTab,
    isPlaying: Boolean,
    currentPositionMs: Long,
    capabilities: DeviceEncoderCapabilities,
    exportConfig: ExportConfig,
    exportState: ExportUiState,
    onBack: () -> Unit,
    onPausePlayback: () -> Unit,
    onPlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onStepRelative: (Long) -> Unit,
    onSelectClip: (Int) -> Unit,
    onSelectTab: (EditorToolTab) -> Unit,
    onAspectRatioChange: (AspectRatioPreset) -> Unit,
    onAddClipsClick: () -> Unit,
    onTrimChange: (Long, Long, Boolean) -> Unit,
    onSplitAtPlayhead: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onRotate90: () -> Unit,
    onMoveClip: (Int) -> Unit,
    onDuplicateClip: () -> Unit,
    onDeleteClip: () -> Unit,
    onFilterChange: (VideoFilter, Float) -> Unit,
    onApplyFilterToAll: () -> Unit,
    onAdjustmentsChange: (ColorAdjustments) -> Unit,
    onResetAdjustments: () -> Unit,
    onSpecialEffectChange: (SpecialEffect, Float) -> Unit,
    onApplyEffectToAll: () -> Unit,
    onTransitionChange: (Int, ClipTransition) -> Unit,
    onTextOverlayChange: (ClipTextOverlay?) -> Unit,
    onUpdateExportConfig: ((ExportConfig) -> ExportConfig) -> Unit,
    onStartExport: () -> Unit,
    onCancelExport: () -> Unit,
    onResetExportState: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler { onBack() }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                onPausePlayback()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var showExportSheet by remember { mutableStateOf(false) }
    var showAspectMenu by remember { mutableStateOf(false) }

    val selectedClip = project.clips.getOrNull(selectedClipIndex)
        ?: project.clips.firstOrNull()
        ?: return

    val previewRatio = CompositionBuilder.resolveProjectAspectRatio(project).coerceIn(0.45f, 2.2f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Top Studio Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("editor_back_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back to projects",
                        tint = Color.White,
                    )
                }
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box {
                    Surface(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showAspectMenu = true }
                            .testTag("aspect_ratio_button"),
                        color = CarbonElevated,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AspectRatio,
                                contentDescription = "Aspect ratio",
                                tint = ElectricCyan,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = project.aspectRatio.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showAspectMenu,
                        onDismissRequest = { showAspectMenu = false },
                        containerColor = CarbonSurface,
                    ) {
                        AspectRatioPreset.entries.forEach { preset ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = preset.label,
                                            color = if (preset == project.aspectRatio) ElectricCyan else Color.White,
                                            style = MaterialTheme.typography.titleSmall,
                                        )
                                        Text(
                                            text = preset.subtitle,
                                            color = TextSecondary,
                                            style = MaterialTheme.typography.labelMedium,
                                        )
                                    }
                                },
                                onClick = {
                                    showAspectMenu = false
                                    onAspectRatioChange(preset)
                                },
                            )
                        }
                    }
                }

                Button(
                    onClick = { showExportSheet = true },
                    modifier = Modifier
                        .height(42.dp)
                        .testTag("open_export_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = ObsidianBg,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FileUpload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.action_export),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }
        }

        // Video Preview Viewport
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            val containerRatio = if (maxHeight > 0.dp) maxWidth / maxHeight else 1f
            val frameSizing = if (previewRatio > containerRatio) {
                Modifier.fillMaxWidth().aspectRatio(previewRatio)
            } else {
                Modifier.fillMaxHeight().aspectRatio(previewRatio)
            }
            Box(
                modifier = frameSizing
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(16.dp))
                    .clickable(onClick = onPlayPause),
                contentAlignment = Alignment.Center,
            ) {
                ContentFrame(
                    player = player,
                    modifier = Modifier.fillMaxSize(),
                    surfaceType = SURFACE_TYPE_SURFACE_VIEW,
                    contentScale = ContentScale.Fit,
                    keepContentOnReset = true,
                )

                // Active filter / FX live badge on preview corner
                if (selectedClip.filter.name != "NONE" || selectedClip.specialEffect.name != "NONE") {
                    Surface(
                        color = Color.Black.copy(alpha = 0.60f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                    ) {
                        val badgeText = buildString {
                            if (selectedClip.filter.name != "NONE") append(selectedClip.filter.displayName)
                            if (selectedClip.filter.name != "NONE" && selectedClip.specialEffect.name != "NONE") {
                                append(" • ")
                            }
                            if (selectedClip.specialEffect.name != "NONE") append(selectedClip.specialEffect.displayName)
                        }
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }

        // Transport Controls Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { onStepRelative(-1000L) },
                modifier = Modifier.testTag("rewind_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.FastRewind,
                    contentDescription = "Step back 1 second",
                    tint = TextSecondary,
                )
            }
            Spacer(Modifier.width(12.dp))
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onPlayPause)
                    .testTag("play_pause_button"),
                color = ElectricCyan,
                shape = CircleShape,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(R.string.action_play_pause),
                        tint = ObsidianBg,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            IconButton(
                onClick = { onStepRelative(1000L) },
                modifier = Modifier.testTag("forward_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.FastForward,
                    contentDescription = "Step forward 1 second",
                    tint = TextSecondary,
                )
            }
        }

        // Multi-Clip Timeline
        TimelineTrack(
            clips = project.clips,
            selectedClipIndex = selectedClipIndex,
            currentPositionMs = currentPositionMs,
            totalDurationMs = project.totalDurationMs,
            onSeekTo = onSeekTo,
            onSelectClip = onSelectClip,
            onTransitionClick = { clipIdx ->
                onSelectClip(clipIdx)
                onSelectTab(EditorToolTab.TRANSITION)
            },
            onAddClipsClick = onAddClipsClick,
        )

        // Contextual Tool Dock
        EditorToolDock(
            activeTab = activeTab,
            selectedClip = selectedClip,
            selectedClipIndex = selectedClipIndex,
            totalClips = project.clips.size,
            onSelectTab = onSelectTab,
            onTrimChange = onTrimChange,
            onSplitAtPlayhead = onSplitAtPlayhead,
            onSpeedChange = onSpeedChange,
            onToggleMute = onToggleMute,
            onRotate90 = onRotate90,
            onMoveClip = onMoveClip,
            onDuplicateClip = onDuplicateClip,
            onDeleteClip = onDeleteClip,
            onFilterChange = onFilterChange,
            onApplyFilterToAll = onApplyFilterToAll,
            onAdjustmentsChange = onAdjustmentsChange,
            onResetAdjustments = onResetAdjustments,
            onSpecialEffectChange = onSpecialEffectChange,
            onApplyEffectToAll = onApplyEffectToAll,
            onTransitionChange = { trans -> onTransitionChange(selectedClipIndex, trans) },
            onTextOverlayChange = onTextOverlayChange,
        )
    }

    if (showExportSheet) {
        ExportBottomSheet(
            project = project,
            config = exportConfig,
            exportState = exportState,
            capabilities = capabilities,
            onUpdateConfig = onUpdateExportConfig,
            onStartExport = onStartExport,
            onCancelExport = onCancelExport,
            onDismiss = {
                onResetExportState()
                showExportSheet = false
            },
        )
    }
}
