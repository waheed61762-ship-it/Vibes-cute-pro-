package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FilterBAndW
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Vignette
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ClipTextOverlay
import com.example.model.ClipTransition
import com.example.model.ColorAdjustments
import com.example.model.EditorToolTab
import com.example.model.SpecialEffect
import com.example.model.VideoClip
import com.example.model.VideoFilter
import com.example.ui.formatTimecode
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
import com.example.ui.theme.VioletGlow

@Composable
fun EditorToolDock(
    activeTab: EditorToolTab,
    selectedClip: VideoClip,
    selectedClipIndex: Int,
    totalClips: Int,
    onSelectTab: (EditorToolTab) -> Unit,
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
    onTransitionChange: (ClipTransition) -> Unit,
    onTextOverlayChange: (ClipTextOverlay?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CarbonSurface),
    ) {
        // Tool panel content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 128.dp, max = 196.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            when (activeTab) {
                EditorToolTab.EDIT -> EditToolPanel(
                    clip = selectedClip,
                    clipIndex = selectedClipIndex,
                    totalClips = totalClips,
                    onTrimChange = onTrimChange,
                    onSplitAtPlayhead = onSplitAtPlayhead,
                    onSpeedChange = onSpeedChange,
                    onToggleMute = onToggleMute,
                    onRotate90 = onRotate90,
                    onMoveClip = onMoveClip,
                    onDuplicateClip = onDuplicateClip,
                    onDeleteClip = onDeleteClip,
                )
                EditorToolTab.FILTERS -> FiltersToolPanel(
                    clip = selectedClip,
                    onFilterChange = onFilterChange,
                    onApplyToAll = onApplyFilterToAll,
                )
                EditorToolTab.ADJUST -> AdjustToolPanel(
                    adjustments = selectedClip.adjustments,
                    onAdjustmentsChange = onAdjustmentsChange,
                    onReset = onResetAdjustments,
                )
                EditorToolTab.EFFECTS -> SpecialEffectsToolPanel(
                    clip = selectedClip,
                    onEffectChange = onSpecialEffectChange,
                    onApplyToAll = onApplyEffectToAll,
                )
                EditorToolTab.TRANSITION -> TransitionToolPanel(
                    clip = selectedClip,
                    clipIndex = selectedClipIndex,
                    onTransitionChange = onTransitionChange,
                )
                EditorToolTab.TEXT -> TextOverlayToolPanel(
                    clip = selectedClip,
                    onTextOverlayChange = onTextOverlayChange,
                )
            }
        }

        // Bottom Tool Navigation Strip
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(ObsidianBg)
                .border(1.dp, HairlineBorder)
                .padding(vertical = 4.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(EditorToolTab.entries) { tab ->
                val selected = tab == activeTab
                val icon = when (tab) {
                    EditorToolTab.EDIT -> Icons.Rounded.ContentCut
                    EditorToolTab.FILTERS -> Icons.Rounded.Palette
                    EditorToolTab.ADJUST -> Icons.Rounded.Tune
                    EditorToolTab.EFFECTS -> Icons.Rounded.AutoAwesome
                    EditorToolTab.TRANSITION -> Icons.AutoMirrored.Rounded.CompareArrows
                    EditorToolTab.TEXT -> Icons.Rounded.TextFields
                }
                Surface(
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectTab(tab) }
                        .testTag("tool_tab_${tab.name.lowercase()}"),
                    color = if (selected) ElectricCyanContainer else Color.Transparent,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = tab.label,
                            tint = if (selected) ElectricCyan else TextSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) ElectricCyan else TextSecondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditToolPanel(
    clip: VideoClip,
    clipIndex: Int,
    totalClips: Int,
    onTrimChange: (Long, Long, Boolean) -> Unit,
    onSplitAtPlayhead: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onRotate90: () -> Unit,
    onMoveClip: (Int) -> Unit,
    onDuplicateClip: () -> Unit,
    onDeleteClip: () -> Unit,
) {
    val speedOptions = listOf(0.5f, 0.75f, 1.0f, 1.5f, 2.0f, 3.0f)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Trim Range Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Trim Clip #${clipIndex + 1}",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = "${formatTimecode(clip.trimStartMs)} – ${formatTimecode(clip.trimEndMs)}",
                style = MaterialTheme.typography.labelSmall,
                color = ElectricCyan,
            )
        }
        RangeSlider(
            value = clip.trimStartMs.toFloat()..clip.trimEndMs.toFloat(),
            onValueChange = { range ->
                val start = range.start.toLong()
                val end = range.endInclusive.toLong()
                if (end - start >= 300L) {
                    onTrimChange(start, end, true)
                }
            },
            valueRange = 0f..clip.sourceDurationMs.toFloat().coerceAtLeast(300f),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("trim_range_slider"),
            colors = SliderDefaults.colors(
                thumbColor = ElectricCyan,
                activeTrackColor = ElectricCyan,
                inactiveTrackColor = CarbonTrack,
            ),
        )

        // Action Pills Row
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                ToolActionChip(
                    icon = Icons.Rounded.ContentCut,
                    label = "Split",
                    accent = NeonCoral,
                    onClick = onSplitAtPlayhead,
                    modifier = Modifier.testTag("split_clip_button"),
                )
            }
            item {
                ToolActionChip(
                    icon = if (clip.isMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                    label = if (clip.isMuted) "Muted" else "Audio",
                    accent = if (clip.isMuted) NeonCoral else ElectricCyan,
                    onClick = onToggleMute,
                    modifier = Modifier.testTag("mute_clip_button"),
                )
            }
            item {
                ToolActionChip(
                    icon = Icons.AutoMirrored.Rounded.RotateRight,
                    label = "Rotate",
                    accent = ElectricCyan,
                    onClick = onRotate90,
                    modifier = Modifier.testTag("rotate_clip_button"),
                )
            }
            item {
                ToolActionChip(
                    icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    label = "Move L",
                    accent = TextSecondary,
                    enabled = clipIndex > 0,
                    onClick = { onMoveClip(-1) },
                    modifier = Modifier.testTag("move_left_button"),
                )
            }
            item {
                ToolActionChip(
                    icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    label = "Move R",
                    accent = TextSecondary,
                    enabled = clipIndex < totalClips - 1,
                    onClick = { onMoveClip(1) },
                    modifier = Modifier.testTag("move_right_button"),
                )
            }
            item {
                ToolActionChip(
                    icon = Icons.Rounded.ContentCopy,
                    label = "Duplicate",
                    accent = VioletGlow,
                    onClick = onDuplicateClip,
                    modifier = Modifier.testTag("duplicate_clip_button"),
                )
            }
            item {
                ToolActionChip(
                    icon = Icons.Rounded.DeleteOutline,
                    label = "Delete",
                    accent = NeonCoral,
                    enabled = totalClips > 1,
                    onClick = onDeleteClip,
                    modifier = Modifier.testTag("delete_clip_button"),
                )
            }
        }

        // Speed Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Speed",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(speedOptions) { spd ->
                    val selected = kotlin.math.abs(clip.speed - spd) < 0.05f
                    FilterChip(
                        selected = selected,
                        onClick = { onSpeedChange(spd) },
                        label = { Text("${spd}x", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricCyanContainer,
                            selectedLabelColor = ElectricCyan,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun FiltersToolPanel(
    clip: VideoClip,
    onFilterChange: (VideoFilter, Float) -> Unit,
    onApplyToAll: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Visual Filters", style = MaterialTheme.typography.titleSmall)
            TextButton(onClick = onApplyToAll) {
                Text("Apply to all clips", color = ElectricCyan)
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(VideoFilter.entries) { filter ->
                val selected = clip.filter == filter
                val tintColor = Color(filter.badgeColorHex)
                Surface(
                    modifier = Modifier
                        .width(84.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onFilterChange(filter, clip.filterIntensity) }
                        .testTag("filter_chip_${filter.name.lowercase()}"),
                    color = if (selected) ElectricCyanContainer else CarbonElevated,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) ElectricCyan else HairlineBorder,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(tintColor, tintColor.copy(alpha = 0.35f)),
                                    ),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (filter == VideoFilter.BLACK_AND_WHITE) {
                                Icon(
                                    imageVector = Icons.Rounded.FilterBAndW,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        Text(
                            text = filter.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) ElectricCyan else Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        if (clip.filter != VideoFilter.NONE) {
            LabeledSliderRow(
                label = "Intensity",
                value = clip.filterIntensity,
                range = 0f..1f,
                displayValue = "${(clip.filterIntensity * 100).toInt()}%",
                onValueChange = { onFilterChange(clip.filter, it) },
            )
        }
    }
}

@Composable
private fun AdjustToolPanel(
    adjustments: ColorAdjustments,
    onAdjustmentsChange: (ColorAdjustments) -> Unit,
    onReset: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Color Grading", style = MaterialTheme.typography.titleSmall)
            TextButton(onClick = onReset) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Reset", color = ElectricCyan)
            }
        }

        LabeledSliderRow(
            label = "Brightness",
            value = adjustments.brightness,
            range = -0.45f..0.45f,
            displayValue = String.format("%+.2f", adjustments.brightness),
            onValueChange = { onAdjustmentsChange(adjustments.copy(brightness = it)) },
        )
        LabeledSliderRow(
            label = "Contrast",
            value = adjustments.contrast,
            range = -0.45f..0.60f,
            displayValue = String.format("%+.2f", adjustments.contrast),
            onValueChange = { onAdjustmentsChange(adjustments.copy(contrast = it)) },
        )
        LabeledSliderRow(
            label = "Saturation",
            value = adjustments.saturation,
            range = -1.0f..1.0f,
            displayValue = String.format("%+.2f", adjustments.saturation),
            onValueChange = { onAdjustmentsChange(adjustments.copy(saturation = it)) },
        )
    }
}

@Composable
private fun SpecialEffectsToolPanel(
    clip: VideoClip,
    onEffectChange: (SpecialEffect, Float) -> Unit,
    onApplyToAll: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Special FX (GPU Shaders)", style = MaterialTheme.typography.titleSmall)
            TextButton(onClick = onApplyToAll) {
                Text("Apply to all", color = ElectricCyan)
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(SpecialEffect.entries) { fx ->
                val selected = clip.specialEffect == fx
                val icon = when (fx) {
                    SpecialEffect.NONE -> Icons.Rounded.AutoAwesome
                    SpecialEffect.BLUR -> Icons.Rounded.BlurOn
                    SpecialEffect.GLITCH -> Icons.Rounded.BrokenImage
                    SpecialEffect.VIGNETTE -> Icons.Rounded.Vignette
                    SpecialEffect.PIXELATE -> Icons.Rounded.GridOn
                    SpecialEffect.BEAT_ZOOM -> Icons.Rounded.ZoomIn
                }
                Surface(
                    modifier = Modifier
                        .width(96.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onEffectChange(fx, clip.effectIntensity) }
                        .testTag("fx_chip_${fx.name.lowercase()}"),
                    color = if (selected) NeonCoral.copy(alpha = 0.20f) else CarbonElevated,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) NeonCoral else HairlineBorder,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = fx.displayName,
                            tint = if (selected) NeonCoral else TextSecondary,
                            modifier = Modifier.size(24.dp),
                        )
                        Text(
                            text = fx.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) NeonCoral else Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        if (clip.specialEffect != SpecialEffect.NONE) {
            LabeledSliderRow(
                label = "FX Power",
                value = clip.effectIntensity,
                range = 0.1f..1.0f,
                displayValue = "${(clip.effectIntensity * 100).toInt()}%",
                onValueChange = { onEffectChange(clip.specialEffect, it) },
            )
        }
    }
}

@Composable
private fun TransitionToolPanel(
    clip: VideoClip,
    clipIndex: Int,
    onTransitionChange: (ClipTransition) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = if (clipIndex == 0) {
                "Intro Transition (Clip #1)"
            } else {
                "Transition into Clip #${clipIndex + 1}"
            },
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = "Applies smoothly at the start boundary of the selected clip.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(ClipTransition.entries) { trans ->
                val selected = clip.transitionIn == trans
                val icon = when (trans) {
                    ClipTransition.NONE -> Icons.Rounded.ContentCut
                    ClipTransition.FADE_BLACK -> Icons.AutoMirrored.Rounded.CompareArrows
                    ClipTransition.FLASH_WHITE -> Icons.Rounded.FlashOn
                    ClipTransition.ZOOM_IN -> Icons.Rounded.ZoomIn
                    ClipTransition.SLIDE_LEFT -> Icons.AutoMirrored.Rounded.KeyboardArrowLeft
                }
                Surface(
                    modifier = Modifier
                        .width(96.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onTransitionChange(trans) }
                        .testTag("transition_chip_${trans.name.lowercase()}"),
                    color = if (selected) ElectricCyanContainer else CarbonElevated,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) ElectricCyan else HairlineBorder,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = trans.displayName,
                            tint = if (selected) ElectricCyan else TextSecondary,
                            modifier = Modifier.size(22.dp),
                        )
                        Text(
                            text = trans.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) ElectricCyan else Color.White,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TextOverlayToolPanel(
    clip: VideoClip,
    onTextOverlayChange: (ClipTextOverlay?) -> Unit,
) {
    var draftText by remember(clip.id) { mutableStateOf(clip.textOverlay?.text ?: "") }
    var selectedColor by remember(clip.id) {
        mutableStateOf(clip.textOverlay?.colorArgb ?: 0xFFFFFFFF.toInt())
    }
    var verticalPos by remember(clip.id) {
        mutableStateOf(clip.textOverlay?.verticalPosition ?: -0.68f)
    }

    val swatches = listOf(
        0xFFFFFFFF.toInt() to Color.White,
        0xFF22D3EE.toInt() to ElectricCyan,
        0xFFFB7185.toInt() to NeonCoral,
        0xFFFBBF24.toInt() to AmberGold,
        0xFF34D399.toInt() to LuminousMint,
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = draftText,
                onValueChange = { draftText = it },
                placeholder = { Text("Type caption overlay...") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("text_overlay_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = HairlineBorder,
                ),
            )
            OutlinedButton(
                onClick = {
                    if (draftText.isBlank()) {
                        onTextOverlayChange(null)
                    } else {
                        onTextOverlayChange(
                            ClipTextOverlay(
                                text = draftText.trim(),
                                colorArgb = selectedColor,
                                verticalPosition = verticalPos,
                            ),
                        )
                    }
                },
                modifier = Modifier
                    .height(52.dp)
                    .testTag("apply_text_button"),
            ) {
                Text("Apply", color = ElectricCyan)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                swatches.forEach { (argb, composeColor) ->
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(composeColor)
                            .border(
                                width = if (selectedColor == argb) 2.5.dp else 1.dp,
                                color = if (selectedColor == argb) ElectricCyan else HairlineBorder,
                                shape = CircleShape,
                            )
                            .clickable {
                                selectedColor = argb
                                if (draftText.isNotBlank()) {
                                    onTextOverlayChange(
                                        ClipTextOverlay(
                                            text = draftText.trim(),
                                            colorArgb = argb,
                                            verticalPosition = verticalPos,
                                        ),
                                    )
                                }
                            },
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Top" to 0.68f, "Mid" to 0f, "Low" to -0.68f).forEach { (lbl, pos) ->
                    FilterChip(
                        selected = kotlin.math.abs(verticalPos - pos) < 0.1f,
                        onClick = {
                            verticalPos = pos
                            if (draftText.isNotBlank()) {
                                onTextOverlayChange(
                                    ClipTextOverlay(
                                        text = draftText.trim(),
                                        colorArgb = selectedColor,
                                        verticalPosition = pos,
                                    ),
                                )
                            }
                        },
                        label = { Text(lbl, style = MaterialTheme.typography.labelMedium) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolActionChip(
    icon: ImageVector,
    label: String,
    accent: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
        color = CarbonElevated.copy(alpha = if (enabled) 1f else 0.45f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) accent else TextMuted,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) Color.White else TextMuted,
            )
        }
    }
}

@Composable
private fun LabeledSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    displayValue: String,
    onValueChange: (Float) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            modifier = Modifier.width(74.dp),
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = ElectricCyan,
                activeTrackColor = ElectricCyan,
                inactiveTrackColor = CarbonTrack,
            ),
        )
        Text(
            text = displayValue,
            style = MaterialTheme.typography.labelSmall,
            color = ElectricCyan,
            modifier = Modifier.width(48.dp),
        )
    }
}
