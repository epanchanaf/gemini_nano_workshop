package com.sdkecuador.gemininano.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import com.sdkecuador.gemininano.data.genai.model.imageScaleForInputLimit
import kotlin.math.roundToInt

fun decodeAiImage(context: Context, uri: Uri): Bitmap {
    val source = ImageDecoder.createSource(context.contentResolver, uri)
    return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        val originalWidth = info.size.width
        val originalHeight = info.size.height
        val scale = imageScaleForInputLimit(originalWidth, originalHeight)
        decoder.setTargetSize(
            (originalWidth * scale).roundToInt().coerceAtLeast(1),
            (originalHeight * scale).roundToInt().coerceAtLeast(1)
        )
    }
}
