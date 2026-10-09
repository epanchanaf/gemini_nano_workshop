package com.sdkecuador.gemininano

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sdkecuador.gemininano.data.genai.client.MlKitGenAiClientFactory
import com.sdkecuador.gemininano.data.genai.repository.ChatRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatMultimodalDeviceTest {
    @Test
    fun answersQuestionAboutAnImage() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = instrumentation.startActivitySync(
            Intent(instrumentation.targetContext, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        instrumentation.waitForIdleSync()
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(80, 170, 235))
        val paint = Paint().apply { color = Color.rgb(56, 127, 67) }
        canvas.drawRect(0f, 300f, 512f, 512f, paint)
        paint.color = Color.rgb(252, 220, 85)
        canvas.drawCircle(390f, 100f, 55f, paint)

        val repository = ChatRepository(MlKitGenAiClientFactory(instrumentation.targetContext))
        try {
            repository.prepare {}
            val response = repository.streamResponse("Describe esta imagen.", image = bitmap).toList()
            assertTrue(response.joinToString("").isNotBlank())
        } finally {
            repository.close()
            activity.finish()
            bitmap.recycle()
        }
    }
}
