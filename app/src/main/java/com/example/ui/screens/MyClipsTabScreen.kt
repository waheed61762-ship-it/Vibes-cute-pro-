package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import com.example.model.GeneratedClipRecord
import com.example.ui.UiState
import com.example.ui.components.MetricStatPill
import com.example.ui.components.StatusPillBadge
import com.example.ui.components.StudioAmbientBackground
import com.example.ui.components.VibeCutGlassCard
import com.example.ui.components.VibeCutGradientButton
import com.example.ui.formatDurationShort
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanContainer
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.LuminousMint
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletGlow
import java.util.Locale

@Composable
fun MyClipsTabScreen(
    clipsState: UiState<List<GeneratedClipRecord>>,
    onOpenInEditor: (GeneratedClipRecord) -> Unit,
    onDeleteClip: (String) -> Unit,
    onSwitchToStudio: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var ratioFilter by remember { mutableStateOf("All") }
    var previewClip by remember { mutableStateOf<GeneratedClipRecord?>(null) }

    StudioAmbientBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "My Clips",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                    )
                    Text(
                        text = "Manage, preview, edit & share generated cuts",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                }
                if (clipsState is UiState.Success) {
                    StatusPillBadge(
                        text = "${clipsState.data.size} Saved",
                        accentColor = ElectricCyan,
                        leadingDot = true,
                    )
                }
            }

            if (clipsState is UiState.Success && clipsState.data.isNotEmpty()) {
                val hdCount = clipsState.data.count { it.resolutionLabel != "360p" }
                val cleanCount = clipsState.data.count { !it.watermarkApplied }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MetricStatPill(
                        value = clipsState.data.size.toString(),
                        label = "Total Clips",
                        accentColor = ElectricCyan,
                        modifier = Modifier.weight(1f),
                    )
                    MetricStatPill(
                        value = hdCount.toString(),
                        label = "HD / UHD",
                        accentColor = VioletGlow,
                        modifier = Modifier.weight(1f),
                    )
                    MetricStatPill(
                        value = cleanCount.toString(),
                        label = "Clean Exports",
                        accentColor = LuminousMint,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = ElectricCyan) },
                placeholder = { Text("Search generated clips...", color = TextMuted) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("clips_search_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = HairlineBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                ),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("All", "9:16", "4:5", "1:1", "16:9").forEach { ratio ->
                    FilterChip(
                        selected = ratioFilter == ratio,
                        onClick = { ratioFilter = ratio },
                        label = { Text(ratio) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricCyanContainer,
                            selectedLabelColor = ElectricCyan,
                        ),
                    )
                }
            }

            when (clipsState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = ElectricCyan)
                    }
                }

                is UiState.Error -> {
                    VibeCutGlassCard(accentColor = NeonCoral, highlighted = true) {
                        Text("Unable to sync clips", style = MaterialTheme.typography.titleMedium, color = NeonCoral)
                        Text(clipsState.message, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }

                is UiState.Success -> {
                    val filtered = clipsState.data.filter { clip ->
                        val matchesRatio = ratioFilter == "All" || clip.aspectRatio == ratioFilter
                        val matchesQuery = searchQuery.isBlank() ||
                            clip.title.contains(searchQuery, ignoreCase = true) ||
                            clip.sourceLabel.contains(searchQuery, ignoreCase = true) ||
                            clip.formatLabel.contains(searchQuery, ignoreCase = true)
                        matchesRatio && matchesQuery
                    }

                    if (filtered.isEmpty()) {
                        VibeCutGlassCard(accentColor = ElectricCyan) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(ElectricCyanContainer)
                                        .border(1.dp, ElectricCyan.copy(alpha = 0.45f), RoundedCornerShape(18.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.VideoLibrary,
                                        contentDescription = null,
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(32.dp),
                                    )
                                }
                                Text(
                                    text = if (clipsState.data.isEmpty()) "No Generated Clips Yet" else "No Matching Clips",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                )
                                Text(
                                    text = "Generate short clips in Studio and preview, edit, or share them directly from this library.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                )
                                VibeCutGradientButton(
                                    text = "Open Studio",
                                    onClick = onSwitchToStudio,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(filtered, key = { it.id.ifBlank { it.outputUriString + it.title } }) { clip ->
                                ClipRecordCard(
                                    clip = clip,
                                    onPreview = { previewClip = clip },
                                    onShare = {
                                        val uri = runCatching { Uri.parse(clip.outputUriString) }.getOrNull()
                                        if (uri != null) {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "video/mp4"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            runCatching {
                                                context.startActivity(Intent.createChooser(shareIntent, "Share ${clip.title}"))
                                            }
                                        }
                                    },
                                    onOpenEditor = { onOpenInEditor(clip) },
                                    onDelete = { if (clip.id.isNotBlank()) onDeleteClip(clip.id) },
                                )
                            }
                            item { Spacer(Modifier.height(12.dp)) }
                        }
                    }
                }
            }
        }
    }

    previewClip?.let { clip ->
        ClipPreviewDialog(
            clip = clip,
            onDismiss = { previewClip = null },
        )
    }
}

