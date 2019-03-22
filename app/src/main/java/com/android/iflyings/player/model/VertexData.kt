package com.android.iflyings.player.model

import android.graphics.Rect

class VertexData {

    private var mCanvasRect: Rect? = null

    var windowWidth = 0
        private set
    var windowHeight = 0
        private set

    var radius: Float = 0.0f
        private set
    var centerX: Float = 0.0f
        private set
    var centerY: Float = 0.0f
        private set

    val left: Int
        get() = mCanvasRect?.left ?: 0

    val top: Int
        get() = mCanvasRect?.top ?: 0

    val right: Int
        get() = mCanvasRect?.right ?: windowWidth

    val bottom: Int
        get() = mCanvasRect?.bottom ?: windowHeight

    val width: Int
        get() = mCanvasRect?.width() ?: windowWidth

    val height: Int
        get() = mCanvasRect?.height() ?: windowHeight

    fun setWindowSize(width: Int, height: Int) {
        windowWidth = width
        windowHeight = height
        update(mCanvasRect, width, height)
    }

    fun setCanvasRect(rect: Rect?) {
        mCanvasRect = rect
        update(rect, windowWidth, windowHeight)
    }

    fun isAvailable(): Boolean {
        return windowWidth > 0 && windowHeight > 0
    }

    private fun update(rect: Rect?, width: Int, height: Int) {
        if (!isAvailable()) {
            throw IllegalArgumentException("vertex data is error")
        }
        if (rect != null) {
            val w = rect.width().toDouble()
            val h = rect.height().toDouble()
            radius = (Math.sqrt(w * w + h * h) / 2.0f).toFloat()
            centerX = rect.centerX().toFloat()
            centerY = rect.centerY().toFloat()
        } else {
            val w = width.toDouble()
            val h = height.toDouble()
            radius = (Math.sqrt(w * w + h * h) / 2.0f).toFloat()
            centerX = width / 2.0f
            centerY = height / 2.0f
        }
    }

    fun getVertexBuffer(rect: Rect?): FloatArray {
        val vertexs = FloatArray(12)
        if (rect != null) {
            vertexs[0] = 2.0f * rect.left / windowWidth - 1
            vertexs[1] = 1 - 2.0f * rect.bottom / windowHeight
            vertexs[2] = 0f
            vertexs[3] = 2.0f * rect.right / windowWidth - 1
            vertexs[4] = 1 - 2.0f * rect.bottom / windowHeight
            vertexs[5] = 0f
            vertexs[6] = 2.0f * rect.left / windowWidth - 1
            vertexs[7] = 1 - 2.0f * rect.top / windowHeight
            vertexs[8] = 0f
            vertexs[9] = 2.0f * rect.right / windowWidth - 1
            vertexs[10] = 1 - 2.0f * rect.top / windowHeight
            vertexs[11] = 0f
        } else {
            vertexs[0] = 2.0f * left / windowWidth - 1
            vertexs[1] = 1 - 2.0f * bottom / windowHeight
            vertexs[2] = 0f
            vertexs[3] = 2.0f * right / windowWidth - 1
            vertexs[4] = 1 - 2.0f * bottom / windowHeight
            vertexs[5] = 0f
            vertexs[6] = 2.0f * left / windowWidth - 1
            vertexs[7] = 1 - 2.0f * top / windowHeight
            vertexs[8] = 0f
            vertexs[9] = 2.0f * right / windowWidth - 1
            vertexs[10] = 1 - 2.0f * top / windowHeight
            vertexs[11] = 0f
        }
        //Log.i("zw","vertexs = ${vertexs[0]},${vertexs[1]},${vertexs[3]},${vertexs[4]},${vertexs[6]},${vertexs[7]},${vertexs[9]},${vertexs[10]}")
        return vertexs
    }

    override fun toString(): String {
        return "VertexData: {Window=[$windowWidth,$windowHeight]," +
                "Canvas=[$left,$top,$width,$height]}"
    }
}