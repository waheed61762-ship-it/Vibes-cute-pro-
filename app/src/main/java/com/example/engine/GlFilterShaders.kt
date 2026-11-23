package com.example.engine

import android.content.Context
import android.graphics.Matrix
import android.opengl.GLES20
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.util.GlProgram
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.GlEffect
import androidx.media3.effect.GlShaderProgram
import androidx.media3.effect.MatrixTransformation
import androidx.media3.effect.BaseGlShaderProgram
import com.example.model.ClipTransition
import com.example.model.ColorAdjustments
import com.example.model.SpecialEffect
import com.example.model.VideoFilter
import kotlin.math.sin

private val VERTEX_SHADER_GLSL = """
#version 100
attribute vec4 aFramePosition;
varying vec2 vTexSamplingCoord;
void main() {
    gl_Position = aFramePosition;
    vTexSamplingCoord = vec2(aFramePosition.x * 0.5 + 0.5, aFramePosition.y * 0.5 + 0.5);
}
""".trimIndent()

private val COLOR_GRADING_FRAGMENT_SHADER = """
#version 100
precision mediump float;
uniform sampler2D uTexSampler;
uniform int uFilterType;
uniform float uFilterIntensity;
uniform float uBrightness;
uniform float uContrast;
uniform float uSaturation;
uniform float uFadeAmount;
uniform float uFlashAmount;
varying vec2 vTexSamplingCoord;

vec3 applyFilter(vec3 c, int fType) {
    if (fType == 1) {
        // B&W Noir
        float lum = dot(c, vec3(0.299, 0.587, 0.114));
        lum = clamp((lum - 0.5) * 1.25 + 0.5, 0.0, 1.0);
        return vec3(lum);
    } else if (fType == 2) {
        // Sepia
        return clamp(vec3(
            dot(c, vec3(0.393, 0.769, 0.189)),
            dot(c, vec3(0.349, 0.686, 0.168)),
            dot(c, vec3(0.272, 0.534, 0.131))
        ), 0.0, 1.0);
    } else if (fType == 3) {
        // Vintage Film (warm fade + lifted shadows)
        vec3 v = vec3(c.r * 1.04 + 0.04, c.g * 0.98 + 0.03, c.b * 0.82 + 0.06);
        float lum = dot(v, vec3(0.299, 0.587, 0.114));
        return clamp(mix(vec3(lum), v, 0.82), 0.0, 1.0);
    } else if (fType == 4) {
        // Teal & Gold Cinematic
        float lum = dot(c, vec3(0.299, 0.587, 0.114));
        vec3 shadows = vec3(0.05, 0.25, 0.32);
        vec3 highlights = vec3(1.05, 0.88, 0.68);
        vec3 dual = mix(shadows, highlights, smoothstep(0.15, 0.85, lum));
        return clamp(mix(c, c * dual * 1.15, 0.62), 0.0, 1.0);
    } else if (fType == 5) {
        // Vivid Pop
        float lum = dot(c, vec3(0.299, 0.587, 0.114));
        vec3 sat = mix(vec3(lum), c, 1.38);
        return clamp((sat - 0.5) * 1.15 + 0.5, 0.0, 1.0);
    } else if (fType == 6) {
        // Golden Hour Warm
        return clamp(vec3(c.r * 1.14 + 0.03, c.g * 1.03, c.b * 0.82), 0.0, 1.0);
    } else if (fType == 7) {
        // Arctic Cool
        return clamp(vec3(c.r * 0.86, c.g * 0.99, c.b * 1.16 + 0.03), 0.0, 1.0);
    } else if (fType == 8) {
        // Matte Fade
        vec3 faded = c * 0.86 + vec3(0.07, 0.065, 0.08);
        float lum = dot(faded, vec3(0.299, 0.587, 0.114));
        return clamp(mix(vec3(lum), faded, 0.85), 0.0, 1.0);
    } else if (fType == 9) {
        // Neon Cyber
        vec3 cyber = vec3(c.r * 1.15 + c.b * 0.14, c.g * 0.85 + 0.04, c.b * 1.24 + 0.06);
        return clamp((cyber - 0.5) * 1.18 + 0.5, 0.0, 1.0);
    }
    return c;
}

void main() {
    vec4 src = texture2D(uTexSampler, vTexSamplingCoord);
    vec3 color = src.rgb;

    if (uFilterType != 0) {
        vec3 filtered = applyFilter(color, uFilterType);
        color = mix(color, filtered, uFilterIntensity);
    }

    // Brightness, Contrast, Saturation
    color = color + vec3(uBrightness);
    color = (color - 0.5) * (1.0 + uContrast) + 0.5;
    float lum = dot(color, vec3(0.299, 0.587, 0.114));
    color = mix(vec3(lum), color, 1.0 + uSaturation);
    color = clamp(color, 0.0, 1.0);

    // Transition fades
    color = mix(color, vec3(0.0), clamp(uFadeAmount, 0.0, 1.0));
    color = mix(color, vec3(1.0), clamp(uFlashAmount, 0.0, 1.0));

    gl_FragColor = vec4(color, src.a);
}
""".trimIndent()

