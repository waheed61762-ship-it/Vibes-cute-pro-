package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AdPlacementType
import com.example.model.BatchGenerationState
import com.example.model.ClipDiscoveryMode
import com.example.model.ClipLengthOption
import com.example.model.CropMode
import com.example.model.MonetizationConfig
import com.example.model.PlannedClipSegment
import com.example.model.ResolvedLinkMedia
import com.example.model.SocialAspectFormat
import com.example.model.StudioQualityTier
import com.example.model.UserProfile
import com.example.model.VideoClip
import com.example.ui.components.AspectRatioPreviewTile
import com.example.ui.components.StatusPillBadge
import com.example.ui.components.StudioAmbientBackground
import com.example.ui.components.StudioSectionHeader
import com.example.ui.components.StudioTimelineWaveformCanvas
import com.example.ui.components.VibeCutGlassCard
import com.example.ui.components.VibeCutGradientButton
import com.example.ui.formatDurationShort
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
fun StudioTabScreen(
    linkInput: String,
    isResolvingLink: Boolean,
    isLoadingMedia: Boolean,
    resolvedLinkMedia: ResolvedLinkMedia?,
    sourceClip: VideoClip?,
    clipLength: ClipLengthOption,
    aspectFormat: SocialAspectFormat,
    clipCount: Int,
    discoveryMode: ClipDiscoveryMode,
    cropMode: CropMode,
    qualityTier: StudioQualityTier,
    watermarkEnabled: Boolean,
    plannedSegments: List<PlannedClipSegment>,
    batchState: BatchGenerationState,
    userProfile: UserProfile?,
    monetizationConfig: MonetizationConfig,
    hasBatchAccess: Boolean,
    hasQualityAccess: Boolean,
    hasWatermarkAccess: Boolean,
    onLinkInputChange: (String) -> Unit,
    onInspectLink: () -> Unit,
    onPickGalleryVideo: () -> Unit,
    onSelectClipLength: (ClipLengthOption) -> Unit,
    onSelectAspectFormat: (SocialAspectFormat) -> Unit,
    onSelectClipCount: (Int) -> Unit,
    onSelectDiscoveryMode: (ClipDiscoveryMode) -> Unit,
    onSelectCropMode: (CropMode) -> Unit,
    onSelectQualityTier: (StudioQualityTier) -> Unit,
    onToggleWatermark: (Boolean) -> Unit,
    onRequestUnlock: (AdPlacementType, () -> Unit) -> Unit,
    onGenerateClips: () -> Unit,
    onResetBatchState: () -> Unit,
    onOpenFullEditor: () -> Unit,
    onViewMyClips: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    StudioAmbientBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Pro Studio Hero Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(166.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(ElectricCyan.copy(alpha = 0.55f), VioletGlow.copy(alpha = 0.35f)),
                        ),
                        shape = RoundedCornerShape(24.dp),
                    ),
            ) {
                Image(
                    painter = painterResource(R.drawable.img_vibecut_hero_1795454760936),
                    contentDescription = "VibeCut Studio header",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    ObsidianBg.copy(alpha = 0.94f),
                                    ObsidianBg.copy(alpha = 0.62f),
                                    ObsidianBg.copy(alpha = 0.35f),
                                ),
                            ),
                        ),
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StatusPillBadge(
                            text = "VIBECUT STUDIO v2.1",
                            accentColor = ElectricCyan,
                            leadingDot = true,
                        )
                        StatusPillBadge(
                            text = if (monetizationConfig.adSupportedMonetization) {
                                "Ad Mode • ${userProfile?.bonusCredits ?: 0} Cr"
                            } else if (userProfile?.subscriptionActive == true) {
                                "Pro Unlocked"
                            } else {
                                "Pro Plan Mode"
                            },
                            accentColor = AmberGold,
                        )
                    }

                    Column {
                        Text(
                            text = "Create Viral Short Clips",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "YouTube, TikTok, Reels & Gallery • 15s–10m • Up to 1440p UHD",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                        )
                    }
                }
            }

            // Source Media Card
            VibeCutGlassCard(
                accentColor = ElectricCyan,
                highlighted = sourceClip != null,
            ) {
                StudioSectionHeader(
                    title = "1. Load Source Video",
                    subtitle = "Paste a social/MP4 URL or pick from gallery",
                    icon = Icons.Rounded.Link,
                    accentColor = ElectricCyan,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = linkInput,
                        onValueChange = onLinkInputChange,
                        leadingIcon = { Icon(Icons.Rounded.Link, contentDescription = null, tint = ElectricCyan) },
                        placeholder = { Text("YouTube, TikTok, FB, IG or .mp4", color = TextMuted) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("studio_link_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = HairlineBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                        ),
                    )
                    VibeCutGradientButton(
                        text = "Load",
                        onClick = onInspectLink,
                        enabled = !isResolvingLink,
                        isLoading = isResolvingLink,
                        modifier = Modifier
                            .width(86.dp)
                            .testTag("load_link_button"),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onPickGalleryVideo,
                        enabled = !isLoadingMedia,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("pick_gallery_video_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.55f)),
                    ) {
                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp), tint = ElectricCyan)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isLoadingMedia) "Inspecting..." else "Pick Gallery Video",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    if (sourceClip != null) {
                        OutlinedButton(
                            onClick = onOpenFullEditor,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("open_timeline_editor_button"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, VioletGlow.copy(alpha = 0.65f)),
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp), tint = VioletGlow)
                            Spacer(Modifier.width(6.dp))
                            Text("Timeline Editor", color = VioletGlow, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                resolvedLinkMedia?.let { resolved ->
                    Surface(
                        color = ObsidianBg.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, HairlineBorder),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${resolved.platform.label} • ${resolved.title}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = resolved.statusNote,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (resolved.directStreamPlayable) LuminousMint else AmberGold,
                                )
                            }
                            if (!resolved.directStreamPlayable) {
                                OutlinedButton(
                                    onClick = {
                                        runCatching {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(resolved.originalUrl)))
                                        }
                                    },
                                    modifier = Modifier.height(40.dp),
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = "Open link", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                sourceClip?.let { clip ->
                    Surface(
                        color = ElectricCyanContainer.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(22.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(clip.displayName, style = MaterialTheme.typography.titleSmall, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${clip.width}×${clip.height} • ${formatDurationShort(clip.sourceDurationMs)} ready", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                            }
                        }
                    }
                }
            }

            // Duration & Aspect Ratio Card
            VibeCutGlassCard(accentColor = VioletGlow) {
                StudioSectionHeader(
                    title = "2. Clip Length & Aspect Format",
                    subtitle = "Choose target duration and social platform framing",
                    icon = Icons.Rounded.Timer,
                    accentColor = VioletGlow,
                )

                // Segmented Equalizer Pill Bar for Clip Length
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ObsidianBg.copy(alpha = 0.8f))
                        .border(1.dp, HairlineBorder, RoundedCornerShape(14.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    ClipLengthOption.entries.forEach { option ->
                        val selected = clipLength == option
                        val bg by animateColorAsState(
                            targetValue = if (selected) ElectricCyan else Color.Transparent,
                            label = "len_bg",
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(bg)
                                .clickable { onSelectClipLength(option) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                ),
                                color = if (selected) ObsidianBg else TextSecondary,
                            )
                        }
                    }
                }

                // Visual Aspect Ratio Frame Tiles
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SocialAspectFormat.entries.forEach { format ->
                        AspectRatioPreviewTile(
                            label = format.label,
                            ratioText = format.ratioLabel,
                            ratioValue = format.preset.ratio ?: 1f,
                            selected = aspectFormat == format,
                            onClick = { onSelectAspectFormat(format) },
                        )
                    }
                }
            }

            // Batch Count, Discovery Mode & Crop Mode Card
            VibeCutGlassCard(accentColor = ElectricCyan) {
                StudioSectionHeader(
                    title = "3. Discovery, Count & Framing",
                    subtitle = "1–2 clips free • 3–40 batch clips unlockable",
                    icon = Icons.Rounded.Tune,
                    accentColor = ElectricCyan,
                    trailing = {
                        StatusPillBadge(
                            text = "$clipCount Clips",
                            accentColor = if (clipCount <= 2 || hasBatchAccess) LuminousMint else AmberGold,
                        )
                    },
                )

                Slider(
                    value = clipCount.toFloat(),
                    onValueChange = { onSelectClipCount(it.toInt().coerceIn(1, 40)) },
                    valueRange = 1f..40f,
                    colors = SliderDefaults.colors(
                        thumbColor = ElectricCyan,
                        activeTrackColor = ElectricCyan,
                        inactiveTrackColor = CarbonTrack,
                    ),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(1, 2, 5, 10, 20, 40).forEach { count ->
                        FilterChip(
                            selected = clipCount == count,
                            onClick = { onSelectClipCount(count) },
                            label = { Text(if (count <= 2) "$count Free" else "$count Clips") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCyanContainer,
                                selectedLabelColor = ElectricCyan,
                            ),
                        )
                    }
                }

                if (!hasBatchAccess && clipCount > 2) {
                    OutlinedButton(
                        onClick = { onRequestUnlock(AdPlacementType.REWARDED_BATCH) {} },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("unlock_batch_clips_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.6f)),
                    ) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, tint = AmberGold, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (monetizationConfig.adSupportedMonetization) {
                                "Watch Ad to Unlock 3–40 Batch Clips"
                            } else {
                                "Unlock 3–40 Clips with VibeCut Pro"
                            },
                            color = AmberGold,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                Text("Clip Discovery Mode", style = MaterialTheme.typography.titleSmall, color = Color.White)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ClipDiscoveryMode.entries.forEach { mode ->
                        FilterChip(
                            selected = discoveryMode == mode,
                            onClick = { onSelectDiscoveryMode(mode) },
                            label = { Text(mode.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCyanContainer,
                                selectedLabelColor = ElectricCyan,
                            ),
                        )
                    }
                }
                Text(discoveryMode.description, style = MaterialTheme.typography.labelMedium, color = TextMuted)

                Text("Video Frame Crop Mode", style = MaterialTheme.typography.titleSmall, color = Color.White)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CropMode.entries.forEach { mode ->
                        val selected = cropMode == mode
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectCropMode(mode) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) ElectricCyanContainer else ObsidianBg.copy(alpha = 0.7f),
                            border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) ElectricCyan else HairlineBorder),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Crop,
                                    contentDescription = null,
                                    tint = if (selected) ElectricCyan else TextSecondary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Column {
                                    Text(mode.label, style = MaterialTheme.typography.titleSmall, color = Color.White)
                                    Text(mode.subtitle, style = MaterialTheme.typography.labelSmall, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }

            // Quality & Watermark Card
            VibeCutGlassCard(accentColor = AmberGold) {
                StudioSectionHeader(
                    title = "4. Export Quality & Watermark",
                    subtitle = "360p free • 720p HD, 1080p FHD & 1440p UHD",
                    icon = Icons.Rounded.HighQuality,
                    accentColor = AmberGold,
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StudioQualityTier.entries.forEach { tier ->
                        val locked = !tier.freeTier && !hasQualityAccess
                        FilterChip(
                            selected = qualityTier == tier,
                            onClick = {
                                if (locked) {
                                    onRequestUnlock(AdPlacementType.REWARDED_QUALITY) {
                                        onSelectQualityTier(tier)
                                    }
                                } else {
                                    onSelectQualityTier(tier)
                                }
                            },
                            leadingIcon = if (locked) {
                                { Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(14.dp), tint = AmberGold) }
                            } else null,
                            label = { Text(if (tier.freeTier) "${tier.label} Free" else tier.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCyanContainer,
                                selectedLabelColor = ElectricCyan,
                            ),
                        )
                    }
                }

                Surface(
                    color = ObsidianBg.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, HairlineBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("VibeCut Studio Watermark", style = MaterialTheme.typography.titleSmall, color = Color.White)
                            Text(
                                text = when {
                                    !watermarkEnabled -> "Watermark removed — clean studio export"
                                    hasWatermarkAccess -> "Switch OFF to export watermark-free"
                                    monetizationConfig.adSupportedMonetization -> "Turn OFF to unlock clean export via ad"
                                    else -> "Turn OFF with VibeCut Pro subscription"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = if (!watermarkEnabled) LuminousMint else TextMuted,
                            )
                        }
                        Switch(
                            checked = watermarkEnabled,
                            onCheckedChange = onToggleWatermark,
                            modifier = Modifier.testTag("studio_watermark_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ObsidianBg,
                                checkedTrackColor = ElectricCyan,
                            ),
                        )
                    }
                }
            }

            // Waveform & Timeline Preview Card
            VibeCutGlassCard(accentColor = ElectricCyan) {
                StudioSectionHeader(
                    title = "Timeline Cut Preview (${plannedSegments.size} Clips)",
                    subtitle = "${aspectFormat.label} • ${cropMode.label} • ${if (watermarkEnabled) "Watermark ON" else "No Watermark"}",
                    icon = Icons.Rounded.MovieCreation,
                    accentColor = ElectricCyan,
                )

                StudioTimelineWaveformCanvas(segments = plannedSegments)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    plannedSegments.take(12).forEach { seg ->
                        Surface(
                            color = ObsidianBg.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .width(146.dp)
                                .clickable(enabled = sourceClip != null) { onOpenFullEditor() },
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(seg.title, style = MaterialTheme.typography.labelLarge, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    text = "${formatDurationShort(seg.trimStartMs)} – ${formatDurationShort(seg.trimEndMs)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricCyan,
                                )
                                Text(
                                    text = seg.discoveryReason,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }

            // Batch Status / Primary Generate Action
            when (batchState) {
                is BatchGenerationState.Running -> {
                    VibeCutGlassCard(accentColor = ElectricCyan, highlighted = true) {
                        Text(batchState.statusLabel, style = MaterialTheme.typography.titleSmall, color = Color.White)
                        LinearProgressIndicator(
                            progress = { batchState.overallPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = ElectricCyan,
                            trackColor = CarbonTrack,
                        )
                        OutlinedButton(
                            onClick = onResetBatchState,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                        ) {
                            Text("Cancel Batch", color = NeonCoral)
                        }
                    }
                }

                is BatchGenerationState.Done -> {
                    VibeCutGlassCard(accentColor = LuminousMint, highlighted = true) {
                        Text(batchState.savedSummary, style = MaterialTheme.typography.titleSmall, color = LuminousMint)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedButton(
                                onClick = onResetBatchState,
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text("Dismiss")
                            }
                            VibeCutGradientButton(
                                text = "View My Clips",
                                onClick = {
                                    onResetBatchState()
                                    onViewMyClips()
                                },
                                icon = Icons.Rounded.PlayArrow,
                                modifier = Modifier.weight(1.2f),
                            )
                        }
                    }
                }

                is BatchGenerationState.Error -> {
                    VibeCutGlassCard(accentColor = NeonCoral, highlighted = true) {
                        Text(batchState.message, style = MaterialTheme.typography.bodyMedium, color = NeonCoral)
                        VibeCutGradientButton(
                            text = "Retry Batch Render",
                            onClick = onGenerateClips,
                            accentStart = NeonCoral,
                            accentEnd = AmberGold,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                is BatchGenerationState.Idle -> {
                    VibeCutGradientButton(
                        text = "Generate $clipCount ${aspectFormat.ratioLabel} Clip${if (clipCount == 1) "" else "s"} • ${qualityTier.label}",
                        onClick = onGenerateClips,
                        icon = Icons.Rounded.AutoAwesome,
                        accentStart = ElectricCyan,
                        accentEnd = VioletGlow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generate_clips_button"),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
        }
    }
}
