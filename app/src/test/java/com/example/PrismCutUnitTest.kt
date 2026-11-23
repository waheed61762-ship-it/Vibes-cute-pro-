package com.example

import com.example.ads.AdOrchestrator
import com.example.engine.ClipBatchPlanner
import com.example.engine.CompositionBuilder
import com.example.model.AspectRatioPreset
import com.example.model.ClipDiscoveryMode
import com.example.model.ClipLengthOption
import com.example.model.ExportResolution
import com.example.model.MonetizationConfig
import com.example.model.ProjectDraft
import com.example.model.VideoClip
import com.example.ui.formatTimecode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrismCutUnitTest {

    @Test
    fun monetizationMutualExclusion_togglesSubscriptionInverse() {
        val adsOn = MonetizationConfig(adSupportedMonetization = true, subscriptionEnabled = true)
            .enforceMutualExclusion()
        assertTrue(adsOn.adSupportedMonetization)
        assertFalse(adsOn.subscriptionEnabled)

        val adsOff = MonetizationConfig(adSupportedMonetization = false, subscriptionEnabled = false)
            .enforceMutualExclusion()
        assertFalse(adsOff.adSupportedMonetization)
        assertTrue(adsOff.subscriptionEnabled)
    }

    @Test
    fun unityPlacementValidation_catchesEmptyOrInvalidPlacements() {
        val emptyPlacement = AdOrchestrator.validateUnityPlacement("5700112", "")
        assertFalse(emptyPlacement.isValid)

        val valid = AdOrchestrator.validateUnityPlacement("5700112", "Rewarded_Android")
        assertTrue(valid.isValid)
    }

    @Test
    fun clipBatchPlanner_respectsRequestedCountAndBounds() {
        val planned = ClipBatchPlanner.planSegments(
            sourceDurationMs = 120_000L,
            clipLength = ClipLengthOption.SEC_15,
            requestedCount = 8,
            discoveryMode = ClipDiscoveryMode.VIRAL_CLIPS,
        )
        assertEquals(8, planned.size)
        planned.forEach { seg ->
            assertTrue(seg.trimStartMs >= 0L)
            assertTrue(seg.trimEndMs <= 120_000L)
            assertTrue(seg.durationMs >= 250L)
        }
    }

    @Test
    fun exportDimensions_matchAspectRatioAndEvenBounds() {
        val sampleClip = VideoClip(
            uriString = "content://media/external/video/1",
            displayName = "Clip 1",
            sourceDurationMs = 10_000L,
            width = 1920,
            height = 1080,
        )

        val project16x9 = ProjectDraft(
            title = "Landscape",
            aspectRatio = AspectRatioPreset.RATIO_16_9,
            clips = listOf(sampleClip),
        )
        assertEquals(1920 to 1080, CompositionBuilder.computeExportDimensions(project16x9, ExportResolution.FHD_1080P))
        assertEquals(3840 to 2160, CompositionBuilder.computeExportDimensions(project16x9, ExportResolution.UHD_4K))
        assertEquals(7680 to 4320, CompositionBuilder.computeExportDimensions(project16x9, ExportResolution.UHD_8K))

        val project9x16 = project16x9.copy(aspectRatio = AspectRatioPreset.RATIO_9_16)
        assertEquals(1080 to 1920, CompositionBuilder.computeExportDimensions(project9x16, ExportResolution.FHD_1080P))
        assertEquals(2160 to 3840, CompositionBuilder.computeExportDimensions(project9x16, ExportResolution.UHD_4K))
    }

    @Test
    fun clipEffectiveDuration_accountsForTrimAndSpeed() {
        val clip = VideoClip(
            uriString = "content://media/external/video/2",
            displayName = "Fast Clip",
            sourceDurationMs = 12_000L,
            width = 1080,
            height = 1920,
            trimStartMs = 2_000L,
            trimEndMs = 8_000L,
            speed = 2.0f,
        )
        assertEquals(6_000L, clip.trimmedSourceDurationMs)
        assertEquals(3_000L, clip.effectiveDurationMs)
    }

    @Test
    fun timecodeFormatter_formatsMinutesSecondsCentiseconds() {
        assertEquals("00:00.00", formatTimecode(0L))
        assertEquals("01:05.25", formatTimecode(65_250L))
    }
}
