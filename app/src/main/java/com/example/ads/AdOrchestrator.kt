package com.example.ads

import com.example.model.AdNetwork
import com.example.model.AdPlacementType
import com.example.model.MonetizationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class PlacementValidationResult(
    val isValid: Boolean,
    val message: String,
)

sealed interface AdSessionState {
    data object Idle : AdSessionState

    data class Loading(
        val network: AdNetwork,
        val placement: AdPlacementType,
        val statusLabel: String,
    ) : AdSessionState

    data class TestSandboxAd(
        val network: AdNetwork,
        val placement: AdPlacementType,
        val configLabel: String,
    ) : AdSessionState

    data class AdsterraWebContainer(
        val placement: AdPlacementType,
        val targetUrl: String,
        val tagId: String,
        val testMode: Boolean,
    ) : AdSessionState

    data class NonBlockingNotice(
        val network: AdNetwork,
        val placement: AdPlacementType,
        val errorTitle: String,
        val errorDetail: String,
        val httpCode: Int? = null,
    ) : AdSessionState
}

object AdOrchestrator {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun validateUnityPlacement(
        gameId: String,
        placementId: String,
    ): PlacementValidationResult {
        val cleanGame = gameId.trim()
        val cleanPlacement = placementId.trim()
        if (cleanGame.isEmpty() || !cleanGame.matches(Regex("^[0-9]{4,12}$"))) {
            return PlacementValidationResult(
                isValid = false,
                message = "Unity Game ID must be a 4–12 digit numeric ID.",
            )
        }
        if (cleanPlacement.isEmpty()) {
            return PlacementValidationResult(
                isValid = false,
                message = "Unity Placement ID is empty. Enter a valid placement such as Rewarded_Android or Interstitial_Android.",
            )
        }
        if (!cleanPlacement.matches(Regex("^[A-Za-z0-9_\\-]{3,64}$"))) {
            return PlacementValidationResult(
                isValid = false,
                message = "Unity Placement ID '$cleanPlacement' contains invalid characters.",
            )
        }
        return PlacementValidationResult(
            isValid = true,
            message = "Unity placement '$cleanPlacement' validated for Game ID $cleanGame.",
        )
    }

    fun validateNetworkConfig(
        config: MonetizationConfig,
        placement: AdPlacementType,
    ): PlacementValidationResult {
        return when (config.networkEnum()) {
            AdNetwork.START_IO -> {
                val appId = config.startIoAppId.trim()
                if (appId.isEmpty() || !appId.matches(Regex("^[0-9]{5,15}$"))) {
                    PlacementValidationResult(
                        false,
                        "Start.io App ID is missing or invalid (example: 210382301).",
                    )
                } else {
                    PlacementValidationResult(true, "Start.io App ID $appId ready.")
                }
            }
            AdNetwork.UNITY_ADS -> {
                val placementId = if (placement == AdPlacementType.INTERSTITIAL_BATCH) {
                    config.unityInterstitialPlacementId
                } else {
                    config.unityRewardedPlacementId
                }
                validateUnityPlacement(config.unityGameId, placementId)
            }
            AdNetwork.ADSTERRA -> {
                val url = config.adsterraDirectLinkUrl.trim()
                val tag = if (placement == AdPlacementType.INTERSTITIAL_BATCH) {
                    config.adsterraInterstitialTagId.trim()
                } else {
                    config.adsterraBannerTagId.trim()
                }
                if (!url.startsWith("https://") && !url.startsWith("http://")) {
                    PlacementValidationResult(
                        false,
                        "Adsterra Direct Link / Ad URL must start with https:// or http://.",
                    )
                } else if (tag.isEmpty()) {
                    PlacementValidationResult(
                        false,
                        "Adsterra Placement Tag ID is unconfigured.",
                    )
                } else {
                    PlacementValidationResult(true, "Adsterra tag '$tag' ready.")
                }
            }
        }
    }

