package com.sdkecuador.gemininano

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sdkecuador.gemininano.data.genai.client.MlKitGenAiClientFactory
import com.sdkecuador.gemininano.data.genai.repository.ImageDescriptionRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

@RunWith(AndroidJUnit4::class)
class ImageDescriptionDeviceTest {
    @Test
    fun describesGeneratedLandscapeOnDevice() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = instrumentation.startActivitySync(
            Intent(instrumentation.targetContext, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { isAntiAlias = true }
        canvas.drawColor(Color.rgb(80, 170, 235))
        paint.color = Color.rgb(252, 220, 85)
        canvas.drawCircle(395f, 105f, 55f, paint)
        paint.color = Color.rgb(56, 127, 67)
        canvas.drawRect(0f, 300f, 512f, 512f, paint)
        paint.color = Color.rgb(65, 90, 110)
        val mountains = android.graphics.Path().apply {
            moveTo(0f, 330f)
            lineTo(160f, 120f)
            lineTo(300f, 330f)
            lineTo(512f, 180f)
            lineTo(512f, 330f)
            close()
        }
        canvas.drawPath(mountains, paint)

        val encoded = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val decoded = ImageDecoder.decodeBitmap(
            ImageDecoder.createSource(ByteBuffer.wrap(encoded.toByteArray()))
        ) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.setTargetSize(info.size.width, info.size.height)
        }
        val repository = ImageDescriptionRepository(MlKitGenAiClientFactory(instrumentation.targetContext))
        try {
            repository.prepare {}
            assertTrue(repository.describe(decoded).isNotBlank())
        } finally {
            repository.close()
            activity.finish()
            decoded.recycle()
            bitmap.recycle()
        }
    }
}
