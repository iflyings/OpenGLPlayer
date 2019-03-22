package com.android.iflyings.player.info

import android.graphics.Color
import android.graphics.Rect
import android.opengl.Matrix
import com.android.iflyings.player.MediaWindow
import com.android.iflyings.player.model.MediaModel
import com.android.iflyings.player.model.TextureData
import com.android.iflyings.player.model.VertexData
import com.android.iflyings.player.model.WindowData
import com.android.iflyings.player.shader.MediaShader
import com.android.iflyings.player.transformer.*
import com.android.iflyings.player.utils.TextureUtils
import org.json.JSONObject
import java.io.File
import kotlin.random.Random


abstract class MediaInfo internal constructor(texRect: Rect? = null) {

    private val mMediaModel = MediaModel()
    private val mTextureData = TextureData()
    private val mVertexData = VertexData()

    private lateinit var mWindowData: WindowData
    private lateinit var mMediaShader: MediaShader
    private lateinit var mMediaListener: OnMediaListener

    private var mScaleType = MediaModel.ScaleType.FitXY
    private var mTextureId = TextureUtils.NO_TEXTURE_ID

    private var mMediaTransformer = -1

    val windowLeft
        get() = mVertexData.left
    val windowRight
        get() = mVertexData.right
    val windowTop
        get() = mVertexData.top
    val windowBottom
        get() = mVertexData.bottom
    val windowWidth
        get() = mVertexData.width
    val windowHeight
        get() = mVertexData.height
    val textureLeft
        get() = mTextureData.left
    val textureRight
        get() = mTextureData.right
    val textureTop
        get() = mTextureData.top
    val textureBottom
        get() = mTextureData.bottom
    val textureWidth
        get() = mTextureData.width
    val textureHeight
        get() = mTextureData.height

    init {
        mTextureData.setTextureShow(texRect)
    }

    fun getMediaTransformer(): MediaWindow.MediaTransformer {
        val type = if (mMediaTransformer in 0..9) {
            mMediaTransformer
        } else {
            Random.nextInt(10)
        }
        return when (type) {
            0 -> FadeOutTransformer()
            1 -> ZoomOutTransformer()
            2 -> CircleOutTransformer()
            3 -> MoveLeftTransformer()
            4 -> MoveTopTransformer()
            5 -> RotateHTransformer()
            6 -> RotateVTransformer()
            7 -> ShutterHVTransformer()
            8 -> MosaicTransformer()
            9 -> ColourElapseTransformer()
            else -> FadeOutTransformer()
        }
    }

    fun setOnMediaListener(listener: OnMediaListener) {
        mMediaListener = listener
    }

    override fun toString(): String {
        return "\n$mTextureData\n$mVertexData\n$mWindowData\n" +
                "TextureId = $mTextureId"
    }

    fun mediaCreate(windowData: WindowData) {
        mWindowData = windowData
        mVertexData.setWindowSize(windowData.windowWidth, windowData.windowHeight)
        mMediaShader = onCreated(windowData.windowWidth, windowData.windowHeight)
        val matrix = FloatArray(16)
        Matrix.setIdentityM(matrix, 0)
        mMediaShader.setTextureMatrix(matrix)
        reset()
    }
    fun mediaDraw(textureIndex: Int): Int {
        if (TextureUtils.NO_TEXTURE_ID != mTextureId) {
            onDraw()
            mMediaShader.draw(mTextureId, textureIndex, mMediaModel)
            return textureIndex + 1
        }
        return textureIndex
    }
    fun mediaDestroy() {
        postInGLThread(Runnable {
            if (TextureUtils.NO_TEXTURE_ID != mTextureId) {
                TextureUtils.unloadTexture(mTextureId)
                mTextureId = TextureUtils.NO_TEXTURE_ID
            }
            mMediaShader.destroy()
        })
        onDestroyed()
        mMediaListener.onDestroyed(this)
    }

