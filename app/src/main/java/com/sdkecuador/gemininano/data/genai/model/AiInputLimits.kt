package com.sdkecuador.gemininano.data.genai.model

import kotlin.math.min
import kotlin.math.sqrt

/** Límites de entrada visibles en la demo y aplicados también por la capa de datos. */
object AiInputLimits {
    const val MIN_SUMMARY_CHARACTERS = 400
    const val MAX_SUMMARY_TOKENS = 4_000
    /** Maximum accepted by the ML Kit Proofreading and Rewriting APIs. */
    const val MAX_PROOFREADING_REWRITING_TOKENS = 255
    /** Demo limit for short Prompt API text before the exact model limit is checked. */
    const val MAX_SHORT_TEXT_TOKENS = 1_024
    const val MAX_IMAGE_EDGE_PX = 2_048
    const val MAX_IMAGE_PIXELS = 4_000_000
    const val MAX_CHAT_OUTPUT_TOKENS = 256
}

/** Aproximación deliberada para la UI; la API Prompt comprueba el conteo real antes de generar. */
fun String.estimatedTokenCount(): Int = (trim().length + 3) / 4

fun isSupportedImageSize(width: Int, height: Int): Boolean =
    width > 0 &&
        height > 0 &&
        width <= AiInputLimits.MAX_IMAGE_EDGE_PX &&
        height <= AiInputLimits.MAX_IMAGE_EDGE_PX &&
        width.toLong() * height <= AiInputLimits.MAX_IMAGE_PIXELS

fun imageScaleForInputLimit(width: Int, height: Int): Float {
    require(width > 0 && height > 0)
    return min(
        1f,
        min(
            AiInputLimits.MAX_IMAGE_EDGE_PX.toFloat() / maxOf(width, height),
            sqrt(AiInputLimits.MAX_IMAGE_PIXELS.toFloat() / (width.toLong() * height).toFloat())
        )
    )
}
