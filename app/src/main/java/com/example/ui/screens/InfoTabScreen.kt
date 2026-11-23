package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.model.MonetizationConfig
import com.example.model.UserProfile
import com.example.ui.components.StatusPillBadge
import com.example.ui.components.StudioAmbientBackground
import com.example.ui.components.StudioSectionHeader
import com.example.ui.components.VibeCutGlassCard
import com.example.ui.components.VibeCutGradientButton
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.LuminousMint
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletGlow

@Composable
fun InfoTabScreen(
    monetizationConfig: MonetizationConfig,
    userProfile: UserProfile?,
    userEmail: String?,
    onOpenAdminPanel: () -> Unit,
    onOpenSubscriptionPaywall: () -> Unit,
    onSignOutClick: () -> Unit,
    onShowToast: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showPrivacyModal by remember { mutableStateOf(false) }

    val appVersionLabel = stringResource(R.string.app_version_label)
    val developerName = stringResource(R.string.developer_name)
    val appDescription = stringResource(R.string.app_description_full)
    val privacyPolicy = stringResource(R.string.privacy_policy_full)
    val disclaimer = stringResource(R.string.disclaimer_full)

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Info & Studio Controls",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                    )
                    Text(
                        text = "App profile, privacy, disclaimer & monetization settings",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                }
                StatusPillBadge(text = "v2.1", accentColor = ElectricCyan)
            }

            // App Identity & Description Card
            VibeCutGlassCard(accentColor = ElectricCyan, highlighted = true) {
                StudioSectionHeader(
                    title = appVersionLabel,
                    subtitle = "Developer: $developerName",
                    icon = Icons.Rounded.Info,
                    accentColor = ElectricCyan,
                    trailing = {
                        StatusPillBadge(text = "Verified Build", accentColor = LuminousMint)
                    },
                )
                Text(
                    text = appDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }

            // Active Monetization Mode Card (Respects Mutual Exclusion Rule)
            VibeCutGlassCard(
                accentColor = if (monetizationConfig.adSupportedMonetization) ElectricCyan else AmberGold,
            ) {
                if (monetizationConfig.adSupportedMonetization) {
                    StudioSectionHeader(
                        title = "Ad-Supported Monetization",
                        subtitle = "Active Network: ${monetizationConfig.networkEnum().displayName}",
                        icon = Icons.Rounded.WorkspacePremium,
                        accentColor = ElectricCyan,
                    )
                    Text(
                        text = "Free tier unlocks 360p and 1–2 clips. Watch a short ad via ${monetizationConfig.networkEnum().displayName} to unlock 720p HD, 1080p FHD, 1440p UHD, 3–40 batch clips, and watermark removal.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                } else {
                    val subActive = userProfile?.subscriptionActive == true
                    StudioSectionHeader(
                        title = "VibeCut Pro Subscription",
                        subtitle = if (subActive) "Active Plan: ${userProfile?.planEnum()?.title}" else "Ad-free subscription mode enabled",
                        icon = Icons.Rounded.WorkspacePremium,
                        accentColor = AmberGold,
                    )
                    Text(
                        text = if (subActive) {
                            "All 720p–1440p exports, 3–40 batch clips, and watermark-free rendering are unlocked."
                        } else {
                            "Ad-supported mode is currently turned OFF. Subscribe to VibeCut Pro to unlock 720p–1440p exports, 3–40 batch clips, and watermark removal."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (subActive) LuminousMint else TextSecondary,
                    )
                    VibeCutGradientButton(
                        text = if (subActive) "Manage Subscription" else "Upgrade to VibeCut Pro",
                        onClick = onOpenSubscriptionPaywall,
                        accentStart = AmberGold,
                        accentEnd = Color(0xFFF59E0B),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("info_subscription_button"),
                    )
                }
            }

            // Privacy Policy Card + Modal
            VibeCutGlassCard(accentColor = LuminousMint) {
                StudioSectionHeader(
                    title = "Privacy Policy",
                    subtitle = "Built-in transparent disclosure • Start.io, Unity Ads & Adsterra",
                    icon = Icons.Rounded.PrivacyTip,
                    accentColor = LuminousMint,
                )
                Text(
                    text = "VibeCut processes user-selected media and social links strictly to create requested clips, integrates configurable ad partners (Start.io, Unity Ads, Adsterra), never sells personal data, and attributes developer contact to $developerName.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    VibeCutGradientButton(
                        text = "Read Privacy Policy",
                        onClick = { showPrivacyModal = true },
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("view_privacy_policy_button"),
                    )
                    OutlinedButton(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("VibeCut Privacy Policy", privacyPolicy))
                            onShowToast("Privacy Policy copied to clipboard.")
                        },
                        modifier = Modifier
                            .weight(0.75f)
                            .height(52.dp)
                            .testTag("copy_privacy_policy_button"),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, HairlineBorder),
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy Privacy Policy", modifier = Modifier.size(18.dp), tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text("Copy", color = Color.White)
                    }
                }
            }

            // Disclaimer Card
            VibeCutGlassCard(accentColor = AmberGold) {
                StudioSectionHeader(
                    title = "Disclaimer",
                    subtitle = "Creator copyright & independent tool notice",
                    icon = Icons.Rounded.Gavel,
                    accentColor = AmberGold,
                )
                Text(
                    text = disclaimer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }

            // Admin Panel Entry Card
            VibeCutGlassCard(accentColor = VioletGlow, highlighted = true) {
                StudioSectionHeader(
                    title = "Admin Monetization Control",
                    subtitle = "Configure Start.io, Unity Ads, Adsterra & All-User Sync",
                    icon = Icons.Rounded.AdminPanelSettings,
                    accentColor = VioletGlow,
                )
                Text(
                    text = "Switch ad networks live, validate Unity placements, toggle Test Mode, configure N-batch interstitials, and sync monetization mode across all users.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                VibeCutGradientButton(
                    text = "Open Admin Panel",
                    onClick = onOpenAdminPanel,
                    icon = Icons.Rounded.AdminPanelSettings,
                    accentStart = ElectricCyan,
                    accentEnd = VioletGlow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_admin_panel_button"),
                )
            }

            // Account & Sign-Out Card
            VibeCutGlassCard(accentColor = NeonCoral) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userProfile?.displayName ?: "Signed-In Creator",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                        )
                        Text(
                            text = userEmail ?: "Google Account Connected",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted,
                        )
                    }
                    OutlinedButton(
                        onClick = onSignOutClick,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("sign_out_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, NeonCoral.copy(alpha = 0.55f)),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, tint = NeonCoral, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Sign Out", color = NeonCoral)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
        }
    }

    if (showPrivacyModal) {
        PrivacyPolicyModal(
            policyText = privacyPolicy,
            onCopy = {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("VibeCut Privacy Policy", privacyPolicy))
                onShowToast("Privacy Policy copied.")
            },
            onShare = {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "VibeCut Privacy Policy")
                    putExtra(Intent.EXTRA_TEXT, privacyPolicy)
                }
                runCatching {
                    context.startActivity(Intent.createChooser(shareIntent, "Share Privacy Policy"))
                }
            },
            onDismiss = { showPrivacyModal = false },
        )
    }
}

@Composable
private fun PrivacyPolicyModal(
    policyText: String,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler { onDismiss() }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("privacy_policy_modal"),
            shape = RoundedCornerShape(24.dp),
            color = CarbonSurface,
            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.45f)),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("VibeCut Privacy Policy", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close privacy policy", tint = Color.White)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        text = policyText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onCopy,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text("Copy", color = Color.White)
                    }
                    VibeCutGradientButton(
                        text = "Share",
                        onClick = onShare,
                        icon = Icons.Rounded.Share,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