private val SPECIAL_EFFECT_FRAGMENT_SHADER = """
#version 100
precision mediump float;
uniform sampler2D uTexSampler;
uniform int uEffectType;
uniform float uIntensity;
uniform float uTimeSec;
uniform vec2 uTexelSize;
varying vec2 vTexSamplingCoord;

float hash11(float p) {
    p = fract(p * 0.1031);
    p *= p + 33.33;
    p *= p + p;
    return fract(p);
}

void main() {
    vec2 uv = vTexSamplingCoord;
    if (uEffectType == 1) {
        // Soft 9-tap Gaussian Blur
        float radius = 1.5 + uIntensity * 6.5;
        vec2 texStep = uTexelSize * radius;
        vec4 sum = texture2D(uTexSampler, uv) * 0.20;
        sum += texture2D(uTexSampler, clamp(uv + vec2(-texStep.x, 0.0), 0.001, 0.999)) * 0.12;
        sum += texture2D(uTexSampler, clamp(uv + vec2(texStep.x, 0.0), 0.001, 0.999)) * 0.12;
        sum += texture2D(uTexSampler, clamp(uv + vec2(0.0, -texStep.y), 0.001, 0.999)) * 0.12;
        sum += texture2D(uTexSampler, clamp(uv + vec2(0.0, texStep.y), 0.001, 0.999)) * 0.12;
        sum += texture2D(uTexSampler, clamp(uv + vec2(-texStep.x, -texStep.y), 0.001, 0.999)) * 0.08;
        sum += texture2D(uTexSampler, clamp(uv + vec2(texStep.x, -texStep.y), 0.001, 0.999)) * 0.08;
        sum += texture2D(uTexSampler, clamp(uv + vec2(-texStep.x, texStep.y), 0.001, 0.999)) * 0.08;
        sum += texture2D(uTexSampler, clamp(uv + vec2(texStep.x, texStep.y), 0.001, 0.999)) * 0.08;
        gl_FragColor = sum;
    } else if (uEffectType == 2) {
        // RGB Glitch (chromatic aberration + animated scanline jitter)
        float tick = floor(uTimeSec * 14.0);
        float band = floor(uv.y * 22.0);
        float noise = hash11(band + tick * 7.13);
        float burst = step(0.68, hash11(tick * 3.7));
        float tear = (noise - 0.5) * 0.06 * uIntensity * (0.35 + 0.65 * burst);
        float shift = (0.005 + 0.022 * uIntensity) * (0.6 + 0.4 * sin(uTimeSec * 18.0));
        vec2 gUv = vec2(clamp(uv.x + tear, 0.001, 0.999), uv.y);
        float r = texture2D(uTexSampler, vec2(clamp(gUv.x + shift, 0.001, 0.999), gUv.y)).r;
        float g = texture2D(uTexSampler, gUv).g;
        float b = texture2D(uTexSampler, vec2(clamp(gUv.x - shift, 0.001, 0.999), gUv.y)).b;
        float scan = 1.0 - 0.12 * uIntensity * step(0.5, fract(uv.y * 120.0));
        gl_FragColor = vec4(vec3(r, g, b) * scan, 1.0);
    } else if (uEffectType == 3) {
        // Vignette
        vec4 src = texture2D(uTexSampler, uv);
        float dist = distance(uv, vec2(0.5, 0.5));
        float vig = smoothstep(0.88 - uIntensity * 0.32, 0.25, dist);
        gl_FragColor = vec4(src.rgb * mix(1.0, vig, 0.45 + 0.55 * uIntensity), src.a);
    } else if (uEffectType == 4) {
        // Retro Pixelate
        float cells = mix(140.0, 28.0, uIntensity);
        vec2 pUv = (floor(uv * cells) + 0.5) / cells;
        gl_FragColor = texture2D(uTexSampler, clamp(pUv, 0.001, 0.999));
    } else {
        gl_FragColor = texture2D(uTexSampler, uv);
    }
}
""".trimIndent()

