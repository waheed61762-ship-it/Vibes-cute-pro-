package com.example.engine

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.CompositionPlayer
import com.example.data.ProjectStorage
import com.example.model.DeviceEncoderCapabilities
import com.example.model.ExportConfig
import com.example.model.ExportUiState
import com.example.model.ProjectDraft
import com.example.model.VideoClip
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

@OptIn(UnstableApi::class)
class VibeCutMediaServices(
    context: Context,
    scope: CoroutineScope,
) {
    val appContext: Context = context.applicationContext
    val projectStorage = ProjectStorage(appContext)
    val exportManager = VideoExportManager(appContext, scope)
    val capabilities: DeviceEncoderCapabilities = CodecCapabilityChecker.inspect()

    val exportState: StateFlow<ExportUiState> = exportManager.exportState

    fun createPlayer(): CompositionPlayer =
        CompositionPlayer.Builder(appContext).build()

    suspend fun inspectUris(uris: List<Uri>): List<VideoClip> {
        val list = mutableListOf<VideoClip>()
        for (uri in uris) {
            runCatching {
                appContext.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            MediaInspector.inspectVideoClip(appContext, uri)?.let { list.add(it) }
        }
        return list
    }

    suspend fun hydrateClipThumbnails(clip: VideoClip): VideoClip {
        if (clip.thumbnails.isNotEmpty()) return clip
        val thumbs = MediaInspector.loadThumbnailsForClip(
            appContext,
            clip.uri,
            clip.trimStartMs,
            clip.trimEndMs,
        )
        return clip.copy(thumbnails = thumbs)
    }

    fun startExport(project: ProjectDraft, config: ExportConfig) {
        exportManager.startExport(project, config)
    }

    suspend fun exportSegmentSync(
        project: ProjectDraft,
        config: ExportConfig,
        filePrefix: String,
    ): Result<RenderedClipResult> =
        exportManager.exportSegmentSync(project, config, filePrefix)

    fun cancelExport() {
        exportManager.cancelExport()
    }

    fun resetExportState() {
        exportManager.resetState()
    }
}
