package com.cvc953.localplayer.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ArtworkLoader {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = (maxMemory / 16).coerceAtLeast(2048)

    private val thumbnailCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount / 1024
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int,
    ): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    suspend fun loadThumbnail(
        context: Context,
        uri: Uri?,
        filePath: String? = null,
        targetSizePx: Int = 256,
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (uri == null && filePath.isNullOrBlank()) return@withContext null

        val cacheKey = "${uri?.toString() ?: filePath}_$targetSizePx"
        synchronized(thumbnailCache) {
            thumbnailCache.get(cacheKey)?.let { return@withContext it }
        }

        var bitmap: Bitmap? = null

        if (uri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && uri.scheme == "content") {
            try {
                bitmap = context.contentResolver.loadThumbnail(uri, Size(targetSizePx, targetSizePx), null)
            } catch (_: Exception) {
            }
        }

        if (bitmap == null && uri != null) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val picture = retriever.embeddedPicture
                if (picture != null && picture.isNotEmpty()) {
                    val opts = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                        inPreferredConfig = Bitmap.Config.RGB_565
                    }
                    BitmapFactory.decodeByteArray(picture, 0, picture.size, opts)
                    opts.inSampleSize = calculateInSampleSize(opts, targetSizePx, targetSizePx)
                    opts.inJustDecodeBounds = false
                    bitmap = BitmapFactory.decodeByteArray(picture, 0, picture.size, opts)
                }
            } catch (_: Exception) {
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {
                }
            }
        }

        if (bitmap == null && !filePath.isNullOrBlank()) {
            try {
                val dir = File(filePath).parentFile
                if (dir != null && dir.exists()) {
                    val candidates = listOf("cover.jpg", "folder.jpg", "album.jpg", "front.jpg", "cover.png", "folder.png")
                    for (name in candidates) {
                        val imgFile = File(dir, name)
                        if (imgFile.exists() && imgFile.length() > 0) {
                            val opts = BitmapFactory.Options().apply {
                                inJustDecodeBounds = true
                                inPreferredConfig = Bitmap.Config.RGB_565
                            }
                            BitmapFactory.decodeFile(imgFile.absolutePath, opts)
                            opts.inSampleSize = calculateInSampleSize(opts, targetSizePx, targetSizePx)
                            opts.inJustDecodeBounds = false
                            bitmap = BitmapFactory.decodeFile(imgFile.absolutePath, opts)
                            if (bitmap != null) break
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }

        if (bitmap != null) {
            synchronized(thumbnailCache) {
                thumbnailCache.put(cacheKey, bitmap)
            }
        }

        bitmap
    }

    fun clearCache() {
        synchronized(thumbnailCache) {
            thumbnailCache.evictAll()
        }
    }
}