@OptIn(UnstableApi::class)
class ColorGradingGlEffect(
    private val filter: VideoFilter,
    private val filterIntensity: Float,
    private val adjustments: ColorAdjustments,
    private val transitionIn: ClipTransition,
    private val transitionDurationMs: Long,
) : GlEffect {

    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram {
        return ColorGradingShaderProgram(
            useHdr = useHdr,
            filterIndex = filter.ordinal,
            filterIntensity = filterIntensity,
            adjustments = adjustments,
            transitionIn = transitionIn,
            transitionDurationMs = transitionDurationMs,
        )
    }

    override fun isNoOp(inputWidth: Int, inputHeight: Int): Boolean {
        return filter == VideoFilter.NONE &&
            adjustments.brightness == 0f &&
            adjustments.contrast == 0f &&
            adjustments.saturation == 0f &&
            transitionIn != ClipTransition.FADE_BLACK &&
            transitionIn != ClipTransition.FLASH_WHITE
    }
}

@OptIn(UnstableApi::class)
private class ColorGradingShaderProgram(
    useHdr: Boolean,
    private val filterIndex: Int,
    private val filterIntensity: Float,
    private val adjustments: ColorAdjustments,
    private val transitionIn: ClipTransition,
    private val transitionDurationMs: Long,
) : BaseGlShaderProgram(useHdr, /* texturePoolCapacity= */ 1) {

    private val glProgram = GlProgram(VERTEX_SHADER_GLSL, COLOR_GRADING_FRAGMENT_SHADER).apply {
        setBufferAttribute(
            "aFramePosition",
            GlUtil.getNormalizedCoordinateBounds(),
            GlUtil.HOMOGENEOUS_COORDINATE_VECTOR_SIZE,
        )
    }

    override fun configure(inputWidth: Int, inputHeight: Int): Size {
        return Size(inputWidth, inputHeight)
    }

    override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
        val tMs = presentationTimeUs / 1000f
        val transDur = transitionDurationMs.coerceAtLeast(1L).toFloat()
        val progress = (tMs / transDur).coerceIn(0f, 1f)

        val fadeAmount = if (transitionIn == ClipTransition.FADE_BLACK) 1f - progress else 0f
        val flashAmount = if (transitionIn == ClipTransition.FLASH_WHITE) 1f - progress else 0f

        glProgram.use()
        glProgram.setSamplerTexIdUniform("uTexSampler", inputTexId, 0)
        glProgram.setIntUniform("uFilterType", filterIndex)
        glProgram.setFloatUniform("uFilterIntensity", filterIntensity)
        glProgram.setFloatUniform("uBrightness", adjustments.brightness)
        glProgram.setFloatUniform("uContrast", adjustments.contrast)
        glProgram.setFloatUniform("uSaturation", adjustments.saturation)
        glProgram.setFloatUniform("uFadeAmount", fadeAmount)
        glProgram.setFloatUniform("uFlashAmount", flashAmount)
        glProgram.bindAttributesAndUniforms()

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GlUtil.checkGlError()
    }

    override fun release() {
        super.release()
        glProgram.delete()
    }
}

