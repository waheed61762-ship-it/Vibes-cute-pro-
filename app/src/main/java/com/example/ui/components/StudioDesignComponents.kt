package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.PlannedClipSegment
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CarbonElevated
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CarbonTrack
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanContainer
import com.example.ui.theme.ElectricCyanDark
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.LuminousMint
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletGlow
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun StudioAmbientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .drawBehind {
                // Top-right Electric Cyan ambient glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ElectricCyan.copy(alpha = 0.14f),
                            ElectricCyan.copy(alpha = 0.03f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.88f, size.height * 0.08f),
                        radius = size.maxDimension * 0.52f,
                    ),
                    center = Offset(size.width * 0.88f, size.height * 0.08f),
                    radius = size.maxDimension * 0.52f,
                )
                // Bottom-left Violet Cinema glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            VioletGlow.copy(alpha = 0.12f),
                            VioletGlow.copy(alpha = 0.02f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.12f, size.height * 0.84f),
                        radius = size.maxDimension * 0.48f,
                    ),
                    center = Offset(size.width * 0.12f, size.height * 0.84f),
                    radius = size.maxDimension * 0.48f,
                )
            },
        content = content,
    )
}

@Composable
fun VibeCutGlassCard(
    modifier: Modifier = Modifier,
    accentColor: Color = ElectricCyan,
    highlighted: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val borderBrush = if (highlighted) {
        Brush.linearGradient(
            colors = listOf(
                accentColor.copy(alpha = 0.85f),
                VioletGlow.copy(alpha = 0.55f),
                accentColor.copy(alpha = 0.35f),
            ),
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                accentColor.copy(alpha = 0.24f),
                HairlineBorder.copy(alpha = 0.85f),
                HairlineBorder.copy(alpha = 0.55f),
            ),
        )
    }

    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        CarbonElevated.copy(alpha = 0.90f),
                        CarbonSurface.copy(alpha = 0.96f),
                    ),
                ),
            )
            .border(width = if (highlighted) 1.5.dp else 1.dp, brush = borderBrush, shape = shape),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
fun StudioSectionHeader(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    accentColor: Color = ElectricCyan,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.14f))
                        .border(1.dp, accentColor.copy(alpha = 0.32f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                }
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun VibeCutGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    accentStart: Color = ElectricCyan,
    accentEnd: Color = Color(0xFF38BDF8),
    contentColor: Color = ObsidianBg,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.975f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "cta_scale",
    )

    val shape = RoundedCornerShape(16.dp)
    val bgBrush = if (enabled) {
        Brush.horizontalGradient(listOf(accentStart, accentEnd))
    } else {
        Brush.horizontalGradient(listOf(CarbonTrack, CarbonTrack))
    }

    Box(
        modifier = modifier
            .scale(scale)
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(bgBrush)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                enabled = enabled && !isLoading,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = contentColor,
                    strokeWidth = 2.2.dp,
                )
                Spacer(Modifier.width(10.dp))
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) contentColor else TextMuted,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = if (enabled) contentColor else TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun AspectRatioPreviewTile(
    label: String,
    ratioText: String,
    ratioValue: Float,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) ElectricCyanContainer else CarbonElevated.copy(alpha = 0.72f),
        label = "aspect_bg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) ElectricCyan else HairlineBorder,
        label = "aspect_border",
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Visual mini proportional frame glyph
            val (boxW, boxH) = when {
                ratioValue < 0.7f -> 15.dp to 24.dp // 9:16
                ratioValue < 0.95f -> 18.dp to 22.dp // 4:5
                abs(ratioValue - 1f) < 0.05f -> 20.dp to 20.dp // 1:1
                else -> 26.dp to 16.dp // 16:9
            }
            Box(
                modifier = Modifier.size(28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = boxW, height = boxH)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (selected) ElectricCyan.copy(alpha = 0.22f) else CarbonTrack,
                        )
                        .border(
                            width = 1.5.dp,
                            color = if (selected) ElectricCyan else TextSecondary,
                            shape = RoundedCornerShape(4.dp),
                        ),
                )
            }
            Text(
                text = label.substringBefore(" "),
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) Color.White else TextSecondary,
            )
            Text(
                text = ratioText,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) ElectricCyan else TextMuted,
            )
        }
    }
}

@Composable
fun StudioTimelineWaveformCanvas(
    segments: List<PlannedClipSegment>,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianBg.copy(alpha = 0.75f))
            .border(1.dp, HairlineBorder, RoundedCornerShape(12.dp)),
    ) {
        val barCount = 46
        val spacing = size.width / barCount
        val barWidth = (spacing * 0.56f).coerceAtLeast(2.dp.toPx())
        val centerY = size.height / 2f

        for (i in 0 until barCount) {
            val norm = i.toFloat() / barCount
            val wave = 0.25f + 0.65f * abs(sin(i * 0.62f + 0.4f)) * abs(sin(i * 0.19f + 1.1f))
            val barH = size.height * wave.coerceIn(0.18f, 0.85f)
            val isHighlighted = segments.isNotEmpty() && ((i + segments.size) % 3 != 0)
            val barColor = when {
                norm > 0.72f -> VioletGlow.copy(alpha = if (isHighlighted) 0.82f else 0.35f)
                isHighlighted -> ElectricCyan.copy(alpha = 0.85f)
                else -> ElectricCyanDark.copy(alpha = 0.38f)
            }
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x = i * spacing + (spacing - barWidth) / 2f, y = centerY - barH / 2f),
                size = Size(width = barWidth, height = barH),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }

        // Playhead indicator
        val playheadX = size.width * 0.22f
        drawLine(
            color = AmberGold,
            start = Offset(playheadX, 0f),
            end = Offset(playheadX, size.height),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f),
        )
    }
}

@Composable
fun StatusPillBadge(
    text: String,
    accentColor: Color = ElectricCyan,
    leadingDot: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = accentColor.copy(alpha = 0.14f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.40f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (leadingDot) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(accentColor),
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun MetricStatPill(
    value: String,
    label: String,
    accentColor: Color = ElectricCyan,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = CarbonElevated.copy(alpha = 0.85f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, HairlineBorder),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = accentColor,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