@Composable
private fun ClipRecordCard(
    clip: GeneratedClipRecord,
    onPreview: () -> Unit,
    onShare: () -> Unit,
    onOpenEditor: () -> Unit,
    onDelete: () -> Unit,
) {
    VibeCutGlassCard(
        modifier = Modifier
            .clickable(onClick = onPreview)
            .testTag("clip_card_${clip.id}"),
        accentColor = if (clip.watermarkApplied) ElectricCyan else LuminousMint,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(ElectricCyanContainer, VioletGlow.copy(alpha = 0.25f)),
                        ),
                    )
                    .border(1.dp, ElectricCyan.copy(alpha = 0.45f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Movie,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(28.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = clip.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${clip.formatLabel} • ${formatDurationShort(clip.durationMs)} • ${clip.resolutionLabel}",
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricCyan,
                )
                Text(
                    text = String.format(Locale.US, "%.1f MB • %s", clip.fileSizeMb, clip.sourceLabel),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            StatusPillBadge(
                text = if (clip.watermarkApplied) "Watermarked" else "Clean",
                accentColor = if (clip.watermarkApplied) AmberGold else LuminousMint,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VibeCutGradientButton(
                text = "Preview",
                onClick = onPreview,
                icon = Icons.Rounded.PlayArrow,
                modifier = Modifier.weight(1.1f),
            )
            OutlinedButton(
                onClick = onOpenEditor,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, HairlineBorder),
            ) {
                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(Modifier.width(4.dp))
                Text("Edit", color = Color.White)
            }
            IconButton(
                onClick = onShare,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(Icons.Rounded.Share, contentDescription = "Share clip", tint = ElectricCyan)
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete clip", tint = NeonCoral)
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun ClipPreviewDialog(
    clip: GeneratedClipRecord,
    onDismiss: () -> Unit,
) {
    BackHandler { onDismiss() }
    val context = LocalContext.current
    val player = remember(clip.outputUriString) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(clip.outputUriString)))
            repeatMode = Player.REPEAT_MODE_ALL
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = CarbonSurface,
            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.45f)),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(clip.title, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${clip.formatLabel} • ${clip.resolutionLabel}", style = MaterialTheme.typography.labelMedium, color = ElectricCyan)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close preview", tint = Color.White)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(9f / 14f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                        .border(1.dp, HairlineBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    ContentFrame(
                        player = player,
                        modifier = Modifier.fillMaxSize(),
                        surfaceType = SURFACE_TYPE_SURFACE_VIEW,
                        contentScale = ContentScale.Fit,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            val playIntent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(Uri.parse(clip.outputUriString), "video/mp4")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            runCatching { context.startActivity(playIntent) }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text("External Player", color = Color.White)
                    }
                    VibeCutGradientButton(
                        text = "Done",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
