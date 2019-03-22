package com.android.iflyings.player.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.support.v4.provider.DocumentFile
import java.io.FileDescriptor

import java.io.IOException
import java.io.InputStream

object BitmapUtils {

    fun loadBitmapFromText(text: String, textSize: Int): Bitmap {
        val paint = Paint()
        paint.textAlign = Paint.Align.LEFT// 若设置为center，则文本左半部分显示不全 paint.setColor(Color.RED);
        paint.isAntiAlias = true// 消除锯齿
        paint.textSize = textSize.toFloat()
        paint.color = Color.WHITE
        val fontMetrics = paint.fontMetrics
        val width = paint.measureText(text)
        val height = fontMetrics.bottom - fontMetrics.top
        val baseline = -fontMetrics.top
        val showRect = Rect()
        paint.getTextBounds(text, 0, text.length, showRect)
        val whiteBitmap = Bitmap.createBitmap(Math.ceil(width.toDouble()).toInt(), Math.ceil(height.toDouble()).toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(whiteBitmap)
        canvas.drawColor(Color.BLACK)
        canvas.drawText(text, 0f, baseline, paint)
        canvas.save()
        canvas.restore()
        return whiteBitmap
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val halfWidth = options.outWidth / 2
        val halfHeight = options.outHeight / 2
        var inSampleSize = 1
        while (halfWidth / inSampleSize > reqWidth && halfHeight / inSampleSize > reqHeight) {
            inSampleSize *= 2
        }
        return inSampleSize
    }

    fun loadBitmapFromPath(imagePath: String, width: Int, height: Int, rect: Rect?): Bitmap {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        BitmapFactory.decodeFile(imagePath, options)
        options.inSampleSize = calculateInSampleSize(options, width, height)
        if (rect != null) {
            rect.left = rect.left / options.inSampleSize
            rect.top = rect.top / options.inSampleSize
            rect.right = rect.right / options.inSampleSize
            rect.bottom = rect.bottom / options.inSampleSize
        }
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(imagePath, options)
    }

    fun loadBitmapFromStream(fileDescriptor: FileDescriptor, width: Int, height: Int): Bitmap {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options)
        options.inSampleSize = calculateInSampleSize(options, width, height)
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options)
    }

    fun loadBitmapFromAssets(context: Context, filePath: String): Bitmap? {
        var inputStream: InputStream? = null
        try {
            inputStream = context.resources.assets.open(filePath)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        if (inputStream == null) return null
        val options = BitmapFactory.Options()
        options.inScaled = false
        return BitmapFactory.decodeStream(inputStream)
    }

    fun loadBitmapFromRaw(context: Context, resourceId: Int): Bitmap {
        val options = BitmapFactory.Options()
        options.inScaled = false
        return BitmapFactory.decodeResource(context.resources, resourceId, options)
    }
}
