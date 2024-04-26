package com.willeypianotuning.toneanalyzer.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import timber.log.Timber
import kotlin.math.max
import kotlin.math.min

class BitmapUtils(private val context: Context) {

    private fun getBitmapBounds(imageId: Int): Rect {
        val bounds = Rect()
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeResource(context.resources, imageId, options)
        bounds.right = options.outWidth
        bounds.bottom = options.outHeight
        return bounds
    }

    fun decodeBitmap(imageId: Int, width: Int, height: Int): Bitmap? {
        val bounds = getBitmapBounds(imageId)
        var sampleSize = max(bounds.width() / width, bounds.height() / height)
        sampleSize = min(
            sampleSize,
            max(bounds.width() / height, bounds.height() / width)
        )
        val options = BitmapFactory.Options()
        options.inSampleSize = max(sampleSize, 1)
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        val bitmap = BitmapFactory.decodeResource(context.resources, imageId, options)

        // Scale down the sampled bitmap if it's still larger than the desired dimension.
        if (bitmap != null) {
            var scale = min(
                width.toFloat() / bitmap.width,
                height.toFloat() / bitmap.height
            )
            scale = max(
                scale, min(
                    height.toFloat() / bitmap.width,
                    width.toFloat() / bitmap.height
                )
            )
            if (scale < 1) {
                val m = Matrix()
                m.setScale(scale, scale)
                val transformed = createBitmap(bitmap, m)
                bitmap.recycle()
                return transformed
            }
        } else {
            Timber.e("Bitmap decoding failed")
        }
        return bitmap
    }

    companion object {
        private fun createBitmap(bitmap: Bitmap, m: Matrix): Bitmap {
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
        }
    }
}