    suspend fun requestAd(
        config: MonetizationConfig,
        placement: AdPlacementType,
    ): AdSessionState = withContext(Dispatchers.IO) {
        val network = config.networkEnum()
        val validation = validateNetworkConfig(config, placement)
        if (!validation.isValid) {
            return@withContext AdSessionState.NonBlockingNotice(
                network = network,
                placement = placement,
                errorTitle = "${network.displayName} Placement Not Configured",
                errorDetail = validation.message,
            )
        }

        when (network) {
            AdNetwork.START_IO -> {
                if (config.startIoTestMode) {
                    AdSessionState.TestSandboxAd(
                        network = AdNetwork.START_IO,
                        placement = placement,
                        configLabel = "Start.io Test Mode • App ID ${config.startIoAppId.trim()}",
                    )
                } else {
                    probeLiveAdEndpoint(
                        network = AdNetwork.START_IO,
                        placement = placement,
                        probeUrl = "https://req.startappservice.com/1.4/getad?appId=${config.startIoAppId.trim()}",
                    )
                }
            }
            AdNetwork.UNITY_ADS -> {
                val placementId = if (placement == AdPlacementType.INTERSTITIAL_BATCH) {
                    config.unityInterstitialPlacementId.trim()
                } else {
                    config.unityRewardedPlacementId.trim()
                }
                if (config.unityTestMode) {
                    AdSessionState.TestSandboxAd(
                        network = AdNetwork.UNITY_ADS,
                        placement = placement,
                        configLabel = "Unity Ads Test • Game ${config.unityGameId.trim()} • $placementId",
                    )
                } else {
                    probeLiveAdEndpoint(
                        network = AdNetwork.UNITY_ADS,
                        placement = placement,
                        probeUrl = "https://auction.unityads.unity3d.com/v6/games/${config.unityGameId.trim()}/requests",
                    )
                }
            }
            AdNetwork.ADSTERRA -> {
                val tag = if (placement == AdPlacementType.INTERSTITIAL_BATCH) {
                    config.adsterraInterstitialTagId.trim()
                } else {
                    config.adsterraBannerTagId.trim()
                }
                AdSessionState.AdsterraWebContainer(
                    placement = placement,
                    targetUrl = config.adsterraDirectLinkUrl.trim(),
                    tagId = tag,
                    testMode = config.adsterraTestMode,
                )
            }
        }
    }

    private suspend fun probeLiveAdEndpoint(
        network: AdNetwork,
        placement: AdPlacementType,
        probeUrl: String,
    ): AdSessionState {
        val result = withTimeoutOrNull(6_000L) {
            runCatching {
                val req = Request.Builder()
                    .url(probeUrl)
                    .get()
                    .header("User-Agent", "VibeCut/2.1 Android")
                    .build()
                client.newCall(req).execute().use { resp ->
                    resp.code
                }
            }
        }

        if (result == null) {
            return AdSessionState.NonBlockingNotice(
                network = network,
                placement = placement,
                errorTitle = "Ad Request Timed Out",
                errorDetail = "${network.displayName} took too long to respond. Retry or continue safely without blocking your workflow.",
            )
        }

        return result.fold(
            onSuccess = { code ->
                when (code) {
                    204 -> AdSessionState.NonBlockingNotice(
                        network = network,
                        placement = placement,
                        errorTitle = "No Ad Available (HTTP 204)",
                        errorDetail = "${network.displayName} returned HTTP 204 No-Fill for this placement. You can retry now or continue with non-blocking access.",
                        httpCode = 204,
                    )
                    in 200..299 -> AdSessionState.TestSandboxAd(
                        network = network,
                        placement = placement,
                        configLabel = "${network.displayName} Live Placement Verified",
                    )
                    else -> AdSessionState.NonBlockingNotice(
                        network = network,
                        placement = placement,
                        errorTitle = "Ad Unavailable (HTTP $code)",
                        errorDetail = "${network.displayName} did not fill this placement right now. Retry or continue smoothly.",
                        httpCode = code,
                    )
                }
            },
            onFailure = { err ->
                AdSessionState.NonBlockingNotice(
                    network = network,
                    placement = placement,
                    errorTitle = "Network Unavailable",
                    errorDetail = "${err.localizedMessage ?: "Connection failed"}. You can retry or continue without waiting.",
                )
            },
        )
    }
}
