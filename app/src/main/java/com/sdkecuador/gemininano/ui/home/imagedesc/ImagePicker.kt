package com.sdkecuador.gemininano.ui.home.imagedesc

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sdkecuador.gemininano.data.genai.model.AiInputLimits
import kotlin.math.roundToInt
import com.sdkecuador.gemininano.data.genai.model.imageScaleForInputLimit

@Composable
fun ImagePickerScreen(
    modifier: Modifier = Modifier,
    onImagePicked: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val source = ImageDecoder.createSource(context.contentResolver, it)
            val bmp = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val originalWidth = info.size.width
                val originalHeight = info.size.height
                val scale = imageScaleForInputLimit(originalWidth, originalHeight)
                decoder.setTargetSize(
                    (originalWidth * scale).roundToInt().coerceAtLeast(1),
                    (originalHeight * scale).roundToInt().coerceAtLeast(1)
                )
            }
            bitmap = bmp
            onImagePicked(bmp)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(onClick = { imagePickerLauncher.launch("image/*") }) {
            Text("Elegir una imagen")
        }
        Text(
            "Máximo: 2 048 px por lado y 4 MP. Las imágenes grandes se ajustan al seleccionar.",
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
        )

        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentScale = ContentScale.Crop
            )
        }
    }
}
