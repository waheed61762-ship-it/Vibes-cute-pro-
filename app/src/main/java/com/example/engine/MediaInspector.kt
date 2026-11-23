package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.model.VideoClip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MediaInspector {

    suspend fun inspectVideoClip(context: Context, uri: Uri): VideoClip? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val durationMs = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.coerceAtLeast(500L) ?: return@withContext null

            val rawWidth = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull() ?: 1920
            val rawHeight = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull() ?: 1080
            val rotation = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                ?.toIntOrNull() ?: 0

            val (width, height) = if (rotation == 90 || rotation == 270) {
                rawHeight to rawWidth
            } else {
                rawWidth to rawHeight
            }

            val displayName = resolveDisplayName(context, uri)
            val thumbs = extractThumbnailsInternal(retriever, 0L, durationMs, count = 6)

            VideoClip(
                uriString = uri.toString(),
                displayName = displayName,
                sourceDurationMs = durationMs,
                width = width,
                height = height,
                trimStartMs = 0L,
                trimEndMs = durationMs,
                thumbnails = thumbs,
            )
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    suspend fun loadThumbnailsForClip(
        context: Context,
        uri: Uri,
        startMs: Long,
        endMs: Long,
        count: Int = 6,
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            extractThumbnailsInternal(retriever, startMs, endMs, count)
        } catch (_: Exception) {
            emptyList()
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun extractThumbnailsInternal(
        retriever: MediaMetadataRetriever,
        startMs: Long,
        endMs: Long,
        count: Int,
    ): List<Bitmap> {
        val safeEnd = endMs.coerceAtLeast(startMs + 300L)
        val span = (safeEnd - startMs).coerceAtLeast(1L)
        val results = mutableListOf<Bitmap>()
        for (i in 0 until count) {
            val fraction = if (count <= 1) 0f else i.toFloat() / (count - 1).toFloat()
            val timeUs = (startMs + (span * fraction).toLong()) * 1000L
            val frame = runCatching {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
                    retriever.getScaledFrameAtTime(
                        timeUs,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                        160,
                        160,
                    )
                } else {
                    retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let {
                        Bitmap.createScaledBitmap(it, 128, 128, true)
                    }
                }
            }.getOrNull()
            if (frame != null) {
                results.add(frame)
            }
        }
        return results
    }

    private fun resolveDisplayName(context: Context, uri: Uri): String {
        runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        val name = cursor.getString(idx)
                        if (!name.isNullOrBlank()) return name.substringBeforeLast('.')
                    }
                }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: "Video Clip"
    }
}
