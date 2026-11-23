package com.example.engine

import com.example.model.ClipDiscoveryMode
import com.example.model.ClipLengthOption
import com.example.model.PlannedClipSegment

object ClipBatchPlanner {

    fun planSegments(
        sourceDurationMs: Long,
        clipLength: ClipLengthOption,
        requestedCount: Int,
        discoveryMode: ClipDiscoveryMode,
    ): List<PlannedClipSegment> {
        val safeDuration = sourceDurationMs.coerceAtLeast(1_000L)
        val count = requestedCount.coerceIn(1, 40)
        val targetLen = clipLength.durationMs.coerceAtMost(safeDuration).coerceAtLeast(1_000L)

        if (count == 1) {
            val start = when (discoveryMode) {
                ClipDiscoveryMode.VIRAL_CLIPS -> (safeDuration * 0.10f).toLong().coerceAtMost((safeDuration - targetLen).coerceAtLeast(0L))
                ClipDiscoveryMode.SMART_HIGHLIGHTS -> ((safeDuration - targetLen) / 2L).coerceAtLeast(0L)
                ClipDiscoveryMode.SEQUENTIAL -> 0L
            }
            val end = (start + targetLen).coerceAtMost(safeDuration)
            return listOf(
                PlannedClipSegment(
                    index = 0,
                    title = "Clip 1 • ${discoveryMode.label}",
                    trimStartMs = start,
                    trimEndMs = end,
                    discoveryReason = reasonLabel(discoveryMode, 0),
                ),
            )
        }

        val usableSpan = (safeDuration - targetLen).coerceAtLeast(0L)
        return List(count) { idx ->
            val startMs = when (discoveryMode) {
                ClipDiscoveryMode.SEQUENTIAL -> {
                    val raw = idx * targetLen
                    if (raw + targetLen <= safeDuration) {
                        raw
                    } else {
                        ((usableSpan * idx) / (count - 1).coerceAtLeast(1))
                    }
                }
                ClipDiscoveryMode.SMART_HIGHLIGHTS -> {
                    if (usableSpan == 0L) 0L
                    else (usableSpan * idx) / (count - 1).coerceAtLeast(1)
                }
                ClipDiscoveryMode.VIRAL_CLIPS -> {
                    if (usableSpan == 0L) {
                        0L
                    } else {
                        // Front-load engaging hook anchors + distribute remaining peak windows
                        val anchorFraction = when (idx) {
                            0 -> 0.04f
                            1 -> 0.28f
                            2 -> 0.56f
                            else -> (idx.toFloat() / (count - 1).coerceAtLeast(1)).coerceIn(0f, 1f)
                        }
                        (usableSpan * anchorFraction).toLong()
                    }
                }
            }.coerceIn(0L, (safeDuration - 500L).coerceAtLeast(0L))

            val endMs = (startMs + targetLen).coerceAtMost(safeDuration).coerceAtLeast(startMs + 500L)
            PlannedClipSegment(
                index = idx,
                title = "Clip ${idx + 1} • ${clipLength.label}",
                trimStartMs = startMs,
                trimEndMs = endMs,
                discoveryReason = reasonLabel(discoveryMode, idx),
            )
        }
    }

    private fun reasonLabel(mode: ClipDiscoveryMode, idx: Int): String = when (mode) {
        ClipDiscoveryMode.VIRAL_CLIPS -> when (idx) {
            0 -> "Opening Hook Peak"
            1 -> "High-Retention Pivot"
            2 -> "Climax Highlight"
            else -> "Viral Moment #${idx + 1}"
        }
        ClipDiscoveryMode.SMART_HIGHLIGHTS -> "Highlight Window #${idx + 1}"
        ClipDiscoveryMode.SEQUENTIAL -> "Part ${idx + 1}"
    }
}
