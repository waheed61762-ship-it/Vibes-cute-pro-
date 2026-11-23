package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ads.AdSessionState
import com.example.model.AdPlacementType
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CarbonElevated
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CarbonTrack
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanContainer
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.LuminousMint
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AdExperienceDialog(
    state: AdSessionState,
    bonusCredits: Long,
    onRetry: () -> Unit,
    onCompleteUnlock: (isGraceRecovery: Boolean) -> Unit,
    onSpendBonusCredit: (AdPlacementType) -> Unit,
    onDismiss: () -> Unit,
) {
    if (state is AdSessionState.Idle) return

    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .testTag("ad_experience_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = CarbonSurface,
            border = BorderStroke(1.dp, HairlineBorder),
        ) {
            when (state) {
                is AdSessionState.Loading -> {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        CircularProgressIndicator(
                            color = ElectricCyan,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(44.dp),
                        )
                        Text(
                            text = state.network.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                        )
                        Text(
                            text = state.statusLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) {
                            Text("Cancel", color = TextMuted)
                        }
                    }
                }

                is AdSessionState.TestSandboxAd -> {
                    val totalSeconds = if (state.placement == AdPlacementType.INTERSTITIAL_BATCH) 2 else 3
                    var remaining by remember(state) { mutableIntStateOf(totalSeconds) }
                    LaunchedEffect(state) {
                        while (remaining > 0) {
                            delay(1_000L)
                            remaining--
                        }
                    }

                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                color = ElectricCyanContainer,
                                shape = RoundedCornerShape(50),
                            ) {
                                Text(
                                    text = state.network.displayName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ElectricCyan,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }
                            Text(
                                text = if (remaining > 0) "Reward ready in ${remaining}s" else "Unlocked",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (remaining > 0) AmberGold else LuminousMint,
                            )
                        }

                        Surface(
                            color = CarbonElevated,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, HairlineBorder),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = ElectricCyan,
                                    )
                                    Text(
                                        text = state.placement.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                    )
                                }
                                Text(
                                    text = state.placement.rewardSummary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                )
                                Text(
                                    text = state.configLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = {
                                ((totalSeconds - remaining).toFloat() / totalSeconds.coerceAtLeast(1)).coerceIn(0f, 1f)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = ElectricCyan,
                            trackColor = CarbonTrack,
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                            ) {
                                Text("Close", color = TextSecondary)
                            }
                            Button(
                                onClick = { onCompleteUnlock(false) },
                                enabled = remaining == 0,
                                modifier = Modifier
                                    .weight(1.4f)
                                    .height(48.dp)
                                    .testTag("ad_claim_reward_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricCyan,
                                    contentColor = ObsidianBg,
                                ),
                            ) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (state.placement == AdPlacementType.INTERSTITIAL_BATCH) "Continue" else "Claim Reward",
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }

                is AdSessionState.AdsterraWebContainer -> {
                    var webStatus by remember(state) { mutableStateOf("Loading Adsterra container...") }
                    var hasLoadError by remember(state) { mutableStateOf(false) }
                    var isPageReady by remember(state) { mutableStateOf(state.testMode) }

                    LaunchedEffect(state) {
                        if (!isPageReady) {
                            delay(6_000L)
                            if (!isPageReady) {
                                hasLoadError = true
                                webStatus = "Adsterra container timed out — non-blocking pass ready."
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = "Adsterra Sponsored View",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                )
                                Text(
                                    text = "Tag: ${state.tagId}" + if (state.testMode) " • Test Mode" else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricCyan,
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(state.targetUrl)))
                                    }
                                    isPageReady = true
                                },
                                modifier = Modifier.height(48.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                    contentDescription = "Open Adsterra link",
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Open")
                            }
                        }

                        if (state.testMode) {
                            Surface(
                                color = CarbonElevated,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, HairlineBorder),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = "Adsterra Test Sandbox Active",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = LuminousMint,
                                    )
                                    Text(
                                        text = "Placement: ${state.placement.title}\nTarget URL: ${state.targetUrl}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary,
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(210.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(CarbonElevated),
                            ) {
                                AndroidView(
                                    factory = { ctx ->
                                        WebView(ctx).apply {
                                            settings.javaScriptEnabled = true
                                            settings.domStorageEnabled = true
                                            webViewClient = object : WebViewClient() {
                                                override fun onPageFinished(view: WebView?, url: String?) {
                                                    isPageReady = true
                                                    webStatus = "Adsterra placement loaded."
                                                }

                                                override fun onReceivedError(
                                                    view: WebView?,
                                                    request: WebResourceRequest?,
                                                    error: WebResourceError?,
                                                ) {
                                                    if (request?.isForMainFrame == true) {
                                                        hasLoadError = true
                                                        isPageReady = true
                                                        webStatus = "Adsterra unavailable right now — non-blocking pass ready."
                                                    }
                                                }

                                                override fun onReceivedHttpError(
                                                    view: WebView?,
                                                    request: WebResourceRequest?,
                                                    errorResponse: WebResourceResponse?,
                                                ) {
                                                    if (request?.isForMainFrame == true &&
                                                        (errorResponse?.statusCode == 204 || (errorResponse?.statusCode ?: 200) >= 400)
                                                    ) {
                                                        hasLoadError = true
                                                        isPageReady = true
                                                        webStatus = "HTTP ${errorResponse?.statusCode ?: 204} no-fill — non-blocking pass ready."
                                                    }
                                                }
                                            }
                                            loadUrl(state.targetUrl)
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }

                        Text(
                            text = webStatus,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (hasLoadError) AmberGold else TextSecondary,
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                            ) {
                                Text("Close", color = TextSecondary)
                            }
                            Button(
                                onClick = { onCompleteUnlock(hasLoadError) },
                                enabled = isPageReady || hasLoadError,
                                modifier = Modifier
                                    .weight(1.4f)
                                    .height(48.dp)
                                    .testTag("adsterra_continue_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricCyan,
                                    contentColor = ObsidianBg,
                                ),
                            ) {
                                Text(
                                    text = if (hasLoadError) "Continue Smoothly" else "Complete & Unlock",
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }

                is AdSessionState.NonBlockingNotice -> {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = "Ad status notice",
                                tint = AmberGold,
                                modifier = Modifier.size(28.dp),
                            )
                            Column {
                                Text(
                                    text = state.errorTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                )
                                Text(
                                    text = "${state.network.displayName} • ${state.placement.title}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AmberGold,
                                )
                            }
                        }

                        Surface(
                            color = CarbonElevated,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, HairlineBorder),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = state.errorDetail,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(14.dp),
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedButton(
                                onClick = onRetry,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("ad_retry_button"),
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Retry Ad")
                            }

                            Button(
                                onClick = { onCompleteUnlock(true) },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(48.dp)
                                    .testTag("ad_grace_continue_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LuminousMint,
                                    contentColor = ObsidianBg,
                                ),
                            ) {
                                Text("Continue Pass", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (bonusCredits > 0 && state.placement != AdPlacementType.INTERSTITIAL_BATCH) {
                            OutlinedButton(
                                onClick = { onSpendBonusCredit(state.placement) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                            ) {
                                Text("Or Use 1 Invite Credit ($bonusCredits available)", color = ElectricCyan)
                            }
                        }
                    }
                }
            }
        }
    }
}
