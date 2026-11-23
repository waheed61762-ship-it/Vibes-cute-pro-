package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ClipTransition
import com.example.model.SpecialEffect
import com.example.model.VideoClip
import com.example.model.VideoFilter
import com.example.ui.formatDurationShort
import com.example.ui.formatTimecode
import com.example.ui.theme.CarbonElevated
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CarbonTrack
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@Composable
fun TimelineTrack(
    clips: List<VideoClip>,
    selectedClipIndex: Int,
    currentPositionMs: Long,
    totalDurationMs: Long,
    onSeekTo: (Long) -> Unit,
    onSelectClip: (Int) -> Unit,
    onTransitionClick: (Int) -> Unit,
    onAddClipsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CarbonSurface)
            .border(1.dp, HairlineBorder)
            .padding(vertical = 8.dp),
    ) {
        // Playhead Scrubber Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatTimecode(currentPositionMs),
                style = MaterialTheme.typography.labelSmall,
                color = ElectricCyan,
            )
            Slider(
                value = if (totalDurationMs > 0L) {
                    (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                },
                onValueChange = { frac ->
                    onSeekTo((frac * totalDurationMs).toLong())
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
                    .testTag("timeline_scrubber"),
                colors = SliderDefaults.colors(
                    thumbColor = ElectricCyan,
                    activeTrackColor = ElectricCyan,
                    inactiveTrackColor = CarbonTrack,
                ),
            )
            Text(
                text = formatTimecode(totalDurationMs),
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
            )
        }

        // Multi-Clip Filmstrip Row with Transition Diamonds
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            itemsIndexed(clips, key = { _, clip -> clip.id }) { index, clip ->
                if (index > 0) {
                    TransitionJunctionButton(
                        transition = clip.transitionIn,
                        onClick = { onTransitionClick(index) },
                        modifier = Modifier.testTag("transition_junction_$index"),
                    )
                }

                val isSelected = index == selectedClipIndex
                val clipWidthDp = ((clip.effectiveDurationMs / 1000f) * 34f)
                    .coerceIn(104f, 220f)
                    .dp

                ClipFilmstripItem(
                    clip = clip,
                    index = index,
                    isSelected = isSelected,
                    widthDp = clipWidthDp,
                    onClick = { onSelectClip(index) },
                )
            }

            item {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    modifier = Modifier
                        .size(width = 50.dp, height = 56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onAddClipsClick)
                        .testTag("timeline_add_clip_button"),
                    color = CarbonElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.45f)),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Add clip",
                            tint = ElectricCyan,
                            modifier = Modifier.size(22.dp),
                        )
                        Text(
                            text = "Add",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricCyan,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransitionJunctionButton(
    transition: ClipTransition,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val active = transition != ClipTransition.NONE
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(30.dp),
            shape = RoundedCornerShape(8.dp),
            color = if (active) NeonCoral.copy(alpha = 0.22f) else CarbonElevated,
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = if (active) NeonCoral else HairlineBorder,
            ),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.CompareArrows,
                    contentDescription = "Clip transition: ${transition.displayName}",
                    tint = if (active) NeonCoral else TextSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun ClipFilmstripItem(
    clip: VideoClip,
    index: Int,
    isSelected: Boolean,
    widthDp: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    val borderColor = if (isSelected) ElectricCyan else HairlineBorder
    val borderWidth = if (isSelected) 2.dp else 1.dp

    Box(
        modifier = Modifier
            .width(widthDp)
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CarbonTrack)
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("timeline_clip_$index"),
    ) {
        if (clip.thumbnails.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxSize()) {
                clip.thumbnails.forEach { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (isSelected) 0.20f else 0.36f)),
        )

        // Top badges (Clip number, filter/FX badge, mute icon)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = ObsidianBg.copy(alpha = 0.78f),
                shape = RoundedCornerShape(6.dp),
            ) {
                Text(
                    text = "#${index + 1} ${formatDurationShort(clip.effectiveDurationMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    maxLines = 1,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                if (clip.filter != VideoFilter.NONE || clip.specialEffect != SpecialEffect.NONE) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = "Has filter or effect",
                        tint = ElectricCyan,
                        modifier = Modifier
                            .size(15.dp)
                            .background(ObsidianBg.copy(alpha = 0.75f), CircleShape)
                            .padding(2.dp),
                    )
                }
                if (clip.isMuted) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.VolumeOff,
                        contentDescription = "Muted",
                        tint = NeonCoral,
                        modifier = Modifier
                            .size(15.dp)
                            .background(ObsidianBg.copy(alpha = 0.75f), CircleShape)
                            .padding(2.dp),
                    )
                }
            }
        }

        // Bottom clip name
        Text(
            text = clip.displayName,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 6.dp, vertical = 4.dp),
        )
    }
}