    protected abstract fun onCreated(width: Int, height: Int): MediaShader
    protected abstract fun onDraw()
    protected abstract fun onDestroyed()

    protected fun postInGLThread(runnable: Runnable) {
        mMediaListener.runInGLThread(runnable)
    }
    protected fun postInUserThread(runnable: Runnable) {
        mMediaListener.runInUserThread(runnable)
    }

    protected fun setTextureSize(texWidth: Int, texHeight: Int) {
        mTextureData.setTextureSize(texWidth, texHeight)
        mMediaModel.notifyMediaModelUpdated(mVertexData, mTextureData, mScaleType)
    }
    protected fun setTextureSizeAndRect(texWidth: Int, texHeight: Int, texRect: Rect?) {
        mTextureData.setTextureSize(texWidth, texHeight)
        mTextureData.setTextureShow(texRect)
        mMediaModel.notifyMediaModelUpdated(mVertexData, mTextureData, mScaleType)
    }
    protected fun setTextureId(textureId: Int) {
        mTextureId = textureId
    }
    protected fun setTextureMatrix(matrix: FloatArray) {
        mMediaShader.setTextureMatrix(matrix)
    }
    protected fun notifyCanvasShowChanged(rect: Rect) {
        mMediaModel.vertexBuffer.put(mVertexData.getVertexBuffer(rect)).position(0)
    }
    protected fun notifyTextureShowChanged(rect: Rect) {
        mMediaModel.textureBuffer.put(mTextureData.getTextureBuffer(rect)).position(0)
    }

    protected fun notifyMediaCreated() {
        mMediaListener.onCreated(this)
    }
    protected fun notifyMediaCompleted() {
        mMediaListener.onCompleted(this)
    }
    protected fun notifyMediaFailed(message: String) {
        mMediaListener.onFailed(this, message)
    }

    fun setScale(x: Float, y: Float, z: Float) {
        val matrix = FloatArray(16)
        Matrix.setIdentityM(matrix, 0)
        Matrix.scaleM(matrix, 0, x, y, z)
        mMediaShader.setPositionMatrix(matrix)
    }
    fun setRotate(a: Float, x: Float, y: Float, z: Float) {
        val matrix = FloatArray(16)
        Matrix.setIdentityM(matrix, 0)
        Matrix.rotateM(matrix, 0, a, x, y, z)
        mMediaShader.setPositionMatrix(matrix)
    }

    fun reset() {
        mMediaShader.setTransType(1)
        mMediaShader.setTransData(1f, 0f, 0f, 0f)
        mMediaShader.setDrawType(0)
        val matrix = FloatArray(16)
        Matrix.setIdentityM(matrix, 0)
        mMediaShader.setPositionMatrix(matrix)
        //mMediaShader.setTextureMatrix(matrix)
        //setBurrs(1f, 1f)
    }

    fun normal() {

    }
    fun hide() {
        mMediaShader.setTransType(0)
    }
    fun setAlpha(f1: Float) {
        mMediaShader.setTransType(1)
        mMediaShader.setTransData(f1, 0f, 0f, 0f)
    }
    fun setCircle(f1: Float) {
        mMediaShader.setTransType(2)
        mMediaShader.setTransData(
                mVertexData.centerX + mWindowData.windowLeft,
                mWindowData.screenHeight - (mVertexData.centerY + mWindowData.windowTop),
                f1 * mVertexData.radius, 0f)
    }
    fun setRect(x1: Float, x2: Float, y1: Float, y2: Float) {
        mMediaShader.setTransType(3)
        mMediaShader.setTransData(
                mVertexData.left + mVertexData.width * x1,
                mVertexData.left + mVertexData.width * x2,
                mWindowData.screenHeight - (mWindowData.windowTop + mVertexData.height * y2),
                mWindowData.screenHeight - (mWindowData.windowTop + mVertexData.height * y1))
    }
    fun setShutter(f1: Float) {
        mMediaShader.setTransType(4)
        mMediaShader.setTransData(
                mWindowData.screenHeight.toFloat() - mVertexData.bottom.toFloat(),
                mVertexData.left.toFloat(),
                100f, 100 * f1)
    }
    fun setBright(f1: Float) {
        mMediaShader.setTransType(5)
        mMediaShader.setTransData(f1, 0f, 0f, 0f)
    }
    fun setThreshold(color: Int) {
        mMediaShader.setTransType(6)
        mMediaShader.setTransData(Color.red(color) / 255f, Color.green(color) / 255f, Color.blue(color) / 255f, Color.alpha(color) / 255f)
    }

