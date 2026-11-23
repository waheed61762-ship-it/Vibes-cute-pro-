package com.example.engine

import android.media.MediaCodecList
import android.media.MediaFormat
import com.example.model.DeviceEncoderCapabilities

object CodecCapabilityChecker {

    fun inspect(): DeviceEncoderCapabilities {
        var maxAvcW = 1920
        var maxAvcH = 1080
        var maxHevcW = 1920
        var maxHevcH = 1080

        runCatching {
            val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            for (info in codecList.codecInfos) {
                if (!info.isEncoder) continue
                for (type in info.supportedTypes) {
                    val caps = runCatching { info.getCapabilitiesForType(type) }.getOrNull() ?: continue
                    val videoCaps = caps.videoCapabilities ?: continue
                    val w = videoCaps.supportedWidths.upper
                    val h = videoCaps.supportedHeights.upper
                    if (type.equals(MediaFormat.MIMETYPE_VIDEO_AVC, ignoreCase = true)) {
                        if (w * h > maxAvcW * maxAvcH) {
                            maxAvcW = w
                            maxAvcH = h
                        }
                    } else if (type.equals(MediaFormat.MIMETYPE_VIDEO_HEVC, ignoreCase = true)) {
                        if (w * h > maxHevcW * maxHevcH) {
                            maxHevcW = w
                            maxHevcH = h
                        }
                    }
                }
            }
        }

        val maxLong = maxOf(maxAvcW, maxAvcH, maxHevcW, maxHevcH)
        val maxShort = maxOf(minOf(maxAvcW, maxAvcH), minOf(maxHevcW, maxHevcH))
        val supports4k = maxLong >= 3840 && maxShort >= 2160
        val supports8k = maxLong >= 7680 && maxShort >= 4320

        val summary = when {
            supports8k -> "Hardware 8K HEVC Ready"
            supports4k -> "Hardware 4K Ready"
            else -> "Hardware 1080p Full HD"
        }

        return DeviceEncoderCapabilities(
            maxAvcWidth = maxAvcW,
            maxAvcHeight = maxAvcH,
            maxHevcWidth = maxHevcW,
            maxHevcHeight = maxHevcH,
            supports4k = supports4k,
            supports8k = supports8k,
            hardwareSummary = summary,
        )
    }
}
