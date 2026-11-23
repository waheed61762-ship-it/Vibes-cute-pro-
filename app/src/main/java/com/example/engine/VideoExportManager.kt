package com.example.engine

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import com.example.model.ExportConfig
import com.example.model.ExportUiState
import com.example.model.ProjectDraft
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

data class RenderedClipResult(
    val outputUriString: String,
    val displayPath: String,
    val fileSizeMb: Double,
    val resolutionLabel: String,
)

@OptIn(UnstableApi::class)
class VideoExportManager(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val _exportState = MutableStateFlow<ExportUiState>(ExportUiState.Idle)
    val exportState: StateFlow<ExportUiState> = _exportState.asStateFlow()

    private var activeTransformer: Transformer? = null
    private var progressJob: Job? = null
    private var tempOutputFile: File? = null
    private var startTimeMs: Long = 0L

    fun startExport(project: ProjectDraft, config: ExportConfig) {
        if (_exportState.value is ExportUiState.Exporting) return

        val composition = CompositionBuilder.buildForExport(project, config.resolution)
        if (composition == null) {
            _exportState.value = ExportUiState.Error("Add at least one video clip before exporting.")
            return
        }

        val (targetW, targetH) = CompositionBuilder.computeExportDimensions(project, config.resolution)
        val outDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val fileName = "VibeCut_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.mp4"
        val tempFile = File(outDir, fileName)
        if (tempFile.exists()) tempFile.delete()
        tempOutputFile = tempFile
        startTimeMs = System.currentTimeMillis()

        _exportState.value = ExportUiState.Exporting(
            progressPercent = 0,
            elapsedSeconds = 0,
            statusLabel = "Initializing ${config.resolution.label} (${targetW}×${targetH})...",
        )

        val encoderFactory = DefaultEncoderFactory.Builder(context)
            .setRequestedVideoEncoderSettings(
                VideoEncoderSettings.Builder()
                    .setBitrate(config.bitrateMbps * 1_000_000)
                    .build(),
            )
            .setEnableFallback(true)
            .build()

        val transformer = Transformer.Builder(context)
            .setLooper(Looper.getMainLooper())
            .setVideoMimeType(config.codec.mimeType)
            .setEncoderFactory(encoderFactory)
            .addListener(
                object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        progressJob?.cancel()
                        activeTransformer = null
                        val elapsed = ((System.currentTimeMillis() - startTimeMs) / 1000L).toInt()
                        scope.launch {
                            _exportState.value = ExportUiState.Exporting(
                                progressPercent = 99,
                                elapsedSeconds = elapsed,
                                statusLabel = "Saving to Movies/VibeCut...",
                            )
                            val saved = saveToGallery(tempFile, fileName)
                            if (saved != null) {
                                val sizeMb = tempFile.length() / (1024.0 * 1024.0)
                                tempFile.delete()
                                _exportState.value = ExportUiState.Success(
                                    outputUriString = saved.toString(),
                                    displayPath = "Movies/VibeCut/$fileName",
                                    fileSizeMb = sizeMb,
                                    resolutionLabel = "${config.resolution.label} (${targetW}×${targetH})",
                                    elapsedSeconds = elapsed,
                                )
                            } else {
                                _exportState.value = ExportUiState.Error(
                                    "Rendered video could not be saved to device Gallery.",
                                )
                            }
                        }
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException,
                    ) {
                        progressJob?.cancel()
                        activeTransformer = null
                        tempFile.delete()
                        _exportState.value = ExportUiState.Error(
                            exportException.localizedMessage
                                ?: "Hardware encoder could not process ${config.resolution.label}. Try 1080p or 720p.",
                        )
                    }
                },
            )
            .build()

        activeTransformer = transformer
        Handler(Looper.getMainLooper()).post {
            runCatching {
                transformer.start(composition, tempFile.absolutePath)
                startProgressPolling(transformer, config.resolution.label, targetW, targetH)
            }.onFailure { err ->
                activeTransformer = null
                tempFile.delete()
                _exportState.value = ExportUiState.Error(
                    err.localizedMessage ?: "Failed to start video export.",
                )
            }
        }
    }

    suspend fun exportSegmentSync(
        project: ProjectDraft,
        config: ExportConfig,
        filePrefix: String,
    ): Result<RenderedClipResult> {
        val composition = CompositionBuilder.buildForExport(project, config.resolution)
            ?: return Result.failure(IllegalArgumentException("Empty clip segment."))
        val (targetW, targetH) = CompositionBuilder.computeExportDimensions(project, config.resolution)
        val outDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val cleanPrefix = filePrefix.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(24)
        val fileName = "${cleanPrefix}_${System.currentTimeMillis()}.mp4"
        val tempFile = File(outDir, fileName)
        if (tempFile.exists()) tempFile.delete()

        val exportResult: Result<File> = suspendCancellableCoroutine { cont ->
            val encoderFactory = DefaultEncoderFactory.Builder(context)
                .setRequestedVideoEncoderSettings(
                    VideoEncoderSettings.Builder()
                        .setBitrate(config.bitrateMbps * 1_000_000)
                        .build(),
                )
                .setEnableFallback(true)
                .build()

            val transformer = Transformer.Builder(context)
                .setLooper(Looper.getMainLooper())
                .setVideoMimeType(config.codec.mimeType)
                .setEncoderFactory(encoderFactory)
                .addListener(
                    object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, result: ExportResult) {
                            activeTransformer = null
                            if (cont.isActive) cont.resume(Result.success(tempFile))
                        }

                        override fun onError(
                            composition: Composition,
                            result: ExportResult,
                            exportException: ExportException,
                        ) {
                            activeTransformer = null
                            tempFile.delete()
                            if (cont.isActive) cont.resume(Result.failure(exportException))
                        }
                    },
                )
                .build()

            activeTransformer = transformer
            cont.invokeOnCancellation {
                Handler(Looper.getMainLooper()).post {
                    runCatching { transformer.cancel() }
                    activeTransformer = null
                    tempFile.delete()
                }
            }

            Handler(Looper.getMainLooper()).post {
                runCatching {
                    transformer.start(composition, tempFile.absolutePath)
                }.onFailure { err ->
                    activeTransformer = null
                    tempFile.delete()
                    if (cont.isActive) cont.resume(Result.failure(err))
                }
            }
        }

        val renderedFile = exportResult.getOrElse { return Result.failure(it) }
        val savedUri = saveToGallery(renderedFile, fileName)
            ?: return Result.failure(IllegalStateException("Could not save $fileName to Movies/VibeCut."))
        val sizeMb = renderedFile.length() / (1024.0 * 1024.0)
        renderedFile.delete()

        return Result.success(
            RenderedClipResult(
                outputUriString = savedUri.toString(),
                displayPath = "Movies/VibeCut/$fileName",
                fileSizeMb = sizeMb,
                resolutionLabel = "${config.resolution.label} (${targetW}×${targetH})",
            ),
        )
    }

    private fun startProgressPolling(
        transformer: Transformer,
        resLabel: String,
        targetW: Int,
        targetH: Int,
    ) {
        progressJob?.cancel()
        progressJob = scope.launch(Dispatchers.Main) {
            val holder = ProgressHolder()
            while (isActive && activeTransformer === transformer) {
                val state = transformer.getProgress(holder)
                val elapsed = ((System.currentTimeMillis() - startTimeMs) / 1000L).toInt()
                if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                    val pct = holder.progress.coerceIn(0, 99)
                    _exportState.value = ExportUiState.Exporting(
                        progressPercent = pct,
                        elapsedSeconds = elapsed,
                        statusLabel = "Rendering $resLabel (${targetW}×${targetH}) • $pct%",
                    )
                } else if (state == Transformer.PROGRESS_STATE_NOT_STARTED) {
                    break
                }
                delay(200L)
            }
        }
    }

    fun cancelExport() {
        progressJob?.cancel()
        Handler(Looper.getMainLooper()).post {
            runCatching { activeTransformer?.cancel() }
            activeTransformer = null
        }
        tempOutputFile?.delete()
        tempOutputFile = null
        _exportState.value = ExportUiState.Idle
    }

    fun resetState() {
        if (_exportState.value !is ExportUiState.Exporting) {
            _exportState.value = ExportUiState.Idle
        }
    }

    private suspend fun saveToGallery(sourceFile: File, fileName: String): Uri? = withContext(Dispatchers.IO) {
        if (!sourceFile.exists()) return@withContext null
        val resolver = context.contentResolver

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/VibeCut")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@withContext null
            return@withContext runCatching {
                resolver.openOutputStream(uri)?.use { out ->
                    sourceFile.inputStream().use { input -> input.copyTo(out) }
                } ?: error("Cannot open output stream")
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                uri
            }.onFailure {
                runCatching { resolver.delete(uri, null, null) }
            }.getOrNull()
        } else {
            @Suppress("DEPRECATION")
            val moviesDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                "VibeCut",
            ).apply { mkdirs() }
            val dest = File(moviesDir, fileName)
            return@withContext runCatching {
                sourceFile.copyTo(dest, overwrite = true)
                MediaScannerConnection.scanFile(context, arrayOf(dest.absolutePath), arrayOf("video/mp4"), null)
                Uri.fromFile(dest)
            }.getOrNull()
        }
    }
}
