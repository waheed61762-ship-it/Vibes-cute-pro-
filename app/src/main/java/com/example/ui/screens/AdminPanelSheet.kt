package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ads.AdOrchestrator
import com.example.model.AdNetwork
import com.example.model.AdPlacementType
import com.example.model.MonetizationConfig
import com.example.ui.components.StatusPillBadge
import com.example.ui.components.VibeCutGlassCard
import com.example.ui.components.VibeCutGradientButton
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelSheet(
    currentConfig: MonetizationConfig,
    onSaveLive: (MonetizationConfig) -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler { onDismiss() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var adSupported by remember(currentConfig) { mutableStateOf(currentConfig.adSupportedMonetization) }
    var activeNetwork by remember(currentConfig) { mutableStateOf(currentConfig.networkEnum()) }

    var startIoAppId by remember(currentConfig) { mutableStateOf(currentConfig.startIoAppId) }
    var startIoTestMode by remember(currentConfig) { mutableStateOf(currentConfig.startIoTestMode) }

    var unityGameId by remember(currentConfig) { mutableStateOf(currentConfig.unityGameId) }
    var unityRewardedPlacement by remember(currentConfig) { mutableStateOf(currentConfig.unityRewardedPlacementId) }
    var unityInterstitialPlacement by remember(currentConfig) { mutableStateOf(currentConfig.unityInterstitialPlacementId) }
    var unityTestMode by remember(currentConfig) { mutableStateOf(currentConfig.unityTestMode) }

    var adsterraDirectUrl by remember(currentConfig) { mutableStateOf(currentConfig.adsterraDirectLinkUrl) }
    var adsterraBannerTag by remember(currentConfig) { mutableStateOf(currentConfig.adsterraBannerTagId) }
    var adsterraInterstitialTag by remember(currentConfig) { mutableStateOf(currentConfig.adsterraInterstitialTagId) }
    var adsterraTestMode by remember(currentConfig) { mutableStateOf(currentConfig.adsterraTestMode) }

    var interstitialEveryN by remember(currentConfig) { mutableIntStateOf(currentConfig.interstitialEveryNBatches.coerceIn(1, 20)) }

    val draftConfig = MonetizationConfig(
        adSupportedMonetization = adSupported,
        subscriptionEnabled = !adSupported,
        activeAdNetwork = activeNetwork.name,
        startIoAppId = startIoAppId,
        startIoTestMode = startIoTestMode,
        unityGameId = unityGameId,
        unityRewardedPlacementId = unityRewardedPlacement,
        unityInterstitialPlacementId = unityInterstitialPlacement,
        unityTestMode = unityTestMode,
        adsterraDirectLinkUrl = adsterraDirectUrl,
        adsterraBannerTagId = adsterraBannerTag,
        adsterraInterstitialTagId = adsterraInterstitialTag,
        adsterraTestMode = adsterraTestMode,
        interstitialEveryNBatches = interstitialEveryN,
    )

    val unityRewardedValidation = AdOrchestrator.validateUnityPlacement(unityGameId, unityRewardedPlacement)
    val unityInterstitialValidation = AdOrchestrator.validateUnityPlacement(unityGameId, unityInterstitialPlacement)
    val selectedValidation = AdOrchestrator.validateNetworkConfig(draftConfig, AdPlacementType.REWARDED_BATCH)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CarbonSurface,
        contentColor = Color.White,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("admin_panel_sheet"),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(26.dp))
                    Column {
                        Text("Admin Monetization Panel", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "Live All-User Config Sync • Multi-Network Ads",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricCyan,
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close Admin Panel")
                }
            }

            // Mutual Exclusion Control Card
            VibeCutGlassCard(
                accentColor = if (adSupported) ElectricCyan else AmberGold,
                highlighted = true,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable Ad-Supported Monetization", style = MaterialTheme.typography.titleSmall, color = Color.White)
                        Text(
                            text = if (adSupported) {
                                "ON: Subscriptions hidden for all users. Features unlocked via ads."
                            } else {
                                "OFF: Paid Subscription UI shown for all users instead of ads."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (adSupported) LuminousMint else AmberGold,
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Switch(
                        checked = adSupported,
                        onCheckedChange = { adSupported = it },
                        modifier = Modifier.testTag("admin_ad_supported_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianBg,
                            checkedTrackColor = ElectricCyan,
                        ),
                    )
                }

                StatusPillBadge(
                    text = if (adSupported) {
                        "Mutual Exclusion: Paid Subscription System = OFF"
                    } else {
                        "Mutual Exclusion: Paid Subscription System = ON"
                    },
                    accentColor = if (adSupported) ElectricCyan else AmberGold,
                    leadingDot = true,
                )
            }

            // Primary Ad Network Selector
            Text("Primary Ad Network", style = MaterialTheme.typography.titleSmall, color = Color.White)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AdNetwork.entries.forEach { network ->
                    FilterChip(
                        selected = activeNetwork == network,
                        onClick = { activeNetwork = network },
                        label = { Text(network.displayName) },
                        modifier = Modifier.testTag("admin_network_${network.name.lowercase()}"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricCyanContainer,
                            selectedLabelColor = ElectricCyan,
                        ),
                    )
                }
            }

            when (activeNetwork) {
                AdNetwork.START_IO -> {
                    VibeCutGlassCard(accentColor = ElectricCyan) {
                        Text("Start.io Configuration", style = MaterialTheme.typography.titleSmall, color = ElectricCyan)
                        AdminTextField(
                            value = startIoAppId,
                            onValueChange = { startIoAppId = it },
                            label = "Start.io App ID (e.g. 210382301)",
                        )
                        TestModeToggleRow(
                            label = "Start.io Test Mode",
                            subtitle = if (startIoTestMode) "ON — Test / Sandbox Ads" else "OFF — Live Production Earnings",
                            checked = startIoTestMode,
                            onCheckedChange = { startIoTestMode = it },
                        )
                    }
                }

                AdNetwork.UNITY_ADS -> {
                    VibeCutGlassCard(accentColor = ElectricCyan) {
                        Text("Unity Ads Configuration", style = MaterialTheme.typography.titleSmall, color = ElectricCyan)
                        AdminTextField(
                            value = unityGameId,
                            onValueChange = { unityGameId = it },
                            label = "Unity Game ID (4–12 digits)",
                        )
                        AdminTextField(
                            value = unityRewardedPlacement,
                            onValueChange = { unityRewardedPlacement = it },
                            label = "Rewarded Placement ID (e.g. Rewarded_Android)",
                        )
                        AdminTextField(
                            value = unityInterstitialPlacement,
                            onValueChange = { unityInterstitialPlacement = it },
                            label = "Interstitial Placement ID (e.g. Interstitial_Android)",
                        )
                        TestModeToggleRow(
                            label = "Unity Test Mode",
                            subtitle = if (unityTestMode) "ON — Safe Test Sandbox" else "OFF — Live Unity Placements",
                            checked = unityTestMode,
                            onCheckedChange = { unityTestMode = it },
                        )
                        ValidationNoticeRow(
                            isValid = unityRewardedValidation.isValid && unityInterstitialValidation.isValid,
                            text = if (!unityRewardedValidation.isValid) {
                                unityRewardedValidation.message
                            } else {
                                unityInterstitialValidation.message
                            },
                        )
                    }
                }

                AdNetwork.ADSTERRA -> {
                    VibeCutGlassCard(accentColor = ElectricCyan) {
                        Text("Adsterra Configuration", style = MaterialTheme.typography.titleSmall, color = ElectricCyan)
                        AdminTextField(
                            value = adsterraDirectUrl,
                            onValueChange = { adsterraDirectUrl = it },
                            label = "Adsterra Direct Link / Ad URL",
                        )
                        AdminTextField(
                            value = adsterraBannerTag,
                            onValueChange = { adsterraBannerTag = it },
                            label = "Adsterra Banner / Rewarded Tag ID",
                        )
                        AdminTextField(
                            value = adsterraInterstitialTag,
                            onValueChange = { adsterraInterstitialTag = it },
                            label = "Adsterra Interstitial Tag ID",
                        )
                        TestModeToggleRow(
                            label = "Adsterra Test Mode",
                            subtitle = if (adsterraTestMode) "ON — Safe Preview Container" else "OFF — Live Web/Direct Container",
                            checked = adsterraTestMode,
                            onCheckedChange = { adsterraTestMode = it },
                        )
                    }
                }
            }

            if (!selectedValidation.isValid && activeNetwork != AdNetwork.UNITY_ADS) {
                ValidationNoticeRow(isValid = false, text = selectedValidation.message)
            }

            // Interstitial Every N Batches Stepper
            VibeCutGlassCard(accentColor = VioletGlow) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Show Interstitial Ad Every N Batches", style = MaterialTheme.typography.titleSmall, color = Color.White)
                        Text(
                            text = "Triggers sponsored break after every $interstitialEveryN batch${if (interstitialEveryN == 1) "" else "es"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { interstitialEveryN = (interstitialEveryN - 1).coerceAtLeast(1) },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(Icons.Rounded.Remove, contentDescription = "Decrease batch interval", tint = ElectricCyan)
                        }
                        Text(
                            text = interstitialEveryN.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                        )
                        IconButton(
                            onClick = { interstitialEveryN = (interstitialEveryN + 1).coerceAtMost(20) },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Increase batch interval", tint = ElectricCyan)
                        }
                    }
                }
            }

            VibeCutGradientButton(
                text = "Save Ads & Monetization Live",
                onClick = {
                    onSaveLive(draftConfig.enforceMutualExclusion())
                    onDismiss()
                },
                icon = Icons.Rounded.Save,
                accentStart = ElectricCyan,
                accentEnd = VioletGlow,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_admin_monetization_button"),
            )

            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun AdminTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ElectricCyan,
            unfocusedBorderColor = HairlineBorder,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
        ),
    )
}

@Composable
private fun TestModeToggleRow(
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = Color.White)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = TextMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ObsidianBg,
                checkedTrackColor = LuminousMint,
            ),
        )
    }
}

@Composable
private fun ValidationNoticeRow(
    isValid: Boolean,
    text: String,
) {
    Surface(
        color = if (isValid) LuminousMint.copy(alpha = 0.12f) else NeonCoral.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isValid) LuminousMint.copy(alpha = 0.45f) else NeonCoral.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = if (isValid) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = if (isValid) LuminousMint else NeonCoral,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = if (isValid) LuminousMint else NeonCoral,
            )
        }
    }
}
