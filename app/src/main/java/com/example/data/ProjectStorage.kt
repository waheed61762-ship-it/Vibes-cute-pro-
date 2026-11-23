package com.example.data

import android.content.Context
import com.example.model.AspectRatioPreset
import com.example.model.ClipTextOverlay
import com.example.model.ClipTransition
import com.example.model.ColorAdjustments
import com.example.model.ProjectDraft
import com.example.model.SpecialEffect
import com.example.model.VideoClip
import com.example.model.VideoFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ProjectStorage(context: Context) {
    private val file = File(context.filesDir, "prismcut_drafts.json")

    suspend fun loadDrafts(): List<ProjectDraft> = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext emptyList()
        runCatching {
            val raw = file.readText()
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    parseDraft(array.getJSONObject(i))?.let { add(it) }
                }
            }.sortedByDescending { it.updatedAtMillis }
        }.getOrDefault(emptyList())
    }

    suspend fun saveDrafts(drafts: List<ProjectDraft>) = withContext(Dispatchers.IO) {
        runCatching {
            val array = JSONArray()
            drafts.sortedByDescending { it.updatedAtMillis }.take(20).forEach { draft ->
                array.put(serializeDraft(draft))
            }
            file.writeText(array.toString())
        }
    }

    private fun serializeDraft(draft: ProjectDraft): JSONObject {
        val clipsArray = JSONArray()
        draft.clips.forEach { clip ->
            clipsArray.put(
                JSONObject().apply {
                    put("id", clip.id)
                    put("uriString", clip.uriString)
                    put("displayName", clip.displayName)
                    put("sourceDurationMs", clip.sourceDurationMs)
                    put("width", clip.width)
                    put("height", clip.height)
                    put("trimStartMs", clip.trimStartMs)
                    put("trimEndMs", clip.trimEndMs)
                    put("speed", clip.speed.toDouble())
                    put("isMuted", clip.isMuted)
                    put("volume", clip.volume.toDouble())
                    put("rotationDegrees", clip.rotationDegrees)
                    put("filter", clip.filter.name)
                    put("filterIntensity", clip.filterIntensity.toDouble())
                    put("specialEffect", clip.specialEffect.name)
                    put("effectIntensity", clip.effectIntensity.toDouble())
                    put("brightness", clip.adjustments.brightness.toDouble())
                    put("contrast", clip.adjustments.contrast.toDouble())
                    put("saturation", clip.adjustments.saturation.toDouble())
                    put("transitionIn", clip.transitionIn.name)
                    clip.textOverlay?.let { overlay ->
                        put(
                            "textOverlay",
                            JSONObject().apply {
                                put("text", overlay.text)
                                put("colorArgb", overlay.colorArgb)
                                put("scale", overlay.scale.toDouble())
                                put("verticalPosition", overlay.verticalPosition.toDouble())
                            },
                        )
                    }
                },
            )
        }
        return JSONObject().apply {
            put("id", draft.id)
            put("title", draft.title)
            put("updatedAtMillis", draft.updatedAtMillis)
            put("aspectRatio", draft.aspectRatio.name)
            put("clips", clipsArray)
        }
    }

    private fun parseDraft(obj: JSONObject): ProjectDraft? {
        val id = obj.optString("id").ifBlank { return null }
        val title = obj.optString("title", "Untitled Project")
        val updatedAt = obj.optLong("updatedAtMillis", System.currentTimeMillis())
        val aspectRatio = enumValueOrDefault(
            obj.optString("aspectRatio"),
            AspectRatioPreset.ORIGINAL,
        )
        val clipsArr = obj.optJSONArray("clips") ?: JSONArray()
        val clips = buildList {
            for (i in 0 until clipsArr.length()) {
                parseClip(clipsArr.getJSONObject(i))?.let { add(it) }
            }
        }
        return ProjectDraft(
            id = id,
            title = title,
            updatedAtMillis = updatedAt,
            aspectRatio = aspectRatio,
            clips = clips,
        )
    }

    private fun parseClip(obj: JSONObject): VideoClip? {
        val uriString = obj.optString("uriString").ifBlank { return null }
        val duration = obj.optLong("sourceDurationMs", 5000L).coerceAtLeast(500L)
        val overlayObj = obj.optJSONObject("textOverlay")
        val overlay = overlayObj?.let {
            ClipTextOverlay(
                text = it.optString("text", ""),
                colorArgb = it.optInt("colorArgb", 0xFFFFFFFF.toInt()),
                scale = it.optDouble("scale", 1.0).toFloat(),
                verticalPosition = it.optDouble("verticalPosition", 0.72).toFloat(),
            )
        }
        return VideoClip(
            id = obj.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
            uriString = uriString,
            displayName = obj.optString("displayName", "Video Clip"),
            sourceDurationMs = duration,
            width = obj.optInt("width", 1920),
            height = obj.optInt("height", 1080),
            trimStartMs = obj.optLong("trimStartMs", 0L).coerceIn(0L, duration - 250L),
            trimEndMs = obj.optLong("trimEndMs", duration).coerceIn(250L, duration),
            speed = obj.optDouble("speed", 1.0).toFloat().coerceIn(0.25f, 4.0f),
            isMuted = obj.optBoolean("isMuted", false),
            volume = obj.optDouble("volume", 1.0).toFloat().coerceIn(0f, 1f),
            rotationDegrees = obj.optInt("rotationDegrees", 0),
            filter = enumValueOrDefault(obj.optString("filter"), VideoFilter.NONE),
            filterIntensity = obj.optDouble("filterIntensity", 1.0).toFloat().coerceIn(0f, 1f),
            specialEffect = enumValueOrDefault(obj.optString("specialEffect"), SpecialEffect.NONE),
            effectIntensity = obj.optDouble("effectIntensity", 0.75).toFloat().coerceIn(0f, 1f),
            adjustments = ColorAdjustments(
                brightness = obj.optDouble("brightness", 0.0).toFloat(),
                contrast = obj.optDouble("contrast", 0.0).toFloat(),
                saturation = obj.optDouble("saturation", 0.0).toFloat(),
            ),
            transitionIn = enumValueOrDefault(obj.optString("transitionIn"), ClipTransition.NONE),
            textOverlay = overlay,
        )
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(name: String, default: T): T {
        return runCatching { enumValueOf<T>(name) }.getOrDefault(default)
    }
}
