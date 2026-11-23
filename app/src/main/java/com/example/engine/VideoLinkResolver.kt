package com.example.engine

import com.example.model.ResolvedLinkMedia
import com.example.model.SourcePlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit

object VideoLinkResolver {

    private val client = OkHttpClient.Builder()
        .connectTimeout(7, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun detectPlatform(rawUrl: String): SourcePlatform {
        val lower = rawUrl.trim().lowercase(Locale.US)
        return when {
            "youtube.com" in lower || "youtu.be" in lower -> SourcePlatform.YOUTUBE
            "tiktok.com" in lower -> SourcePlatform.TIKTOK
            "facebook.com" in lower || "fb.watch" in lower -> SourcePlatform.FACEBOOK
            "instagram.com" in lower -> SourcePlatform.INSTAGRAM
            else -> SourcePlatform.DIRECT_MP4
        }
    }

    suspend fun resolve(rawUrl: String): Result<ResolvedLinkMedia> = withContext(Dispatchers.IO) {
        val cleaned = rawUrl.trim()
        if (!cleaned.startsWith("http://") && !cleaned.startsWith("https://")) {
            return@withContext Result.failure(
                IllegalArgumentException("Enter a valid https:// link from YouTube, TikTok, Facebook, Instagram, or a direct MP4 URL."),
            )
        }

        val platform = detectPlatform(cleaned)
        if (platform == SourcePlatform.DIRECT_MP4 || cleaned.substringBefore('?').lowercase(Locale.US).endsWith(".mp4")) {
            return@withContext resolveDirectVideo(cleaned, platform)
        }

        val oembedTitle = fetchOEmbedTitle(platform, cleaned)
        val pageMeta = fetchOpenGraphMetadata(cleaned)
        val ogVideo = pageMeta.videoUrl
        val resolvedTitle = oembedTitle
            ?: pageMeta.title
            ?: "${platform.label} Video"

        if (!ogVideo.isNullOrBlank() && (ogVideo.startsWith("http://") || ogVideo.startsWith("https://"))) {
            return@withContext Result.success(
                ResolvedLinkMedia(
                    platform = platform,
                    originalUrl = cleaned,
                    title = resolvedTitle,
                    playableStreamUrl = ogVideo,
                    directStreamPlayable = true,
                    thumbnailUrl = pageMeta.imageUrl,
                    durationHintMs = 180_000L,
                    statusNote = "Direct playable video stream resolved from ${platform.label}. Ready to clip and export.",
                ),
            )
        }

        Result.success(
            ResolvedLinkMedia(
                platform = platform,
                originalUrl = cleaned,
                title = resolvedTitle,
                playableStreamUrl = null,
                directStreamPlayable = false,
                thumbnailUrl = pageMeta.imageUrl,
                durationHintMs = 180_000L,
                statusNote = "${platform.label} metadata loaded. For protected web links, pick the downloaded MP4 from Gallery or paste a direct .mp4 URL to render clips on-device.",
            ),
        )
    }

    private fun resolveDirectVideo(url: String, platform: SourcePlatform): Result<ResolvedLinkMedia> {
        return runCatching {
            val headReq = Request.Builder()
                .url(url)
                .head()
                .header("User-Agent", "VibeCut/2.1 Android")
                .build()
            client.newCall(headReq).execute().use { resp ->
                val contentType = resp.header("Content-Type").orEmpty().lowercase(Locale.US)
                val looksLikeVideo = "video" in contentType ||
                    url.substringBefore('?').lowercase(Locale.US).endsWith(".mp4") ||
                    url.substringBefore('?').lowercase(Locale.US).endsWith(".mov") ||
                    url.substringBefore('?').lowercase(Locale.US).endsWith(".webm")
                if (!resp.isSuccessful && resp.code != 405) {
                    error("Remote server returned HTTP ${resp.code}")
                }
                if (!looksLikeVideo && contentType.isNotEmpty() && "octet-stream" !in contentType) {
                    error("URL returned '$contentType' instead of a direct video file.")
                }
                val fileName = url.substringBefore('?').substringAfterLast('/').ifBlank { "Direct MP4 Video" }
                ResolvedLinkMedia(
                    platform = platform,
                    originalUrl = url,
                    title = fileName,
                    playableStreamUrl = url,
                    directStreamPlayable = true,
                    durationHintMs = 120_000L,
                    statusNote = "Direct MP4 stream verified and ready for timeline preview & batch clipping.",
                )
            }
        }
    }

    private fun fetchOEmbedTitle(platform: SourcePlatform, videoUrl: String): String? {
        val encoded = runCatching { URLEncoder.encode(videoUrl, "UTF-8") }.getOrNull() ?: return null
        val endpoint = when (platform) {
            SourcePlatform.YOUTUBE -> "https://www.youtube.com/oembed?url=$encoded&format=json"
            SourcePlatform.TIKTOK -> "https://www.tiktok.com/oembed?url=$encoded"
            else -> return null
        }
        return runCatching {
            val req = Request.Builder().url(endpoint).get().build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val body = resp.body?.string() ?: return@use null
                JSONObject(body).optString("title").takeIf { it.isNotBlank() }
            }
        }.getOrNull()
    }

    private data class OgMeta(
        val title: String? = null,
        val videoUrl: String? = null,
        val imageUrl: String? = null,
    )

    private fun fetchOpenGraphMetadata(url: String): OgMeta {
        return runCatching {
            val req = Request.Builder()
                .url(url)
                .get()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0 Mobile Safari/537.36")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use OgMeta()
                val html = resp.body?.string()?.take(80_000).orEmpty()
                OgMeta(
                    title = extractMetaContent(html, "og:title"),
                    videoUrl = extractMetaContent(html, "og:video:secure_url")
                        ?: extractMetaContent(html, "og:video:url")
                        ?: extractMetaContent(html, "og:video"),
                    imageUrl = extractMetaContent(html, "og:image"),
                )
            }
        }.getOrDefault(OgMeta())
    }

    private fun extractMetaContent(html: String, propertyName: String): String? {
        val regex = Regex(
            """<meta[^>]+(?:property|name)=["']${Regex.escape(propertyName)}["'][^>]+content=["']([^"']+)["']""",
            RegexOption.IGNORE_CASE,
        )
        return regex.find(html)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
    }
}
