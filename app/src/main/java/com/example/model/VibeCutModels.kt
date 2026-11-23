package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class MainTab(val label: String) {
    STUDIO("Studio"),
    MY_CLIPS("My Clips"),
    INVITE("Invite"),
    INFO("Info"),
}

enum class ClipLengthOption(
    val label: String,
    val durationMs: Long,
) {
    SEC_15("15s", 15_000L),
    SEC_30("30s", 30_000L),
    SEC_60("60s", 60_000L),
    MIN_5("5m", 300_000L),
    MIN_10("10m", 600_000L),
}

enum class SocialAspectFormat(
    val label: String,
    val ratioLabel: String,
    val preset: AspectRatioPreset,
) {
    REELS_9_16("Reels 9:16", "9:16", AspectRatioPreset.RATIO_9_16),
    SHORTS_9_16("Shorts 9:16", "9:16", AspectRatioPreset.RATIO_9_16),
    TIKTOK_9_16("TikTok 9:16", "9:16", AspectRatioPreset.RATIO_9_16),
    FACEBOOK_4_5("Facebook 4:5", "4:5", AspectRatioPreset.RATIO_4_5),
    SQUARE_1_1("Square 1:1", "1:1", AspectRatioPreset.RATIO_1_1),
    YOUTUBE_16_9("YouTube 16:9", "16:9", AspectRatioPreset.RATIO_16_9),
}

enum class ClipDiscoveryMode(
    val label: String,
    val description: String,
) {
    VIRAL_CLIPS("Viral Clips", "Prioritizes high-retention hooks & peak pacing"),
    SMART_HIGHLIGHTS("Smart Highlights", "Extracts evenly spaced highlight moments"),
    SEQUENTIAL("Sequential", "Cuts consecutive chapters from start to finish"),
}

enum class StudioQualityTier(
    val label: String,
    val exportResolution: ExportResolution,
    val freeTier: Boolean,
) {
    SD_360P("360p", ExportResolution.SD_360P, true),
    HD_720P("720p HD", ExportResolution.HD_720P, false),
    FHD_1080P("1080p FHD", ExportResolution.FHD_1080P, false),
    UHD_1440P("1440p UHD", ExportResolution.QHD_1440P, false),
}

enum class SourcePlatform(val label: String) {
    YOUTUBE("YouTube"),
    TIKTOK("TikTok"),
    FACEBOOK("Facebook"),
    INSTAGRAM("Instagram"),
    DIRECT_MP4("Direct MP4"),
    GALLERY("Gallery Video"),
}

data class ResolvedLinkMedia(
    val platform: SourcePlatform,
    val originalUrl: String,
    val title: String,
    val playableStreamUrl: String?,
    val directStreamPlayable: Boolean,
    val thumbnailUrl: String? = null,
    val durationHintMs: Long = 180_000L,
    val statusNote: String,
)

data class PlannedClipSegment(
    val index: Int,
    val title: String,
    val trimStartMs: Long,
    val trimEndMs: Long,
    val discoveryReason: String,
) {
    val durationMs: Long get() = (trimEndMs - trimStartMs).coerceAtLeast(250L)
}

enum class AdNetwork(val displayName: String) {
    START_IO("Start.io"),
    UNITY_ADS("Unity Ads"),
    ADSTERRA("Adsterra"),
}

enum class AdPlacementType(
    val title: String,
    val rewardSummary: String,
) {
    REWARDED_BATCH("Unlock 3–40 Batch Clips", "Unlocks multi-clip batch export up to 40 clips"),
    REWARDED_QUALITY("Unlock HD / FHD / UHD", "Unlocks 720p HD, 1080p FHD & 1440p UHD"),
    REWARDED_WATERMARK("Remove Watermark", "Exports clean watermark-free clips"),
    INTERSTITIAL_BATCH("Sponsored Break", "Supports free access across VibeCut"),
}

