package com.android.iflyings.player.model

import android.graphics.Rect
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MediaModel {

    val vertexBuffer = ByteBuffer.allocateDirect(12 * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()!!
    val textureBuffer = ByteBuffer.allocateDirect(8 * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()!!

    init {
        vertexBuffer.put(CUBE).position(0)
        textureBuffer.put(TEXTURE_ROTATED_0).position(0)
    }

    fun notifyMediaModelUpdated(vertexData: VertexData, textureData: TextureData, scaleType: ScaleType) {
        if (vertexData.isAvailable() && textureData.isAvailable()) {
            scaleType.update(this, vertexData, textureData)
        }
    }

    sealed class ScaleType {

        object Center: ScaleType() {
            override fun update(mediaModel: MediaModel, vertexData: VertexData, textureData: TextureData) {
                val rect = Rect(vertexData.left, vertexData.top, vertexData.right, vertexData.bottom)
                if (1.0f * vertexData.width / vertexData.height >= 1.0f * textureData.width / textureData.height) {
                    val winWidth = (1.0f * vertexData.height * textureData.width / textureData.height).toInt()
                    val spanWidth = (vertexData.width - winWidth) / 2
                    rect.left = vertexData.left + spanWidth
                    rect.right = vertexData.right - spanWidth
                } else {
                    val winHeight = (1.0 * vertexData.width * textureData.height / textureData.width).toInt()
                    val spanHeight = (vertexData.height - winHeight) / 2
                    rect.top = vertexData.top + spanHeight
                    rect.bottom = vertexData.bottom - spanHeight
                }
                //Log.i("zw","CenterInside ${rect.left},${rect.top},${rect.right},${rect.bottom}")
                mediaModel.vertexBuffer.put(vertexData.getVertexBuffer(rect)).position(0)
                mediaModel.textureBuffer.put(textureData.getTextureBuffer(null)).position(0)
            }
        }
        object CenterInside: ScaleType() {
            override fun update(mediaModel: MediaModel, vertexData: VertexData, textureData: TextureData) {
                val rect = Rect(vertexData.left, vertexData.top, vertexData.right, vertexData.bottom)
                if (vertexData.width >= textureData.width && vertexData.height >= textureData.height) {
                    val spanWidth = (vertexData.width - textureData.width) / 2
                    val spanHeight = (vertexData.height - textureData.height) / 2
                    rect.left = vertexData.left + spanWidth
                    rect.right = vertexData.right - spanWidth
                    rect.top = vertexData.top + spanHeight
                    rect.bottom = vertexData.bottom - spanHeight
                } else {
                    if (1.0f * vertexData.width / vertexData.height >= 1.0f * textureData.width / textureData.height) {
                        val winWidth = (1.0f * vertexData.height * textureData.width / textureData.height).toInt()
                        val spanWidth = (vertexData.width - winWidth) / 2
                        rect.left = vertexData.left + spanWidth
                        rect.right = vertexData.right - spanWidth
                    } else {
                        val winHeight = (1.0 * vertexData.width * textureData.height / textureData.width).toInt()
                        val spanHeight = (vertexData.height - winHeight) / 2
                        rect.top = vertexData.top + spanHeight
                        rect.bottom = vertexData.bottom - spanHeight
                    }
                }
                //Log.i("zw","CenterInside ${rect.left},${rect.top},${rect.right},${rect.bottom}")
                mediaModel.vertexBuffer.put(vertexData.getVertexBuffer(rect)).position(0)
                mediaModel.textureBuffer.put(textureData.getTextureBuffer(null)).position(0)
            }
        }

        object FitXY: ScaleType() {
            override fun update(mediaModel: MediaModel, vertexData: VertexData, textureData: TextureData) {
                mediaModel.vertexBuffer.put(vertexData.getVertexBuffer(null)).position(0)
                mediaModel.textureBuffer.put(textureData.getTextureBuffer(null)).position(0)
            }
        }

        object FitStart: ScaleType() {
            override fun update(mediaModel: MediaModel, vertexData: VertexData, textureData: TextureData) {
                val rect = Rect(vertexData.left, vertexData.top, vertexData.right, vertexData.bottom)
                if (vertexData.width >= textureData.width && vertexData.height >= textureData.height) {
                    rect.right = vertexData.left + textureData.width
                    rect.bottom = vertexData.top + textureData.height
                } else {
                    if (1.0f * vertexData.width / vertexData.height >= 1.0f * textureData.width / textureData.height) {
                        val winWidth = 1.0f * vertexData.height * textureData.width / textureData.height
                        rect.bottom = vertexData.bottom
                        rect.right = vertexData.left + winWidth.toInt()
                    } else {
                        val winHeight = 1.0 * vertexData.width * textureData.height / textureData.width
                        rect.right = vertexData.right
                        rect.bottom = vertexData.top + winHeight.toInt()
                    }
                }
                mediaModel.vertexBuffer.put(vertexData.getVertexBuffer(rect)).position(0)
                mediaModel.textureBuffer.put(textureData.getTextureBuffer(null)).position(0)
            }
        }

        abstract fun update(mediaModel: MediaModel, vertexData: VertexData, textureData: TextureData)

    }

    companion object {
        private val CUBE                = floatArrayOf(-1.0f, -1.0f, 0.0f, 1.0f, -1.0f, 0.0f, -1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f)

        private val TEXTURE_ROTATED_0   = floatArrayOf(0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f)
        private val TEXTURE_ROTATED_90  = floatArrayOf(1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f)
        private val TEXTURE_ROTATED_180 = floatArrayOf(1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f)
        private val TEXTURE_ROTATED_270 = floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f)
    }

}