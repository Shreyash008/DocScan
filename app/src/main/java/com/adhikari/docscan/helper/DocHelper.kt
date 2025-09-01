package com.adhikari.docscan.helper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.core.graphics.createBitmap

fun Bitmap.toGrayscale(): Bitmap {
    val config = this.config ?: Bitmap.Config.ARGB_8888
    val grayscaleBitmap = createBitmap(width, height, config)

    val canvas = Canvas(grayscaleBitmap)
    val paint = Paint()
    val colorMatrix = ColorMatrix().apply { setSaturation(0f) }
    paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
    canvas.drawBitmap(this, 0f, 0f, paint)

    return grayscaleBitmap
}


fun Bitmap.toHighContrast(): Bitmap {
    val config = this.config ?: Bitmap.Config.ARGB_8888
    val contrastBitmap = createBitmap(width, height, config)

    val canvas = Canvas(contrastBitmap)
    val paint = Paint()
    val colorMatrix = ColorMatrix().apply {
        // Increase contrast (1.5f) and slightly adjust brightness
        set(floatArrayOf(
            1.5f, 0f,   0f,   0f, -50f,
            0f,   1.5f, 0f,   0f, -50f,
            0f,   0f,   1.5f, 0f, -50f,
            0f,   0f,   0f,   1f,  0f
        ))
    }
    paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
    canvas.drawBitmap(this, 0f, 0f, paint)

    return contrastBitmap
}

