package com.android.iflyings.player.model

import android.graphics.Rect
import android.util.Log
import java.lang.IllegalArgumentException


class TextureData {

    private var mTexRect: Rect? = null
    var texWidth = 0
        private set
    var texHeight = 0
        private set

    val left: Int
        get() = if (mTexRect != null) mTexRect!!.left else 0

    val top: Int
        get() = if (mTexRect != null) mTexRect!!.top else 0

    val right: Int
        get() = if (mTexRect != null) mTexRect!!.right else texWidth

    val bottom: Int
        get() = if (mTexRect != null) mTexRect!!.bottom else texHeight

    val width: Int
        get() = if (mTexRect != null) mTexRect!!.width() else texWidth

    val height: Int
        get() = if (mTexRect != null) mTexRect!!.height() else texHeight

    fun setTextureSize(tWidth: Int, tHeight: Int) {
        texWidth = tWidth
        texHeight = tHeight
    }

    fun setTextureShow(rect: Rect?) {
        mTexRect = rect
    }

    fun isAvailable(): Boolean {
        return texWidth > 0 && texHeight > 0
    }

    fun getTextureBuffer(rect: Rect?): FloatArray {
        if (!isAvailable()) {
            throw IllegalArgumentException("vertex data is error")
        }
        val textures = FloatArray(8)
        if (rect != null) {
            textures[0] = 1.0f * rect.left / texWidth
            textures[1] = 1.0f * rect.bottom / texHeight
            textures[2] = 1.0f * rect.right / texWidth
            textures[3] = 1.0f * rect.bottom / texHeight
            textures[4] = 1.0f * rect.left / texWidth
            textures[5] = 1.0f * rect.top / texHeight
            textures[6] = 1.0f * rect.right / texWidth
            textures[7] = 1.0f * rect.top / texHeight
        } else {
            textures[0] = 1.0f * left / texWidth
            textures[1] = 1.0f * bottom / texHeight
            textures[2] = 1.0f * right / texWidth
            textures[3] = 1.0f * bottom / texHeight
            textures[4] = 1.0f * left / texWidth
            textures[5] = 1.0f * top / texHeight
            textures[6] = 1.0f * right / texWidth
            textures[7] = 1.0f * top / texHeight
        }
        //Log.i("zw","textures = ${textures[0]},${textures[1]},${textures[2]},${textures[3]},${textures[4]},${textures[5]},${textures[6]},${textures[7]}")
        return textures
    }

    override fun toString(): String {
        return "TextureData: {Texture=[$texWidth,$texHeight]," +
                "Rect=[$left,$top,$width,$height]}"
    }

}