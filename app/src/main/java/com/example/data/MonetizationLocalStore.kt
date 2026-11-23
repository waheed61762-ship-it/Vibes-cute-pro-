package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AdNetwork
import com.example.model.MonetizationConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.monetizationDataStore by preferencesDataStore(name = "vibecut_monetization_prefs")

class MonetizationLocalStore(private val context: Context) {

    private object Keys {
        val AD_SUPPORTED = booleanPreferencesKey("ad_supported_monetization")
        val ACTIVE_NETWORK = stringPreferencesKey("active_ad_network")
        val STARTIO_APP_ID = stringPreferencesKey("startio_app_id")
        val STARTIO_TEST_MODE = booleanPreferencesKey("startio_test_mode")
        val UNITY_GAME_ID = stringPreferencesKey("unity_game_id")
        val UNITY_REWARDED_ID = stringPreferencesKey("unity_rewarded_placement_id")
        val UNITY_INTERSTITIAL_ID = stringPreferencesKey("unity_interstitial_placement_id")
        val UNITY_TEST_MODE = booleanPreferencesKey("unity_test_mode")
        val ADSTERRA_DIRECT_URL = stringPreferencesKey("adsterra_direct_link_url")
        val ADSTERRA_BANNER_TAG = stringPreferencesKey("adsterra_banner_tag_id")
        val ADSTERRA_INTERSTITIAL_TAG = stringPreferencesKey("adsterra_interstitial_tag_id")
        val ADSTERRA_TEST_MODE = booleanPreferencesKey("adsterra_test_mode")
        val INTERSTITIAL_EVERY_N = intPreferencesKey("interstitial_every_n_batches")
        val UPDATED_BY = stringPreferencesKey("updated_by_email")
    }

    val configFlow: Flow<MonetizationConfig> = context.monetizationDataStore.data.map { prefs ->
        val adSupported = prefs[Keys.AD_SUPPORTED] ?: true
        MonetizationConfig(
            adSupportedMonetization = adSupported,
            subscriptionEnabled = !adSupported,
            activeAdNetwork = prefs[Keys.ACTIVE_NETWORK] ?: AdNetwork.START_IO.name,
            startIoAppId = prefs[Keys.STARTIO_APP_ID] ?: "210382301",
            startIoTestMode = prefs[Keys.STARTIO_TEST_MODE] ?: true,
            unityGameId = prefs[Keys.UNITY_GAME_ID] ?: "5700112",
            unityRewardedPlacementId = prefs[Keys.UNITY_REWARDED_ID] ?: "Rewarded_Android",
            unityInterstitialPlacementId = prefs[Keys.UNITY_INTERSTITIAL_ID] ?: "Interstitial_Android",
            unityTestMode = prefs[Keys.UNITY_TEST_MODE] ?: true,
            adsterraDirectLinkUrl = prefs[Keys.ADSTERRA_DIRECT_URL]
                ?: "https://www.highperformanceformat.com/adsterra-demo-direct",
            adsterraBannerTagId = prefs[Keys.ADSTERRA_BANNER_TAG] ?: "adsterra_banner_320x50",
            adsterraInterstitialTagId = prefs[Keys.ADSTERRA_INTERSTITIAL_TAG] ?: "adsterra_interstitial_main",
            adsterraTestMode = prefs[Keys.ADSTERRA_TEST_MODE] ?: true,
            interstitialEveryNBatches = (prefs[Keys.INTERSTITIAL_EVERY_N] ?: 2).coerceIn(1, 20),
            updatedByEmail = prefs[Keys.UPDATED_BY] ?: "waheed61762@gmail.com",
        ).enforceMutualExclusion()
    }

    suspend fun save(config: MonetizationConfig) {
        val normalized = config.enforceMutualExclusion()
        context.monetizationDataStore.edit { prefs ->
            prefs[Keys.AD_SUPPORTED] = normalized.adSupportedMonetization
            prefs[Keys.ACTIVE_NETWORK] = normalized.activeAdNetwork
            prefs[Keys.STARTIO_APP_ID] = normalized.startIoAppId
            prefs[Keys.STARTIO_TEST_MODE] = normalized.startIoTestMode
            prefs[Keys.UNITY_GAME_ID] = normalized.unityGameId
            prefs[Keys.UNITY_REWARDED_ID] = normalized.unityRewardedPlacementId
            prefs[Keys.UNITY_INTERSTITIAL_ID] = normalized.unityInterstitialPlacementId
            prefs[Keys.UNITY_TEST_MODE] = normalized.unityTestMode
            prefs[Keys.ADSTERRA_DIRECT_URL] = normalized.adsterraDirectLinkUrl
            prefs[Keys.ADSTERRA_BANNER_TAG] = normalized.adsterraBannerTagId
            prefs[Keys.ADSTERRA_INTERSTITIAL_TAG] = normalized.adsterraInterstitialTagId
            prefs[Keys.ADSTERRA_TEST_MODE] = normalized.adsterraTestMode
            prefs[Keys.INTERSTITIAL_EVERY_N] = normalized.interstitialEveryNBatches.coerceIn(1, 20)
            prefs[Keys.UPDATED_BY] = normalized.updatedByEmail
        }
    }
}
