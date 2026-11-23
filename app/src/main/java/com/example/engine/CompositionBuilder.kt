package com.example.engine

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.effect.SpeedChangeEffect
import androidx.media3.effect.StaticOverlaySettings
import androidx.media3.effect.TextOverlay
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import com.example.model.ClipTransition
import com.example.model.CropMode
import com.example.model.ExportResolution
import com.example.model.ProjectDraft
import com.example.model.VideoClip
import kotlin.math.abs

@OptIn(UnstableApi::class)
object CompositionBuilder {

    fun buildForPreview(project: ProjectDraft): Composition? {
        if (project.clips.isEmpty()) return null
        val items = project.clips.mapIndexed { index, clip ->
            buildEditedMediaItem(
                clip = clip,
                isFirstClip = index == 0,
                includeWatermark = project.watermarkEnabled,
            )
        }
        val sequence = EditedMediaItemSequence.withAudioAndVideoFrom(items)
        val compositionEffects = buildPreviewGlobalEffects(project)
        return Composition.Builder(sequence)
            .setEffects(compositionEffects)
            .build()
    }

    fun buildForExport(
        project: ProjectDraft,
        resolution: ExportResolution,
    ): Composition? {
        if (project.clips.isEmpty()) return null
        val items = project.clips.mapIndexed { index, clip ->
            buildEditedMediaItem(
                clip = clip,
                isFirstClip = index == 0,
                includeWatermark = project.watermarkEnabled,
            )
        }
        val sequence = EditedMediaItemSequence.withAudioAndVideoFrom(items)
        val (targetW, targetH) = computeExportDimensions(project, resolution)
        val layoutMode = when (project.cropMode) {
            CropMode.FILL_CROP -> Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP
            CropMode.FIT_FRAME -> Presentation.LAYOUT_SCALE_TO_FIT
        }
        val presentation = Presentation.createForWidthAndHeight(
            targetW,
            targetH,
            layoutMode,
        )
        return Composition.Builder(sequence)
            .setEffects(Effects(emptyList(), listOf(presentation)))
            .build()
    }

    fun computeExportDimensions(
        project: ProjectDraft,
        resolution: ExportResolution,
    ): Pair<Int, Int> {
        val aspect = resolveProjectAspectRatio(project)
        val rawPair = when {
            abs(aspect - 1f) < 0.05f -> resolution.shortSide to resolution.shortSide
            aspect < 1f -> {
                val h = (resolution.shortSide / aspect).toInt().coerceAtMost(resolution.longSide)
                resolution.shortSide to h
            }
            else -> {
                val w = (resolution.shortSide * aspect).toInt().coerceAtMost(resolution.longSide)
                w to resolution.shortSide
            }
        }
        val evenW = (rawPair.first and 1.inv()).coerceAtLeast(2)
        val evenH = (rawPair.second and 1.inv()).coerceAtLeast(2)
        return evenW to evenH
    }

    fun resolveProjectAspectRatio(project: ProjectDraft): Float {
        project.aspectRatio.ratio?.let { return it }
        val first = project.clips.firstOrNull() ?: return 16f / 9f
        val (w, h) = if (first.rotationDegrees % 180 != 0) {
            first.height to first.width
        } else {
            first.width to first.height
        }
        return if (h > 0) w.toFloat() / h.toFloat() else 16f / 9f
    }

    private fun buildPreviewGlobalEffects(project: ProjectDraft): Effects {
        val ratio = project.aspectRatio.ratio ?: return Effects.EMPTY
        val layoutMode = when (project.cropMode) {
            CropMode.FILL_CROP -> Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP
            CropMode.FIT_FRAME -> Presentation.LAYOUT_SCALE_TO_FIT
        }
        val presentation = Presentation.createForAspectRatio(
            ratio,
            layoutMode,
        )
        return Effects(emptyList(), listOf(presentation))
    }

    private fun buildEditedMediaItem(
        clip: VideoClip,
        isFirstClip: Boolean,
        includeWatermark: Boolean,
    ): EditedMediaItem {
        val mediaItem = MediaItem.Builder()
            .setUri(clip.uri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(clip.trimStartMs.coerceAtLeast(0L))
                    .setEndPositionMs(clip.trimEndMs.coerceAtLeast(clip.trimStartMs + 250L))
                    .build(),
            )
            .build()

        val videoEffects = mutableListOf<Effect>()
        val audioProcessors = mutableListOf<AudioProcessor>()

        if (clip.rotationDegrees % 360 != 0) {
            videoEffects.add(
                ScaleAndRotateTransformation.Builder()
                    .setRotationDegrees((360 - (clip.rotationDegrees % 360)) % 360f)
                    .build(),
            )
        }

        if (abs(clip.speed - 1.0f) > 0.01f) {
            videoEffects.add(SpeedChangeEffect(clip.speed))
            val sonic = SonicAudioProcessor().apply {
                setSpeed(clip.speed)
            }
            audioProcessors.add(sonic)
        }

        val effectiveTransition = if (isFirstClip && clip.transitionIn == ClipTransition.SLIDE_LEFT) {
            ClipTransition.NONE
        } else {
            clip.transitionIn
        }
        buildMotionMatrixEffect(
            transitionIn = effectiveTransition,
            specialEffect = clip.specialEffect,
            effectIntensity = clip.effectIntensity,
        )?.let { videoEffects.add(it) }

        val colorEffect = ColorGradingGlEffect(
            filter = clip.filter,
            filterIntensity = clip.filterIntensity,
            adjustments = clip.adjustments,
            transitionIn = effectiveTransition,
            transitionDurationMs = effectiveTransition.durationMs,
        )
        if (!colorEffect.isNoOp(clip.width, clip.height)) {
            videoEffects.add(colorEffect)
        }

        val specialFx = SpecialEffectGlEffect(
            effect = clip.specialEffect,
            intensity = clip.effectIntensity,
        )
        if (!specialFx.isNoOp(clip.width, clip.height)) {
            videoEffects.add(specialFx)
        }

        val overlays = mutableListOf<TextOverlay>()
        val overlayText = clip.textOverlay?.text?.trim().orEmpty()
        if (overlayText.isNotEmpty()) {
            val overlay = clip.textOverlay!!
            val spannable = SpannableString(overlayText).apply {
                setSpan(ForegroundColorSpan(overlay.colorArgb), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(AbsoluteSizeSpan(28, true), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            val settings = StaticOverlaySettings.Builder()
                .setBackgroundFrameAnchor(0f, overlay.verticalPosition)
                .setOverlayFrameAnchor(0f, 0f)
                .setScale(overlay.scale, overlay.scale)
                .build()
            overlays.add(TextOverlay.createStaticTextOverlay(spannable, settings))
        }

        if (includeWatermark) {
            val wmSpan = SpannableString("VibeCut").apply {
                setSpan(ForegroundColorSpan(0xCCFFFFFF.toInt()), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(AbsoluteSizeSpan(18, true), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            val wmSettings = StaticOverlaySettings.Builder()
                .setBackgroundFrameAnchor(0.74f, -0.84f)
                .setOverlayFrameAnchor(0f, 0f)
                .build()
            overlays.add(TextOverlay.createStaticTextOverlay(wmSpan, wmSettings))
        }

        if (overlays.isNotEmpty()) {
            videoEffects.add(OverlayEffect(overlays.toList()))
        }

        return EditedMediaItem.Builder(mediaItem)
            .setDurationUs(clip.sourceDurationMs * 1000L)
            .setRemoveAudio(clip.isMuted || clip.volume <= 0.01f)
            .setEffects(Effects(audioProcessors, videoEffects))
            .build()
    }
}