    fun setNormal() {
        mMediaShader.setDrawType(0)
        mMediaShader.setDrawData(0f, 0f, 0f, 0f)
    }
    fun setGray() {
        mMediaShader.setDrawType(1)
        mMediaShader.setDrawData(0f, 0f, 0f, 0f)
    }
    fun setOld() {
        mMediaShader.setDrawType(2)
        mMediaShader.setDrawData(0f, 0f, 0f, 0f)
    }
    fun setMosaic(f1: Float) {
        mMediaShader.setDrawType(3)
        mMediaShader.setDrawData(mTextureData.width.toFloat(), mTextureData.height.toFloat(),
                200 * f1, 0f)
    }
    fun setEmboss() {
        mMediaShader.setDrawType(4)
        mMediaShader.setDrawData(mTextureData.width.toFloat(), mTextureData.height.toFloat(),
                0f, 0f)
    }
    fun setShake(f1: Float, f2: Float) {
        mMediaShader.setDrawType(5)
        mMediaShader.setDrawData(10f * f1 / mTextureData.width, 10f * f2 / mTextureData.height,
                0f, 0f)
    }
    fun setBurrs(f1: Float, f2: Float) {
        mMediaShader.setDrawType(6)
        mMediaShader.setDrawData(0.2f * f1, 5f * f2 / mTextureData.width,
                0f, 0f)
    }

    companion object {

        fun from(file: String, jsonObject: JSONObject): MediaInfo {
            val type = jsonObject.getString("type")
            val name = jsonObject.getString("name")
            val rect = jsonObject.takeIf { it.has("rect") && !it.isNull("rect") } ?.let {
                val array = jsonObject.getJSONArray("rect")
                Rect(array.getInt(0), array.getInt(1), array.getInt(2), array.getInt(3))
            }
            val effect = jsonObject.takeIf { it.has("effect") && !it.isNull("effect") } ?.let {
                jsonObject.getInt("effect")
            } ?: -1
            if ("image" == type) {
                val during = jsonObject.takeIf { it.has("during") && !it.isNull("during") } ?.let {
                    jsonObject.getLong("during")
                } ?: 5000L
                val mediaFile = File(file, name)
                if (mediaFile.exists()) {
                    val mediaInfo = ImageInfo.from(mediaFile.absolutePath, rect, during) as MediaInfo
                    mediaInfo.mMediaTransformer = effect
                    return mediaInfo
                }
            } else if ("video" == type) {
                val mediaFile = File(file, name)
                if (mediaFile.exists()) {
                    val mediaInfo = VideoInfo.from(mediaFile.absolutePath, rect) as MediaInfo
                    mediaInfo.mMediaTransformer = effect
                    return mediaInfo
                }
            }
            throw IllegalStateException("unknow media type")
        }

        fun from(file: String, type: String): MediaInfo {
            if ("image" == type) {
                return ImageInfo.from(file)
            } else if ("video" == type) {
                return VideoInfo.from(file)
            }
            throw IllegalStateException("unknow media type")
        }
    }

    interface OnMediaListener {

        fun runInGLThread(r: Runnable)

        fun runInUserThread(r: Runnable)

        fun onCreated(m: MediaInfo)

        fun onCompleted(m: MediaInfo)

        fun onFailed(m: MediaInfo, msg: String)

        fun onDestroyed(m: MediaInfo)

    }

}
