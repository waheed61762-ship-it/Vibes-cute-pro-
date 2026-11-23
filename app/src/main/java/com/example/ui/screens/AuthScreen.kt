package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.StatusPillBadge
import com.example.ui.components.StudioAmbientBackground
import com.example.ui.components.VibeCutGlassCard
import com.example.ui.components.VibeCutGradientButton
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CarbonElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.LuminousMint
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.NeonCoralContainer
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletGlow

@Composable
fun AuthScreen(
    isAuthenticating: Boolean,
    authError: String?,
    onGoogleSignInClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StudioAmbientBackground(
        modifier = modifier
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = 520.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Hero Artwork with layered overlapping brand badge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(ElectricCyan.copy(alpha = 0.45f), HairlineBorder),
                            ),
                            shape = RoundedCornerShape(28.dp),
                        ),
                ) {
                    Image(
                        painter = painterResource(R.drawable.img_vibecut_hero_1795454760936),
                        contentDescription = "VibeCut cinema studio artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        ObsidianBg.copy(alpha = 0.52f),
                                        ObsidianBg.copy(alpha = 0.96f),
                                    ),
                                ),
                            ),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StatusPillBadge(
                            text = stringResource(R.string.app_version_label),
                            accentColor = ElectricCyan,
                            leadingDot = true,
                        )
                        StatusPillBadge(
                            text = "4K ENGINE",
                            accentColor = VioletGlow,
                        )
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(18.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MovieCreation,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(24.dp),
                            )
                            Text(
                                text = "VibeCut Studio",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Turn long videos & social links into viral 15s–10m clips.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                    }
                }

                // Overlapping Glass Feature Highlight Card
                VibeCutGlassCard(
                    modifier = Modifier
                        .offset(y = (-14).dp)
                        .padding(horizontal = 6.dp),
                    highlighted = true,
                ) {
                    AuthHighlightRow(
                        icon = Icons.Rounded.AutoAwesome,
                        accent = ElectricCyan,
                        title = "Reels, Shorts, TikTok & YouTube Framing",
                        subtitle = "Instant 9:16, 4:5, 1:1, and 16:9 batch cuts with smart discovery.",
                    )
                    AuthHighlightRow(
                        icon = Icons.Rounded.HighQuality,
                        accent = AmberGold,
                        title = "Up to 1440p UHD & Pro Timeline Grading",
                        subtitle = "GPU filters, transitions, kinetic text, and watermark control.",
                    )
                    AuthHighlightRow(
                        icon = Icons.Rounded.CloudDone,
                        accent = LuminousMint,
                        title = "Cross-Device Clip Sync & Referral Credits",
                        subtitle = "Keep your clip library and +5 invite credits synced securely.",
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (!authError.isNullOrBlank()) {
                    Surface(
                        color = NeonCoralContainer,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, NeonCoral.copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = authError,
                            style = MaterialTheme.typography.bodyMedium,
                            color = NeonCoral,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }

                VibeCutGradientButton(
                    text = if (isAuthenticating) "Signing in..." else "Continue with Google",
                    onClick = onGoogleSignInClick,
                    icon = Icons.AutoMirrored.Rounded.Login,
                    enabled = !isAuthenticating,
                    isLoading = isAuthenticating,
                    accentStart = ElectricCyan,
                    accentEnd = VioletGlow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("google_sign_in_button"),
                )

                Text(
                    text = "Crafted by ${stringResource(R.string.developer_name)} • VibeCut v2.1",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted,
                )
            }
        }
    }
}

@Composable
private fun AuthHighlightRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    title: String,
    subtitle: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(CarbonElevated)
                .border(1.dp, accent.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(19.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }
    }
}