enum class SubscriptionPlan(
    val title: String,
    val priceLabel: String,
    val badge: String,
) {
    NONE("Free Plan", "Free", "360p • 1–2 clips"),
    PRO_MONTHLY("VibeCut Pro Monthly", "$4.99 / mo", "1440p • 40 clips • No Watermark"),
    PRO_YEARLY("VibeCut Pro Yearly", "$34.99 / yr", "Save 42% • Unlimited Pro"),
}

@Keep
@IgnoreExtraProperties
data class MonetizationConfig(
    val adSupportedMonetization: Boolean = true,
    val subscriptionEnabled: Boolean = false,
    val activeAdNetwork: String = AdNetwork.START_IO.name,
    val startIoAppId: String = "210382301",
    val startIoTestMode: Boolean = true,
    val unityGameId: String = "5700112",
    val unityRewardedPlacementId: String = "Rewarded_Android",
    val unityInterstitialPlacementId: String = "Interstitial_Android",
    val unityTestMode: Boolean = true,
    val adsterraDirectLinkUrl: String = "https://www.highperformanceformat.com/adsterra-demo-direct",
    val adsterraBannerTagId: String = "adsterra_banner_320x50",
    val adsterraInterstitialTagId: String = "adsterra_interstitial_main",
    val adsterraTestMode: Boolean = true,
    val interstitialEveryNBatches: Int = 2,
    val updatedByEmail: String = "waheed61762@gmail.com",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun networkEnum(): AdNetwork =
        AdNetwork.entries.firstOrNull { it.name == activeAdNetwork } ?: AdNetwork.START_IO

    fun enforceMutualExclusion(): MonetizationConfig =
        copy(subscriptionEnabled = !adSupportedMonetization)
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Creator",
    val inviteCode: String = "",
    val referredByCode: String? = null,
    val referralCount: Long = 0L,
    val bonusCredits: Long = 2L,
    val adUnlockedBatches: Long = 0L,
    val adUnlockedHd: Boolean = false,
    val adUnlockedWatermarkFree: Boolean = false,
    val subscriptionActive: Boolean = false,
    val subscriptionPlan: String = SubscriptionPlan.NONE.name,
    val batchesProcessed: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun planEnum(): SubscriptionPlan =
        SubscriptionPlan.entries.firstOrNull { it.name == subscriptionPlan } ?: SubscriptionPlan.NONE
}

@Keep
@IgnoreExtraProperties
data class GeneratedClipRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val sourceLabel: String = "",
    val formatLabel: String = "",
    val aspectRatio: String = "9:16",
    val durationMs: Long = 15_000L,
    val resolutionLabel: String = "360p",
    val outputUriString: String = "",
    val displayPath: String = "",
    val fileSizeMb: Double = 0.0,
    val discoveryMode: String = ClipDiscoveryMode.VIRAL_CLIPS.name,
    val cropMode: String = CropMode.FILL_CROP.name,
    val watermarkApplied: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun discoveryModeEnum(): ClipDiscoveryMode =
        ClipDiscoveryMode.entries.firstOrNull { it.name == discoveryMode } ?: ClipDiscoveryMode.VIRAL_CLIPS

    fun cropModeEnum(): CropMode =
        CropMode.entries.firstOrNull { it.name == cropMode } ?: CropMode.FILL_CROP
}

@Keep
@IgnoreExtraProperties
data class ReferralCodeDoc(
    @DocumentId val id: String = "",
    val code: String = "",
    val ownerUid: String = "",
    val ownerName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class ReferralClaimDoc(
    @DocumentId val id: String = "",
    val claimantUid: String = "",
    val claimantName: String = "",
    val inviterUid: String = "",
    val code: String = "",
    val creditsAwarded: Long = 5L,
    @ServerTimestamp val createdAt: Timestamp? = null,
)

sealed interface BatchGenerationState {
    data object Idle : BatchGenerationState
    data class Running(
        val currentClipIndex: Int,
        val totalClips: Int,
        val overallPercent: Int,
        val statusLabel: String,
    ) : BatchGenerationState
    data class Done(
        val generatedCount: Int,
        val savedSummary: String,
    ) : BatchGenerationState
    data class Error(val message: String) : BatchGenerationState
}
