package com.excelsior.pocketvault.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import androidx.core.graphics.createBitmap
import com.excelsior.pocketvault.domain.model.StoredImage
import com.excelsior.pocketvault.domain.repository.ImageStorageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class ImageStorageManager @Inject constructor(
    @ApplicationContext private val context: Context,
) : ImageStorageRepository {

    override suspend fun importImage(uri: Uri): StoredImage {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        val bitmap = ImageDecoder.decodeBitmap(source)
        val originalFile = createTargetFile("original")
        val thumbnailFile = createTargetFile("thumb")
        saveBitmap(bitmap, originalFile, 92)
        val thumbnail = createThumbnail(bitmap)
        saveBitmap(thumbnail, thumbnailFile, 86)
        return StoredImage(
            originalPath = originalFile.absolutePath,
            thumbnailPath = thumbnailFile.absolutePath,
            aspectRatio = bitmap.width.toFloat() / max(bitmap.height, 1),
        )
    }

    override suspend fun removeImage(originalPath: String?, thumbnailPath: String?) {
        originalPath?.let(::File)?.takeIf(File::exists)?.delete()
        thumbnailPath?.let(::File)?.takeIf(File::exists)?.delete()
    }

    override suspend fun createDemoArtwork(title: String): StoredImage {
        val width = 1440
        val height = 1920
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                width.toFloat(),
                height.toFloat(),
                intArrayOf(Color.parseColor("#203047"), Color.parseColor("#728FCB"), Color.parseColor("#E6A95F")),
                floatArrayOf(0f, 0.52f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), 96f, 96f, backgroundPaint)
        repeat(6) { index ->
            val alpha = 30 + index * 12
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(alpha, 255, 255, 255)
            }
            canvas.drawCircle(
                width * (0.18f + index * 0.12f),
                height * (0.16f + index * 0.11f),
                width * (0.12f + index * 0.01f),
                paint,
            )
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 124f
            isFakeBoldText = true
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#ECF3FF")
            textSize = 54f
        }
        canvas.drawText(title, 110f, 980f, titlePaint)
        canvas.drawText("拾一束光，安放在离线收藏馆里。", 110f, 1080f, subtitlePaint)

        val originalFile = createTargetFile("demo")
        val thumbnailFile = createTargetFile("demo_thumb")
        saveBitmap(bitmap, originalFile, 92)
        saveBitmap(createThumbnail(bitmap), thumbnailFile, 86)
        return StoredImage(
            originalPath = originalFile.absolutePath,
            thumbnailPath = thumbnailFile.absolutePath,
            aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat(),
        )
    }

    private fun createTargetFile(prefix: String): File {
        val directory = File(context.filesDir, "vault_images").apply { mkdirs() }
        return File(directory, "$prefix-${UUID.randomUUID()}.jpg")
    }

    private fun createThumbnail(bitmap: Bitmap): Bitmap {
        val targetWidth = 640
        val targetHeight = (targetWidth / (bitmap.width.toFloat() / max(bitmap.height, 1))).toInt().coerceAtLeast(320)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    private fun saveBitmap(bitmap: Bitmap, file: File, quality: Int) {
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
        }
    }
}
