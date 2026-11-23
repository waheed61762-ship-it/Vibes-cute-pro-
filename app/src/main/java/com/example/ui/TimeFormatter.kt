package com.example.ui

import java.util.Locale

fun formatTimecode(ms: Long): String {
    val safeMs = ms.coerceAtLeast(0L)
    val totalSeconds = safeMs / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    val centiseconds = (safeMs % 1000L) / 10L
    return String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, centiseconds)
}

fun formatDurationShort(ms: Long): String {
    val safeMs = ms.coerceAtLeast(0L)
    val totalSeconds = safeMs / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return if (minutes > 0) {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    } else {
        String.format(Locale.US, "%.1fs", safeMs / 1000f)
    }
}
