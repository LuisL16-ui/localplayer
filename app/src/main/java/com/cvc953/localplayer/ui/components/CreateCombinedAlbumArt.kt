package com.cvc953.localplayer.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas

fun createCombinedAlbumArt(
    bitmaps: List<Bitmap?>,
    size: Int = 384,
): Bitmap {
    val canvas = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvasDrawer = Canvas(canvas)
    val count = minOf(4, bitmaps.size)
    if (count == 0) {
        return canvas
    }

    val halfSize = size / 2

    fun drawAndRecycle(bmp: Bitmap, w: Int, h: Int, x: Float, y: Float) {
        val scaled = Bitmap.createScaledBitmap(bmp, w, h, true)
        canvasDrawer.drawBitmap(scaled, x, y, null)
        if (scaled != bmp) {
            try {
                scaled.recycle()
            } catch (_: Exception) {
            }
        }
    }

    when (count) {
        1 -> {
            val bitmap = bitmaps[0] ?: return canvas
            drawAndRecycle(bitmap, size, size, 0f, 0f)
        }

        2 -> {
            for (i in 0 until 2) {
                val bitmap = bitmaps[i] ?: continue
                drawAndRecycle(bitmap, halfSize, size, (i * halfSize).toFloat(), 0f)
            }
        }

        3 -> {
            val leftBitmap = bitmaps[0]
            if (leftBitmap != null) {
                drawAndRecycle(leftBitmap, halfSize, size, 0f, 0f)
            }
            for (i in 1 until 3) {
                val bitmap = bitmaps[i] ?: continue
                drawAndRecycle(bitmap, halfSize, halfSize, halfSize.toFloat(), ((i - 1) * halfSize).toFloat())
            }
        }

        else -> {
            for (i in 0 until 4) {
                val bitmap = bitmaps[i] ?: continue
                drawAndRecycle(bitmap, halfSize, halfSize, ((i % 2) * halfSize).toFloat(), ((i / 2) * halfSize).toFloat())
            }
        }
    }

    return canvas
}