@OptIn(UnstableApi::class)
class SpecialEffectGlEffect(
    private val effect: SpecialEffect,
    private val intensity: Float,
) : GlEffect {

    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram {
        return SpecialEffectShaderProgram(
            useHdr = useHdr,
            effectCode = when (effect) {
                SpecialEffect.BLUR -> 1
                SpecialEffect.GLITCH -> 2
                SpecialEffect.VIGNETTE -> 3
                SpecialEffect.PIXELATE -> 4
                SpecialEffect.NONE, SpecialEffect.BEAT_ZOOM -> 0
            },
            intensity = intensity,
        )
    }

    override fun isNoOp(inputWidth: Int, inputHeight: Int): Boolean {
        return effect == SpecialEffect.NONE || effect == SpecialEffect.BEAT_ZOOM || intensity <= 0.01f
    }
}

@OptIn(UnstableApi::class)
private class SpecialEffectShaderProgram(
    useHdr: Boolean,
    private val effectCode: Int,
    private val intensity: Float,
) : BaseGlShaderProgram(useHdr, /* texturePoolCapacity= */ 1) {

    private var texelW = 1f / 1080f
    private var texelH = 1f / 1920f

    private val glProgram = GlProgram(VERTEX_SHADER_GLSL, SPECIAL_EFFECT_FRAGMENT_SHADER).apply {
        setBufferAttribute(
            "aFramePosition",
            GlUtil.getNormalizedCoordinateBounds(),
            GlUtil.HOMOGENEOUS_COORDINATE_VECTOR_SIZE,
        )
    }

    override fun configure(inputWidth: Int, inputHeight: Int): Size {
        texelW = 1f / inputWidth.coerceAtLeast(1)
        texelH = 1f / inputHeight.coerceAtLeast(1)
        return Size(inputWidth, inputHeight)
    }

    override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
        glProgram.use()
        glProgram.setSamplerTexIdUniform("uTexSampler", inputTexId, 0)
        glProgram.setIntUniform("uEffectType", effectCode)
        glProgram.setFloatUniform("uIntensity", intensity)
        glProgram.setFloatUniform("uTimeSec", presentationTimeUs / 1_000_000f)
        glProgram.setFloatsUniform("uTexelSize", floatArrayOf(texelW, texelH))
        glProgram.bindAttributesAndUniforms()

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GlUtil.checkGlError()
    }

    override fun release() {
        super.release()
        glProgram.delete()
    }
}

@OptIn(UnstableApi::class)
fun buildMotionMatrixEffect(
    transitionIn: ClipTransition,
    specialEffect: SpecialEffect,
    effectIntensity: Float,
): Effect? {
    val hasMotionTransition = transitionIn == ClipTransition.ZOOM_IN || transitionIn == ClipTransition.SLIDE_LEFT
    val hasBeatZoom = specialEffect == SpecialEffect.BEAT_ZOOM && effectIntensity > 0.01f
    if (!hasMotionTransition && !hasBeatZoom) return null

    return MatrixTransformation { presentationTimeUs ->
        val matrix = Matrix()
        val timeMs = presentationTimeUs / 1000f
        var scale = 1f
        var translateX = 0f

        if (transitionIn == ClipTransition.ZOOM_IN && transitionIn.durationMs > 0L) {
            val p = (timeMs / transitionIn.durationMs.toFloat()).coerceIn(0f, 1f)
            val eased = 1f - (1f - p) * (1f - p)
            scale *= 1.35f - 0.35f * eased
        } else if (transitionIn == ClipTransition.SLIDE_LEFT && transitionIn.durationMs > 0L) {
            val p = (timeMs / transitionIn.durationMs.toFloat()).coerceIn(0f, 1f)
            val eased = 1f - (1f - p) * (1f - p)
            translateX = (1f - eased) * 2.0f
        }

        if (hasBeatZoom) {
            val sec = presentationTimeUs / 1_000_000f
            val pulse = 0.5f + 0.5f * sin(sec * Math.PI.toFloat() * 4f)
            scale *= 1f + pulse * 0.14f * effectIntensity
        }

        if (scale != 1f) {
            matrix.postScale(scale, scale)
        }
        if (translateX != 0f) {
            matrix.postTranslate(translateX, 0f)
        }
        matrix
    }
}
