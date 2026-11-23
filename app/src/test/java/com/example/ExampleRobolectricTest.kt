package com.example

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.ads.AdSessionState
import com.example.model.AdNetwork
import com.example.model.AdPlacementType
import com.example.model.DeviceEncoderCapabilities
import com.example.model.EditorToolTab
import com.example.model.ExportConfig
import com.example.model.ExportUiState
import com.example.model.MonetizationConfig
import com.example.model.ProjectDraft
import com.example.model.UserProfile
import com.example.model.VideoClip
import com.example.ui.components.AdExperienceDialog
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.InfoTabScreen
import com.example.ui.theme.PrismCutTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("VibeCut", appName)
    }

    @Test
    fun `info tab shows developer attribution, privacy modal, and admin trigger`() {
        var adminOpened = false
        composeTestRule.setContent {
            PrismCutTheme {
                InfoTabScreen(
                    monetizationConfig = MonetizationConfig(adSupportedMonetization = true).enforceMutualExclusion(),
                    userProfile = UserProfile(displayName = "Irfan", inviteCode = "VC-IRFAN1"),
                    userEmail = "waheed61762@gmail.com",
                    onOpenAdminPanel = { adminOpened = true },
                    onOpenSubscriptionPaywall = {},
                    onSignOutClick = {},
                    onShowToast = {},
                )
            }
        }

        composeTestRule.onNodeWithText("VibeCut (v2.1)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Developer: Irfan Ali Channa").assertIsDisplayed()
        composeTestRule.onNodeWithTag("open_admin_panel_button").performScrollTo().performClick()
        assertTrue(adminOpened)
    }

    @Test
    fun `non blocking ad notice lets user retry or continue smoothly`() {
        var retried = false
        var continuedGrace = false

        composeTestRule.setContent {
            PrismCutTheme {
                AdExperienceDialog(
                    state = AdSessionState.NonBlockingNotice(
                        network = AdNetwork.UNITY_ADS,
                        placement = AdPlacementType.REWARDED_BATCH,
                        errorTitle = "No Ad Available (HTTP 204)",
                        errorDetail = "Placement returned HTTP 204.",
                        httpCode = 204,
                    ),
                    bonusCredits = 2L,
                    onRetry = { retried = true },
                    onCompleteUnlock = { grace -> if (grace) continuedGrace = true },
                    onSpendBonusCredit = {},
                    onDismiss = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("ad_retry_button").performClick()
        composeTestRule.onNodeWithTag("ad_grace_continue_button").performClick()
        assertTrue(retried)
        assertTrue(continuedGrace)
    }

    @Test
    fun `editor screen switches tabs and opens export studio`() {
        val caps = DeviceEncoderCapabilities(
            maxAvcWidth = 1920,
            maxAvcHeight = 1080,
            maxHevcWidth = 3840,
            maxHevcHeight = 2160,
            supports4k = true,
            supports8k = false,
            hardwareSummary = "Hardware 4K Ready",
        )
        val project = ProjectDraft(
            title = "Test Cut",
            clips = listOf(
                VideoClip(
                    uriString = "content://media/external/video/1",
                    displayName = "DemoClip",
                    sourceDurationMs = 8_000L,
                    width = 1080,
                    height = 1920,
                ),
                VideoClip(
                    uriString = "content://media/external/video/2",
                    displayName = "SecondClip",
                    sourceDurationMs = 6_000L,
                    width = 1080,
                    height = 1920,
                ),
            ),
        )
        val activeTabState = mutableStateOf(EditorToolTab.EDIT)

        composeTestRule.setContent {
            PrismCutTheme {
                EditorScreen(
                    project = project,
                    player = null,
                    selectedClipIndex = 0,
                    activeTab = activeTabState.value,
                    isPlaying = false,
                    currentPositionMs = 1500L,
                    capabilities = caps,
                    exportConfig = ExportConfig(),
                    exportState = ExportUiState.Idle,
                    onBack = {},
                    onPausePlayback = {},
                    onPlayPause = {},
                    onSeekTo = {},
                    onStepRelative = {},
                    onSelectClip = {},
                    onSelectTab = { activeTabState.value = it },
                    onAspectRatioChange = {},
                    onAddClipsClick = {},
                    onTrimChange = { _, _, _ -> },
                    onSplitAtPlayhead = {},
                    onSpeedChange = {},
                    onToggleMute = {},
                    onRotate90 = {},
                    onMoveClip = {},
                    onDuplicateClip = {},
                    onDeleteClip = {},
                    onFilterChange = { _, _ -> },
                    onApplyFilterToAll = {},
                    onAdjustmentsChange = {},
                    onResetAdjustments = {},
                    onSpecialEffectChange = { _, _ -> },
                    onApplyEffectToAll = {},
                    onTransitionChange = { _, _ -> },
                    onTextOverlayChange = {},
                    onUpdateExportConfig = {},
                    onStartExport = {},
                    onCancelExport = {},
                    onResetExportState = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("split_clip_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tool_tab_filters").performClick()
        composeTestRule.waitForIdle()
        assertEquals(EditorToolTab.FILTERS, activeTabState.value)
        composeTestRule.onNodeWithText("Visual Filters").assertIsDisplayed()
        composeTestRule.onNodeWithTag("open_export_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Export Studio").assertIsDisplayed()
        composeTestRule.onNodeWithTag("export_res_uhd_4k").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("export_res_uhd_8k").performScrollTo().assertIsDisplayed()
    }
}
