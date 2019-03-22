package com.android.iflyings.player.info

import android.graphics.Rect
import android.support.v4.provider.DocumentFile
import com.android.iflyings.player.MyApplication
import com.android.iflyings.player.shader.ImageShader
import com.android.iflyings.player.shader.MediaShader
import com.android.iflyings.player.utils.BitmapUtils
import com.android.iflyings.player.utils.TextureUtils
import kotlinx.coroutines.*


class ImageInfo private constructor(texRect: Rect? = null) : MediaInfo(texRect) {

    private val mTexRect: Rect? = texRect
    private lateinit var mImageFile: String
    private var mPlayDuring = 0L
    private var mPlayJob: Job? = null

    override fun onCreated(width: Int, height: Int): MediaShader {
        BitmapUtils.loadBitmapFromPath(mImageFile, width, height, mTexRect).also {
            setTextureSizeAndRect(it.width, it.height, mTexRect)
            postInGLThread(Runnable {
                val textureId = TextureUtils.createBitmapTexture(it, it.width, it.height)
                setTextureId(textureId)
                it.recycle()
                mPlayJob = GlobalScope.launch(Dispatchers.Default) {
                    notifyMediaCreated()
                    delay(mPlayDuring)
                    notifyMediaCompleted()
                }
            })
        }
        return ImageShader.getInstance()
    }

    override fun onDraw() {

    }

    override fun onDestroyed() {
        mPlayJob?.cancel()
        mPlayJob = null
    }

    override fun toString(): String {
        return super.toString() + "\n" + "ImagePath=$mImageFile"
    }

    companion object {
        fun from(file: String, rect: Rect? = null, during: Long = 5000): ImageInfo {
            val imageInfo = ImageInfo(rect)
            imageInfo.mImageFile = file
            imageInfo.mPlayDuring = during
            return imageInfo
        }
    }
}
