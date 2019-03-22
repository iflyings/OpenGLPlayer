package com.android.iflyings.player.info

import android.graphics.Color
import android.graphics.Rect
import android.util.Log
import com.android.iflyings.player.shader.ImageShader
import com.android.iflyings.player.shader.MediaShader
import com.android.iflyings.player.utils.BitmapUtils
import com.android.iflyings.player.utils.TextureUtils
import kotlinx.coroutines.*

class TextInfo private constructor() : MediaInfo() {

    private lateinit var mTextString: String
    private var mTextSize = 20
    private var mTextColor = Color.RED
    private var mPlayJob: Job? = null

    val textColor
        get() = mTextColor

    private fun updateBottom(endPos: Int) {
        if (endPos > 0) {
            var showLeft = windowLeft + endPos - textureWidth
            var showRight = windowLeft + endPos
            val showTop = windowBottom - textureHeight
            val showBottom = windowBottom
            showLeft = if (showLeft < windowLeft) windowLeft else showLeft
            showRight = if (showRight > windowRight) windowRight else showRight

            var texLeft = textureWidth - endPos
            var texRight = windowWidth + textureWidth - endPos
            val texTop = 0
            val texBottom = textureHeight
            texLeft = if (texLeft < 0) 0 else texLeft
            texRight = if (texRight > textureWidth) textureWidth else texRight

            notifyCanvasShowChanged(Rect(showLeft, showTop, showRight, showBottom))
            notifyTextureShowChanged(Rect(texLeft, texTop, texRight, texBottom))
            //Log.i("zw","Font Media update ->show($showLeft, $showTop, $showRight, $showBottom) tex($texLeft, $texTop, $texRight, $texBottom)")
        }
    }

    private fun updateTop(endPos: Int) {
        if (endPos > 0) {
            Log.i("zw","endPos = $endPos,windowLeft = $windowLeft,textureWidth = $textureWidth")
            var showLeft = windowLeft + endPos - textureWidth
            var showRight = windowLeft + endPos
            val showTop = 0
            val showBottom = textureHeight
            showLeft = if (showLeft < windowLeft) windowLeft else showLeft
            showRight = if (showRight > windowRight) windowRight else showRight

            var texLeft = textureWidth - endPos
            var texRight = windowWidth + textureWidth - endPos
            val texTop = 0
            val texBottom = textureHeight
            texLeft = if (texLeft < 0) 0 else texLeft
            texRight = if (texRight > textureWidth) textureWidth else texRight

            notifyCanvasShowChanged(Rect(showLeft, showTop, showRight, showBottom))
            notifyTextureShowChanged(Rect(texLeft, texTop, texRight, texBottom))
            Log.i("zw","Font Media update ->show($showLeft, $showTop, $showRight, $showBottom) tex($texLeft, $texTop, $texRight, $texBottom)")
        }
    }

    override fun onCreated(width: Int, height: Int): MediaShader {
        BitmapUtils.loadBitmapFromText(mTextString, mTextSize).also {
            setTextureSize(it.width, it.height)
            postInGLThread(Runnable {
                val textureId = TextureUtils.createBitmapTexture(it, it.width, it.height)
                setTextureId(textureId)
                it.recycle()
                mPlayJob = GlobalScope.launch(Dispatchers.Default) {
                    notifyMediaCreated()
                    var endPos = windowWidth + textureWidth
                    updateTop(endPos)
                    while (endPos > 0) {
                        updateTop(endPos)
                        endPos -= 5
                        delay(100)
                    }
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

    companion object {

        fun from(textString: String, textSize: Int, textColor: Int): TextInfo {
            val textInfo = TextInfo()
            textInfo.mTextString = textString
            textInfo.mTextSize = textSize
            textInfo.mTextColor = textColor
            return textInfo
        }

    }

}