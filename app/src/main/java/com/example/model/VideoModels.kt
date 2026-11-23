package com.example.model

import android.graphics.Bitmap
import android.net.Uri
import java.util.UUID

enum class VideoFilter(
    val displayName: String,
    val badgeColorHex: Long,
    val description: String,
) {
    NONE("Original", 0xFF64708F, "Unprocessed camera colors"),
    BLACK_AND_WHITE("B&W Noir", 0xFF94A3B8, "High-contrast monochrome cinema"),
    SEPIA("Sepia", 0xFFD97706, "Warm archival silver-halide tone"),
    VINTAGE("Vintage", 0xFFEAB308, "Retro 70s faded film stock"),
    CINEMATIC("Teal & Gold", 0xFF06B6D4, "Blockbuster dual-tone grade"),
    VIVID("Vivid Pop", 0xFFF43F5E, "Punchy contrast & rich colors"),
    WARM("Golden Hour", 0xFFFB923C, "Sunset amber warmth"),
    COOL("Arctic Cool", 0xFF38BDF8, "Crisp cobalt shadows"),
    FADE("Matte Fade", 0xFFA78BFA, "Lifted blacks & pastel highlights"),
    CYBERPUNK("Neon Cyber", 0xFFE879F9, "Electric magenta-cyan night grade"),
}

enum class SpecialEffect(
    val displayName: String,
    val description: String,
) {
    NONE("None", "No special FX"),
    BLUR("Soft Blur", "Smooth 9-tap optical blur"),
    GLITCH("RGB Glitch", "Chromatic split + scanline tear"),
    VIGNETTE("Vignette", "Cinematic radial edge darkening"),
    PIXELATE("Retro Pixel", "8-bit mosaic quantization"),
    BEAT_ZOOM("Beat Zoom", "Rhythmic subtle zoom pulse"),
}

enum class ClipTransition(
    val displayName: String,
    val durationMs: Long,
) {
    NONE("Cut", 0L),
    FADE_BLACK("Fade Black", 650L),
    FLASH_WHITE("Flash White", 450L),
    ZOOM_IN("Zoom Push", 550L),
    SLIDE_LEFT("Slide In", 500L),
}

enum class AspectRatioPreset(
    val label: String,
    val ratio: Float?,
    val subtitle: String,
) {
    ORIGINAL("Auto", null, "Match first clip"),
    RATIO_9_16("9:16", 9f / 16f, "Reels / TikTok / Shorts"),
    RATIO_16_9("16:9", 16f / 9f, "YouTube / Landscape"),
    RATIO_1_1("1:1", 1f, "Square Feed"),
    RATIO_4_5("4:5", 4f / 5f, "Facebook / Portrait"),
}

enum class CropMode(
    val label: String,
    val subtitle: String,
) {
    FILL_CROP("Fill & Crop", "Edge-to-edge full frame"),
    FIT_FRAME("Fit Frame", "Keep entire frame visible"),
}

enum class ExportResolution(
    val label: String,
    val shortSide: Int,
    val longSide: Int,
    val defaultBitrateMbps: Int,
    val badge: String,
) {
    SD_360P("360p SD", 360, 640, 3, "Free Tier"),
    HD_720P("720p HD", 720, 1280, 8, "HD Unlock"),
    FHD_1080P("1080p FHD", 1080, 1920, 18, "Recommended"),
    QHD_1440P("1440p UHD", 1440, 2560, 28, "Ultra Sharp"),
    UHD_4K("4K Ultra HD", 2160, 3840, 45, "Cinema 4K"),
    UHD_8K("8K Master", 4320, 7680, 95, "Flagship"),
}

enum class VideoCodecOption(
    val label: String,
    val mimeType: String,
    val note: String,
) {
    H264("H.264 (AVC)", "video/avc", "Max compatibility"),
    H265("H.265 (HEVC)", "video/hevc", "Best for 4K / 8K"),
}

data class ColorAdjustments(
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
)

data class ClipTextOverlay(
    val text: String = "",
    val colorArgb: Int = 0xFFFFFFFF.toInt(),
    val scale: Float = 1.0f,
    val verticalPosition: Float = 0.72f,
)

data class VideoClip(
    val id: String = UUID.randomUUID().toString(),
    val uriString: String,
    val displayName: String,
    val sourceDurationMs: Long,
    val width: Int,
    val height: Int,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = sourceDurationMs,
    val speed: Float = 1.0f,
    val isMuted: Boolean = false,
    val volume: Float = 1.0f,
    val rotationDegrees: Int = 0,
    val filter: VideoFilter = VideoFilter.NONE,
    val filterIntensity: Float = 1.0f,
    val specialEffect: SpecialEffect = SpecialEffect.NONE,
    val effectIntensity: Float = 0.75f,
    val adjustments: ColorAdjustments = ColorAdjustments(),
    val transitionIn: ClipTransition = ClipTransition.NONE,
    val textOverlay: ClipTextOverlay? = null,
    val thumbnails: List<Bitmap> = emptyList(),
) {
    val uri: Uri get() = Uri.parse(uriString)

    val trimmedSourceDurationMs: Long
        get() = (trimEndMs - trimStartMs).coerceAtLeast(250L)

    val effectiveDurationMs: Long
        get() = (trimmedSourceDurationMs / speed.coerceIn(0.25f, 4.0f)).toLong().coerceAtLeast(200L)
}

data class ProjectDraft(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val aspectRatio: AspectRatioPreset = AspectRatioPreset.ORIGINAL,
    val cropMode: CropMode = CropMode.FILL_CROP,
    val watermarkEnabled: Boolean = false,
    val clips: List<VideoClip> = emptyList(),
) {
    val totalDurationMs: Long
        get() = clips.sumOf { it.effectiveDurationMs }
}

data class ExportConfig(
    val resolution: ExportResolution = ExportResolution.FHD_1080P,
    val fps: Int = 30,
    val codec: VideoCodecOption = VideoCodecOption.H264,
    val bitrateMbps: Int = resolution.defaultBitrateMbps,
)

data class DeviceEncoderCapabilities(
    val maxAvcWidth: Int,
    val maxAvcHeight: Int,
    val maxHevcWidth: Int,
    val maxHevcHeight: Int,
    val supports4k: Boolean,
    val supports8k: Boolean,
    val hardwareSummary: String,
) {
    fun supports(resolution: ExportResolution, codec: VideoCodecOption): Boolean {
        val maxW = if (codec == VideoCodecOption.H265) maxHevcWidth else maxAvcWidth
        val maxH = if (codec == VideoCodecOption.H265) maxHevcHeight else maxAvcHeight
        val maxLong = maxOf(maxW, maxH)
        val maxShort = minOf(maxW, maxH)
        return maxLong >= resolution.longSide && maxShort >= resolution.shortSide
    }
}

sealed interface ExportUiState {
    data object Idle : ExportUiState
    data class Exporting(
        val progressPercent: Int,
        val elapsedSeconds: Int,
        val statusLabel: String,
    ) : ExportUiState
    data class Success(
        val outputUriString: String,
        val displayPath: String,
        val fileSizeMb: Double,
        val resolutionLabel: String,
        val elapsedSeconds: Int,
    ) : ExportUiState
    data class Error(val message: String) : ExportUiState
}

enum class EditorToolTab(val label: String) {
    EDIT("Edit"),
    FILTERS("Filters"),
    ADJUST("Adjust"),
    EFFECTS("Effects"),
    TRANSITION("Transition"),
    TEXT("Text"),
}
