package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.ReferralClaimDoc
import com.example.model.UserProfile
import com.example.ui.components.MetricStatPill
import com.example.ui.components.StatusPillBadge
import com.example.ui.components.StudioAmbientBackground
import com.example.ui.components.StudioSectionHeader
import com.example.ui.components.VibeCutGlassCard
import com.example.ui.components.VibeCutGradientButton
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CarbonElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.LuminousMint
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletGlow

@Composable
fun InviteTabScreen(
    userProfile: UserProfile?,
    confirmedReferrals: List<ReferralClaimDoc>,
    onClaimInviteCode: (String) -> Unit,
    onShowToast: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var codeInput by remember { mutableStateOf("") }
    val inviteCode = userProfile?.inviteCode?.ifBlank { "VC-CREATOR" } ?: "VC-CREATOR"
    val bonusCredits = userProfile?.bonusCredits ?: 0L
    val alreadyClaimed = !userProfile?.referredByCode.isNullOrBlank()

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
                        text = "Invite & Rewards",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                    )
                    Text(
                        text = "+5 Bonus Credits for every new device referral",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                }
                StatusPillBadge(
                    text = "$bonusCredits Credits",
                    accentColor = AmberGold,
                    leadingDot = true,
                )
            }

            // Metrics Summary Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MetricStatPill(
                    value = bonusCredits.toString(),
                    label = "Available Credits",
                    accentColor = ElectricCyan,
                    modifier = Modifier.weight(1f),
                )
                MetricStatPill(
                    value = confirmedReferrals.size.toString(),
                    label = "Joined Friends",
                    accentColor = LuminousMint,
                    modifier = Modifier.weight(1f),
                )
                MetricStatPill(
                    value = "+${confirmedReferrals.size * 5}",
                    label = "Earned Total",
                    accentColor = AmberGold,
                    modifier = Modifier.weight(1f),
                )
            }

            // VIP Referral Pass Card
            VibeCutGlassCard(
                accentColor = ElectricCyan,
                highlighted = true,
            ) {
                StudioSectionHeader(
                    title = "Your Creator Pass",
                    subtitle = "Share your unique code to unlock 720p–1440p & 3–40 clips",
                    icon = Icons.Rounded.CardGiftcard,
                    accentColor = ElectricCyan,
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    ObsidianBg.copy(alpha = 0.92f),
                                    CarbonElevated.copy(alpha = 0.95f),
                                ),
                            ),
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(
                                listOf(ElectricCyan.copy(alpha = 0.65f), VioletGlow.copy(alpha = 0.45f)),
                            ),
                            shape = RoundedCornerShape(16.dp),
                        )
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "INVITE CODE",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                            )
                            Text(
                                text = inviteCode,
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = ElectricCyan,
                            )
                        }
                        StatusPillBadge(
                            text = "${confirmedReferrals.size} Joined",
                            accentColor = LuminousMint,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("VibeCut Invite Code", inviteCode))
                            onShowToast("Copied invite code $inviteCode")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("copy_invite_code_button"),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp), tint = ElectricCyan)
                        Spacer(Modifier.width(6.dp))
                        Text("Copy Code", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }

                    VibeCutGradientButton(
                        text = "Share Invite",
                        onClick = {
                            val shareText = "Turn long videos into viral 15s–10m clips with VibeCut! Join with my invite code $inviteCode to get +5 Bonus Credits."
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            runCatching {
                                context.startActivity(Intent.createChooser(intent, "Share VibeCut Invite"))
                            }
                        },
                        icon = Icons.Rounded.Share,
                        accentStart = ElectricCyan,
                        accentEnd = VioletGlow,
                        modifier = Modifier
                            .weight(1.1f)
                            .testTag("share_invite_button"),
                    )
                }
            }

            // New Device Welcome Code Claim Card
            VibeCutGlassCard(accentColor = AmberGold) {
                StudioSectionHeader(
                    title = "Join From a New Device",
                    subtitle = "Redeem a friend's invite code once to unlock +5 welcome credits",
                    icon = Icons.Rounded.GroupAdd,
                    accentColor = AmberGold,
                )

                if (alreadyClaimed) {
                    Surface(
                        color = LuminousMint.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, LuminousMint.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = LuminousMint)
                            Text(
                                text = "Welcome code ${userProfile?.referredByCode} claimed • +5 Bonus Credits credited!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = LuminousMint,
                            )
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = { codeInput = it.uppercase() },
                        label = { Text("Friend's Invite Code (e.g. VC-8A2B1C)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("claim_invite_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberGold,
                            unfocusedBorderColor = HairlineBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                        ),
                    )
                    VibeCutGradientButton(
                        text = "Claim +5 Bonus Credits",
                        onClick = { onClaimInviteCode(codeInput) },
                        enabled = codeInput.trim().length >= 4,
                        accentStart = AmberGold,
                        accentEnd = Color(0xFFF59E0B),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("claim_invite_button"),
                    )
                }
            }

            // Confirmed Referrals Roster
            VibeCutGlassCard(accentColor = LuminousMint) {
                StudioSectionHeader(
                    title = "Confirmed Referrals (${confirmedReferrals.size})",
                    subtitle = "Verified cross-device signups using your invite pass",
                    icon = Icons.Rounded.Stars,
                    accentColor = LuminousMint,
                )

                if (confirmedReferrals.isEmpty()) {
                    Text(
                        text = "No cross-device referrals confirmed yet. Share your code above to earn +5 credits per new device signup.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                    )
                } else {
                    confirmedReferrals.forEach { claim ->
                        Surface(
                            color = CarbonElevated.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, HairlineBorder),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(LuminousMint.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = claim.claimantName.take(1).uppercase(),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = LuminousMint,
                                        )
                                    }
                                    Column {
                                        Text(claim.claimantName, style = MaterialTheme.typography.titleSmall, color = Color.White)
                                        Text("Code: ${claim.code}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    }
                                }
                                StatusPillBadge(
                                    text = "+${claim.creditsAwarded} Credits",
                                    accentColor = LuminousMint,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
        }
    }
}
